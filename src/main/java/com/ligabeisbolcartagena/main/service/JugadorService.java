package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.JugadorDto;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.JugadorRequest;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.JugadorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JugadorService {

    private final JugadorRepository jugadores;
    private final Mapeador mapeador;

    public JugadorService(JugadorRepository jugadores, Mapeador mapeador) {
        this.jugadores = jugadores;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<JugadorDto> listar() {
        return jugadores.findAllByOrderByApellidosAscNombresAsc().stream().map(mapeador::dto).toList();
    }

    @Transactional(readOnly = true)
    public JugadorDto obtener(Long id) {
        return mapeador.dto(buscar(id));
    }

    @Transactional
    public JugadorDto crear(JugadorRequest req) {
        Jugador j = new Jugador();
        aplicar(j, req);
        return mapeador.dto(jugadores.save(j));
    }

    @Transactional
    public JugadorDto actualizar(Long id, JugadorRequest req) {
        Jugador j = buscar(id);
        aplicar(j, req);
        return mapeador.dto(jugadores.save(j));
    }

    @Transactional
    public void eliminar(Long id) {
        Jugador j = buscar(id);
        try {
            jugadores.delete(j);
            jugadores.flush();
        } catch (DataIntegrityViolationException e) {
            throw Errores.relacionado();
        }
    }

    Jugador buscar(Long id) {
        return jugadores.findById(id).orElseThrow(() -> ApiException.noEncontrado("Jugador", String.valueOf(id)));
    }

    private static void aplicar(Jugador j, JugadorRequest req) {
        j.setNombres(req.nombres().trim());
        j.setApellidos(req.apellidos().trim());
        j.setFechaNacimiento(req.fechaNacimiento());
        j.setBatea(req.batea());
        j.setLanza(req.lanza());
        j.setFotoUrl(req.fotoUrl());
    }
}
