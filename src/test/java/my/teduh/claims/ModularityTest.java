package my.teduh.claims;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fails the build if one module uses another module's internals, or if modules depend on each other
 * in a circle. A module's public API is the public types in its top-level package;
 * everything package-private or in a sub-package is internal.
 */
class ModularityTest {

	private final ApplicationModules modules = ApplicationModules.of(TeduhClaimsApplication.class);

	@Test
	void modulesOnlyUseEachOthersPublicParts() {
		modules.verify();
	}

}
