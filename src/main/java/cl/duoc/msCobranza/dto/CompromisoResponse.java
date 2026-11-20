package cl.duoc.msCobranza.dto;

import cl.duoc.msCobranza.model.Compromiso;
import cl.duoc.msCobranza.model.EstadoCompromiso;

import java.time.LocalDate;

public record CompromisoResponse(
        String folio,
        Long usuarioId,
        String rutUsuario,
        String nombreUsuario,
        String empresa,
        String concepto,
        Integer monto,
        LocalDate vencimiento,
        EstadoCompromiso estado,
        Integer recargo,
        String numeroCuota,
        int montoAPagar
) {
    public static CompromisoResponse desde(Compromiso c) {
        return new CompromisoResponse(c.getFolio(), c.getUsuarioId(), c.getRutUsuario(), c.getNombreUsuario(),
                c.getEmpresa(), c.getConcepto(), c.getMonto(), c.getVencimiento(), c.getEstado(),
                c.getRecargo(), c.getNumeroCuota(), c.montoAPagar());
    }
}
