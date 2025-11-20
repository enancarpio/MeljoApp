package com.example.meljo.views.user;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Usuario;
import com.example.meljo.utils.TextWatcherUtils;
import com.example.meljo.utils.ValidatorUtils;

public class UsuarioEditarDialogFragment extends DialogFragment {

    private final DBHelper dbHelper;
    private final Usuario usuario;
    private UsuarioDialogListener listener;

    public UsuarioEditarDialogFragment(Usuario usuario, DBHelper dbHelper) {
        this.usuario = usuario;
        this.dbHelper = dbHelper;
    }

    public void setListener(UsuarioDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.frag_user_edit_dlg, (ViewGroup) null);

        EditText etNombre = view.findViewById(R.id.editNombreUsuario);
        EditText etApellido = view.findViewById(R.id.editApellidoUsuario);
        EditText etEmail = view.findViewById(R.id.editEmailUsuario);
        EditText etFono = view.findViewById(R.id.etFono);
        EditText etUsername = view.findViewById(R.id.editUsernameUsuario);
        EditText etPassword = view.findViewById(R.id.editPasswordUsuario);
        Spinner spinnerAdmin = view.findViewById(R.id.spinnerAdminUsuario);

        // Configuración spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Usuario", "Administrador"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAdmin.setAdapter(adapter);
        spinnerAdmin.setSelection(usuario.admin ? 1 : 0);

        // Set datos actuales
        etNombre.setText(usuario.nb);
        etApellido.setText(usuario.ape);
        etEmail.setText(usuario.email);
        etFono.setText(usuario.fono);
        etUsername.setText(usuario.username);
        etPassword.setText(usuario.password);

        // Validaciones en tiempo real
        TextWatcherUtils.agregarTextWatcherSoloLetras(etNombre);
        TextWatcherUtils.agregarTextWatcherSoloLetras(etApellido);
        TextWatcherUtils.agregarTextWatcherSoloLetras(etUsername);
        TextWatcherUtils.agregarTextWatcherNumerico(etFono);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Editar Usuario")
                .setView(view)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> guardarCambios(etNombre, etApellido, etFono, etPassword, spinnerAdmin, dialog));
        });

        return dialog;
    }

    private void guardarCambios(EditText etNombre, EditText etApellido, EditText etFono,
                                EditText etPassword, Spinner spinnerAdmin, AlertDialog dialog) {

        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String telefono = etFono.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

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
        if (telefono.isEmpty()) {
            etFono.setError("Campo obligatorio");
            etFono.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Campo obligatorio");
            etPassword.requestFocus();
            return;
        }

        if (ValidatorUtils.validarNombreApellido(nombre, etNombre)
                && ValidatorUtils.validarNombreApellido(apellido, etApellido)
                && ValidatorUtils.validarTelefono(telefono, etFono)
                && ValidatorUtils.validarContrasena(password, etPassword)) {

            usuario.nb = nombre;
            usuario.ape = apellido;
            usuario.fono = telefono;
            usuario.password = password;
            usuario.datemod = DBHelper.obtenerFechaActual();
            usuario.admin = spinnerAdmin.getSelectedItemPosition() == 1;

            dbHelper.actualizarUsuario(usuario, new AppCallback() {
                @Override
                public void onExito(String mensaje) {
                    FragmentActivity activity = requireActivity();
                    activity.runOnUiThread(() -> {
                        if (listener != null) listener.onUsuarioActualizado();
                        dialog.dismiss();
                    });
                }

                @Override
                public void onError(String mensaje) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error al actualizar el usuario", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }
}
