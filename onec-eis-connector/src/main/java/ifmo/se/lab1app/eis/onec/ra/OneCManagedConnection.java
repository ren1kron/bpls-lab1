package ifmo.se.lab1app.eis.onec.ra;

import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

public class OneCManagedConnection implements ManagedConnection {

    private final OneCManagedConnectionFactory factory;
    private final HttpClient httpClient;
    private final List<ConnectionEventListener> listeners = new CopyOnWriteArrayList<>();
    private final List<OneCConnectionImpl> handles = new ArrayList<>();
    private PrintWriter logWriter;

    OneCManagedConnection(OneCManagedConnectionFactory factory) {
        this.factory = factory;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(factory.getConnectTimeoutMillis()))
                .build();
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) {
        OneCConnectionImpl handle = new OneCConnectionImpl(this);
        handles.add(handle);
        return handle;
    }

    @Override
    public void destroy() {
        cleanup();
    }

    @Override
    public void cleanup() {
        for (OneCConnectionImpl handle : handles) {
            handle.detach();
        }
        handles.clear();
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof OneCConnectionImpl handle)) {
            throw new ResourceException("Unsupported 1C EIS connection handle: " + connection);
        }
        handle.attach(this);
        handles.add(handle);
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("1C EIS HTTP connector does not support XA transactions");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("1C EIS HTTP connector does not support local transactions");
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new OneCManagedConnectionMetaData(factory.getEndpointUrl());
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    void send(String jsonPayload) throws ResourceException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(factory.getEndpointUrl()))
                    .timeout(Duration.ofMillis(factory.getRequestTimeoutMillis()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", basicAuthHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResourceException("1C EIS returned HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResourceException("Interrupted while calling 1C EIS", exception);
        } catch (Exception exception) {
            if (exception instanceof ResourceException resourceException) {
                throw resourceException;
            }
            throw new ResourceException("Failed to call 1C EIS", exception);
        }
    }

    void closeHandle(OneCConnectionImpl handle) {
        handles.remove(handle);
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for (ConnectionEventListener listener : listeners) {
            listener.connectionClosed(event);
        }
    }

    private String basicAuthHeader() {
        String credentials = factory.getUsername() + ":" + factory.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
