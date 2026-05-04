package cleanride.dto;

public class PhotoUploadResponse {

    private String message;
    private String fileReference;
    private String photoUrl;

    public PhotoUploadResponse() {}

    public PhotoUploadResponse(String message, String fileReference, String photoUrl) {
        this.message = message;
        this.fileReference = fileReference;
        this.photoUrl = photoUrl;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getFileReference() { return fileReference; }
    public void setFileReference(String fileReference) { this.fileReference = fileReference; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
