package cl.duoc.msCobranza.controller;

import cl.duoc.msCobranza.dto.*;
import cl.duoc.msCobranza.model.EstadoCompromiso;
import cl.duoc.msCobranza.service.CobranzaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/cobranza/compromisos")
@RequiredArgsConstructor
@Tag(name = "Compromisos", description = "Compromisos de pago de los usuarios")
public class CompromisoController {

    private final CobranzaService servicio;

    @GetMapping
    @Operation(summary = "Listar compromisos",
            description = "Todos, o solo los de un usuario con usuarioId. Se puede filtrar por estado "
                    + "(Pendiente, Vencido, En revisión, Pagado, Anulado). Ordenados por vencimiento.")
    public List<CompromisoResponse> listar(
            @Parameter(description = "Id del usuario") @RequestParam(required = false) Long usuarioId,
            @Parameter(description = "Estado del compromiso") @RequestParam(required = false) String estado) {
        return servicio.listar(usuarioId, EstadoCompromiso.desde(estado));
    }

    @GetMapping("/{folio}")
    @Operation(summary = "Obtener un compromiso por folio")
    @ApiResponse(responseCode = "404", description = "No existe el folio")
    public CompromisoResponse obtener(@PathVariable String folio) {
        return servicio.obtener(folio);
    }

    @PostMapping
    @Operation(summary = "Registrar un compromiso")
    @ApiResponse(responseCode = "409", description = "Ya existe un compromiso con ese folio")
    public ResponseEntity<CompromisoResponse> crear(@Valid @RequestBody CompromisoRequest req) {
        CompromisoResponse creado = servicio.crear(req);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{folio}").buildAndExpand(creado.folio()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @DeleteMapping("/{folio}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un compromiso",
            description = "Solo si está Pendiente o Vencido y no tiene pago asociado.")
    @ApiResponse(responseCode = "409", description = "El estado del compromiso no permite eliminarlo")
    public void eliminar(@PathVariable String folio) {
        servicio.eliminar(folio);
    }

    @PostMapping("/{folio}/pago")
    @Operation(summary = "Confirmar el pago de un compromiso",
            description = "Genera el comprobante. Si el compromiso está vencido se cobra monto + recargo. "
                    + "No permite pagar una cuota si existe una anterior de la misma empresa sin pagar.")
    @ApiResponse(responseCode = "409", description = "El compromiso no admite pago en su estado actual")
    public ResponseEntity<ComprobanteResponse> pagar(@PathVariable String folio, @Valid @RequestBody PagoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.pagar(folio, req));
    }

    @PatchMapping("/{folio}/revision")
    @Operation(summary = "Marcar el compromiso En revisión",
            description = "Lo usa el microservicio de Reportes al registrar un reporte sobre el compromiso.")
    public CompromisoResponse marcarEnRevision(@PathVariable String folio) {
        return servicio.marcarEnRevision(folio);
    }

    @PatchMapping("/{folio}/resolucion")
    @Operation(summary = "Resolver la revisión del compromiso",
            description = "Lo usa el microservicio de Reportes al resolver un reporte. "
                    + "El compromiso vuelve a Pendiente o queda Anulado.")
    public CompromisoResponse resolver(@PathVariable String folio, @Valid @RequestBody ResolucionRequest req) {
        return servicio.resolverRevision(folio, req.estado());
    }
}
