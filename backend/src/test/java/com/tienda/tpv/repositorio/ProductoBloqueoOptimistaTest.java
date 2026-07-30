package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.UnidadMedida;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba que el bloqueo optimista (@Version en Producto) evita que una venta o
 * movimiento de stock concurrente sobrescriba silenciosamente un cambio de stock
 * más reciente. Deliberadamente NO usa @Transactional de clase: hacen falta
 * varias transacciones reales confirmadas por separado para reproducir el conflicto,
 * así que limpiamos la fila creada a mano en @AfterEach.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductoBloqueoOptimistaTest {

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long productoId;

    @AfterEach
    void limpiar() {
        if (productoId != null) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> productoRepositorio.deleteById(productoId));
        }
    }

    @Test
    void guardarUnaCopiaDesactualizadaLanzaBloqueoOptimista() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        productoId = tx.execute(status -> {
            Producto producto = new Producto();
            producto.setNombre("Producto con bloqueo optimista");
            producto.setPrecioVenta(new BigDecimal("1.00"));
            producto.setPrecioCoste(new BigDecimal("0.50"));
            producto.setIvaPorcentaje(new BigDecimal("21"));
            producto.setStockActual(new BigDecimal("10"));
            producto.setUnidadMedida(UnidadMedida.UNIDAD);
            return productoRepositorio.save(producto).getId();
        });

        // Dos "lecturas simultáneas": ambas transacciones ven todavía version=0
        Producto copiaA = tx.execute(status -> productoRepositorio.findById(productoId).orElseThrow());
        Producto copiaB = tx.execute(status -> productoRepositorio.findById(productoId).orElseThrow());

        // La primera transacción en confirmar gana y la versión en BD sube a 1
        tx.executeWithoutResult(status -> {
            copiaA.setStockActual(copiaA.getStockActual().subtract(BigDecimal.ONE));
            productoRepositorio.saveAndFlush(copiaA);
        });

        // La copia desactualizada (todavía version=0) ya no puede guardarse
        copiaB.setStockActual(copiaB.getStockActual().subtract(BigDecimal.ONE));
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> productoRepositorio.saveAndFlush(copiaB)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
