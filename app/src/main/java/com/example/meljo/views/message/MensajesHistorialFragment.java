package com.example.meljo.views.message;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Mensaje;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MensajesHistorialFragment extends Fragment {

    private MensajesUsuarioAdapter adapter;
    private FirebaseFirestore firestore;
    private final List<MensajesUsuario> listaCompleta = new ArrayList<>();
    private boolean ordenAscendente = true;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_mensaje_history_all, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        firestore = FirebaseFirestore.getInstance();

        Spinner sFiltro = view.findViewById(R.id.sFiltro);
        RadioButton rbOrden = view.findViewById(R.id.rbOrden);
        EditText etBuscar = view.findViewById(R.id.etBuscar);

        adapter = new MensajesUsuarioAdapter(new ArrayList<>(), mu -> eliminarMensajesUsuario(mu));
        recyclerView.setAdapter(adapter);

        cargarHistorial();

        sFiltro.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltroYOrden();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        rbOrden.setOnClickListener(v -> {
            ordenAscendente = !ordenAscendente;
            rbOrden.setCompoundDrawablesWithIntrinsicBounds(
                    ordenAscendente ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float,
                    0, 0, 0
            );
            aplicarFiltroYOrden();
        });

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltroYOrden();
            }
        });
    }

    private void cargarHistorial() {
        DBHelper dbHelper = new DBHelper();
        dbHelper.getHistorialTodosUsuarios(new AppCallback() {
            @Override
            public void onHistorialObtenido(List<Mensaje> listaMensajes) {
                listaCompleta.clear();
                LinkedHashMap<String, MensajesUsuario> mapUsuarios = new LinkedHashMap<>();
                for (Mensaje m : listaMensajes) {
                    MensajesUsuario mu = mapUsuarios.get(m.getUid());
                    if (mu == null) {
                        mu = new MensajesUsuario(m.getUid(), m.getCorreo());
                        mapUsuarios.put(m.getUid(), mu);
                    }
                    mu.addMensaje(m.getMessage());
                }
                listaCompleta.addAll(mapUsuarios.values());

                Set<String> setCorreos = new LinkedHashSet<>();
                setCorreos.add("Todos");
                for (MensajesUsuario mu : listaCompleta) {
                    setCorreos.add(mu.getCorreo());
                }

                Spinner sFiltro = getView().findViewById(R.id.sFiltro);
                ArrayAdapter<String> filtroAdapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_item, new ArrayList<>(setCorreos));
                filtroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sFiltro.setAdapter(filtroAdapter);

                aplicarFiltroYOrden();
            }

            @Override
            public void onHistorialObtenidoError(Exception e) {
                Toast.makeText(getContext(), "Error cargando historial", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void aplicarFiltroYOrden() {
        if (getView() == null) return;

        Spinner sFiltro = getView().findViewById(R.id.sFiltro);
        EditText etBuscar = getView().findViewById(R.id.etBuscar);

        String filtroCorreo = sFiltro.getSelectedItem() != null ? sFiltro.getSelectedItem().toString() : "Todos";
        String textoBusqueda = etBuscar.getText().toString().trim().toLowerCase();

        List<MensajesUsuario> filtrados = new ArrayList<>();
        for (MensajesUsuario mu : listaCompleta) {
            if (filtroCorreo.equals("Todos") || mu.getCorreo().equals(filtroCorreo)) {
                if (textoBusqueda.isEmpty()
                        || mu.getCorreo().toLowerCase().contains(textoBusqueda)
                        || mu.getUid().toLowerCase().contains(textoBusqueda)) {
                    filtrados.add(mu);
                }
            }
        }

        filtrados.sort((m1, m2) -> ordenAscendente ?
                m1.getCorreo().compareToIgnoreCase(m2.getCorreo()) :
                m2.getCorreo().compareToIgnoreCase(m1.getCorreo()));

        adapter.updateData(filtrados);
    }

    private void eliminarMensajesUsuario(final MensajesUsuario mu) {
        firestore.collection("mensajes").document(mu.getUid()).collection("chat").get()
                .addOnSuccessListener(snaps -> {
                    WriteBatch batch = firestore.batch();
                    for (QueryDocumentSnapshot doc : snaps) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Mensajes de " + mu.getCorreo() + " eliminados", Toast.LENGTH_SHORT).show();
                        cargarHistorial();
                    });
                });
    }

    public static class MensajesUsuario {
        private final String uid;
        private final String correo;
        private final List<String> mensajes = new ArrayList<>();

        public MensajesUsuario(String uid, String correo) {
            this.uid = uid;
            this.correo = correo;
        }

        public String getUid() { return uid; }
        public String getCorreo() { return correo; }
        public List<String> getMensajes() { return mensajes; }
        public void addMensaje(String mensaje) { mensajes.add(mensaje); }
    }

    public static class MensajesUsuarioAdapter extends RecyclerView.Adapter<MensajesUsuarioAdapter.ViewHolder> {
        private List<MensajesUsuario> lista;
        private final OnEliminarClickListener listener;

        public interface OnEliminarClickListener {
            void onEliminarClick(MensajesUsuario mensajesUsuario);
        }

        public MensajesUsuarioAdapter(List<MensajesUsuario> lista, OnEliminarClickListener listener) {
            this.lista = lista;
            this.listener = listener;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            final android.widget.ImageButton btnEliminar;
            final android.widget.TextView tvCorreo;
            final android.widget.TextView tvMensajes;
            final android.widget.TextView tvUid;

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
                if (listener != null) listener.onEliminarClick(mu);
            });
        }

        @Override
        public int getItemCount() {
            return lista.size();
        }

        public void updateData(List<MensajesUsuario> nuevos) {
            lista = nuevos;
            notifyDataSetChanged();
        }
    }
}
