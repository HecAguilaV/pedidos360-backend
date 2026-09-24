package com.pedidos360.repository;

import com.pedidos360.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByCodigo(String codigo);
    Optional<Producto> findByNombreIgnoreCase(String nombre);
}
