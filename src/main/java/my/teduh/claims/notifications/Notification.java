package my.teduh.claims.notifications;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One message for one person about one claim. Unread until readAt is set. */
@Entity
@Table(name = "notifications")
class Notification {

	@Id
	private UUID id;

	private UUID recipientId;

	private UUID claimId;

	private String message;

	private Instant createdAt;

	private Instant readAt;

	protected Notification() {
	}

	Notification(UUID recipientId, UUID claimId, String message, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.recipientId = recipientId;
		this.claimId = claimId;
		this.message = message;
		this.createdAt = createdAt;
	}

	void markRead(Instant now) {
		if (readAt == null) {
			readAt = now;
		}
	}

	NotificationApi.Item toItem() {
		return new NotificationApi.Item(id, claimId, message, createdAt, readAt != null);
	}

}
