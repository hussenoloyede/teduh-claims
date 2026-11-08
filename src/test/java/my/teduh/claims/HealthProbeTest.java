package my.teduh.claims;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The two addresses Azure Container Apps will call, without signing in, to check on the app. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthProbeTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void livenessAndReadinessAnswerWithoutSigningIn() {
		assertThat(mvc.get().uri("/actuator/health/liveness").exchange()).hasStatus(HttpStatus.OK)
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("UP");
		assertThat(mvc.get().uri("/actuator/health/readiness").exchange()).hasStatus(HttpStatus.OK);
	}

	@Test
	void otherActuatorEndpointsAreClosed() {
		assertThat(mvc.get().uri("/actuator/env").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
