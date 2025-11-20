package com.example.meljo.views.property;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Propiedad;
import com.example.meljo.utils.TextWatcherUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PropiedadNuevaDialogFragment extends DialogFragment {

    private EditText etNombre, etPrecio, etDescripcion, etCuartos, etAseos, etMetros, etDireccion;
    private Button btnSeleccionarUbicacion;
    private FusedLocationProviderClient fusedLocationClient;
    private PropiedadDialogListener listener;

    private double latitud = 0.0;
    private double longitud = 0.0;
    private String direccion = "";

    public void setPropiedadDialogListener(PropiedadDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.frag_property_new_dlg, null);

        etNombre = view.findViewById(R.id.etNbcasa);
        etPrecio = view.findViewById(R.id.etPrecio);
        etDescripcion = view.findViewById(R.id.etDescripcion);
        etCuartos = view.findViewById(R.id.etCuartos);
        etAseos = view.findViewById(R.id.etAseos);
        etMetros = view.findViewById(R.id.etMetros);
        etDireccion = view.findViewById(R.id.etDireccion);
        btnSeleccionarUbicacion = view.findViewById(R.id.btnSeleccionarUbicacion);

        TextWatcherUtils.agregarTextWatcherCodigo(etNombre);
        TextWatcherUtils.agregarTextWatcherCodigo(etDescripcion);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        btnSeleccionarUbicacion.setOnClickListener(v -> verificarGPSYObtenerUbicacion());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Nueva Propiedad")
                .setView(view)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(v -> guardarPropiedad());
        });

        return dialog;
    }

    private void verificarGPSYObtenerUbicacion() {
        String direccionManual = etDireccion.getText().toString().trim();
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Obteniendo ubicación...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Si el usuario introdujo una dirección manual, la geocodificamos
        if (!direccionManual.isEmpty()) {
            Toast.makeText(getContext(), "Buscando dirección ingresada...", Toast.LENGTH_SHORT).show();
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> resultados = geocoder.getFromLocationName(direccionManual, 1);
                if (!resultados.isEmpty()) {
                    Address resultado = resultados.get(0);
                    latitud = resultado.getLatitude();
                    longitud = resultado.getLongitude();
                    direccion = resultado.getAddressLine(0);
                    progressDialog.dismiss();
                    abrirMapa();
                    return;
                }
                Toast.makeText(getContext(), "Dirección no disponible, se cargará la ubicación ACTUAL", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(getContext(), "Error al buscar dirección. Se usará la ubicación actual.", Toast.LENGTH_SHORT).show();
            }
        }

        // Si no hay dirección manual, usamos el GPS
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(LocationManager.class);
        boolean gpsActivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsActivo) {
            progressDialog.dismiss();
            Toast.makeText(getContext(), "GPS desactivado. Actívalo para obtener la ubicación.", Toast.LENGTH_LONG).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != 0 &&
                ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != 0) {
            progressDialog.dismiss();
            Toast.makeText(getContext(), "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    progressDialog.dismiss();
                    if (location != null) {
                        latitud = location.getLatitude();
                        longitud = location.getLongitude();
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        try {
                            List<Address> direcciones = geocoder.getFromLocation(latitud, longitud, 1);
                            if (!direcciones.isEmpty()) {
                                direccion = direcciones.get(0).getAddressLine(0);
                            } else {
                                direccion = "Dirección no disponible";
                            }
                        } catch (IOException e) {
                            direccion = "Dirección no disponible";
                        }
                    } else {
                        Toast.makeText(getContext(), "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                    }
                    abrirMapa();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Error obteniendo ubicación", Toast.LENGTH_SHORT).show();
                    abrirMapa();
                });
    }

    private void abrirMapa() {
        PropiedadSeleccionMapaDialogFragment mapaDialog =
                PropiedadSeleccionMapaDialogFragment.newInstance(latitud, longitud, direccion, false);

        mapaDialog.setOnUbicacionSeleccionadaListener((lat, lon, dir) -> {
            latitud = lat;
            longitud = lon;
            direccion = dir;
            etDireccion.setText(direccion);
        });

        mapaDialog.show(getParentFragmentManager(), "SeleccionMapa");
    }

    private void guardarPropiedad() {
        String nombre = etNombre.getText().toString().trim();
        String precioStr = etPrecio.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String direccionStr = etDireccion.getText().toString().trim();

        double precio = precioStr.isEmpty() ? 0.0 : Double.parseDouble(precioStr);
        int cuartos = etCuartos.getText().toString().trim().isEmpty() ? 0 : Integer.parseInt(etCuartos.getText().toString().trim());
        int aseos = etAseos.getText().toString().trim().isEmpty() ? 0 : Integer.parseInt(etAseos.getText().toString().trim());
        int metros = etMetros.getText().toString().trim().isEmpty() ? 0 : Integer.parseInt(etMetros.getText().toString().trim());

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
        if (direccionStr.isEmpty()) {
            etDireccion.setError("Campo obligatorio ó elija ubicación.");
            etDireccion.requestFocus();
            return;
        }

        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Propiedad nueva = new Propiedad(nombre, precio, descripcion, fecha, fecha, false, metros, cuartos, aseos, direccionStr, latitud, longitud);

        new DBHelper().insertarPropiedad(nueva, new AppCallback() {
            @Override
            public void onPropNewEditExito(String mensaje, Propiedad propiedad) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onPropiedadGuardada(propiedad);
                    dismiss();
                });
            }

            @Override
            public void onError(String mensaje) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
