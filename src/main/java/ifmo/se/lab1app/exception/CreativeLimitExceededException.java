package ifmo.se.lab1app.exception;

public class CreativeLimitExceededException extends RuntimeException {
    public CreativeLimitExceededException(String message) {
        super(message);
    }
}
