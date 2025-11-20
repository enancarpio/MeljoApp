package com.example.meljo.views.user;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Usuario;
import com.example.meljo.utils.TextWatcherUtils;
import com.example.meljo.utils.ValidatorUtils;

public class UsuarioNuevoDialogFragment extends DialogFragment {

    private DBHelper dbHelper;
    private EditText etNombre, etApellido, etCorreo, etTelefono, etContrasena;
    private UsuarioDialogListener listener;
    private final boolean modoInvitado;

    public UsuarioNuevoDialogFragment(boolean modoInvitado) {
        this.modoInvitado = modoInvitado;
    }

    public void setListener(UsuarioDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dbHelper = new DBHelper();

        View view = LayoutInflater.from(getContext()).inflate(R.layout.frag_user_new_dlg, null);

        etNombre = view.findViewById(R.id.etNombre);
        etApellido = view.findViewById(R.id.etApellido);
        etCorreo = view.findViewById(R.id.etCorreo);
        etTelefono = view.findViewById(R.id.etFono);
        etContrasena = view.findViewById(R.id.etContrasena);

        // Validaciones de entrada
        TextWatcherUtils.agregarTextWatcherSoloLetras(etNombre);
        TextWatcherUtils.agregarTextWatcherSoloLetras(etApellido);
        TextWatcherUtils.agregarTextWatcherNumerico(etTelefono);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Nuevo Usuario")
                .setView(view)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(v -> guardarUsuario());
        });

        return dialog;
    }

    private void guardarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String contrasena = etContrasena.getText().toString().trim();

        // Validación de campos vacíos
        if (nombre.isEmpty()) {
            etNombre.setError("Campo obligatorio");
            etNombre.requestFocus();
            return;
        }
        if (apellido.isEmpty()) {
            etApellido.setError("Campo obligatorio");
            etApellido.requestFocus();
            return;
        }
        if (correo.isEmpty()) {
            etCorreo.setError("Campo obligatorio");
            etCorreo.requestFocus();
            return;
        }
        if (telefono.isEmpty()) {
            etTelefono.setError("Campo obligatorio");
            etTelefono.requestFocus();
            return;
        }
        if (contrasena.isEmpty()) {
            etContrasena.setError("Campo obligatorio");
            etContrasena.requestFocus();
            return;
        }

        // Validaciones específicas
        if (!ValidatorUtils.validarNombreApellido(nombre, etNombre)) return;
        if (!ValidatorUtils.validarNombreApellido(apellido, etApellido)) return;
        if (!ValidatorUtils.validarCorreo(correo, etCorreo)) return;
        if (!ValidatorUtils.validarTelefono(telefono, etTelefono)) return;
        if (!ValidatorUtils.validarContrasena(contrasena, etContrasena)) return;

        // Crear el nuevo usuario
        Usuario nuevoUsuario = new Usuario(
                correo,      // id / username / email
                correo,
                nombre,
                apellido,
                correo,
                contrasena,
                false,
                "",
                "",
                telefono
        );

        DBHelper.insertarUsuario(nuevoUsuario, new AppCallback() {
            @Override
            public void onUsuarioInsertado(long id) {
                requireActivity().runOnUiThread(() -> {
                    if (listener != null) listener.onUsuarioGuardado(null);
                    if (modoInvitado) {
                        Toast.makeText(getContext(), "Registro exitoso", Toast.LENGTH_SHORT).show();
                    }
                    dismiss();
                });
            }

            @Override
            public void onError(String mensaje) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error al guardar el usuario", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}
