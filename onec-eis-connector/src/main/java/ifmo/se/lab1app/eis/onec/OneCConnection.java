package ifmo.se.lab1app.eis.onec;

import jakarta.resource.ResourceException;

public interface OneCConnection extends AutoCloseable {

    void send(String jsonPayload) throws ResourceException;

    @Override
    void close() throws ResourceException;
}
