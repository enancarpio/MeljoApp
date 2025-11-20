package com.example.meljo.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/* loaded from: classes8.dex */
public class TextWatcherUtils {
    public static void agregarTextWatcherSoloLetras(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() { // from class: com.example.meljo.utils.TextWatcherUtils.1
            private String anterior = "";

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                this.anterior = s.toString();
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
                editText.removeTextChangedListener(this);
                String filtrado = s.toString().replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑ ]", "");
                if (!filtrado.equals(s.toString())) {
                    s.replace(0, s.length(), filtrado);
                }
                if (!filtrado.isEmpty()) {
                    String capitalizado = filtrado.substring(0, 1).toUpperCase() + filtrado.substring(1);
                    if (!capitalizado.equals(filtrado)) {
                        editText.setText(capitalizado);
                        editText.setSelection(capitalizado.length());
                    }
                }
                editText.addTextChangedListener(this);
            }
        });
    }

    public static void agregarTextWatcherNumerico(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() { // from class: com.example.meljo.utils.TextWatcherUtils.2
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
                String filtrado = s.toString().replaceAll("[^0-9]", "");
                if (!s.toString().equals(filtrado)) {
                    editText.setText(filtrado);
                    editText.setSelection(filtrado.length());
                }
            }
        });
    }

    public static void agregarTextWatcherCodigo(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() { // from class: com.example.meljo.utils.TextWatcherUtils.3
            private String anterior = "";

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                this.anterior = s.toString();
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
                editText.removeTextChangedListener(this);
                String texto = s.toString();
                String filtrado = texto.replaceAll("[^a-zA-Z0-9_.\\-,]", "");
                if (filtrado.length() > 200) {
                    filtrado = filtrado.substring(0, 200);
                }
                if (!filtrado.isEmpty() && Character.isLetter(filtrado.charAt(0))) {
                    filtrado = Character.toUpperCase(filtrado.charAt(0)) + filtrado.substring(1);
                }
                if (filtrado.length() < 1) {
                    editText.setError("Dato obligatorio.");
                } else {
                    editText.setError(null);
                }
                if (!filtrado.equals(texto)) {
                    editText.setText(filtrado);
                    editText.setSelection(filtrado.length());
                }
                editText.addTextChangedListener(this);
            }
        });
    }
}
