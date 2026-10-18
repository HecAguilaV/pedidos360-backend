package com.pedidos360.service;

import com.pedidos360.model.Producto;
import com.pedidos360.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<Producto> findAll() {
        return productoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Producto> findById(Long id) {
        return productoRepository.findById(id);
    }

    public Producto create(Producto producto) {
        producto.setId(null);
        Producto saved = productoRepository.save(producto);
        if (saved.getCodigo() == null || saved.getCodigo().isBlank()) {
            saved.setCodigo(String.format("PROD-%03d", saved.getId()));
            saved = productoRepository.save(saved);
        }
        return saved;
    }


    public boolean deleteById(Long id) {
        if (productoRepository.existsById(id)) {
            productoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
