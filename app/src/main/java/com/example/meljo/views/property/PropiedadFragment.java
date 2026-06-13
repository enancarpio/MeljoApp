package com.example.meljo.views.property;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Propiedad;
import com.example.meljo.views.catalog.CatalogoViewModel;

import java.util.ArrayList;
import java.util.List;

public class PropiedadFragment extends Fragment implements PropiedadDialogListener {

    private PropiedadAdapter adapter;
    private DBHelper dbHelper;
    private EditText etBuscar;
    private Spinner sFiltro;
    private RadioButton rbOrden;
    private boolean ordenAscendente = true;

    private final List<Propiedad> lista = new ArrayList<>();
    private final List<Propiedad> listaOriginal = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_model, container, false);

        // Inicializar vistas
        dbHelper = new DBHelper();
        rbOrden = view.findViewById(R.id.rbOrden);
        sFiltro = view.findViewById(R.id.sFiltro);
        etBuscar = view.findViewById(R.id.etBuscar);
        Button btnNuevo = view.findViewById(R.id.btnNuevo);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PropiedadAdapter(lista, new PropiedadAdapter.OnPropiedadClickListener() {
            @Override
            public void onEditarClick(Propiedad propiedad) {
                PropiedadEditarDialogFragment dialog = PropiedadEditarDialogFragment.newInstance(propiedad);
                dialog.setListener(PropiedadFragment.this);
                dialog.setDbHelper(dbHelper);
                dialog.show(getParentFragmentManager(), "EditarPropiedad");
            }

            @Override
            public void onEliminarClick(Propiedad propiedad) {
                dbHelper.eliminarPropiedad(propiedad.id, new AppCallback() {
                    @Override
                    public void onExito(String mensaje) {
                        requireActivity().runOnUiThread(() -> {
                            listaOriginal.remove(propiedad);
                            aplicarFiltroYOrden();
                            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                            notificarCatalogoCambio(); // 🔥 Actualiza catálogo
                        });
                    }

                    @Override
                    public void onError(String mensaje) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            }
        });
        recyclerView.setAdapter(adapter);

        // Cargar propiedades desde Supabase
        cargarPropiedades();

        // Configurar búsqueda, filtro y orden
        configurarControles(view);

        return view;
    }

    // ───────────────────────────────
    // ⚙️ Configuración de eventos UI
    // ───────────────────────────────
    private void configurarControles(View view) {
        // Orden ascendente / descendente
        rbOrden.setOnClickListener(v -> {
            ordenAscendente = !ordenAscendente;
            rbOrden.setCompoundDrawablesWithIntrinsicBounds(
                    ordenAscendente ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float,
                    0, 0, 0
            );
            aplicarFiltroYOrden();
        });

        // Spinner de filtro
        ArrayAdapter<String> filtroAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Todos", "Vendidos", "Disponibles"}
        );
        filtroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sFiltro.setAdapter(filtroAdapter);

        sFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view1, int pos, long id) {
                aplicarFiltroYOrden();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Búsqueda en tiempo real
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { aplicarFiltroYOrden(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Botón nuevo
        Button btnNuevo = view.findViewById(R.id.btnNuevo);
        btnNuevo.setOnClickListener(v -> {
            PropiedadNuevaDialogFragment dialog = new PropiedadNuevaDialogFragment();
            dialog.setPropiedadDialogListener(this);
            dialog.show(getParentFragmentManager(), "NuevaPropiedad");
        });
    }

    // ───────────────────────────────
    // 📦 Cargar propiedades desde Supabase
    // ───────────────────────────────
    private void cargarPropiedades() {
        dbHelper.getTodasPropiedades(new AppCallback() {
            @Override
            public void onPropFragYnewEditYcataYchatExito(List<Propiedad> propiedades) {
                requireActivity().runOnUiThread(() -> {
                    listaOriginal.clear();
                    listaOriginal.addAll(propiedades);
                    lista.clear();
                    lista.addAll(propiedades);
                    adapter.notifyDataSetChanged();
                    Log.d("PropiedadFragment", "✅ Propiedades cargadas: " + propiedades.size());
                });
            }

            @Override
            public void onError(String mensaje) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error: " + mensaje, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // ───────────────────────────────
    // 🔍 Filtrado, búsqueda y orden
    // ───────────────────────────────
    private void aplicarFiltroYOrden() {
        if (sFiltro.getSelectedItem() == null) return;

        String textoBuscar = etBuscar.getText().toString().toLowerCase();
        String estadoSeleccionado = sFiltro.getSelectedItem().toString();

        List<Propiedad> resultado = new ArrayList<>();

        for (Propiedad p : listaOriginal) {
            boolean coincideFiltro =
                    estadoSeleccionado.equals("Todos") ||
                            (estadoSeleccionado.equals("Vendidos") && p.getVendido()) ||
                            (estadoSeleccionado.equals("Disponibles") && !p.getVendido());

            boolean coincideBusqueda =
                    (p.casaid != null && p.casaid.toLowerCase().contains(textoBuscar)) ||
                            (p.descripcion != null && p.descripcion.toLowerCase().contains(textoBuscar)) ||
                            (p.direccion != null && p.direccion.toLowerCase().contains(textoBuscar));

            if (coincideFiltro && coincideBusqueda) {
                resultado.add(p);
            }
        }

        resultado.sort((a, b) ->
                ordenAscendente ? a.casaid.compareToIgnoreCase(b.casaid)
                        : b.casaid.compareToIgnoreCase(a.casaid)
        );

        lista.clear();
        lista.addAll(resultado);
        adapter.notifyDataSetChanged();
    }

    // ───────────────────────────────
    // 🧩 Callbacks de los diálogos
    // ───────────────────────────────
    @Override
    public void onPropiedadGuardada(Propiedad nueva) {
        listaOriginal.add(nueva);
        aplicarFiltroYOrden();
        notificarCatalogoCambio(); // 🔥 Actualiza catálogo
    }

    @Override
    public void onPropiedadActualizada(Propiedad propiedadEditada) {
        requireActivity().runOnUiThread(() -> {
            Log.d("PropiedadFragment", "♻️ Actualizando propiedad editada ID=" + propiedadEditada.id);

            // 🔥 Recargar desde Supabase para obtener los datos reales actualizados
            dbHelper.getPropiedadPorId(propiedadEditada.id, new AppCallback() {
                @Override
                public void onPropiedadPorIdExito(Propiedad propiedadActualizada) {
                    requireActivity().runOnUiThread(() -> {
                        // 1️⃣ Actualizar lista local (para que el fragmento se refresque al instante)
                        boolean encontrada = false;
                        for (int i = 0; i < listaOriginal.size(); i++) {
                            if (listaOriginal.get(i).id == propiedadActualizada.id) {
                                listaOriginal.set(i, propiedadActualizada);
                                encontrada = true;
                                break;
                            }
                        }
                        if (!encontrada) listaOriginal.add(propiedadActualizada);

                        aplicarFiltroYOrden();
                        Toast.makeText(getContext(), "Propiedad actualizada correctamente", Toast.LENGTH_SHORT).show();

                        // 2️⃣ Actualizar también el ViewModel compartido (para el catálogo)
                        CatalogoViewModel vm = new ViewModelProvider(requireActivity()).get(CatalogoViewModel.class);
                        vm.actualizarPropiedadEnLista(propiedadActualizada);

                        // 3️⃣ Notificar que hay cambios pendientes por si otro fragment lo necesita
                        notificarCatalogoCambio();

                        Log.d("PropiedadFragment", "✅ Propiedad actualizada y sincronizada con CatalogoViewModel");
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error al recargar propiedad: " + error, Toast.LENGTH_SHORT).show()
                    );
                }
            });
        });
    }

    // ───────────────────────────────
    // 🔥 Notificar al catálogo para recargar
    // ───────────────────────────────
    private void notificarCatalogoCambio() {
        CatalogoViewModel vm = new ViewModelProvider(requireActivity()).get(CatalogoViewModel.class);
        vm.marcarRecargaPendiente(dbHelper);
        Log.d("PropiedadFragment", "📢 Notificado a CatalogoViewModel (recarga pendiente)");
    }
}
