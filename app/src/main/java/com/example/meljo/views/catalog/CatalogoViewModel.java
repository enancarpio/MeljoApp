package com.example.meljo.views.catalog;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.models.Imagen;
import com.example.meljo.models.Propiedad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogoViewModel extends ViewModel {

    // 🏠 Lista de propiedades
    private final MutableLiveData<List<Propiedad>> listaOriginal = new MutableLiveData<>(new ArrayList<>());

    // 🖼️ Cache de imágenes y multimedia
    private final MutableLiveData<Map<String, List<Imagen>>> imagenesCache = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Map<String, CatalogoAdapter.MultimediaResult>> multimediaCache = new MutableLiveData<>(new HashMap<>());

    // ⏳ Estado de carga
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    // 🔄 Bandera de recarga entre fragments
    private final MutableLiveData<Boolean> recargarCatalogo = new MutableLiveData<>(false);

    private boolean cargaIniciada = false;

    // ───────────────────────────────
    // 🔹 Getters LiveData
    // ───────────────────────────────
    public LiveData<List<Propiedad>> getListaOriginal() { return listaOriginal; }
    public LiveData<Map<String, List<Imagen>>> getImagenesCache() { return imagenesCache; }
    public LiveData<Map<String, CatalogoAdapter.MultimediaResult>> getMultimediaCache() { return multimediaCache; }
    public LiveData<Boolean> getCargando() { return cargando; }

    public void setImagenesCache(Map<String, List<Imagen>> cache) {
        if (cache == null) cache = new HashMap<>();
        imagenesCache.postValue(cache);
    }

    public void setMultimediaCache(Map<String, CatalogoAdapter.MultimediaResult> cache) {
        if (cache == null) cache = new HashMap<>();
        multimediaCache.postValue(cache);
    }

    // ───────────────────────────────
    // 🏠 Carga inicial (solo una vez)
    // ───────────────────────────────
    public void cargarPropiedadesSiNecesario(DBHelper dbHelper) {
        Log.d("CatalogoViewModel", "🟦 cargarPropiedadesSiNecesario() → cargaIniciada=" + cargaIniciada);
        if (!cargaIniciada) {
            iniciarCargaCompleta(dbHelper);
        }
    }

    private void iniciarCargaCompleta(DBHelper dbHelper) {
        cargaIniciada = true;
        cargando.postValue(true);

        dbHelper.getTodasPropiedades(new AppCallback() {
            @Override
            public void onPropFragYnewEditYcataYchatExito(List<Propiedad> propiedades) {
                listaOriginal.postValue(propiedades != null ? propiedades : new ArrayList<>());
                cargando.postValue(false);
                Log.d("CatalogoViewModel", "✅ Propiedades cargadas: " + (propiedades != null ? propiedades.size() : 0));
            }

            @Override
            public void onError(String error) {
                Log.e("CatalogoViewModel", "❌ Error al cargar propiedades: " + error);
                cargando.postValue(false);
                cargaIniciada = false;
            }
        });
    }

    // ───────────────────────────────
    // ♻️ Forzar recarga total
    // ───────────────────────────────
    public void forzarRecargaPropiedades(DBHelper dbHelper) {
        Log.d("CatalogoViewModel", "♻️ Forzando recarga completa...");
        cargaIniciada = false;
        listaOriginal.postValue(new ArrayList<>());
        // ❌ NO LIMPIAR imagenesCache
        // imagenesCache.postValue(new HashMap<>());
        // ❌ NO LIMPIAR multimediaCache para que no muestre el video como imagen
        //multimediaCache.postValue(new HashMap<>());
        iniciarCargaCompleta(dbHelper);
    }

    public void marcarRecargaPendiente(DBHelper dbHelper) {
        Log.d("CatalogoViewModel", "📢 marcarRecargaPendiente() → forzando recarga de propiedades");
        forzarRecargaPropiedades(dbHelper);
        recargarCatalogo.postValue(true);
    }

    // ───────────────────────────────
    // 🔄 Actualizar una propiedad puntual
    // ───────────────────────────────
    public void actualizarPropiedadEnLista(Propiedad propiedadActualizada) {
        List<Propiedad> lista = listaOriginal.getValue();
        if (lista == null) return;

        boolean encontrada = false;
        for (int i = 0; i < lista.size(); i++) {
            Propiedad p = lista.get(i);
            if (p.id == propiedadActualizada.id) {
                lista.set(i, propiedadActualizada);
                encontrada = true;
                break;
            }
        }

        if (!encontrada) lista.add(propiedadActualizada);

        listaOriginal.postValue(new ArrayList<>(lista));
        Log.d("CatalogoViewModel", "📢 Propiedad actualizada en lista ID=" + propiedadActualizada.id);
    }

    // ───────────────────────────────
    // 🖼️ Actualizar imagen en caché
    // ───────────────────────────────
    public void actualizarImagenEnCache(Imagen imagenActualizada) {
        if (imagenActualizada == null || imagenActualizada.casaid == null) return;

        Map<String, List<Imagen>> cacheActual = imagenesCache.getValue();
        if (cacheActual == null) cacheActual = new HashMap<>();

        String casaId = imagenActualizada.casaid;
        List<Imagen> listaImagenes = cacheActual.getOrDefault(casaId, new ArrayList<>());

        boolean encontrada = false;
        for (int i = 0; i < listaImagenes.size(); i++) {
            Imagen img = listaImagenes.get(i);
            if (img.imagenid == imagenActualizada.imagenid) {
                listaImagenes.set(i, imagenActualizada);
                encontrada = true;
                break;
            }
        }

        if (!encontrada) {
            listaImagenes.add(imagenActualizada);
        }

        cacheActual.put(casaId, listaImagenes);
        imagenesCache.postValue(new HashMap<>(cacheActual));

        actualizarMultimediaDesdeImagenes();

        Log.d("CatalogoViewModel", "🖼️ Imagen actualizada en cache casaId=" + casaId + ", imagenId=" + imagenActualizada.imagenid);
    }

    // ───────────────────────────────
    // 🗑️ Eliminar imagen de la caché global
    // ───────────────────────────────
    public void eliminarImagenDeCache(int imagenId) {
        if (imagenId <= 0) return;

        Map<String, List<Imagen>> cacheActual = imagenesCache.getValue();
        if (cacheActual == null || cacheActual.isEmpty()) return;

        boolean modificada = false;
        for (Map.Entry<String, List<Imagen>> entry : cacheActual.entrySet()) {
            List<Imagen> lista = entry.getValue();
            if (lista == null) continue;

            int sizeBefore = lista.size();
            lista.removeIf(img -> img.imagenid == imagenId);

            if (lista.size() != sizeBefore) {
                modificada = true;
                Log.d("CatalogoViewModel", "🗑️ Imagen eliminada de cache: imagenId=" + imagenId + ", casaId=" + entry.getKey());
            }
        }

        if (modificada) {
            imagenesCache.postValue(new HashMap<>(cacheActual));

            // ✅ Sincroniza multimediaCache para que desaparezca el icono 🎥 o 🖼️ si corresponde
            actualizarMultimediaDesdeImagenes();
        }
    }

    // ───────────────────────────────
    // ♻️ Actualizar todas las imágenes de una propiedad específica
    // ───────────────────────────────
    public void actualizarImagenesDePropiedad(String casaId, List<Imagen> nuevasImagenes) {
        if (casaId == null || casaId.isEmpty()) return;

        Map<String, List<Imagen>> cacheActual = imagenesCache.getValue();
        if (cacheActual == null) {
            cacheActual = new HashMap<>();
        }

        cacheActual.put(casaId, nuevasImagenes != null ? nuevasImagenes : new ArrayList<>());
        imagenesCache.postValue(new HashMap<>(cacheActual));

        actualizarMultimediaDesdeImagenes();

        Log.d("CatalogoViewModel", "♻️ Imágenes actualizadas para casaId=" + casaId +
                " → total=" + (nuevasImagenes != null ? nuevasImagenes.size() : 0));
    }

    public void actualizarMultimediaDesdeImagenes() {
        Map<String, List<Imagen>> cache = imagenesCache.getValue();
        if (cache == null || cache.isEmpty()) return;

        Map<String, CatalogoAdapter.MultimediaResult> nuevoMultimediaCache = new HashMap<>();

        for (Map.Entry<String, List<Imagen>> entry : cache.entrySet()) {
            String casaId = entry.getKey();
            List<Imagen> imagenes = entry.getValue();

            boolean tieneFotos = false;
            boolean tieneVideos = false;

            if (imagenes != null) {
                for (Imagen img : imagenes) {
                    if (img == null || img.tipo == null) continue;
                    String tipo = img.tipo.toLowerCase();
                    if (tipo.contains("video")) {
                        tieneVideos = true;
                    } else {
                        // cualquier otro tipo lo tratamos como foto
                        tieneFotos = true;
                    }
                }
            }

            // usa el constructor que espera (boolean, boolean)
            CatalogoAdapter.MultimediaResult result = new CatalogoAdapter.MultimediaResult(tieneFotos, tieneVideos);
            nuevoMultimediaCache.put(casaId, result);
        }

        multimediaCache.postValue(nuevoMultimediaCache);
        Log.d("CatalogoViewModel", "🎬 multimediaCache sincronizado con imagenesCache → casas: " + nuevoMultimediaCache.size());
    }

}
