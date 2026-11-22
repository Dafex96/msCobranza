package cl.duoc.msCobranza.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void noEncontradoEs404() {
        ProblemDetail p = handler.noEncontrado(new RecursoNoEncontradoException("no existe"));
        assertEquals(404, p.getStatus());
        assertEquals("no existe", p.getDetail());
    }

    @Test
    void reglaDeNegocioEs409() {
        ProblemDetail p = handler.reglaNegocio(new ReglaNegocioException("no permitido"));
        assertEquals(409, p.getStatus());
        assertEquals("no permitido", p.getDetail());
    }

    @Test
    void argumentoInvalidoEs400() {
        ProblemDetail p = handler.argumentoInvalido(new IllegalArgumentException("malo"));
        assertEquals(400, p.getStatus());
        assertEquals("malo", p.getDetail());
    }
}
