package com.example.meljo.views.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.example.meljo.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class UsuarioFragment extends Fragment implements UsuarioDialogListener {

    private UsuarioAdapter adapter;
    private DBHelper dbHelper;
    private List<Usuario> listaCompleta = new ArrayList<>();
    private boolean ordenAscendente = true;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_model, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Button btnNuevo = view.findViewById(R.id.btnNuevo);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Spinner sFiltro = view.findViewById(R.id.sFiltro);
        RadioButton rbOrden = view.findViewById(R.id.rbOrden);
        EditText etBuscar = view.findViewById(R.id.etBuscar);

        dbHelper = new DBHelper();
        adapter = new UsuarioAdapter(new ArrayList<>(), new UsuarioAdapter.OnUsuarioClickListener() {
            @Override
            public void onEliminarClick(Usuario usuario) {
                dbHelper.eliminarUsuario(usuario, new AppCallback() {
                    @Override
                    public void onUsuarioEliminado(boolean ok) {
                        if (getActivity() == null) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!ok) {
                                Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                            } else {
                                listaCompleta.remove(usuario);
                                aplicarFiltroYOrden();
                                Toast.makeText(getContext(), "Eliminado: " + usuario.nb, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }

            @Override
            public void onEditarClick(Usuario usuario) {
                UsuarioEditarDialogFragment dialog = new UsuarioEditarDialogFragment(usuario, dbHelper);
                dialog.setListener(UsuarioFragment.this);
                dialog.show(getParentFragmentManager(), "EditarUsuario");
            }
        });

        recyclerView.setAdapter(adapter);

        // Filtro
        ArrayAdapter<String> filtroAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Todos", "Administradores", "Usuarios"}
        );
        filtroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sFiltro.setAdapter(filtroAdapter);
        sFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                aplicarFiltroYOrden();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Orden
        rbOrden.setOnClickListener(v -> {
            ordenAscendente = !ordenAscendente;
            rbOrden.setCompoundDrawablesWithIntrinsicBounds(
                    ordenAscendente ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float,
                    0, 0, 0
            );
            aplicarFiltroYOrden();
        });

        // Búsqueda
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltroYOrden();
            }
        });

        // Botón nuevo
        btnNuevo.setOnClickListener(v -> {
            UsuarioNuevoDialogFragment dialog = new UsuarioNuevoDialogFragment(false);
            dialog.setListener(UsuarioFragment.this);
            dialog.show(getParentFragmentManager(), "NuevoUsuario");
        });

        // Carga inicial
        cargarUsuarios();
    }

    private void cargarUsuarios() {
        dbHelper.getAllUsuarios(new AppCallback() {
            @Override
            public void onUsuariosCargados(List<Usuario> usuarios) {
                requireActivity().runOnUiThread(() -> {
                    listaCompleta = usuarios;
                    aplicarFiltroYOrden();
                });
            }

            @Override
            public void onError(String mensaje) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error al cargar usuarios: " + mensaje, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    @Override
    public void onUsuarioGuardado(Usuario usuario) {
        cargarUsuarios();
        Toast.makeText(getContext(), "Usuario guardado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUsuarioActualizado() {
        cargarUsuarios();
        Toast.makeText(getContext(), "Usuario actualizado", Toast.LENGTH_SHORT).show();
    }

    private void aplicarFiltroYOrden() {
        if (getView() == null) return;

        Spinner sFiltro = getView().findViewById(R.id.sFiltro);
        EditText etBuscar = getView().findViewById(R.id.etBuscar);

        int filtro = sFiltro.getSelectedItemPosition();
        String textoBusqueda = etBuscar.getText().toString().trim().toLowerCase();

        List<Usuario> filtrados = new ArrayList<>();
        for (Usuario u : listaCompleta) {
            boolean coincideFiltro = (filtro == 0) ||
                    (filtro == 1 && u.admin) ||
                    (filtro == 2 && !u.admin);

            if (!coincideFiltro) continue;

            String nombre = u.nb != null ? u.nb.toLowerCase() : "";
            String apellido = u.ape != null ? u.ape.toLowerCase() : "";
            String correo = u.email != null ? u.email.toLowerCase() : "";
            String username = u.username != null ? u.username.toLowerCase() : "";
            String telefono = u.fono != null ? u.fono.toLowerCase() : "";

            boolean coincideBusqueda = textoBusqueda.isEmpty() ||
                    nombre.contains(textoBusqueda) ||
                    apellido.contains(textoBusqueda) ||
                    correo.contains(textoBusqueda) ||
                    username.contains(textoBusqueda) ||
                    telefono.contains(textoBusqueda);

            if (coincideBusqueda) filtrados.add(u);
        }

        filtrados.sort((u1, u2) -> ordenAscendente
                ? u1.nb.compareToIgnoreCase(u2.nb)
                : u2.nb.compareToIgnoreCase(u1.nb)
        );

        requireActivity().runOnUiThread(() -> adapter.updateData(filtrados));
    }
}
