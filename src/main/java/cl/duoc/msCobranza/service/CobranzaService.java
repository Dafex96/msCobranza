package cl.duoc.msCobranza.service;

import cl.duoc.msCobranza.dto.*;
import cl.duoc.msCobranza.exception.RecursoNoEncontradoException;
import cl.duoc.msCobranza.exception.ReglaNegocioException;
import cl.duoc.msCobranza.model.Compromiso;
import cl.duoc.msCobranza.model.EstadoCompromiso;
import cl.duoc.msCobranza.model.Pago;
import cl.duoc.msCobranza.repository.CompromisoRepository;
import cl.duoc.msCobranza.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cl.duoc.msCobranza.model.EstadoCompromiso.*;

@Service
@RequiredArgsConstructor
public class CobranzaService {

    private static final Set<EstadoCompromiso> ABIERTOS = Set.of(PENDIENTE, VENCIDO);

    private final CompromisoRepository compromisos;
    private final PagoRepository pagos;
    private final Clock clock;

    @Transactional
    public List<CompromisoResponse> listar(Long usuarioId, EstadoCompromiso estado) {
        sincronizarVencidos();
        List<Compromiso> lista = usuarioId == null
                ? compromisos.findAllByOrderByVencimientoAscIdAsc()
                : compromisos.findByUsuarioIdOrderByVencimientoAscIdAsc(usuarioId);
        return lista.stream()
                .filter(c -> estado == null || c.getEstado() == estado)
                .map(CompromisoResponse::desde)
                .toList();
    }

    @Transactional
    public CompromisoResponse obtener(String folio) {
        sincronizarVencidos();
        return CompromisoResponse.desde(buscar(folio));
    }

    @Transactional
    public CompromisoResponse crear(CompromisoRequest req) {
        if (compromisos.existsByFolio(req.folio())) {
            throw new ReglaNegocioException("Ya existe un compromiso con el folio " + req.folio());
        }
        LocalDate hoy = LocalDate.now(clock);
        Compromiso c = Compromiso.builder()
                .folio(req.folio().trim())
                .usuarioId(req.usuarioId())
                .rutUsuario(req.rutUsuario())
                .nombreUsuario(req.nombreUsuario())
                .empresa(req.empresa().trim())
                .concepto(req.concepto().trim())
                .monto(req.monto())
                .vencimiento(req.vencimiento())
                .estado(req.vencimiento().isBefore(hoy) ? VENCIDO : PENDIENTE)
                .recargo(req.recargo() == null ? 0 : req.recargo())
                .numeroCuota(req.numeroCuota() == null ? "" : req.numeroCuota())
                .build();
        return CompromisoResponse.desde(compromisos.save(c));
    }

    @Transactional
    public void eliminar(String folio) {
        sincronizarVencidos();
        Compromiso c = buscar(folio);
        if (!ABIERTOS.contains(c.getEstado()) || pagos.existsByFolio(folio)) {
            throw new ReglaNegocioException("El compromiso " + folio
                    + " no se puede eliminar porque está en estado '" + c.getEstado().getEtiqueta() + "'");
        }
        compromisos.delete(c);
    }

    @Transactional
    public ComprobanteResponse pagar(String folio, PagoRequest req) {
        sincronizarVencidos();
        Compromiso c = buscar(folio);

        if (!ABIERTOS.contains(c.getEstado())) {
            throw new ReglaNegocioException("El compromiso " + folio + " está en estado '"
                    + c.getEstado().getEtiqueta() + "' y no admite pago");
        }
        if (compromisos.existeCuotaAnteriorAbierta(c.getUsuarioId(), c.getEmpresa(), c.getVencimiento(), c.getId(), ABIERTOS)) {
            throw new ReglaNegocioException("Debe pagar primero la cuota anterior de " + c.getEmpresa());
        }

        Pago pago = pagos.saveAndFlush(Pago.builder()
                .folio(c.getFolio())
                .empresa(c.getEmpresa())
                .concepto(c.getConcepto())
                .montoPagado(c.montoAPagar())
                .fechaHora(LocalDateTime.now(clock))
                .canal(req.canal() == null || req.canal().isBlank() ? "App" : req.canal().trim())
                .medioPago(req.medioPago().trim())
                .build());
        pago.setNumeroComprobante(String.format("CPR-%06d", pago.getId()));
        c.setEstado(PAGADO);
        return ComprobanteResponse.desde(pago);
    }

    @Transactional(readOnly = true)
    public ComprobanteResponse comprobantePorFolio(String folio) {
        return pagos.findByFolio(folio)
                .map(ComprobanteResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay comprobante para el folio " + folio));
    }

    @Transactional(readOnly = true)
    public ComprobanteResponse comprobantePorNumero(String numero) {
        return pagos.findByNumeroComprobante(numero)
                .map(ComprobanteResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el comprobante " + numero));
    }

    @Transactional(readOnly = true)
    public List<ComprobanteResponse> comprobantesDeUsuario(Long usuarioId) {
        return pagos.findByUsuario(usuarioId).stream().map(ComprobanteResponse::desde).toList();
    }

    @Transactional
    public CompromisoResponse marcarEnRevision(String folio) {
        sincronizarVencidos();
        Compromiso c = buscar(folio);
        if (!ABIERTOS.contains(c.getEstado())) {
            throw new ReglaNegocioException("Solo un compromiso Pendiente o Vencido puede pasar a revisión; "
                    + folio + " está en '" + c.getEstado().getEtiqueta() + "'");
        }
        c.setEstado(EN_REVISION);
        return CompromisoResponse.desde(c);
    }

    @Transactional
    public CompromisoResponse resolverRevision(String folio, EstadoCompromiso nuevoEstado) {
        if (nuevoEstado != PENDIENTE && nuevoEstado != ANULADO) {
            throw new ReglaNegocioException("Una revisión solo puede resolverse como 'Pendiente' o 'Anulado'");
        }
        sincronizarVencidos();
        Compromiso c = buscar(folio);
        if (c.getEstado() != EN_REVISION) {
            throw new ReglaNegocioException("El compromiso " + folio + " no está en revisión");
        }
        c.setEstado(nuevoEstado);
        sincronizarVencidos();
        return CompromisoResponse.desde(buscar(folio));
    }

    private Compromiso buscar(String folio) {
        return compromisos.findByFolio(folio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el compromiso con folio " + folio));
    }

    private void sincronizarVencidos() {
        compromisos.marcarVencidos(LocalDate.now(clock), PENDIENTE, VENCIDO);
    }
}
