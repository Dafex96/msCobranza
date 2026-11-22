package cl.duoc.msCobranza.controller;

import cl.duoc.msCobranza.dto.ComprobanteResponse;
import cl.duoc.msCobranza.service.CobranzaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprobanteControllerTest {

    @Mock
    CobranzaService servicio;
    @InjectMocks
    ComprobanteController controller;

    private final ComprobanteResponse comprobante = new ComprobanteResponse("CPR-000001", "PF-1", "Enel", "Luz",
            1000, LocalDateTime.of(2026, 11, 22, 10, 0), "App", "Efectivo");

    @Test
    void historial() {
        when(servicio.comprobantesDeUsuario(1L)).thenReturn(List.of(comprobante));
        assertEquals(1, controller.historial(1L).size());
    }

    @Test
    void porNumero() {
        when(servicio.comprobantePorNumero("CPR-000001")).thenReturn(comprobante);
        assertEquals("PF-1", controller.porNumero("CPR-000001").folioCompromiso());
    }

    @Test
    void porFolio() {
        when(servicio.comprobantePorFolio("PF-1")).thenReturn(comprobante);
        assertEquals("CPR-000001", controller.porFolio("PF-1").numero());
    }
}
