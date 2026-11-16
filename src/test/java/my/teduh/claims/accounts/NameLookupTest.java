package my.teduh.claims.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import my.teduh.claims.TestcontainersConfiguration;

/** The names other modules may show, from the seeded demo people. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NameLookupTest {

	@Autowired
	private NameLookup names;

	@Test
	void knownPeopleGetTheirDisplayNamesAndUnknownIdsAreLeftOut() {
		UUID nobody = UUID.randomUUID();
		assertThat(names.namesOf(List.of(DemoAccounts.AISYAH, DemoAccounts.FARID, nobody)))
			.containsOnlyKeys(DemoAccounts.AISYAH, DemoAccounts.FARID)
			.containsEntry(DemoAccounts.AISYAH, "Aisyah Rahman")
			.containsEntry(DemoAccounts.FARID, "Farid Hassan");
	}

	@Test
	void noIdsOrOnlyMissingOnesGiveAnEmptyMap() {
		assertThat(names.namesOf(List.of())).isEmpty();
		// An unassigned claim has no agent, so a null can arrive with the other ids.
		assertThat(names.namesOf(Arrays.asList((UUID) null))).isEmpty();
	}

}
