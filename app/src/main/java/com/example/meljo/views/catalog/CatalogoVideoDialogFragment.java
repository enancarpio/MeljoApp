package com.example.meljo.views.catalog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Imagen;

import java.util.ArrayList;
import java.util.List;

public class CatalogoVideoDialogFragment extends DialogFragment {

    private static final String ARG_NBCASA = "nbcasa";
    private String nbcasa;

    public static CatalogoVideoDialogFragment newInstance(String nbcasa) {
        CatalogoVideoDialogFragment fragment = new CatalogoVideoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NBCASA, nbcasa);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nbcasa = getArguments().getString(ARG_NBCASA);
        }
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_items, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Imagen> videos = new ArrayList<>();
        CatalogoVideoAdapter adapter = new CatalogoVideoAdapter(videos, getContext());
        recyclerView.setAdapter(adapter);

        DBHelper dbHelper = new DBHelper();
        dbHelper.obtenerMultimediaPorTipo(nbcasa, "video", new AppCallback() {
            @Override
            public void onImagenFragYcatalogoExito(List<Imagen> lista) {
                requireActivity().runOnUiThread(() -> {
                    videos.clear();
                    videos.addAll(lista);
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String mensaje) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error cargando videos: " + mensaje, Toast.LENGTH_SHORT).show()
                );
            }
        });

        return view;
    }
}
