package my.teduh.claims.documents;

import java.time.Instant;

/** A read-only address for one file that stops working at expiresAt. */
public record DownloadLink(String url, Instant expiresAt) {

}
