package com.pedidos360.repository;

import com.pedidos360.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteContainingIgnoreCase(String cliente);
    List<Pedido> findByEstado(String estado);
}
