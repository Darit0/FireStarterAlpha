package interntask.fileuploader.exception;

public class FileValidationException extends RuntimeException {
    private final String errorType;

    public FileValidationException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public String getErrorType() {
        return errorType;
    }
}