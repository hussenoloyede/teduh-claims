package my.teduh.claims.documents;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** The checks every file passes before it is stored. Plain Java, so they are unit tested without Spring. */
final class FileCheck {

	static final int MAX_BYTES = 10 * 1024 * 1024;

	static final int MAX_NAME_LENGTH = 100;

	private FileCheck() {
	}

	/** A file that passed: a safe name, its real type, its bytes and their SHA-256 fingerprint. */
	record CheckedFile(String name, FileType type, byte[] content, String sha256) {
	}

	static CheckedFile check(UploadedFile file) {
		String name = cleanName(file.originalName());
		byte[] content = (file.content() == null) ? new byte[0] : file.content();
		if (content.length == 0) {
			throw new DocumentRejectedException(name + " is empty.");
		}
		if (content.length > MAX_BYTES) {
			throw new DocumentRejectedException(name + " is larger than 10 MB.");
		}
		FileType type = FileType.detect(content)
			.orElseThrow(() -> new DocumentRejectedException(name + " isn't a PDF, JPEG or PNG."));
		return new CheckedFile(name, type, content, sha256(content));
	}

	/**
	 * Keeps the last part of the name (some browsers send a whole path such as C:\fakepath\receipt.pdf),
	 * then replaces anything other than letters, digits, dots, dashes and underscores. The name ends up in a
	 * storage path and a download header, so nothing in it may change the meaning of either.
	 */
	static String cleanName(String originalName) {
		String name = (originalName == null) ? "" : originalName;
		name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
		name = name.replaceAll("[^A-Za-z0-9._-]", "_");
		if (name.length() > MAX_NAME_LENGTH) {
			name = name.substring(name.length() - MAX_NAME_LENGTH);
		}
		return name.isBlank() || name.chars().allMatch(c -> c == '.') ? "document" : name;
	}

	/** A fingerprint of the bytes. Kept with each file so a later change or a duplicate can be spotted. */
	static String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every Java runtime has SHA-256", e);
		}
	}

}
