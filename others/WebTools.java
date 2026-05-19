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

/**
 * 免费网页搜索和抓取工具
 *
 * 搜索后端：
 *   - Parallel: https://search.parallel.ai/mcp  (MCP JSON-RPC, 无需API Key, 免费)
 *   - Exa:      https://mcp.exa.ai/mcp          (MCP JSON-RPC, 无Key有限制,
 *                                                  免费Key从 dashboard.exa.ai 申请)
 * 抓取：
 *   - 直接 HttpURLConnection GET
 *
 * 全部基于 OpenCode 源码中 webfetch.ts / websearch.ts / mcp-websearch.ts 的实现。
 */
public class WebTools {

    private static final String TAG = "WebTools";

    // Parallel Search MCP endpoint (免费, 无需Key)
    private static final String PARALLEL_URL = "https://search.parallel.ai/mcp";

    // Exa AI MCP endpoint (无Key有频率限制, 免费Key: https://dashboard.exa.ai/api-keys)
    private static final String EXA_URL = "https://mcp.exa.ai/mcp";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ==================== 数据模型 ====================

    public static class SearchResult {
        public String title;
        public String url;
        public String snippet;
        public String publishDate;
    }

    public static class FetchResult {
        public String url;
        public String contentType;
        public String content;
        public long contentLength;
    }

    // ==================== 回调接口 ====================

    public interface SearchCallback {
        void onSuccess(List<SearchResult> results);
        void onError(String error);
    }

    public interface FetchCallback {
        void onSuccess(FetchResult result);
        void onError(String error);
    }

    // ==================== 网页搜索 (Parallel, 免费) ====================

