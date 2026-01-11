package pl.edu.pw.mini.zpoif.weatherapp;

import java.util.ArrayList;
import java.util.List;

public class WarningGenerator {

    /**
     * Metoda analizuje dane i zwraca listę ostrzeżeń.
     * Analizujemy najbliższe 24 godziny.
     */
    public static List<String> generujOstrzezenia(List<Double> temperatures, List<Double> rain, List<Double> windSpeed) {
        List<String> ostrzezenia = new ArrayList<>();
        boolean isFreezing = false;
        boolean isSlippery = false;
        boolean isWindy = false;
        boolean isHot = false;

        // Sprawdzamy 24h
        int count = Math.min(temperatures.size(), 24);

        for (int i = 0; i < count; i++) {
            double t = temperatures.get(i);

            // Pobieramy deszcz i wiatr
            double r = (rain != null && i < rain.size()) ? rain.get(i) : 0.0;
            double w = (windSpeed != null && i < windSpeed.size()) ? windSpeed.get(i) : 0.0;

            // 1. upał
            if (t > 30.0) isHot = true;

            // 2. mróz
            if (t < -10.0) isFreezing = true;

            // 3. ślisko
            if (t <= 2.0 && t >= -3.0 && r > 0.0) {
                isSlippery = true;
            }

            // 4. silny wiat
            if (w > 25.0) isWindy = true;
        }

        if (isSlippery) ostrzezenia.add("UWAGA: Może być ślisko! (Gołoledź)");
        if (isWindy) ostrzezenia.add("UWAGA: Silny wiatr!");
        if (isFreezing) ostrzezenia.add("UWAGA: Bardzo niskie temperatury!");
        if (isHot) ostrzezenia.add("UWAGA: Bardzo wysokie temperatury!");

        return ostrzezenia;
    }
}
