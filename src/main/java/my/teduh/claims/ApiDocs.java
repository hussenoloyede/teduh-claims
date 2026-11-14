package my.teduh.claims;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The OpenAPI document at /v3/api-docs. springdoc builds it by reading the controllers and the records they
 * take and return. The React app's TypeScript types are generated from a saved copy (web/openapi.json), and
 * ApiDocsTest fails when that copy no longer matches, so a changed endpoint shows up as a TypeScript error
 * instead of a broken page.
 */
@Configuration
class ApiDocs {

	@Bean
	OpenAPI teduhApi() {
		return new OpenAPI()
			.info(new Info().title("Teduh Claims API")
				.version("1")
				.description("Claims for Teduh, a fictional travel insurer made for a portfolio project."))
			// The React app calls the API on its own site, so the address is always relative. Without this,
			// springdoc writes the address of whoever asked, which would change the saved copy.
			.servers(List.of(new Server().url("/")));
	}

	/**
	 * Jackson writes every field of a record, null or not, so a field is always in the JSON. springdoc doesn't
	 * know that and calls every field optional, which would make TypeScript ask "is it there?" on every read.
	 * This marks every field required, except those marked @Schema(nullable = true), which may be null or
	 * left out. ProblemDetail is Spring's error shape, where fields really are optional, so it is left alone.
	 */
	@Bean
	OpenApiCustomizer fieldsAreRequiredUnlessNullable() {
		return openApi -> {
			if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
				return;
			}
			openApi.getComponents().getSchemas().forEach((name, schema) -> {
				@SuppressWarnings("rawtypes")
				Map<String, Schema> properties = schema.getProperties();
				if (name.equals("ProblemDetail") || properties == null) {
					return;
				}
				List<String> required = properties.entrySet()
					.stream()
					.filter(property -> !Boolean.TRUE.equals(property.getValue().getNullable()))
					.map(Map.Entry::getKey)
					.sorted()
					.toList();
				schema.setRequired(required.isEmpty() ? null : required);
			});
		};
	}

}