    /**
     * 使用 Parallel Search 搜索网页（免费，无需API Key）
     */
    public void searchParallel(String query, SearchCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("jsonrpc", "2.0");
                body.put("id", 1);
                body.put("method", "tools/call");

                JSONObject args = new JSONObject();
                args.put("objective", query);

                JSONArray searchQueries = new JSONArray();
                searchQueries.put(query);
                args.put("search_queries", searchQueries);
                args.put("session_id", "android-" + System.currentTimeMillis());

                JSONObject params = new JSONObject();
                params.put("name", "web_search");
                params.put("arguments", args);
                body.put("params", params);

                String response = httpPost(PARALLEL_URL, body.toString(),
                        "application/json, text/event-stream", null);

                JSONObject json = new JSONObject(response);
                JSONObject result = json.getJSONObject("result");
                JSONArray contentArr = result.getJSONArray("content");
                String text = contentArr.getJSONObject(0).getString("text");

                JSONObject searchData = new JSONObject(text);
                JSONArray results = searchData.getJSONArray("results");

                List<SearchResult> parsed = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject r = results.getJSONObject(i);
                    SearchResult sr = new SearchResult();
                    sr.title = r.optString("title", "");
                    sr.url = r.optString("url", "");
                    sr.publishDate = r.optString("publish_date", null);
                    JSONArray excerpts = r.optJSONArray("excerpts");
                    if (excerpts != null && excerpts.length() > 0) {
                        sr.snippet = excerpts.getString(0);
                    }
                    parsed.add(sr);
                }
                callback.onSuccess(parsed);
            } catch (Exception e) {
                callback.onError("Parallel search failed: " + e.getMessage());
            }
        });
    }

    // ==================== 网页搜索 (Exa, 免费但有限制) ====================

    /**
     * 使用 Exa AI 搜索网页
     * 无 API Key 时有频率限制，建议在 https://dashboard.exa.ai 申请免费 Key
     */
    public void searchExa(String query, int numResults, String exaApiKey, SearchCallback callback) {
        executor.execute(() -> {
            try {
                String url = EXA_URL;
                String authHeader = null;
                if (exaApiKey != null && !exaApiKey.isEmpty()) {
                    url = EXA_URL + "?exaApiKey=" + exaApiKey;
                    authHeader = "Bearer " + exaApiKey;
                }

                JSONObject body = new JSONObject();
                body.put("jsonrpc", "2.0");
                body.put("id", 1);
                body.put("method", "tools/call");

                JSONObject args = new JSONObject();
                args.put("query", query);
                args.put("type", "auto");
                args.put("numResults", numResults > 0 ? numResults : 8);
                args.put("livecrawl", "fallback");
                args.put("contextMaxCharacters", 10000);

                JSONObject params = new JSONObject();
                params.put("name", "web_search_exa");
                params.put("arguments", args);
                body.put("params", params);

                String response = httpPost(url, body.toString(),
                        "application/json, text/event-stream", authHeader);

                JSONObject json = new JSONObject(response);
                // Exa 返回格式与 Parallel 相同 (MCP JSON-RPC)
                JSONObject result = json.getJSONObject("result");
                JSONArray contentArr = result.getJSONArray("content");
                String text = contentArr.getJSONObject(0).getString("text");

                JSONObject searchData = new JSONObject(text);
                JSONArray results = searchData.getJSONArray("results");

                List<SearchResult> parsed = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject r = results.getJSONObject(i);
                    SearchResult sr = new SearchResult();
                    sr.title = r.optString("title", "");
                    sr.url = r.optString("url", "");
                    sr.publishDate = r.optString("publishedDate", null);
                    sr.snippet = r.optString("text", r.optString("snippet", ""));
                    parsed.add(sr);
                }
                callback.onSuccess(parsed);
            } catch (Exception e) {
                callback.onError("Exa search failed: " + e.getMessage());
            }
        });
    }

    // ==================== 网页抓取 (完全免费) ====================

    /**
     * 抓取指定网页并提取纯文本
     * maxBytes: 最大返回字节数 (默认 500KB)
     */
    public void fetchUrl(String urlStr, int maxBytes, FetchCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept",
                        "text/html,application/xhtml+xml,text/plain;q=0.9,*/*;q=0.8");
                conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    conn.disconnect();
                    callback.onError("HTTP " + code);
                    return;
                }

                String contentType = conn.getContentType();
                if (contentType == null) contentType = "";

                FetchResult result = new FetchResult();
                result.url = urlStr;
                result.contentType = contentType.split(";")[0].trim();

                // 读取内容，限制最大字节
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                int limit = maxBytes > 0 ? maxBytes : 512000;
                while ((line = reader.readLine()) != null && sb.length() < limit) {
                    sb.append(line).append("\n");
                }
                reader.close();
                conn.disconnect();

                // HTML -> 纯文本
                if (contentType.contains("text/html") || contentType.contains("application/xhtml")) {
                    result.content = extractText(sb.toString());
                } else {
                    result.content = sb.toString();
                }
                result.contentLength = result.content.length();

                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onError("Fetch failed: " + e.getMessage());
            }
        });
    }

    // ==================== 工具方法 ====================

    private String httpPost(String urlStr, String jsonBody, String accept, String auth)
            throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", accept);
        conn.setRequestProperty("User-Agent", "opencode/android");
        if (auth != null) {
            conn.setRequestProperty("Authorization", auth);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        byte[] data = jsonBody.getBytes(StandardCharsets.UTF_8);
        OutputStream os = conn.getOutputStream();
        os.write(data);
        os.flush();
        os.close();

        int code = conn.getResponseCode();
        String response = readAll(conn);
        conn.disconnect();

        if (code != 200) {
            throw new Exception("HTTP " + code + ": " +
                    (response.length() > 200 ? response.substring(0, 200) : response));
        }
        return response;
    }

    private String readAll(HttpURLConnection conn) throws Exception {
        BufferedReader reader;
        if (conn.getErrorStream() != null) {
            reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * 简单 HTML 标签去除，提取纯文本
     */
    private static String extractText(String html) {
        // 移除 script/style/noscript 块
        String cleaned = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<noscript[^>]*>.*?</noscript>", " ")
                .replaceAll("(?is)<iframe[^>]*>.*?</iframe>", " ");

        // 移除所有 HTML 标签
        String text = cleaned.replaceAll("<[^>]+>", " ");

        // 解码常见 HTML 实体
        text = text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ");

        // 压缩空白
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
