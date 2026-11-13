package my.teduh.claims;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The React app and the Storybook are plain files in static/ inside the jar (the Dockerfile puts them there).
 * Spring Boot already answers / with static/index.html. A folder's index.html is only found automatically at
 * the root, so the Storybook's address needs these two lines.
 */
@Configuration
class WebPages implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		// Without the slash, the Storybook's relative links (./iframe.html) would point at the wrong folder.
		registry.addRedirectViewController("/storybook", "/storybook/");
		registry.addViewController("/storybook/").setViewName("forward:/storybook/index.html");
	}

}
