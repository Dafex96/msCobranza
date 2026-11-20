package cl.duoc.msCobranza.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;

public enum EstadoCompromiso {
    PENDIENTE("Pendiente"),
    VENCIDO("Vencido"),
    EN_REVISION("En revisión"),
    PAGADO("Pagado"),
    ANULADO("Anulado");

    private final String etiqueta;

    EstadoCompromiso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    @JsonCreator
    public static EstadoCompromiso desde(String valor) {
        if (valor == null) {
            return null;
        }
        String buscado = normalizar(valor);
        for (EstadoCompromiso e : values()) {
            if (normalizar(e.etiqueta).equals(buscado) || normalizar(e.name()).equals(buscado)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de compromiso no válido: " + valor);
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().replace('_', ' ').toLowerCase();
    }
}
