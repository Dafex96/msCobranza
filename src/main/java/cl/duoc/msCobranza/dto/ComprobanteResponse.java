package cl.duoc.msCobranza.dto;

import cl.duoc.msCobranza.model.Pago;

import java.time.LocalDateTime;

public record ComprobanteResponse(
        String numero,
        String folioCompromiso,
        String empresa,
        String concepto,
        Integer montoPagado,
        LocalDateTime fechaHora,
        String canal,
        String medioPago
) {
    public static ComprobanteResponse desde(Pago p) {
        return new ComprobanteResponse(p.getNumeroComprobante(), p.getFolio(), p.getEmpresa(), p.getConcepto(),
                p.getMontoPagado(), p.getFechaHora(), p.getCanal(), p.getMedioPago());
    }
}
