package com.example.apigoogle;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMapa;
    private EditText etLatitud, etLongitud, etNombreUbicacion;
    private Button btnIr, btnGuardarUbicacion, btnCerrarSesion;
    private ListView listViewUbicaciones;

    private ArrayList<Ubicacion> listaUbicaciones = new ArrayList<>();
    private ArrayAdapter<Ubicacion> adapter;

    // Clase para representar una ubicación
    private static class Ubicacion {
        String nombre;
        double latitud;
        double longitud;

        Ubicacion(String nombre, double latitud, double longitud) {
            this.nombre = nombre;
            this.latitud = latitud;
            this.longitud = longitud;
        }

        @NonNull
        @Override
        public String toString() {
            return nombre;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etLatitud = findViewById(R.id.etLatitud);
        etLongitud = findViewById(R.id.etLongitud);
        etNombreUbicacion = findViewById(R.id.etNombreUbicacion);
        btnIr = findViewById(R.id.btnIr);
        btnGuardarUbicacion = findViewById(R.id.btnGuardarUbicacion);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        listViewUbicaciones = findViewById(R.id.listViewUbicaciones);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaUbicaciones);
        listViewUbicaciones.setAdapter(adapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupListeners();
        cargarUbicaciones();
    }

    private void setupListeners() {
        btnIr.setOnClickListener(v -> irALaUbicacion());
        btnGuardarUbicacion.setOnClickListener(v -> guardarUbicacion());

        btnCerrarSesion.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        listViewUbicaciones.setOnItemClickListener((parent, view, position, id) -> {
            Ubicacion ubicacion = listaUbicaciones.get(position);
            LatLng latLng = new LatLng(ubicacion.latitud, ubicacion.longitud);
            mMapa.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        });

        listViewUbicaciones.setOnItemLongClickListener((parent, view, position, id) -> {
            mostrarDialogoEliminar(position);
            return true;
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMapa = googleMap;
        mMapa.getUiSettings().setZoomControlsEnabled(true);
        actualizarMarcadoresEnMapa();
        // Mover la cámara a una ubicación por defecto si la lista está vacía
        if (listaUbicaciones.isEmpty()) {
            LatLng cusco = new LatLng(-13.5320, -71.9675);
            mMapa.moveCamera(CameraUpdateFactory.newLatLng(cusco));
        }
    }

    private void irALaUbicacion() {
        String latitudStr = etLatitud.getText().toString();
        String longitudStr = etLongitud.getText().toString();

        if (latitudStr.isEmpty() || longitudStr.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese la latitud y longitud", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double latitud = Double.parseDouble(latitudStr);
            double longitud = Double.parseDouble(longitudStr);
            if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
                Toast.makeText(this, "Valores de latitud o longitud fuera de rango", Toast.LENGTH_SHORT).show();
                return;
            }
            LatLng nuevaUbicacion = new LatLng(latitud, longitud);
            mMapa.moveCamera(CameraUpdateFactory.newLatLngZoom(nuevaUbicacion, 15));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor, ingrese números válidos para la latitud y longitud", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarUbicacion() {
        String nombre = etNombreUbicacion.getText().toString();
        String latitudStr = etLatitud.getText().toString();
        String longitudStr = etLongitud.getText().toString();

        if (nombre.isEmpty() || latitudStr.isEmpty() || longitudStr.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double latitud = Double.parseDouble(latitudStr);
            double longitud = Double.parseDouble(longitudStr);

            if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
                Toast.makeText(this, "Valores de latitud o longitud fuera de rango", Toast.LENGTH_SHORT).show();
                return;
            }

            Ubicacion nuevaUbicacion = new Ubicacion(nombre, latitud, longitud);
            listaUbicaciones.add(nuevaUbicacion);
            adapter.notifyDataSetChanged();
            actualizarMarcadoresEnMapa();
            salvarUbicaciones();
            Toast.makeText(this, "Ubicación guardada: " + nombre, Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor, ingrese números válidos para la latitud y longitud", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoEliminar(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Ubicación")
                .setMessage("¿Estás seguro de que quieres eliminar esta ubicación?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    listaUbicaciones.remove(position);
                    adapter.notifyDataSetChanged();
                    actualizarMarcadoresEnMapa();
                    salvarUbicaciones();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void actualizarMarcadoresEnMapa() {
        if (mMapa == null) return;
        mMapa.clear();
        for (Ubicacion u : listaUbicaciones) {
            LatLng latLng = new LatLng(u.latitud, u.longitud);
            mMapa.addMarker(new MarkerOptions().position(latLng).title(u.nombre));
        }
    }

    private void salvarUbicaciones() {
        SharedPreferences prefs = getSharedPreferences("UbicacionesPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();
        for (Ubicacion u : listaUbicaciones) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("nombre", u.nombre);
                jsonObject.put("latitud", u.latitud);
                jsonObject.put("longitud", u.longitud);
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        editor.putString("listaUbicaciones", jsonArray.toString());
        editor.apply();
    }

    private void cargarUbicaciones() {
        SharedPreferences prefs = getSharedPreferences("UbicacionesPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("listaUbicaciones", null);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String nombre = jsonObject.getString("nombre");
                    double latitud = jsonObject.getDouble("latitud");
                    double longitud = jsonObject.getDouble("longitud");
                    listaUbicaciones.add(new Ubicacion(nombre, latitud, longitud));
                }
                adapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
