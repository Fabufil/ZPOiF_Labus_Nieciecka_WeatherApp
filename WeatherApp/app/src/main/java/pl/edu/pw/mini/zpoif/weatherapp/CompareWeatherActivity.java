package pl.edu.pw.mini.zpoif.weatherapp;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompareWeatherActivity extends AppCompatActivity {

    private EditText etCity1, etCity2;
    private Button btnCompare, btnSelectParams;

    // Pola tekstowe podstawowe
    private TextView tvName1, tvTemp1, tvWind1, tvRain1;
    private TextView tvName2, tvTemp2, tvWind2, tvRain2;
    // USUNIĘTO: private TextView tvSummary;

    private View resultsContainer;
    private TableLayout comparisonTable;

    // Lista dostępnych parametrów do porównania
    private final String[] parameterNames = {
            "Temperatura", "Wilgotność", "Odczuwalna",
            "Wiatr", "Szansa opadów", "Opady (mm)",
            "Ciśnienie", "Widoczność", "Chmury"
    };
    // Domyślnie wybrane
    private boolean[] selectedParameters = {true, true, false, true, true, false, false, false, false};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_weather);

        etCity1 = findViewById(R.id.etCity1);
        etCity2 = findViewById(R.id.etCity2);
        btnCompare = findViewById(R.id.btnCompare);
        btnSelectParams = findViewById(R.id.btnSelectParams);

        resultsContainer = findViewById(R.id.resultsContainer);
        // USUNIĘTO: tvSummary = findViewById(R.id.tvSummary);
        comparisonTable = findViewById(R.id.comparisonTable);

        tvName1 = findViewById(R.id.tvName1);
        tvTemp1 = findViewById(R.id.tvTemp1);
        tvWind1 = findViewById(R.id.tvWind1);
        tvRain1 = findViewById(R.id.tvRain1);

        tvName2 = findViewById(R.id.tvName2);
        tvTemp2 = findViewById(R.id.tvTemp2);
        tvWind2 = findViewById(R.id.tvWind2);
        tvRain2 = findViewById(R.id.tvRain2);

        btnSelectParams.setOnClickListener(v -> showParameterSelectionDialog());

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

    private void showParameterSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Wybierz parametry do tabeli");
        builder.setMultiChoiceItems(parameterNames, selectedParameters, (dialog, which, isChecked) -> {
            selectedParameters[which] = isChecked;
        });
        builder.setPositiveButton("OK", (dialog, which) -> {});
        builder.create().show();
    }

    private void performComparison(String city1, String city2) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        btnCompare.setEnabled(false);
        btnCompare.setText("Pobieranie...");
        resultsContainer.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                String coords1 = WeatherGetter.getCoordinates(city1);
                String coords2 = WeatherGetter.getCoordinates(city2);

                String json1 = WeatherGetter.getWeather(coords1, 1);
                String json2 = WeatherGetter.getWeather(coords2, 1);

                List<WeatherDataPoint> data1 = WeatherGetter.parseFullWeather(json1);
                List<WeatherDataPoint> data2 = WeatherGetter.parseFullWeather(json2);

                if (data1.isEmpty() || data2.isEmpty()) throw new Exception("Brak danych.");

                int hour = Math.min(LocalTime.now().getHour(), data1.size() - 1);

                WeatherDataPoint p1 = data1.get(hour);
                WeatherDataPoint p2 = data2.get(hour);

                handler.post(() -> {
                    updateUI(city1, p1, city2, p2);
                    generateTable(city1, p1, city2, p2);
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

    private void generateTable(String name1, WeatherDataPoint p1, String name2, WeatherDataPoint p2) {
        comparisonTable.removeAllViews();

        TableRow header = new TableRow(this);
        header.setBackgroundColor(Color.LTGRAY);
        header.addView(createTextView("Parametr", true));
        header.addView(createTextView(name1.toUpperCase(), true));
        header.addView(createTextView(name2.toUpperCase(), true));
        comparisonTable.addView(header);

        if (selectedParameters[0]) addRow("Temperatura", p1.temperature + " °C", p2.temperature + " °C");
        if (selectedParameters[1]) addRow("Wilgotność", p1.humidity + "%", p2.humidity + "%");
        if (selectedParameters[2]) addRow("Odczuwalna", p1.apparentTemperature + " °C", p2.apparentTemperature + " °C");
        if (selectedParameters[3]) addRow("Wiatr", p1.windSpeed + " km/h", p2.windSpeed + " km/h");
        if (selectedParameters[4]) addRow("Szansa opadów", p1.precipitationProbability + "%", p2.precipitationProbability + "%");
        if (selectedParameters[5]) addRow("Opady", p1.precipitation + " mm", p2.precipitation + " mm");
        if (selectedParameters[6]) addRow("Ciśnienie", p1.surfacePressure + " hPa", p2.surfacePressure + " hPa");
        if (selectedParameters[7]) addRow("Widoczność", (p1.visibility/1000) + " km", (p2.visibility/1000) + " km");
        if (selectedParameters[8]) addRow("Chmury", p1.cloudCover + "%", p2.cloudCover + "%");
    }

    private void addRow(String param, String v1, String v2) {
        TableRow row = new TableRow(this);
        row.setPadding(0, 8, 0, 8);
        row.addView(createTextView(param, false));
        row.addView(createTextView(v1, false));
        row.addView(createTextView(v2, false));

        View line = new View(this);
        line.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 2));
        line.setBackgroundColor(Color.parseColor("#E0E0E0"));

        comparisonTable.addView(row);
        comparisonTable.addView(line);
    }

    private TextView createTextView(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 16, 16, 16);
        tv.setGravity(Gravity.CENTER);
        if (isHeader) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(14);
        } else {
            tv.setTextColor(Color.DKGRAY);
            tv.setTextSize(14);
        }
        return tv;
    }

    private void updateUI(String name1, WeatherDataPoint p1, String name2, WeatherDataPoint p2) {
        resultsContainer.setVisibility(View.VISIBLE);

        tvName1.setText(name1.toUpperCase());
        tvTemp1.setText(String.format("%.1f°C", p1.temperature));
        tvWind1.setText(String.format("%.1f km/h", p1.windSpeed));
        tvRain1.setText(String.format("%d%%", p1.precipitationProbability));

        tvName2.setText(name2.toUpperCase());
        tvTemp2.setText(String.format("%.1f°C", p2.temperature));
        tvWind2.setText(String.format("%.1f km/h", p2.windSpeed));
        tvRain2.setText(String.format("%d%%", p2.precipitationProbability));


    }
}