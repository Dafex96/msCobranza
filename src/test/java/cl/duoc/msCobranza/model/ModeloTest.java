package cl.duoc.msCobranza.model;

import cl.duoc.msCobranza.dto.ComprobanteResponse;
import cl.duoc.msCobranza.dto.CompromisoResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ModeloTest {

    @Test
    void estadoSeBuscaPorEtiquetaONombreSinTildesNiMayusculas() {
        assertEquals(EstadoCompromiso.EN_REVISION, EstadoCompromiso.desde("En revisión"));
        assertEquals(EstadoCompromiso.EN_REVISION, EstadoCompromiso.desde("en revision"));
        assertEquals(EstadoCompromiso.EN_REVISION, EstadoCompromiso.desde("EN_REVISION"));
        assertEquals(EstadoCompromiso.PAGADO, EstadoCompromiso.desde(" pagado "));
        assertEquals("En revisión", EstadoCompromiso.EN_REVISION.getEtiqueta());
    }

    @Test
    void estadoNuloDevuelveNuloYElInvalidoFalla() {
        assertNull(EstadoCompromiso.desde(null));
        assertThrows(IllegalArgumentException.class, () -> EstadoCompromiso.desde("otro"));
    }

    @Test
    void conversorGuardaLaEtiqueta() {
        EstadoCompromisoConverter c = new EstadoCompromisoConverter();
        assertEquals("Vencido", c.convertToDatabaseColumn(EstadoCompromiso.VENCIDO));
        assertEquals(EstadoCompromiso.VENCIDO, c.convertToEntityAttribute("Vencido"));
        assertNull(c.convertToDatabaseColumn(null));
        assertNull(c.convertToEntityAttribute(null));
    }

    @Test
    void elRecargoSoloAplicaSiEstaVencido() {
        Compromiso c = Compromiso.builder().monto(1000).recargo(100).estado(EstadoCompromiso.PENDIENTE).build();
        assertEquals(1000, c.montoAPagar());
        c.setEstado(EstadoCompromiso.VENCIDO);
        assertEquals(1100, c.montoAPagar());
    }

    @Test
    void valoresPorDefectoDelBuilder() {
        Compromiso c = Compromiso.builder().build();
        assertEquals(0, c.getRecargo());
        assertEquals("", c.getNumeroCuota());
    }

    @Test
    void dtosMapeanLasEntidades() {
        Compromiso c = Compromiso.builder().id(1L).folio("PF-1").usuarioId(1L).rutUsuario("1-9").nombreUsuario("Ana")
                .empresa("Enel").concepto("Luz").monto(1000).vencimiento(LocalDate.of(2026, 12, 1))
                .estado(EstadoCompromiso.VENCIDO).recargo(100).numeroCuota("1/3").build();
        CompromisoResponse r = CompromisoResponse.desde(c);
        assertEquals("PF-1", r.folio());
        assertEquals(1100, r.montoAPagar());
        assertEquals("Ana", r.nombreUsuario());

        Pago p = Pago.builder().numeroComprobante("CPR-000001").folio("PF-1").empresa("Enel").concepto("Luz")
                .montoPagado(1100).fechaHora(LocalDateTime.of(2026, 11, 22, 10, 0)).canal("App")
                .medioPago("Efectivo").build();
        ComprobanteResponse cr = ComprobanteResponse.desde(p);
        assertEquals("CPR-000001", cr.numero());
        assertEquals("PF-1", cr.folioCompromiso());
        assertEquals(1100, cr.montoPagado());
    }
}
