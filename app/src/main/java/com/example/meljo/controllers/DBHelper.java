package com.example.meljo.controllers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.meljo.BuildConfig;
import com.example.meljo.models.Imagen;
import com.example.meljo.models.Propiedad;
import com.example.meljo.models.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.*;
import okio.BufferedSink;


/**
 * DBHelper: fachada + utilidades. Mantiene constantes y métodos utilitarios.
 * Delegará la lógica por módulos: usuarios, propiedades, imágenes, chat.
 */
public class DBHelper {

    // Constantes y configuración
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;
    private static final String SUPABASE_JWT = "Bearer " + SUPABASE_API_KEY;
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final OkHttpClient client = new OkHttpClient();

    // Firebase
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // Módulos (instanciados con referencia a this si necesitan)
    private final DBUsuariosModule usuariosModule;
    private final DBPropiedadesModule propiedadesModule;
    private final DBImagenesModule imagenesModule;
    private final DBChatModule chatModule;

    public DBHelper() {
        this.usuariosModule = new DBUsuariosModule(this);
        this.propiedadesModule = new DBPropiedadesModule(this);
        this.imagenesModule = new DBImagenesModule(this);
        this.chatModule = new DBChatModule(this);
    }

    // ---------------------------
    // UTILIDADES (SE QUEDAN AQUÍ)
    // ---------------------------

