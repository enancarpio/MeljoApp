package com.example.meljo.views.catalog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Imagen;
import com.example.meljo.models.Propiedad;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador del catálogo de propiedades.
 * Muestra información básica de cada propiedad y su multimedia asociada.
 */
public class CatalogoAdapter extends ListAdapter<Propiedad, CatalogoAdapter.ViewHolder> {



    private static final DiffUtil.ItemCallback<Propiedad> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Propiedad>() {

                @Override
                public boolean areItemsTheSame(@NonNull Propiedad oldItem, @NonNull Propiedad newItem) {
                    // Compara únicamente IDs únicos de las propiedades
                    return oldItem.getCasaid() != null && oldItem.getCasaid().equals(newItem.getCasaid());
                }

                @SuppressLint("DiffUtilEquals") //evita el error del == que sugiere cambiar a equals()
                @Override
                public boolean areContentsTheSame(@NonNull Propiedad oldItem, @NonNull Propiedad newItem) {
                    // Compara campos relevantes para detectar cambios en el contenido
                    if (oldItem == newItem) return true;

                    boolean sameDireccion = (oldItem.getDireccion() != null)
                            ? oldItem.getDireccion().equals(newItem.getDireccion())
                            : newItem.getDireccion() == null;

                    boolean sameLatitud = Double.compare(oldItem.getLatitud(), newItem.getLatitud()) == 0;
                    boolean sameLongitud = Double.compare(oldItem.getLongitud(), newItem.getLongitud()) == 0;

                    boolean sameDatereg = (oldItem.getDatereg() != null)
                            ? oldItem.getDatereg().equals(newItem.getDatereg())
                            : newItem.getDatereg() == null;

                    boolean sameDatemod = (oldItem.getDatemod() != null)
                            ? oldItem.getDatemod().equals(newItem.getDatemod())
                            : newItem.getDatemod() == null;

                    // Aquí puedes agregar más campos si los consideras importantes
                    return sameDireccion && sameLatitud && sameLongitud && sameDatereg && sameDatemod;
                }
            };

    private final Context context;
    private final OnBotonClickListener listener;
    private final DBHelper dbHelper;

    private final Map<String, MultimediaResult> multimediaCache = new HashMap<>();
    private final Map<String, List<Imagen>> imagenesCache = new HashMap<>();

    /** Interfaz para manejar clics en botones de cada propiedad */
    public interface OnBotonClickListener {
        void onVerImagenes(Propiedad propiedad);
        void onVerUbicacion(Propiedad propiedad);
        void onVerVideos(Propiedad propiedad);
    }

    public CatalogoAdapter(Context context,
                           List<Propiedad> listaInicial,
                           OnBotonClickListener listener,
                           DBHelper dbHelper) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
        this.dbHelper = dbHelper;
        submitList(listaInicial);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_catalog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Propiedad propiedad = getItem(position);
        final String casaId = propiedad.getCasaid();

        holder.itemView.setTag(casaId);
        holder.resetViews();

        // ─────────────────────────────
        // 1️⃣ Mostrar información básica
        // ─────────────────────────────
        holder.txtTitulo.setText(propiedad.getCasaid());
        holder.txtDescripcion.setText(
                propiedad.getCuartos() + " hab   " +
                        propiedad.getMetros() + " m²   " +
                        propiedad.getAseos() + " baños   " +
                        propiedad.getDescripcion()
        );
        holder.txtPrecio.setText(propiedad.getPrecio() + " €");
        holder.txtDireccion.setText("Inmueble en " + propiedad.getDireccion());

