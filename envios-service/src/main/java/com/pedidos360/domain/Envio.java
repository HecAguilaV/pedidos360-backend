package com.pedidos360.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "envios", indexes = {
    @Index(name = "idx_envio_order_id", columnList = "orderId"),
    @Index(name = "idx_envio_guia", columnList = "numeroGuia", unique = true)
})
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long orderId;

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String numeroGuia;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String destinatario;

    @Column(length = 150)
    private String destinatarioEmail;

    @Column(length = 200)
    private String direccionEntrega;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String estado;

    @Column(length = 80)
    private String empresaTransporte;

    @Column(nullable = false)
    private LocalDateTime fechaDespacho;

    @Column(nullable = false)
    private LocalDateTime fechaEstimadaEntrega;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Envio() {
    }

    public Envio(Long orderId, String numeroGuia, String destinatario, String destinatarioEmail, String direccionEntrega, String estado, String empresaTransporte, LocalDateTime fechaDespacho, LocalDateTime fechaEstimadaEntrega) {
        this.orderId = orderId;
        this.numeroGuia = numeroGuia;
        this.destinatario = destinatario;
        this.destinatarioEmail = destinatarioEmail;
        this.direccionEntrega = direccionEntrega;
        this.estado = estado;
        this.empresaTransporte = empresaTransporte;
        this.fechaDespacho = fechaDespacho;
        this.fechaEstimadaEntrega = fechaEstimadaEntrega;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.fechaDespacho == null) {
            this.fechaDespacho = LocalDateTime.now();
        }
        if (this.fechaEstimadaEntrega == null) {
            this.fechaEstimadaEntrega = LocalDateTime.now().plusDays(2);
        }
        if (this.estado == null || this.estado.isBlank()) {
            this.estado = "PREPARANDO_PAQUETE";
        }
        if (this.empresaTransporte == null || this.empresaTransporte.isBlank()) {
            this.empresaTransporte = "Pedidos360 Express";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getDestinatarioEmail() {
        return destinatarioEmail;
    }

    public void setDestinatarioEmail(String destinatarioEmail) {
        this.destinatarioEmail = destinatarioEmail;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEmpresaTransporte() {
        return empresaTransporte;
    }

    public void setEmpresaTransporte(String empresaTransporte) {
        this.empresaTransporte = empresaTransporte;
    }

    public LocalDateTime getFechaDespacho() {
        return fechaDespacho;
    }

    public void setFechaDespacho(LocalDateTime fechaDespacho) {
        this.fechaDespacho = fechaDespacho;
    }

    public LocalDateTime getFechaEstimadaEntrega() {
        return fechaEstimadaEntrega;
    }

    public void setFechaEstimadaEntrega(LocalDateTime fechaEstimadaEntrega) {
        this.fechaEstimadaEntrega = fechaEstimadaEntrega;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
