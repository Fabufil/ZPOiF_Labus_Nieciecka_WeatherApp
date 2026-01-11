package pl.edu.pw.mini.zpoif.weatherapp;

import java.util.Map;

public interface OnWeatherSettingsListener {

    void onSettingsChanged(int days, Map<String, Boolean> options);
}