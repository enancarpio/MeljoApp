package com.example.meljo.views.image;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.meljo.R;
import com.example.meljo.models.Imagen;

import java.io.File;
import java.util.List;

public class ImagenAdapter extends RecyclerView.Adapter<ImagenAdapter.ViewHolder> {

    private final List<Imagen> lista;
    private final OnImagenClickListener listener;

    public interface OnImagenClickListener {
        void onEditarClick(Imagen imagen);
        void onEliminarClick(Imagen imagen);
    }

    public ImagenAdapter(List<Imagen> lista, OnImagenClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageButton btnEditar;
        final ImageButton btnEliminar;
        final ImageView imageViewPreview;
        final ImageView playIcon;
        final TextView tvNbcasa;
        final TextView tvNbimagen;
        final TextView txtImagenFecha;
        final TextView txtImagenRuta;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNbcasa = itemView.findViewById(R.id.tvNbcasa);
            txtImagenRuta = itemView.findViewById(R.id.tvUrimagen);
            tvNbimagen = itemView.findViewById(R.id.tvNbimagen);
            txtImagenFecha = itemView.findViewById(R.id.txtImagenFecha);
            btnEditar = itemView.findViewById(R.id.btnEditarImagen);
            btnEliminar = itemView.findViewById(R.id.btnEliminarImagen);
            imageViewPreview = itemView.findViewById(R.id.imageViewPreview);
            playIcon = itemView.findViewById(R.id.playIcon);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Imagen img = lista.get(position);
        Log.d("ImagenAdapter", "Renderizando: " + img.imagenid + " - " + img.nbimagen);

        holder.tvNbcasa.setText("Propiedad: " + img.casaid);
        holder.txtImagenRuta.setText("Ruta: " + img.urimagen);
        holder.txtImagenFecha.setText("Modificación: " + img.datemod);
        holder.tvNbimagen.setText("Imágen: " + img.nbimagen);

        if ("video".equalsIgnoreCase(img.tipo)) {
            holder.playIcon.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(img.urimagen))
                    .thumbnail(0.1f)
                    .into(holder.imageViewPreview);

            holder.imageViewPreview.setOnClickListener(v -> abrirVideo(holder, img));
        } else {
            holder.playIcon.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(img.urimagen))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(holder.imageViewPreview);
            holder.imageViewPreview.setOnClickListener(null);
        }

        holder.btnEditar.setOnClickListener(v -> listener.onEditarClick(img));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminarClick(img));
    }

    private void abrirVideo(ViewHolder holder, Imagen img) {
        try {
            Context context = holder.itemView.getContext();
            String path = img.urimagen.startsWith("file://") ? Uri.parse(img.urimagen).getPath() : img.urimagen;
            File videoFile = new File(path);
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", videoFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "video/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(holder.itemView.getContext(), "Error al abrir video", Toast.LENGTH_SHORT).show();
            Log.e("ImagenAdapter", "Error al abrir video", e);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void setListaNueva(List<Imagen> nuevaLista) {
        Log.d("ImagenAdapter", "Actualizando lista. Tamaño nuevo: " + nuevaLista.size());
        lista.clear();
        lista.addAll(nuevaLista);
        notifyDataSetChanged();
    }
}
