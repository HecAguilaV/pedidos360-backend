package com.pedidos360.service;

import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.model.Pedido;
import com.pedidos360.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

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
        List<OrdenCreadaEvent.ItemOrdenEvent> mappedItems = (pedido.getItems() != null && !pedido.getItems().isEmpty())
                ? pedido.getItems().stream()
                    .map(item -> new OrdenCreadaEvent.ItemOrdenEvent(
                            item.getProductCode(),
                            item.getProductName(),
                            item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1,
                            item.getUnitPrice() != null ? item.getUnitPrice() : pedido.getTotal()
                    ))
                    .toList()
                : List.of();
        return createWithItems(pedido, mappedItems);
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

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        orderEventPublisher.publishOrderCreated(event);
                    } catch (Exception e) {
                        log.error("Orden #{} ya confirmada en BD, pero fallo la publicacion post-commit de 'orden.creada': {}",
                                saved.getId(), e.getMessage(), e);
                    }
                }
            });
        } else {
            orderEventPublisher.publishOrderCreated(event);
        }

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
