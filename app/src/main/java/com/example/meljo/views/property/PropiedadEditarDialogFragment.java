package com.example.meljo.views.property;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Propiedad;
import com.example.meljo.utils.TextWatcherUtils;

/**
 * Diálogo para editar una propiedad existente.
 */
public class PropiedadEditarDialogFragment extends DialogFragment
        implements PropiedadSeleccionMapaDialogFragment.OnUbicacionSeleccionadaListener {

    private EditText etNombre, etPrecio, etDescripcion, etCuartos, etAseos, etMetros, etDireccion;
    private Button btnSeleccionarUbicacion;
    private DBHelper dbHelper;
    private Propiedad propiedad;
    private PropiedadDialogListener listener;
    private CheckBox chbVendida;

    // 1. CONSTRUCTOR VACÍO OBLIGATORIO PARA EVITAR EL CRASH
    public PropiedadEditarDialogFragment() {
    }

    // 2. METODO ESTÁTICO PARA INSTANCIAR PASANDO LA PROPIEDAD
    public static PropiedadEditarDialogFragment newInstance(Propiedad propiedad) {
        PropiedadEditarDialogFragment fragment = new PropiedadEditarDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("ARG_PROPIEDAD", propiedad); // Asegúrate de que Propiedad implemente Serializable o Parcelable
        fragment.setArguments(args);
        return fragment;
    }

    // 3. SETTERS PARA LOS COMPONENTES QUE NO VAN EN EL BUNDLE
    public void setListener(PropiedadDialogListener listener) {
        this.listener = listener;
    }

    public void setDbHelper(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 4. RECUPERAR LA PROPIEDAD SI EL SISTEMA RECREA EL FRAGMENTO
        if (getArguments() != null) {
            propiedad = (Propiedad) getArguments().getSerializable("ARG_PROPIEDAD");
        }

        // Inicializar dbHelper si fue destruido
        if (dbHelper == null) {
            dbHelper = new DBHelper();
        }
/*
        // Inicializar dbHelper si fue destruido (asumiendo que tiene singleton o constructor con context)
        if (dbHelper == null) {
            dbHelper = new DBHelper(requireContext());
        }

 */

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.frag_property_edit_dlg, (ViewGroup) null);

        // Referencias UI
        etNombre = view.findViewById(R.id.etNbcasa);
        etPrecio = view.findViewById(R.id.etPrecio);
        etDescripcion = view.findViewById(R.id.etDescripcion);
        etCuartos = view.findViewById(R.id.etCuartos);
        etAseos = view.findViewById(R.id.etAseos);
        etMetros = view.findViewById(R.id.etMetros);
        etDireccion = view.findViewById(R.id.etDireccion);
        btnSeleccionarUbicacion = view.findViewById(R.id.btnSeleccionarUbicacion);
        chbVendida = view.findViewById(R.id.chbVendida);

        // Validar que la propiedad no sea nula antes de cargar
        if (propiedad != null) {
            etNombre.setText(propiedad.casaid);
            etPrecio.setText(String.valueOf(propiedad.precio));
            etDescripcion.setText(propiedad.descripcion);
            etCuartos.setText(String.valueOf(propiedad.cuartos));
            etAseos.setText(String.valueOf(propiedad.aseos));
            etMetros.setText(String.valueOf(propiedad.metros));
            etDireccion.setText(propiedad.direccion);
            chbVendida.setChecked(propiedad.vendido);
        }

        // TextWatcher
        TextWatcherUtils.agregarTextWatcherCodigo(etNombre);
        TextWatcherUtils.agregarTextWatcherCodigo(etDescripcion);

        // Botón para abrir el mapa
        btnSeleccionarUbicacion.setOnClickListener(v -> abrirSelectorUbicacion());

        // Crear el diálogo
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Editar Propiedad")
                .setView(view)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        // Personalizar botón Guardar (para validar antes de cerrar)
        dialog.setOnShowListener(d -> {
            Button btnGuardar = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(v -> guardarCambios(dialog));
        });

        return dialog;
    }

    /**
     * Abre el selector de ubicación en el mapa.
     */
    private void abrirSelectorUbicacion() {
        if (propiedad == null) return;

        PropiedadSeleccionMapaDialogFragment mapaDialog =
                PropiedadSeleccionMapaDialogFragment.newInstance(
                        propiedad.getLatitud(),
                        propiedad.getLongitud(),
                        propiedad.getDireccion(),
                        false
                );

        mapaDialog.setOnUbicacionSeleccionadaListener(this);
        mapaDialog.show(getParentFragmentManager(), "SeleccionMapa");
    }

    /**
     * Valida los campos y guarda los cambios.
     */
    private void guardarCambios(AlertDialog dialog) {
        if (propiedad == null) return;

        String nombre = etNombre.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();

        double precio = etPrecio.getText().toString().trim().isEmpty()
                ? 0.0 : Double.parseDouble(etPrecio.getText().toString().trim());
        int cuartos = etCuartos.getText().toString().trim().isEmpty()
                ? 0 : Integer.parseInt(etCuartos.getText().toString().trim());
        int aseos = etAseos.getText().toString().trim().isEmpty()
                ? 0 : Integer.parseInt(etAseos.getText().toString().trim());
        int metros = etMetros.getText().toString().trim().isEmpty()
                ? 0 : Integer.parseInt(etMetros.getText().toString().trim());

        // Validaciones
        if (nombre.isEmpty()) {
            etNombre.setError("Campo obligatorio");
            etNombre.requestFocus();
            return;
        }
        if (precio == 0.0) {
            etPrecio.setError("Campo obligatorio");
            etPrecio.requestFocus();
            return;
        }
        if (descripcion.isEmpty()) {
            etDescripcion.setError("Campo obligatorio");
            etDescripcion.requestFocus();
            return;
        }
        if (metros == 0) {
            etMetros.setError("Campo obligatorio");
            etMetros.requestFocus();
            return;
        }
        if (direccion.isEmpty()) {
            etDireccion.setError("Campo obligatorio o elija ubicación.");
            etDireccion.requestFocus();
            return;
        }

        // Asignar valores
        propiedad.casaid = nombre;
        propiedad.descripcion = descripcion;
        propiedad.direccion = direccion;
        propiedad.precio = precio;
        propiedad.cuartos = cuartos;
        propiedad.aseos = aseos;
        propiedad.metros = metros;
        propiedad.vendido = chbVendida.isChecked();

        // Actualizar en base de datos
        dbHelper.actualizarPropiedad(propiedad, new AppCallback() {
            @Override
            public void onPropNewEditExito(String mensaje, Propiedad propiedadActualizada) {
                if (listener != null) listener.onPropiedadActualizada(propiedadActualizada);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show()
                    );
                }
                dialog.dismiss();
            }

            @Override
            public void onError(String mensaje) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    /**
     * Callback al seleccionar una ubicación en el mapa.
     */
    @Override
    public void onUbicacionSeleccionada(double latitud, double longitud, String direccion) {
        if (propiedad != null) {
            propiedad.latitud = latitud;
            propiedad.longitud = longitud;
            propiedad.direccion = direccion;
            etDireccion.setText(direccion);
        }
    }
}
