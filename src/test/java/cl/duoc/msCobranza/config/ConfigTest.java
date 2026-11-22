package cl.duoc.msCobranza.config;

import cl.duoc.msCobranza.model.Compromiso;
import cl.duoc.msCobranza.model.EstadoCompromiso;
import cl.duoc.msCobranza.repository.CompromisoRepository;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigTest {

    @Mock
    CompromisoRepository compromisos;

    @Test
    void relojUsaLaZonaDeChile() {
        assertEquals(ZoneId.of("America/Santiago"), new AppConfig().clock().getZone());
    }

    @Test
    void swaggerTieneTituloYVersion() {
        OpenAPI api = new SwaggerConfig().swaggerInfo();
        assertEquals("PagoFlex - Microservicio de Cobranza", api.getInfo().getTitle());
        assertEquals("1.0.0", api.getInfo().getVersion());
    }

    @Test
    void cargaLosDatosDePruebaSiLaTablaEstaVacia() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("America/Santiago"));
        when(compromisos.count()).thenReturn(0L);

        new DataInitializer(compromisos, reloj).run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Compromiso>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(compromisos).saveAll(captor.capture());
        List<Compromiso> cargados = new ArrayList<>();
        captor.getValue().forEach(cargados::add);

        assertEquals(4, cargados.size());
        assertTrue(cargados.stream().allMatch(c -> c.getEstado() == EstadoCompromiso.PENDIENTE && c.getUsuarioId() == 1L));
        assertTrue(cargados.get(0).getVencimiento().isBefore(LocalDate.of(2026, 11, 22)));
        assertTrue(cargados.get(1).getVencimiento().isAfter(LocalDate.of(2026, 11, 22)));
    }

    @Test
    void noVuelveACargarSiYaHayDatos() {
        when(compromisos.count()).thenReturn(4L);

        new DataInitializer(compromisos, Clock.systemUTC()).run();

        verify(compromisos, never()).saveAll(any());
    }
}
