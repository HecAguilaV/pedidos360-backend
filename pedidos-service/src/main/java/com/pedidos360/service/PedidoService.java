package com.pedidos360.service;

import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.model.Pedido;
import com.pedidos360.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final OrderEventPublisher orderEventPublisher;

    public PedidoService(PedidoRepository pedidoRepository, OrderEventPublisher orderEventPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Pedido> findAll() {
        return pedidoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Pedido> findById(Long id) {
        return pedidoRepository.findById(id);
    }

    public Pedido create(Pedido pedido) {
        return createWithItems(pedido, List.of(
            new OrdenCreadaEvent.ItemOrdenEvent("PROD-001", "Laptop Pro Gamer", 1, pedido.getTotal())
        ));
    }

    public Pedido createWithItems(Pedido pedido, List<OrdenCreadaEvent.ItemOrdenEvent> items) {
        pedido.setId(null);
        Pedido saved = pedidoRepository.save(pedido);

        String email = saved.getClienteEmail() != null && !saved.getClienteEmail().isBlank()
            ? saved.getClienteEmail()
            : "cliente@pedidos360.cl";

        OrdenCreadaEvent event = new OrdenCreadaEvent(
            saved.getId(),
            saved.getCliente(),
            email,
            saved.getTotal(),
            saved.getEstado(),
            items != null && !items.isEmpty() ? items : List.of(),
            saved.getFecha() != null ? saved.getFecha() : LocalDateTime.now()
        );

        orderEventPublisher.publishOrderCreated(event);

        return saved;
    }

    public boolean deleteById(Long id) {
        if (pedidoRepository.existsById(id)) {
            pedidoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
