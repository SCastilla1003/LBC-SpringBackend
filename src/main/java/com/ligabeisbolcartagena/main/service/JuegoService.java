package com.ligabeisbolcartagena.main.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligabeisbolcartagena.main.api.dto.ComunDtos.JugadorResumen;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.AccionDto;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.CambioRequest;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.EquipoJuegoDto;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.EstadoJuegoDto;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.JugadaRequest;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.LanzamientoRequest;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.TurnoDto;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.AccionJuego;
import com.ligabeisbolcartagena.main.dominio.AccionJuegoRepository;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.Jugada;
import com.ligabeisbolcartagena.main.dominio.Jugada.Resultado;
import com.ligabeisbolcartagena.main.dominio.JugadaRepository;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.JugadorRepository;
import com.ligabeisbolcartagena.main.dominio.Lanzamiento;
import com.ligabeisbolcartagena.main.dominio.LanzamientoRepository;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.MovimientoJugada;
import com.ligabeisbolcartagena.main.dominio.MovimientoJugadaRepository;
import com.ligabeisbolcartagena.main.dominio.Partido;
import com.ligabeisbolcartagena.main.dominio.PartidoRepository;
import com.ligabeisbolcartagena.main.dominio.Plantilla;
import com.ligabeisbolcartagena.main.dominio.PlantillaRepository;
import com.ligabeisbolcartagena.main.dominio.PosicionAlineacion;
import com.ligabeisbolcartagena.main.dominio.PosicionAlineacionRepository;
import com.ligabeisbolcartagena.main.dominio.Sustitucion;
import com.ligabeisbolcartagena.main.dominio.SustitucionRepository;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;
import com.ligabeisbolcartagena.main.service.juego.AlineacionActual;
import com.ligabeisbolcartagena.main.service.juego.EstadoVivo;
import com.ligabeisbolcartagena.main.service.juego.MotorJuego;
import com.ligabeisbolcartagena.main.service.juego.Movimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Anotador en vivo: inicia el partido, registra lanzamientos, jugadas y cambios, y deshace
 * la última acción. Cada acción guarda en game_action la foto del estado anterior.
 */
@Service
public class JuegoService {

    private final PartidoRepository partidos;
    private final PartidoService partidoService;
    private final PosicionAlineacionRepository alineaciones;
    private final PlantillaRepository plantillas;
    private final JugadorRepository jugadores;
    private final JugadaRepository jugadas;
    private final MovimientoJugadaRepository movimientos;
    private final LanzamientoRepository lanzamientos;
    private final SustitucionRepository sustituciones;
    private final AccionJuegoRepository acciones;
    private final UsuarioCuentaRepository usuarios;
    private final Mapeador mapeador;
    private final ObjectMapper json;
    private final ZoneId zona;

    public JuegoService(PartidoRepository partidos, PartidoService partidoService,
                        PosicionAlineacionRepository alineaciones, PlantillaRepository plantillas,
                        JugadorRepository jugadores, JugadaRepository jugadas, MovimientoJugadaRepository movimientos,
                        LanzamientoRepository lanzamientos, SustitucionRepository sustituciones,
                        AccionJuegoRepository acciones, UsuarioCuentaRepository usuarios, Mapeador mapeador,
                        ObjectMapper json) {
        this.partidos = partidos;
        this.partidoService = partidoService;
        this.alineaciones = alineaciones;
        this.plantillas = plantillas;
        this.jugadores = jugadores;
        this.jugadas = jugadas;
        this.movimientos = movimientos;
        this.lanzamientos = lanzamientos;
        this.sustituciones = sustituciones;
        this.acciones = acciones;
        this.usuarios = usuarios;
        this.mapeador = mapeador;
        this.json = json;
        this.zona = mapeador.zona();
    }

    // ------------------------------------------------------------------ lectura

    @Transactional(readOnly = true)
    public EstadoJuegoDto estado(Long id) {
        Partido p = partidoService.buscar(id);
        Contexto c = contexto(p);
        List<String> faltantes = c.alineacion.faltantes(p.getEquipoLocal().getNombre(), p.getEquipoVisita().getNombre());
        List<AccionDto> ultimas = acciones.findTop30ByPartidoIdOrderByIdDesc(id).stream()
                .map(a -> new AccionDto(a.getId(), a.getTipo(), a.getDescripcion(), a.getEntrada(), a.getMitad(),
                        a.getCreadoEn()))
                .toList();
        return new EstadoJuegoDto(partidoService.detalle(p), equipo(p, c, true), equipo(p, c, false),
                faltantes.isEmpty(), faltantes, !ultimas.isEmpty(), ultimas);
    }

