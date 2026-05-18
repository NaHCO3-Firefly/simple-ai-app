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

    public boolean isDarkMode() { return sp.getBoolean(KEY_DARK_MODE, false); }
    public void setDarkMode(boolean dark) { sp.edit().putBoolean(KEY_DARK_MODE, dark).apply(); }
}
