package ifmo.se.lab1app.worker.kafka;

class MalformedCreativeUploadEventException extends IllegalArgumentException {

    MalformedCreativeUploadEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
