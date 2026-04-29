package ifmo.se.lab1app.eis.onec.ra;

import java.io.PrintWriter;
import java.io.Serial;
import java.util.Objects;
import java.util.Set;
import javax.security.auth.Subject;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;
import jakarta.resource.spi.ResourceAdapterInternalException;

public class OneCManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

    @Serial
    private static final long serialVersionUID = 1L;

    private String endpointUrl;
    private String username;
    private String password;
    private Integer connectTimeoutMillis = 5000;
    private Integer requestTimeoutMillis = 10000;
    private transient PrintWriter logWriter;
    private ResourceAdapter resourceAdapter;

    @Override
    public Object createConnectionFactory(ConnectionManager connectionManager) {
        return new OneCConnectionFactoryImpl(this, connectionManager);
    }

    @Override
    public Object createConnectionFactory() {
        return new OneCConnectionFactoryImpl(this, null);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) {
        return new OneCManagedConnection(this);
    }
    //можно ли перезапустить то что уже есть?
    @Override
    public ManagedConnection matchManagedConnections(
            @SuppressWarnings("rawtypes") Set connectionSet,
            Subject subject,
            ConnectionRequestInfo connectionRequestInfo
    ) {
        for (Object connection : connectionSet) {
            if (connection instanceof OneCManagedConnection managedConnection) {
                return managedConnection;
            }
        }
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceAdapterInternalException {
        this.resourceAdapter = resourceAdapter;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(Integer connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public Integer getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public void setRequestTimeoutMillis(Integer requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OneCManagedConnectionFactory that)) {
            return false;
        }
        return Objects.equals(endpointUrl, that.endpointUrl)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(connectTimeoutMillis, that.connectTimeoutMillis)
                && Objects.equals(requestTimeoutMillis, that.requestTimeoutMillis);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointUrl, username, password, connectTimeoutMillis, requestTimeoutMillis);
    }
}
