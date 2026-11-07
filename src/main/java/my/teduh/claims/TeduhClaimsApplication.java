package my.teduh.claims;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TeduhClaimsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeduhClaimsApplication.class, args);
	}

	/**
	 * The one place the app reads the time from. Tests swap in a fixed clock, so "today" and
	 * "now" are the same on every run.
	 */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
