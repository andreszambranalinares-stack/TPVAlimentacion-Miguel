package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.EstadoPedido;
import com.tienda.tpv.dominio.PedidoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoProveedorRepositorio extends JpaRepository<PedidoProveedor, Long> {

    List<PedidoProveedor> findByEstadoOrderByFechaHoraDesc(EstadoPedido estado);

    List<PedidoProveedor> findByProveedorIdOrderByFechaHoraDesc(Long proveedorId);

    List<PedidoProveedor> findAllByOrderByFechaHoraDesc();
}
