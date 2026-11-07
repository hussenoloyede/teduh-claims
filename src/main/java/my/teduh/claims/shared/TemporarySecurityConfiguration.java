package my.teduh.claims.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * WEEK 2 ONLY. Spring Security is on the classpath, so by default every request needs a sign-in.
 * Until week 3 builds real sign-in, sessions, roles and CSRF protection, this lets every request through
 * so the endpoints can be built and tested. Week 3 deletes this class.
 */
@Configuration
class TemporarySecurityConfiguration {

	@Bean
	SecurityFilterChain openUntilWeekThree(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
			.csrf(csrf -> csrf.disable());
		return http.build();
	}

}
