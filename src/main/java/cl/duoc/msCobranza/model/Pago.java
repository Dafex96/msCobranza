package cl.duoc.msCobranza.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long id;

    @Column(name = "numero_comprobante", unique = true, length = 20)
    private String numeroComprobante;

    @Column(nullable = false, unique = true, updatable = false, length = 40)
    private String folio;

    @Column(nullable = false, updatable = false, length = 120)
    private String empresa;

    @Column(nullable = false, updatable = false, length = 200)
    private String concepto;

    @Column(name = "monto_pagado", nullable = false, updatable = false)
    private Integer montoPagado;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, updatable = false, length = 30)
    private String canal;

    @Column(name = "medio_pago", nullable = false, updatable = false, length = 50)
    private String medioPago;
}
