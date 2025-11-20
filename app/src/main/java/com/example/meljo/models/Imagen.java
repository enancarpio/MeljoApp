package com.example.meljo.models;

public class Imagen {
    public String casaid;
    public String datemod;
    public String datereg;
    public int imagenid;
    public String nbimagen;
    public String tipo;
    public String urimagen;

    public Imagen(int imagenid) {
    }

    public Imagen(int imagenid, String urimagen, String casaid, String datereg, String datemod, String nbimagen, String tipo) {
        this.imagenid = imagenid;
        this.urimagen = urimagen;
        this.casaid = casaid;
        this.datereg = datereg;
        this.datemod = datemod;
        this.nbimagen = nbimagen;
        this.tipo = tipo;
    }

    public Imagen() {
    }

    public int getImagenid() {
        return this.imagenid;
    }

    public void setImagenid(int imagenid) {
        this.imagenid = imagenid;
    }

    public String getUrimagen() {
        return this.urimagen;
    }

    public void setUrimagen(String urimagen) {
        this.urimagen = urimagen;
    }

    public String getCasaid() {
        return this.casaid;
    }

    public void setCasaid(String casaid) {
        this.casaid = casaid;
    }

    public String getDatereg() {
        return this.datereg;
    }

    public void setDatereg(String datereg) {
        this.datereg = datereg;
    }

    public String getDatemod() {
        return this.datemod;
    }

    public void setDatemod(String datemod) {
        this.datemod = datemod;
    }

    public String getNbimagen() {
        return this.nbimagen;
    }

    public void setNbimagen(String nbimagen) {
        this.nbimagen = nbimagen;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
