package com.example.meljo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.meljo.controllers.AppCallback;
import com.example.meljo.models.Usuario;
import com.example.meljo.views.InicioFragment;
import com.example.meljo.views.catalog.CatalogoFragment;
import com.example.meljo.views.chat.ChatFragment;
import com.example.meljo.views.image.ImagenFragment;
import com.example.meljo.views.login.LoginDialogFragment;
import com.example.meljo.views.message.MensajesHistorialFragment;
import com.example.meljo.views.message.MensajesUserFragment;
import com.example.meljo.views.property.PropiedadFragment;
import com.example.meljo.views.user.UsuarioDialogListener;
import com.example.meljo.views.user.UsuarioFragment;
import com.example.meljo.views.user.UsuarioNuevoDialogFragment;

public class MainActivity extends AppCompatActivity implements UsuarioDialogListener, AppCallback {
    private Menu menu;
    private Usuario usuarioActual;
    private boolean dobleClickParaSalir = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean abrirChatTrasLogin = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        usuarioActual = (Usuario) getIntent().getSerializableExtra("usuario_actual");

        if (savedInstanceState == null) {
            navegarA(new InicioFragment(), false);
        }

        verificarPermisosUbicacion();

        getOnBackPressedDispatcher().addCallback(this, new BackPressCallback(true, this));
    }

    private static class BackPressCallback extends OnBackPressedCallback {

        private final MainActivity activity;

        BackPressCallback(boolean enabled, MainActivity activity) {
            super(enabled);
            this.activity = activity;
        }

        @Override
        public void handleOnBackPressed() {
            FragmentManager fm = activity.getSupportFragmentManager();

            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            } else {
                if (activity.dobleClickParaSalir) {
                    activity.finish();
                    return;
                }

                activity.dobleClickParaSalir = true;
                Toast.makeText(activity, "Doble click para salir", Toast.LENGTH_SHORT).show();

                activity.handler.postDelayed(() -> activity.dobleClickParaSalir = false, 2000L);
            }
        }
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        actualizarMenu();
        return true;
    }

    private void actualizarMenu() {
        if (this.menu == null) return;

        boolean esAdmin = this.usuarioActual != null && this.usuarioActual.admin;
        boolean estaLogueado = this.usuarioActual != null;
        boolean hayInternet = hayConexionInternet();

        // 🔹 Menús administrativos
        this.menu.findItem(R.id.menu_casa).setVisible(esAdmin && hayInternet);
        this.menu.findItem(R.id.menu_imagen).setVisible(esAdmin && hayInternet);
        this.menu.findItem(R.id.menu_user).setVisible(esAdmin && hayInternet);
        this.menu.findItem(R.id.menu_historial).setVisible(esAdmin && hayInternet);
        this.menu.findItem(R.id.menu_historial_all).setVisible(esAdmin && hayInternet);

        // 🔹 Sesión
        this.menu.findItem(R.id.menu_registrarse).setVisible(!estaLogueado && hayInternet);
        this.menu.findItem(R.id.menu_iniciar_sesion).setVisible(!estaLogueado && hayInternet);
        this.menu.findItem(R.id.menu_cerrar_sesion).setVisible(estaLogueado && hayInternet);

        // 🔹 Si no hay internet, muestra aviso
        if (!hayInternet) {
            Toast.makeText(this, "Sin conexión a internet. Algunas funciones están deshabilitadas.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_registrarse) {
            new UsuarioNuevoDialogFragment(true).show(getSupportFragmentManager(), "registrar");
            return true;
        }
        if (id == R.id.menu_iniciar_sesion) {
            new LoginDialogFragment().show(getSupportFragmentManager(), "login");
            return true;
        }
        if (id == R.id.menu_salir) {
            finish();
            return true;
        }
        if (id == R.id.menu_cerrar_sesion) {
            this.usuarioActual = null;
            actualizarMenu();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            navegarA(new CatalogoFragment(), true);
            return true;
        }
        if (id == R.id.menu_casa) {
            navegarA(new PropiedadFragment(), true);
            return true;
        }
        if (id == R.id.menu_imagen) {
            navegarA(new ImagenFragment(), true);
            return true;
        }
        if (id == R.id.menu_user) {
            navegarA(new UsuarioFragment(), true);
            return true;
        }
        if (id == R.id.menu_historial) {
            navegarA(new MensajesUserFragment(), true);
            return true;
        }
        if (id == R.id.menu_historial_all) {
            navegarA(new MensajesHistorialFragment(), true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override // com.example.meljo.views.user.UsuarioDialogListener
    public void onUsuarioGuardado(Usuario nuevoUsuario) {
        Toast.makeText(this, "Usuario guardado", Toast.LENGTH_SHORT).show();
    }

    @Override // com.example.meljo.views.user.UsuarioDialogListener
    public void onUsuarioActualizado() {
        Toast.makeText(this, "Usuario actualizado", Toast.LENGTH_SHORT).show();
    }

    public Usuario getUsuarioActual() {
        return this.usuarioActual;
    }

    public void navegarA(Fragment nuevoFragment, boolean agregarAlBackStack) {
        String tag = nuevoFragment.getClass().getSimpleName();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.isAdded()) {
                transaction.hide(f);
            }
        }
        Fragment fragmentExistente = fm.findFragmentByTag(tag);
        if (fragmentExistente != null) {
            transaction.show(fragmentExistente);
        } else {
            transaction.add(R.id.fragmentContainerView, nuevoFragment, tag);
            if (agregarAlBackStack) {
                transaction.addToBackStack(tag);
            }
        }
        transaction.commitAllowingStateLoss();
        fm.executePendingTransactions();
        Log.d("MainActivity", "✅ Navegación completada → " + tag);
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        Log.d("LoginMainActivity", "✅ setUsuarioActual: " + usuario.nb + ", admin: " + usuario.admin);
        invalidateOptionsMenu();
    }

    public void setAbrirChatTrasLogin(boolean abrir) {
        this.abrirChatTrasLogin = abrir;
    }

    @Override // com.example.meljo.controllers.AppCallback
    public void onLoginExitoso(Usuario usuario) {
        setUsuarioActual(usuario);
        Toast.makeText(this, "Bienvenido " + usuario.nb, Toast.LENGTH_SHORT).show();
        if (this.abrirChatTrasLogin) {
            navegarA(new ChatFragment(), true);
            this.abrirChatTrasLogin = false;
        } else {
            navegarA(new CatalogoFragment(), true);
        }
    }

    private void verificarPermisosUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 1001);
        } else {
            Log.d("MainActivity", "✅ Permiso de ubicación ya concedido");
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == 0) {
                Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean nuncaPreguntar = !ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.ACCESS_FINE_LOCATION");
            if (nuncaPreguntar) {
                Toast.makeText(this, "Permiso denegado permanentemente. Ve a Ajustes para activarlo.", Toast.LENGTH_SHORT).show();
                abrirAjustesAplicacion();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void abrirAjustesAplicacion() {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
    public boolean hayConexionInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    private final BroadcastReceiver conexionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean conectado = hayConexionInternet();
            if (!conectado) {
                Toast.makeText(context, "Conexión perdida. Volviendo al inicio.", Toast.LENGTH_SHORT).show();
                navegarA(new InicioFragment(), false);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(conexionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        actualizarMenu(); // 🔁 Refresca según el estado actual
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(conexionReceiver);
    }


}
