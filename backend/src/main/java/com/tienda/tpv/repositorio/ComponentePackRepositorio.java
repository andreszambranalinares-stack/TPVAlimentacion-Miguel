package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.ComponentePack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComponentePackRepositorio extends JpaRepository<ComponentePack, Long> {

    List<ComponentePack> findByPackId(Long packId);

    void deleteByPackId(Long packId);
}