    private EquipoJuegoDto equipo(Partido p, Contexto c, boolean local) {
        Equipo e = local ? p.getEquipoLocal() : p.getEquipoVisita();
        List<TurnoDto> orden = c.alineacion.orden(local).stream()
                .map(t -> new TurnoDto(t.orden(), t.posicion(), resumen(c, t.jugadorId())))
                .toList();
        Long lanzador = c.alineacion.lanzadorDe(local);
        List<JugadorResumen> disponibles = (local ? c.plantillaLocal : c.plantillaVisita).stream()
                .map(Plantilla::getJugador)
                .filter(j -> !c.alineacion.enJuego(local, j.getId()) && !c.alineacion.retirado(local, j.getId()))
                .map(mapeador::res)
                .toList();
        long lanzados = lanzador == null ? 0 : lanzamientos.countByPartidoIdAndLanzadorId(p.getId(), lanzador);
        int proximo = local ? p.getProximoOrdenLocal() : p.getProximoOrdenVisita();
        return new EquipoJuegoDto(mapeador.res(e), orden, proximo, resumen(c, lanzador), lanzados, disponibles);
    }

    private JugadorResumen resumen(Contexto c, Long id) {
        if (id == null) {
            return null;
        }
        Jugador j = c.jugadores.get(id);
        return j == null ? null : mapeador.res(j);
    }

    // ------------------------------------------------------------------ escrituras

    @Transactional
    public EstadoJuegoDto iniciar(Long id, String commandId, String usuario) {
        return ejecutar(id, commandId, usuario, "INICIO", (p, s, c, a) -> {
            if (p.getEstado() != Partido.Estado.SCHEDULED && p.getEstado() != Partido.Estado.LINEUP_PUBLISHED) {
                throw ApiException.conflicto("El partido ya empezó");
            }
            List<String> faltan = c.alineacion.faltantes(p.getEquipoLocal().getNombre(),
                    p.getEquipoVisita().getNombre());
            if (!faltan.isEmpty()) {
                throw ApiException.invalida("Para iniciar el partido completa las alineaciones: "
                        + String.join("; ", faltan) + ".", Map.of("faltantes", faltan));
            }
            MotorJuego.iniciar(s, c.alineacion);
            a.setDescripcion("¡Empieza el partido! " + p.getEquipoVisita().getNombre() + " al bate. Lanza "
                    + c.nombre(s.lanzador) + ".");
        });
    }

    @Transactional
    public EstadoJuegoDto lanzamiento(Long id, LanzamientoRequest req, String usuario) {
        return ejecutar(id, req.commandId(), usuario, "LANZAMIENTO", (p, s, c, a) -> {
            Long bateador = s.bateador;
            Long lanzador = s.lanzador;
            Optional<Resultado> cierra = MotorJuego.lanzamiento(s, req.tipo());
            Lanzamiento l = guardarLanzamiento(p, s, req.tipo(), bateador, lanzador, req.commandId());
            a.setLanzamientoId(l.getId());
            if (cierra.isPresent()) {
                String texto = registrarJugada(p, s, c, a, cierra.get(), null, 0, req.commandId(), bateador, lanzador);
                a.setTipo("JUGADA");
                a.setDescripcion(texto);
            } else {
                a.setDescripcion(etiqueta(req.tipo()) + " a " + c.nombre(bateador) + " (" + s.bolas + "-" + s.strikes
                        + ")");
            }
        });
    }

