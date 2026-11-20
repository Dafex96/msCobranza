package cl.duoc.msCobranza.controller;

import cl.duoc.msCobranza.dto.ComprobanteResponse;
import cl.duoc.msCobranza.service.CobranzaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cobranza/comprobantes")
@RequiredArgsConstructor
@Tag(name = "Comprobantes", description = "Comprobantes de pago (solo lectura, son inmutables)")
public class ComprobanteController {

    private final CobranzaService servicio;

    @GetMapping
    @Operation(summary = "Historial de pagos de un usuario", description = "Del más reciente al más antiguo.")
    public List<ComprobanteResponse> historial(@RequestParam Long usuarioId) {
        return servicio.comprobantesDeUsuario(usuarioId);
    }

    @GetMapping("/{numero}")
    @Operation(summary = "Obtener un comprobante por su número (CPR-000001)")
    public ComprobanteResponse porNumero(@PathVariable String numero) {
        return servicio.comprobantePorNumero(numero);
    }

    @GetMapping("/folio/{folio}")
    @Operation(summary = "Obtener el comprobante de un compromiso por su folio")
    public ComprobanteResponse porFolio(@PathVariable String folio) {
        return servicio.comprobantePorFolio(folio);
    }
}
