package com.example.meljo.models;

import java.io.Serializable;

public class Usuario implements Serializable {
    public boolean admin;
    public String ape;
    public String datemod;
    public String datereg;
    public String email;
    public String fono;
    public String nb;
    public String password;
    public String userid;
    public String username;

    public Usuario(String userid, String email, String nb, String ape, String username, String password, boolean admin, String datereg, String datemod, String fono) {
        this.userid = userid;
        this.email = email;
        this.nb = nb;
        this.ape = ape;
        this.username = username;
        this.password = password;
        this.admin = admin;
        this.datereg = datereg;
        this.datemod = datemod;
        this.fono = fono;
    }

    public Usuario() {
    }
}
