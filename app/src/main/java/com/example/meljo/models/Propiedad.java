package com.example.meljo.models;

public class Propiedad {
    public int aseos;
    public String casaid;
    public int cuartos;
    public String datemod;
    public String datereg;
    public String descripcion;
    public String direccion;
    public int id;
    public double latitud;
    public double longitud;
    public int metros;
    public double precio;
    public boolean vendido;

    public Propiedad() {
    }

    public Propiedad(String casaid, double precio, String descripcion, String datereg, String datemod, boolean vendido, int metros, int cuartos, int aseos, String direccion, double latitud, double longitud) {
        this.casaid = casaid;
        this.precio = precio;
        this.descripcion = descripcion;
        this.datereg = datereg;
        this.datemod = datemod;
        this.vendido = vendido;
        this.metros = metros;
        this.cuartos = cuartos;
        this.aseos = aseos;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getCasaid() {
        return this.casaid;
    }

    public double getPrecio() {
        return this.precio;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public String getDatereg() {
        return this.datereg;
    }

    public String getDatemod() {
        return this.datemod;
    }

    public boolean getVendido() {
        return this.vendido;
    }

    public int getMetros() {
        return this.metros;
    }

    public int getCuartos() {
        return this.cuartos;
    }

    public int getAseos() {
        return this.aseos;
    }

    public String getDireccion() {
        return this.direccion;
    }

    public double getLatitud() {
        return this.latitud;
    }

    public double getLongitud() {
        return this.longitud;
    }
}
