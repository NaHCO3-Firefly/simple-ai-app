package net.archie.ai;

import android.util.Log;

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
                    String body = readAll(conn);
                    conn.disconnect();
                    JSONObject json = new JSONObject(body);
                    JSONArray data = json.getJSONArray("data");
                    List<String> modelIds = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        modelIds.add(data.getJSONObject(i).getString("id"));
                    }
                    callback.onSuccess(modelIds);
                } else {
                    String errorBody = readAll(conn);
                    conn.disconnect();
                    callback.onError("HTTP " + code + ": " + errorBody);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public interface StreamCallback {
        void onUpdate(AiResponse current);
        void onComplete(AiResponse full);
        void onError(String error);
    }

    public void sendMessage(String apiKey, String model, List<Message> history,
                            boolean thinking, String reasoningEffort,
                            String systemPrompt, StreamCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject body = new JSONObject();
                body.put("model", model);

                JSONArray messages = new JSONArray();
                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    JSONObject sysMsg = new JSONObject();
                    sysMsg.put("role", "system");
                    sysMsg.put("content", systemPrompt);
                    messages.put(sysMsg);
                }
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
                body.put("stream", true);

                if (thinking) {
                    body.put("reasoning_effort", reasoningEffort);
                }

                long startTime = System.currentTimeMillis();

                URL url = new URL(CHAT_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(180000);

                byte[] postData = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(postData);
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    String errorBody = readAll(conn);
                    conn.disconnect();
                    callback.onError("HTTP " + code + ": " + errorBody);
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

                StringBuilder contentBuf = new StringBuilder();
                StringBuilder thinkingBuf = new StringBuilder();
                int promptTokens = 0, completionTokens = 0;
                long lastUpdate = 0;
                AiResponse current = new AiResponse();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) break;

                    try {
                        JSONObject chunk = new JSONObject(data);
                        JSONArray choices = chunk.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
                            if (delta != null) {
                                Object c = delta.opt("content");
                                if (c instanceof String) contentBuf.append((String) c);
                                Object r = delta.opt("reasoning_content");
                                if (r instanceof String) thinkingBuf.append((String) r);
                            }
                        }

                        JSONObject usage = chunk.optJSONObject("usage");
                        if (usage != null) {
                            promptTokens = usage.optInt("prompt_tokens", 0);
                            completionTokens = usage.optInt("completion_tokens", 0);
                        }

                        long now = System.currentTimeMillis();
                        if (now - lastUpdate > 80 || contentBuf.toString().endsWith("\n")) {
                            lastUpdate = now;
                            current.content = contentBuf.toString();
                            current.thinking = thinkingBuf.toString();
                            callback.onUpdate(current);
                        }
                    } catch (Exception e) {
                        Log.w("OpenCodeApi", "SSE parse error", e);
                    }
                }
                reader.close();
                conn.disconnect();

                AiResponse full = new AiResponse();
                full.content = contentBuf.toString();
                full.thinking = thinkingBuf.toString();
                full.promptTokens = promptTokens;
                full.completionTokens = completionTokens;
                full.tookMs = System.currentTimeMillis() - startTime;
                callback.onComplete(full);

            } catch (Exception e) {
                if (conn != null) conn.disconnect();
                callback.onError(e.getMessage());
            }
        });
    }

    private String readAll(HttpURLConnection conn) throws Exception {
        BufferedReader reader;
        if (conn.getErrorStream() != null) {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    public void generateTitle(String apiKey, String model, List<Message> history, Callback<String> callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                JSONArray messages = new JSONArray();
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", "请根据以下对话内容，生成一个简短的标题（不超过15个字），只输出标题文本，不要加引号、标点或任何额外说明。If the conversation is in English, output an English title under 8 words.");
                messages.put(sysMsg);

                for (Message msg : history) {
                    if (msg.type == Message.TYPE_AI && (msg.content.equals("...") || msg.content.isEmpty()))
                        continue;
                    JSONObject m = new JSONObject();
                    m.put("role", msg.type == Message.TYPE_USER ? "user" : "assistant");
                    m.put("content", msg.content);
                    messages.put(m);
                }

                JSONObject reqMsg = new JSONObject();
                reqMsg.put("role", "user");
                reqMsg.put("content", "请为以上对话生成标题");
                messages.put(reqMsg);

                JSONObject body = new JSONObject();
                body.put("model", model);
                body.put("messages", messages);
                body.put("stream", false);
                body.put("max_tokens", 50);

                URL url = new URL(CHAT_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                byte[] postData = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(postData);
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    String responseBody = readAll(conn);
                    conn.disconnect();
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray choices = json.getJSONArray("choices");
                    if (choices.length() > 0) {
                        String content = choices.getJSONObject(0)
                                .getJSONObject("message").getString("content");
                        content = content.trim().replaceAll("^[\"'「『]|[\"'」』]$", "").trim();
                        callback.onSuccess(content);
                    } else {
                        callback.onError("No response from API");
                    }
                } else {
                    String errorBody = readAll(conn);
                    conn.disconnect();
                    callback.onError("HTTP " + code + ": " + errorBody);
                }
            } catch (Exception e) {
                if (conn != null) conn.disconnect();
                callback.onError(e.getMessage());
            }
        });
    }
}
