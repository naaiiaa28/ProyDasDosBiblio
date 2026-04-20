package com.example.biblioteca;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.content.Context;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {
    //puente entre la lista de datos y el RecyclerView. Se encarga de crear las tarjetas visuales y rellenarlas con los datos

    private List<MediaItem> lista;
    private OnItemClickListener listener;

    public interface OnItemClickListener { //los botones que se pueden pulsar
        void onEdit(MediaItem item);
        void onDelete(MediaItem item);
        void onClick(MediaItem item);
    }

    public MediaAdapter(List<MediaItem> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder { 
        TextView tvTitulo, tvTipo, tvEstado, tvPuntuacion, tvProgreso;
        CardView cardView;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(View v) {
            super(v);
            cardView     = v.findViewById(R.id.cardView);
            tvTitulo     = v.findViewById(R.id.tvTitulo);
            tvTipo       = v.findViewById(R.id.tvTipo);
            tvEstado     = v.findViewById(R.id.tvEstado);
            tvPuntuacion = v.findViewById(R.id.tvPuntuacion);
            tvProgreso   = v.findViewById(R.id.tvProgreso);
            btnEdit      = v.findViewById(R.id.btnEdit);
            btnDelete    = v.findViewById(R.id.btnDelete);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media, parent, false);
        return new ViewHolder(v);
    }

    //Se llama para cada tarjeta visible y es el que realmente rellena los datos en pantalla.
    @Override
    public void onBindViewHolder(ViewHolder h, int position) {
        MediaItem item = lista.get(position);
        h.tvTitulo.setText(item.getTitulo());
        h.tvTipo.setText(traducirTipo(h.itemView.getContext(), item.getTipo()) + " • " + item.getGenero());
        h.tvPuntuacion.setText("⭐ " + (int) item.getPuntuacion() + "/5");

        // Colores basados en valor en español (siempre así en BD)
        switch (item.getEstado() != null ? item.getEstado() : "") {
            case "Visto":       h.tvEstado.setTextColor(0xFF4CAF50); break;
            case "En progreso": h.tvEstado.setTextColor(0xFFFF9800); break;
            default:            h.tvEstado.setTextColor(0xFF9E9E9E); break;
        }

        // Traducir estado para mostrar
        h.tvEstado.setText(traducirEstado(h.itemView.getContext(), item.getEstado()));

        if ("Serie".equals(item.getTipo()) && "En progreso".equals(item.getEstado())) {
            h.tvProgreso.setVisibility(View.VISIBLE);
            h.tvProgreso.setText(" • T" + item.getTemporadaActual() + " E" + item.getCapituloActual());
        } else {
            h.tvProgreso.setVisibility(View.GONE);
        }

        h.cardView.setOnClickListener(v -> listener.onClick(item));
        h.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    //convierten los valores en español guardados en la BD al texto del idioma activo
    private String traducirEstado(Context context, String estadoES) {
        if (estadoES == null) return "";
        switch (estadoES) {
            case "Visto":       return context.getString(R.string.estado_visto);
            case "En progreso": return context.getString(R.string.estado_en_progreso);
            case "Pendiente":   return context.getString(R.string.estado_pendiente);
            default:            return estadoES;
        }
    }
    
    @Override
    public int getItemCount() { return lista.size(); }

    public void actualizarLista(List<MediaItem> nuevaLista) {
        lista = nuevaLista;
        notifyDataSetChanged();
    }

    private String traducirTipo(Context context, String tipoES) {
        if (tipoES == null) return "";
        switch (tipoES) {
            case "Película": return context.getString(R.string.tipo_pelicula);
            case "Serie":    return context.getString(R.string.tipo_serie);
            default:         return tipoES;
        }
    }
}