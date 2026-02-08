package pl.edu.pw.mini.zpoif.weatherapp;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class WeatherGetter {

    // metoda pobierająca współrzędne na podstawie nazwy miasta
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

    // metoda pobierająca dane pogodowe na podstawie współrzędnych
    public static String getWeather(String coordsQuery, int days) throws Exception {
        // Zabezpieczenie: Open-Meteo obsługuje od 1 do 16 dni
        if (days < 1) days = 1;
        if (days > 16) days = 16;


        String urlString = "https://api.open-meteo.com/v1/forecast?" + coordsQuery +
                "&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation_probability,precipitation,rain,snowfall,showers,snow_depth,surface_pressure,cloud_cover,visibility,wind_speed_10m" +
                "&past_days=0" +
                "&forecast_days=" + days;

        return makeHttpRequest(urlString);
    }

    // metoda do requestów
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
    // metoda wygląda na taką, co by wypadało usunąć
    // metoda parsująca JSON i wyświetlająca powiadomienia typu grilowany chleb
    public static void parseWeather(Context context, String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonObject hourly = obj.getAsJsonObject("hourly");

            JsonArray temperatures = hourly.getAsJsonArray("temperature_2m");
            JsonArray rainChance = hourly.getAsJsonArray("precipitation_probability");
            JsonArray windSpeed = hourly.getAsJsonArray("wind_speed_10m");

            // aktualna godzina
            int currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);

            // pogoda dla tejże godziny
            double temp = temperatures.get(currentHour).getAsDouble();
            int prob = rainChance.get(currentHour).getAsInt();
            double wind = windSpeed.get(currentHour).getAsDouble();

            // grillowany chleb text
            String result = String.format(
                    "Aktualna pogoda (%02d:00):\n" +
                            "🌡 Temperatura: %.1f°C\n" +
                            "🌧 Szansa opadów: %d%%\n" +
                            "💨 Wiatr: %.1f km/h",
                    currentHour, temp, prob, wind
            );

            Toast.makeText(context, result, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Błąd danych: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static List<WeatherDataPoint> parseFullWeather(String json) {
        List<WeatherDataPoint> weatherList = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject hourly = root.getAsJsonObject("hourly");


            JsonArray times = hourly.getAsJsonArray("time");
            JsonArray temps = hourly.getAsJsonArray("temperature_2m");
            JsonArray humidities = hourly.getAsJsonArray("relative_humidity_2m");
            JsonArray apparentTemps = hourly.getAsJsonArray("apparent_temperature");
            JsonArray precipProb = hourly.getAsJsonArray("precipitation_probability");
            JsonArray precip = hourly.getAsJsonArray("precipitation");
            JsonArray rain = hourly.getAsJsonArray("rain");
            JsonArray snowfall = hourly.getAsJsonArray("snowfall");
            JsonArray showers = hourly.getAsJsonArray("showers");
            JsonArray snowDepth = hourly.getAsJsonArray("snow_depth");
            JsonArray pressures = hourly.getAsJsonArray("surface_pressure");
            JsonArray clouds = hourly.getAsJsonArray("cloud_cover");
            JsonArray visibility = hourly.getAsJsonArray("visibility");
            JsonArray winds = hourly.getAsJsonArray("wind_speed_10m");


            for (int i = 0; i < times.size(); i++) {
                WeatherDataPoint point = new WeatherDataPoint(
                        times.get(i).getAsString(),
                        temps.get(i).getAsDouble(),
                        humidities.get(i).getAsInt(),
                        apparentTemps.get(i).getAsDouble(),
                        precipProb.get(i).getAsInt(),
                        precip.get(i).getAsDouble(),
                        rain.get(i).getAsDouble(),
                        snowfall.get(i).getAsDouble(),
                        showers.get(i).getAsDouble(),
                        snowDepth.get(i).getAsDouble(),
                        pressures.get(i).getAsDouble(),
                        clouds.get(i).getAsInt(),
                        visibility.get(i).getAsDouble(),
                        winds.get(i).getAsDouble()
                );
                weatherList.add(point);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return weatherList;
    }
}