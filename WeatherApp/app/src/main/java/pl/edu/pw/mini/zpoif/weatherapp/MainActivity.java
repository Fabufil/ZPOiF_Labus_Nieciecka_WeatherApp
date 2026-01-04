package pl.edu.pw.mini.zpoif.weatherapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

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
    }

    // Metoda wywoływana po naciśnięciu przycisku
    public void weatherOnClick(View view) {
        // Operacje sieciowe MUSZĄ być w osobnym wątku
        new Thread(() -> {
            try {
                // 1. Pobierz współrzędne dla Warszawy
                String coords = WeatherGetter.getCoordinates("Warsaw");

                // 2. Pobierz pogodę dla tych współrzędnych
                String jsonResponse = WeatherGetter.getWeather(coords);

                // 3. Wyświetl wynik na UI (wątek główny)
                runOnUiThread(() -> {
                    WeatherGetter.parseWeather(MainActivity.this, jsonResponse);
                });

            } catch (Exception e) {
                Log.e("WeatherApp", "Error fetching weather", e);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Błąd: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}