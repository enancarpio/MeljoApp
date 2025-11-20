package com.example.meljo.controllers;

import com.example.meljo.models.Imagen;
import com.example.meljo.models.Mensaje;
import com.example.meljo.models.Propiedad;
import com.example.meljo.models.Usuario;

import java.util.List;

/* loaded from: classes6.dex */
public interface AppCallback {
    default void onHistorialObtenidoError(Exception e) {
    }

    default void onHistorialObtenido(List<Mensaje> lista) {
    }

    default void onImagenNewEditExito(String mensaje, Imagen imagen) {
    }

    default void onImagenFragYcatalogoExito(List<Imagen> imagenes) {
    }

    default void onMultimediaExito(boolean tieneFotos, boolean tieneVideos) {
    }

    default void onPropNewEditExito(String mensaje, Propiedad propiedad) {
    }

    default void onPropFragYnewEditYcataYchatExito(List<Propiedad> propiedades) {
    }

    default void onExito(String mensaje) {
    }

    default void onHashPropiedadesExito(String hash) {
    }

    default void onHashImagenesExito(String hash) {
    }

    default void onLoginExitoso(Usuario usuario) {
    }

    default void onUsuarioVerificado(Usuario usuario) {
    }

    default void onUsuarioEliminado(boolean ok) {
    }

    default void onUsuarioInsertado(long id) {
    }

    default void onUsuariosCargados(List<Usuario> usuarios) {
    }

    default void onError(String mensaje) {
    }

    default void onVersionDualExito(long versionPropiedades, long versionImagenes) {}
    default void onPropiedadPorIdExito(Propiedad propiedad) {}
}
