package cl.duoc.msCobranza.repository;

import cl.duoc.msCobranza.model.Compromiso;
import cl.duoc.msCobranza.model.EstadoCompromiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompromisoRepository extends JpaRepository<Compromiso, Long> {

    Optional<Compromiso> findByFolio(String folio);

    boolean existsByFolio(String folio);

    List<Compromiso> findAllByOrderByVencimientoAscIdAsc();

    List<Compromiso> findByUsuarioIdOrderByVencimientoAscIdAsc(Long usuarioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Compromiso c set c.estado = :vencido " +
            "where c.estado = :pendiente and c.vencimiento < :hoy")
    int marcarVencidos(@Param("hoy") LocalDate hoy,
                       @Param("pendiente") EstadoCompromiso pendiente,
                       @Param("vencido") EstadoCompromiso vencido);

    @Query("select count(a) > 0 from Compromiso a " +
            "where a.usuarioId = :usuarioId and a.empresa = :empresa and a.estado in :abiertos " +
            "and (a.vencimiento < :vencimiento or (a.vencimiento = :vencimiento and a.id < :id))")
    boolean existeCuotaAnteriorAbierta(@Param("usuarioId") Long usuarioId,
                                       @Param("empresa") String empresa,
                                       @Param("vencimiento") LocalDate vencimiento,
                                       @Param("id") Long id,
                                       @Param("abiertos") Collection<EstadoCompromiso> abiertos);
}
