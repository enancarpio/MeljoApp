package com.example.meljo.views.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meljo.BuildConfig;
import com.example.meljo.R;
import com.example.meljo.controllers.AppCallback;
import com.example.meljo.controllers.DBHelper;
import com.example.meljo.controllers.MensajesCallback;
import com.example.meljo.models.Propiedad;
import com.example.meljo.network.OpenAIClient;
import com.example.meljo.network.OpenAIRequest;
import com.example.meljo.network.OpenAIResponse;
import com.example.meljo.network.OpenAIService;
import com.example.meljo.network.WitClient;
import com.example.meljo.network.WitResponse;
import com.example.meljo.network.WitService;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private static final String WIT_KEY = BuildConfig.WIT_API_KEY;
    private static final String CHAT_KEY = BuildConfig.GROQ_API_KEY;
    private ChatAdapter adapter;
    private Button btnHistorial, btnEnviar;
    private EditText etMensaje;
    private RecyclerView recyclerView;
    private DBHelper dbHelper;
    private List<Item> messageList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etMensaje = view.findViewById(R.id.et2);
        btnEnviar = view.findViewById(R.id.benviar);
        btnHistorial = view.findViewById(R.id.bhistorial);
        recyclerView = view.findViewById(R.id.recyclerView);

        dbHelper = new DBHelper();
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        agregarMensajeBot("Bienvenido a MELJO APP");

        btnEnviar.setOnClickListener(v -> enviarMensaje());
        btnHistorial.setOnClickListener(v -> cargarHistorial());
    }

    private void enviarMensaje() {
        String inputOriginal = etMensaje.getText().toString().trim();
        if (inputOriginal.isEmpty()) return;

        messageList.add(new Item2(inputOriginal));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        dbHelper.insertUserMessage(inputOriginal);

// Detecta IDs dentro del texto
        if (contieneIdPropiedad(inputOriginal)) {
            String id = extraerIdPropiedad(inputOriginal);
            buscarPropiedadDirecta(id);
        } else {
            // Enviar a Wit.ai
            getWitResponse(limpiarTextoParaWit(inputOriginal));
        }

        etMensaje.setText("");
    }

    // Detecta IDs tipo "Piso001", "Casa05", "Depa10", etc.
