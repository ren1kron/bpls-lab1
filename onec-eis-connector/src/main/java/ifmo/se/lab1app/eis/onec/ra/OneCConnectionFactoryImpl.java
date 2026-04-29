package ifmo.se.lab1app.eis.onec.ra;

import ifmo.se.lab1app.eis.onec.OneCConnection;
import ifmo.se.lab1app.eis.onec.OneCConnectionFactory;
import java.io.Serial;
import javax.naming.Reference;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

public class OneCConnectionFactoryImpl implements OneCConnectionFactory, Referenceable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final OneCManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;
    private Reference reference;

    public OneCConnectionFactoryImpl(
            OneCManagedConnectionFactory managedConnectionFactory,
            ConnectionManager connectionManager
    ) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
    }

    @Override
    public OneCConnection getConnection() throws ResourceException {
        if (connectionManager == null) {
            return (OneCConnection) managedConnectionFactory
                    .createManagedConnection(null, null)
                    .getConnection(null, null);
        }
        return (OneCConnection) connectionManager.allocateConnection(managedConnectionFactory, null);
    }

    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    @Override
    public Reference getReference() {
        return reference;
    }
}
