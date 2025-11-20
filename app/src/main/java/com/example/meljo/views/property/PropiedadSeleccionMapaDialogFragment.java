package com.example.meljo.views.property;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.example.meljo.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PropiedadSeleccionMapaDialogFragment extends DialogFragment implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 2001;

    private OnUbicacionSeleccionadaListener callback;
    private String direccionInicial;
    private double latitudInicial;
    private double longitudInicial;
    private GoogleMap mMap;
    private Marker marcador;
    private boolean soloVisualizacion = false;

    // Interfaz para devolver la ubicación seleccionada
    public interface OnUbicacionSeleccionadaListener {
        void onUbicacionSeleccionada(double latitud, double longitud, String direccion);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnUbicacionSeleccionadaListener) {
            callback = (OnUbicacionSeleccionadaListener) context;
        } else {
            // Si el contexto no implementa la interfaz, se puede asignar manualmente luego
            Toast.makeText(context, "Implementa OnUbicacionSeleccionadaListener o usa setOnUbicacionSeleccionadaListener()", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_property_selec_mapa_dlg, container, false);

        if (getArguments() != null) {
            latitudInicial = getArguments().getDouble("latitud", 0.0);
            longitudInicial = getArguments().getDouble("longitud", 0.0);
            direccionInicial = getArguments().getString("direccion", "");
            soloVisualizacion = getArguments().getBoolean("soloVisualizacion", false);
        }

        Button btnConfirmar = view.findViewById(R.id.btnConfirmar);
        btnConfirmar.setVisibility(soloVisualizacion ? View.GONE : View.VISIBLE);

        btnConfirmar.setOnClickListener(v -> {
            if (marcador != null && callback != null) {
                LatLng pos = marcador.getPosition();
                String direccion = obtenerDireccionDesdeLatLng(pos);
                callback.onUbicacionSeleccionada(pos.latitude, pos.longitude, direccion);
                dismiss();
            } else {
                Toast.makeText(getContext(), "Toca el mapa para seleccionar una ubicación", Toast.LENGTH_SHORT).show();
            }
        });

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commitNow();
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION_PERMISSION);
            return;
        }

        habilitarUbicacionEnMapa();

        LatLng ubicacion = new LatLng(latitudInicial, longitudInicial);
        marcador = mMap.addMarker(new MarkerOptions().position(ubicacion).title(direccionInicial));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15f));

        if (!soloVisualizacion) {
            mMap.setOnMapClickListener(latLng -> {
                if (marcador != null) marcador.remove();
                marcador = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Ubicación seleccionada"));
            });
        }
    }

    private void habilitarUbicacionEnMapa() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                habilitarUbicacionEnMapa();
                Toast.makeText(getContext(), "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String obtenerDireccionDesdeLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> direcciones = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (direcciones != null && !direcciones.isEmpty()) {
                return direcciones.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Dirección no disponible";
    }

    // Método estático para crear una nueva instancia del fragmento
    public static PropiedadSeleccionMapaDialogFragment newInstance(double latitud, double longitud, String direccion, boolean soloVisualizacion) {
        PropiedadSeleccionMapaDialogFragment fragment = new PropiedadSeleccionMapaDialogFragment();
        Bundle args = new Bundle();
        args.putDouble("latitud", latitud);
        args.putDouble("longitud", longitud);
        args.putString("direccion", direccion);
        args.putBoolean("soloVisualizacion", soloVisualizacion);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnUbicacionSeleccionadaListener(OnUbicacionSeleccionadaListener listener) {
        this.callback = listener;
    }
}
