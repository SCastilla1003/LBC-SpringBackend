package com.ligabeisbolcartagena.main;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MainApplication.class);
        // Fechas y horas de los partidos se guardan como Date en Mongo: se interpretan
        // siempre en la zona de la liga, aunque el servidor (Railway) esté en UTC.
        app.addListeners((org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent e) -> {
            Environment env = e.getEnvironment();
            TimeZone.setDefault(TimeZone.getTimeZone(env.getProperty("lbc.zona-horaria", "America/Bogota")));
        });
        app.run(args);
    }
}
