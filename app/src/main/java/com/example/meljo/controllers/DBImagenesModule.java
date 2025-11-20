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
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DBImagenesModule {

    private final DBHelper helper;

    public DBImagenesModule(DBHelper helper) {
        this.helper = helper;
    }

    // ------------------ UTILIDADES PRIVADAS ------------------

    private Imagen parseImagen(JsonObject o) {
        Imagen img = new Imagen();
        img.imagenid = o.has("imagenid") && !o.get("imagenid").isJsonNull() ? o.get("imagenid").getAsInt() : 0;
        img.urimagen = o.has("urimagen") && !o.get("urimagen").isJsonNull() ? o.get("urimagen").getAsString() : null;
        img.casaid = o.has("casaid") && !o.get("casaid").isJsonNull() ? o.get("casaid").getAsString() : null;
        img.datereg = o.has("datereg") && !o.get("datereg").isJsonNull() ? o.get("datereg").getAsString() : null;
        img.datemod = o.has("datemod") && !o.get("datemod").isJsonNull() ? o.get("datemod").getAsString() : null;
        img.nbimagen = o.has("nbimagen") && !o.get("nbimagen").isJsonNull() ? o.get("nbimagen").getAsString() : null;
        img.tipo = o.has("tipo") && !o.get("tipo").isJsonNull() ? o.get("tipo").getAsString() : null;
        return img;
    }

    private void runOnMain(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    private void parseAndCallbackImagenes(String body, AppCallback callback) {
        try {
            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
            List<Imagen> lista = new ArrayList<>();
            for (JsonElement el : arr) {
                if (el != null && !el.isJsonNull()) {
                    lista.add(parseImagen(el.getAsJsonObject()));
                }
            }
            callback.onImagenFragYcatalogoExito(lista);
        } catch (Exception e) {
            Log.e("DBHelper", "Error parseando JSON: " + e.getMessage(), e);
            callback.onImagenFragYcatalogoExito(new ArrayList<>());
        }
    }

    private void executeRequest(Request request, AppCallback callback, boolean expectsBody) {
        DBHelper.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnMain(() -> callback.onError("Error de red: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || (expectsBody && response.body() == null)) {
                    String bodyError = (response.body() != null) ? response.body().string() : "Sin cuerpo";
                    runOnMain(() -> callback.onError("Error Supabase (" + response.code() + "): " + bodyError));
                    return;
                }

                if (expectsBody) {
                    String body = response.body().string();
                    parseAndCallbackImagenes(body, callback);
                }
            }
        });
    }

    // ------------------ MÉTODOS PÚBLICOS ------------------

    public void getAllImagenes(final AppCallback callback) {
        Request request = new Request.Builder()
                .url(DBHelper.getSupabaseUrl() + "/rest/v1/imagenes?select=*")
                .addHeader("apikey", DBHelper.getSupabaseApiKey())
                .addHeader("Authorization", DBHelper.getSupabaseJwt())
                .addHeader("Accept", "application/json")
                .get()
                .build();

        executeRequest(request, callback, true);
    }

    public void insertarImagen(Context ctx, String imagenUri, final String nombrePropiedad, final String nbimagen,
                               final String fechaRegistro, final String fechaModificacion, final String tipo,
                               AppCallback callback) throws FileNotFoundException {
        try {
            final AppCallback appCallback = callback;

            if (imagenUri.startsWith("http://") || imagenUri.startsWith("https://")) {
                insertarRegistroImagen(imagenUri, nombrePropiedad, nbimagen, fechaRegistro, fechaModificacion, tipo, appCallback);
                return;
            }

            Uri uri = Uri.parse(imagenUri);
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) {
                appCallback.onError("No se pudo abrir el archivo");
                return;
            }

            String filename = UUID.randomUUID().toString() + "_" + nbimagen.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
            final String pathInBucket = "propiedades/" + nombrePropiedad + "/" + filename;
            String uploadUrl = DBHelper.getSupabaseUrl() + "/storage/v1/object/imagenesmeljo/" + pathInBucket;
            String mime = ctx.getContentResolver().getType(uri);
            if (mime == null) mime = "application/octet-stream";

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
                    appCallback.onError("Fallo al subir archivo: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try { in.close(); } catch (IOException ignored) {}
                    if (!response.isSuccessful()) {
                        appCallback.onError("Error upload: " + response.code() + " - " +
                                (response.body() != null ? response.body().string() : "Sin cuerpo"));
                        return;
                    }
                    String publicUrl = DBHelper.getSupabaseUrl() + "/storage/v1/object/public/imagenesmeljo/" + pathInBucket;
                    insertarRegistroImagen(publicUrl, nombrePropiedad, nbimagen, fechaRegistro, fechaModificacion, tipo, appCallback);
                }
            });

        } catch (Exception ex) {
            Log.e("DBHelper", "insertarImagen exception: " + ex.getMessage(), ex);
            callback.onError("Error: " + ex.getMessage());
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

        Log.d("DBHelper", "Insertando registro: " + json.toString());

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request reqInsert = new Request.Builder()
                .url(DBHelper.getSupabaseUrl() + "/rest/v1/imagenes")
                .addHeader("apikey", DBHelper.getSupabaseApiKey())
                .addHeader("Authorization", DBHelper.getSupabaseApiKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        DBHelper.getClient().newCall(reqInsert).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnMain(() -> callback.onError("Fallo al insertar registro: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String res = response.body().string();
                    JsonArray arr = JsonParser.parseString(res).getAsJsonArray();
                    if (!arr.isEmpty()) {
                        JsonObject o = arr.get(0).getAsJsonObject();
                        Imagen img = parseImagen(o);
                        runOnMain(() -> callback.onImagenNewEditExito("Imagen registrada correctamente", img));
                        return;
                    }
                    runOnMain(() -> callback.onError("No se devolvió registro desde Supabase"));
                    return;
                }
                String errorBody = response.body() != null ? response.body().string() : "Sin cuerpo";
                runOnMain(() -> callback.onError("Error insert imagen: " + response.code() + " - " + errorBody));
            }
        });
    }

    public void obtenerImagenesPorCasa(String nbcasa, String tipoFiltro, final AppCallback callback) {
        try {
            String encodedCasa = URLEncoder.encode(nbcasa, "UTF-8");
            String url = DBHelper.getSupabaseUrl() + "/rest/v1/imagenes?casaid=eq." + encodedCasa;
            if (tipoFiltro != null && !tipoFiltro.isEmpty()) {
                url += "&tipo=eq." + URLEncoder.encode(tipoFiltro, "UTF-8");
            }

            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", DBHelper.getSupabaseApiKey())
                    .addHeader("Authorization", DBHelper.getSupabaseJwt())
                    .addHeader("Accept", "application/json")
                    .get()
                    .build();

            executeRequest(req, callback, true);

        } catch (Exception e) {
            callback.onError("Error construyendo URL: " + e.getMessage());
        }
    }

    public void obtenerMultimediaPorTipo(String nbcasa, String tipo, AppCallback callback) {
        obtenerImagenesPorCasa(nbcasa, tipo, callback);
    }

    public void actualizarImagen(Context ctx, final Imagen imagen, String nuevaUriLocal, final AppCallback callback) throws FileNotFoundException {

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
                        runOnMain(() -> callback.onError("Error patch imagen: " + e.getMessage()));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String res = response.body().string();
                            JsonArray arr = JsonParser.parseString(res).getAsJsonArray();
                            if (!arr.isEmpty()) {
                                Imagen updated = parseImagen(arr.get(0).getAsJsonObject());
                                runOnMain(() -> callback.onImagenNewEditExito("Imagen actualizada", updated));
                                return;
                            }
                            runOnMain(() -> callback.onError("No se devolvió registro al actualizar"));
                            return;
                        }
                        runOnMain(() -> callback.onError("Fallo al actualizar. Código: " + (response != null ? response.code() : -1)));
                    }
                });

            } catch (Exception ex) {
                runOnMain(() -> callback.onError("Excepción al actualizar: " + ex.getMessage()));
            }
        };

        if (nuevaUriLocal == null) {
            doPatch.run();
            return;
        }

        // Subida de nueva imagen
        try {
            Uri uri = Uri.parse(nuevaUriLocal);
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) {
                callback.onError("No se pudo abrir el archivo nuevo");
                return;
            }

            String filename = UUID.randomUUID().toString() + "_" + imagen.nbimagen.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
            String pathInBucket = "propiedades/" + imagen.casaid + "/" + filename;
            String uploadUrl = DBHelper.getSupabaseUrl() + "/storage/v1/object/imagenesmeljo/" + pathInBucket;
            String mime = ctx.getContentResolver().getType(uri);
            if (mime == null) mime = "application/octet-stream";

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
                    imagen.urimagen = DBHelper.getSupabaseUrl() + "/storage/v1/object/public/imagenesmeljo/" + pathInBucket;
                    imagen.datemod = DBHelper.obtenerFechaActual();
                    doPatch.run();
                }
            });

        } catch (Exception ex) {
            callback.onError("Excepción subir nuevo archivo: " + ex.getMessage());
        }
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
                runOnMain(() -> callback.onError("Error al obtener imagen: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnMain(() -> callback.onError("No se pudo obtener imagen. Código: " + (response != null ? response.code() : -1)));
                    return;
                }

                String body = response.body().string();
                JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                if (arr.isEmpty()) {
                    runOnMain(() -> callback.onError("Imagen no encontrada"));
                    return;
                }

                JsonObject obj = arr.get(0).getAsJsonObject();
                final String urimagen = obj.has("urimagen") && !obj.get("urimagen").isJsonNull() ? obj.get("urimagen").getAsString() : null;

                // Eliminar registro
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
                        runOnMain(() -> callback.onError("Error al eliminar registro: " + e.getMessage()));
                    }

                    @Override
                    public void onResponse(Call call2, Response response2) throws IOException {
                        if (!response2.isSuccessful()) {
                            runOnMain(() -> callback.onError("Fallo al eliminar registro: " + response2.code()));
                            return;
                        }

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
                                        runOnMain(() -> callback.onExito("Registro eliminado. Archivo: fallo al eliminar del bucket (" + e.getMessage() + ")"));
                                    }

                                    @Override
                                    public void onResponse(Call call3, Response response3) throws IOException {
                                        runOnMain(() -> callback.onExito(
                                                response3.isSuccessful() ? "Imagen eliminada correctamente" :
                                                        "Registro eliminado. Archivo no eliminado: " + response3.code()
                                        ));
                                    }
                                });
                                return;
                            }
                        }

                        runOnMain(() -> callback.onExito("Imagen eliminada (registro)"));
                    }
                });
            }
        });
    }

    public void obtenerMultimediaEstado(final String nbcasa, final AppCallback callback) {
        final boolean[] tieneFotos = {false};
        final boolean[] tieneVideos = {false};

        obtenerImagenesPorCasa(nbcasa, "imagen", new AppCallback() {
            @Override
            public void onImagenFragYcatalogoExito(List<Imagen> imagenes) {
                tieneFotos[0] = imagenes != null && !imagenes.isEmpty();
                obtenerImagenesPorCasa(nbcasa, "video", new AppCallback() {
                    @Override
                    public void onImagenFragYcatalogoExito(List<Imagen> videos) {
                        tieneVideos[0] = videos != null && !videos.isEmpty();
                        runOnMain(() -> callback.onMultimediaExito(tieneFotos[0], tieneVideos[0]));
                    }

                    @Override
                    public void onError(String mensaje) {
                        runOnMain(() -> callback.onError(mensaje));
                    }
                });
            }

            @Override
            public void onError(String mensaje) {
                runOnMain(() -> callback.onError(mensaje));
            }
        });
    }
}
