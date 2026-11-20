package cl.duoc.msCobranza.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para confirmar el pago de un compromiso")
public record PagoRequest(
        @Schema(example = "Tarjeta de débito") @NotBlank @Size(max = 50) String medioPago,
        @Schema(description = "Canal desde el que se paga. Si no se envía se usa \"App\"", example = "App")
        @Size(max = 30) String canal
) {
}
