package pl.edu.pw.mini.zpoif.weatherapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompareWeatherActivity extends AppCompatActivity {

    private EditText etCity1, etCity2;
    private Button btnCompare;

    // Pola tekstowe dla miasta 1
    private TextView tvName1, tvTemp1, tvWind1, tvRain1;
    // Pola tekstowe dla miasta 2
    private TextView tvName2, tvTemp2, tvWind2, tvRain2;

    private TextView tvSummary;
    private View resultsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_weather);

        etCity1 = findViewById(R.id.etCity1);
        etCity2 = findViewById(R.id.etCity2);
        btnCompare = findViewById(R.id.btnCompare);
        resultsContainer = findViewById(R.id.resultsContainer);
        tvSummary = findViewById(R.id.tvSummary);

        // Inicjalizacja widoków wyników
        tvName1 = findViewById(R.id.tvName1);
        tvTemp1 = findViewById(R.id.tvTemp1);
        tvWind1 = findViewById(R.id.tvWind1);
        tvRain1 = findViewById(R.id.tvRain1);

        tvName2 = findViewById(R.id.tvName2);
        tvTemp2 = findViewById(R.id.tvTemp2);
        tvWind2 = findViewById(R.id.tvWind2);
        tvRain2 = findViewById(R.id.tvRain2);

        btnCompare.setOnClickListener(v -> {
            String city1 = etCity1.getText().toString().trim();
            String city2 = etCity2.getText().toString().trim();

            if (city1.isEmpty() || city2.isEmpty()) {
                Toast.makeText(this, "Podaj oba miasta!", Toast.LENGTH_SHORT).show();
                return;
            }

            performComparison(city1, city2);
        });
    }

    private void performComparison(String city1, String city2) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        btnCompare.setEnabled(false);
        btnCompare.setText("Pobieranie...");
        resultsContainer.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                // 1. Pobierz współrzędne
                String coords1 = WeatherGetter.getCoordinates(city1);
                String coords2 = WeatherGetter.getCoordinates(city2);

                // 2. Pobierz pogodę (tylko 1 dzień, bo to szybkie porównanie)
                String json1 = WeatherGetter.getWeather(coords1, 1);
                String json2 = WeatherGetter.getWeather(coords2, 1);

                // 3. Parsuj
                List<WeatherDataPoint> data1 = WeatherGetter.parseFullWeather(json1);
                List<WeatherDataPoint> data2 = WeatherGetter.parseFullWeather(json2);

                if (data1.isEmpty() || data2.isEmpty()) {
                    throw new Exception("Brak danych pogodowych dla jednego z miast.");
                }

                // Pobieramy aktualną godzinę (lub najbliższą dostępną)
                WeatherDataPoint p1 = data1.get(0);
                WeatherDataPoint p2 = data2.get(0);

                // 4. Aktualizacja UI
                handler.post(() -> {
                    updateUI(city1, p1, city2, p2);
                    btnCompare.setEnabled(true);
                    btnCompare.setText("PORÓWNAJ");
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    Toast.makeText(this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnCompare.setEnabled(true);
                    btnCompare.setText("PORÓWNAJ");
                });
            }
        });
    }

    private void updateUI(String name1, WeatherDataPoint p1, String name2, WeatherDataPoint p2) {
        resultsContainer.setVisibility(View.VISIBLE);

        // Ustawienie danych Miasta 1
        tvName1.setText(name1.toUpperCase());
        tvTemp1.setText(String.format("%.1f°C", p1.temperature));
        tvWind1.setText(String.format("%.1f km/h", p1.windSpeed));
        tvRain1.setText(String.format("%d%%", p1.precipitationProbability));

        // Ustawienie danych Miasta 2
        tvName2.setText(name2.toUpperCase());
        tvTemp2.setText(String.format("%.1f°C", p2.temperature));
        tvWind2.setText(String.format("%.1f km/h", p2.windSpeed));
        tvRain2.setText(String.format("%d%%", p2.precipitationProbability));

        // Generowanie podsumowania
        StringBuilder sb = new StringBuilder();
        if (p1.temperature > p2.temperature) {
            sb.append("🌡️ W ").append(name1).append(" jest cieplej o ").append(String.format("%.1f", p1.temperature - p2.temperature)).append("°C.\n");
        } else {
            sb.append("🌡️ W ").append(name2).append(" jest cieplej o ").append(String.format("%.1f", p2.temperature - p1.temperature)).append("°C.\n");
        }

        if (p1.precipitationProbability < p2.precipitationProbability) {
            sb.append("☀️ Lepsza pogoda (mniej deszczu) w: ").append(name1);
        } else if (p1.precipitationProbability > p2.precipitationProbability) {
            sb.append("☀️ Lepsza pogoda (mniej deszczu) w: ").append(name2);
        } else {
            sb.append("🌧️ Szansa na deszcz jest taka sama.");
        }

        tvSummary.setText(sb.toString());
    }
}