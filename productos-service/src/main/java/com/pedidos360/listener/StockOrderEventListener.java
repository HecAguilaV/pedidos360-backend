package com.pedidos360.listener;

import com.pedidos360.config.RabbitMQConfig;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.model.ProcessedOrderStockEvent;
import com.pedidos360.model.Producto;
import com.pedidos360.repository.ProcessedOrderStockEventRepository;
import com.pedidos360.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class StockOrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockOrderEventListener.class);
    private final ProductoRepository productoRepository;
    private final ProcessedOrderStockEventRepository processedOrderRepository;

    public StockOrderEventListener(
            ProductoRepository productoRepository,
            ProcessedOrderStockEventRepository processedOrderRepository) {
        this.productoRepository = productoRepository;
        this.processedOrderRepository = processedOrderRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_QUEUE)
    @Transactional
    public void onOrderCreated(OrdenCreadaEvent event) {
        log.info("==> [RabbitMQ Consumer] Evento 'orden.creada' recibido en stock-decrement. Orden ID: {}, Cliente: {}",
                event.orderId(), event.cliente());

        if (event.orderId() != null && processedOrderRepository.existsByOrderId(event.orderId())) {
            log.info("La orden ID: {} ya tuvo descuento de stock procesado previamente (idempotencia). Omitiendo duplicado.",
                    event.orderId());
            return;
        }

        if (event.items() == null || event.items().isEmpty()) {
            log.warn("La orden ID: {} no contiene items para descontar stock.", event.orderId());
            return;
        }

        for (OrdenCreadaEvent.ItemOrdenEvent item : event.items()) {
            Optional<Producto> productoOpt = findProduct(item);

            if (productoOpt.isPresent()) {
                Producto producto = productoOpt.get();
                int cantidad = (item.quantity() != null && item.quantity() > 0) ? item.quantity() : 1;
                int stockAnterior = producto.getStock();
                int nuevoStock = Math.max(0, stockAnterior - cantidad);
                producto.setStock(nuevoStock);
                productoRepository.save(producto);

                log.info("STOCK DESCONTADO [Producto: '{}', Codigo: '{}']: Stock previo = {}, Cantidad comprada = {}, Nuevo stock = {}",
                        producto.getNombre(), producto.getCodigo(), stockAnterior, cantidad, nuevoStock);
            } else {
                log.warn("No se encontro producto para item: '{}' (codigo: '{}'). No se pudo descontar stock.",
                        item.productName(), item.productCode());
            }
        }

        if (event.orderId() != null) {
            processedOrderRepository.save(new ProcessedOrderStockEvent(event.orderId(), LocalDateTime.now()));
        }
    }

    private Optional<Producto> findProduct(OrdenCreadaEvent.ItemOrdenEvent item) {
        if (item.productCode() != null && !item.productCode().isBlank()) {
            Optional<Producto> byCodigo = productoRepository.findByCodigo(item.productCode().trim());
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

        // Si el codigo contiene un ID numerico (ej. PROD-002 o SKU-P360-2)
        if (item.productCode() != null) {
            try {
                String normalized = item.productCode().replace("SKU-P360-", "");
                Long id = Long.parseLong(normalized.replaceAll("\\D+", ""));
                return productoRepository.findById(id);
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}


