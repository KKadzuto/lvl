package solo.lev.lvl.services;

import android.os.AsyncTask;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

public class WeatherService {
    private static final String API_KEY = "1937901ea52a9fc7274dab553f37f910";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/forecast";
    private static final String CITY = "Aktobe";
    private static final String COUNTRY = "KZ";
    public interface WeatherCallback {
        void onSuccess(String temperature, String description, String humidity);
        void onError(String error);
    }
    public static void getWeather(String date, WeatherCallback callback) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                HttpURLConnection conn = null;
                BufferedReader reader = null;
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    Date targetDate = dateFormat.parse(date);
                    targetDate.setHours(12);
                    targetDate.setMinutes(0);
                    targetDate.setSeconds(0);
                    long timestamp = targetDate.getTime() / 1000;
                    String urlString = String.format(
                        "%s?q=%s,%s&appid=%s&units=metric&lang=ru",
                        BASE_URL, CITY, COUNTRY, API_KEY
                    );
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    InputStream inputStream = conn.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString() + "||" + dateFormat.format(targetDate);
                } catch (Exception e) {
                    return "error:Не удалось получить прогноз погоды";
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                        }
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("error:")) {
                    callback.onError("Не удалось получить прогноз погоды");
                } else {
                    try {
                        String[] parts = result.split("\\|\\|");
                        JSONObject json = new JSONObject(parts[0]);
                        String targetDate = parts[1];
                        
                        JSONArray list = json.getJSONArray("list");
                        JSONObject targetForecast = null;
                        SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                        
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject forecast = list.getJSONObject(i);
                            String dt = forecast.getString("dt_txt").split(" ")[0];
                            Date forecastDate = dtFormat.parse(dt);
                            String forecastDateStr = dateFormat.format(forecastDate);
                            if (forecastDateStr.equals(targetDate)) {
                                targetForecast = forecast;
                                break;
                            }
                        }
                        if (targetForecast == null) {
                            callback.onError("Не удалось получить прогноз погоды");
                            return;
                        }
                        JSONObject main = targetForecast.getJSONObject("main");
                        double temp = main.getDouble("temp");
                        int humidity = main.getInt("humidity");
                        String description = targetForecast.getJSONArray("weather")
                            .getJSONObject(0)
                            .getString("description");
                        
                        String temperature = String.format(Locale.getDefault(), "%.1f°C", temp);
                        callback.onSuccess(
                            temperature,
                            description,
                            humidity + "%"
                        );
                    } catch (Exception e) {
                        callback.onError("Не удалось получить прогноз погоды");
                    }
                }
            }
        }.execute(date);
    }
} 