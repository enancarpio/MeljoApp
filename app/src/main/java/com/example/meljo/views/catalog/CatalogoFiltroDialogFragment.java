package com.example.meljo.views.catalog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.fragment.app.DialogFragment;

import com.example.meljo.R;

import java.util.ArrayList;
import java.util.List;

public class CatalogoFiltroDialogFragment extends DialogFragment {

    private CheckBox cbDisponibles, cbVendidas;
    private Spinner spAseos, spCuartos, spMetros;
    private Button btnAplicar;
    private OnFiltroAplicadoListener listener;

    public interface OnFiltroAplicadoListener {
        void onFiltroAplicado();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof OnFiltroAplicadoListener) {
            listener = (OnFiltroAplicadoListener) getParentFragment();
        } else if (context instanceof OnFiltroAplicadoListener) {
            listener = (OnFiltroAplicadoListener) context;
        } else {
            throw new RuntimeException("Debe implementar OnFiltroAplicadoListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_catalog_filtro_dlg, container, false);

        cbVendidas = view.findViewById(R.id.cbVendidas);
        cbDisponibles = view.findViewById(R.id.cbDisponibles);
        spAseos = view.findViewById(R.id.spAseos);
        spCuartos = view.findViewById(R.id.spCuartos);
        spMetros = view.findViewById(R.id.spMetros);
        btnAplicar = view.findViewById(R.id.btnAplicarFiltro);

        configurarSpinners();

        SharedPreferences prefs = requireContext().getSharedPreferences("catalogo_prefs", Context.MODE_PRIVATE);

        // Recuperar valores previos
        String estado = prefs.getString("filtro_estado", "Todos");
        cbVendidas.setChecked(estado.equals("Vendidos") || estado.equals("Todos"));
        cbDisponibles.setChecked(estado.equals("Disponibles") || estado.equals("Todos"));

        spAseos.setSelection(prefs.getInt("filtro_aseos", 0));
        spCuartos.setSelection(prefs.getInt("filtro_cuartos", 0));
        spMetros.setSelection((int) prefs.getFloat("filtro_metros_min", 0) / 100);

        btnAplicar.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();

            String estadoSel;
            if (cbVendidas.isChecked() && !cbDisponibles.isChecked()) {
                estadoSel = "Vendidos";
            } else if (!cbVendidas.isChecked() && cbDisponibles.isChecked()) {
                estadoSel = "Disponibles";
            } else {
                estadoSel = "Todos";
            }
            editor.putString("filtro_estado", estadoSel);

            editor.putInt("filtro_aseos", spAseos.getSelectedItemPosition());
            editor.putInt("filtro_cuartos", spCuartos.getSelectedItemPosition());
            editor.putFloat("filtro_metros_min", spMetros.getSelectedItemPosition() * 100);
            editor.putFloat("filtro_metros_max", Float.MAX_VALUE);

            editor.apply();
            if (listener != null) listener.onFiltroAplicado();
            dismiss();
        });

        return view;
    }

    private void configurarSpinners() {
        ArrayAdapter<Integer> adapterAseosCuartos = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, crearRango(0, 5, 1));
        adapterAseosCuartos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spAseos.setAdapter(adapterAseosCuartos);
        spCuartos.setAdapter(adapterAseosCuartos);

        ArrayAdapter<Integer> adapterMetros = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, crearRango(0, 1000, 100));
        adapterMetros.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMetros.setAdapter(adapterMetros);
    }

    private List<Integer> crearRango(int inicio, int fin, int paso) {
        List<Integer> lista = new ArrayList<>();
        for (int i = inicio; i <= fin; i += paso) {
            lista.add(i);
        }
        return lista;
    }
}
