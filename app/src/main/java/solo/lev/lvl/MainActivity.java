package solo.lev.lvl;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import solo.lev.lvl.fragments.HomeFragment;
import solo.lev.lvl.fragments.AddGoalFragment;
import solo.lev.lvl.fragments.SettingsFragment;
import solo.lev.lvl.services.AppBlockerService;
import java.util.ArrayList;
import java.util.List;
import solo.lev.lvl.models.Goal;

public class MainActivity extends AppCompatActivity implements GoalListener {
    private Button logoutButton;
    private TextView welcomeText;
    private SharedPreferences preferences;
    private BottomNavigationView bottomNavigationView;
    private List<Goal> goals;
    private HomeFragment homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences settingsPrefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDarkMode = settingsPrefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        if (!preferences.getBoolean("permissions_requested", false)) {
            checkPermissions();
        } else {
            if (hasUsageStatsPermission()) {
                startService(new Intent(this, AppBlockerService.class));
            }
        }
        if (!preferences.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }
        logoutButton = findViewById(R.id.logoutButton);
        welcomeText = findViewById(R.id.welcomeText);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        String currentUserEmail = preferences.getString("currentUser", "");
        String userName = preferences.getString("name_" + currentUserEmail, "");
        if (!userName.isEmpty()) {
            welcomeText.setText("Добро пожаловать, \n" + userName + "!");
        } else {
            welcomeText.setText("Добро пожаловать!");
        }
        logoutButton.setOnClickListener(v -> logout());
        goals = new ArrayList<>();
        homeFragment = new HomeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .commit();

        setupBottomNavigation();
    }

    private void checkPermissions() {
        if (!hasUsageStatsPermission()) {
            new AlertDialog.Builder(this)
                .setTitle("Необходимо разрешение")
                .setMessage("Для работы блокировки приложений необходим доступ к статистике использования. Пожалуйста, предоставьте разрешение в настройках.")
                .setPositiveButton("Открыть настройки", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Отмена", (dialog, which) -> {
                    preferences.edit().putBoolean("permissions_requested", true).apply();
                })
                .setCancelable(false)
                .show();
        } else {
            preferences.edit().putBoolean("permissions_requested", true).apply();
            startService(new Intent(this, AppBlockerService.class));
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!preferences.getBoolean("permissions_requested", false)) {
            checkPermissions();
        } else if (hasUsageStatsPermission()) {
            startService(new Intent(this, AppBlockerService.class));
        }
    }

    public void logout() {
        Log.d("MainActivity", "Начало процесса выхода");
        
        // Очищаем данные пользователя
        SharedPreferences preferences = getSharedPreferences("GoalsPrefs", MODE_PRIVATE);
        preferences.edit().clear().apply();
        
        // Останавливаем сервис блокировки приложений
        stopService(new Intent(this, AppBlockerService.class));
        
        // Создаем интент для перехода на экран входа
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Запускаем активность входа и закрываем текущую активность
        startActivity(loginIntent);
        finishAffinity();
        
        Log.d("MainActivity", "Процесс выхода завершен");
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    selectedFragment = new HomeFragment();
                    break;
                case R.id.navigation_add:
                    selectedFragment = new AddGoalFragment();
                    break;
                case R.id.navigation_settings:
                    selectedFragment = new SettingsFragment();
                    break;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
                return true;
            }
            return false;
        });

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new HomeFragment())
            .commit();
    }

    @Override
    public void onGoalAdded(Goal goal) {
        Log.d("MainActivity", "Adding goal: " + goal.getTitle());
        goals.add(goal);

        HomeFragment homeFragment = new HomeFragment();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, homeFragment)
            .commit();

        getSupportFragmentManager().executePendingTransactions();
        if (homeFragment != null) {
            homeFragment.addGoal(goal);
        }
    }

    public List<Goal> getGoals() {
        return goals;
    }

    public void addGoal(Goal goal) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).addGoal(goal);
        } else {
            if (homeFragment != null) {
                homeFragment.addGoal(goal);
            }
        }
    }
}