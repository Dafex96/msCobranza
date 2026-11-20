package cl.duoc.msCobranza.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "compromisos", indexes = {
        @Index(name = "idx_compromisos_usuario", columnList = "usuario_id"),
        @Index(name = "idx_compromisos_estado_venc", columnList = "estado, vencimiento")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compromiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compromiso")
    private Long id;

    @Column(nullable = false, unique = true, length = 40, updatable = false)
    private String folio;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "rut_usuario", length = 15)
    private String rutUsuario;

    @Column(name = "nombre_usuario", length = 120)
    private String nombreUsuario;

    @Column(nullable = false, length = 120)
    private String empresa;

    @Column(nullable = false, length = 200)
    private String concepto;

    @Column(nullable = false)
    private Integer monto;

    @Column(nullable = false)
    private LocalDate vencimiento;

    @Column(nullable = false, length = 20)
    private EstadoCompromiso estado;

    @Column(nullable = false)
    @Builder.Default
    private Integer recargo = 0;

    @Column(name = "numero_cuota", nullable = false, length = 20)
    @Builder.Default
    private String numeroCuota = "";

    public int montoAPagar() {
        return monto + (estado == EstadoCompromiso.VENCIDO ? recargo : 0);
    }
}
