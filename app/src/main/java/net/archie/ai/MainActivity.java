package net.archie.ai;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private EditText inputField;
    private ChatAdapter adapter;
    private ListView convListView;
    private ArrayAdapter<String> convListAdapter;
    private OpenCodeApi api;
    private Prefs prefs;
    private ConversationStore store;
    private Logger logger;
    private Conversation currentConv;
    private boolean titleSet;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSending;
    private boolean isAtBottom = true;

    private List<Conversation> conversations = new ArrayList<>();
    private List<String> cachedModels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Prefs tmpPrefs = new Prefs(getApplicationContext());
        AppCompatDelegate.setDefaultNightMode(
                tmpPrefs.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new Prefs(this);
        store = new ConversationStore(this);
        api = new OpenCodeApi();
        logger = new Logger(this);
        cachedModels = new ArrayList<>(prefs.getCachedModels());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof TextView) {
                child.setOnClickListener(v -> showTitleDialog());
                break;
            }
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, 0, 0);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        recyclerView = findViewById(R.id.recycler_chat);
        inputField = findViewById(R.id.edit_input);
        ImageButton sendButton = findViewById(R.id.btn_send);
        ImageButton btnNewConv = findViewById(R.id.btn_new_conv);
        convListView = findViewById(R.id.list_conversations);

        adapter = new ChatAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int last = lm.findLastCompletelyVisibleItemPosition();
                isAtBottom = last >= adapter.getItemCount() - 2;
            }
        });

        convListAdapter = new ArrayAdapter<>(this, R.layout.drawer_item_conversation);
        convListView.setAdapter(convListAdapter);

        convListView.setOnItemClickListener((parent, view, position, id) -> {
            switchConversation(position);
            drawerLayout.closeDrawers();
        });
        convListView.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteConversation(position);
            return true;
        });

        btnNewConv.setOnClickListener(v -> {
            newConversation();
            drawerLayout.closeDrawers();
        });

        sendButton.setOnClickListener(v -> onSend());
        inputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onSend();
                return true;
            }
            return false;
        });

        loadConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String key = prefs.getApiKey();
        String model = prefs.getModel();
        if (TextUtils.isEmpty(key)) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (TextUtils.isEmpty(model) && cachedModels.isEmpty()) {
            showModelDialog();
        }
        if (cachedModels.isEmpty()) {
            cachedModels = new ArrayList<>(prefs.getCachedModels());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentConversation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_model) {
            showModelDialog();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadConversations() {
        conversations = store.listAll();
        refreshConvList();
        String activeId = store.getActiveId();
        if (!TextUtils.isEmpty(activeId)) {
            for (Conversation c : conversations) {
                if (c.id.equals(activeId)) {
                    switchToConversation(c);
                    return;
                }
            }
            Conversation c = store.load(activeId);
            if (c != null) {
                conversations.add(0, c);
                switchToConversation(c);
                return;
            }
        }
        if (!conversations.isEmpty()) {
            switchToConversation(conversations.get(0));
        } else {
            newConversation();
        }
    }

    private void newConversation() {
        saveCurrentConversation();
        Conversation c = new Conversation(UUID.randomUUID().toString(), "新对话");
        currentConv = c;
        conversations.add(0, c);
        refreshConvList();
        adapter.clear();
        getSupportActionBar().setTitle(c.title);
        titleSet = false;
        store.save(c);
        store.setActiveId(c.id);
    }

    private void switchConversation(int position) {
        if (position < 0 || position >= conversations.size()) return;
        saveCurrentConversation();
        switchToConversation(conversations.get(position));
    }

    private void switchToConversation(Conversation c) {
        currentConv = c;
        adapter.clear();
        for (Message m : c.messages) {
            adapter.addMessage(m);
        }
        getSupportActionBar().setTitle(c.title);
        titleSet = !c.title.equals("新对话");
        store.setActiveId(c.id);
        scrollToBottomInstant();
    }

    private void deleteConversation(int position) {
        if (position < 0 || position >= conversations.size()) return;
        Conversation c = conversations.get(position);
        new AlertDialog.Builder(this)
                .setTitle("删除对话")
                .setMessage("确定删除「" + c.title + "」？")
                .setPositiveButton("删除", (dialog, which) -> {
                    store.delete(c.id);
                    conversations.remove(position);
                    refreshConvList();
                    if (currentConv != null && currentConv.id.equals(c.id)) {
                        if (!conversations.isEmpty()) {
                            switchToConversation(conversations.get(0));
                        } else {
                            newConversation();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void refreshConvList() {
        convListAdapter.clear();
        for (Conversation c : conversations) {
            convListAdapter.add(c.title);
        }
        convListAdapter.notifyDataSetChanged();
    }

    private void saveCurrentConversation() {
        if (currentConv == null) return;
        currentConv.messages.clear();
        currentConv.messages.addAll(adapter.getMessages());
        currentConv.timestamp = System.currentTimeMillis();
        store.save(currentConv);
    }

    private void onSend() {
        if (isSending) return;
        String text = inputField.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String apiKey = prefs.getApiKey();
        String model = prefs.getModel();
        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(model)) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        if (currentConv == null) newConversation();

        inputField.setText("");
        hideKeyboard();
        isSending = true;

        if (!titleSet && adapter.getMessages().isEmpty()) {
            String title = text.length() > 15 ? text.substring(0, 15) + "…" : text;
            currentConv.title = title;
            getSupportActionBar().setTitle(title);
            titleSet = true;
            refreshConvList();
            saveCurrentConversation();
        }

        Message userMsg = new Message(text, Message.TYPE_USER);
        adapter.addMessage(userMsg);
        currentConv.messages.add(userMsg);
        logger.append("[用户] " + text);
        scrollToBottom();

        Message aiMsg = new Message("...", Message.TYPE_AI);
        adapter.addMessage(aiMsg);
        currentConv.messages.add(aiMsg);
        scrollToBottom();

        List<Message> history = new ArrayList<>(adapter.getMessages());
        history.remove(history.size() - 1);

        boolean thinking = prefs.isThinkingEnabled();
        String effort = prefs.getReasoningEffort();
        String systemPrompt = prefs.getSystemPrompt();
        boolean includeThinkingInContext = prefs.isIncludeThinkingInContext();

        api.sendMessage(apiKey, model, history, thinking, effort, systemPrompt, includeThinkingInContext, new OpenCodeApi.StreamCallback() {
            @Override
            public void onUpdate(AiResponse current) {
                handler.post(() -> {
                    adapter.updateLastStream(current);
                    scrollToBottom();
                });
            }

            @Override
            public void onComplete(AiResponse full) {
                handler.post(() -> {
                    adapter.updateLastComplete(full);
                    if (!TextUtils.isEmpty(full.thinking)) {
                        logger.append("[思考] (" + full.tookMs + "ms) " + full.thinking);
                    }
                    logger.append("[AI] (" + full.completionTokens + "tokens/" + full.tookMs + "ms) " + full.content);
                    saveCurrentConversation();
                    scrollToBottom();
                    isSending = false;
                });
            }

            @Override
            public void onError(String error) {
                handler.post(() -> {
                    AiResponse err = new AiResponse();
                    err.content = translateError(error);
                    adapter.updateLastComplete(err);
                    logger.append("[错误] " + error);
                    saveCurrentConversation();
                    scrollToBottom();
                    isSending = false;
                });
            }
        });
    }

    private String translateError(String error) {
        if (error == null) return "未知错误";
        if (error.contains("401")) return "API Key 无效，请在设置中重新输入";
        if (error.contains("403")) return "API 访问被拒绝";
        if (error.contains("429")) return "请求过于频繁，请稍后重试";
        if (error.contains("500")) return "服务器错误，请稍后重试";
        if (error.contains("timeout") || error.contains("Timeout") || error.contains("timed out"))
            return "网络超时，请检查网络连接";
        if (error.contains("Unable to resolve host") || error.contains("connect"))
            return "网络连接失败，请检查网络";
        return "错误: " + error;
    }

    private void showModelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("模型设置");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 0);

        Spinner modelSpinner = new Spinner(this);
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, cachedModels);
        modelSpinner.setAdapter(modelAdapter);
        String currentModel = prefs.getModel();
        if (!TextUtils.isEmpty(currentModel)) {
            int idx = cachedModels.indexOf(currentModel);
            if (idx >= 0) modelSpinner.setSelection(idx);
        }

        LinearLayout modelRow = new LinearLayout(this);
        modelRow.setOrientation(LinearLayout.HORIZONTAL);
        modelRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        modelSpinner.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        modelRow.addView(modelSpinner);
        android.widget.Button refreshBtn = new android.widget.Button(this);
        refreshBtn.setText("刷新");
        refreshBtn.setTextSize(12);
        refreshBtn.setOnClickListener(v -> fetchModelsForDialog(modelSpinner, modelAdapter));
        modelRow.addView(refreshBtn);
        layout.addView(modelRow);

        TextView effortLabel = new TextView(this);
        effortLabel.setText("思考强度");
        effortLabel.setTextSize(14);
        effortLabel.setPadding(0, 12, 0, 0);
        layout.addView(effortLabel);

        Spinner effortSpinner = new Spinner(this);
        String[] efforts = {"low", "medium", "high", "max"};
        ArrayAdapter<String> effortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, efforts);
        effortSpinner.setAdapter(effortAdapter);
        String currentEffort = prefs.getReasoningEffort();
        for (int i = 0; i < efforts.length; i++) {
            if (efforts[i].equals(currentEffort)) {
                effortSpinner.setSelection(i);
                break;
            }
        }
        layout.addView(effortSpinner);

        builder.setView(layout);
        builder.setPositiveButton("确定", (dialog, which) -> {
            Object selected = modelSpinner.getSelectedItem();
            if (selected != null) prefs.setModel(selected.toString());
            Object effortSelected = effortSpinner.getSelectedItem();
            if (effortSelected != null) prefs.setReasoningEffort(effortSelected.toString());
            Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void fetchModelsForDialog(Spinner spinner, ArrayAdapter<String> adapter) {
        String key = prefs.getApiKey();
        if (TextUtils.isEmpty(key)) {
            Toast.makeText(this, "请先设置 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "正在拉取模型...", Toast.LENGTH_SHORT).show();
        api.fetchModels(key, new OpenCodeApi.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> result) {
                handler.post(() -> {
                    cachedModels.clear();
                    cachedModels.addAll(result);
                    prefs.setCachedModels(new HashSet<>(result));
                    adapter.clear(); adapter.addAll(cachedModels); adapter.notifyDataSetChanged();
                    String cm = prefs.getModel();
                    if (!TextUtils.isEmpty(cm)) {
                        int idx = cachedModels.indexOf(cm);
                        if (idx >= 0) spinner.setSelection(idx);
                    }
                    Toast.makeText(MainActivity.this,
                            "加载了 " + result.size() + " 个模型", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                handler.post(() ->
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showTitleDialog() {
        if (currentConv == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑标题");

        final EditText input = new EditText(this);
        input.setText(currentConv.title);
        input.setSingleLine();
        input.setSelection(input.getText().length());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(48, 24, 48, 8);
        input.setLayoutParams(lp);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newTitle)) {
                currentConv.title = newTitle;
                getSupportActionBar().setTitle(newTitle);
                titleSet = true;
                refreshConvList();
                saveCurrentConversation();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void scrollToBottom() {
        if (!isAtBottom) return;
        recyclerView.postDelayed(() -> {
            int pos = adapter.getItemCount() - 1;
            if (pos >= 0) recyclerView.smoothScrollToPosition(pos);
        }, 50);
    }

    private void scrollToBottomInstant() {
        recyclerView.post(() -> {
            int pos = adapter.getItemCount() - 1;
            if (pos >= 0) recyclerView.scrollToPosition(pos);
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(inputField.getWindowToken(), 0);
    }
}
