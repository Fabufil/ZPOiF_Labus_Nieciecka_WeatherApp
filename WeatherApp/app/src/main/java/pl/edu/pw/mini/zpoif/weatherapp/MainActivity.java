package pl.edu.pw.mini.zpoif.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText etLat;
    private EditText etLon;
    private Button btnOpenWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        etLat = findViewById(R.id.et_lat);
        etLon = findViewById(R.id.et_lon);
        btnOpenWeather = findViewById(R.id.btn_open_weather);

        btnOpenWeather.setOnClickListener(v -> {
            openWeatherActivity();
        });
    }

    private void openWeatherActivity() {
        String latStr = etLat.getText().toString();
        String lonStr = etLon.getText().toString();

        if (latStr.isEmpty() || lonStr.isEmpty()) {
            Toast.makeText(this, "Wpisz obie współrzędne!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);

            Intent intent = new Intent(MainActivity.this, ShowWeatherActivity.class);

            // Przekazujemy koordynaty do nowej activity
            intent.putExtra("LATITUDE", lat);
            intent.putExtra("LONGITUDE", lon);
            startActivity(intent);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Podano nieprawidłowe liczby!", Toast.LENGTH_SHORT).show();
        }
    }
}