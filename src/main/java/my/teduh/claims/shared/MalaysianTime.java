package my.teduh.claims.shared;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Teduh is a Malaysian insurer, so "today" means today in Malaysia, not in UTC. Between midnight and
 * 8 am Malaysian time the two disagree, and a claim for "today" would otherwise look like a future date.
 */
public final class MalaysianTime {

	public static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");

	private MalaysianTime() {
	}

	public static LocalDate today(Clock clock) {
		return LocalDate.now(clock.withZone(ZONE));
	}

}