    @Transactional
    public EstadoJuegoDto jugada(Long id, JugadaRequest req, String usuario) {
        return ejecutar(id, req.commandId(), usuario, "JUGADA", (p, s, c, a) -> {
            Long bateador = s.bateador;
            Long lanzador = s.lanzador;
            List<Movimiento> movs = req.movimientos() == null ? null
                    : req.movimientos().stream().map(m -> new Movimiento(m.desde(), m.hasta())).toList();
            Lanzamiento l = null;
            if (s.estado == Partido.Estado.LIVE && MotorJuego.CON_LANZAMIENTO_EN_JUEGO.contains(req.resultado())) {
                l = guardarLanzamiento(p, s, Lanzamiento.Tipo.IN_PLAY, bateador, lanzador, req.commandId());
            }
            String texto = registrarJugada(p, s, c, a, req.resultado(), movs,
                    req.errores() == null ? 0 : req.errores(), req.commandId(), bateador, lanzador);
            if (l != null) {
                a.setLanzamientoId(l.getId());
            }
            a.setDescripcion(texto);
        });
    }

    @Transactional
    public EstadoJuegoDto cambio(Long id, CambioRequest req, String usuario) {
        return ejecutar(id, req.commandId(), usuario, "CAMBIO", (p, s, c, a) -> {
            if (s.estado != Partido.Estado.LIVE && s.estado != Partido.Estado.SUSPENDED) {
                throw ApiException.conflicto("Los cambios se hacen con el partido en juego");
            }
            boolean local = req.local();
            Long sale = req.saleId();
            Long entra = req.entraId();
            Sustitucion.Tipo tipo = req.tipo();
            String posicion = req.posicion() == null || req.posicion().isBlank() ? null : req.posicion().trim();
            if (posicion != null && !posicion.matches("^(P|C|1B|2B|3B|SS|LF|CF|RF|DH)$")) {
                throw ApiException.invalida("Posición no válida");
            }
            if (!c.alineacion.enJuego(local, sale)) {
                throw ApiException.invalida(c.nombre(sale) + " no está jugando por ese equipo");
            }
            boolean soloPosicion = sale.equals(entra);
            if (soloPosicion) {
                if (tipo != Sustitucion.Tipo.DEFENSIVE || posicion == null) {
                    throw ApiException.invalida("Para mover a un jugador de posición elige la nueva posición");
                }
            } else {
                boolean enPlantilla = (local ? c.plantillaLocal : c.plantillaVisita).stream()
                        .anyMatch(pl -> pl.getJugador().getId().equals(entra));
                if (!enPlantilla) {
                    throw ApiException.invalida("El jugador que entra no está en el roster del equipo");
                }
                if (c.alineacion.enJuego(local, entra)) {
                    throw ApiException.invalida(c.nombre(entra) + " ya está jugando");
                }
                if (c.alineacion.retirado(local, entra)) {
                    throw ApiException.invalida(c.nombre(entra) + " ya salió del partido y no puede volver");
                }
            }
            boolean ofensiva = local == s.bateaLocal();
            switch (tipo) {
                case PINCH_HITTER -> {
                    if (!ofensiva || !sale.equals(s.bateador)) {
                        throw ApiException.invalida("El emergente entra por el bateador que está al bate");
                    }
                    s.bateador = entra;
                }
                case PINCH_RUNNER -> {
                    int base = baseDe(s, sale);
                    if (!ofensiva || base == 0) {
                        throw ApiException.invalida("El corredor emergente entra por un corredor que está en base");
                    }
                    s.bases[base] = entra;
                }
                case PITCHER -> {
                    if (!sale.equals(c.alineacion.lanzadorDe(local))) {
                        throw ApiException.invalida(c.nombre(sale) + " no es el lanzador actual");
                    }
                    if (!ofensiva) {
                        s.lanzador = entra;
                    }
                }
                case DEFENSIVE -> {
                    if (sale.equals(c.alineacion.lanzadorDe(local)) && !soloPosicion) {
                        throw ApiException.invalida("Para cambiar el lanzador usa \"Cambio de lanzador\"");
                    }
                }
            }
            Integer orden = c.alineacion.ordenDe(local, sale);
            Sustitucion sub = new Sustitucion();
            sub.setPartido(p);
            sub.setEquipo(local ? p.getEquipoLocal() : p.getEquipoVisita());
            sub.setEntrada(s.entrada);
            sub.setMitad(s.mitad);
            sub.setJugadorSale(jugadores.getReferenceById(sale));
            sub.setJugadorEntra(jugadores.getReferenceById(entra));
            sub.setTipo(tipo);
            sub.setOrdenBateo(orden);
            sub.setPosicion(posicion);
            sub.setCommandId(req.commandId());
            sub.setOcurridoEn(ahora());
            a.setSustitucionId(sustituciones.save(sub).getId());
            String equipo = (local ? p.getEquipoLocal() : p.getEquipoVisita()).getAbreviatura();
            a.setDescripcion(switch (tipo) {
                case PINCH_HITTER -> equipo + ": " + c.nombre(entra) + " batea de emergente por " + c.nombre(sale) + ".";
                case PINCH_RUNNER -> equipo + ": " + c.nombre(entra) + " corre por " + c.nombre(sale) + ".";
                case PITCHER -> equipo + ": " + c.nombre(entra) + " entra a lanzar por " + c.nombre(sale) + ".";
                case DEFENSIVE -> soloPosicion
                        ? equipo + ": " + c.nombre(sale) + " pasa a " + posicion + "."
                        : equipo + ": " + c.nombre(entra) + " entra por " + c.nombre(sale)
                                + (posicion == null ? "" : " (" + posicion + ")") + ".";
            });
        });
    }

