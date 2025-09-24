package interntask.fileprocessor.dto;

public record FileStatusUpdate(String fileHash, String status, String message) {
    public String toJson() {
        return """
        {"fileHash":"%s","status":"%s","message":"%s"}""".formatted(fileHash, status, message);
    }
}
