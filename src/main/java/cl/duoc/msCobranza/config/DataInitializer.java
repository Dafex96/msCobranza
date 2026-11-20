package cl.duoc.msCobranza.config;

import cl.duoc.msCobranza.model.Compromiso;
import cl.duoc.msCobranza.model.EstadoCompromiso;
import cl.duoc.msCobranza.repository.CompromisoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cobranza.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final long USUARIO_ID = 1L;
    private static final String RUT = "20.435.214-5";
    private static final String NOMBRE = "Sofia Perez";

    private final CompromisoRepository compromisos;
    private final Clock clock;

    @Override
    public void run(String... args) {
        if (compromisos.count() > 0) {
            return;
        }
        LocalDate hoy = LocalDate.now(clock);
        compromisos.saveAll(List.of(
                nuevo("PF-UNI-001", "Duoc Uc", "Mensualidad", 356000, hoy.minusDays(1), 7000),
                nuevo("PF-LUZ-001", "Enel", "Luz", 28000, hoy.plusDays(5), 1100),
                nuevo("PF-COND-001", "Condominio", "Gasto común", 45000, hoy.plusDays(20), 3000),
                nuevo("PF-AGUA-001", "Aguas Andinas", "Agua", 35000, hoy.plusDays(30), 2000)
        ));
        log.info("Datos de prueba de cobranza cargados: {} compromisos", compromisos.count());
    }

    private Compromiso nuevo(String folio, String empresa, String concepto, int monto, LocalDate vencimiento, int recargo) {
        return Compromiso.builder()
                .folio(folio).usuarioId(USUARIO_ID).rutUsuario(RUT).nombreUsuario(NOMBRE)
                .empresa(empresa).concepto(concepto).monto(monto).vencimiento(vencimiento)
                .estado(EstadoCompromiso.PENDIENTE).recargo(recargo).numeroCuota("")
                .build();
    }
}
