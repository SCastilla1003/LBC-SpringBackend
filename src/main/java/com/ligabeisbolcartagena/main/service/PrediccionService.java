package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.PrediccionDtos.OpcionesPrediccionDto;
import com.ligabeisbolcartagena.main.api.dto.PrediccionDtos.PrediccionDto;
import com.ligabeisbolcartagena.main.api.dto.PrediccionDtos.PrediccionRequest;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.api.error.CodigoError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reenvía las predicciones al servicio Python (desplegado aparte en Railway).
 * La URL base viene de PREDICCION_URL; sin ella la predicción no está disponible.
 */
@Service
public class PrediccionService {

    private static final String NO_DISPONIBLE = "El servicio de predicción no está disponible en este momento";

    private final RestClient cliente;

    public PrediccionService(@Value("${lbc.prediccion.url:}") String url) {
        if (url == null || url.isBlank()) {
            this.cliente = null;
            return;
        }
        String base = url.trim();
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "https://" + base;
        }
        SimpleClientHttpRequestFactory factoria = new SimpleClientHttpRequestFactory();
        factoria.setConnectTimeout(3000);
        factoria.setReadTimeout(8000);
        this.cliente = RestClient.builder().baseUrl(base).requestFactory(factoria).build();
    }

    public PrediccionDto predecir(PrediccionRequest req) {
        RestClient c = disponible();
        try {
            return c.post()
                    .uri("/predecir")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(PrediccionDto.class);
        } catch (RestClientException e) {
            throw new ApiException(CodigoError.SERVICIO_NO_DISPONIBLE, NO_DISPONIBLE);
        }
    }

    public OpcionesPrediccionDto opciones() {
        RestClient c = disponible();
        try {
            return c.get().uri("/opciones").retrieve().body(OpcionesPrediccionDto.class);
        } catch (RestClientException e) {
            throw new ApiException(CodigoError.SERVICIO_NO_DISPONIBLE, NO_DISPONIBLE);
        }
    }

    private RestClient disponible() {
        if (cliente == null) {
            throw new ApiException(CodigoError.SERVICIO_NO_DISPONIBLE,
                    "La predicción no está configurada (falta PREDICCION_URL)");
        }
        return cliente;
    }
}
