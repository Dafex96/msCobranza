package cl.duoc.msCobranza.repository;

import cl.duoc.msCobranza.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByFolio(String folio);

    Optional<Pago> findByNumeroComprobante(String numeroComprobante);

    boolean existsByFolio(String folio);

    @Query("select p from Pago p where p.folio in " +
            "(select c.folio from Compromiso c where c.usuarioId = :usuarioId) " +
            "order by p.fechaHora desc")
    List<Pago> findByUsuario(@Param("usuarioId") Long usuarioId);
}
