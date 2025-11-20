package com.example.meljo.views.property;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.R;
import com.example.meljo.models.Propiedad;

import java.util.List;

public class PropiedadAdapter extends RecyclerView.Adapter<PropiedadAdapter.ViewHolder> {

    private List<Propiedad> lista;
    private final OnPropiedadClickListener listener;

    public interface OnPropiedadClickListener {
        void onEditarClick(Propiedad propiedad);
        void onEliminarClick(Propiedad propiedad);
    }

    public PropiedadAdapter(List<Propiedad> lista, OnPropiedadClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nombre, precio, descripcion, registro, cuartos, aseos, metros, direccion, latitud, longitud;
        final ImageButton btnEditar, btnEliminar;

        public ViewHolder(View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.txtNombre);
            precio = itemView.findViewById(R.id.txtPrecio);
            descripcion = itemView.findViewById(R.id.etDescripcion);
            registro = itemView.findViewById(R.id.etDatereg);
            cuartos = itemView.findViewById(R.id.etCuartos);
            aseos = itemView.findViewById(R.id.etAseos);
            metros = itemView.findViewById(R.id.etMetros);
            direccion = itemView.findViewById(R.id.etDireccion);
            latitud = itemView.findViewById(R.id.etLatitud);
            longitud = itemView.findViewById(R.id.etLongitud);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_property, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Propiedad p = lista.get(position);

        holder.nombre.setText(p.casaid);
        holder.precio.setText("$" + p.precio);
        holder.descripcion.setText(p.descripcion);
        holder.registro.setText(p.datereg);
        holder.cuartos.setText("Cuartos: " + p.cuartos);
        holder.aseos.setText("Aseos: " + p.aseos);
        holder.metros.setText("Metros: " + p.metros);
        holder.direccion.setText("Dirección: " + p.direccion);
        holder.latitud.setText("Latitud: " + p.latitud);
        holder.longitud.setText("Longitud: " + p.longitud);

        holder.btnEditar.setOnClickListener(v -> {
            if (listener != null) listener.onEditarClick(p);
        });

        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) listener.onEliminarClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void updateData(List<Propiedad> nuevaLista) {
        lista = nuevaLista;
        notifyDataSetChanged();
    }
}
