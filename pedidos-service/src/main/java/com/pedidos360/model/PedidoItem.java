package com.pedidos360.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class PedidoItem {

    @JsonAlias({"productId", "codigoProducto"})
    @Column(name = "product_code", length = 80)
    private String productCode;

    @JsonAlias({"nombre", "nombreProducto"})
    @Column(name = "product_name", length = 150)
    private String productName;

    @JsonAlias({"cantidad"})
    @Column(name = "quantity")
    private Integer quantity;

    @JsonAlias({"precio", "precioUnitario"})
    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public PedidoItem() {
    }

    public PedidoItem(String productCode, String productName, Integer quantity, BigDecimal unitPrice) {
        this.productCode = productCode;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
