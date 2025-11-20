package com.example.meljo.views.catalog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.fragment.app.DialogFragment;

import com.example.meljo.R;

public class CatalogoOrdenDialogFragment extends DialogFragment {

    private OnOrdenAplicadoListener listener;

    public interface OnOrdenAplicadoListener {
        void onOrdenAplicado();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof OnOrdenAplicadoListener) {
            listener = (OnOrdenAplicadoListener) getParentFragment();
        } else if (context instanceof OnOrdenAplicadoListener) {
            listener = (OnOrdenAplicadoListener) context;
        } else {
            throw new RuntimeException("Debe implementar OnOrdenAplicadoListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_catalog_orden_dlg, container, false);

        RadioGroup rgOrden = view.findViewById(R.id.rgOrden);
        Button btnAplicar = view.findViewById(R.id.btnAplicarOrden);

        SharedPreferences prefs = requireContext().getSharedPreferences("catalogo_prefs", Context.MODE_PRIVATE);
        String orden = prefs.getString("orden_actual", "reciente");

        // Selecciona el radio guardado previamente
        int radioId = obtenerRadioIdDesdeOrden(view, orden);
        if (radioId != -1) rgOrden.check(radioId);

        btnAplicar.setOnClickListener(v -> {
            int selectedId = rgOrden.getCheckedRadioButtonId();
            String nuevoOrden = obtenerOrdenDesdeRadio(selectedId, view);

            prefs.edit().putString("orden_actual", nuevoOrden).apply();

            if (listener != null) listener.onOrdenAplicado();
            dismiss();
        });

        return view;
    }

    private int obtenerRadioIdDesdeOrden(View view, String orden) {
        switch (orden) {
            case "precio_asc": return R.id.rbPrecioAsc;
            case "precio_desc": return R.id.rbPrecioDesc;
            case "metros_asc": return R.id.rbMetrosAsc;
            case "metros_desc": return R.id.rbMetrosDesc;
            case "cuartos_asc": return R.id.rbCuartosAsc;
            case "cuartos_desc": return R.id.rbCuartosDesc;
            case "aseos_asc": return R.id.rbAseosAsc;
            case "aseos_desc": return R.id.rbAseosDesc;
            default: return R.id.rbReciente;
        }
    }

    private String obtenerOrdenDesdeRadio(int id, View view) {
        if (id == R.id.rbPrecioAsc) return "precio_asc";
        if (id == R.id.rbPrecioDesc) return "precio_desc";
        if (id == R.id.rbMetrosAsc) return "metros_asc";
        if (id == R.id.rbMetrosDesc) return "metros_desc";
        if (id == R.id.rbCuartosAsc) return "cuartos_asc";
        if (id == R.id.rbCuartosDesc) return "cuartos_desc";
        if (id == R.id.rbAseosAsc) return "aseos_asc";
        if (id == R.id.rbAseosDesc) return "aseos_desc";
        return "reciente";
    }
}
