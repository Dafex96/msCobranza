package cl.duoc.msCobranza.controller;

import cl.duoc.msCobranza.dto.*;
import cl.duoc.msCobranza.model.Compromiso;
import cl.duoc.msCobranza.model.EstadoCompromiso;
import cl.duoc.msCobranza.service.CobranzaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompromisoControllerTest {

    @Mock
    CobranzaService servicio;
    @InjectMocks
    CompromisoController controller;

    private CompromisoResponse respuesta(EstadoCompromiso estado) {
        return CompromisoResponse.desde(Compromiso.builder().id(1L).folio("PF-1").usuarioId(1L).empresa("Enel")
                .concepto("Luz").monto(1000).vencimiento(LocalDate.of(2026, 12, 1)).estado(estado)
                .recargo(0).numeroCuota("").build());
    }

    @BeforeEach
    void contextoHttp() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/cobranza/compromisos");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void listarConvierteElEstadoDeTexto() {
        when(servicio.listar(1L, EstadoCompromiso.EN_REVISION)).thenReturn(List.of(respuesta(EstadoCompromiso.EN_REVISION)));
        assertEquals(1, controller.listar(1L, "En revisión").size());
    }

    @Test
    void listarSinEstadoNoFiltra() {
        when(servicio.listar(null, null)).thenReturn(List.of());
        assertTrue(controller.listar(null, null).isEmpty());
        verify(servicio).listar(null, null);
    }

    @Test
    void obtenerDelegaEnElServicio() {
        when(servicio.obtener("PF-1")).thenReturn(respuesta(EstadoCompromiso.PENDIENTE));
        assertEquals("PF-1", controller.obtener("PF-1").folio());
    }

    @Test
    void crearResponde201ConLocation() {
        CompromisoRequest req = new CompromisoRequest("PF-1", 1L, null, null, "Enel", "Luz", 1000,
                LocalDate.of(2026, 12, 1), 0, "");
        when(servicio.crear(req)).thenReturn(respuesta(EstadoCompromiso.PENDIENTE));

        ResponseEntity<CompromisoResponse> r = controller.crear(req);

        assertEquals(201, r.getStatusCode().value());
        assertNotNull(r.getHeaders().getLocation());
        assertTrue(r.getHeaders().getLocation().toString().endsWith("/api/cobranza/compromisos/PF-1"));
    }

    @Test
    void eliminarDelegaEnElServicio() {
        controller.eliminar("PF-1");
        verify(servicio).eliminar("PF-1");
    }

    @Test
    void pagarResponde201ConElComprobante() {
        PagoRequest req = new PagoRequest("Efectivo", null);
        ComprobanteResponse comprobante = new ComprobanteResponse("CPR-000001", "PF-1", "Enel", "Luz", 1000,
                LocalDateTime.of(2026, 11, 22, 10, 0), "App", "Efectivo");
        when(servicio.pagar("PF-1", req)).thenReturn(comprobante);

        ResponseEntity<ComprobanteResponse> r = controller.pagar("PF-1", req);

        assertEquals(201, r.getStatusCode().value());
        assertEquals("CPR-000001", r.getBody().numero());
    }

    @Test
    void marcarEnRevisionYResolver() {
        when(servicio.marcarEnRevision("PF-1")).thenReturn(respuesta(EstadoCompromiso.EN_REVISION));
        when(servicio.resolverRevision("PF-1", EstadoCompromiso.ANULADO)).thenReturn(respuesta(EstadoCompromiso.ANULADO));

        assertEquals(EstadoCompromiso.EN_REVISION, controller.marcarEnRevision("PF-1").estado());
        assertEquals(EstadoCompromiso.ANULADO,
                controller.resolver("PF-1", new ResolucionRequest(EstadoCompromiso.ANULADO)).estado());
    }
}
