package my.teduh.claims.claims;

import java.util.Arrays;
import java.util.Optional;

/** The four tabs of the agent queue (A02), and how each is named in the URL: ?tab=in-review-by-me. */
public enum QueueTab {

	SUBMITTED("submitted"),
	IN_REVIEW_BY_ME("in-review-by-me"),
	WAITING_ON_CUSTOMER("waiting-on-customer"),
	DECIDED("decided");

	private final String urlName;

	QueueTab(String urlName) {
		this.urlName = urlName;
	}

	public static Optional<QueueTab> fromUrlName(String name) {
		return Arrays.stream(values()).filter(tab -> tab.urlName.equals(name)).findFirst();
	}

}
