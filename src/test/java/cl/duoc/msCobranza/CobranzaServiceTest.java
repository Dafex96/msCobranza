package cl.duoc.msCobranza;

import cl.duoc.msCobranza.dto.ComprobanteResponse;
import cl.duoc.msCobranza.dto.CompromisoResponse;
import cl.duoc.msCobranza.dto.PagoRequest;
import cl.duoc.msCobranza.exception.ReglaNegocioException;
import cl.duoc.msCobranza.model.EstadoCompromiso;
import cl.duoc.msCobranza.service.CobranzaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CobranzaServiceTest {

    @Autowired
    CobranzaService servicio;

    private static final PagoRequest DEBITO = new PagoRequest("Tarjeta de débito", null);

    @Test
    void elMicroservicioCargaLosDatosDePrueba() {
        assertEquals(4, servicio.listar(1L, null).size());
    }

    @Test
    void elCompromisoVencidoPagaMontoMasRecargo() {
        CompromisoResponse antes = servicio.obtener("PF-UNI-001");
        assertEquals(EstadoCompromiso.VENCIDO, antes.estado());

        ComprobanteResponse comprobante = servicio.pagar("PF-UNI-001", DEBITO);

        assertEquals(356000 + 7000, comprobante.montoPagado());
        assertTrue(comprobante.numero().startsWith("CPR-"));
        assertEquals("App", comprobante.canal());
        assertEquals(EstadoCompromiso.PAGADO, servicio.obtener("PF-UNI-001").estado());
    }

    @Test
    void elCompromisoPendienteNoPagaRecargo() {
        ComprobanteResponse comprobante = servicio.pagar("PF-LUZ-001", DEBITO);
        assertEquals(28000, comprobante.montoPagado());
    }

    @Test
    void noSePuedePagarDosVeces() {
        servicio.pagar("PF-LUZ-001", DEBITO);
        assertThrows(ReglaNegocioException.class, () -> servicio.pagar("PF-LUZ-001", DEBITO));
    }

    @Test
    void flujoDeRevisionDesdeReportes() {
        assertEquals(EstadoCompromiso.EN_REVISION, servicio.marcarEnRevision("PF-LUZ-001").estado());
        assertThrows(ReglaNegocioException.class, () -> servicio.pagar("PF-LUZ-001", DEBITO));

        assertEquals(EstadoCompromiso.ANULADO,
                servicio.resolverRevision("PF-LUZ-001", EstadoCompromiso.ANULADO).estado());
    }
}
