package net.archie.ai;

import java.util.ArrayList;
import java.util.List;

public class AiResponse {
    public String content;
    public String thinking;
    public int promptTokens;
    public int completionTokens;
    public long tookMs;
    public List<ToolCall> toolCalls;
}
