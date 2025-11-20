package com.example.meljo.views.catalog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Imagen;

import java.util.ArrayList;
import java.util.List;

public class CatalogoImagenDialogFragment extends DialogFragment {

    private static final String ARG_NBCASA = "nbcasa";
    private static final String TAG = "CatalogoImagenDialog";

    private CatalogoViewPagerImagenAdapter adapter;
    private ImageButton btnAbajo;
    private ImageButton btnArriba;
    private DBHelper dbHelper;
    private List<Imagen> imagenes = new ArrayList<>();
    private String nbcasa;
    private TextView txtContador;
    private ViewPager2 viewPager;

    public static CatalogoImagenDialogFragment newInstance(String nbcasa) {
        CatalogoImagenDialogFragment fragment = new CatalogoImagenDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NBCASA, nbcasa);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nbcasa = getArguments().getString(ARG_NBCASA);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_catalog_image_dlg_galeria, container, false);

        viewPager = view.findViewById(R.id.viewPagerImagenesDialog);
        txtContador = view.findViewById(R.id.txtContadorImagenDialog);
        btnArriba = view.findViewById(R.id.btnArribaDialog);
        btnAbajo = view.findViewById(R.id.btnAbajoDialog);

        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        adapter = null;
        dbHelper = new DBHelper();

        cargarImagenes();

        btnArriba.setOnClickListener(v -> {
            if (adapter == null || adapter.getItemCount() == 0) return;
            int current = viewPager.getCurrentItem();
            int total = adapter.getItemCount();
            int next = (current - 1 + total) % total;
            viewPager.setCurrentItem(next, true);
        });

        btnAbajo.setOnClickListener(v -> {
            if (adapter == null || adapter.getItemCount() == 0) return;
            int current = viewPager.getCurrentItem();
            int total = adapter.getItemCount();
            int next = (current + 1) % total;
            viewPager.setCurrentItem(next, true);
        });

        return view;
    }

    private void cargarImagenes() {
        Log.d(TAG, "Cargando imágenes para casa: " + nbcasa);
        dbHelper.obtenerImagenesPorCasa(nbcasa, "imagen", new AppCallback() {
            @Override
            public void onImagenFragYcatalogoExito(List<Imagen> lista) {
                requireActivity().runOnUiThread(() -> {
                    imagenes.clear();
                    imagenes.addAll(lista);

                    Log.d(TAG, "Imágenes obtenidas: " + imagenes.size());
                    for (Imagen img : imagenes) {
                        Log.d(TAG, " - " + img.getNbimagen() + " (" + img.getUrimagen() + ")");
                    }

                    if (adapter == null) {
                        adapter = new CatalogoViewPagerImagenAdapter(CatalogoImagenDialogFragment.this, imagenes);
                        viewPager.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    txtContador.setText("1 / " + imagenes.size());
                });
            }

            @Override
            public void onError(String mensaje) {
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Error cargando imágenes: " + mensaje);
                    Toast.makeText(getContext(), "Error cargando imágenes: " + mensaje, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
