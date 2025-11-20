package com.example.meljo.views.user;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.meljo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

/**
 * Fragmento que muestra el historial de mensajes del usuario autenticado.
 */
public class MensajesHistorialUserFragment extends Fragment {

    private ListView listView;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_mensaje_user, container, false);
        listView = view.findViewById(R.id.messageList);
        cargarHistorial();
        return view;
    }

    /**
     * Carga el historial de mensajes del usuario autenticado desde Firestore.
     */
    private void cargarHistorial() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "No hay usuario logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("mensajes")
                .document(uid)
                .collection("chat")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(this::mostrarHistorial)
                .addOnFailureListener(e -> {
                    Log.e("Historial", "Error cargando historial", e);
                    Toast.makeText(requireContext(), "Error al cargar historial", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Muestra el historial en el ListView.
     */
    private void mostrarHistorial(QuerySnapshot querySnapshot) {
        ArrayList<String> mensajes = new ArrayList<>();

        for (QueryDocumentSnapshot doc : querySnapshot) {
            String tipo = doc.getString("type");
            String msg = doc.getString("message");

            if (tipo != null && msg != null) {
                mensajes.add(tipo + ": " + msg);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                mensajes
        );

        listView.setAdapter(adapter);
    }
}
