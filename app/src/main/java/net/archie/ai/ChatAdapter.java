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

    public void addMessage(Message msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateLastMessage(AiResponse response) {
        if (!messages.isEmpty()) {
            Message last = messages.get(messages.size() - 1);
            last.content = response.content;
            last.thinkingContent = response.thinking;
            notifyItemChanged(messages.size() - 1);
        }
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void clear() {
        messages.clear();
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
                if (holder.thinkingLabel2 != null) {
                    holder.thinkingLabel2.setVisibility(View.GONE);
                }
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
        TextView contentView;
        TextView timeView;
        TextView thinkingLabel;
        TextView thinkingLabel2;
        TextView thinkingView;

        ViewHolder(View itemView) {
            super(itemView);
            contentView = itemView.findViewById(R.id.text_content);
            timeView = itemView.findViewById(R.id.text_time);
            thinkingLabel = itemView.findViewById(R.id.text_thinking_label);
            thinkingLabel2 = itemView.findViewById(R.id.text_thinking_label2);
            thinkingView = itemView.findViewById(R.id.text_thinking);
        }
    }
}
