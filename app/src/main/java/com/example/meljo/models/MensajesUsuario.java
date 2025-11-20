package com.example.meljo.models;

import java.util.ArrayList;
import java.util.List;

public class MensajesUsuario {
    private String correo;
    private List<String> mensajes = new ArrayList();
    private String uid;

    public MensajesUsuario(String uid, String correo) {
        this.uid = uid;
        this.correo = correo;
    }

    public String getUid() {
        return this.uid;
    }

    public String getCorreo() {
        return this.correo;
    }

    public List<String> getMensajes() {
        return this.mensajes;
    }

    public void addMensaje(String msg) {
        this.mensajes.add(msg);
    }
}
