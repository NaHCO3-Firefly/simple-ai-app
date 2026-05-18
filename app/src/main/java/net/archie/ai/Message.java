package net.archie.ai;

public class Message {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;

    public int type;
    public String content;
    public String thinkingContent;
    public boolean thinkingExpanded;
    public long timestamp;

    public Message(String content, int type) {
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}
