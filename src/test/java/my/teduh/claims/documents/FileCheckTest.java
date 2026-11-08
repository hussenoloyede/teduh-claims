package my.teduh.claims.documents;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The checks every uploaded file goes through. Plain Java: no Spring, no database, no storage. */
class FileCheckTest {

	private static final byte[] PDF = "%PDF-1.7 made-up".getBytes(US_ASCII);

	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3 };

	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3 };

	@Test
	void theTypeComesFromTheFirstBytes() {
		assertThat(FileCheck.check(new UploadedFile("a.bin", PDF)).type()).isEqualTo(FileType.PDF);
		assertThat(FileCheck.check(new UploadedFile("a.bin", PNG)).type()).isEqualTo(FileType.PNG);
		assertThat(FileCheck.check(new UploadedFile("a.bin", JPEG)).type()).isEqualTo(FileType.JPEG);
	}

	@Test
	void anythingElseIsRefusedWhateverItsName() {
		byte[] zip = { 'P', 'K', 3, 4, 1, 2, 3 };

		assertThatThrownBy(() -> FileCheck.check(new UploadedFile("receipt.pdf", zip)))
			.isInstanceOf(DocumentRejectedException.class)
			.hasMessage("receipt.pdf isn't a PDF, JPEG or PNG.");
	}

	@Test
	void aFileShorterThanItsSignatureIsRefused() {
		assertThatThrownBy(() -> FileCheck.check(new UploadedFile("tiny.pdf", "%PD".getBytes(US_ASCII))))
			.isInstanceOf(DocumentRejectedException.class);
	}

	@Test
	void anEmptyFileIsRefused() {
		assertThatThrownBy(() -> FileCheck.check(new UploadedFile("receipt.pdf", new byte[0])))
			.isInstanceOf(DocumentRejectedException.class)
			.hasMessage("receipt.pdf is empty.");
	}

	@Test
	void tenMegabytesIsTheLimitExactly() {
		byte[] atLimit = Arrays.copyOf(PDF, FileCheck.MAX_BYTES);
		byte[] overLimit = Arrays.copyOf(PDF, FileCheck.MAX_BYTES + 1);

		assertThat(FileCheck.check(new UploadedFile("big.pdf", atLimit)).content()).hasSize(FileCheck.MAX_BYTES);
		assertThatThrownBy(() -> FileCheck.check(new UploadedFile("big.pdf", overLimit)))
			.isInstanceOf(DocumentRejectedException.class)
			.hasMessage("big.pdf is larger than 10 MB.");
	}

	@Test
	void theFingerprintIsSha256InHex() {
		// SHA-256 of "abc", from the standard's published test vectors.
		assertThat(FileCheck.sha256("abc".getBytes(US_ASCII)))
			.isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"receipt.pdf                  | receipt.pdf",
			"C:\\fakepath\\receipt.pdf    | receipt.pdf",
			"../../etc/passwd             | passwd",
			"My receipt (1).pdf           | My_receipt__1_.pdf",
			"résumé.pdf                   | r_sum_.pdf",
			"..                           | document",
			"''                           | document" })
	void namesAreCleaned(String original, String cleaned) {
		assertThat(FileCheck.cleanName(original)).isEqualTo(cleaned);
	}

	@Test
	void aVeryLongNameKeepsItsEnding() {
		String name = "a".repeat(150) + ".pdf";

		assertThat(FileCheck.cleanName(name)).hasSize(FileCheck.MAX_NAME_LENGTH).endsWith(".pdf");
	}

}