    @Transactional
    public EstadoJuegoDto finalizar(Long id, String commandId, String usuario) {
        return ejecutar(id, commandId, usuario, "FIN", (p, s, c, a) -> {
            if (s.estado != Partido.Estado.LIVE && s.estado != Partido.Estado.SUSPENDED) {
                throw ApiException.conflicto("Solo se puede finalizar un partido en juego o suspendido");
            }
            if (s.carrerasLocal == s.carrerasVisita) {
                throw ApiException.invalida("En el béisbol no hay empates. Si el partido no puede seguir, "
                        + "detenlo indicando si es retrasado, pospuesto o cancelado.");
            }
            MotorJuego.terminar(s);
            a.setDescripcion("Final del partido: " + p.getEquipoVisita().getAbreviatura() + " " + s.carrerasVisita
                    + ", " + p.getEquipoLocal().getAbreviatura() + " " + s.carrerasLocal + ".");
        });
    }

    @Transactional
    public EstadoJuegoDto suspender(Long id, String commandId, Partido.Motivo motivo, String usuario) {
        return ejecutar(id, commandId, usuario, "SUSPENSION", (p, s, c, a) -> {
            if (s.estado != Partido.Estado.LIVE) {
                throw ApiException.conflicto("Solo se puede detener un partido en vivo");
            }
            s.estado = Partido.Estado.SUSPENDED;
            s.motivo = motivo;
            a.setDescripcion(motivo == null ? "Partido suspendido."
                    : "Partido " + etiqueta(motivo) + " con el marcador " + marcador(p, s)
                            + ". El administrador decidirá si continúa o se deja así.");
        });
    }

    /** El administrador decide qué pasa con un partido detenido: continuarlo o dejarlo así. */
    @Transactional
    public EstadoJuegoDto decidir(Long id, String commandId, boolean continuar, String usuario) {
        return ejecutar(id, commandId, usuario, "DECISION", (p, s, c, a) -> {
            if (s.estado != Partido.Estado.SUSPENDED) {
                throw ApiException.conflicto("El partido no está detenido");
            }
            if (continuar) {
                s.estado = Partido.Estado.LIVE;
                s.motivo = null;
                a.setDescripcion("El administrador decide continuar el partido. ¡Se reanuda!");
            } else {
                Partido.Motivo motivo = s.motivo == null ? Partido.Motivo.CANCELADO : s.motivo;
                MotorJuego.terminar(s);
                s.motivo = motivo;
                a.setDescripcion("El administrador deja el partido así (" + etiqueta(motivo) + "): "
                        + marcador(p, s) + ".");
            }
        });
    }

    @Transactional
    public EstadoJuegoDto reanudar(Long id, String commandId, String usuario) {
        return ejecutar(id, commandId, usuario, "REANUDACION", (p, s, c, a) -> {
            if (s.estado != Partido.Estado.SUSPENDED) {
                throw ApiException.conflicto("El partido no está suspendido");
            }
            if (s.motivo != null) {
                throw ApiException.conflicto("El partido está " + etiqueta(s.motivo)
                        + ": el administrador decide si continúa o se deja así");
            }
            s.estado = Partido.Estado.LIVE;
            a.setDescripcion("Se reanuda el partido.");
        });
    }