// Devuelve true si encuentra algún ID dentro del texto
    private boolean contieneIdPropiedad(String texto) {
        // Busca cualquier palabra que empiece con piso, casa, depa, apartamento o terreno seguida de números
        Pattern pattern = Pattern.compile("(?i)\\b(piso|casa|depa|apartamento|terreno)\\d{1,4}\\b");
        Matcher matcher = pattern.matcher(texto);
        return matcher.find();
    }

    // Extrae el primer ID válido del texto
    private String extraerIdPropiedad(String texto) {
        Pattern pattern = Pattern.compile("(?i)\\b(piso|casa|depa|apartamento|terreno)\\d{1,4}\\b");
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }


    private void buscarPropiedadDirecta(String nombre) {
        agregarMensajeBot("Buscando la propiedad \"" + nombre + "\" ...");

        DBHelper dbHelper = new DBHelper();
        dbHelper.buscarPropiedad(nombre, new AppCallback() {
            @Override
            public void onPropFragYnewEditYcataYchatExito(List<Propiedad> propiedades) {
                if (propiedades.isEmpty()) {
                    agregarMensajeBot("No se encontró ninguna propiedad con el ID \"" + nombre + "\".");
                    // Si no hay resultados, intenta con GPT (por si era otra cosa)
                    getGptResponse(nombre);
                } else {
                    for (Propiedad p : propiedades) {
                        String info = "🏠 " + p.getCasaid() + "\n" +
                                "📍 " + p.getDireccion() + "\n" +
                                "💰 " + p.getPrecio() + " €\n" +
                                "📏 " + p.getMetros() + " m²";
                        agregarMensajeBot(info);
                    }
                }
            }

            @Override
            public void onError(String errorMsg) {
                agregarMensajeBot("Error al buscar: " + errorMsg);
            }
        });
    }

    private void getWitResponse(final String message) {
        WitService witApi = WitClient.getClient().create(WitService.class);
        Call<WitResponse> call = witApi.getMessage(
                WIT_KEY,
                message
        );

        call.enqueue(new Callback<WitResponse>() {
            @Override
            public void onResponse(Call<WitResponse> call, Response<WitResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    getGptResponse(message);
                    return;
                }

                WitResponse witResponse = response.body();

                // 🔒 Evita error si no hay intents
                if (witResponse.getIntents() == null || witResponse.getIntents().isEmpty()) {
                    getGptResponse(message);
                    return;
                }

                String intent = witResponse.getIntents().get(0).getName();
                if (intent == null) {
                    getGptResponse(message);
                    return;
                }

                switch (intent) {
                    case "saludo":
                        agregarMensajeBot("¡Hola! 😊 ¿En qué puedo ayudarte hoy?");
                        break;

                    case "buscarPropiedad":
                        String nombre = extraerNombreCasa(message);
                        if (nombre != null) {
                            agregarMensajeBot("Buscando la propiedad \"" + nombre + "\"...");
                            buscarPropiedadDirecta(nombre);
                        } else {
                            agregarMensajeBot("¿Qué propiedad te gustaría buscar?");
                        }
                        break;

                    case "filtrar_propiedades":
                        Map<String, String> filtros = extraerFiltrosAvanzados(message);
                        if (filtros.isEmpty()) {
                            agregarMensajeBot("No pude identificar filtros. Prueba con 'propiedades con más de 3 cuartos'.");
                        } else {
                            agregarMensajeBot("Aplicando filtros: " + filtros);
                        }
                        break;

                    default:
                        getGptResponse(message);
                        break;
                }
            }

            @Override
            public void onFailure(Call<WitResponse> call, Throwable t) {
                getGptResponse(message);
            }
        });
    }

    private void getGptResponse(String message) {
        OpenAIService gptApi = OpenAIClient.getClient().create(OpenAIService.class);
        List<OpenAIRequest.Message> messages = new ArrayList<>();
        messages.add(new OpenAIRequest.Message("system", "Eres asesor/a inmobiliario/a de MELJO"));
        messages.add(new OpenAIRequest.Message("user", message));

        OpenAIRequest request = new OpenAIRequest("llama-3.3-70b-versatile", messages);

        gptApi.getChatCompletion(
                CHAT_KEY,
                request
        ).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(Call<OpenAIResponse> call, Response<OpenAIResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String reply = response.body().choices.get(0).message.content;
                    agregarMensajeBot(reply);
                } else {
                    try {
                        agregarMensajeBot("Error: " + response.errorBody().string());
                    } catch (IOException e) {
                        agregarMensajeBot("Error sin contenido");
                    }
                }
            }

            @Override
            public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                agregarMensajeBot("Fallo conexión GPT: " + t.getMessage());
            }
        });
    }

    private void agregarMensajeBot(final String mensaje) {
        requireActivity().runOnUiThread(() -> {
            messageList.add(new Item1(mensaje));
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);

            // ✅ Inserta el mensaje del bot en BD
            dbHelper.insertBotResponse(mensaje);
        });
    }

    private String extraerNombreCasa(String texto) {
        if (texto == null || texto.trim().isEmpty()) return null;
        String limpio = texto.toLowerCase().replaceAll("[^a-záéíóúüñ0-9 ]", " ");
        String[] stopWords = {
                "quiero", "ver", "buscar", "propiedad", "propiedades", "muéstrame",
                "mostrar", "casa", "enseñame", "de", "la", "el", "un", "una", "las",
                "los", "en"
        };
        for (String stop : stopWords) {
            limpio = limpio.replaceAll("\\b" + stop + "\\b", " ");
        }
        limpio = limpio.trim().replaceAll(" +", " ");
        return limpio.isEmpty() ? null : limpio;
    }

    private String limpiarTextoParaWit(String texto) {
        return texto.replaceAll("(?i)\\b(casa|departamento|terreno)\\s\\d+\\b", "$1");
    }

    private Map<String, String> extraerFiltrosAvanzados(String texto) {
        String txt = texto.toLowerCase();
        Map<String, String> filtros = new HashMap<>();
        procesarFiltro(txt, "precio", filtros);
        procesarFiltro(txt, "metro", filtros);
        procesarFiltro(txt, "cuarto", filtros);
        procesarFiltro(txt, "aseo", filtros);
        return filtros;
    }

    private void procesarFiltro(String texto, String campo, Map<String, String> filtros) {
        if (!texto.contains(campo)) return;
        int valor = extraerNumero(texto);
        if (valor == -1) return;
        if (texto.contains("más de")) {
            filtros.put(campo, "gt." + valor);
        } else if (texto.contains("menos de")) {
            filtros.put(campo, "lt." + valor);
        } else {
            filtros.put(campo, "eq." + valor);
        }
    }

    private int extraerNumero(String texto) {
        Matcher matcher = Pattern.compile("\\d+").matcher(texto);
        return matcher.find() ? Integer.parseInt(matcher.group()) : -1;
    }

    private void cargarHistorial() {
        dbHelper.getHistorialMensajesLogueado(new MensajesCallback<List<Map<String, Object>>>() {
            @Override
            public void onGetHistoryUserMsgSuccess(List<Map<String, Object>> mensajes) {
                if (mensajes.isEmpty()) {
                    agregarMensajeBot("No hay historial en la BD de Firebase.");
                    return;
                }
                StringBuilder historial = new StringBuilder("Historial de mensajes:\n");
                for (Map<String, Object> msg : mensajes) {
                    String tipo = (String) msg.get("type");
                    String contenido = (String) msg.get("message");
                    historial.append(tipo).append(": ").append(contenido).append("\n");
                }
                agregarMensajeBot(historial.toString());
            }

            @Override
            public void onError(String mensaje) {
                agregarMensajeBot("Error al obtener historial: " + mensaje);
            }
        });
    }

}
