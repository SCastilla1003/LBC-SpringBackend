package com.ligabeisbolcartagena.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.JugadorRequest;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.JugadorRepository;
import com.ligabeisbolcartagena.main.dominio.RegistroAuditoria;
import com.ligabeisbolcartagena.main.dominio.RegistroAuditoriaRepository;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditoriaServiceTest {

    RegistroAuditoriaRepository registros = mock(RegistroAuditoriaRepository.class);
    UsuarioCuentaRepository usuarios = mock(UsuarioCuentaRepository.class);
    AuditoriaService auditoria = new AuditoriaService(registros, usuarios, new ObjectMapper().findAndRegisterModules());
    UsuarioCuenta admin = new UsuarioCuenta();

    @BeforeEach
    void sesion() {
        admin.setUsername("santiago");
        when(usuarios.findByUsername("santiago")).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "santiago", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    RegistroAuditoria guardado() {
        ArgumentCaptor<RegistroAuditoria> r = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(registros).save(r.capture());
        return r.getValue();
    }

    @Test
    void anotaQuienQueYElAntesYDespuesEnJson() {
        auditoria.registrar("EDITAR", "Equipo", 4L, "Editó el equipo Leones", java.util.Map.of("color", "rojo"),
                java.util.Map.of("color", "azul"));
        RegistroAuditoria r = guardado();
        assertThat(r.getUsuario()).isEqualTo("santiago");
        assertThat(r.getAdmin()).isSameAs(admin);
        assertThat(r.getAccion()).isEqualTo("EDITAR");
        assertThat(r.getEntidadId()).isEqualTo(4L);
        assertThat(r.getDescripcion()).isEqualTo("Editó el equipo Leones");
        assertThat(r.getAntes()).isEqualTo("{\"color\":\"rojo\"}");
        assertThat(r.getDespues()).isEqualTo("{\"color\":\"azul\"}");
    }

    @Test
    void crearUnJugadorQuedaRegistrado() {
        JugadorRepository jugadores = mock(JugadorRepository.class);
        when(jugadores.save(any(Jugador.class))).thenAnswer(i -> {
            Jugador j = i.getArgument(0);
            j.setId(27L);
            return j;
        });
        JugadorService servicio = new JugadorService(jugadores, new Mapeador("America/Bogota"));
        servicio.setAuditoria(auditoria);
        servicio.crear(new JugadorRequest("Giancarlo", "Stanton", null, Jugador.LadoBateo.D, Jugador.LadoLanzamiento.D,
                false, null));
        RegistroAuditoria r = guardado();
        assertThat(r.getAccion()).isEqualTo("CREAR");
        assertThat(r.getEntidad()).isEqualTo("Jugador");
        assertThat(r.getEntidadId()).isEqualTo(27L);
        assertThat(r.getDescripcion()).isEqualTo("Creó al jugador Giancarlo Stanton");
        assertThat(r.getAntes()).isNull();
        assertThat(r.getDespues()).contains("\"apellidos\":\"Stanton\"");
    }

    @Test
    void sinSesionNoHayUsuarioYUnaDescripcionLargaSeRecorta() {
        SecurityContextHolder.clearContext();
        auditoria.registrar("ELIMINAR", "Partido", 9L, "x".repeat(500), null, null);
        RegistroAuditoria r = guardado();
        assertThat(r.getUsuario()).isNull();
        assertThat(r.getDescripcion()).hasSize(400).endsWith("...");
    }
}
