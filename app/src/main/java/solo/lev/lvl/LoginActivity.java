package solo.lev.lvl;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        
        // Если это выход из приложения, очищаем все данные
        if (getIntent().hasExtra("logout")) {
            preferences.edit()
                .clear()
                .apply();
            return;
        }

        // Проверяем, залогинен ли пользователь
        if (preferences.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);

        loginButton.setOnClickListener(v -> {
            if (validateInput()) {
                loginUser();
            }
        });

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private boolean validateInput() {
        if (emailInput.getText().toString().trim().isEmpty() || 
            passwordInput.getText().toString().isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", 
                         Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String savedEmail = preferences.getString("email_" + email, "");
        String savedPassword = preferences.getString("password_" + email, "");

        if (email.equals(savedEmail) && password.equals(savedPassword)) {
            preferences.edit()
                .putBoolean("isLoggedIn", true)
                .putString("currentUser", email)
                .apply();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Неверный email или пароль", Toast.LENGTH_LONG).show();
        }
    }
} 