    /** Deshace la última acción: vuelve a la foto anterior y borra lo que esa acción creó. */
    @Transactional
    public EstadoJuegoDto deshacer(Long id) {
        Partido p = partidoService.buscar(id);
        AccionJuego ultima = acciones.findTopByPartidoIdOrderByIdDesc(id)
                .orElseThrow(() -> ApiException.conflicto("No hay nada que deshacer"));
        EstadoVivo antes;
        try {
            antes = json.readValue(ultima.getEstadoAntes(), EstadoVivo.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Estado guardado ilegible en la acción " + ultima.getId(), e);
        }
        antes.aplicarA(p, jugadores::getReferenceById);
        acciones.delete(ultima);
        if (ultima.getJugadaId() != null) {
            jugadas.deleteById(ultima.getJugadaId());
        }
        if (ultima.getLanzamientoId() != null) {
            lanzamientos.deleteById(ultima.getLanzamientoId());
        }
        if (ultima.getSustitucionId() != null) {
            sustituciones.deleteById(ultima.getSustitucionId());
        }
        partidos.save(p);
        partidos.flush();
        return estado(id);
    }

    // ------------------------------------------------------------------ comunes

    @FunctionalInterface
    private interface Accion {
        void aplicar(Partido p, EstadoVivo s, Contexto c, AccionJuego a);
    }

    /** Carga, comprueba duplicados, fotografía el estado, aplica la acción y la registra. */
    private EstadoJuegoDto ejecutar(Long id, String commandId, String usuario, String tipo, Accion accion) {
        Partido p = partidoService.buscar(id);
        if (acciones.existsByPartidoIdAndCommandId(id, commandId)) {
            return estado(id); // reintento de algo que ya se aplicó
        }
        EstadoVivo s = EstadoVivo.de(p);
        AccionJuego a = new AccionJuego();
        a.setPartido(p);
        a.setTipo(tipo);
        a.setCommandId(commandId);
        a.setEntrada(s.entrada);
        a.setMitad(s.mitad);
        a.setEstadoAntes(foto(s));
        a.setAnotador(usuarios.findByUsername(usuario).orElse(null));
        accion.aplicar(p, s, contexto(p), a);
        s.aplicarA(p, jugadores::getReferenceById);
        if (p.getAnotador() == null) {
            p.setAnotador(a.getAnotador());
        }
        partidos.save(p);
        acciones.save(a);
        partidos.flush();
        return estado(id);
    }

    /** Aplica una jugada, guarda la fila de game_play con sus movimientos y arma el texto. */
    private String registrarJugada(Partido p, EstadoVivo s, Contexto c, AccionJuego a, Resultado r,
                                   List<Movimiento> movs, int errores, String commandId, Long bateador,
                                   Long lanzador) {
        String fotoAntes = foto(s);
        int entrada = s.entrada;
        Mitad mitad = s.mitad;
        MotorJuego.Resumen res = MotorJuego.aplicarJugada(s, r, movs, errores, c.alineacion);

        Jugada j = new Jugada();
        j.setPartido(p);
        j.setSecuencia(jugadas.findTopByPartidoIdOrderBySecuenciaDesc(p.getId()).map(x -> x.getSecuencia() + 1)
                .orElse(1));
        j.setCommandId(commandId);
        j.setOcurridoEn(ahora());
        j.setEntrada(entrada);
        j.setMitad(mitad);
        j.setBateador(jugadores.getReferenceById(bateador));
        j.setLanzador(jugadores.getReferenceById(lanzador));
        j.setResultado(r);
        j.setCarreras(res.carreras());
        j.setOutsRegistrados(res.outs());
        j.setEstadoAntes(fotoAntes);
        j.setAnotador(a.getAnotador());
        j = jugadas.save(j);
        for (Movimiento m : res.movimientos()) {
            Long quien = res.corredores().get(m.desde());
            if (quien == null) {
                continue;
            }
            MovimientoJugada mj = new MovimientoJugada();
            mj.setJugada(j);
            mj.setJugador(jugadores.getReferenceById(quien));
            mj.setBaseDesde(m.desde());
            mj.setBaseHasta(m.hasta());
            mj.setEsOut(m.esOut());
            movimientos.save(mj);
        }
        a.setJugadaId(j.getId());

        StringBuilder t = new StringBuilder();
        if (MotorJuego.DE_CORREDORES.contains(r)) {
            List<String> quienes = new ArrayList<>();
            for (Movimiento m : res.movimientos()) {
                Long quien = res.corredores().get(m.desde());
                if (quien != null) {
                    quienes.add(c.nombre(quien) + (m.esOut() ? " (out)" : " a " + MotorJuego.nombreBase(m.hasta())));
                }
            }
            t.append(etiqueta(r)).append(": ").append(String.join(", ", quienes)).append('.');
        } else {
            t.append(c.nombre(bateador)).append(": ").append(etiqueta(r)).append('.');
            List<String> outsCorredores = new ArrayList<>();
            for (Movimiento m : res.movimientos()) {
                if (m.desde() > 0 && m.esOut()) {
                    outsCorredores.add(c.nombre(res.corredores().get(m.desde())));
                }
            }
            if (!outsCorredores.isEmpty()) {
                t.append(" Out: ").append(String.join(", ", outsCorredores)).append('.');
            }
        }
        if (!res.anotaron().isEmpty()) {
            List<String> nombres = res.anotaron().stream().map(c::nombre).toList();
            t.append(nombres.size() == 1 ? " Anota " : " Anotan ").append(String.join(", ", nombres)).append('.');
        }
        if (res.finPartido() && s.finalEspecial != null) {
            t.append(" ¡").append(etiqueta(s.finalEspecial)).append('!');
        }
        if (res.finPartido()) {
            t.append(" ¡Final del partido! ").append(p.getEquipoVisita().getAbreviatura()).append(' ')
                    .append(s.carrerasVisita).append(", ").append(p.getEquipoLocal().getAbreviatura()).append(' ')
                    .append(s.carrerasLocal).append('.');
        } else if (res.cambioMitad()) {
            t.append(" Fin de la ").append(mitad == Mitad.TOP ? "alta" : "baja").append(" del ").append(entrada)
                    .append('.');
            List<String> automaticos = new ArrayList<>();
            for (int base = 3; base >= 1; base--) {
                if (s.bases[base] != null) {
                    automaticos.add(c.nombre(s.bases[base]) + " en " + MotorJuego.nombreBase(base));
                }
            }
            if (!automaticos.isEmpty()) {
                t.append(" Muerte súbita: ").append(String.join(", ", automaticos)).append('.');
            }
        }
        return t.toString();
    }

    private Lanzamiento guardarLanzamiento(Partido p, EstadoVivo s, Lanzamiento.Tipo tipo, Long bateador,
                                           Long lanzador, String commandId) {
        if (bateador == null || lanzador == null) {
            throw ApiException.conflicto("No hay bateador o lanzador en juego");
        }
        Lanzamiento l = new Lanzamiento();
        l.setPartido(p);
        l.setEntrada(s.entrada);
        l.setMitad(s.mitad);
        l.setBateador(jugadores.getReferenceById(bateador));
        l.setLanzador(jugadores.getReferenceById(lanzador));
        l.setTipo(tipo);
        l.setCommandId(commandId);
        l.setOcurridoEn(ahora());
        return lanzamientos.save(l);
    }

    private static int baseDe(EstadoVivo s, Long jugador) {
        for (int b = 1; b <= 3; b++) {
            if (jugador.equals(s.bases[b])) {
                return b;
            }
        }
        return 0;
    }

    private String foto(EstadoVivo s) {
        try {
            return json.writeValueAsString(s);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(zona);
    }

    /** Alineación actual, rosters y nombres de los jugadores del partido. */
    private Contexto contexto(Partido p) {
        Long temporada = p.getTemporada().getId();
        List<PosicionAlineacion> filas = alineaciones.findByPartidoIdOrderByIdAsc(p.getId());
        List<Sustitucion> cambios = sustituciones.findByPartidoIdOrderByIdAsc(p.getId());
        List<Plantilla> deLocal = plantillas.findByTemporadaIdAndEquipoId(temporada, p.getEquipoLocal().getId());
        List<Plantilla> deVisita = plantillas.findByTemporadaIdAndEquipoId(temporada, p.getEquipoVisita().getId());
        Map<Long, Jugador> porId = new LinkedHashMap<>();
        deLocal.forEach(pl -> porId.put(pl.getJugador().getId(), pl.getJugador()));
        deVisita.forEach(pl -> porId.put(pl.getJugador().getId(), pl.getJugador()));
        filas.forEach(f -> porId.putIfAbsent(f.getJugador().getId(), f.getJugador()));
        cambios.forEach(sub -> {
            porId.putIfAbsent(sub.getJugadorSale().getId(), sub.getJugadorSale());
            porId.putIfAbsent(sub.getJugadorEntra().getId(), sub.getJugadorEntra());
        });
        AlineacionActual alineacion = AlineacionActual.construir(p.getEquipoLocal().getId(), filas, cambios);
        return new Contexto(alineacion, deLocal, deVisita, porId);
    }

    private record Contexto(AlineacionActual alineacion, List<Plantilla> plantillaLocal,
                            List<Plantilla> plantillaVisita, Map<Long, Jugador> jugadores) {
        String nombre(Long id) {
            Jugador j = id == null ? null : jugadores.get(id);
            return j == null ? "?" : j.getNombres() + " " + j.getApellidos();
        }
    }

    private static String marcador(Partido p, EstadoVivo s) {
        return p.getEquipoVisita().getAbreviatura() + " " + s.carrerasVisita + ", "
                + p.getEquipoLocal().getAbreviatura() + " " + s.carrerasLocal;
    }

    static String etiqueta(Partido.Motivo m) {
        return switch (m) {
            case RETRASADO -> "retrasado";
            case POSPUESTO -> "pospuesto";
            case CANCELADO -> "cancelado";
        };
    }

    static String etiqueta(Partido.FinalEspecial f) {
        return switch (f) {
            case WALK_OFF -> "WALK OFF";
            case WALK_OFF_HOME_RUN -> "WALK OFF HOME RUN";
            case WALK_OFF_GRAND_SLAM -> "WALK OFF GRAND SLAM";
        };
    }

    static String etiqueta(Lanzamiento.Tipo t) {
        return switch (t) {
            case BALL -> "Bola";
            case CALLED_STRIKE -> "Strike cantado";
            case SWINGING_STRIKE -> "Strike tirándole";
            case FOUL -> "Foul";
            case IN_PLAY -> "En juego";
        };
    }

    static final Map<Resultado, String> ETIQUETAS = new HashMap<>();

    static {
        ETIQUETAS.put(Resultado.SINGLE, "sencillo");
        ETIQUETAS.put(Resultado.DOUBLE, "doble");
        ETIQUETAS.put(Resultado.TRIPLE, "triple");
        ETIQUETAS.put(Resultado.HOME_RUN, "¡jonrón!");
        ETIQUETAS.put(Resultado.WALK, "base por bolas");
        ETIQUETAS.put(Resultado.HIT_BY_PITCH, "golpeado por lanzamiento");
        ETIQUETAS.put(Resultado.STRIKEOUT, "ponche");
        ETIQUETAS.put(Resultado.GROUND_OUT, "rolata para out");
        ETIQUETAS.put(Resultado.FLY_OUT, "elevado para out");
        ETIQUETAS.put(Resultado.LINE_OUT, "línea para out");
        ETIQUETAS.put(Resultado.POP_OUT, "globo para out");
        ETIQUETAS.put(Resultado.FIELDERS_CHOICE, "bola ocupada");
        ETIQUETAS.put(Resultado.REACHED_ON_ERROR, "se embasa por error");
        ETIQUETAS.put(Resultado.SACRIFICE_BUNT, "toque de sacrificio");
        ETIQUETAS.put(Resultado.SACRIFICE_FLY, "elevado de sacrificio");
        ETIQUETAS.put(Resultado.DOUBLE_PLAY, "doble play");
        ETIQUETAS.put(Resultado.STOLEN_BASE, "Base robada");
        ETIQUETAS.put(Resultado.CAUGHT_STEALING, "Atrapado robando");
        ETIQUETAS.put(Resultado.WILD_PITCH, "Lanzamiento descontrolado");
        ETIQUETAS.put(Resultado.PASSED_BALL, "Passed ball");
        ETIQUETAS.put(Resultado.BALK, "Balk");
    }

    static String etiqueta(Resultado r) {
        return ETIQUETAS.get(r);
    }
}
