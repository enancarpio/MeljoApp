package com.example.meljo.views.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.R;
import com.example.meljo.models.Usuario;

import java.util.List;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.ViewHolder> {

    private List<Usuario> lista;
    private final OnUsuarioClickListener listener;

    public interface OnUsuarioClickListener {
        void onEditarClick(Usuario usuario);
        void onEliminarClick(Usuario usuario);
    }

    public UsuarioAdapter(List<Usuario> lista, OnUsuarioClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nombre, apellido, email, fono, admin, nbuser;
        final ImageButton btnEditar, btnEliminar;

        public ViewHolder(View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.txtNombreUsuario);
            apellido = itemView.findViewById(R.id.tvApellido);
            email = itemView.findViewById(R.id.txtEmailUsuario);
            fono = itemView.findViewById(R.id.etFono);
            admin = itemView.findViewById(R.id.txtUser);
            nbuser = itemView.findViewById(R.id.etNbUser);
            btnEditar = itemView.findViewById(R.id.btnEditarUsuario);
            btnEliminar = itemView.findViewById(R.id.btnEliminarUsuario);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Usuario u = lista.get(position);

        holder.nombre.setText(u.nb);
        holder.apellido.setText(u.ape);
        holder.email.setText(u.email);
        holder.fono.setText(u.fono);
        holder.admin.setText(u.admin ? "Administrador" : "Usuario");
        holder.nbuser.setText(u.username);

        holder.btnEditar.setOnClickListener(v -> listener.onEditarClick(u));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminarClick(u));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void updateData(List<Usuario> nuevos) {
        this.lista = nuevos;
        notifyDataSetChanged();
    }
}
