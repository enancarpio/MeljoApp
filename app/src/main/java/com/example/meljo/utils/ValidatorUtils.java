package com.example.meljo.utils;

import android.widget.EditText;

/* loaded from: classes8.dex */
public class ValidatorUtils {
    public static boolean validarNombreApellido(String texto, EditText campo) {
        if (!texto.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]{1,30}")) {
            campo.setError("Solo letras. Máx. 30 caracteres.");
            campo.requestFocus();
            return false;
        }
        String[] palabras = texto.trim().split("\\s+");
        if (palabras.length > 4) {
            campo.setError("Máximo 4 palabras");
            campo.requestFocus();
            return false;
        }
        return true;
    }

    public static boolean validarCorreo(String correo, EditText campo) {
        if (!correo.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
            campo.setError("Correo no válido");
            campo.requestFocus();
            return false;
        }
        return true;
    }

    public static boolean validarTelefono(String telefono, EditText campo) {
        if (!telefono.matches("^\\d{9}$")) {
            campo.setError("Debe contener exactamente 9 dígitos");
            campo.requestFocus();
            return false;
        }
        return true;
    }

    public static boolean validarUsuario(String texto, EditText campo) {
        if (!texto.matches("[a-zA-ZñÑáéíóúÁÉÍÓÚ]{6,15}")) {
            campo.setError("Solo letras. 6 a 15 caracteres.");
            campo.requestFocus();
            return false;
        }
        return true;
    }

    public static boolean validarContrasena(String contrasena, EditText campo) {
        if (!contrasena.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!?.*()_\\-]).{8,12}$")) {
            campo.setError("Debe tener 8-12 caracteres, mayúscula, minúscula, número y símbolo.");
            campo.requestFocus();
            return false;
        }
        return true;
    }

    public static boolean validarCodigoVarios(String codigo, EditText campo) {
        if (!codigo.matches("^[a-zA-Z0-9_-]{6,15}$")) {
            campo.setError("Debe tener 6-15 caracteres y solo usar letras, números, _ o -");
            campo.requestFocus();
            return false;
        }
        if (Character.isLetter(codigo.charAt(0))) {
            campo.setText(codigo.substring(0, 1).toUpperCase() + codigo.substring(1));
        }
        return true;
    }
}
