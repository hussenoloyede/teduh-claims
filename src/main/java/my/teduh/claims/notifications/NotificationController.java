package my.teduh.claims.notifications;

import java.time.Clock;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import my.teduh.claims.accounts.SignedInUser;
import my.teduh.claims.notifications.NotificationApi.Inbox;

/**
 * The bell. Customers and agents both have one, and each only ever sees their own notifications.
 * Small enough that the controller talks to the repository directly; there are no rules to put in a service.
 */
@RestController
@RequestMapping("/api/notifications")
class NotificationController {

	private final NotificationRepository notifications;

	private final Clock clock;

	NotificationController(NotificationRepository notifications, Clock clock) {
		this.notifications = notifications;
		this.clock = clock;
	}

	/** The React app asks for this every 30 seconds and whenever the tab comes back into view. */
	@GetMapping
	@Transactional(readOnly = true)
	public Inbox inbox(@AuthenticationPrincipal SignedInUser me) {
		return new Inbox(notifications.countByRecipientIdAndReadAtIsNull(me.id()),
				notifications.findTop50ByRecipientIdOrderByCreatedAtDesc(me.id())
					.stream()
					.map(Notification::toItem)
					.toList());
	}

	@PostMapping("/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void markRead(@AuthenticationPrincipal SignedInUser me, @PathVariable UUID id) {
		notifications.findByIdAndRecipientId(id, me.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found."))
			.markRead(clock.instant());
	}

	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void markAllRead(@AuthenticationPrincipal SignedInUser me) {
		notifications.markAllRead(me.id(), clock.instant());
	}

}
