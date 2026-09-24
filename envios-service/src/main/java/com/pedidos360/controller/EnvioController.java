package com.pedidos360.controller;

import com.pedidos360.controller.dto.UpdateEnvioStatusRequest;
import com.pedidos360.domain.Envio;
import com.pedidos360.repository.EnvioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/envios")
public class EnvioController {

    private final EnvioRepository envioRepository;

    public EnvioController(EnvioRepository envioRepository) {
        this.envioRepository = envioRepository;
    }

    @GetMapping
    public ResponseEntity<List<Envio>> listarEnvios() {
        return ResponseEntity.ok(envioRepository.findAllByOrderByFechaDespachoDesc());
    }

    @GetMapping("/tracking/{numeroGuia}")
    public ResponseEntity<Envio> obtenerPorTracking(@PathVariable String numeroGuia) {
        return envioRepository.findByNumeroGuia(numeroGuia)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guia de seguimiento no encontrada: " + numeroGuia));
    }

    @GetMapping("/orden/{orderId}")
    public ResponseEntity<Envio> obtenerPorOrden(@PathVariable Long orderId) {
        return envioRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe despacho registrado para la orden #" + orderId));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Envio> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEnvioStatusRequest request) {

        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Envio no encontrado con ID: " + id));

        envio.setEstado(request.estado());
        Envio actualizado = envioRepository.save(envio);

        return ResponseEntity.ok(actualizado);
    }
}
