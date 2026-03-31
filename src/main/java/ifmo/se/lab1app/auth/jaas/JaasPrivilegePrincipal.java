package ifmo.se.lab1app.auth.jaas;

import java.security.Principal;

public record JaasPrivilegePrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
