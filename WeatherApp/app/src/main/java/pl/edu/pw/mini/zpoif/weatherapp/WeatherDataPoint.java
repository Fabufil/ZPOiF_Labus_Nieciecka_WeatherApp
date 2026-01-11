package pl.edu.pw.mini.zpoif.weatherapp;

public class WeatherDataPoint {
    public String time;
    public double temperature;
    public int humidity;
    public double apparentTemperature;
    public int precipitationProbability;
    public double precipitation;
    public double rain;
    public double snowfall;
    public double showers;
    public double snowDepth;
    public double surfacePressure;
    public int cloudCover;
    public double visibility;
    public double windSpeed;

    public WeatherDataPoint(String time, double temperature, int humidity, double apparentTemperature,
                            int precipitationProbability, double precipitation, double rain,
                            double snowfall, double showers, double snowDepth,
                            double surfacePressure, int cloudCover, double visibility, double windSpeed) {
        this.time = time;
        this.temperature = temperature;
        this.humidity = humidity;
        this.apparentTemperature = apparentTemperature;
        this.precipitationProbability = precipitationProbability;
        this.precipitation = precipitation;
        this.rain = rain;
        this.snowfall = snowfall;
        this.showers = showers;
        this.snowDepth = snowDepth;
        this.surfacePressure = surfacePressure;
        this.cloudCover = cloudCover;
        this.visibility = visibility;
        this.windSpeed = windSpeed;
    }
}
