package my.teduh.claims.notifications;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The JSON shapes for /api/notifications. */
public final class NotificationApi {

	private NotificationApi() {
	}

	/** What the bell needs in one request: the number on the bell and the list under it. */
	public record Inbox(long unreadCount, List<Item> notifications) {
	}

	public record Item(UUID id, UUID claimId, String message, Instant createdAt, boolean read) {
	}

}
