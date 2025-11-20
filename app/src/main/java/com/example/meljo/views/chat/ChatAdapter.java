package com.example.meljo.views.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.R;

import java.util.List;

/* loaded from: classes7.dex */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Item> datos;

    public ChatAdapter(List<Item> datos) {
        this.datos = datos;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int position) {
        return this.datos.get(position).getType();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_enviado, parent, false);
            return new MiViewHolder(view);
        }
        View view2 = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_recibido, parent, false);
        return new MiViewHolder2(view2);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MiViewHolder) {
            MiViewHolder type1Holder = (MiViewHolder) holder;
            Item1 item = (Item1) this.datos.get(position);
            type1Holder.textView.setText(item.getTitle());
        } else {
            MiViewHolder2 type2Holder = (MiViewHolder2) holder;
            Item2 item2 = (Item2) this.datos.get(position);
            type2Holder.textView.setText(item2.getTitle());
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.datos.size();
    }

    public static class MiViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public MiViewHolder(View itemView) {
            super(itemView);
            this.textView = (TextView) itemView.findViewById(R.id.text_received);
        }
    }

    public static class MiViewHolder2 extends RecyclerView.ViewHolder {
        TextView textView;

        public MiViewHolder2(View itemView) {
            super(itemView);
            this.textView = (TextView) itemView.findViewById(R.id.text_sent);
        }
    }
}
