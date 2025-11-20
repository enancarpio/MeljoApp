package com.example.meljo.controllers;

import com.example.meljo.models.Propiedad;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Request;

/**
 * DBPropiedadesModule totalmente compatible con DBHelper.
 */
public class DBPropiedadesModule {

    public DBPropiedadesModule(DBHelper helper) {
        // no necesitamos guardarlo, DBHelper es estático
    }

    // -------------------------
    // Conversión Propiedad ↔ JSON
    // -------------------------

    private JsonObject propiedadToJson(Propiedad p, boolean incluirId) {
        JsonObject json = new JsonObject();
        if (incluirId) json.addProperty("casaid", p.casaid);

        json.addProperty("precio", p.precio);
        json.addProperty("descripcion", p.descripcion);
        json.addProperty("datereg", p.datereg);
        json.addProperty("datemod", p.datemod);
        json.addProperty("vendido", p.vendido);
        json.addProperty("metros", p.metros);
        json.addProperty("cuartos", p.cuartos);
        json.addProperty("aseos", p.aseos);
        json.addProperty("direccion", p.direccion);
        json.addProperty("latitud", p.latitud);
        json.addProperty("longitud", p.longitud);

        return json;
    }

    // -------------------------
    // Parseo de respuesta
    // -------------------------

    public List<Propiedad> parsePropiedades(String body) {
        List<Propiedad> lista = new ArrayList<>();
        if (body == null || body.isEmpty() || body.equals("null")) return lista;

        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();

        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            Propiedad p = new Propiedad();

            p.id = DBHelper.getIntSafe(o, "id");
            p.casaid = DBHelper.getStringSafe(o, "casaid");
            p.precio = DBHelper.getDoubleSafe(o, "precio");
            p.descripcion = DBHelper.getStringSafe(o, "descripcion");
            p.datereg = DBHelper.getStringSafe(o, "datereg");
            p.datemod = DBHelper.getStringSafe(o, "datemod");
            p.vendido = DBHelper.getBooleanSafe(o, "vendido");
            p.metros = DBHelper.getIntSafe(o, "metros");
            p.cuartos = DBHelper.getIntSafe(o, "cuartos");
            p.aseos = DBHelper.getIntSafe(o, "aseos");
            p.direccion = DBHelper.getStringSafe(o, "direccion");
            p.latitud = DBHelper.getDoubleSafe(o, "latitud");
            p.longitud = DBHelper.getDoubleSafe(o, "longitud");

            lista.add(p);
        }
        return lista;
    }

    // -------------------------
    // Métodos públicos CRUD
    // -------------------------

    public void insertarPropiedad(final Propiedad p, final AppCallback callback) {

        Request req = DBHelper.construirRequest(
                DBHelper.getSupabaseUrl() + "/rest/v1/propiedades",
                "POST",
                propiedadToJson(p, true),
                true
        );

        DBHelper.ejecutarRequest(
                req,
                true,
                body -> {
                    List<Propiedad> lista = parsePropiedades(body);
                    if (!lista.isEmpty()) {
                        p.id = lista.get(0).id;
                        callback.onPropNewEditExito("Propiedad insertada con éxito", p);
                    } else {
                        callback.onError("La respuesta no devolvió propiedad");
                    }
                },
                callback::onError
        );
    }

    public void actualizarPropiedad(final Propiedad p, final AppCallback callback) {

        Request req = DBHelper.construirRequest(
                DBHelper.getSupabaseUrl() + "/rest/v1/propiedades?id=eq." + p.id,
                "PATCH",
                propiedadToJson(p, false),
                true
        );

        DBHelper.ejecutarRequest(
                req,
                false,
                body -> callback.onPropNewEditExito("Propiedad actualizada correctamente", p),
                callback::onError
        );
    }

    public void eliminarPropiedad(int id, final AppCallback callback) {

        Request req = DBHelper.construirRequest(
                DBHelper.getSupabaseUrl() + "/rest/v1/propiedades?id=eq." + id,
                "DELETE",
                null,
                false
        );

        DBHelper.ejecutarRequest(
                req,
                false,
                body -> callback.onExito("Propiedad eliminada correctamente"),
                callback::onError
        );
    }

    public void getTodasPropiedades(final AppCallback callback) {

        Request req = DBHelper.construirRequest(
                DBHelper.getSupabaseUrl() + "/rest/v1/propiedades?select=*",
                "GET",
                null,
                false
        );

        DBHelper.ejecutarRequest(
                req,
                true,
                body -> callback.onPropFragYnewEditYcataYchatExito(parsePropiedades(body)),
                callback::onError
        );
    }

    public void getPropiedadPorId(int id, final AppCallback callback) {

        Request req = DBHelper.construirRequest(
                DBHelper.getSupabaseUrl() + "/rest/v1/propiedades?id=eq." + id + "&select=*",
                "GET",
                null,
                false
        );

        DBHelper.ejecutarRequest(
                req,
                true,
                body -> {
                    List<Propiedad> lista = parsePropiedades(body);
                    if (!lista.isEmpty()) callback.onPropiedadPorIdExito(lista.get(0));
                    else callback.onError("Propiedad no encontrada");
                },
                callback::onError
        );
    }

    public void buscarPropiedad(final String texto, final AppCallback callback) {

        String url = DBHelper.getSupabaseUrl()
                + "/rest/v1/propiedades?or=(casaid.ilike.*"
                + texto
                + "*,descripcion.ilike.*"
                + texto
                + "*,direccion.ilike.*"
                + texto
                + "*)&select=*";

        Request req = DBHelper.construirRequest(url, "GET", null, false);

        DBHelper.ejecutarRequest(
                req,
                true,
                body -> {
                    List<Propiedad> lista = parsePropiedades(body);
                    if (!lista.isEmpty()) callback.onPropFragYnewEditYcataYchatExito(lista);
                    else callback.onError("No se encontraron propiedades");
                },
                callback::onError
        );
    }
}
