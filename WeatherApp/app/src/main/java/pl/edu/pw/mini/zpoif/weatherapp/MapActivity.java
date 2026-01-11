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

    // Zmienne do przechowywania wyboru
    private double selectedLat = 0;
    private double selectedLon = 0;
    private boolean isSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Konfiguracja OSM
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        // 2. Inicjalizacja widoków
        map = findViewById(R.id.map);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        tvSelectedInfo = findViewById(R.id.tvSelectedInfo);

        // 3. Konfiguracja Mapy
        setupMap();

        // 4. Obsługa przycisku "Dalej"
        btnConfirmLocation.setOnClickListener(v -> {
            if (isSelected) {
                // Przechodzimy do WeatherActivity z wybranymi współrzędnymi
                //Intent intent = new Intent(MapActivity.this, ShowWeatherActivity.class);
                //intent.putExtra("LAT", selectedLat);
                //intent.putExtra("LON", selectedLon);


                Intent intent = new Intent(MapActivity.this, ShowWeatherActivity.class);

                // Przekazujemy koordynaty do nowej activity
                intent.putExtra("LATITUDE", selectedLat);
                intent.putExtra("LONGITUDE", selectedLon);
                startActivity(intent);

                // Ponieważ nie mamy nazwy miasta (bo klikamy w mapę), przekazujemy ładny tekst ze współrzędnymi
                String coordsName = String.format(Locale.US, "Lat: %.2f, Lon: %.2f", selectedLat, selectedLon);
                intent.putExtra("CITY", coordsName);

                startActivity(intent);
            } else {
                Toast.makeText(this, "Najpierw wybierz miejsce na mapie!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMap() {
        map.setMultiTouchControls(true);
        map.getController().setZoom(6.0);
        map.getController().setCenter(new GeoPoint(52.0, 19.0)); // Centrum Polski na start

        // --- OBSŁUGA KLIKNIĘCIA PALCEM ---
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                zaktualizujWybor(p.getLatitude(), p.getLongitude());
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                // Opcjonalnie: też można obsługiwać długie przytrzymanie
                zaktualizujWybor(p.getLatitude(), p.getLongitude());
                return true;
            }
        };

        // Dodajemy nakładkę (Overlay) która wyłapuje dotyk
        MapEventsOverlay overlayEvents = new MapEventsOverlay(mReceive);
        map.getOverlays().add(overlayEvents);
    }

    private void zaktualizujWybor(double lat, double lon) {
        this.selectedLat = lat;
        this.selectedLon = lon;
        this.isSelected = true;

        // 1. Wyczyść stare znaczniki (ale zostaw Overlay od dotyku!)
        // Najprościej wyczyścić wszystko i dodać Overlay od nowa
        map.getOverlays().clear();
        setupMap(); // Przywraca obsługę kliknięć

        // 2. Dodaj nową pinezkę
        GeoPoint point = new GeoPoint(lat, lon);
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle("Wybrane miejsce");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);

        // 3. Przesuń/Odśwież mapę
        map.getController().animateTo(point); // Ładna animacja przesunięcia
        map.invalidate();

        // 4. Aktualizuj UI
        String info = String.format(Locale.US, "Wybrano: %.4f, %.4f", lat, lon);
        tvSelectedInfo.setText(info);
        btnConfirmLocation.setEnabled(true); // Aktywujemy przycisk
        btnConfirmLocation.setText("ZATWIERDŹ: " + info);
    }

    // Ważne dla cyklu życia mapy
    @Override
    public void onResume() { super.onResume(); map.onResume(); }
    @Override
    public void onPause() { super.onPause(); map.onPause(); }
}