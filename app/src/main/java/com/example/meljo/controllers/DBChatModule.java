package com.example.meljo.controllers;

import android.util.Log;

import com.example.meljo.models.Mensaje;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DBChatModule: Agrupa la lógica de chat / Firestore de manera optimizada.
 */
public class DBChatModule {

    private final DBHelper helper;

    public DBChatModule(DBHelper helper) {
        this.helper = helper;
    }

    // Métodos auxiliares

    private FirebaseUser getUsuarioActual() {
        return helper.getAuth().getCurrentUser();
    }

    private boolean ejecutarSiUsuarioLogueado(AppCallback callback, UsuarioRunnable runnable) {
        FirebaseUser user = getUsuarioActual();
        if (user == null) {
            if (callback != null) callback.onError("Usuario no logueado");
            return false;
        }
        runnable.run(user.getUid());
        return true;
    }

    private interface UsuarioRunnable {
        void run(String uid);
    }

    private void asegurarDatosUsuarioEnRaiz(String uid) {
        FirebaseUser currentUser = getUsuarioActual();
        if (currentUser != null && currentUser.getEmail() != null) {
            Map<String, Object> datosUsuario = new HashMap<>();
            datosUsuario.put("correo", currentUser.getEmail());

            helper.getFirestore().collection("mensajes")
                    .document(uid)
                    .set(datosUsuario, com.google.firebase.firestore.SetOptions.merge())
                    .addOnFailureListener(e ->
                            Log.e("DBHelper", "Error guardando datos de usuario en raíz", e)
                    );
        }
    }

    private void insertarMensaje(String tipo, String mensaje, AppCallback callback) {
        FirebaseUser user = getUsuarioActual();
        if (user == null) {
            Log.e("DBHelper", "Usuario no logueado, no se puede guardar mensaje");
            if (callback != null) callback.onError("Usuario no logueado");
            return;
        }

        String uid = user.getUid();
        asegurarDatosUsuarioEnRaiz(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("type", tipo);
        data.put("message", mensaje);
        data.put("timestamp", System.currentTimeMillis());

        helper.getFirestore().collection("mensajes")
                .document(uid)
                .collection("chat")
                .add(data)
                .addOnSuccessListener(aVoid -> Log.d("DBHelper", tipo + " guardado correctamente"))
                .addOnFailureListener(e -> Log.e("DBHelper", "Error guardando " + tipo, e));
    }

    // Métodos públicos

    public void insertUserMessage(String message) {
        insertarMensaje("user", message, null);
    }

    public void insertBotResponse(String mensaje) {
        insertarMensaje("bot", mensaje, null);
    }

    public void clearMessagesFirebase(final AppCallback callback) {
        ejecutarSiUsuarioLogueado(callback, uid -> {
            // Referencia al documento raíz del usuario
            DocumentReference userDocRef = helper.getFirestore().collection("mensajes").document(uid);

            userDocRef.collection("chat").get().addOnSuccessListener(query -> {
                com.google.firebase.firestore.WriteBatch batch = helper.getFirestore().batch();

                // 1. Borrar todos los mensajes individuales
                for (DocumentSnapshot doc : query) {
                    batch.delete(doc.getReference());
                }

                // 2. BORRAR TAMBIÉN EL DOCUMENTO RAÍZ DEL UID
                batch.delete(userDocRef);

                batch.commit()
                        .addOnSuccessListener(aVoid -> callback.onExito("Historial eliminado por completo"))
                        .addOnFailureListener(e -> callback.onError("Error al procesar el borrado: " + e.getMessage()));
            });
        });
    }

    public void getHistorialMensajesLogueado(final MensajesCallback<List<Map<String, Object>>> callback) {
        ejecutarSiUsuarioLogueado(null, uid -> {
            helper.getFirestore().collection("mensajes")
                    .document(uid)
                    .collection("chat")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(query -> {
                        List<Map<String, Object>> mensajes = new ArrayList<>();
                        for (DocumentSnapshot doc : query) mensajes.add(doc.getData());
                        callback.onGetHistoryUserMsgSuccess(mensajes);
                    })
                    .addOnFailureListener(e ->
                            callback.onError("Error al obtener historial de mensajes: " + e.getMessage()));
        });
    }

    public void getHistorialTodosUsuarios(final AppCallback callback) {
        helper.getFirestore().collection("mensajes")
                .get()
                .addOnSuccessListener(usuariosSnap -> {
                    Log.d("HISTORIAL", "Usuarios encontrados en 'mensajes': " + usuariosSnap.size());

                    if (usuariosSnap.isEmpty()) {
                        callback.onHistorialObtenido(new ArrayList<>());
                        return;
                    }

                    List<Mensaje> listaMensajes = new ArrayList<>();
                    AtomicInteger usuariosProcesados = new AtomicInteger(0);
                    int totalUsuarios = usuariosSnap.size();

                    for (DocumentSnapshot usuarioDoc : usuariosSnap) {
                        final String uidFinal = usuarioDoc.getId();
                        final String correoFinal = usuarioDoc.getString("correo") != null
                                ? usuarioDoc.getString("correo")
                                : "correo_desconocido";

                        helper.getFirestore().collection("mensajes")
                                .document(uidFinal)
                                .collection("chat")
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                .get()
                                .addOnSuccessListener(chatSnap -> {
                                    for (DocumentSnapshot mensajeDoc : chatSnap) {
                                        String type = mensajeDoc.getString("type");
                                        String message = mensajeDoc.getString("message");
                                        long timestamp = mensajeDoc.getLong("timestamp") != null
                                                ? mensajeDoc.getLong("timestamp")
                                                : 0L;
                                        listaMensajes.add(new Mensaje(uidFinal, correoFinal, type, message, timestamp));
                                    }
                                    if (usuariosProcesados.incrementAndGet() == totalUsuarios) {
                                        callback.onHistorialObtenido(listaMensajes);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("HISTORIAL", "Error leyendo chat de " + uidFinal, e);
                                    if (usuariosProcesados.incrementAndGet() == totalUsuarios) {
                                        callback.onHistorialObtenido(listaMensajes);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HISTORIAL", "Error leyendo colección 'mensajes'", e);
                    callback.onHistorialObtenidoError(e);
                });
    }
}
