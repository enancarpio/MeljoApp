package com.example.meljo.views.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * Fragmento que muestra los mensajes del usuario en tiempo real desde Firebase.
 */
public class MensajesUserFragment extends Fragment {

    private Button backButton, clearButton;
    private ListView messageList;
    private DBHelper dbHelper;
    private ListenerRegistration mensajesListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_mensaje_user, container, false);

        messageList = view.findViewById(R.id.messageList);
        backButton = view.findViewById(R.id.backButton);
        clearButton = view.findViewById(R.id.clearButton);
        dbHelper = new DBHelper();

        loadMessagesRealtime();

        backButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        clearButton.setOnClickListener(v ->
                dbHelper.clearMessagesFirebase(new AppCallback() {
                    @Override
                    public void onExito(String mensaje) {
                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String mensaje) {
                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                    }
                })
        );

        return view;
    }

    /**
     * Carga los mensajes en tiempo real desde Firebase para el usuario actual.
     */
    private void loadMessagesRealtime() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(requireContext(), "Usuario no logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        mensajesListener = FirebaseFirestore.getInstance()
                .collection("mensajes")
                .document(user.getUid())
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(requireContext(), "Error al cargar mensajes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayList<String> messages = new ArrayList<>();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String tipo = doc.getString("type");
                            String contenido = doc.getString("message");
                            messages.add(tipo + ": " + contenido);
                        }
                    } else {
                        messages.add("No hay mensajes en el historial.");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            messages
                    );
                    messageList.setAdapter(adapter);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mensajesListener != null) {
            mensajesListener.remove();
            mensajesListener = null;
        }
    }
}
