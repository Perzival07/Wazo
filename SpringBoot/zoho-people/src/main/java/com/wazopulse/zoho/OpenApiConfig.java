package com.wazopulse.zoho;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Swagger UI at /swagger-ui.html. The "Authorize" button fills the dev-only identity headers. */
@Configuration
@Profile("dev")
public class OpenApiConfig {
    private static final String[] HEADERS = {"X-Tenant-Id", "X-User-Id", "X-User-Email"};

    @Bean
    OpenAPI zohoOpenApi() {
        Components components = new Components();
        OpenAPI api = new OpenAPI().info(new Info().title("Wazo Pulse - Zoho People").version("0.1.0"));
        for (String h : HEADERS) {
            components.addSecuritySchemes(h, new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(h));
            api.addSecurityItem(new SecurityRequirement().addList(h));
        }
        return api.components(components);
    }
}
