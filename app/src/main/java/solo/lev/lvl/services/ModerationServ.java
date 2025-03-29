package solo.lev.lvl.services;

import android.util.Log;
import okhttp3.*;
import org.json.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ModerationServ {
    private static final String TAG = "ModerationServ";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface ModerationCallback {
        void onSuccess(boolean isAppropriate, String feedback);
        void onError(String error);
    }

    public static void moderateGoal(String title, String description, String deadline, ModerationCallback callback) {
        if (title == null || description == null) {
            callback.onError("Название или описание цели не может быть пустым");
            return;
        }
        String prompt = String.format(
                "Проанализируйте цель и её описание на русском языке:\n" +
                        "Название цели: %s\n" +
                        "Описание цели: %s\n\n" +
                        "Дедлайн: %s\n\n" +
                        "Пожалуйста, оцените следующие аспекты:\n" +
                        "1. Соответствие названия и описания (логическая связь)\n" +
                        "2. Измеримость (есть ли конкретные критерии достижения)\n\n" +
                        "3. Можно ли это сделать в это время? Например, спорт ночью невозможен, а сон днём необычен.\n" +
                        "Если нельзя выполнить в то время объясни почему\n" +
                        "Ответ в JSON-формате:{\"appropriate\": true/false, \"feedback\": 'краткий комментарий'}",
                title, description,deadline
        );

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.put(message);
            jsonBody.put("messages", messages);
            jsonBody.put("temperature", 0.5);
        } catch (JSONException e) {
            callback.onError("Ошибка при формировании запроса: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    if (!response.isSuccessful()) {
                        callback.onError("Ошибка API: " + jsonResponse.optString("error", "Неизвестная ошибка"));
                        return;
                    }

                    String content = jsonResponse
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    JSONObject result = new JSONObject(content);
                    boolean isAppropriate = result.getBoolean("appropriate");
                    String feedback = result.getString("feedback");
                    callback.onSuccess(isAppropriate, feedback);
                } catch (JSONException e) {
                    callback.onError("Ошибка при обработке ответа: " + e.getMessage());
                }
            }
        });
    }
}