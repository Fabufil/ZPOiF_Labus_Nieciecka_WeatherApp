package pl.edu.pw.mini.zpoif.weatherapp;

import android.content.Context;
import android.widget.Toast;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class WeatherGetter {

    // Metoda pobierająca współrzędne na podstawie nazwy miasta
    public static String getCoordinates(String name) throws Exception {
        String encodedName = URLEncoder.encode(name, "UTF-8");
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedName + "&count=1&language=en&format=json";

        String json = makeHttpRequest(urlString);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        if (!obj.has("results")) throw new Exception("Nie znaleziono miasta!");

        JsonObject result = obj.getAsJsonArray("results").get(0).getAsJsonObject();
        double lat = result.get("latitude").getAsDouble();
        double lon = result.get("longitude").getAsDouble();

        return "latitude=" + lat + "&longitude=" + lon;
    }

    // Metoda pobierająca dane pogodowe na podstawie współrzędnych
    public static String getWeather(String coordsQuery) throws Exception {
        String urlString = "https://api.open-meteo.com/v1/forecast?" + coordsQuery + "&hourly=temperature_2m&forecast_days=1";
        return makeHttpRequest(urlString);
    }

    // Pomocnicza metoda do zapytań HTTP
    private static String makeHttpRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    // Metoda parsująca JSON i wyświetlająca Toast
    public static void parseWeather(Context context, String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            double temp = obj.getAsJsonObject("hourly")
                    .getAsJsonArray("temperature_2m")
                    .get(0).getAsDouble();

            String message = "Aktualna temperatura: " + temp + "°C";
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Błąd parsowania danych", Toast.LENGTH_SHORT).show();
        }
    }
}