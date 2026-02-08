package pl.edu.pw.mini.zpoif.weatherapp;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

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

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Usunięto: implements OnWeatherSettingsListener (logika przeniesiona do tej klasy)
public class ShowWeatherActivity extends AppCompatActivity {

    private TextView tvCurrentWeather;
    private LineChart chart;
    private TextView tvWarnings;
    private Button btnSaveChart;

    // Nowe elementy UI do sterowania
    private EditText etDays;
    private Button btnSelectParams, btnUpdate;

    private double latitude;
    private double longitude;

    private List<WeatherDataPoint> currentDataList = new ArrayList<>();

    // Definicje parametrów (identyczne jak w CompareWeatherActivity)
    private final String[] parameterNames = {
            "Temperatura", "Wilgotność", "Odczuwalna",
            "Szansa opadów", "Opady (mm)", "Deszcz (mm)",
            "Śnieg (cm)", "Ciśnienie", "Chmury",
            "Widoczność", "Wiatr"
    };
    // Klucze mapowania do logiki rysowania
    private final String[] parameterKeys = {
            "temp", "humidity", "apparent",
            "precip_prob", "precip", "rain",
            "snow", "pressure", "cloud",
            "visibility", "wind"
    };
    // Domyślne zaznaczenie (Temperatura i Wilgotność)
    private boolean[] selectedParameters = {true, true, false, false, false, false, false, false, false, false, false};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Wymuszenie trybu jasnego
        setContentView(R.layout.activity_show_weather);

        tvCurrentWeather = findViewById(R.id.tv_current_weather);
        tvWarnings = findViewById(R.id.tv_warnings);
        chart = findViewById(R.id.weather_chart);
        btnSaveChart = findViewById(R.id.btn_save_chart);

        // Inicjalizacja nowych przycisków
        etDays = findViewById(R.id.et_days);
        btnSelectParams = findViewById(R.id.btn_select_params);
        btnUpdate = findViewById(R.id.btn_update);

        latitude = getIntent().getDoubleExtra("LATITUDE", 52.23);
        longitude = getIntent().getDoubleExtra("LONGITUDE", 21.01);

        tvCurrentWeather.setText(String.format("Lokalizacja: %.4f, %.4f\nWybierz parametry i kliknij Aktualizuj.", latitude, longitude));

        configureChartAppearance();

        // 1. Obsługa przycisku wyboru parametrów (Dialog)
        btnSelectParams.setOnClickListener(v -> showParameterSelectionDialog());

        // 2. Obsługa przycisku Aktualizuj
        btnUpdate.setOnClickListener(v -> {
            String daysStr = etDays.getText().toString();
            if(daysStr.isEmpty()) return;
            int days = Integer.parseInt(daysStr);
            if(days < 1) days = 1;
            if(days > 16) days = 16;

            fetchAndDisplayData(days);
        });

