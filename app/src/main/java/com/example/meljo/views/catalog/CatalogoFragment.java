package com.example.meljo.views.catalog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.MainActivity;
import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Imagen;
import com.example.meljo.models.Propiedad;
import com.example.meljo.views.InicioFragment;
import com.example.meljo.views.chat.ChatFragment;
import com.example.meljo.views.login.LoginDialogFragment;
import com.example.meljo.views.property.PropiedadSeleccionMapaDialogFragment;
import com.example.meljo.views.catalog.CatalogoAdapter;
import com.example.meljo.views.catalog.CatalogoViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * Fragment principal del catálogo de propiedades.
 * Se encarga de mostrar la lista de propiedades, aplicar filtros y ordenar resultados.
 */
public class CatalogoFragment extends Fragment implements
        CatalogoAdapter.OnBotonClickListener,
        PropiedadSeleccionMapaDialogFragment.OnUbicacionSeleccionadaListener,
        CatalogoFiltroDialogFragment.OnFiltroAplicadoListener,
        CatalogoOrdenDialogFragment.OnOrdenAplicadoListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBarCarga;
    private EditText etBuscar;
    private CatalogoAdapter adapter;
    private CatalogoViewModel viewModel;
    private DBHelper dbHelper;
    private SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Set<String> propiedadesCacheadas = new HashSet<>();
    private TextView txtSinResultados;

    // --- Ciclo de vida ---
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list_catalog, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        progressBarCarga = view.findViewById(R.id.progressBarCarga);
        etBuscar = view.findViewById(R.id.etBuscar);
        txtSinResultados = view.findViewById(R.id.txtSinResultados);

        prefs = requireContext().getSharedPreferences("catalogo_prefs", 0);
        dbHelper = new DBHelper();
        viewModel = new ViewModelProvider(requireActivity()).get(CatalogoViewModel.class);

        adapter = new CatalogoAdapter(requireContext(), new ArrayList<>(), this, dbHelper);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        configurarBotones(view);
        configurarBusqueda();
        observarDatos();

        return view;
    }

    /** Configura la barra de búsqueda para filtrar propiedades */
    private void configurarBusqueda() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltrosYOrden(s.toString());
            }
        });
    }

    /** Configura botones de acción (filtros, orden, navegación, contacto, chat) */
    private void configurarBotones(View v) {
        ImageButton btnOrdenar = v.findViewById(R.id.btnOrdenar);
        ImageButton btnFiltrar = v.findViewById(R.id.btnFiltrar);

        btnFiltrar.setOnClickListener(view -> new CatalogoFiltroDialogFragment()
                .show(getChildFragmentManager(), "filtro"));

        btnOrdenar.setOnClickListener(view -> new CatalogoOrdenDialogFragment()
                .show(getChildFragmentManager(), "orden"));

        // Botones inferiores
        v.findViewById(R.id.bHome).setOnClickListener(x ->
                ((MainActivity) requireActivity()).navegarA(new InicioFragment(), true));

        v.findViewById(R.id.bCall).setOnClickListener(x -> {
            Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:604947975"));
            startActivity(i);
        });

        v.findViewById(R.id.bContact).setOnClickListener(x -> {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:enancarpio@gmail.com"));
            startActivity(Intent.createChooser(i, "Enviar email"));
        });

        v.findViewById(R.id.bChat).setOnClickListener(x -> abrirChat());
    }

    /** Abre el chat o muestra el diálogo de login si el usuario no ha iniciado sesión */
    private void abrirChat() {
        MainActivity main = (MainActivity) requireActivity();
        if (main.getUsuarioActual() == null) {
            Toast.makeText(requireContext(),
                    "Para usar el CHATBOT, debes iniciar sesión o registrarte.",
                    Toast.LENGTH_SHORT).show();
            main.setAbrirChatTrasLogin(true);
            new LoginDialogFragment().show(requireActivity().getSupportFragmentManager(), "login");
        } else {
            main.navegarA(new ChatFragment(), true);
        }
    }

    /** Observa los LiveData del ViewModel y actualiza UI cuando cambian */
    private void observarDatos() {
        // 🔹 Estado de carga
        viewModel.getCargando().observe(getViewLifecycleOwner(), isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                progressBarCarga.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                txtSinResultados.setVisibility(View.GONE);
                Log.d("OBSERVER", "Estado: CARGANDO");
            } else {
                progressBarCarga.setVisibility(View.GONE);
                Log.d("OBSERVER", "Estado: CARGA COMPLETADA");
            }
        });

        // 🔹 Lista de propiedades
        viewModel.getListaOriginal().observe(getViewLifecycleOwner(), lista -> {
            Log.d("OBSERVER", "Lista recibida: " + (lista == null ? "null" : lista.size()));

            if (lista == null) {
                Log.d("OBSERVER", "Lista null → iniciar carga manualmente");
                viewModel.cargarPropiedadesSiNecesario(dbHelper);
                return;
            }

            if (lista.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                txtSinResultados.setVisibility(View.VISIBLE);
                Log.d("OBSERVER", "Lista vacía → mostrar SIN RESULTADOS");
            } else {
                txtSinResultados.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                aplicarFiltrosYOrden(etBuscar.getText().toString());
                precargarMultimediaCache(lista);
                Log.d("OBSERVER", "Lista con datos → mostrar " + lista.size());
            }
        });

        // 🔹 Cache de imágenes → actualiza el adapter y sincroniza multimediaCache automáticamente
        viewModel.getImagenesCache().observe(getViewLifecycleOwner(), imagenes -> {
            Log.d("CatalogoFragment", "🔁 Detectado cambio en imágenes → actualizando adapter");
            if (adapter != null && imagenes != null) {
                adapter.setImagenesCache(imagenes);
            }
        });

        // 🔹 Cache multimedia → actualiza botones 🎥/🖼️ en el catálogo
        viewModel.getMultimediaCache().observe(getViewLifecycleOwner(), multimedia -> {
            Log.d("CatalogoFragment", "🎬 Multimedia cache actualizada → redibujando lista");
            if (adapter != null && multimedia != null) {
                adapter.setMultimediaCache(multimedia);
            }
        });

        // 🔹 Carga inicial si no se ha hecho
        viewModel.cargarPropiedadesSiNecesario(dbHelper);
    }

    /** Pre-carga multimedia (fotos y videos) en caché para optimizar la lista */
    private void precargarMultimediaCache(List<Propiedad> propiedades) {
        if (propiedades == null || propiedades.isEmpty()) return;

        // Mapas locales para acumular resultados antes de actualizar el ViewModel
        Map<String, CatalogoAdapter.MultimediaResult> multimediaMap =
                viewModel.getMultimediaCache().getValue() != null ?
                        new HashMap<>(viewModel.getMultimediaCache().getValue()) :
                        new HashMap<>();

        Map<String, List<Imagen>> imagenesMap =
                viewModel.getImagenesCache().getValue() != null ?
                        new HashMap<>(viewModel.getImagenesCache().getValue()) :
                        new HashMap<>();

        for (Propiedad propiedad : propiedades) {
            String id = propiedad.getCasaid();

            // Evitar procesar la misma propiedad varias veces
            if (propiedadesCacheadas.contains(id)) continue;
            propiedadesCacheadas.add(id);

            // --- Multimedia (fotos y videos) ---
            if (!multimediaMap.containsKey(id)) {
                dbHelper.obtenerMultimediaEstado(id, new AppCallback() {
                    @Override
                    public void onMultimediaExito(boolean tieneFotos, boolean tieneVideos) {
                        multimediaMap.put(id, new CatalogoAdapter.MultimediaResult(tieneFotos, tieneVideos));

                        // Actualizamos el ViewModel solo una vez por propiedad
                        requireActivity().runOnUiThread(() -> {
                            viewModel.setMultimediaCache(new HashMap<>(multimediaMap));
                            Log.d("CatalogoFragment",
                                    "Multimedia cargada para propiedad " + id +
                                            " → fotos=" + tieneFotos + ", videos=" + tieneVideos);
                        });
                    }

                    @Override
                    public void onError(String msg) {
                        multimediaMap.put(id, new CatalogoAdapter.MultimediaResult(false, false));
                        requireActivity().runOnUiThread(() -> {
                            viewModel.setMultimediaCache(new HashMap<>(multimediaMap));
                            Log.d("CatalogoFragment",
                                    "Error cargando multimedia para propiedad " + id + ": " + msg);
                        });
                    }
                });
            }

            // --- Imágenes ---
            if (!imagenesMap.containsKey(id)) {
                dbHelper.obtenerImagenesPorCasa(id, "imagen", new AppCallback() {
                    @Override
                    public void onImagenFragYcatalogoExito(List<Imagen> imagenes) {
                        imagenesMap.put(id, imagenes != null ? imagenes : new ArrayList<>());

                        requireActivity().runOnUiThread(() -> {
                            viewModel.setImagenesCache(new HashMap<>(imagenesMap));
                            Log.d("CatalogoFragment",
                                    "Imágenes cargadas para propiedad " + id +
                                            " → total=" + (imagenes != null ? imagenes.size() : 0));
                        });
                    }

                    @Override
                    public void onError(String msg) {
                        imagenesMap.put(id, new ArrayList<>());
                        requireActivity().runOnUiThread(() -> {
                            viewModel.setImagenesCache(new HashMap<>(imagenesMap));
                            Log.d("CatalogoFragment",
                                    "Error cargando imágenes para propiedad " + id + ": " + msg);
                        });
                    }
                });
            }
        }
    }

    // --- Interfaces de interacción ---

    @Override
    public void onVerUbicacion(Propiedad propiedad) {
        PropiedadSeleccionMapaDialogFragment fragment =
                PropiedadSeleccionMapaDialogFragment.newInstance(
                        propiedad.getLatitud(),
                        propiedad.getLongitud(),
                        propiedad.getDireccion(),
                        true);
        fragment.show(getParentFragmentManager(), "mapa_visualizacion");
    }

    @Override
    public void onVerImagenes(Propiedad propiedad) {
        CatalogoImagenDialogFragment dialog = CatalogoImagenDialogFragment.newInstance(propiedad.getCasaid());
        dialog.show(getParentFragmentManager(), "imagenes");
    }

    @Override
    public void onVerVideos(Propiedad propiedad) {
        CatalogoVideoDialogFragment dialog = CatalogoVideoDialogFragment.newInstance(propiedad.getCasaid());
        dialog.show(getParentFragmentManager(), "videos");
    }

    @Override
    public void onUbicacionSeleccionada(double latitud, double longitud, String direccion) {
        // No se usa en este fragmento (solo visualización)
    }

    @Override
    public void onFiltroAplicado() {
        aplicarFiltrosYOrden(etBuscar.getText().toString());
    }

    @Override
    public void onOrdenAplicado() {
        aplicarFiltrosYOrden(etBuscar.getText().toString());
    }

    private void aplicarFiltrosYOrden(String textoBusqueda) {
        List<Propiedad> listaOriginal = viewModel.getListaOriginal().getValue();
        if (listaOriginal == null) {
            Log.d("OBSERVER", "aplicarFiltrosYOrden: listaOriginal es null");
            return;
        }

        if (textoBusqueda == null) textoBusqueda = "";
        textoBusqueda = textoBusqueda.trim().toLowerCase();

        // --- Recuperar filtros guardados ---
        String filtroEstado = prefs.getString("filtro_estado", "Todos"); // "Todos", "Vendidos", "Disponibles"
        int filtroCuartos = prefs.getInt("filtro_cuartos", 0);
        int filtroAseos = prefs.getInt("filtro_aseos", 0);
        double filtroMetrosMin = prefs.getFloat("filtro_metros_min", 0);
        double filtroMetrosMax = prefs.getFloat("filtro_metros_max", Float.MAX_VALUE);
        double filtroPrecioMin = prefs.getFloat("filtro_precio_min", 0);
        double filtroPrecioMax = prefs.getFloat("filtro_precio_max", Float.MAX_VALUE);

        String orden = prefs.getString("orden_actual", "reciente");

        List<Propiedad> filtradas = new ArrayList<>();

        for (Propiedad p : listaOriginal) {
            if (p == null) continue;

            // --- Búsqueda ---
            boolean coincideBusqueda =
                    textoBusqueda.isEmpty()
                            || (p.getCasaid() != null && p.getCasaid().toLowerCase().contains(textoBusqueda))
                            || (p.getDireccion() != null && p.getDireccion().toLowerCase().contains(textoBusqueda))
                            || (p.getDescripcion() != null && p.getDescripcion().toLowerCase().contains(textoBusqueda));

            // --- Estado ---
            boolean coincideEstado =
                    filtroEstado.equals("Todos")
                            || (filtroEstado.equals("Vendidos") && p.getVendido())
                            || (filtroEstado.equals("Disponibles") && !p.getVendido());

            // --- Cuartos, aseos, metros, precio ---
            boolean coincideCuartos = (filtroCuartos == 0 || p.getCuartos() == filtroCuartos);
            boolean coincideAseos = (filtroAseos == 0 || p.getAseos() == filtroAseos);
            boolean coincideMetros = (p.getMetros() >= filtroMetrosMin && p.getMetros() <= filtroMetrosMax);
            boolean coincidePrecio = (p.getPrecio() >= filtroPrecioMin && p.getPrecio() <= filtroPrecioMax);

            if (coincideBusqueda && coincideEstado && coincideCuartos && coincideAseos && coincideMetros && coincidePrecio) {
                filtradas.add(p);
            }
        }

        // --- Ordenamiento ---
        filtradas.sort((a, b) -> {
            switch (orden) {
                case "precio_asc": return Double.compare(a.getPrecio(), b.getPrecio());
                case "precio_desc": return Double.compare(b.getPrecio(), a.getPrecio());
                case "metros_asc": return Double.compare(a.getMetros(), b.getMetros());
                case "metros_desc": return Double.compare(b.getMetros(), a.getMetros());
                case "cuartos_asc": return Integer.compare(a.getCuartos(), b.getCuartos());
                case "cuartos_desc": return Integer.compare(b.getCuartos(), a.getCuartos());
                case "aseos_asc": return Integer.compare(a.getAseos(), b.getAseos());
                case "aseos_desc": return Integer.compare(b.getAseos(), a.getAseos());
                default: // "reciente" u otros
                    return b.getDatemod().compareTo(a.getDatemod());
            }
        });

        Log.d("OBSERVER", "Filtradas: " + filtradas.size() + " de " + listaOriginal.size());

        if (filtradas.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            txtSinResultados.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            txtSinResultados.setVisibility(View.GONE);
        }
        // 🔽 Envía la nueva lista y, cuando termine, sube al inicio
        adapter.submitList(filtradas, () -> recyclerView.scrollToPosition(0));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("CatalogoFragment", "🔄 onResume → solo refrescando adaptador, sin recargar propiedades");

        if (adapter != null) {
            adapter.notifyDataSetChanged(); // 🔥 Solo refrescar vista, no forzar recarga
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && viewModel != null && dbHelper != null) {
            Log.d("CatalogoFragment", "👁 Reapareció → verificando actualizaciones...");
            viewModel.cargarPropiedadesSiNecesario(dbHelper);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }



}
