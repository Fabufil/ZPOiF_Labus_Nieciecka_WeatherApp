package pl.edu.pw.mini.zpoif.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etCityName;
    private Button btnSearchCity;
    private Button btnOpenMap;
    private Button btnOpenCompare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCityName = findViewById(R.id.etCityName);
        btnSearchCity = findViewById(R.id.btnSearchCity);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnOpenCompare = findViewById(R.id.btnOpenCompare);

        // szukanie po nazwie miasta
        btnSearchCity.setOnClickListener(v -> {
            String city = etCityName.getText().toString().trim();
            if (!city.isEmpty()) {
                searchCityAndShowWeather(city);
            } else {
                Toast.makeText(this, "Wpisz nazwę miasta!", Toast.LENGTH_SHORT).show();
            }
        });

        // Przejście do mapy
        btnOpenMap.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        });

        // przejście do porównania
        btnOpenCompare.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CompareWeatherActivity.class);
            startActivity(intent);
        });
    }

    private void searchCityAndShowWeather(String cityName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        btnSearchCity.setEnabled(false);
        btnSearchCity.setText("Szukam...");

        executor.execute(() -> {
            try {

                String coordsQuery = WeatherGetter.getCoordinates(cityName);


                double lat = 0;
                double lon = 0;

                String[] parts = coordsQuery.split("&");
                for (String part : parts) {
                    String[] kv = part.split("=");
                    if (kv[0].equals("latitude")) lat = Double.parseDouble(kv[1]);
                    if (kv[0].equals("longitude")) lon = Double.parseDouble(kv[1]);
                }

                double finalLat = lat;
                double finalLon = lon;

                handler.post(() -> {
                    btnSearchCity.setEnabled(true);
                    btnSearchCity.setText("SPRAWDŹ POGODĘ");

                    // ShowWeatherActivity
                    Intent intent = new Intent(MainActivity.this, ShowWeatherActivity.class);
                    intent.putExtra("LATITUDE", finalLat);
                    intent.putExtra("LONGITUDE", finalLon);
                    startActivity(intent);
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    btnSearchCity.setEnabled(true);
                    btnSearchCity.setText("SPRAWDŹ POGODĘ");
                    Toast.makeText(MainActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}