        // ─────────────────────────────
        // 2️⃣ Mostrar multimedia (fotos/videos) si ya está en caché
        // ─────────────────────────────
        if (multimediaCache.containsKey(casaId)) {
            MultimediaResult cached = multimediaCache.get(casaId);
            mostrarMultimedia(holder, cached.tieneFotos, cached.tieneVideos);
        } else {
            holder.txtSinMultimedia.setVisibility(View.GONE);
            dbHelper.obtenerMultimediaEstado(casaId, new AppCallback() {
                @Override
                public void onMultimediaExito(boolean tieneFotos, boolean tieneVideos) {
                    ((Activity) holder.itemView.getContext()).runOnUiThread(() -> {
                        if (casaId.equals(holder.itemView.getTag())) {
                            multimediaCache.put(casaId, new MultimediaResult(tieneFotos, tieneVideos));
                            mostrarMultimedia(holder, tieneFotos, tieneVideos);
                        }
                    });
                }

                @Override
                public void onError(String mensaje) {
                    ((Activity) holder.itemView.getContext()).runOnUiThread(() -> {
                        if (casaId.equals(holder.itemView.getTag())) {
                            holder.txtSinMultimedia.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        }

        // ─────────────────────────────
        // 3️⃣ Mostrar imágenes desde el cache del ViewModel
        // ─────────────────────────────
        List<Imagen> imagenes = imagenesCache.get(casaId);
        if (imagenes != null && !imagenes.isEmpty()) {
            mostrarImagenes(holder, imagenes);
            holder.txtSinMultimedia.setVisibility(View.GONE);
        } else {
            holder.txtSinMultimedia.setVisibility(View.VISIBLE);
        }

        // ─────────────────────────────
        // 4️⃣ Listeners
        // ─────────────────────────────
        holder.bUbicacion.setOnClickListener(v -> listener.onVerUbicacion(propiedad));
        holder.bImagenes.setOnClickListener(v -> listener.onVerImagenes(propiedad));
        holder.bVideos.setOnClickListener(v -> listener.onVerVideos(propiedad));
    }

    /** Muestra los botones correspondientes según la disponibilidad multimedia */
    private void mostrarMultimedia(ViewHolder holder, boolean tieneFotos, boolean tieneVideos) {
        holder.bImagenes.setVisibility(tieneFotos ? View.VISIBLE : View.GONE);
        holder.bVideos.setVisibility(tieneVideos ? View.VISIBLE : View.GONE);
        holder.txtSinMultimedia.setVisibility((tieneFotos || tieneVideos) ? View.GONE : View.VISIBLE);
    }

    /** Configura el ViewPager2 con las imágenes de la propiedad */
    private void mostrarImagenes(final ViewHolder holder, final List<Imagen> imagenes) {
        if (imagenes == null || imagenes.isEmpty()) return;

        ((Activity) holder.itemView.getContext()).runOnUiThread(() -> {
            CatalogoViewPagerImagenAdapter adapterVP =
                    new CatalogoViewPagerImagenAdapter((AppCompatActivity) context, imagenes);

            holder.viewPager.setAdapter(adapterVP);
            holder.viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
            holder.viewPager.setOffscreenPageLimit(adapterVP.getItemCount());

            int total = imagenes.size();
            holder.txtContador.setVisibility(View.VISIBLE);
            holder.txtContador.setText("Imagen 1 de " + total);

            holder.bIzq.setVisibility(total > 1 ? View.VISIBLE : View.GONE);
            holder.bDer.setVisibility(total > 1 ? View.VISIBLE : View.GONE);

            holder.bIzq.setOnClickListener(v -> {
                int current = holder.viewPager.getCurrentItem();
                holder.viewPager.setCurrentItem((current - 1 + total) % total, true);
            });

            holder.bDer.setOnClickListener(v -> {
                int current = holder.viewPager.getCurrentItem();
                holder.viewPager.setCurrentItem((current + 1) % total, true);
            });

            if (holder.pageChangeCallback != null) {
                holder.viewPager.unregisterOnPageChangeCallback(holder.pageChangeCallback);
            }

            holder.pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    holder.txtContador.setText("Imagen " + (position + 1) + " de " + total);
                }
            };

            holder.viewPager.registerOnPageChangeCallback(holder.pageChangeCallback);
            holder.viewPager.setVisibility(View.VISIBLE);
            holder.viewPager.requestLayout();
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.pageChangeCallback != null) {
            holder.viewPager.unregisterOnPageChangeCallback(holder.pageChangeCallback);
            holder.pageChangeCallback = null;
        }
    }


    /** ViewHolder que gestiona los elementos visuales de cada tarjeta */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageButton bIzq, bDer, bUbicacion, bImagenes, bVideos;
        TextView txtTitulo, txtDescripcion, txtPrecio, txtDireccion, txtSinMultimedia, txtContador;
        ViewPager2 viewPager;
        ViewPager2.OnPageChangeCallback pageChangeCallback;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txtTituloCasa);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcion);
            txtPrecio = itemView.findViewById(R.id.txtPrecio);
            txtDireccion = itemView.findViewById(R.id.txtDireccion);
            txtSinMultimedia = itemView.findViewById(R.id.txtSinMultimedia);
            txtContador = itemView.findViewById(R.id.txtContadorImagen);
            viewPager = itemView.findViewById(R.id.viewPagerImagenes);
            bIzq = itemView.findViewById(R.id.btnIzquierda);
            bDer = itemView.findViewById(R.id.btnDerecha);
            bUbicacion = itemView.findViewById(R.id.btnUbicacion);
            bImagenes = itemView.findViewById(R.id.btnImagenes);
            bVideos = itemView.findViewById(R.id.btnVideos);
        }

        /** Oculta los elementos que no deben mostrarse hasta tener datos */
        void resetViews() {
            bImagenes.setVisibility(View.GONE);
            bVideos.setVisibility(View.GONE);
            txtSinMultimedia.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);
            txtContador.setVisibility(View.GONE);
            bIzq.setVisibility(View.GONE);
            bDer.setVisibility(View.GONE);
        }
    }

    /** Clase auxiliar para mantener el estado multimedia */
    static class MultimediaResult {
        boolean tieneFotos;
        boolean tieneVideos;

        MultimediaResult(boolean tieneFotos, boolean tieneVideos) {
            this.tieneFotos = tieneFotos;
            this.tieneVideos = tieneVideos;
        }
    }

    public void setImagenesCache(Map<String, List<Imagen>> nuevasImagenes) {
        if (nuevasImagenes != null) {
            this.imagenesCache.clear();
            this.imagenesCache.putAll(nuevasImagenes);
            notifyDataSetChanged(); // 🔥 redibuja las propiedades
            Log.d("CATALOGO_ADAPTER", "♻️ Cache de imágenes actualizada: " + nuevasImagenes.size() + " propiedades");
        }
    }

    public void setMultimediaCache(Map<String, MultimediaResult> nuevoCache) {
        if (nuevoCache != null) {
            this.multimediaCache.clear();
            this.multimediaCache.putAll(nuevoCache);
            notifyDataSetChanged(); // 🔥 fuerza a redibujar la lista
            Log.d("CATALOGO_ADAPTER", "🎥 Cache multimedia actualizada: " + nuevoCache.size() + " propiedades");
        }
    }


}
