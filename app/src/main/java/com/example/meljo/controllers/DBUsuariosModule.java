package com.example.meljo.controllers;

import com.example.meljo.models.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * DBUsuariosModule simplificado para usar exclusivamente DBHelper.
 */
public class DBUsuariosModule {

    public DBUsuariosModule(DBHelper helper) {
        // No necesitamos instancia, DBHelper es estático.
    }

    // ---------------------------------------------------------
    // Conversión JSON ↔ Usuario
    // ---------------------------------------------------------

    private Usuario jsonToUsuario(JsonObject obj) {
        return new Usuario(
                DBHelper.getStringSafe(obj, "userid"),
                DBHelper.getStringSafe(obj, "email"),
                DBHelper.getStringSafe(obj, "nb"),
                DBHelper.getStringSafe(obj, "ape"),
                DBHelper.getStringSafe(obj, "username"),
                DBHelper.getStringSafe(obj, "password"),
                obj.has("admin") && obj.get("admin").getAsBoolean(),
                DBHelper.getStringSafe(obj, "datereg"),
                DBHelper.getStringSafe(obj, "datemod"),
                DBHelper.getStringSafe(obj, "fono")
        );
    }

    private List<Usuario> parseUsuarios(String body) {
        List<Usuario> lista = new ArrayList<>();
        if (body == null || body.isEmpty() || body.equals("null")) return lista;

        JsonArray array = JsonParser.parseString(body).getAsJsonArray();

        for (JsonElement e : array) {
            lista.add(jsonToUsuario(e.getAsJsonObject()));
        }
        return lista;
    }

    // ---------------------------------------------------------
    // AUTENTICACIÓN FIREBASE + SUPABASE
    // ---------------------------------------------------------

    public void verificarCredenciales(String email, String password, AppCallback callback) {
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError("Credenciales inválidas");
                        return;
                    }

                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser == null) {
                        callback.onError("Usuario Firebase no encontrado");
                        return;
                    }

                    getUsuarioPorUid(firebaseUser.getUid(), callback);
                });
    }

    // ---------------------------------------------------------
    // CRUD
    // ---------------------------------------------------------

    public void getUsuarioPorUid(String uid, AppCallback callback) {

        String url = DBHelper.getSupabaseUrl() + "/rest/v1/usuarios?userid=eq." + uid;

        Request req = DBHelper.construirRequest(url, "GET", null, false);

        DBHelper.ejecutarRequest(
                req,
                true,
                body -> {
                    List<Usuario> lista = parseUsuarios(body);
                    if (lista.isEmpty()) callback.onError("Usuario no encontrado");
                    else callback.onUsuarioVerificado(lista.get(0));
                },
                callback::onError
        );
    }

    public void getAllUsuarios(AppCallback callback) {

        String url = DBHelper.getSupabaseUrl() + "/rest/v1/usuarios?select=*";

        Request req = DBHelper.construirRequest(url, "GET", null, false);

        DBHelper.ejecutarRequest(
                req,
                true,
                body -> callback.onUsuariosCargados(parseUsuarios(body)),
                callback::onError
        );
    }

    public void eliminarUsuario(Usuario usuario, AppCallback callback) {
        eliminarUsuarioDeSupabase(usuario.userid, callback);
    }

    private void eliminarUsuarioDeSupabase(String userid, AppCallback callback) {

        String url = DBHelper.getSupabaseUrl() + "/rest/v1/usuarios?userid=eq." + userid;

        Request req = DBHelper.construirRequest(url, "DELETE", null, false);

        DBHelper.ejecutarRequest(
                req,
                false,
                body -> callback.onUsuarioEliminado(true),
                error -> callback.onUsuarioEliminado(false)
        );
    }

    public static void insertarUsuarioStatic(Usuario u, AppCallback callback) {

        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.createUserWithEmailAndPassword(u.email, u.password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        String msg = task.getException() != null ?
                                task.getException().getMessage() :
                                "Error desconocido Firebase";
                        callback.onError("Error al crear usuario en Firebase: " + msg);
                        return;
                    }

                    FirebaseUser fbUser = task.getResult().getUser();
                    if (fbUser == null) {
                        callback.onError("UID Firebase es nulo");
                        return;
                    }

                    u.userid = fbUser.getUid();

                    // JSON Supabase
                    JsonObject json = new JsonObject();
                    json.addProperty("userid", u.userid);
                    json.addProperty("email", u.email);
                    json.addProperty("nb", u.nb);
                    json.addProperty("ape", u.ape);
                    json.addProperty("fono", u.fono);
                    json.addProperty("username", u.username);
                    json.addProperty("password", u.password);
                    json.addProperty("admin", u.admin);

                    Request req = new Request.Builder()
                            .url(DBHelper.getSupabaseUrl() + "/rest/v1/usuarios")
                            .addHeader("apikey", DBHelper.getSupabaseApiKey())
                            .addHeader("Authorization", DBHelper.getSupabaseJwt())
                            .addHeader("Prefer", "return=representation")
                            .post(RequestBody.create(
                                    json.toString(),
                                    MediaType.parse("application/json")
                            ))
                            .build();

                    DBHelper.ejecutarRequest(
                            req,
                            true,
                            body -> {
                                JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                                if (!arr.isEmpty()) {
                                    callback.onUsuarioInsertado(
                                            arr.get(0).getAsJsonObject().get("id").getAsLong()
                                    );
                                } else callback.onError("No se devolvió usuario tras insertarlo");
                            },
                            callback::onError
                    );
                });
    }

    public void actualizarUsuario(Usuario u, AppCallback callback) {

        JsonObject json = new JsonObject();
        json.addProperty("nb", u.nb);
        json.addProperty("ape", u.ape);
        json.addProperty("password", u.password);
        json.addProperty("admin", u.admin);
        json.addProperty("datemod", u.datemod);
        json.addProperty("fono", u.fono);

        String url = DBHelper.getSupabaseUrl() + "/rest/v1/usuarios?userid=eq." + u.userid;

        Request req = DBHelper.construirRequest(url, "PATCH", json, true);

        DBHelper.ejecutarRequest(
                req,
                false,
                body -> callback.onExito("Usuario actualizado correctamente"),
                callback::onError
        );
    }
}