        // 3. Obsługa zapisu (Twoja metoda)
        btnSaveChart.setOnClickListener(v -> {
            if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
                saveChartToGallery();
            } else {
                Toast.makeText(this, "Najpierw pobierz dane, aby zapisać wykres!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showParameterSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Wybierz dane do wykresu");
        builder.setMultiChoiceItems(parameterNames, selectedParameters, (dialog, which, isChecked) -> {
            selectedParameters[which] = isChecked;
        });
        builder.setPositiveButton("Zatwierdź", (dialog, which) -> {});
        builder.create().show();
    }

    private void fetchAndDisplayData(int days) {
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
                        this.currentDataList = data;

                        int currentHourIndex = getCurrentHourIndex(data);
                        if (currentHourIndex < data.size()) {
                            updateCurrentWeatherText(data.get(currentHourIndex));
                        }

                        // Konwersja tablicy boolean[] na Mapę dla metody updateChart
                        Map<String, Boolean> optionsMap = new HashMap<>();
                        for(int i=0; i<selectedParameters.length; i++) {
                            optionsMap.put(parameterKeys[i], selectedParameters[i]);
                        }

                        updateChart(data, optionsMap);
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
        int hour = LocalTime.now().getHour();
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
        String timeStr = current.time;
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

        chart.getAxisLeft().removeAllLimitLines();
        chart.getAxisRight().removeAllLimitLines();

        if (options.getOrDefault("temp", false)) addDataSetWithAverage(lineData, data, "temp", "Temp (°C)", Color.RED, chart.getAxisLeft());
        if (options.getOrDefault("humidity", false)) addDataSetWithAverage(lineData, data, "humidity", "Wilgotność (%)", Color.BLUE, chart.getAxisLeft());
        if (options.getOrDefault("apparent", false)) addDataSetWithAverage(lineData, data, "apparent", "Odczuwalna (°C)", Color.MAGENTA, chart.getAxisLeft());
        if (options.getOrDefault("precip_prob", false)) addDataSetWithAverage(lineData, data, "precip_prob", "Szansa opadów (%)", Color.CYAN, chart.getAxisLeft());
        if (options.getOrDefault("precip", false)) addDataSetWithAverage(lineData, data, "precip", "Opady (mm)", Color.DKGRAY, chart.getAxisLeft());
        if (options.getOrDefault("rain", false)) addDataSetWithAverage(lineData, data, "rain", "Deszcz (mm)", Color.BLUE, chart.getAxisLeft());
        if (options.getOrDefault("snow", false)) addDataSetWithAverage(lineData, data, "snow", "Śnieg (cm)", Color.LTGRAY, chart.getAxisLeft());
        if (options.getOrDefault("pressure", false)) addDataSetWithAverage(lineData, data, "pressure", "Ciśnienie (hPa)", Color.GREEN, chart.getAxisRight());
        if (options.getOrDefault("cloud", false)) addDataSetWithAverage(lineData, data, "cloud", "Chmury (%)", Color.GRAY, chart.getAxisLeft());
        if (options.getOrDefault("visibility", false)) addDataSetWithAverage(lineData, data, "visibility", "Widoczność (km)", Color.YELLOW, chart.getAxisLeft());
        if (options.getOrDefault("wind", false)) addDataSetWithAverage(lineData, data, "wind", "Wiatr (km/h)", Color.parseColor("#FF8800"), chart.getAxisLeft());

        chart.setData(lineData);
        chart.animateX(800);
        chart.invalidate();
    }

    private void addDataSetWithAverage(LineData lineData, List<WeatherDataPoint> data, String type, String label, int color, YAxis axis) {
        LineDataSet set = createDataSet(data, type, label, color);
        lineData.addDataSet(set);

        float sum = 0;
        int count = 0;
        for (Entry e : set.getValues()) {
            sum += e.getY();
            count++;
        }

        if (count > 0) {
            float avg = sum / count;
            LimitLine ll = new LimitLine(avg, "Śr: " + String.format("%.1f", avg));
            ll.setLineColor(color);
            ll.setLineWidth(1f);
            ll.enableDashedLine(10f, 10f, 0f);
            ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            ll.setTextSize(10f);
            ll.setTextColor(color);
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

        if (type.equals("pressure")) set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        else set.setAxisDependency(YAxis.AxisDependency.LEFT);

        return set;
    }

    private void configureChartAppearance() {
        chart.setNoDataText("Wybierz parametry i kliknij Aktualizuj");
        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < currentDataList.size()) {
                    String rawTime = currentDataList.get(index).time;
                    try {
                        LocalDateTime ldt = LocalDateTime.parse(rawTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        return ldt.format(DateTimeFormatter.ofPattern("dd.MM HH:mm"));
                    } catch (Exception e) {
                        return rawTime;
                    }
                }
                return "";
            }
        });
        xAxis.setLabelRotationAngle(-45);

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
        Bitmap bitmap = chart.getChartBitmap();
        String filename = "WykresPogody_" + System.currentTimeMillis() + ".png";
        OutputStream fos;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues resolver = new ContentValues();
                resolver.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                resolver.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                resolver.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WeatherApp");
                resolver.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, resolver);
                fos = getContentResolver().openOutputStream(imageUri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                resolver.clear();
                resolver.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(imageUri, resolver, null, null);

                Toast.makeText(this, "Zapisano w folderze Obrazy/WeatherApp", Toast.LENGTH_LONG).show();
            } else {
                String savedImageURL = MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        bitmap,
                        filename,
                        "Wykres wygenerowany przez aplikację pogodową"
                );
                if (savedImageURL != null) Toast.makeText(this, "Zapisano wykres w Galerii", Toast.LENGTH_LONG).show();
                else Toast.makeText(this, "Błąd zapisu (sprawdź uprawnienia)", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Błąd zapisu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}