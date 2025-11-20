package com.example.meljo.views.image;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Imagen;
import com.example.meljo.models.Propiedad;
import com.example.meljo.utils.TextWatcherUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ImagenNuevoDialogFragment extends DialogFragment {

    private EditText etNombre;
    private ImageView imagePreview;
    private VideoView videoPreview;
    private Spinner spinner;
    private Uri imagenUri = null;
    private ImagenDialogListener listener;

    public void setListener(ImagenDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.frag_image_new_dlg, null);

        imagePreview = view.findViewById(R.id.imgPreview);
        videoPreview = view.findViewById(R.id.videoPreview);
        etNombre = view.findViewById(R.id.etNbimagen);
        spinner = view.findViewById(R.id.spropiedades);
        Button btnSeleccionar = view.findViewById(R.id.bseleccionar);

        TextWatcherUtils.agregarTextWatcherCodigo(etNombre);

        // Cargar propiedades
        new DBHelper().getTodasPropiedades(new AppCallback() {
            @Override
            public void onPropFragYnewEditYcataYchatExito(List<Propiedad> propiedades) {
                FragmentActivity activity = requireActivity();
                activity.runOnUiThread(() -> {
                    List<String> props = new ArrayList<>();
                    for (Propiedad p : propiedades) props.add(p.casaid);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, props);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                });
            }
            @Override
            public void onError(String mensaje) {}
        });

        btnSeleccionar.setOnClickListener(v -> seleccionarArchivo());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle("Nueva Imagen/Video")
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(v -> guardarImagen(dialog));
        });

        return dialog;
    }

    private void seleccionarArchivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        startActivityForResult(Intent.createChooser(intent, "Selecciona archivo"), 1001);
    }

    private void guardarImagen(AlertDialog dialog) {
        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            etNombre.setError("Campo obligatorio");
            etNombre.requestFocus();
            return;
        }

        if (imagenUri == null) {
            Toast.makeText(getContext(), "Debes seleccionar un archivo", Toast.LENGTH_SHORT).show();
            return;
        }

        String casaId = spinner.getSelectedItem() != null ? spinner.getSelectedItem().toString() : "";
        String tipo = "imagen";
        String mime = getContext().getContentResolver().getType(imagenUri);
        if (mime != null && mime.startsWith("video")) tipo = "video";

        // 🔹 Obtener la fecha actual
        String fechaActual = DBHelper.obtenerFechaActual();

        try {
            new DBHelper().insertarImagen(
                    requireContext(),
                    imagenUri.toString(),
                    casaId,
                    nombre,
                    fechaActual,   // datereg
                    fechaActual,   // datemod
                    tipo,
                    new AppCallback() {
                        @Override
                        public void onImagenNewEditExito(String mensaje, Imagen imagen) {
                            requireActivity().runOnUiThread(() -> {
                                if (listener != null) listener.onImagenGuardada();
                                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        }

                        @Override
                        public void onError(String mensaje) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Error: " + mensaje, Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Archivo no encontrado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1 && data != null && data.getData() != null) {
            imagenUri = data.getData();
            mostrarPreview(imagenUri);
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
}
