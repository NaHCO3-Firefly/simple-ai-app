package net.archie.ai;

import android.content.Context;
import android.content.SharedPreferences;

public class Logger {
    private static final String NAME = "simple_ai_log";
    private final SharedPreferences sp;

    public Logger(Context context) {
        sp = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public void append(String msg) {
        String existing = sp.getString("log", "");
        sp.edit().putString("log", existing + msg + "\n").apply();
    }

    public String read() {
        return sp.getString("log", "");
    }

    public void clear() {
        sp.edit().putString("log", "").apply();
    }
}
