package cl.duoc.msCobranza.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoCompromisoConverter implements AttributeConverter<EstadoCompromiso, String> {

    @Override
    public String convertToDatabaseColumn(EstadoCompromiso estado) {
        return estado == null ? null : estado.getEtiqueta();
    }

    @Override
    public EstadoCompromiso convertToEntityAttribute(String valor) {
        return valor == null ? null : EstadoCompromiso.desde(valor);
    }
}
