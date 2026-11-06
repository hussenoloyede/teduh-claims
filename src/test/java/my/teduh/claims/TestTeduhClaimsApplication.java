package my.teduh.claims;

import org.springframework.boot.SpringApplication;

public class TestTeduhClaimsApplication {

	public static void main(String[] args) {
		SpringApplication.from(TeduhClaimsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
