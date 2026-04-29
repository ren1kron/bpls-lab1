package ifmo.se.lab1app.eis.onec.ra;

import ifmo.se.lab1app.eis.onec.OneCConnection;
import jakarta.resource.ResourceException;

public class OneCConnectionImpl implements OneCConnection {

    private OneCManagedConnection managedConnection;
    private boolean closed;

    OneCConnectionImpl(OneCManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
    }

    @Override
    public void send(String jsonPayload) throws ResourceException {
        if (closed || managedConnection == null) {
            throw new ResourceException("1C EIS connection is already closed");
        }
        managedConnection.send(jsonPayload);
    }

    @Override
    public void close() throws ResourceException {
        if (!closed && managedConnection != null) {
            closed = true;
            managedConnection.closeHandle(this);
            managedConnection = null;
        }
    }

    void detach() {
        closed = true;
        managedConnection = null;
    }

    void attach(OneCManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
        this.closed = false;
    }
}
