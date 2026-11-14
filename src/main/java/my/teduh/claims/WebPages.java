package my.teduh.claims;

import java.io.IOException;
import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * The React app and the Storybook are plain files in static/ inside the jar (the Dockerfile puts them there).
 *
 * The React app has its own addresses (/claims/..., /agent/queue), which only exist in the browser: React
 * Router reads the address and shows the right screen. When someone reloads such a page or opens a shared
 * link, the browser asks the server for /claims/..., and there is no file by that name. So any address that
 * isn't a real file, an API call, the Storybook or the API document gets index.html, and React Router takes
 * it from there. A missing file (a name with a dot) still gets 404, so a broken script link fails loudly
 * instead of quietly loading the page again.
 */
@Configuration
class WebPages implements WebMvcConfigurer {

	/** First parts of an address that belong to the server, never to the React app. */
	private static final Set<String> SERVER_PATHS = Set.of("api", "actuator", "storybook", "v3");

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("forward:/index.html");
		// Without the slash, the Storybook's relative links (./iframe.html) would point at the wrong folder.
		registry.addRedirectViewController("/storybook", "/storybook/");
		registry.addViewController("/storybook/").setViewName("forward:/storybook/index.html");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			// false: no cache of answers. A cache would keep an entry for every made-up address anyone tries.
			.resourceChain(false)
			.addResolver(new ReactAppResolver());
	}

	/** Real files first; otherwise index.html for the React app's own addresses. */
	static class ReactAppResolver extends PathResourceResolver {

		@Override
		protected Resource getResource(String resourcePath, Resource location) throws IOException {
			Resource file = super.getResource(resourcePath, location);
			if (file != null || !isReactAppAddress(resourcePath)) {
				return file;
			}
			return super.getResource("index.html", location);
		}

		/** "claims/123" is; "api/claims", "storybook/x" and "assets/app.js" aren't. */
		static boolean isReactAppAddress(String path) {
			int slash = path.indexOf('/');
			String first = (slash < 0) ? path : path.substring(0, slash);
			String last = path.substring(path.lastIndexOf('/') + 1);
			return !SERVER_PATHS.contains(first) && !last.contains(".");
		}

	}

}
