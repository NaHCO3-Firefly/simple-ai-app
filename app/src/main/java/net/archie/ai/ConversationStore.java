package net.archie.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
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
        String json = sp.getString(KEY_LIST, "[]");
        JSONArray list;
        try { list = new JSONArray(json); } catch (Exception e) { list = new JSONArray(); }
        JSONArray newList = new JSONArray();
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            if (!entry.getString("id").equals(conv.id)) {
                newList.put(entry);
            }
        }
        newList.put(conv.toJson());
        sp.edit().putString(KEY_LIST, newList.toString()).apply();
    }

    public Conversation load(String id) {
        String json = sp.getString(KEY_LIST, "[]");
        JSONArray list;
        try { list = new JSONArray(json); } catch (Exception e) { return null; }
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            if (entry.getString("id").equals(id)) {
                return Conversation.fromJson(entry);
            }
        }
        return null;
    }

    public List<Conversation> listAll() {
        List<Conversation> result = new ArrayList<>();
        String json = sp.getString(KEY_LIST, "[]");
        JSONArray list;
        try { list = new JSONArray(json); } catch (Exception e) { return result; }
        for (int i = 0; i < list.length(); i++) {
            result.add(Conversation.fromJson(list.getJSONObject(i)));
        }
        Collections.sort(result, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        return result;
    }

    public void delete(String id) {
        String json = sp.getString(KEY_LIST, "[]");
        JSONArray list;
        try { list = new JSONArray(json); } catch (Exception e) { list = new JSONArray(); }
        JSONArray newList = new JSONArray();
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            if (!entry.getString("id").equals(id)) {
                newList.put(entry);
            }
        }
        sp.edit().putString(KEY_LIST, newList.toString()).apply();
    }

    public String getActiveId() {
        return sp.getString(KEY_ACTIVE, "");
    }

    public void setActiveId(String id) {
        sp.edit().putString(KEY_ACTIVE, id).apply();
    }
}
