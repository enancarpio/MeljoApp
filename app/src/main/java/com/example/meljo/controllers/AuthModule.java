package com.example.meljo.controllers;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * Encapsula toda la lógica de autenticación usando FirebaseAuth.
 */
public class AuthModule {

    private static final String TAG = "AuthModule";
    private final FirebaseAuth auth;

    public AuthModule() {
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Retorna el usuario actual logueado, o null si no hay ninguno.
     */
    public FirebaseUser getUsuarioActual() {
        return auth.getCurrentUser();
    }

    /**
     * Login con email y password.
     */
    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(auth.getCurrentUser());
                    } else {
                        callback.onError(task.getException() != null
                                ? task.getException().getMessage()
                                : "Error desconocido al iniciar sesión");
                    }
                });
    }

    /**
     * Registro con email y password.
     */
    public void registrar(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(auth.getCurrentUser());
                    } else {
                        callback.onError(task.getException() != null
                                ? task.getException().getMessage()
                                : "Error desconocido al registrar usuario");
                    }
                });
    }

    /**
     * Cierra sesión del usuario actual.
     */
    public void logout() {
        auth.signOut();
        Log.d(TAG, "Usuario deslogueado correctamente");
    }

    /**
     * Envía email de recuperación de contraseña.
     */
    public void enviarEmailRecuperacion(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onError(task.getException() != null
                                ? task.getException().getMessage()
                                : "Error enviando correo de recuperación");
                    }
                });
    }

    /**
     * Interfaz para callbacks de autenticación.
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String mensaje);
    }
}
