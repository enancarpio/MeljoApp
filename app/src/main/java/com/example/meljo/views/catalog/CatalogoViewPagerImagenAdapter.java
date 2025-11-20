package com.example.meljo.views.catalog;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.meljo.models.Imagen;

import java.util.List;

public class CatalogoViewPagerImagenAdapter extends FragmentStateAdapter {
    private final List<Imagen> imagenes;

    public CatalogoViewPagerImagenAdapter(FragmentActivity fragmentActivity, List<Imagen> imagenes) {
        super(fragmentActivity);
        this.imagenes = imagenes;
    }

    public CatalogoViewPagerImagenAdapter(Fragment fragment, List<Imagen> imagenes) {
        super(fragment);
        this.imagenes = imagenes;
    }

    @Override // androidx.viewpager2.adapter.FragmentStateAdapter
    public Fragment createFragment(int position) {
        Imagen imagen = this.imagenes.get(position);
        return CatalogoImagenSlideFragment.newInstance(imagen.getUrimagen(), position + 1, this.imagenes.size());
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.imagenes.size();
    }

    @Override // androidx.viewpager2.adapter.FragmentStateAdapter, androidx.recyclerview.widget.RecyclerView.Adapter
    public long getItemId(int position) {
        return this.imagenes.get(position).getUrimagen().hashCode();
    }

    @Override // androidx.viewpager2.adapter.FragmentStateAdapter
    public boolean containsItem(long itemId) {
        for (Imagen img : this.imagenes) {
            if (img.getUrimagen().hashCode() == itemId) {
                return true;
            }
        }
        return false;
    }
}
