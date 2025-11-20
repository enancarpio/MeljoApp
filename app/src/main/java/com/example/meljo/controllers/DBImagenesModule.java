package com.example.meljo.controllers;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.meljo.models.Imagen;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import okhttp3.*;

public class DBImagenesModule {

    private final DBHelper helper;

    public DBImagenesModule(DBHelper helper) {
        this.helper = helper;
    }

    public void getAllImagenes(final AppCallback callback) {
        Request request = new Request.Builder()
                .url(DBHelper.getSupabaseUrl() + "/rest/v1/imagenes?select=*")
                .addHeader("apikey", DBHelper.getSupabaseApiKey())
                .addHeader("Authorization", DBHelper.getSupabaseJwt())
                .addHeader("Accept", "application/json")
                .get()
                .build();
        DBHelper.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DBHelper", "getAllImagenes failure: " + e.getMessage());
                callback.onError("Error de red: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    try {
                        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                        List<Imagen> lista = new ArrayList<>();
                        Iterator<JsonElement> it = arr.iterator();
                        while (it.hasNext()) {
                            JsonElement el = it.next();
                            JsonObject o = el.getAsJsonObject();
                            Imagen img = new Imagen();
                            img.imagenid = (!o.has("imagenid") || o.get("imagenid").isJsonNull()) ? 0 : o.get("imagenid").getAsInt();
                            String asString = null;
                            img.urimagen = (!o.has("urimagen") || o.get("urimagen").isJsonNull()) ? null : o.get("urimagen").getAsString();
                            img.casaid = (!o.has("casaid") || o.get("casaid").isJsonNull()) ? null : o.get("casaid").getAsString();
                            img.datereg = (!o.has("datereg") || o.get("datereg").isJsonNull()) ? null : o.get("datereg").getAsString();
                            img.datemod = (!o.has("datemod") || o.get("datemod").isJsonNull()) ? null : o.get("datemod").getAsString();
                            img.nbimagen = (!o.has("nbimagen") || o.get("nbimagen").isJsonNull()) ? null : o.get("nbimagen").getAsString();
                            if (o.has("tipo") && !o.get("tipo").isJsonNull()) {
                                asString = o.get("tipo").getAsString();
                            }
                            img.tipo = asString;
                            lista.add(img);
                        }
                        callback.onImagenFragYcatalogoExito(lista);
                        return;
                    } catch (Exception ex) {
                        Log.e("DBHelper", "parse getAllImagenes: " + ex.getMessage());
                        callback.onError("Error parseando respuesta");
                        return;
                    }
                }
                callback.onError("Error Supabase: " + (response != null ? response.code() : -1));
            }
        });
    }

    public void insertarImagen(Context ctx, String imagenUri, final String nombrePropiedad, final String nbimagen,
                               final String fechaRegistro, final String fechaModificacion, final String tipo,
                               AppCallback callback) throws FileNotFoundException {

        Exception ex = null; // variable para almacenar excepciones
        final AppCallback appCallback = callback;

        try {

            // Caso 1: URL remota
            if (imagenUri.startsWith("http://") || imagenUri.startsWith("https://")) {
                Log.d("DBHelper", "Se detecta URL pública, solo se insertará registro en la tabla.");
                insertarRegistroImagen(imagenUri, nombrePropiedad, nbimagen, fechaRegistro, fechaModificacion, tipo, appCallback);
                return;
            }

            // Caso 2: Imagen local
            Uri uri = Uri.parse(imagenUri);
            final InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) {
                appCallback.onError("No se pudo abrir el archivo");
                return;
            }

            // Generar nombre de archivo único
            String filename = UUID.randomUUID().toString() + "_" + nbimagen.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
            final String pathInBucket = "propiedades/" + nombrePropiedad + "/" + filename;
            String uploadUrl = DBHelper.getSupabaseUrl() + "/storage/v1/object/imagenesmeljo/" + pathInBucket;

            // Determinar MIME type
            String mime = ctx.getContentResolver().getType(uri);
            if (mime == null) mime = "application/octet-stream";

            // Crear RequestBody y request
            RequestBody fileBody = helper.createRequestBodyFromStreamForModule(in, mime);
            Request requestUpload = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Authorization", DBHelper.getSupabaseJwt())
                    .addHeader("apikey", DBHelper.getSupabaseApiKey())
                    .put(fileBody)
                    .build();

            DBHelper.getClient().newCall(requestUpload).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("DBHelper", "Fallo al subir archivo: " + e.getMessage());
                    appCallback.onError("Fallo al subir archivo: " + e.getMessage());
                    try {
                        in.close();
                    } catch (IOException ignored) {}
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        in.close();
                    } catch (IOException ignored) {}

                    Log.d("DBHelper", "Respuesta subida archivo: " + response.code());
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Sin cuerpo de respuesta";
                        Log.e("DBHelper", "Error upload: " + response.code() + " - " + errorBody);
                        appCallback.onError("Error upload: " + response.code() + " - " + errorBody);
                    } else {
                        String publicUrl = DBHelper.getSupabaseUrl() + "/storage/v1/object/public/imagenesmeljo/" + pathInBucket;
                        Log.d("DBHelper", "Archivo subido correctamente. URL pública: " + publicUrl);
                        insertarRegistroImagen(publicUrl, nombrePropiedad, nbimagen, fechaRegistro, fechaModificacion, tipo, appCallback);
                    }
                }
            });

        } catch (Exception e) {
            ex = e;
            Log.e("DBHelper", "insertarImagen exception: " + ex.getMessage(), ex);
            appCallback.onError("Error: " + ex.getMessage());
        }
    }

    public void insertarRegistroImagen(final String urlImagen,
                                       final String nombrePropiedad,
                                       final String nbimagen,
                                       final String fechaRegistro,
                                       final String fechaModificacion,
                                       final String tipo,
                                       final AppCallback callback) {

        JsonObject json = new JsonObject();
        json.addProperty("urimagen", urlImagen);
        json.addProperty("casaid", nombrePropiedad);
        json.addProperty("datereg", fechaRegistro);
        json.addProperty("datemod", fechaModificacion);
        json.addProperty("nbimagen", nbimagen);
        json.addProperty("tipo", tipo);

        Log.d("DBHelper", "Insertando registro en tabla 'imagenes': " + json.toString());

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request reqInsert = new Request.Builder()
                .url(DBHelper.getSupabaseUrl() + "/rest/v1/imagenes")
                .addHeader("apikey", DBHelper.getSupabaseApiKey())
                .addHeader("Authorization", DBHelper.getSupabaseApiKey()) // conservado como en tu original
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        DBHelper.getClient().newCall(reqInsert).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DBHelper", "Fallo al insertar registro: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Fallo al insertar registro: " + e.getMessage())
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("DBHelper", "Respuesta insert tabla: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String res = response.body().string();
                    Log.d("DBHelper", "Respuesta insert: " + res);

                    JsonArray arr = JsonParser.parseString(res).getAsJsonArray();
                    if (!arr.isEmpty()) {
                        JsonObject o = arr.get(0).getAsJsonObject();
                        Imagen img = new Imagen();

                        img.imagenid = o.has("imagenid") && !o.get("imagenid").isJsonNull()
                                ? o.get("imagenid").getAsInt() : 0;

                        img.urimagen = o.has("urimagen") && !o.get("urimagen").isJsonNull()
                                ? o.get("urimagen").getAsString() : urlImagen;

                        img.casaid = o.has("casaid") && !o.get("casaid").isJsonNull()
                                ? o.get("casaid").getAsString() : nombrePropiedad;

                        img.nbimagen = o.has("nbimagen") && !o.get("nbimagen").isJsonNull()
                                ? o.get("nbimagen").getAsString() : nbimagen;

                        img.tipo = o.has("tipo") && !o.get("tipo").isJsonNull()
                                ? o.get("tipo").getAsString() : tipo;

                        img.datereg = o.has("datereg") && !o.get("datereg").isJsonNull()
                                ? o.get("datereg").getAsString() : fechaRegistro;

                        img.datemod = o.has("datemod") && !o.get("datemod").isJsonNull()
                                ? o.get("datemod").getAsString() : fechaModificacion;

                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onImagenNewEditExito("Imagen registrada correctamente", img)
                        );
                        return;
                    }

                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("No se devolvió registro desde Supabase")
                    );
                    return;
                }

                String errorBody = response.body() != null ? response.body().string() : "Sin cuerpo de respuesta";
                Log.e("DBHelper", "Error insert imagen: " + response.code() + " - " + errorBody);

                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Error insert imagen: " + response.code() + " - " + errorBody)
                );
            }
        });
    }

    public void eliminarImagen(int id, AppCallback callback) {
        String selectUrl = DBHelper.getSupabaseUrl() + "/rest/v1/imagenes?imagenid=eq." + id + "&select=*";

        Request reqSelect = new Request.Builder()
                .url(selectUrl)
                .addHeader("apikey", DBHelper.getSupabaseApiKey())
                .addHeader("Authorization", DBHelper.getSupabaseJwt())
                .get()
                .build();

        DBHelper.getClient().newCall(reqSelect).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Error al obtener imagen: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("No se pudo obtener imagen. Código: " + (response != null ? response.code() : -1));
                    return;
                }

                String body = response.body().string();
                JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                if (arr.isEmpty()) {
                    callback.onError("Imagen no encontrada");
                    return;
                }

                JsonObject obj = arr.get(0).getAsJsonObject();
                final String urimagen = (!obj.has("urimagen") || obj.get("urimagen").isJsonNull())
                        ? null : obj.get("urimagen").getAsString();

                // Primero eliminar el registro de la tabla 'imagenes'
                String deleteUrl = DBHelper.getSupabaseUrl() + "/rest/v1/imagenes?imagenid=eq." + id;
                Request delReq = new Request.Builder()
                        .url(deleteUrl)
                        .addHeader("apikey", DBHelper.getSupabaseApiKey())
                        .addHeader("Authorization", DBHelper.getSupabaseJwt())
                        .delete()
                        .build();

                DBHelper.getClient().newCall(delReq).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call2, IOException e) {
                        callback.onError("Error al eliminar registro: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call2, Response response2) throws IOException {
                        if (!response2.isSuccessful()) {
                            callback.onError("Fallo al eliminar registro: " + response2.code());
                            return;
                        }

                        // Si hay URL de imagen, eliminar también del bucket
                        if (urimagen != null && urimagen.contains("/storage/v1/object/public/")) {
                            int idx = urimagen.indexOf("/public/imagenesmeljo/");
                            if (idx >= 0) {
                                String path = urimagen.substring(idx + "/public/imagenesmeljo/".length());
                                String deleteUrl = DBHelper.getSupabaseUrl() + "/storage/v1/object/imagenesmeljo/" + path;

                                Request reqDeleteObj = new Request.Builder()
                                        .url(deleteUrl)
                                        .addHeader("apikey", DBHelper.getSupabaseApiKey())
                                        .addHeader("Authorization", DBHelper.getSupabaseJwt())
                                        .delete()
                                        .build();

                                DBHelper.getClient().newCall(reqDeleteObj).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call3, IOException e) {
                                        callback.onExito("Registro eliminado. Archivo: fallo al eliminar del bucket (" + e.getMessage() + ")");
                                    }

                                    @Override
                                    public void onResponse(Call call3, Response response3) throws IOException {
                                        if (response3.isSuccessful()) {
                                            callback.onExito("Imagen eliminada correctamente");
                                        } else {
                                            callback.onExito("Registro eliminado. Archivo no eliminado: " + response3.code());
                                        }
                                    }
                                });
                                return;
                            }
                        }

                        callback.onExito("Imagen eliminada (registro)");
                    }
                });
            }
        });
    }

    public void actualizarImagen(Context ctx, final Imagen imagen, String nuevaUriLocal, final AppCallback callback) throws FileNotFoundException {
        // Define el bloque que actualiza los datos en Supabase
        Runnable doPatch = () -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("urimagen", imagen.urimagen);
                json.addProperty("casaid", imagen.casaid);
                json.addProperty("datemod", imagen.datemod);
                json.addProperty("nbimagen", imagen.nbimagen);
                json.addProperty("tipo", imagen.tipo);

                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
                Request req = new Request.Builder()
                        .url(DBHelper.getSupabaseUrl() + "/rest/v1/imagenes?imagenid=eq." + imagen.imagenid)
                        .addHeader("apikey", DBHelper.getSupabaseApiKey())
                        .addHeader("Authorization", DBHelper.getSupabaseJwt())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .patch(body)
                        .build();

                DBHelper.getClient().newCall(req).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onError("Error patch imagen: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String res = response.body().string();
                            JsonArray arr = JsonParser.parseString(res).getAsJsonArray();
                            if (!arr.isEmpty()) {
                                JsonObject o = arr.get(0).getAsJsonObject();
                                Imagen img = new Imagen();
                                img.imagenid = o.has("imagenid") && !o.get("imagenid").isJsonNull()
                                        ? o.get("imagenid").getAsInt()
                                        : imagen.imagenid;

                                img.urimagen = o.has("urimagen") && !o.get("urimagen").isJsonNull()
                                        ? o.get("urimagen").getAsString()
                                        : imagen.urimagen;

                                img.casaid = o.has("casaid") && !o.get("casaid").isJsonNull()
                                        ? o.get("casaid").getAsString()
                                        : imagen.casaid;

                                img.nbimagen = o.has("nbimagen") && !o.get("nbimagen").isJsonNull()
                                        ? o.get("nbimagen").getAsString()
                                        : imagen.nbimagen;

                                img.tipo = o.has("tipo") && !o.get("tipo").isJsonNull()
                                        ? o.get("tipo").getAsString()
                                        : imagen.tipo;

                                img.datereg = o.has("datereg") && !o.get("datereg").isJsonNull()
                                        ? o.get("datereg").getAsString()
                                        : imagen.datereg;

                                img.datemod = o.has("datemod") && !o.get("datemod").isJsonNull()
                                        ? o.get("datemod").getAsString()
                                        : imagen.datemod;
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                    callback.onImagenNewEditExito("Imagen actualizada", img);
                                });
                                return;
                            }
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                callback.onError("No se devolvió registro al actualizar");
                            });
                            return;
                        }
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            callback.onError("Fallo al actualizar. Código: " + (response != null ? response.code() : -1));
                        });
                    }
                });
            } catch (Exception ex) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Excepción al actualizar: " + ex.getMessage());
                });
            }
        };

        // Si no hay nueva URI local, solo hacer el PATCH
        if (nuevaUriLocal == null) {
            doPatch.run();
            return;
        }

        // Si hay nueva imagen, primero subir al bucket y luego actualizar
        try {
            Uri uri = Uri.parse(nuevaUriLocal);
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in != null) {
                String filename = UUID.randomUUID().toString() + "_" + imagen.nbimagen.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
                String pathInBucket = "propiedades/" + imagen.casaid + "/" + filename;
                String uploadUrl = DBHelper.getSupabaseUrl() + "/storage/v1/object/imagenesmeljo/" + pathInBucket;
                String mime = ctx.getContentResolver().getType(uri);
                RequestBody fileBody = helper.createRequestBodyFromStreamForModule(in, mime);

                Request requestUpload = new Request.Builder()
                        .url(uploadUrl)
                        .addHeader("Authorization", DBHelper.getSupabaseJwt())
                        .addHeader("apikey", DBHelper.getSupabaseApiKey())
                        .put(fileBody)
                        .build();

                DBHelper.getClient().newCall(requestUpload).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        try { in.close(); } catch (IOException ignored) {}
                        callback.onError("Fallo al subir nuevo archivo: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try { in.close(); } catch (IOException ignored) {}
                        if (!response.isSuccessful()) {
                            callback.onError("Error al subir nuevo archivo: " + response.code());
                            return;
                        }
                        String publicUrl = DBHelper.getSupabaseUrl() + "/storage/v1/object/public/imagenesmeljo/" + pathInBucket;
                        imagen.urimagen = publicUrl;
                        imagen.datemod = DBHelper.obtenerFechaActual();
                        doPatch.run();
                    }
                });
            } else {
                callback.onError("No se pudo abrir el archivo nuevo");
            }
        } catch (Exception ex) {
            callback.onError("Excepción subir nuevo archivo: " + ex.getMessage());
        }
    }

    public void obtenerImagenesPorCasa(String nbcasa, String tipoFiltro, final AppCallback callback) {
        Log.d("DBHelper", "obtenerImagenesPorCasa llamado con casaId=" + nbcasa + " y tipoFiltro=" + tipoFiltro);
        try {
            // Codificar parámetros
            String encodedCasa = URLEncoder.encode(nbcasa, "UTF-8");
            String url = DBHelper.getSupabaseUrl() + "/rest/v1/imagenes?casaid=eq." + encodedCasa;

            if (tipoFiltro != null && !tipoFiltro.isEmpty()) {
                url += "&tipo=eq." + URLEncoder.encode(tipoFiltro, "UTF-8");
            }

            Log.d("DBHelper", "🔗 URL Final → " + url);

            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", DBHelper.getSupabaseApiKey())
                    .addHeader("Authorization", DBHelper.getSupabaseJwt())
                    .addHeader("Accept", "application/json")
                    .get()
                    .build();

            DBHelper.getClient().newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Error de red: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        String bodyError = response.body() != null ? response.body().string() : "Sin cuerpo";
                        callback.onError("Error Supabase (" + response.code() + "): " + bodyError);
                        return;
                    }

                    String b = response.body().string();
                    Log.d("DBHelper", "📦 Respuesta Supabase: " + b);

                    try {
                        // Protege contra JSON nulo o vacío
                        if (b == null || b.equals("null") || b.isEmpty()) {
                            callback.onImagenFragYcatalogoExito(new ArrayList<>());
                            return;
                        }

                        JsonArray arr = JsonParser.parseString(b).getAsJsonArray();
                        List<Imagen> lista = new ArrayList<>();

                        for (JsonElement el : arr) {
                            if (el == null || el.isJsonNull()) continue;

                            JsonObject o = el.getAsJsonObject();
                            Imagen img = new Imagen();
                            img.imagenid = o.has("imagenid") && !o.get("imagenid").isJsonNull() ? o.get("imagenid").getAsInt() : 0;
                            img.urimagen = o.has("urimagen") && !o.get("urimagen").isJsonNull() ? o.get("urimagen").getAsString() : null;
                            img.casaid = o.has("casaid") && !o.get("casaid").isJsonNull() ? o.get("casaid").getAsString() : null;
                            img.datereg = o.has("datereg") && !o.get("datereg").isJsonNull() ? o.get("datereg").getAsString() : null;
                            img.datemod = o.has("datemod") && !o.get("datemod").isJsonNull() ? o.get("datemod").getAsString() : null;
                            img.nbimagen = o.has("nbimagen") && !o.get("nbimagen").isJsonNull() ? o.get("nbimagen").getAsString() : null;
                            img.tipo = o.has("tipo") && !o.get("tipo").isJsonNull() ? o.get("tipo").getAsString() : null;

                            lista.add(img);
                        }

                        callback.onImagenFragYcatalogoExito(lista);
                    } catch (Exception e) {
                        Log.d("DBHelper", "Error parseando JSON", e);
                        callback.onImagenFragYcatalogoExito(new ArrayList<>()); // evita fallar la app
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Error construyendo URL: " + e.getMessage());
        }
    }

    public void obtenerMultimediaEstado(final String nbcasa, final AppCallback callback) {
        final boolean[] tieneFotos = {false};
        final boolean[] tieneVideos = {false};
        final boolean[] error = {false};
        obtenerImagenesPorCasa(nbcasa, "imagen", new AppCallback() {
            @Override
            public void onImagenFragYcatalogoExito(List<Imagen> imagenes) {
                tieneFotos[0] = (imagenes == null || imagenes.isEmpty()) ? false : true;
                DBImagenesModule.this.obtenerImagenesPorCasa(nbcasa, "video", new AppCallback() {
                    @Override
                    public void onImagenFragYcatalogoExito(List<Imagen> videos) {
                        tieneVideos[0] = (videos == null || videos.isEmpty()) ? false : true;
                        callback.onMultimediaExito(tieneFotos[0], tieneVideos[0]);
                    }

                    @Override
                    public void onError(String mensaje) {
                        error[0] = true;
                        callback.onError(mensaje);
                    }
                });
            }

            @Override
            public void onError(String mensaje) {
                error[0] = true;
                callback.onError(mensaje);
            }
        });
    }

    public void obtenerMultimediaPorTipo(String nbcasa, String tipo, AppCallback callback) {
        obtenerImagenesPorCasa(nbcasa, tipo, callback);
    }
}
