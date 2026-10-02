package com.pedidos360.repository;

import com.pedidos360.domain.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByOrderId(Long orderId);

    Optional<Pago> findByTransaccionId(String transaccionId);

    List<Pago> findAllByOrderByFechaPagoDesc();
}
