package cl.duoc.msCobranza.service;

import cl.duoc.msCobranza.dto.*;
import cl.duoc.msCobranza.exception.RecursoNoEncontradoException;
import cl.duoc.msCobranza.exception.ReglaNegocioException;
import cl.duoc.msCobranza.model.Compromiso;
import cl.duoc.msCobranza.model.EstadoCompromiso;
import cl.duoc.msCobranza.model.Pago;
import cl.duoc.msCobranza.repository.CompromisoRepository;
import cl.duoc.msCobranza.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static cl.duoc.msCobranza.model.EstadoCompromiso.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CobranzaServiceUnitTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 22);

    @Mock
    CompromisoRepository compromisos;
    @Mock
    PagoRepository pagos;

    CobranzaService servicio;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new CobranzaService(compromisos, pagos, reloj);
    }

    private Compromiso comp(Long id, String folio, EstadoCompromiso estado, LocalDate vencimiento) {
        return Compromiso.builder().id(id).folio(folio).usuarioId(1L).rutUsuario("1-9").nombreUsuario("Ana")
                .empresa("Enel").concepto("Luz").monto(10000).vencimiento(vencimiento)
                .estado(estado).recargo(500).numeroCuota("").build();
    }

    private void existe(Compromiso c) {
        when(compromisos.findByFolio(c.getFolio())).thenReturn(Optional.of(c));
    }

    private void guardaPagoConId() {
        when(pagos.saveAndFlush(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });
    }

    @Test
    void listarSinUsuarioDevuelveTodosYSincronizaVencidos() {
        when(compromisos.findAllByOrderByVencimientoAscIdAsc()).thenReturn(List.of(
                comp(1L, "A", PENDIENTE, HOY), comp(2L, "B", VENCIDO, HOY.minusDays(1))));

        List<CompromisoResponse> lista = servicio.listar(null, null);

        assertEquals(2, lista.size());
        verify(compromisos).marcarVencidos(HOY, PENDIENTE, VENCIDO);
    }

    @Test
    void listarPorUsuarioYEstadoFiltra() {
        when(compromisos.findByUsuarioIdOrderByVencimientoAscIdAsc(1L)).thenReturn(List.of(
                comp(1L, "A", PENDIENTE, HOY), comp(2L, "B", VENCIDO, HOY.minusDays(1))));

        List<CompromisoResponse> lista = servicio.listar(1L, VENCIDO);

        assertEquals(1, lista.size());
        assertEquals("B", lista.get(0).folio());
    }

    @Test
    void obtenerDevuelveElCompromiso() {
        existe(comp(1L, "A", PENDIENTE, HOY));
        CompromisoResponse r = servicio.obtener("A");
        assertEquals("A", r.folio());
        assertEquals(10000, r.montoAPagar());
    }

    @Test
    void obtenerInexistenteLanza404() {
        when(compromisos.findByFolio("X")).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener("X"));
    }

    private CompromisoRequest request(String folio, LocalDate venc, Integer recargo, String cuota) {
        return new CompromisoRequest(folio, 1L, "1-9", "Ana", " Enel ", " Luz ", 10000, venc, recargo, cuota);
    }

    @Test
    void crearConFolioRepetidoLanzaConflicto() {
        when(compromisos.existsByFolio("A")).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(request("A", HOY, 0, "")));
        verify(compromisos, never()).save(any());
    }

    @Test
    void crearConFechaFuturaQuedaPendienteYAplicaValoresPorDefecto() {
        when(compromisos.save(any(Compromiso.class))).thenAnswer(inv -> inv.getArgument(0));

        CompromisoResponse r = servicio.crear(request("A", HOY.plusDays(3), null, null));

        assertEquals(PENDIENTE, r.estado());
        assertEquals(0, r.recargo());
        assertEquals("", r.numeroCuota());
        assertEquals("Enel", r.empresa());
        assertEquals("Luz", r.concepto());
    }

    @Test
    void crearConFechaPasadaQuedaVencido() {
        when(compromisos.save(any(Compromiso.class))).thenAnswer(inv -> inv.getArgument(0));

        CompromisoResponse r = servicio.crear(request("A", HOY.minusDays(1), 500, "1/3"));

        assertEquals(VENCIDO, r.estado());
        assertEquals(10500, r.montoAPagar());
        assertEquals("1/3", r.numeroCuota());
    }

    @Test
    void eliminarCompromisoAbiertoSinPago() {
        Compromiso c = comp(1L, "A", PENDIENTE, HOY);
        existe(c);
        when(pagos.existsByFolio("A")).thenReturn(false);

        servicio.eliminar("A");

        verify(compromisos).delete(c);
    }

    @Test
    void noEliminaCompromisoPagado() {
        existe(comp(1L, "A", PAGADO, HOY));
        assertThrows(ReglaNegocioException.class, () -> servicio.eliminar("A"));
        verify(compromisos, never()).delete(any());
    }

    @Test
    void noEliminaCompromisoConPagoAsociado() {
        existe(comp(1L, "A", PENDIENTE, HOY));
        when(pagos.existsByFolio("A")).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> servicio.eliminar("A"));
    }

    @Test
    void pagarVencidoCobraRecargoYGeneraComprobante() {
        Compromiso c = comp(5L, "A", VENCIDO, HOY.minusDays(2));
        existe(c);
        guardaPagoConId();

        ComprobanteResponse r = servicio.pagar("A", new PagoRequest(" Tarjeta ", null));

        assertEquals("CPR-000007", r.numero());
        assertEquals(10500, r.montoPagado());
        assertEquals("App", r.canal());
        assertEquals("Tarjeta", r.medioPago());
        assertEquals(PAGADO, c.getEstado());
        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagos).saveAndFlush(captor.capture());
        assertEquals("A", captor.getValue().getFolio());
    }

    @Test
    void pagarPendienteNoCobraRecargoYUsaElCanalIndicado() {
        existe(comp(5L, "A", PENDIENTE, HOY));
        guardaPagoConId();

        ComprobanteResponse r = servicio.pagar("A", new PagoRequest("Efectivo", "Caja"));

        assertEquals(10000, r.montoPagado());
        assertEquals("Caja", r.canal());
    }

    @Test
    void pagarConCanalEnBlancoUsaApp() {
        existe(comp(5L, "A", PENDIENTE, HOY));
        guardaPagoConId();
        assertEquals("App", servicio.pagar("A", new PagoRequest("Efectivo", "  ")).canal());
    }

    @Test
    void noPermitePagarSiElEstadoNoEsAbierto() {
        existe(comp(5L, "A", EN_REVISION, HOY));
        assertThrows(ReglaNegocioException.class, () -> servicio.pagar("A", new PagoRequest("Efectivo", null)));
        verify(pagos, never()).saveAndFlush(any());
    }

    @Test
    void noPermitePagarSiHayCuotaAnteriorSinPagar() {
        existe(comp(5L, "A", PENDIENTE, HOY));
        when(compromisos.existeCuotaAnteriorAbierta(any(), any(), any(), any(), any())).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> servicio.pagar("A", new PagoRequest("Efectivo", null)));
        verify(pagos, never()).saveAndFlush(any());
    }

    private Pago pago() {
        return Pago.builder().id(1L).numeroComprobante("CPR-000001").folio("A").empresa("Enel").concepto("Luz")
                .montoPagado(10000).fechaHora(HOY.atStartOfDay()).canal("App").medioPago("Efectivo").build();
    }

    @Test
    void comprobantePorFolio() {
        when(pagos.findByFolio("A")).thenReturn(Optional.of(pago()));
        assertEquals("CPR-000001", servicio.comprobantePorFolio("A").numero());
    }

    @Test
    void comprobantePorFolioInexistente() {
        when(pagos.findByFolio("A")).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.comprobantePorFolio("A"));
    }

    @Test
    void comprobantePorNumero() {
        when(pagos.findByNumeroComprobante("CPR-000001")).thenReturn(Optional.of(pago()));
        assertEquals("A", servicio.comprobantePorNumero("CPR-000001").folioCompromiso());
    }

    @Test
    void comprobantePorNumeroInexistente() {
        when(pagos.findByNumeroComprobante("CPR-9")).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.comprobantePorNumero("CPR-9"));
    }

    @Test
    void historialDeUnUsuario() {
        when(pagos.findByUsuario(1L)).thenReturn(List.of(pago()));
        assertEquals(1, servicio.comprobantesDeUsuario(1L).size());
    }

    // ------------------------------------------------------------ revisión (Reportes)

    @Test
    void marcarEnRevisionDesdeVencido() {
        Compromiso c = comp(1L, "A", VENCIDO, HOY.minusDays(1));
        existe(c);
        assertEquals(EN_REVISION, servicio.marcarEnRevision("A").estado());
    }

    @Test
    void noMarcaEnRevisionUnCompromisoPagado() {
        existe(comp(1L, "A", PAGADO, HOY));
        assertThrows(ReglaNegocioException.class, () -> servicio.marcarEnRevision("A"));
    }

    @Test
    void resolverRevisionComoAnulado() {
        Compromiso c = comp(1L, "A", EN_REVISION, HOY);
        existe(c);
        assertEquals(ANULADO, servicio.resolverRevision("A", ANULADO).estado());
    }

    @Test
    void resolverRevisionComoPendiente() {
        existe(comp(1L, "A", EN_REVISION, HOY));
        assertEquals(PENDIENTE, servicio.resolverRevision("A", PENDIENTE).estado());
    }

    @Test
    void resolverRevisionConEstadoInvalido() {
        assertThrows(ReglaNegocioException.class, () -> servicio.resolverRevision("A", PAGADO));
    }

    @Test
    void resolverRevisionDeUnCompromisoQueNoEstaEnRevision() {
        existe(comp(1L, "A", PENDIENTE, HOY));
        assertThrows(ReglaNegocioException.class, () -> servicio.resolverRevision("A", ANULADO));
    }
}
