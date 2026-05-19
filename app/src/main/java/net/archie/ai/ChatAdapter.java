package net.archie.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final List<Message> messages = new ArrayList<>();
    private String lastTokenInfo = "";

    public void addMessage(Message msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateLastStream(AiResponse resp) {
        if (!messages.isEmpty()) {
            Message last = messages.get(messages.size() - 1);
            if (last.type != Message.TYPE_AI) return;
            last.content = resp.content;
            last.thinkingContent = resp.thinking;
            if (resp.thinking != null && !resp.thinking.isEmpty()) last.thinkingExpanded = true;
            if (resp.tookMs > 0) {
                lastTokenInfo = resp.completionTokens + " token · " + (resp.tookMs / 1000.0) + "s";
            }
            notifyItemChanged(messages.size() - 1);
        }
    }

    public void updateLastComplete(AiResponse resp) {
        if (!messages.isEmpty()) {
            Message last = messages.get(messages.size() - 1);
            if (last.type != Message.TYPE_AI) return;
            last.content = resp.content;
            last.thinkingContent = resp.thinking;
            if (resp.thinking != null && !resp.thinking.isEmpty()) last.thinkingExpanded = true;
            if (resp.tookMs > 0) {
                String info = resp.completionTokens + " token · " + (resp.tookMs / 1000.0) + "s";
                lastTokenInfo = info;
                last.tokenInfo = info;
            }
            notifyItemChanged(messages.size() - 1);
        }
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void clear() {
        messages.clear();
        lastTokenInfo = "";
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == Message.TYPE_USER) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_ai, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messages.get(position);
        holder.contentView.setText(msg.content);
        holder.timeView.setText(formatTime(msg.timestamp));

        if (holder.thinkingView != null) {
            if (msg.thinkingContent != null && !msg.thinkingContent.isEmpty()) {
                holder.thinkingView.setVisibility(View.VISIBLE);
                holder.thinkingLabel.setVisibility(View.VISIBLE);
                holder.thinkingView.setText(msg.thinkingContent);
                holder.thinkingView.setVisibility(msg.thinkingExpanded ? View.VISIBLE : View.GONE);
                String label = msg.thinkingExpanded ? "▼ 思考过程" : "▶ 思考过程 (点击展开)";
                holder.thinkingLabel.setText(label);

                View.OnClickListener toggle = v -> {
                    msg.thinkingExpanded = !msg.thinkingExpanded;
                    notifyItemChanged(position);
                };
                holder.thinkingLabel.setOnClickListener(toggle);

                if (holder.thinkingLabel2 != null) {
                    holder.thinkingLabel2.setVisibility(View.VISIBLE);
                    holder.thinkingLabel2.setOnClickListener(toggle);
                }
            } else {
                holder.thinkingView.setVisibility(View.GONE);
                holder.thinkingLabel.setVisibility(View.GONE);
                if (holder.thinkingLabel2 != null) holder.thinkingLabel2.setVisibility(View.GONE);
            }
        }

        if (holder.tokenView != null) {
            boolean isLast = position == messages.size() - 1;
            if (msg.type == Message.TYPE_AI) {
                if (isLast && !lastTokenInfo.isEmpty()) {
                    holder.tokenView.setText(lastTokenInfo);
                    holder.tokenView.setVisibility(View.VISIBLE);
                } else if (msg.tokenInfo != null && !msg.tokenInfo.isEmpty()) {
                    holder.tokenView.setText(msg.tokenInfo);
                    holder.tokenView.setVisibility(View.VISIBLE);
                } else {
                    holder.tokenView.setVisibility(View.GONE);
                }
            } else {
                holder.tokenView.setVisibility(View.GONE);
            }
        }

        if (holder.contentView.getText().length() == 0) {
            holder.contentView.setVisibility(View.GONE);
        } else {
            holder.contentView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView contentView, timeView, thinkingLabel, thinkingLabel2, thinkingView, tokenView;

        ViewHolder(View itemView) {
            super(itemView);
            contentView = itemView.findViewById(R.id.text_content);
            timeView = itemView.findViewById(R.id.text_time);
            thinkingLabel = itemView.findViewById(R.id.text_thinking_label);
            thinkingLabel2 = itemView.findViewById(R.id.text_thinking_label2);
            thinkingView = itemView.findViewById(R.id.text_thinking);
            tokenView = itemView.findViewById(R.id.text_token_info);
        }
    }
}
