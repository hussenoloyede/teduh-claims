package my.teduh.claims.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import my.teduh.claims.TestcontainersConfiguration;
import my.teduh.claims.accounts.DemoAccounts;
import my.teduh.claims.accounts.Role;
import my.teduh.claims.accounts.SignedInUser;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PolicyApiTest {

	private static final SignedInUser AISYAH = new SignedInUser(DemoAccounts.AISYAH, "aisyah@example.com", "",
			"Aisyah Rahman", Role.CUSTOMER);

	private static final SignedInUser FARID = new SignedInUser(DemoAccounts.FARID, "farid@example.com", "",
			"Farid Hassan", Role.AGENT);

	@Autowired
	private MockMvcTester mvc;

	@Test
	void aCustomerSeesOnlyTheirOwnPoliciesWithTheirLimits() throws Exception {
		MvcTestResult result = mvc.get().uri("/api/policies").with(user(AISYAH)).exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		String json = result.getResponse().getContentAsString();
		List<String> numbers = JsonPath.read(json, "$[*].policyNumber");
		assertThat(numbers).containsExactly("TTP-26-004817");
		Object baggageLimit = JsonPath.read(json, "$[0].limits.BAGGAGE_DELAYED");
		assertThat(new BigDecimal(baggageLimit.toString())).isEqualByComparingTo("1000");
	}

	@Test
	void agentsHaveNoPolicies() {
		assertThat(mvc.get().uri("/api/policies").with(user(FARID)).exchange()).hasStatus(HttpStatus.FORBIDDEN);
	}

}
