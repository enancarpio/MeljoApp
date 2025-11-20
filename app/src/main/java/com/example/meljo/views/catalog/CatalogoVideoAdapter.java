package com.example.meljo.views.catalog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.meljo.R;
import com.example.meljo.models.Imagen;

import java.util.List;

public class CatalogoVideoAdapter extends RecyclerView.Adapter<CatalogoVideoAdapter.VideoViewHolder> {

    private final Context context;
    private final List<Imagen> videos;

    public CatalogoVideoAdapter(List<Imagen> videos, Context context) {
        this.videos = videos;
        this.context = context;
    }

    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_catalog_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(VideoViewHolder holder, int position) {
        Imagen video = videos.get(position);
        final String videoUrl = video.getUrimagen();
        holder.nombre.setText(video.getNbimagen());
        Log.d("CatalogoVideoAdapter", "Video URL: " + videoUrl);
        Glide.with(context).load(videoUrl).thumbnail(0.1f).centerCrop().into(holder.thumbnail);

        holder.itemView.setOnClickListener(v -> {
            try {
                Uri videoUri = Uri.parse(videoUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(videoUri, "video/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "Error al abrir video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("CatalogoVideoAdapter", "Excepción al abrir video", e);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        final TextView nombre;
        final ImageView thumbnail;

        public VideoViewHolder(View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.textVideoName);
            thumbnail = itemView.findViewById(R.id.imageVideoThumbnail);
        }
    }
}
