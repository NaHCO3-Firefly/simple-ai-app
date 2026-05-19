package net.archie.ai;

public class Message {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_TOOL = 2;

    public int type;
    public String content;
    public String thinkingContent;
    public boolean thinkingExpanded;
    public String tokenInfo;
    public long timestamp;
    public String toolName;
    public String toolCallId;

    public Message(String content, int type) {
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}
