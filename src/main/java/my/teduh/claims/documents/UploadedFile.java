package my.teduh.claims.documents;

/**
 * A file as it arrived: the name the browser sent and its bytes. Nothing about it is trusted yet.
 * The browser's content type isn't even kept, because anyone can set it to anything.
 */
public record UploadedFile(String originalName, byte[] content) {

}
