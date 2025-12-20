package com.example.meljo.views.chat;

import android.os.Bundle;
import android.util.Log;
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
import com.example.meljo.network.DialogflowClient;
import com.example.meljo.network.DialogflowRequest;
import com.example.meljo.network.DialogflowResponse;
import com.example.meljo.network.DialogflowService;
import com.example.meljo.network.DialogflowTokenProvider;
import com.example.meljo.network.OpenAIClient;
import com.example.meljo.network.OpenAIRequest;
import com.example.meljo.network.OpenAIResponse;
import com.example.meljo.network.OpenAIService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private static final String CHAT_KEY = BuildConfig.GROQ_API_KEY;
    private static final String GROQ_MOD = BuildConfig.GROQ_MODEL;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ChatAdapter adapter;
    private Button btnHistorial, btnEnviar;
    private EditText etMensaje;
    private RecyclerView recyclerView;
    private final DBHelper dbHelper = new DBHelper();
    private List<Item> messageList;
    private static final Pattern ID_PATTERN = Pattern.compile("[a-zA-Z]+\\d+");

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

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        agregarMensajeBotSinGuardar("Bienvenido a MELJO APP");

        btnEnviar.setOnClickListener(v -> enviarMensaje());
        btnHistorial.setOnClickListener(v -> cargarHistorial());
    }


    private void solicitarDialogflow(String message) {

        executor.execute(() -> {
            try {
                String token = "Bearer " + DialogflowTokenProvider.getAccessToken();
                llamarDialogflow(token, message);

            } catch (IOException e) {
                requireActivity().runOnUiThread(() ->
                        agregarMensajeBot("Error de autenticación con Dialogflow")
                );
            }
        });
    }
    private void llamarDialogflow(String token, String message) {

        String projectId = "gbot-trmq";
        String sessionId = UUID.randomUUID().toString();

        DialogflowService api =
                DialogflowClient.getClient().create(DialogflowService.class);

        DialogflowRequest request = new DialogflowRequest(message);

        api.detectIntent(
                token,
                projectId,
                sessionId,
                request
        ).enqueue(new Callback<DialogflowResponse>() {

            @Override
            public void onResponse(Call<DialogflowResponse> call,
                                   Response<DialogflowResponse> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    requireActivity().runOnUiThread(() ->
                            getGptResponse(message)
                    );
                    return;
                }

                DialogflowResponse.QueryResult result = response.body().queryResult;

                if (result == null || result.intent == null) {
                    requireActivity().runOnUiThread(() ->
                            getGptResponse(message)
                    );
                    return;
                }

                String intent = result.intent.displayName;

                Log.e("CHAT_DEBUG", "🎯 Intent Dialogflow: " + intent);

                requireActivity().runOnUiThread(() ->
                        procesarIntent(intent, message)
                );
            }

            @Override
            public void onFailure(Call<DialogflowResponse> call, Throwable t) {
                requireActivity().runOnUiThread(() ->
                        getGptResponse(message)
                );
            }
        });
    }

    private void procesarIntent(String intent, String message) {
        switch (intent) {
            case "saludo":
                agregarMensajeBotSinGuardar("¡Hola! 😊 ¿En qué puedo ayudarte hoy?");
                agregarMensajeBot("Resuelto");
                break;
            case "despedida":
                agregarMensajeBotSinGuardar("¡Chao! 😊 ¡Que tengas un excelente día!");
                agregarMensajeBot("Resuelto");
                break;
            case "contacto":
                agregarMensajeBotSinGuardar("MELJO CONSTRUCCIONES\nCompañía Ltda.\n" +
                        "Manta - Ecuador\nCONSTRUCCIÓN\nY VENTA DE INMUEBLES\n+593 5 292 7367\n" +
                        "www.meljocontrucciones.ec"
                );
                agregarMensajeBot("Resuelto");
                break;
            case "todas":
                listarTodasPropiedades();
                agregarMensajeBot("Resuelto");
                break;
            case "buscar":
                String nombre = extraerNbPropiedad(message);
                if (nombre != null) {
                    buscarPropiedadDirecta(nombre);
                    agregarMensajeBot("Resuelto");
                }
                else {
                    agregarMensajeBotSinGuardar("¿Qué propiedad deseas buscar?");
                }
                break;
            case "filtrar":
                Map<String, String> filtros = extraerFiltrosAvanzados(message);
                if (filtros.isEmpty()) {
                    agregarMensajeBotSinGuardar("No pude identificar filtros. Prueba con: 'propiedades con más de 3 cuartos'.");
                } else {
                    agregarMensajeBotSinGuardar("Aplicando filtros: " + filtros);

                    dbHelper.buscarPropiedadesFiltradas(filtros, new AppCallback() {

                        @Override
                        public void onPropFragYnewEditYcataYchatExito(List<Propiedad> lista) {

                            if (lista.isEmpty()) {
                                agregarMensajeBotSinGuardar("No encontré propiedades con esos filtros.");
                                return;
                            }

                            agregarMensajeBotSinGuardar("Encontré " + lista.size() + " propiedades:");

                            for (Propiedad p : lista) {
                                agregarMensajeBotSinGuardar(formatearPropiedad(p, true));
                                agregarMensajeBot("Resuelto");
                            }
                        }

                        @Override
                        public void onError(String error) {
                            agregarMensajeBotSinGuardar("Ocurrió un error al aplicar los filtros: " + error);
                        }
                    });
                }
                break;
            default:
                getGptResponse(message);
                break;
        }
    }
    private void procesarFiltro(String texto, String campo, Map<String, String> filtros) {

        Log.e("CHAT_DEBUG", "🔍 procesarFiltro → campo=" + campo + ", texto=" + texto);

        String regexCampo;

        switch (campo) {
            case "precio":
            case "precios":
                regexCampo = "\\b(precio|precios)\\b";
                break;
            case "metro":
            case "metros":
                regexCampo = "\\b(metro|metros)\\b";
                campo = "metros";
                break;
            case "cuarto":
            case "cuartos":
                regexCampo = "\\b(cuarto|cuartos|habitacion|habitaciones)\\b";
                campo = "cuartos";
                break;
            case "aseo":
            case "aseos":
                regexCampo = "\\b(aseo|aseos|baño|baños)\\b";
                campo = "aseos";
                break;
            default:
                regexCampo = "\\b" + campo + "\\b";
        }

        Pattern pattern = Pattern.compile(regexCampo);
        Matcher matcher = pattern.matcher(texto);

        if (!matcher.find()) {
            Log.e("CHAT_DEBUG", "❌ No contiene el campo (sing/plural): " + campo);
            return;
        }

        // 🔠 Normalizar números en letras antes de extraer
        String textoNormalizado = normalizarNumerosEnLetras(texto);
        int valor = extraerNumero(textoNormalizado);

        Log.e("CHAT_DEBUG", "   ↳ Valor numérico detectado: " + valor);

        if (valor == -1) {
            Log.e("CHAT_DEBUG", "❌ No se detectó número válido");
            return;
        }

        if (texto.contains("más de") || texto.contains("mas de")) {
            filtros.put(campo, "gte." + valor);
            Log.e("CHAT_DEBUG", "   ✔ Interpretado 'más de " + valor + "' como >= " + valor + " para " + campo);
        } else if (texto.contains("menos de")) {
            filtros.put(campo, "lte." + valor);
            Log.e("CHAT_DEBUG", "   ✔ Interpretado 'menos de " + valor + "' como <= " + valor + " para " + campo);
        } else {
            filtros.put(campo, "eq." + valor);
            Log.e("CHAT_DEBUG", "   ✔ Filtro aplicado: " + campo + " = " + valor);
        }
    }
    private Map<String, String> extraerFiltrosAvanzados(String texto) {
        Log.e("CHAT_DEBUG", "📥 Entró a extraerFiltrosAvanzados con: " + texto);

        texto = normalizarNumerosEnLetras(texto);

        String txt = texto.toLowerCase();
        Map<String, String> filtros = new HashMap<>();

        // Filtros numéricos
        procesarFiltro(txt, "precio", filtros);
        procesarFiltro(txt, "metros", filtros);
        procesarFiltro(txt, "cuartos", filtros);
        procesarFiltro(txt, "aseos", filtros);

        // Disponibles o vendidas
        detectarDisponiblesOVendidas(txt, filtros);

        Log.e("CHAT_DEBUG", "📤 Filtros obtenidos: " + filtros);

        return filtros;
    }
    private int extraerNumero(String texto) {
        Log.e("CHAT_DEBUG", "🔢 extraerNumero desde: " + texto);

        Matcher matcher = Pattern.compile("\\d+").matcher(texto);

        if (matcher.find()) {
            int num = Integer.parseInt(matcher.group());
            Log.e("CHAT_DEBUG", "   ✔ Número encontrado: " + num);
            return num;
        }

        Log.e("CHAT_DEBUG", "   ❌ No se encontró número");
        return -1;
    }
    private String normalizarNumerosEnLetras(String texto) {

        Log.e("CHAT_DEBUG", "🔠 Normalizando números en letras: " + texto);

        Map<String, Integer> numeros = new HashMap<>();
        numeros.put("cero", 0);
        numeros.put("un", 1);
        numeros.put("uno", 1);
        numeros.put("una", 1);
        numeros.put("dos", 2);
        numeros.put("tres", 3);
        numeros.put("cuatro", 4);
        numeros.put("cinco", 5);
        numeros.put("seis", 6);
        numeros.put("siete", 7);
        numeros.put("ocho", 8);
        numeros.put("nueve", 9);
        numeros.put("diez", 10);
        numeros.put("once", 11);
        numeros.put("doce", 12);
        numeros.put("trece", 13);
        numeros.put("catorce", 14);
        numeros.put("quince", 15);
        numeros.put("dieciséis", 16);
        numeros.put("dieciseis", 16);
        numeros.put("diecisiete", 17);
        numeros.put("dieciocho", 18);
        numeros.put("diecinueve", 19);
        numeros.put("veinte", 20);

        String resultado = texto.toLowerCase();

        for (Map.Entry<String, Integer> entry : numeros.entrySet()) {
            resultado = resultado.replaceAll(
                    "\\b" + entry.getKey() + "\\b",
                    String.valueOf(entry.getValue())
            );
        }

        Log.e("CHAT_DEBUG", "🔢 Texto normalizado: " + resultado);
        return resultado;
    }
    private void detectarDisponiblesOVendidas(String texto, Map<String, String> filtros) {

        Log.e("CHAT_DEBUG", "🟦 detectarDisponiblesOVendidas() texto=" + texto);

        String t = texto.toLowerCase();

        // Disponible
        if (t.contains("disponible") ||
                t.contains("disponibles") ||
                t.contains("no vendida") ||
                t.contains("no vendido") ||
                t.contains("no vendidas") ||
                t.contains("no vendidos")) {

            filtros.put("vendido", "eq.false");
            Log.e("CHAT_DEBUG", "   🟢 Detectado: disponible → vendido=false");
        }

        // Vendida
        if (t.contains("vendida") || t.contains("vendidas") ||
                t.contains("vendido") || t.contains("vendidos")) {

            filtros.put("vendido", "eq.true");
            Log.e("CHAT_DEBUG", "   🔴 Detectado: vendida → vendido=true");
        }

        Log.e("CHAT_DEBUG", "⬅️ Estado final filtros(vendido): " + filtros.get("vendido"));
    }
    private boolean contieneIdPropiedad(String texto) {
        return ID_PATTERN.matcher(texto).find();
    }
    private String extraerIdPropiedad(String texto) {
        Matcher matcher = ID_PATTERN.matcher(texto);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    private String formatearPropiedad(Propiedad p, boolean completo) {

        String info =
                "🏠 " + p.getCasaid() + "\n" +
                        "📍 " + p.getDireccion() + "\n" +
                        "💰 " + p.getPrecio() + " €\n" +
                        "📏 " + p.getMetros() + " m²";

        if (completo) {
            info += "\n🚪 Cuartos: " + p.getCuartos() +
                    "\n🛁 Aseos: " + p.getAseos();
        }

        return info;
    }
    private String extraerNbPropiedad(String texto) {
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
    private void buscarPropiedadDirecta(String nombre) {
        agregarMensajeBot("Buscando la propiedad \"" + nombre + "\" ...");

        dbHelper.buscarPropiedad(nombre, new AppCallback() {
            @Override
            public void onPropFragYnewEditYcataYchatExito(List<Propiedad> propiedades) {
                if (propiedades.isEmpty()) {
                    agregarMensajeBot("No se encontró ninguna propiedad con el ID \"" + nombre + "\".");
                    // Si no hay resultados, intenta con GPT (por si era otra cosa)
                    getGptResponse(nombre);
                } else {
                    for (Propiedad p : propiedades) {
                        agregarMensajeBotSinGuardar(formatearPropiedad(p, true));
                        agregarMensajeBot("Resuelto");
                    }
                }
            }

            @Override
            public void onError(String errorMsg) {
                agregarMensajeBot("Error al buscar: " + errorMsg+" Nombre:"+nombre);
            }
        });
    }
    private void listarTodasPropiedades() {

        agregarMensajeBotSinGuardar("📋 Listando todas las propiedades disponibles...");

        dbHelper.getTodasPropiedades(new AppCallback() {

            @Override
            public void onPropFragYnewEditYcataYchatExito(List<Propiedad> propiedades) {

                if (propiedades == null || propiedades.isEmpty()) {
                    agregarMensajeBot("No hay propiedades registradas.");
                    return;
                }

                for (Propiedad p : propiedades) {
                    agregarMensajeBotSinGuardar(formatearPropiedad(p, true));
                }

                agregarMensajeBotSinGuardar("✅ Listado completo");
            }

            @Override
            public void onError(String errorMsg) {
                agregarMensajeBot("❌ Error al listar propiedades: " + errorMsg);
            }
        });
    }

    private void enviarMensaje() {
        String inputOriginal = etMensaje.getText().toString().trim();
        Log.e("CHAT_DEBUG", "📝 Texto original: " + inputOriginal);
        Log.e("CHAT_DEBUG", "🔎 contieneIdPropiedad: " + contieneIdPropiedad(inputOriginal));
        Log.e("CHAT_DEBUG", "🆔 extraerIdPropiedad: " + extraerIdPropiedad(inputOriginal));

        if (inputOriginal.isEmpty()) return;

        messageList.add(new Item2(inputOriginal));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        dbHelper.insertUserMessage(inputOriginal);

// Detecta IDs dentro del texto
        if (contieneIdPropiedad(inputOriginal)) {
            String id = extraerIdPropiedad(inputOriginal);
            Log.e("CHAT_DEBUG", "🎯 ID detectado: " + id);
            buscarPropiedadDirecta(id);
            etMensaje.setText("");
            return;
        } else {
            // Enviar a Wit.ai
            //getWitResponse(limpiarTextoParaWit(inputOriginal));

            // Enviar a Dialogflow
            solicitarDialogflow(inputOriginal);

        }

        etMensaje.setText("");
    }
    private void cargarHistorial() {
        messageList.clear();
        adapter.notifyDataSetChanged();

        dbHelper.getHistorialMensajesLogueado(new MensajesCallback<List<Map<String, Object>>>() {
            @Override
            public void onGetHistoryUserMsgSuccess(List<Map<String, Object>> mensajes) {
                if (mensajes.isEmpty()) {
                    agregarMensajeBotSinGuardar("No hay historial en la BD de Firebase.");
                    return;
                }

                for (Map<String, Object> msg : mensajes) {
                    String tipo = (String) msg.get("type");
                    String contenido = (String) msg.get("message");

                    if ("user".equals(tipo)) {
                        messageList.add(new Item2(contenido));
                    } else {
                        messageList.add(new Item1(contenido));
                    }
                }

                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onError(String mensaje) {
                agregarMensajeBotSinGuardar("Error al obtener historial: " + mensaje);
            }
        });
    }
    private void agregarMensajeBotBase(String mensaje, boolean guardar) {

        requireActivity().runOnUiThread(() -> {
            messageList.add(new Item1(mensaje));
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);

            if (guardar) {
                dbHelper.insertBotResponse(mensaje);
            }
        });
    }
    private void agregarMensajeBot(String mensaje) {
        agregarMensajeBotBase(mensaje, true);
    }
    private void agregarMensajeBotSinGuardar(String mensaje) {
        agregarMensajeBotBase(mensaje, false);
    }

    private void getGptResponse(String message) {
        Log.e("CHAT_DEBUG", "🤖 Usando GPT porque no coincidió. Mensaje: " + message);

        OpenAIService gptApi = OpenAIClient.getClient().create(OpenAIService.class);
        List<OpenAIRequest.Message> messages = new ArrayList<>();
        messages.add(new OpenAIRequest.Message("system", "Asesor IA de MELJO"));
        messages.add(new OpenAIRequest.Message("user", message));

        OpenAIRequest request = new OpenAIRequest(GROQ_MOD, messages);

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
}
