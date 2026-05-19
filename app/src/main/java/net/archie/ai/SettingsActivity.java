package net.archie.ai;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileWriter;

public class SettingsActivity extends AppCompatActivity {

    private EditText apiKeyInput, systemPromptInput;
    private AutoCompleteTextView searchServerInput;
    private Switch darkSwitch;
    private TextView logView;
    private Prefs prefs;
    private Logger logger;
    private ConversationStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        prefs = new Prefs(this);
        logger = new Logger(this);
        store = new ConversationStore(this);

        apiKeyInput = findViewById(R.id.edit_api_key);
        systemPromptInput = findViewById(R.id.edit_system_prompt);
        searchServerInput = findViewById(R.id.edit_search_server);
        String[] presetUrls = {
                "http://192.168.1.63:3210",
                "http://[2409:8a60:18c3:78c0:1046:fed4:7ed8:433e]:3210",
                "http://[fe80::ba85:84ff:fe9a:469a]:3210"
        };
        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, presetUrls);
        searchServerInput.setAdapter(searchAdapter);
        logView = findViewById(R.id.text_log);
        darkSwitch = findViewById(R.id.switch_dark);
        Switch includeThinkingSwitch = findViewById(R.id.switch_include_thinking);
        Button saveBtn = findViewById(R.id.btn_save);
        Button clearLogBtn = findViewById(R.id.btn_clear_log);
        Button exportBtn = findViewById(R.id.btn_export);
        Button clearAllBtn = findViewById(R.id.btn_clear_all);

        String savedKey = prefs.getApiKey();
        if (!TextUtils.isEmpty(savedKey)) apiKeyInput.setText(savedKey);
        String savedPrompt = prefs.getSystemPrompt();
        systemPromptInput.setText(savedPrompt);
        String savedSearchServer = prefs.getSearchServer();
        if (!TextUtils.isEmpty(savedSearchServer)) searchServerInput.setText(savedSearchServer);
        logView.setText(logger.read());
        darkSwitch.setChecked(prefs.isDarkMode());
        includeThinkingSwitch.setChecked(prefs.isIncludeThinkingInContext());

        darkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDarkMode(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            recreate();
        });

        saveBtn.setOnClickListener(v -> {
            String key = apiKeyInput.getText().toString().trim();
            if (TextUtils.isEmpty(key)) {
                Toast.makeText(this, "请输入 API Key", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.setApiKey(key);
            String prompt = systemPromptInput.getText().toString().trim();
            prefs.setSystemPrompt(prompt);
            prefs.setSearchServer(searchServerInput.getText().toString().trim());
            prefs.setIncludeThinkingInContext(includeThinkingSwitch.isChecked());
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            finish();
        });

        clearLogBtn.setOnClickListener(v -> {
            logger.clear();
            logView.setText("");
            Toast.makeText(this, "日志已清除", Toast.LENGTH_SHORT).show();
        });

        exportBtn.setOnClickListener(v -> exportConversations());

        clearAllBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("清除所有会话")
                    .setMessage("确定删除所有对话记录？此操作不可撤销。")
                    .setPositiveButton("删除", (dialog, which) -> {
                        store.deleteAll();
                        Toast.makeText(this, "已清除所有会话", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void exportConversations() {
        try {
            File dir = getExternalFilesDir("conversations");
            if (dir == null) {
                Toast.makeText(this, "无法访问存储", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!dir.exists()) dir.mkdirs();

            java.util.List<Conversation> all = store.listAll();
            int count = 0;
            for (Conversation c : all) {
                File file = new File(dir, sanitize(c.title) + "_" + c.id.substring(0, 8) + ".json");
                FileWriter fw = new FileWriter(file);
                fw.write(c.toJson().toString(2));
                fw.close();
                count++;
            }
            Toast.makeText(this, "已导出 " + count + " 个对话到 " + dir.getPath(),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_");
    }
}
