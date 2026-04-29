package ifmo.se.lab1app.eis.onec.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionMetaData;

public class OneCManagedConnectionMetaData implements ManagedConnectionMetaData {

    private final String endpointUrl;

    OneCManagedConnectionMetaData(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Override
    public String getEISProductName() {
        return "1C HTTP Service";
    }

    @Override
    public String getEISProductVersion() {
        return "HTTP endpoint " + endpointUrl;
    }

    @Override
    public int getMaxConnections() {
        return 0;
    }

    @Override
    public String getUserName() throws ResourceException {
        return null;
    }
}
