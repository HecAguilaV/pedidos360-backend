package com.pedidos360.listener;

import com.pedidos360.config.RabbitMQConfig;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.model.Producto;
import com.pedidos360.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class StockOrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockOrderEventListener.class);
    private final ProductoRepository productoRepository;

    public StockOrderEventListener(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_QUEUE)
    @Transactional
    public void onOrderCreated(OrdenCreadaEvent event) {
        log.info("==> [RabbitMQ Consumer] Evento 'orden.creada' recibido en stock-decrement. Orden ID: {}, Cliente: {}",
                event.orderId(), event.cliente());

        if (event.items() == null || event.items().isEmpty()) {
            log.warn("La orden ID: {} no contiene items para descontar stock.", event.orderId());
            return;
        }

        for (OrdenCreadaEvent.ItemOrdenEvent item : event.items()) {
            Optional<Producto> productoOpt = findProduct(item);

            if (productoOpt.isPresent()) {
                Producto producto = productoOpt.get();
                int stockAnterior = producto.getStock();
                int nuevoStock = Math.max(0, stockAnterior - item.quantity());
                producto.setStock(nuevoStock);
                productoRepository.save(producto);

                log.info("STOCK DESCONTADO [Producto: '{}', Codigo: '{}']: Stock previo = {}, Cantidad comprada = {}, Nuevo stock = {}",
                        producto.getNombre(), producto.getCodigo(), stockAnterior, item.quantity(), nuevoStock);
            } else {
                log.warn("No se encontro producto para item: '{}' (codigo: '{}'). No se pudo descontar stock.",
                        item.productName(), item.productCode());
            }
        }
    }

    private Optional<Producto> findProduct(OrdenCreadaEvent.ItemOrdenEvent item) {
        if (item.productCode() != null && !item.productCode().isBlank()) {
            Optional<Producto> byCodigo = productoRepository.findByCodigo(item.productCode());
            if (byCodigo.isPresent()) {
                return byCodigo;
            }
        }

        if (item.productName() != null && !item.productName().isBlank()) {
            Optional<Producto> byNombre = productoRepository.findByNombreIgnoreCase(item.productName().trim());
            if (byNombre.isPresent()) {
                return byNombre;
            }
        }

        // Si el codigo contiene un ID numerico
        try {
            Long id = Long.parseLong(item.productCode().replaceAll("\\D+", ""));
            return productoRepository.findById(id);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
