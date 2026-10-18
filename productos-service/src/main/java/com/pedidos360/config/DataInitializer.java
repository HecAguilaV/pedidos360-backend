package com.pedidos360.config;

import com.pedidos360.model.Producto;
import com.pedidos360.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initProductos(ProductoRepository productoRepository) {
        return args -> {
            if (productoRepository.count() == 0) {
                log.info("Inicializando catalogo de productos en CLP...");
                productoRepository.saveAll(List.of(
                    new Producto("PROD-001", "Laptop Pro 16", "Notebook profesional 16 pulgadas, 32GB RAM, SSD 1TB NVMe", new BigDecimal("1499990"), 15),
                    new Producto("PROD-002", "Mouse Inalámbrico", "Mouse ergonómico inalámbrico con sensor óptico de precisión", new BigDecimal("29990"), 50),
                    new Producto("PROD-003", "Teclado Mecánico", "Teclado mecánico compacto con switches lineales silenciosos", new BigDecimal("89990"), 30),
                    new Producto("PROD-004", "Monitor 27 UHD", "Monitor 27 pulgadas resolución 4K IPS 144Hz USB-C", new BigDecimal("399990"), 20)
                ));
                log.info("4 productos iniciales en CLP cargados en la base de datos.");
            } else {
                productoRepository.findAll().forEach(producto -> {
                    boolean changed = false;
                    if (producto.getCodigo() == null || producto.getCodigo().isBlank()) {
                        producto.setCodigo(String.format("PROD-%03d", producto.getId()));
                        changed = true;
                    }
                    if (producto.getPrecio() != null && producto.getPrecio().compareTo(new BigDecimal("2000")) < 0) {
                        BigDecimal clpPrice = switch (producto.getCodigo()) {
                            case "PROD-001" -> new BigDecimal("1499990");
                            case "PROD-002" -> new BigDecimal("29990");
                            case "PROD-003" -> new BigDecimal("89990");
                            case "PROD-004" -> new BigDecimal("399990");
                            default -> producto.getPrecio().multiply(new BigDecimal("1000")).setScale(0, java.math.RoundingMode.HALF_UP);
                        };
                        producto.setPrecio(clpPrice);
                        changed = true;
                    }
                    if (changed) {
                        productoRepository.save(producto);
                    }
                });
            }
        };
    }
}
