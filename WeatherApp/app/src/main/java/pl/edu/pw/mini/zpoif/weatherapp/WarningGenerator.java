package pl.edu.pw.mini.zpoif.weatherapp;

import java.util.ArrayList;
import java.util.List;

public class WarningGenerator {

    public static List<String> generateWarnings(List<WeatherDataPoint> data) {
        List<String> warnings = new ArrayList<>();

        boolean isFreezing = false;
        boolean isSlippery = false;
        boolean isWindy = false;
        boolean isHot = false;
        boolean isStormy = false;
        boolean isSnowing = false;
        boolean isRaining = false;
        boolean isLowPressure = false;
        boolean isFoggy = false;



        int count = Math.min(data.size(), 24);

        for (int i = 0; i < count; i++) {
            WeatherDataPoint point = data.get(i);

            // upał
            if (point.temperature > 30.0) isHot = true;

            // mróz
            if (point.temperature < -10.0) isFreezing = true;

            // ślisko
            if (point.temperature <= 2.0 && point.temperature >= -3.0 && point.rain > 0.0) {
                isSlippery = true;
            }

            // silny wiatr
            if (point.windSpeed > 25.0) isWindy = true;

            // burza
            if (point.rain > 2.0 || (point.visibility < 200 && point.visibility > 0)) {
                isStormy = true;
            }

            // śnieżyca
            if (point.snowfall > 0.5) {
                isSnowing = true;
            }

            //  deszcz
            if (point.precipitationProbability > 80) {
                isRaining = true;
            }

            //  niskie ciśnienie
            if (point.surfacePressure < 990.0) {
                isLowPressure = true;
            }

            // mgła
            if (point.visibility < 300.0 && point.windSpeed < 10.0) {
                isFoggy = true;
            }

        }

        if (isSlippery) warnings.add(" UWAGA: Może być ślisko!");
        if (isWindy) warnings.add("UWAGA: Silny wiatr!");
        if (isFreezing) warnings.add("UWAGA: Bardzo niskie temperatury!");
        if (isHot) warnings.add("UWAGA: Upał!");
        if (isStormy) warnings.add("UWAGA: Trudne warunki (ulewa/mgła)!");
        if (isSnowing) warnings.add("UWAGA: Opady śniegu!");
        if (isRaining) warnings.add("UWAGA: Opady deszczu - weź parasol!");
        if (isLowPressure) warnings.add("UWAGA: Niskie ciśnienie!");
        if (isFoggy) warnings.add("UWAGA: Mgla!");
        return warnings;
    }
}