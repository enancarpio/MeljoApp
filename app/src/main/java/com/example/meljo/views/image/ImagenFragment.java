package com.example.meljo.views.image;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Imagen;
import com.example.meljo.views.catalog.CatalogoViewModel;

import java.util.*;

public class ImagenFragment extends Fragment {

    private RecyclerView recyclerView;
    private ImagenAdapter adapter;
    private DBHelper dbHelper;
    private EditText etBuscar;
    private RadioButton rbOrden;
    private Spinner sFiltro;
    private List<Imagen> listaCompleta = new ArrayList<>();
    private boolean ordenAscendente = true;
    private CatalogoViewModel catalogoViewModel; // 🔥 Conexión global al catálogo

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_model, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        sFiltro = view.findViewById(R.id.sFiltro);
        rbOrden = view.findViewById(R.id.rbOrden);
        etBuscar = view.findViewById(R.id.etBuscar);
        Button btnNuevo = view.findViewById(R.id.btnNuevo);

        dbHelper = new DBHelper();
        catalogoViewModel = new ViewModelProvider(requireActivity()).get(CatalogoViewModel.class); // 🔗

        adapter = new ImagenAdapter(new ArrayList<>(), new ImagenAdapter.OnImagenClickListener() {
            @Override
            public void onEditarClick(Imagen imagen) {
                ImagenEditarDialogFragment dialog = new ImagenEditarDialogFragment();
                dialog.setImagen(imagen);
                dialog.setListener(new ImagenDialogListener() {
                    @Override public void onImagenGuardada() {}

                    @Override
                    public void onImagenActualizada(Imagen imagenActualizada) {
                        for (int i = 0; i < listaCompleta.size(); i++) {
                            if (listaCompleta.get(i).imagenid == imagenActualizada.imagenid) {
                                listaCompleta.set(i, imagenActualizada);
                                break;
                            }
                        }
                        aplicarFiltroYOrden();

                        catalogoViewModel.actualizarImagenEnCache(imagenActualizada); // 🔥 Actualiza cache global
                        notificarCatalogoCambio();
                    }
                });
                dialog.show(requireActivity().getSupportFragmentManager(), "EditarImagen");
            }

            @Override
            public void onEliminarClick(Imagen imagen) {
                dbHelper.eliminarImagen(imagen.imagenid, new AppCallback() {
                    @Override
                    public void onExito(String mensaje) {
                        requireActivity().runOnUiThread(() -> {
                            listaCompleta.removeIf(i -> i.imagenid == imagen.imagenid);
                            aplicarFiltroYOrden();

                            catalogoViewModel.eliminarImagenDeCache(imagen.imagenid); // 🔥 Remueve del cache global
                            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
                            notificarCatalogoCambio();
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

        configurarControles(view);
        cargarImagenes();
    }

    // ───────────────────────────────
    // ⚙️ Configuración de eventos UI
    // ───────────────────────────────
    private void configurarControles(View view) {
        sFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { aplicarFiltroYOrden(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { aplicarFiltroYOrden(); }
        });

        Button btnNuevo = view.findViewById(R.id.btnNuevo);
        btnNuevo.setOnClickListener(v -> {
            ImagenNuevoDialogFragment dialog = new ImagenNuevoDialogFragment();
            dialog.setListener(new ImagenDialogListener() {
                @Override
                public void onImagenGuardada() {
                    cargarImagenes(); // Recarga toda la lista
                    notificarCatalogoCambio();
                }

                @Override
                public void onImagenActualizada(Imagen imagenActualizada) {
                    for (int i = 0; i < listaCompleta.size(); i++) {
                        if (listaCompleta.get(i).imagenid == imagenActualizada.imagenid) {
                            listaCompleta.set(i, imagenActualizada);
                            adapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    aplicarFiltroYOrden();

                    catalogoViewModel.actualizarImagenEnCache(imagenActualizada); // 🔥
                    notificarCatalogoCambio();
                }
            });
            dialog.show(getParentFragmentManager(), "NuevaImagen");
        });
    }

    // ───────────────────────────────
    // 📦 Cargar imágenes desde Supabase
    // ───────────────────────────────
    private void cargarImagenes() {
        dbHelper.getAllImagenes(new AppCallback() {
            @Override
            public void onImagenFragYcatalogoExito(List<Imagen> imagenes) {
                requireActivity().runOnUiThread(() -> {
                    listaCompleta.clear();
                    listaCompleta.addAll(imagenes);
                    recargarSpinnerPropiedades();
                    aplicarFiltroYOrden();

                    // 🔥 Sincronizar todas las imágenes en el cache global
                    Map<String, List<Imagen>> agrupadas = new HashMap<>();
                    for (Imagen img : imagenes) {
                        if (img.casaid == null) continue;
                        agrupadas.computeIfAbsent(img.casaid, k -> new ArrayList<>()).add(img);
                    }
                    for (String casaId : agrupadas.keySet()) {
                        catalogoViewModel.actualizarImagenesDePropiedad(casaId, agrupadas.get(casaId));
                    }

                    Log.d("ImagenFragment", "✅ Imágenes cargadas: " + imagenes.size());
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

    private void recargarSpinnerPropiedades() {
        Set<String> nbcasas = new LinkedHashSet<>();
        nbcasas.add("Todos");
        for (Imagen img : listaCompleta) {
            if (img.casaid != null && !img.casaid.trim().isEmpty())
                nbcasas.add(img.casaid);
        }

        ArrayAdapter<String> filtroAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>(nbcasas)
        );
        filtroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sFiltro.setAdapter(filtroAdapter);
    }


    // ───────────────────────────────
    // 🔍 Filtrado, búsqueda y orden
    // ───────────────────────────────
    private void aplicarFiltroYOrden() {
        if (sFiltro.getSelectedItem() == null) return;

        String filtro = sFiltro.getSelectedItem().toString();
        String buscar = etBuscar.getText().toString().trim().toLowerCase();

        List<Imagen> filtradas = new ArrayList<>();
        for (Imagen img : listaCompleta) {
            boolean coincideFiltro = filtro.equals("Todos") || (img.casaid != null && img.casaid.equals(filtro));
            boolean coincideBusqueda =
                    buscar.isEmpty() ||
                            (img.casaid != null && img.casaid.toLowerCase().contains(buscar)) ||
                            (img.nbimagen != null && img.nbimagen.toLowerCase().contains(buscar)) ||
                            (img.urimagen != null && img.urimagen.toLowerCase().contains(buscar)) ||
                            (img.tipo != null && img.tipo.toLowerCase().contains(buscar));

            if (coincideFiltro && coincideBusqueda) filtradas.add(img);
        }

        filtradas.sort((a, b) -> {
            String c1 = a.casaid != null ? a.casaid : "";
            String c2 = b.casaid != null ? b.casaid : "";
            return ordenAscendente ? c1.compareToIgnoreCase(c2) : c2.compareToIgnoreCase(c1);
        });

        adapter.setListaNueva(filtradas);
    }

    // ───────────────────────────────
    // 🔥 Notificar al catálogo
    // ───────────────────────────────
    private void notificarCatalogoCambio() {
        catalogoViewModel.marcarRecargaPendiente(dbHelper);
        Log.d("ImagenFragment", "📢 Notificado a CatalogoViewModel (recarga pendiente)");
    }
}
