package com.example.meljo.views.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.R;
import com.example.meljo.models.MensajesUsuario;

import java.util.List;

public class MensajesHistorialAdapter extends RecyclerView.Adapter<MensajesHistorialAdapter.ViewHolder> {

    private final List<MensajesUsuario> lista;
    private final OnEliminarClickListener listener;

    public interface OnEliminarClickListener {
        void onEliminarClick(MensajesUsuario mensajesUsuario);
    }

    public MensajesHistorialAdapter(List<MensajesUsuario> lista, OnEliminarClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageButton btnEliminar;
        final TextView tvCorreo;
        final TextView tvMensajes;
        final TextView tvUid;

        public ViewHolder(View itemView) {
            super(itemView);
            tvCorreo = itemView.findViewById(R.id.tvCorreo);
            tvUid = itemView.findViewById(R.id.tvUid);
            tvMensajes = itemView.findViewById(R.id.tvMensajes);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mensaje_history_all, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final MensajesUsuario mu = lista.get(position);
        holder.tvCorreo.setText(mu.getCorreo());
        holder.tvUid.setText(mu.getUid());

        StringBuilder sb = new StringBuilder();
        for (String msg : mu.getMensajes()) {
            sb.append("- ").append(msg).append("\n");
        }
        holder.tvMensajes.setText(sb.toString().trim());

        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEliminarClick(mu);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }
}
