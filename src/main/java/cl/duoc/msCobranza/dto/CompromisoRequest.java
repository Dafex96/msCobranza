package cl.duoc.msCobranza.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Datos para registrar un compromiso de pago")
public record CompromisoRequest(
        @Schema(example = "PF-LUZ-002") @NotBlank @Size(max = 40) String folio,
        @Schema(description = "Id del usuario en el microservicio de Usuarios", example = "1")
        @NotNull @Positive Long usuarioId,
        @Schema(example = "20.435.214-5") @Size(max = 15) String rutUsuario,
        @Schema(example = "Sofia Perez") @Size(max = 120) String nombreUsuario,
        @Schema(example = "Enel") @NotBlank @Size(max = 120) String empresa,
        @Schema(example = "Luz") @NotBlank @Size(max = 200) String concepto,
        @Schema(example = "28000") @NotNull @Positive Integer monto,
        @Schema(example = "2026-12-25") @NotNull LocalDate vencimiento,
        @Schema(description = "Recargo que se suma si el compromiso vence sin pagarse", example = "1100")
        @PositiveOrZero Integer recargo,
        @Schema(example = "1/3") @Size(max = 20) String numeroCuota
) {
}
