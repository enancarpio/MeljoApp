package com.example.meljo.network;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

/**
 * Servicio de conexión con Supabase.
 * Se encarga de realizar peticiones HTTP (GET, POST, PUT, DELETE)
 * y devolver las respuestas en formato JSON.
 */
public class SupabaseService {

    private static final String TAG = "SupabaseService";

    // Cliente HTTP reutilizable para mejorar rendimiento
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build();

    // Credenciales y URLs base (ajusta si usas variables de entorno o configuración externa)
    private final String supabaseUrl;
    private final String supabaseKey;

    public SupabaseService(String supabaseUrl, String supabaseKey) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
    }

    /** Construye la cabecera común para todas las peticiones */
    private Headers buildHeaders() {
        return new Headers.Builder()
                .add("apikey", supabaseKey)
                .add("Authorization", "Bearer " + supabaseKey)
                .add("Content-Type", "application/json")
                .build();
    }

    /**
     * Ejecuta una petición GET a Supabase.
     * @param table Nombre de la tabla
     * @param filter Filtros opcionales tipo querystring (por ejemplo: "?id=eq.1")
     */
    public JSONArray get(String table, String filter) {
        String url = supabaseUrl + "/rest/v1/" + table + (filter != null ? filter : "");
        Request request = new Request.Builder()
                .url(url)
                .headers(buildHeaders())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                Log.e(TAG, "Error GET (" + response.code() + "): " + body);
                return new JSONArray();
            }
            return new JSONArray(body);
        } catch (Exception e) {
            Log.e(TAG, "Excepción en GET: " + e.getMessage(), e);
            return new JSONArray();
        }
    }

    /**
     * Ejecuta una petición POST para insertar datos.
     * @param table Tabla destino
     * @param jsonData Datos en formato JSON
     */
    public JSONObject post(String table, JSONObject jsonData) {
        String url = supabaseUrl + "/rest/v1/" + table;
        RequestBody body = RequestBody.create(jsonData.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .headers(buildHeaders())
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                Log.e(TAG, "Error POST (" + response.code() + "): " + responseBody);
                return new JSONObject().put("error", responseBody);
            }
            return new JSONObject(responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Excepción en POST: " + e.getMessage(), e);
            return new JSONObject();
        }
    }

    /**
     * Ejecuta una petición PUT (actualización).
     * @param table Tabla destino
     * @param filter Filtro tipo querystring (por ejemplo "?id=eq.10")
     * @param jsonData Datos nuevos
     */
    public JSONObject put(String table, String filter, JSONObject jsonData) {
        String url = supabaseUrl + "/rest/v1/" + table + (filter != null ? filter : "");
        RequestBody body = RequestBody.create(jsonData.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .headers(buildHeaders())
                .method("PATCH", body) // PATCH = actualización parcial (PUT completa también posible)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                Log.e(TAG, "Error PUT (" + response.code() + "): " + responseBody);
                return new JSONObject().put("error", responseBody);
            }
            return new JSONObject(responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Excepción en PUT: " + e.getMessage(), e);
            return new JSONObject();
        }
    }

    /**
     * Ejecuta una petición DELETE.
     * @param table Tabla destino
     * @param filter Filtro tipo querystring (por ejemplo "?id=eq.5")
     */
    public boolean delete(String table, String filter) {
        String url = supabaseUrl + "/rest/v1/" + table + (filter != null ? filter : "");
        Request request = new Request.Builder()
                .url(url)
                .headers(buildHeaders())
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            boolean ok = response.isSuccessful();
            if (!ok) {
                String body = response.body() != null ? response.body().string() : "";
                Log.e(TAG, "Error DELETE (" + response.code() + "): " + body);
            }
            return ok;
        } catch (IOException e) {
            Log.e(TAG, "Excepción en DELETE: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Ejecuta una petición personalizada (por ejemplo, para RPC o funciones).
     * @param endpoint Ruta completa (ej: "/rest/v1/rpc/mi_funcion")
     * @param jsonData Cuerpo de la petición
     */
    public JSONObject postFunction(String endpoint, JSONObject jsonData) {
        String url = supabaseUrl + endpoint;
        RequestBody body = RequestBody.create(jsonData.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .headers(buildHeaders())
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                Log.e(TAG, "Error RPC (" + response.code() + "): " + responseBody);
                return new JSONObject().put("error", responseBody);
            }
            return new JSONObject(responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Excepción en RPC: " + e.getMessage(), e);
            return new JSONObject();
        }
    }
}
