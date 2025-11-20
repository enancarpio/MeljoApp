package com.example.meljo.views.image;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Imagen;
import com.example.meljo.models.Propiedad;
import com.example.meljo.utils.TextWatcherUtils;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ImagenEditarDialogFragment extends DialogFragment {

    private EditText etNombre;
    private ImageView imagePreview;
    private VideoView videoPreview;
    private Spinner spinner;
    private Imagen imagen;
    private Uri imagenEditadaUri;
    private ImagenDialogListener listener;

    public void setImagen(Imagen imagen) { this.imagen = imagen; }
    public void setListener(ImagenDialogListener listener) { this.listener = listener; }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.frag_image_edit_dlg, null);
        imagePreview = view.findViewById(R.id.imgPreviewEditar);
        videoPreview = view.findViewById(R.id.videoPreviewEditar);
        spinner = view.findViewById(R.id.spropiedadesEditar);
        etNombre = view.findViewById(R.id.etNbimagenEditar);
        Button btnSeleccionar = view.findViewById(R.id.bseleccionarEditar);

        TextWatcherUtils.agregarTextWatcherCodigo(etNombre);

        // 🔹 Cargar propiedades de forma segura (UI en hilo principal)
        new DBHelper().getTodasPropiedades(new AppCallback() {
            @Override
            public void onPropFragYnewEditYcataYchatExito(List<Propiedad> propiedades) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    List<String> props = new ArrayList<>();
                    int selectedIndex = 0;

                    for (int i = 0; i < propiedades.size(); i++) {
                        props.add(propiedades.get(i).casaid);
                        if (propiedades.get(i).casaid.equals(imagen.casaid)) {
                            selectedIndex = i;
                        }
                    }

                    ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            props
                    );
                    adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinner.setAdapter(adapterSpinner);
                    spinner.setSelection(selectedIndex);
                });
            }

            @Override
            public void onError(String mensaje) {
                Log.e("ImagenEditarDialog", mensaje);
            }
        });

        etNombre.setText(imagen.nbimagen);
        mostrarPreview(Uri.parse(imagen.urimagen));

        btnSeleccionar.setOnClickListener(v -> seleccionarArchivo());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle("Editar Imagen/Video")
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(v -> guardarCambios(dialog));
        });

        return dialog;
    }

    private void seleccionarArchivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*","video/*"});
        startActivityForResult(Intent.createChooser(intent, "Selecciona archivo"), 1002);
    }

    private void guardarCambios(AlertDialog dialog) {
        String nuevoNombre = etNombre.getText().toString().trim();
        if (nuevoNombre.isEmpty()) {
            etNombre.setError("Campo obligatorio");
            etNombre.requestFocus();
            return;
        }

        imagen.nbimagen = nuevoNombre;
        imagen.casaid = spinner.getSelectedItem().toString();
        imagen.datemod = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String nuevaUriLocal = imagenEditadaUri != null ? imagenEditadaUri.toString() : null;

        try {
            new DBHelper().actualizarImagen(requireContext(), imagen, nuevaUriLocal, new AppCallback() {
                @Override
                public void onImagenNewEditExito(String mensaje, Imagen imagenActualizada) {
                    if (listener != null) listener.onImagenActualizada(imagenActualizada);
                    Toast.makeText(getContext(), "Imagen actualizada correctamente", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
                @Override
                public void onError(String mensaje) {
                    Toast.makeText(getContext(), "Error: " + mensaje, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Archivo no encontrado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarPreview(Uri uri) {
        String mime = getContext().getContentResolver().getType(uri);
        if (mime != null && mime.startsWith("video")) {
            videoPreview.setVisibility(View.VISIBLE);
            imagePreview.setVisibility(View.GONE);
            videoPreview.setVideoURI(uri);
            videoPreview.seekTo(100);
        } else {
            imagePreview.setVisibility(View.VISIBLE);
            videoPreview.setVisibility(View.GONE);
            Glide.with(getContext()).load(uri).into(imagePreview);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1 && data != null && data.getData() != null) {
            imagenEditadaUri = data.getData();
            mostrarPreview(imagenEditadaUri);
        }
    }
}
