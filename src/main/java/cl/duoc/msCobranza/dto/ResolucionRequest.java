package cl.duoc.msCobranza.dto;

import cl.duoc.msCobranza.model.EstadoCompromiso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Resultado de la revisión de un reporte sobre un compromiso")
public record ResolucionRequest(
        @Schema(description = "Estado en que queda el compromiso: \"Pendiente\" o \"Anulado\"", example = "Pendiente")
        @NotNull EstadoCompromiso estado
) {
}
