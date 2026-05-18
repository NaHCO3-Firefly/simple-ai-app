package net.archie.ai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Conversation {
    public String id;
    public String title;
    public long timestamp;
    public List<Message> messages;

    public Conversation(String id, String title) {
        this.id = id;
        this.title = title;
        this.timestamp = System.currentTimeMillis();
        this.messages = new ArrayList<>();
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("title", title);
            json.put("timestamp", timestamp);
            JSONArray msgs = new JSONArray();
            for (Message m : messages) {
                JSONObject mj = new JSONObject();
                mj.put("type", m.type);
                mj.put("content", m.content);
                mj.put("thinkingContent", m.thinkingContent != null ? m.thinkingContent : "");
                mj.put("tokenInfo", m.tokenInfo != null ? m.tokenInfo : "");
                mj.put("timestamp", m.timestamp);
                msgs.put(mj);
            }
            json.put("messages", msgs);
            return json;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public static Conversation fromJson(JSONObject json) {
        try {
            Conversation c = new Conversation(json.getString("id"), json.getString("title"));
            c.timestamp = json.getLong("timestamp");
            JSONArray msgs = json.getJSONArray("messages");
            for (int i = 0; i < msgs.length(); i++) {
                JSONObject mj = msgs.getJSONObject(i);
                Message m = new Message(mj.getString("content"), mj.getInt("type"));
                m.timestamp = mj.getLong("timestamp");
                String tc = mj.optString("thinkingContent", "");
                if (!tc.isEmpty()) m.thinkingContent = tc;
                String ti = mj.optString("tokenInfo", "");
                if (!ti.isEmpty()) m.tokenInfo = ti;
                c.messages.add(m);
            }
            return c;
        } catch (JSONException e) {
            return new Conversation("error", "error");
        }
    }
}
