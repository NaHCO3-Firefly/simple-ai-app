package net.archie.ai;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText apiKeyInput;
    private TextView logView;
    private Prefs prefs;
    private Logger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new Prefs(this);
        logger = new Logger(this);

        apiKeyInput = findViewById(R.id.edit_api_key);
        logView = findViewById(R.id.text_log);
        Button saveBtn = findViewById(R.id.btn_save);
        Button clearLogBtn = findViewById(R.id.btn_clear_log);

        String savedKey = prefs.getApiKey();
        if (!TextUtils.isEmpty(savedKey)) {
            apiKeyInput.setText(savedKey);
        }

        logView.setText(logger.read());

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
    }
}
