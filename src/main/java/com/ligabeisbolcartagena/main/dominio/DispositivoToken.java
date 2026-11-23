package com.ligabeisbolcartagena.main.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Token de notificaciones push de un dispositivo. */
@Entity
@Table(name = "device_token")
public class DispositivoToken {

    public enum Plataforma { ANDROID, IOS, WEB }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private UsuarioCuenta cuenta;

    @Column(name = "token_push", nullable = false)
    private String tokenPush;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plataforma plataforma;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UsuarioCuenta getCuenta() { return cuenta; }
    public void setCuenta(UsuarioCuenta cuenta) { this.cuenta = cuenta; }
    public String getTokenPush() { return tokenPush; }
    public void setTokenPush(String tokenPush) { this.tokenPush = tokenPush; }
    public Plataforma getPlataforma() { return plataforma; }
    public void setPlataforma(Plataforma plataforma) { this.plataforma = plataforma; }
}
