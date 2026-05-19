package net.archie.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class WebActivity extends AppCompatActivity {

    private EditText searchInput, fetchUrlInput;
    private TextView resultView;
    private ProgressBar progressBar;
    private ScrollView scrollResult;
    private Spinner engineSpinner;
    private OpenCodeApi api;
    private Prefs prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        prefs = new Prefs(this);
        api = new OpenCodeApi();

        searchInput = findViewById(R.id.edit_search);
        fetchUrlInput = findViewById(R.id.edit_fetch_url);
        engineSpinner = findViewById(R.id.spinner_engines);
        resultView = findViewById(R.id.text_result);
        progressBar = findViewById(R.id.progress_bar);
        scrollResult = findViewById(R.id.scroll_result);
        Button btnSearch = findViewById(R.id.btn_search);
        Button btnFetch = findViewById(R.id.btn_fetch);

        String[] engines = {"bing", "duckduckgo", "baidu", "sogou", "brave", "startpage"};
        ArrayAdapter<String> engineAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, engines);
        engineSpinner.setAdapter(engineAdapter);
        engineSpinner.setSelection(0);

        String defaultServer = prefs.getSearchServer();
        if (!TextUtils.isEmpty(defaultServer)) {
            resultView.setText("搜索服务器: " + defaultServer + "\n");
        }

        btnSearch.setOnClickListener(v -> doSearch());
        btnFetch.setOnClickListener(v -> doFetch());
    }

    private void doSearch() {
        String query = searchInput.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }

        String searchServer = prefs.getSearchServer();
        if (TextUtils.isEmpty(searchServer)) {
            Toast.makeText(this, "请先在设置中配置搜索服务器", Toast.LENGTH_SHORT).show();
            return;
        }

        String engine = engineSpinner.getSelectedItem().toString();
        showLoading(true);
        resultView.setText("正在搜索: " + query + " ...\n");

        api.webSearch(searchServer, query, new OpenCodeApi.Callback<String>() {
            @Override
            public void onSuccess(String result) {
                handler.post(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("=== 搜索结果: ").append(query).append(" ===\n\n");
                    sb.append(result);
                    resultView.setText(sb.toString());
                    showLoading(false);
                    scrollResult.post(() -> scrollResult.fullScroll(View.FOCUS_DOWN));
                });
            }

            @Override
            public void onError(String error) {
                handler.post(() -> {
                    resultView.setText("搜索失败: " + error);
                    showLoading(false);
                });
            }
        });
    }

    private void doFetch() {
        String url = fetchUrlInput.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "请输入网页URL", Toast.LENGTH_SHORT).show();
            return;
        }

        String searchServer = prefs.getSearchServer();
        if (TextUtils.isEmpty(searchServer)) {
            Toast.makeText(this, "请先在设置中配置搜索服务器", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        final String finalUrl = url;
        showLoading(true);
        resultView.setText("正在抓取: " + finalUrl + " ...\n");

        api.webFetch(searchServer, finalUrl, 0, new OpenCodeApi.Callback<String>() {
            @Override
            public void onSuccess(String result) {
                handler.post(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("=== 网页内容: ").append(finalUrl).append(" ===\n\n");
                    if (result.length() > 10000) {
                        sb.append(result.substring(0, 10000)).append("\n\n... (内容过长，已截断)");
                    } else {
                        sb.append(result);
                    }
                    resultView.setText(sb.toString());
                    showLoading(false);
                    scrollResult.post(() -> scrollResult.fullScroll(View.FOCUS_DOWN));
                });
            }

            @Override
            public void onError(String error) {
                handler.post(() -> {
                    resultView.setText("抓取失败: " + error);
                    showLoading(false);
                });
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
