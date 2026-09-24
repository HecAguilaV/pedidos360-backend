package com.pedidos360.repository;

import com.pedidos360.domain.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Long> {

    Optional<Envio> findByNumeroGuia(String numeroGuia);

    Optional<Envio> findByOrderId(Long orderId);

    List<Envio> findAllByOrderByFechaDespachoDesc();
}
