package my.teduh.claims.notifications;

import java.time.Clock;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import my.teduh.claims.claims.ClaimStatus;
import my.teduh.claims.claims.ClaimStatusChanged;

/**
 * Turns each claim change into a notification for the person who needs to act or know next.
 *
 * @ApplicationModuleListener means: run after the claim's transaction has committed, on a background thread,
 * in a new transaction of its own. Spring Modulith marks the stored event as delivered when this method
 * returns. If the app stops first, the event is still in event_publication and is delivered again on the
 * next start (republish-outstanding-events-on-restart in application.yaml).
 *
 * The claims module never calls this class and doesn't know it exists.
 */
@Component
class ClaimNotifications {

	private final NotificationRepository notifications;

	private final Clock clock;

	ClaimNotifications(NotificationRepository notifications, Clock clock) {
		this.notifications = notifications;
		this.clock = clock;
	}

	@ApplicationModuleListener
	void on(ClaimStatusChanged change) {
		String ref = change.reference();
		switch (change.to()) {
			case SUBMITTED -> send(change.customerId(), change,
					"We've received your claim " + ref + ". An agent will start reviewing it soon.");
			case UNDER_REVIEW -> {
				if (change.from() == ClaimStatus.MORE_INFO_NEEDED) {
					send(change.assignedAgentId(), change, "The customer responded on claim " + ref + ".");
				}
				else {
					send(change.customerId(), change, "An agent has started reviewing your claim " + ref + ".");
				}
			}
			case MORE_INFO_NEEDED -> send(change.customerId(), change,
					"Your agent needs more information for claim " + ref + ". See the claim page for what to send.");
			case APPROVED -> send(change.customerId(), change,
					"Your claim " + ref + " was approved. See the claim page for the amount.");
			case REJECTED -> send(change.customerId(), change,
					"Your claim " + ref + " was rejected. See the claim page for the reason.");
		}
	}

	private void send(UUID recipient, ClaimStatusChanged change, String message) {
		notifications.save(new Notification(recipient, change.claimId(), message, clock.instant()));
	}

}
