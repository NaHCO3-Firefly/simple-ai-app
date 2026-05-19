package net.archie.ai;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Prefs {
    private static final String NAME = "simple_ai_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String KEY_THINKING = "thinking_enabled";
    private static final String KEY_REASONING_EFFORT = "reasoning_effort";
    private static final String KEY_CACHED_MODELS = "cached_models";
    private static final String KEY_SYSTEM_PROMPT = "system_prompt";
    private static final String KEY_INCLUDE_THINKING = "include_thinking_in_context";
    private static final String KEY_SEARCH_SERVER = "search_server";
    private static final String KEY_DARK_MODE = "dark_mode";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() { return sp.getString(KEY_API_KEY, ""); }
    public void setApiKey(String key) { sp.edit().putString(KEY_API_KEY, key).apply(); }

    public String getModel() { return sp.getString(KEY_MODEL, ""); }
    public void setModel(String model) { sp.edit().putString(KEY_MODEL, model).apply(); }

    public boolean isThinkingEnabled() { return sp.getBoolean(KEY_THINKING, true); }
    public void setThinkingEnabled(boolean enabled) { sp.edit().putBoolean(KEY_THINKING, enabled).apply(); }

    public String getReasoningEffort() { return sp.getString(KEY_REASONING_EFFORT, "medium"); }
    public void setReasoningEffort(String effort) { sp.edit().putString(KEY_REASONING_EFFORT, effort).apply(); }

    public Set<String> getCachedModels() { return sp.getStringSet(KEY_CACHED_MODELS, new HashSet<>()); }
    public void setCachedModels(Set<String> models) { sp.edit().putStringSet(KEY_CACHED_MODELS, models).apply(); }

    public String getSystemPrompt() { return sp.getString(KEY_SYSTEM_PROMPT, "You are a helpful assistant. 保持对话连贯，记住前面说过的话，维持一致的人格和语气。"); }
    public void setSystemPrompt(String prompt) { sp.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply(); }

    public boolean isIncludeThinkingInContext() { return sp.getBoolean(KEY_INCLUDE_THINKING, false); }
    public void setIncludeThinkingInContext(boolean include) { sp.edit().putBoolean(KEY_INCLUDE_THINKING, include).apply(); }

    public String getSearchServer() { return sp.getString(KEY_SEARCH_SERVER, "http://192.168.1.63:3210"); }
    public void setSearchServer(String url) { sp.edit().putString(KEY_SEARCH_SERVER, url).apply(); }

    public boolean isDarkMode() { return sp.getBoolean(KEY_DARK_MODE, false); }
    public void setDarkMode(boolean dark) { sp.edit().putBoolean(KEY_DARK_MODE, dark).apply(); }
}
