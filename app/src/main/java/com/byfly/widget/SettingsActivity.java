package com.byfly.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText editLogin, editPassword;
    private Button btnSave;
    private TextView textResult;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editLogin = findViewById(R.id.edit_login);
        editPassword = findViewById(R.id.edit_password);
        btnSave = findViewById(R.id.btn_save);
        textResult = findViewById(R.id.text_result);
        progressBar = findViewById(R.id.progress_bar);

        SharedPreferences prefs = getSharedPreferences(ByFlyWidgetProvider.PREFS_NAME, MODE_PRIVATE);
        editLogin.setText(prefs.getString("login", ""));
        editPassword.setText(prefs.getString("password", ""));

        TextView textAuthor = findViewById(R.id.text_author);
        textAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browser = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/plumbersky-arch"));
                startActivity(browser);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String login = editLogin.getText().toString().trim();
                String password = editPassword.getText().toString().trim();

                if (login.isEmpty() || password.isEmpty()) {
                    showResult(getString(R.string.error_login), false);
                    return;
                }

                btnSave.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                textResult.setVisibility(View.GONE);

                new VerifyTask().execute(login, password);
            }
        });
    }

    private void showResult(String message, boolean success) {
        textResult.setText(message);
        textResult.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        btnSave.setEnabled(true);
        textResult.setTextColor(success ? 0xFF2E7D32 : 0xFFC62828);
    }

    private class VerifyTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            try {
                String balance = ByFlyFetcher.fetchBalance(params[0], params[1]);
                return new String[]{balance, null};
            } catch (Exception e) {
                return new String[]{null, e.getMessage()};
            }
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result[0] != null) {
                String login = editLogin.getText().toString().trim();
                String password = editPassword.getText().toString().trim();

                SharedPreferences.Editor editor = getSharedPreferences(
                        ByFlyWidgetProvider.PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString("login", login);
                editor.putString("password", password);
                editor.putString("balance_" + login, result[0]);
                editor.apply();

                updateAllWidgets();
                showResult("Баланс: " + result[0], true);
            } else {
                String errMsg = result[1] != null ? result[1] : getString(R.string.error_login);
                showResult(errMsg, false);
            }
        }
    }

    private void updateAllWidgets() {
        AppWidgetManager mgr = AppWidgetManager.getInstance(this);

        ComponentName cn1 = new ComponentName(this, ByFlyWidgetProvider.class);
        for (int id : mgr.getAppWidgetIds(cn1)) {
            ByFlyWidgetProvider.updateWidget(this, mgr, id, R.layout.widget_byfly);
        }

        ComponentName cn2 = new ComponentName(this, ByFlyWidgetSmallProvider.class);
        for (int id : mgr.getAppWidgetIds(cn2)) {
            ByFlyWidgetProvider.updateWidget(this, mgr, id, R.layout.widget_byfly_small);
        }
    }
}
