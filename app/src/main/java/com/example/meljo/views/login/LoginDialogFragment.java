package com.example.meljo.views.login;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Usuario;
import com.example.meljo.utils.ValidatorUtils;
import com.example.meljo.views.user.UsuarioNuevoDialogFragment;

public class LoginDialogFragment extends DialogFragment {

    private AppCallback appCallback;
    private DBHelper dbHelper;
    private EditText etContrasena;
    private EditText etCorreo;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AppCallback) {
            this.appCallback = (AppCallback) context;
        } else {
            throw new RuntimeException("La actividad debe implementar AppCallback");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dbHelper = new DBHelper();
        View view = LayoutInflater.from(getContext()).inflate(R.layout.frag_login_dlg, null);
        etCorreo = view.findViewById(R.id.etCorreo);
        etContrasena = view.findViewById(R.id.etContrasena);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Iniciar Sesión")
                .setView(view)
                .setPositiveButton("Ingresar", null)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Registro", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnIngresar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnRegistro = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            btnIngresar.setOnClickListener(v -> validarYVerificarLogin(dialog));
            btnRegistro.setOnClickListener(v -> {
                UsuarioNuevoDialogFragment registroDialog = new UsuarioNuevoDialogFragment(true);
                registroDialog.show(getParentFragmentManager(), "registro");
                dialog.dismiss();
            });
        });

        return dialog;
    }

    private void validarYVerificarLogin(Dialog dialog) {
        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString().trim();
        boolean valido = true;

        if (correo.isEmpty()) {
            etCorreo.setError("Campo obligatorio");
            etCorreo.requestFocus();
            valido = false;
        } else if (!ValidatorUtils.validarCorreo(correo, etCorreo)) {
            valido = false;
        }

        if (contrasena.isEmpty()) {
            etContrasena.setError("Campo obligatorio");
            etContrasena.requestFocus();
            valido = false;
        }

        if (valido) {
            verificarLogin(correo, contrasena, dialog);
        }
    }

    private void verificarLogin(String correo, String contrasena, Dialog dialog) {
        dbHelper.verificarCredenciales(correo, contrasena, new AppCallback() {
            @Override
            public void onUsuarioVerificado(Usuario usuario) {
                Log.d("LoginDialog", "Usuario verificado: " + usuario.nb + " admin=" + usuario.admin);
                if (appCallback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        appCallback.onLoginExitoso(usuario);
                        dialog.dismiss();
                    });
                }
            }

            @Override
            public void onError(String mensaje) {
                Log.e("LoginDialog", "Error en verificación: " + mensaje);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Context context = isAdded() ? getContext() : (appCallback instanceof Context ? (Context) appCallback : null);
                    if (context != null) {
                        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
