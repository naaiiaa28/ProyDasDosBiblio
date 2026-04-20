package com.example.biblioteca;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.AlertDialog;
import android.content.Context;



public class MapaActivity extends FragmentActivity implements OnMapReadyCallback {


    private LatLng lastUserLocation;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private static final String API_KEY = "AIzaSyD5D94HIMhCBHFZEtrzZ9qtPD12YWjbvLw"; //la py key del google cloud

    @Override
    protected void attachBaseContext(Context newBase) { //idioma
        super.attachBaseContext(LanguageHelper.aplicarIdioma(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment == null) {
            Toast.makeText(this, "Mapa no cargado", Toast.LENGTH_LONG).show();
            return;
        }

        mapFragment.getMapAsync(this);

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        findViewById(R.id.btnRecargar).setOnClickListener(v -> recargarMapa());
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (mMap != null) {
                recargarDesdeGPS(); 
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {//mapa listo
        mMap = googleMap; 

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mostrarDialogoActivarGPS();
        } else {
            recargarDesdeGPS(); 
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                if (location != null) {

                    lastUserLocation = new LatLng(
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 14));

                    buscarCinesCercanos(lastUserLocation);
                }
            });

        // si se hace click en un marcador, se obtiene su placeId y se muestran los detalles del cine
        mMap.setOnMarkerClickListener(marker -> {
            String placeId = (String) marker.getTag();
            if (placeId != null) {
                obtenerDetallesLugar(placeId);
            }
            return false;
        });
    }

    private void buscarCinesCercanos(LatLng ubicacion) { //busca cines cercanos a la ubicacion dada usando la API de Google Places

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
            + "?location=" + ubicacion.latitude + "," + ubicacion.longitude
            + "&radius=20000"
            + "&type=movie_theater"
            + "&fields=place_id,name,geometry"
            + "&key=" + API_KEY;

        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder json = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                reader.close();

                runOnUiThread(() -> procesarCines(json.toString()));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void procesarCines(String json) { //procesa la respuesta JSON de la API de Places y agrega marcadores al mapa para cada cine encontrado
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray results = obj.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {

                JSONObject cine = results.getJSONObject(i);

                String nombre = cine.getString("name");
                String placeId = cine.getString("place_id");

                JSONObject location =
                        cine.getJSONObject("geometry").getJSONObject("location");

                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");

                LatLng pos = new LatLng(lat, lng);

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(nombre));

                marker.setTag(placeId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void obtenerDetallesLugar(String placeId) {//obtiene los detalles de un lugar usando su placeId y muestra la información en un diálogo

        String url = "https://maps.googleapis.com/maps/api/place/details/json"
                + "?place_id=" + placeId
                + "&key=" + API_KEY;

        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder json = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                reader.close();

                runOnUiThread(() -> mostrarDetalles(json.toString()));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void mostrarDetalles(String json) {//procesa la respuesta JSON de los detalles del lugar y muestra la información relevante en un diálogo
    try {
        org.json.JSONObject obj = new org.json.JSONObject(json);
        org.json.JSONObject result = obj.getJSONObject("result");

        String nombre = result.optString("name");
        double rating = result.optDouble("rating", 0);
        String telefono = result.optString("formatted_phone_number", "No disponible");
        String web = result.optString("website", "No disponible");

        String horario = "No disponible";
        if (result.has("opening_hours")) {
            org.json.JSONObject oh = result.getJSONObject("opening_hours");
            org.json.JSONArray weekday = oh.optJSONArray("weekday_text");

            if (weekday != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < weekday.length(); i++) {
                    sb.append(weekday.getString(i)).append("\n");
                }
                horario = sb.toString();
            }
        }

        StringBuilder msg = new StringBuilder();

        msg.append(nombre).append("\n\n");

        msg.append(getString(R.string.rating))
                .append(": ")
                .append(rating)
                .append("\n");

        msg.append(getString(R.string.phone))
                .append(": ")
                .append(telefono)
                .append("\n");

        msg.append(getString(R.string.website))
                .append(": ")
                .append(web)
                .append("\n\n");

        msg.append(getString(R.string.schedule))
                .append(":\n")
                .append(horario);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.cinema_details))
                .setMessage(msg.toString())
                .setPositiveButton(getString(R.string.close), null)
                .show();

    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Error mostrando detalles", Toast.LENGTH_SHORT).show();
    }
}

    private void mostrarDialogoActivarGPS() {//muestra un diálogo para activar el GPS si está desactivado
    new AlertDialog.Builder(this)
            .setTitle(getString(R.string.gps_disabled_title))
            .setMessage(getString(R.string.gps_disabled_message))
            .setPositiveButton(getString(R.string.activate), (d, w) -> {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

   private void recargarMapa() {//recarga el mapa obteniendo la ubicación actual del usuario y buscando cines cercanos 

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mostrarDialogoActivarGPS();
            return;
        }

        recargarDesdeGPS();
    }

    private void recargarDesdeGPS() {//obtiene la ubicación actual del usuario usando el GPS y actualiza el mapa con esa ubicación, además de buscar cines cercanos

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(1000);
        request.setNumUpdates(1); 

        LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();

                lastUserLocation = new LatLng(
                        location.getLatitude(),
                        location.getLongitude()
                );

                mMap.clear();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 14));

                buscarCinesCercanos(lastUserLocation);

                fusedLocationClient.removeLocationUpdates(this); 
            }
        };

        fusedLocationClient.requestLocationUpdates(request, callback, null);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {//maneja el resultado de la solicitud de permisos de ubicación
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == 1) {
                if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    LocationManager locationManager =
                            (LocationManager) getSystemService(LOCATION_SERVICE);

                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        mostrarDialogoActivarGPS();
                    } else {
                        recargarDesdeGPS();
                    }

                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                }
            }
        }
}