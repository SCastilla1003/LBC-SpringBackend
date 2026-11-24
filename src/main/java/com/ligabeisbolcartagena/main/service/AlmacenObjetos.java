package com.ligabeisbolcartagena.main.service;

import java.util.Optional;

/**
 * Almacén de archivos (fotos de jugadores y escudos de equipos). En producción es el
 * bucket S3 de Railway; en las pruebas, una implementación en memoria.
 */
public interface AlmacenObjetos {

    /** Archivo guardado: bytes y tipo de contenido (p. ej. image/jpeg). */
    record Objeto(byte[] datos, String tipo) {
    }

    /** true si el almacén está configurado (credenciales del bucket presentes). */
    boolean disponible();

    void guardar(String clave, byte[] datos, String tipo);

    Optional<Objeto> leer(String clave);

    void borrar(String clave);
}
