package ifmo.se.lab1app.auth.domain;

public enum Privilege {
    CAMPAIGN_VIEW("campaign:view"),
    CAMPAIGN_CREATE("campaign:create"),
    CAMPAIGN_EDIT("campaign:edit"),
    CAMPAIGN_CONFIGURE("campaign:configure"),
    CREATIVE_MANAGE("creative:manage"),
    CAMPAIGN_SUBMIT("campaign:submit"),
    CAMPAIGN_DELETE("campaign:delete"),
    CAMPAIGN_FREEZE("campaign:freeze"),
    CAMPAIGN_PROCEED("campaign:proceed"),
    CAMPAIGN_MODERATE("campaign:moderate"),
    SCHEDULER_RUN("scheduler:run");

    private final String authority;

    Privilege(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
