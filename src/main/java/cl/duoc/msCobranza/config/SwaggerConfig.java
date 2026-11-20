package cl.duoc.msCobranza.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI swaggerInfo() {
        return new OpenAPI().info(new Info()
                .title("PagoFlex - Microservicio de Cobranza")
                .version("1.0.0")
                .description("Compromisos de pago, confirmación de pagos y comprobantes."));
    }
}
