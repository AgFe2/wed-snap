package me.agfe.wedsnap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * Swagger(OpenAPI) 설정 클래스
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                              .title("WedSnap API")
                              .description("결혼식 사진 업로드 서비스 API 문서")
                              .version("1.0.0")
                              .contact(new Contact()
                                               .name("WedSnap Team")
                                               .email("support@wedsnap.com"))
                              .license(new License()
                                               .name("Apache 2.0")
                                               .url("http://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
