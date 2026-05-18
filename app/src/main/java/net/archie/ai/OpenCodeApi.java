package net.archie.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenCodeApi {

    private static final String BASE_URL = "https://opencode.ai/zen/go/v1";
    private static final String CHAT_URL = BASE_URL + "/chat/completions";
    private static final String MODELS_URL = BASE_URL + "/models";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public void fetchModels(String apiKey, Callback<List<String>> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(MODELS_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    String body = readStream(conn);
                    conn.disconnect();

                    JSONObject json = new JSONObject(body);
                    JSONArray data = json.getJSONArray("data");
                    List<String> modelIds = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        modelIds.add(data.getJSONObject(i).getString("id"));
                    }
                    callback.onSuccess(modelIds);
                } else {
                    String errorBody = readStream(conn);
                    conn.disconnect();
                    callback.onError("HTTP " + code + ": " + errorBody);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void sendMessage(String apiKey, String model, List<Message> history,
                            boolean thinking, String reasoningEffort, Callback<AiResponse> callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("model", model);

                JSONArray messages = new JSONArray();
                for (Message msg : history) {
                    JSONObject m = new JSONObject();
                    m.put("role", msg.type == Message.TYPE_USER ? "user" : "assistant");
                    m.put("content", msg.content);
                    if (msg.type == Message.TYPE_AI && msg.thinkingContent != null && !msg.thinkingContent.isEmpty()) {
                        m.put("reasoning_content", msg.thinkingContent);
                    }
                    messages.put(m);
                }
                body.put("messages", messages);
                body.put("stream", false);

                if (thinking) {
                    body.put("reasoning_effort", reasoningEffort);
                }

                URL url = new URL(CHAT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(180000);

                byte[] postData = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(postData);
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    String responseBody = readStream(conn);
                    conn.disconnect();

                    JSONObject response = new JSONObject(responseBody);
                    JSONArray choices = response.getJSONArray("choices");
                    JSONObject message = choices.getJSONObject(0).getJSONObject("message");

                    AiResponse aiResp = new AiResponse();
                    aiResp.content = message.getString("content");
                    aiResp.thinking = message.optString("reasoning_content", "");
                    callback.onSuccess(aiResp);
                } else {
                    String errorBody = readStream(conn);
                    conn.disconnect();
                    callback.onError("HTTP " + code + ": " + errorBody);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    private String readStream(HttpURLConnection conn) throws Exception {
        BufferedReader reader;
        if (conn.getErrorStream() != null) {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