    public static String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // createRequestBodyFromStream: lo dejamos en DBHelper porque es utilidad genérica
    private RequestBody createRequestBodyFromStream(final InputStream in, final String contentType) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType != null ? MediaType.parse(contentType) : null;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    sink.write(buffer, 0, read);
                }
            }
        };
    }

    // Exponer client y constantes a módulos si las necesitan
    public static OkHttpClient getClient() {
        return client;
    }

    public static String getSupabaseUrl() {
        return SUPABASE_URL;
    }

    public static String getSupabaseApiKey() {
        return SUPABASE_API_KEY;
    }

    public static String getSupabaseJwt() {
        return SUPABASE_JWT;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    // Permitir acceso a la utilidad createRequestBodyFromStream desde módulos
    RequestBody createRequestBodyFromStreamForModule(InputStream in, String contentType) {
        return createRequestBodyFromStream(in, contentType);
    }

    // ---------------------------
    // FACHADA: métodos públicos exactamente con las mismas firmas
    // ---------------------------

    // Usuarios
    public void verificarCredenciales(String email, String password, final AppCallback callback) {
        usuariosModule.verificarCredenciales(email, password, callback);
    }

    // Nota: insertarUsuario era static originalmente. Mantengo firma static y lo delego.
    public static void insertarUsuario(final Usuario u, final AppCallback callback) {
        DBUsuariosModule.insertarUsuarioStatic(u, callback);
    }

    public void getAllUsuarios(final AppCallback callback) {
        usuariosModule.getAllUsuarios(callback);
    }

    public void eliminarUsuario(Usuario usuario, final AppCallback callback) {
        usuariosModule.eliminarUsuario(usuario, callback);
    }

    public void actualizarUsuario(Usuario usuario, final AppCallback callback) {
        usuariosModule.actualizarUsuario(usuario, callback);
    }

    // Propiedades
    public void insertarPropiedad(final Propiedad p, final AppCallback callback) {
        propiedadesModule.insertarPropiedad(p, callback);
    }

    public void actualizarPropiedad(final Propiedad p, final AppCallback callback) {
        propiedadesModule.actualizarPropiedad(p, callback);
    }

    public void eliminarPropiedad(int id, final AppCallback callback) {
        propiedadesModule.eliminarPropiedad(id, callback);
    }

    public void buscarPropiedad(final String texto, final AppCallback callback) {
        propiedadesModule.buscarPropiedad(texto, callback);
    }

    public void getPropiedadPorId(int id, final AppCallback callback) {
        propiedadesModule.getPropiedadPorId(id, callback);
    }

    public void getTodasPropiedades(final AppCallback callback) {
        propiedadesModule.getTodasPropiedades(callback);
    }

    public void getPropiedadesPorEstado(boolean vendido, final AppCallback callback) {
        propiedadesModule.getPropiedadesPorEstado(vendido, callback);
    }

    public void buscarPropiedadesFiltradas(Map<String, String> filtros, final AppCallback callback) {
        propiedadesModule.buscarPropiedadesFiltradas(filtros, callback);
    }

    // Imágenes
    public void getAllImagenes(final AppCallback callback) {
        imagenesModule.getAllImagenes(callback);
    }

    public void insertarImagen(Context ctx, String imagenUri, final String nombrePropiedad, final String nbimagen,
                               final String fechaRegistro, final String fechaModificacion, final String tipo,
                               AppCallback callback) throws FileNotFoundException {
        imagenesModule.insertarImagen(ctx, imagenUri, nombrePropiedad, nbimagen, fechaRegistro, fechaModificacion, tipo, callback);
    }

    public void eliminarImagen(int id, AppCallback callback) {
        imagenesModule.eliminarImagen(id, callback);
    }

    public void actualizarImagen(Context ctx, final Imagen imagen, String nuevaUriLocal, final AppCallback callback) throws FileNotFoundException {
        imagenesModule.actualizarImagen(ctx, imagen, nuevaUriLocal, callback);
    }

    public void obtenerImagenesPorCasa(String nbcasa, String tipoFiltro, final AppCallback callback) {
        imagenesModule.obtenerImagenesPorCasa(nbcasa, tipoFiltro, callback);
    }

    public void obtenerMultimediaEstado(final String nbcasa, final AppCallback callback) {
        imagenesModule.obtenerMultimediaEstado(nbcasa, callback);
    }

    public void obtenerMultimediaPorTipo(String nbcasa, String tipo, AppCallback callback) {
        imagenesModule.obtenerMultimediaPorTipo(nbcasa, tipo, callback);
    }

    // Chat
    public void insertUserMessage(String message) {
        chatModule.insertUserMessage(message);
    }

    public void insertBotResponse(String mensaje) {
        chatModule.insertBotResponse(mensaje);
    }

    public void clearMessagesFirebase(final AppCallback callback) {
        chatModule.clearMessagesFirebase(callback);
    }

    public void getHistorialMensajesLogueado(final MensajesCallback<List<java.util.Map<String, Object>>> callback) {
        chatModule.getHistorialMensajesLogueado(callback);
    }

    public void getHistorialTodosUsuarios(final AppCallback callback) {
        chatModule.getHistorialTodosUsuarios(callback);
    }

    public static void ejecutarRequest(Request request, boolean esperaBody, Consumer<String> onSuccess, Consumer<String> onError) {
        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnMain(() -> onError.accept("Error de red: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : null;
                if (!response.isSuccessful() || (esperaBody && body == null)) {
                    runOnMain(() -> onError.accept("Error Supabase (" + response.code() + "): " + body));
                    return;
                }
                if (esperaBody) runOnMain(() -> onSuccess.accept(body));
                else runOnMain(() -> onSuccess.accept(null));
            }
        });
    }

    public static void runOnMain(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    public static Request construirRequest(String url, String method, JsonObject json, boolean preferRepresentation) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", getSupabaseApiKey())
                .addHeader("Authorization", getSupabaseJwt());

        if (json != null && ("POST".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            builder.method(method, body);
            if (preferRepresentation) builder.addHeader("Prefer", "return=representation");
        } else if ("DELETE".equalsIgnoreCase(method)) {
            builder.delete();
        } else {
            builder.get();
        }

        return builder.build();
    }

    public static String getStringSafe(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    public static int getIntSafe(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : 0;
    }

    public static double getDoubleSafe(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : 0.0;
    }

    public static boolean getBooleanSafe(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() && obj.get(key).getAsBoolean();
    }

}
