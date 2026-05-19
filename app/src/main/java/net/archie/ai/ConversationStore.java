package net.archie.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class ConversationStore {
    private static final String NAME = "conversations";
    private static final String KEY_LIST = "conv_list";
    private static final String KEY_ACTIVE = "active_conv";
    private final SharedPreferences sp;

    public ConversationStore(Context context) {
        sp = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public void save(Conversation conv) {
        saveInternal(conv, true);
    }

    private void saveInternal(Conversation conv, boolean retry) {
        try {
            JSONArray list = loadList();
            JSONArray newList = new JSONArray();
            for (int i = 0; i < list.length(); i++) {
                JSONObject entry = list.getJSONObject(i);
                if (!entry.getString("id").equals(conv.id)) {
                    newList.put(entry);
                }
            }
            newList.put(conv.toJson());
            sp.edit().putString(KEY_LIST, newList.toString()).apply();
        } catch (JSONException e) {
            if (retry) {
                sp.edit().putString(KEY_LIST, "[]").apply();
                saveInternal(conv, false);
            }
        }
    }

    public Conversation load(String id) {
        try {
            JSONArray list = loadList();
            for (int i = 0; i < list.length(); i++) {
                JSONObject entry = list.getJSONObject(i);
                if (entry.getString("id").equals(id)) {
                    return Conversation.fromJson(entry);
                }
            }
        } catch (JSONException ignored) {}
        return null;
    }

    public List<Conversation> listAll() {
        List<Conversation> result = new ArrayList<>();
        try {
            JSONArray list = loadList();
            for (int i = 0; i < list.length(); i++) {
                result.add(Conversation.fromJson(list.getJSONObject(i)));
            }
        } catch (JSONException ignored) {}
        Collections.sort(result, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        return result;
    }

    public void delete(String id) {
        try {
            JSONArray list = loadList();
            JSONArray newList = new JSONArray();
            for (int i = 0; i < list.length(); i++) {
                JSONObject entry = list.getJSONObject(i);
                if (!entry.getString("id").equals(id)) {
                    newList.put(entry);
                }
            }
            sp.edit().putString(KEY_LIST, newList.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public void deleteAll() {
        sp.edit().putString(KEY_LIST, "[]").putString(KEY_ACTIVE, "").apply();
    }

    public String getActiveId() {
        return sp.getString(KEY_ACTIVE, "");
    }

    public void setActiveId(String id) {
        sp.edit().putString(KEY_ACTIVE, id).apply();
    }

    private JSONArray loadList() throws JSONException {
        String json = sp.getString(KEY_LIST, "[]");
        return new JSONArray(json);
    }
}
