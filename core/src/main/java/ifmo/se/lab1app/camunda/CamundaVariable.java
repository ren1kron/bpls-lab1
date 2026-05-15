package ifmo.se.lab1app.camunda;

public record CamundaVariable(
        Object value,
        String type
) {

    public static CamundaVariable string(String value) {
        return new CamundaVariable(value, "String");
    }

    public static CamundaVariable bool(Boolean value) {
        return new CamundaVariable(value, "Boolean");
    }

    public static CamundaVariable integer(Number value) {
        return new CamundaVariable(value == null ? null : value.longValue(), "Long");
    }

    public static CamundaVariable date(String value) {
        return new CamundaVariable(value, "Date");
    }
}
