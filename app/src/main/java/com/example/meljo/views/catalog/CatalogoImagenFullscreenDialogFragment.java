package com.example.meljo.views.catalog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.meljo.R;

import com.github.chrisbanes.photoview.PhotoView;


public class CatalogoImagenFullscreenDialogFragment extends DialogFragment {

    private static final String ARG_NUMERO = "numero";
    private static final String ARG_TOTAL = "total";
    private static final String ARG_URL = "url";

    private int numero;
    private int total;
    private String url;

    public static CatalogoImagenFullscreenDialogFragment newInstance(String url, int numero, int total) {
        CatalogoImagenFullscreenDialogFragment fragment = new CatalogoImagenFullscreenDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putInt(ARG_NUMERO, numero);
        args.putInt(ARG_TOTAL, total);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.url = getArguments().getString(ARG_URL);
            this.numero = getArguments().getInt(ARG_NUMERO, 1);
            this.total = getArguments().getInt(ARG_TOTAL, 1);
        }
        setStyle(STYLE_NORMAL, R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_catalog_imagefull_dlg, container, false);

        PhotoView photoView = view.findViewById(R.id.fullscreenImageView);
        TextView txtContador = view.findViewById(R.id.txtContador);

        Glide.with(requireContext()).load(url).into(photoView);
        txtContador.setText("Imagen " + numero + " de " + total);

        photoView.setOnClickListener(v -> dismiss());

        return view;
    }
}
