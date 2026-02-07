package pl.edu.pw.mini.zpoif.weatherapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.io.IOException;

public class ShowWeatherActivity extends AppCompatActivity implements OnWeatherSettingsListener {

    private TextView tvCurrentWeather;
    private LineChart chart;
    private TextView tvWarnings;
    private Button btnSaveChart;

    private double latitude;
    private double longitude;

    // Przechowujemy dane, aby formatter osi X miał do nich dostęp
    private List<WeatherDataPoint> currentDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_weather);

        tvCurrentWeather = findViewById(R.id.tv_current_weather);
        tvWarnings = findViewById(R.id.tv_warnings);
        chart = findViewById(R.id.weather_chart);
        btnSaveChart = findViewById(R.id.btn_save_chart);

        latitude = getIntent().getDoubleExtra("LATITUDE", 52.23);
        longitude = getIntent().getDoubleExtra("LONGITUDE", 21.01);

        tvCurrentWeather.setText(String.format("Lokalizacja: %.4f, %.4f\nWybierz parametry powyżej, aby zobaczyć wykres.", latitude, longitude));

        btnSaveChart.setOnClickListener(v -> {
            if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
                saveChartToGallery();
            } else {
                Toast.makeText(this, "Najpierw pobierz dane, aby zapisać wykres!", Toast.LENGTH_SHORT).show();
            }
        });

        configureChartAppearance();
    }

    @Override
    public void onSettingsChanged(int days, Map<String, Boolean> options) {
        fetchAndDisplayData(days, options);
    }

    private void fetchAndDisplayData(int days, Map<String, Boolean> options) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        tvCurrentWeather.setText("Pobieranie danych...");
        tvWarnings.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                String coordsQuery = "latitude=" + latitude + "&longitude=" + longitude;
                String json = WeatherGetter.getWeather(coordsQuery, days);
                List<WeatherDataPoint> data = WeatherGetter.parseFullWeather(json);

                handler.post(() -> {
                    if (!data.isEmpty()) {
                        this.currentDataList = data; // Zapisujemy do pola klasy dla osi X

                        // POPRAWKA: Pobieranie aktualnej godziny, a nie indeksu 0
                        int currentHourIndex = getCurrentHourIndex(data);
                        if (currentHourIndex < data.size()) {
                            updateCurrentWeatherText(data.get(currentHourIndex));
                        }

                        updateChart(data, options);
                        checkAndShowWarnings(data);
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

    private int getCurrentHourIndex(List<WeatherDataPoint> data) {
        // Dane z OpenMeteo (hourly) zazwyczaj zaczynają się od 00:00 dnia dzisiejszego (dzięki past_days=0)
        // Więc indeks to po prostu aktualna godzina (0-23).
        // Dla pewności można sprawdzić datę, ale w uproszczeniu:
        int hour = LocalTime.now().getHour();
        // Jeśli pobraliśmy więcej dni, to i tak interesuje nas "teraz", czyli godzina w pierwszej dobie.
        return Math.min(hour, data.size() - 1);
    }

    private void checkAndShowWarnings(List<WeatherDataPoint> data) {
        List<String> warnings = WarningGenerator.generateWarnings(data);
        if (!warnings.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String w : warnings) sb.append(w).append("\n");
            tvWarnings.setText(sb.toString().trim());
            tvWarnings.setVisibility(View.VISIBLE);
        } else {
            tvWarnings.setVisibility(View.GONE);
        }
    }

    private void updateCurrentWeatherText(WeatherDataPoint current) {
        double visibilityKm = current.visibility / 1000.0;
        // Parsujemy czas dla wyświetlenia
        String timeStr = current.time; // Format ISO: 2023-10-10T14:00
        String displayTime = timeStr.replace("T", " ");

        String info = String.format("TERAZ (%s):\n Temp: %.1f°C\n Wiatr: %.1f km/h\n Widoczność: %.1f km\n Ciśnienie: %.0f hPa",
                displayTime,
                current.temperature,
                current.windSpeed,
                visibilityKm,
                current.surfacePressure);
        tvCurrentWeather.setText(info);
    }

    private void updateChart(List<WeatherDataPoint> data, Map<String, Boolean> options) {
        LineData lineData = new LineData();

        // Czyścimy stare linie limitów (średnie)
        chart.getAxisLeft().removeAllLimitLines();
        chart.getAxisRight().removeAllLimitLines();

        // Dodajemy serie danych i od razu obliczamy średnią
        if (options.getOrDefault("temp", false))
            addDataSetWithAverage(lineData, data, "temp", "Temp (°C)", Color.RED, chart.getAxisLeft());

        if (options.getOrDefault("humidity", false))
            addDataSetWithAverage(lineData, data, "humidity", "Wilgotność (%)", Color.BLUE, chart.getAxisLeft());

        if (options.getOrDefault("apparent", false))
            addDataSetWithAverage(lineData, data, "apparent", "Odczuwalna (°C)", Color.MAGENTA, chart.getAxisLeft());

        if (options.getOrDefault("precip_prob", false))
            addDataSetWithAverage(lineData, data, "precip_prob", "Szansa opadów (%)", Color.CYAN, chart.getAxisLeft());

        if (options.getOrDefault("precip", false))
            addDataSetWithAverage(lineData, data, "precip", "Opady (mm)", Color.DKGRAY, chart.getAxisLeft());

        if (options.getOrDefault("rain", false))
            addDataSetWithAverage(lineData, data, "rain", "Deszcz (mm)", Color.BLUE, chart.getAxisLeft());

        if (options.getOrDefault("snow", false))
            addDataSetWithAverage(lineData, data, "snow", "Śnieg (cm)", Color.LTGRAY, chart.getAxisLeft());

        if (options.getOrDefault("pressure", false))
            addDataSetWithAverage(lineData, data, "pressure", "Ciśnienie (hPa)", Color.GREEN, chart.getAxisRight());

        if (options.getOrDefault("cloud", false))
            addDataSetWithAverage(lineData, data, "cloud", "Chmury (%)", Color.GRAY, chart.getAxisLeft());

        if (options.getOrDefault("visibility", false))
            addDataSetWithAverage(lineData, data, "visibility", "Widoczność (km)", Color.YELLOW, chart.getAxisLeft());

        if (options.getOrDefault("wind", false))
            addDataSetWithAverage(lineData, data, "wind", "Wiatr (km/h)", Color.parseColor("#FF8800"), chart.getAxisLeft());

        chart.setData(lineData);
        chart.animateX(800);
        chart.invalidate();
    }

    // Metoda pomocnicza do tworzenia Datasetu i dodawania linii średniej
    private void addDataSetWithAverage(LineData lineData, List<WeatherDataPoint> data, String type, String label, int color, YAxis axis) {
        LineDataSet set = createDataSet(data, type, label, color);
        lineData.addDataSet(set);

        // Obliczanie średniej
        float sum = 0;
        int count = 0;
        for (Entry e : set.getValues()) {
            sum += e.getY();
            count++;
        }

        if (count > 0) {
            float avg = sum / count;

            // POPRAWKA: Linia przerywana (średnia)
            LimitLine ll = new LimitLine(avg, "Śr: " + String.format("%.1f", avg));
            ll.setLineColor(color);
            ll.setLineWidth(1f);
            ll.enableDashedLine(10f, 10f, 0f);
            ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            ll.setTextSize(10f);
            ll.setTextColor(color); // Tekst w kolorze linii

            axis.addLimitLine(ll);
        }
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

        // POPRAWKA: Tło i kolory (dla trybu ciemnego)
        chart.setBackgroundColor(Color.WHITE); // Wymuszenie białego tła wykresu
        chart.setDrawGridBackground(false);

        // Oś X z datami
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);

        // Formatter do zamiany indeksu na datę/godzinę
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < currentDataList.size()) {
                    String rawTime = currentDataList.get(index).time;
                    // Format rawTime: 2023-10-27T14:00
                    try {
                        LocalDateTime ldt = LocalDateTime.parse(rawTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        // Zwracamy np. "27.10 14:00"
                        return ldt.format(DateTimeFormatter.ofPattern("dd.MM HH:mm"));
                    } catch (Exception e) {
                        return rawTime;
                    }
                }
                return "";
            }
        });
        xAxis.setLabelRotationAngle(-45); // Obrót napisów żeby się mieściły

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setTextColor(Color.BLACK);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(true);
        rightAxis.setDrawGridLines(false);
        rightAxis.setTextColor(Color.BLACK);

        chart.getLegend().setTextColor(Color.BLACK);
    }

    private void saveChartToGallery() {
        // pobranie wykresu
        Bitmap bitmap = chart.getChartBitmap();

        String filename = "WykresPogody_" + System.currentTimeMillis() + ".png";
        OutputStream fos;

        try {
            // dla nowych androiów
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues resolver = new ContentValues();
                resolver.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                resolver.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                resolver.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WeatherApp");
                resolver.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, resolver);
                fos = getContentResolver().openOutputStream(imageUri);

                // zapis
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                resolver.clear();
                resolver.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(imageUri, resolver, null, null);

                Toast.makeText(this, "Zapisano w folderze Obrazy/WeatherApp", Toast.LENGTH_LONG).show();

            } else {
                // dla starszych Androidów (< 10)
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                String savedImageURL = MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        bitmap,
                        filename,
                        "Wykres wygenerowany przez aplikację pogodową"
                );

                if (savedImageURL != null) {
                    Toast.makeText(this, "Zapisano wykres w Galerii", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Błąd zapisu (sprawdź uprawnienia)", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Błąd zapisu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}