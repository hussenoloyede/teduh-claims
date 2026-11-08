package my.teduh.claims.documents;

/** A file, or a set of files, that breaks a rule. The message is written for the person who uploaded it. */
public class DocumentRejectedException extends RuntimeException {

	public DocumentRejectedException(String message) {
		super(message);
	}

}
