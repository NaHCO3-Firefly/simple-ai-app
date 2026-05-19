package net.archie.ai;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileWriter;

public class SettingsActivity extends AppCompatActivity {

    private EditText apiKeyInput;
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
        logView = findViewById(R.id.text_log);
        darkSwitch = findViewById(R.id.switch_dark);
        Button saveBtn = findViewById(R.id.btn_save);
        Button clearLogBtn = findViewById(R.id.btn_clear_log);
        Button exportBtn = findViewById(R.id.btn_export);

        String savedKey = prefs.getApiKey();
        if (!TextUtils.isEmpty(savedKey)) apiKeyInput.setText(savedKey);
        logView.setText(logger.read());
        darkSwitch.setChecked(prefs.isDarkMode());

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
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            finish();
        });

        clearLogBtn.setOnClickListener(v -> {
            logger.clear();
            logView.setText("");
            Toast.makeText(this, "日志已清除", Toast.LENGTH_SHORT).show();
        });

        exportBtn.setOnClickListener(v -> exportConversations());
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
