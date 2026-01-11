package pl.edu.pw.mini.zpoif.weatherapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShowWeatherActivity extends AppCompatActivity implements OnWeatherSettingsListener {

    private TextView tvCurrentWeather;
    private LineChart chart;

    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_weather);

        tvCurrentWeather = findViewById(R.id.tv_current_weather);
        chart = findViewById(R.id.weather_chart);

        // Odbieramy dane z main
        latitude = getIntent().getDoubleExtra("LATITUDE", 52.23);
        longitude = getIntent().getDoubleExtra("LONGITUDE", 21.01);

        tvCurrentWeather.setText(String.format("Lokalizacja: %.4f, %.4f\nWybierz parametry powyżej, aby zobaczyć wykres.", latitude, longitude));

        configureChartAppearance();
    }

    // gdy użytkownik aktualizuje
    @Override
    public void onSettingsChanged(int days, Map<String, Boolean> options) {
        fetchAndDisplayData(days, options);
    }

    private void fetchAndDisplayData(int days, Map<String, Boolean> options) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        tvCurrentWeather.setText("Pobieranie danych...");

        executor.execute(() -> {
            try {
                String coordsQuery = "latitude=" + latitude + "&longitude=" + longitude;

                // pobieramy pogode na danych wspolrzednych
                String json = WeatherGetter.getWeather(coordsQuery, days);
                List<WeatherDataPoint> data = WeatherGetter.parseFullWeather(json);

                // Powrót do wątku głównego
                handler.post(() -> {
                    if (!data.isEmpty()) {
                        updateCurrentWeatherText(data.get(0));
                        updateChart(data, options);
                    } else {
                        tvCurrentWeather.setText("Brak danych.");
                        chart.clear();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(this, "Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void updateCurrentWeatherText(WeatherDataPoint current) {
        double visibilityKm = current.visibility / 1000.0;

        String info = String.format("TERAZ:\n Temp: %.1f°C\n Wiatr: %.1f km/h\n Widoczność: %.1f km\n Ciśnienie: %.0f hPa",
                current.temperature,
                current.windSpeed,
                visibilityKm,
                current.surfacePressure);
        tvCurrentWeather.setText(info);
    }

    private void updateChart(List<WeatherDataPoint> data, Map<String, Boolean> options) {
        LineData lineData = new LineData();

        if (options.getOrDefault("temp", false))
            lineData.addDataSet(createDataSet(data, "temp", "Temp (°C)", Color.RED));

        if (options.getOrDefault("humidity", false))
            lineData.addDataSet(createDataSet(data, "humidity", "Wilgotność (%)", Color.BLUE));

        if (options.getOrDefault("apparent", false))
            lineData.addDataSet(createDataSet(data, "apparent", "Odczuwalna (°C)", Color.MAGENTA));

        if (options.getOrDefault("precip_prob", false))
            lineData.addDataSet(createDataSet(data, "precip_prob", "Szansa opadów (%)", Color.CYAN));

        if (options.getOrDefault("precip", false))
            lineData.addDataSet(createDataSet(data, "precip", "Opady (mm)", Color.DKGRAY));

        if (options.getOrDefault("rain", false))
            lineData.addDataSet(createDataSet(data, "rain", "Deszcz (mm)", Color.BLUE));

        if (options.getOrDefault("snow", false))
            lineData.addDataSet(createDataSet(data, "snow", "Śnieg (cm)", Color.LTGRAY));

        if (options.getOrDefault("pressure", false))
            lineData.addDataSet(createDataSet(data, "pressure", "Ciśnienie (hPa)", Color.GREEN));

        if (options.getOrDefault("cloud", false))
            lineData.addDataSet(createDataSet(data, "cloud", "Chmury (%)", Color.GRAY));

        // ZMIANA: Etykieta teraz mówi (km)
        if (options.getOrDefault("visibility", false))
            lineData.addDataSet(createDataSet(data, "visibility", "Widoczność (km)", Color.YELLOW));

        if (options.getOrDefault("wind", false))
            lineData.addDataSet(createDataSet(data, "wind", "Wiatr (km/h)", Color.parseColor("#FF8800")));

        chart.setData(lineData);
        chart.animateX(800);
        chart.invalidate();
    }

    private LineDataSet createDataSet(List<WeatherDataPoint> data, String type, String label, int color) {
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            WeatherDataPoint p = data.get(i);
            float value = 0;

            switch (type) {
                case "temp": value = (float) p.temperature; break;
                case "humidity": value = (float) p.humidity; break;
                case "apparent": value = (float) p.apparentTemperature; break;
                case "precip_prob": value = (float) p.precipitationProbability; break;
                case "precip": value = (float) p.precipitation; break;
                case "rain": value = (float) p.rain; break;
                case "snow": value = (float) p.snowfall; break;
                case "pressure": value = (float) p.surfacePressure; break;
                case "cloud": value = (float) p.cloudCover; break;
                case "visibility": value = (float) p.visibility / 1000f; break;
                case "wind": value = (float) p.windSpeed; break;
            }
            entries.add(new Entry(i, value));
        }

        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(color);
        set.setCircleColor(color);
        set.setLineWidth(2f);
        set.setCircleRadius(1.5f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);


        if (type.equals("pressure")) {
            set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        } else {
            set.setAxisDependency(YAxis.AxisDependency.LEFT);
        }

        return set;
    }

    private void configureChartAppearance() {
        chart.setNoDataText("Wybierz parametry i kliknij Aktualizuj");
        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(true);
        rightAxis.setDrawGridLines(false);
        rightAxis.setStartAtZero(false);
    }
}