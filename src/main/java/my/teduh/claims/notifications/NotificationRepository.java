package my.teduh.claims.notifications;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface NotificationRepository extends JpaRepository<Notification, UUID> {

	/** The newest 50 are plenty for the bell's list. */
	List<Notification> findTop50ByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

	long countByRecipientIdAndReadAtIsNull(UUID recipientId);

	/** Someone else's notification comes back empty, like one that doesn't exist. */
	Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

	/** One UPDATE statement instead of loading every unread notification. */
	@Modifying
	@Query("update Notification n set n.readAt = :now where n.recipientId = :recipientId and n.readAt is null")
	int markAllRead(UUID recipientId, Instant now);

}
