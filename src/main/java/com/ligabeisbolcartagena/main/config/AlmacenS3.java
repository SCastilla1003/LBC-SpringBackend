package com.ligabeisbolcartagena.main.config;

import com.ligabeisbolcartagena.main.service.AlmacenObjetos;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.net.URI;
import java.util.Optional;

/**
 * Bucket S3 de Railway. Railway expone las credenciales en las variables BUCKET,
 * ACCESS_KEY_ID, SECRET_ACCESS_KEY, REGION y ENDPOINT. Los buckets de Railway son
 * privados: las fotos se sirven a través de la API, nunca con un enlace público.
 * Si falta alguna variable, el almacén queda deshabilitado y la subida de fotos
 * responde SERVICIO_NO_DISPONIBLE (el resto de la API funciona igual).
 */
@Component
public class AlmacenS3 implements AlmacenObjetos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenS3.class);

    private final String bucket;
    private final S3Client s3;

    public AlmacenS3(@Value("${lbc.bucket.nombre:}") String bucket,
                     @Value("${lbc.bucket.access-key:}") String accessKey,
                     @Value("${lbc.bucket.secret-key:}") String secretKey,
                     @Value("${lbc.bucket.region:auto}") String region,
                     @Value("${lbc.bucket.endpoint:}") String endpoint) {
        boolean completo = !bucket.isBlank() && !accessKey.isBlank() && !secretKey.isBlank() && !endpoint.isBlank();
        this.bucket = bucket;
        if (!completo) {
            log.warn("Bucket de fotos sin configurar (BUCKET, ACCESS_KEY_ID, SECRET_ACCESS_KEY, ENDPOINT): "
                    + "la subida de fotos y escudos está deshabilitada.");
            this.s3 = null;
            return;
        }
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region.isBlank() ? "auto" : region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                // Railway no admite el nombre del bucket en el dominio: direcciones con ruta.
                .forcePathStyle(true)
                .build();
        log.info("Bucket de fotos '{}' configurado.", bucket);
    }

    @Override
    public boolean disponible() {
        return s3 != null;
    }

    @Override
    public void guardar(String clave, byte[] datos, String tipo) {
        s3.putObject(r -> r.bucket(bucket).key(clave).contentType(tipo), RequestBody.fromBytes(datos));
    }

    @Override
    public Optional<Objeto> leer(String clave) {
        try {
            ResponseBytes<GetObjectResponse> r = s3.getObjectAsBytes(g -> g.bucket(bucket).key(clave));
            return Optional.of(new Objeto(r.asByteArray(), r.response().contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    public void borrar(String clave) {
        s3.deleteObject(d -> d.bucket(bucket).key(clave));
    }

    @PreDestroy
    void cerrar() {
        if (s3 != null) {
            s3.close();
        }
    }
}
