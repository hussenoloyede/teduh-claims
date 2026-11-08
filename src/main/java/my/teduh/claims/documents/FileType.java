package my.teduh.claims.documents;

import java.util.Arrays;
import java.util.Optional;

/**
 * The three kinds of file Teduh accepts, each recognised by the bytes it starts with (its "magic number").
 * A renamed program still starts with a program's bytes, so it can't pass as a PDF by its name.
 */
enum FileType {

	PDF("application/pdf", new byte[] { '%', 'P', 'D', 'F', '-' }),

	PNG("image/png", new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A }),

	JPEG("image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });

	private final String contentType;

	private final byte[] signature;

	FileType(String contentType, byte[] signature) {
		this.contentType = contentType;
		this.signature = signature;
	}

	String contentType() {
		return contentType;
	}

	static Optional<FileType> detect(byte[] content) {
		return Arrays.stream(values())
			.filter(type -> content.length >= type.signature.length
					&& Arrays.equals(content, 0, type.signature.length, type.signature, 0, type.signature.length))
			.findFirst();
	}

}
