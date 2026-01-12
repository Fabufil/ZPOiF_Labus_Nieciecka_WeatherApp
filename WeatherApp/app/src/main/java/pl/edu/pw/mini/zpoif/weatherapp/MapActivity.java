package pl.edu.pw.mini.zpoif.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private Button btnConfirmLocation;
    private TextView tvSelectedInfo;

    // przechowanie współrzędnych
    private double selectedLat = 0;
    private double selectedLon = 0;
    private boolean isSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // konfiguracja OSM
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        // inicjalizacja widoków
        map = findViewById(R.id.map);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        tvSelectedInfo = findViewById(R.id.tvSelectedInfo);

        // konfiguracja startowa mapy
        inicjalizacjaMapy();

        // przycisk Dalej
        btnConfirmLocation.setOnClickListener(v -> {
            if (isSelected) {
                // idziemy do ShowWeatherActivity z wybranymi współrzędnymi
                Intent intent = new Intent(MapActivity.this, ShowWeatherActivity.class);
                intent.putExtra("LATITUDE", selectedLat);
                intent.putExtra("LONGITUDE", selectedLon);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Najpierw wybierz miejsce na mapie!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void inicjalizacjaMapy() {
        // poczatkowe ustawienie mapy

        map.setMultiTouchControls(true);
        map.getController().setZoom(6.0);

        // ustawiamy środek na Polskę
        map.getController().setCenter(new GeoPoint(52.0, 19.0));

        // obsługa klikania
        dodajObslugeKlikniec();
    }

    private void dodajObslugeKlikniec() {
        // wykrywanie kliknięć na mapie
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                zaktualizujWybor(p.getLatitude(), p.getLongitude());
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                zaktualizujWybor(p.getLatitude(), p.getLongitude());
                return true;
            }
        };

        MapEventsOverlay overlayEvents = new MapEventsOverlay(mReceive);
        map.getOverlays().add(overlayEvents);
    }

    private void zaktualizujWybor(double lat, double lon) {
        this.selectedLat = lat;
        this.selectedLon = lon;
        this.isSelected = true;

        //czyścimy mapę ze starych pinezek
        map.getOverlays().clear();
        // obsługa kliknięć
        dodajObslugeKlikniec();

        // dodajemy nową pinezkę
        GeoPoint point = new GeoPoint(lat, lon);
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle("Wybrane miejsce");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);

        // przesuwamy mapę do nowego punktu
        map.getController().animateTo(point);
        map.invalidate();

        // aktualizujemy UI
        String info = String.format(Locale.US, "Wybrano: %.4f, %.4f", lat, lon);
        tvSelectedInfo.setText(info);
        btnConfirmLocation.setEnabled(true);
        btnConfirmLocation.setText("ZATWIERDŹ WYBÓR");
    }

    // cykl życia mapy
    @Override
    public void onResume() { super.onResume(); map.onResume(); }
    @Override
    public void onPause() { super.onPause(); map.onPause(); }
}