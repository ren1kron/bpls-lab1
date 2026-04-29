package ifmo.se.lab1app.eis.onec;

import java.io.Serializable;
import jakarta.resource.ResourceException;

public interface OneCConnectionFactory extends Serializable {

    OneCConnection getConnection() throws ResourceException;
}
