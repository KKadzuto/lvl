package solo.lev.lvl.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import solo.lev.lvl.R;
import solo.lev.lvl.LoginActivity;
import solo.lev.lvl.adapters.AppAdapter;
import solo.lev.lvl.models.AppInfo;
import android.content.Context;
import solo.lev.lvl.MainActivity;
import com.google.android.material.textfield.TextInputEditText;
import solo.lev.lvl.services.WeatherService;
import android.widget.Button;
import solo.lev.lvl.adapters.AppSelectAdapter;
import solo.lev.lvl.services.AppBlockerService;
import android.app.Activity;

public class SettingsFragment extends Fragment {
    private Button logoutButton;
    private MaterialButton checkWeatherButton;
    private TextInputEditText dateInput;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private RecyclerView appListRecyclerView;
    private MaterialButton permissionButton;
    private List<ApplicationInfo> installedApps;
    private List<String> selectedApps;
    private AppSelectAdapter adapter;
    private SharedPreferences preferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = requireContext().getSharedPreferences("GoalsPrefs", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        appListRecyclerView = view.findViewById(R.id.appListRecyclerView);
        permissionButton = view.findViewById(R.id.permissionButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        setupPermissionButton();
        loadInstalledApps();
        setupRecyclerView();
        requireContext().startService(new Intent(requireContext(), AppBlockerService.class));
        setupLogoutButton();
        return view;
    }

    private void setupRecyclerView() {
        appListRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppSelectAdapter(requireContext(), installedApps, selectedApps,
            (packageName, isSelected) -> {
                if (isSelected) {
                    selectedApps.add(packageName);
                } else {
                    selectedApps.remove(packageName);
                }
                saveSelectedApps();
            });
        appListRecyclerView.setAdapter(adapter);
    }

    private void setupPermissionButton() {
        permissionButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        });
    }

    private void loadInstalledApps() {
        PackageManager pm = requireContext().getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        installedApps = new ArrayList<>();
        
        for (ResolveInfo resolveInfo : resolveInfos) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(resolveInfo.activityInfo.packageName, 0);
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    installedApps.add(appInfo);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("SettingsFragment", "Ошибка при получении информации о приложении", e);
            }
        }
        
        selectedApps = loadSelectedApps();
    }

    private List<String> loadSelectedApps() {
        String json = preferences.getString("blocked_apps", "[]");
        Type type = new TypeToken<List<String>>(){}.getType();
        List<String> apps = new Gson().fromJson(json, type);
        return apps != null ? apps : new ArrayList<>();
    }

    private void saveSelectedApps() {
        String json = new Gson().toJson(selectedApps);
        preferences.edit().putString("blocked_apps", json).apply();
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });
    }

    private void setupWeatherButton() {
        checkWeatherButton.setOnClickListener(v -> {
            String date = dateInput.getText().toString();
            if (date.isEmpty()) {
                dateInput.setError("Введите дату");
                return;
            }
            if (!date.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                dateInput.setError("Неверный формат даты. Используйте дд.мм.гггг");
                return;
            }
            checkWeatherButton.setEnabled(false);
            checkWeatherButton.setText("Загрузка...");
            WeatherService.getWeather(date, new WeatherService.WeatherCallback() {
                @Override
                public void onSuccess(String temperature, String description, String humidity) {
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        checkWeatherButton.setEnabled(true);
                        checkWeatherButton.setText("Проверить погоду");
                        
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Погода в Актобе")
                            .setMessage(String.format(
                                "Температура: %s\nОписание: %s\nВлажность: %s",
                                temperature, description, humidity
                            ))
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        checkWeatherButton.setEnabled(true);
                        checkWeatherButton.setText("Проверить погоду");
                        Toast.makeText(requireContext(), 
                            "Ошибка при получении погоды: " + error, 
                            Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            loadInstalledApps();
        }
    }
} 