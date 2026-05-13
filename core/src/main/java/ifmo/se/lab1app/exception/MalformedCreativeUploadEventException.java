package ifmo.se.lab1app.exception;

public class MalformedCreativeUploadEventException extends IllegalArgumentException {

    public MalformedCreativeUploadEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
