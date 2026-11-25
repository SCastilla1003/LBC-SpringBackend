package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    /** Lo más reciente primero; [entidad] y [texto] (en minúsculas) son opcionales. */
    @Query("select r from RegistroAuditoria r where (:entidad is null or r.entidad = :entidad) "
            + "and (:texto is null or lower(r.descripcion) like concat('%', :texto, '%') "
            + "or lower(r.usuario) like concat('%', :texto, '%')) order by r.id desc")
    List<RegistroAuditoria> buscar(@Param("entidad") String entidad, @Param("texto") String texto, Pageable pagina);

    @Query("select count(r) from RegistroAuditoria r where (:entidad is null or r.entidad = :entidad) "
            + "and (:texto is null or lower(r.descripcion) like concat('%', :texto, '%') "
            + "or lower(r.usuario) like concat('%', :texto, '%'))")
    long contar(@Param("entidad") String entidad, @Param("texto") String texto);
}
