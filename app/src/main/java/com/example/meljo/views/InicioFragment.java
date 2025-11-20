package com.example.meljo.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.meljo.MainActivity;
import com.example.meljo.R;
import com.example.meljo.views.catalog.CatalogoFragment;
import com.google.android.material.button.MaterialButton;

public class InicioFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_inicio, container, false);

        MaterialButton btnCatalogo = view.findViewById(R.id.btnCatalogo);

        btnCatalogo.setOnClickListener(v -> {
            MainActivity activity = (MainActivity) getActivity();
            if (activity == null) return;

            if (!activity.hayConexionInternet()) {
                Toast.makeText(activity, "Sin conexión a internet.", Toast.LENGTH_SHORT).show();
            } else {
                activity.navegarA(new CatalogoFragment(), true);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        View root = getView();
        if (root != null) {
            root.post(() -> {
                root.invalidate();
                root.requestLayout();
            });
        }
    }
}
