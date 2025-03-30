package solo.lev.lvl.fragments;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import solo.lev.lvl.LoginActivity;
import solo.lev.lvl.R;
import solo.lev.lvl.adapters.AppSelectAdapter;
import solo.lev.lvl.services.AppBlockerService;

public class SettingsFragment extends Fragment {
    private Button logoutButton;
    private RecyclerView appListRecyclerView;
    private MaterialButton permissionButton;
    private List<ApplicationInfo> installedApps;
    private List<String> selectedApps;
    private AppSelectAdapter adapter;
    private SharedPreferences preferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        initViews(view);
        setupPermissionButton();
        loadInstalledApps();
        setupRecyclerView();
        setupLogoutButton();
        return view;
    }

    private void initViews(View view) {
        appListRecyclerView = view.findViewById(R.id.appListRecyclerView);
        permissionButton = view.findViewById(R.id.permissionButton);
        logoutButton = view.findViewById(R.id.logoutButton);
    }

    private void setupRecyclerView() {
        appListRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppSelectAdapter(requireContext(), installedApps, selectedApps,
                (packageName, isSelected) -> {
                    if (isSelected) {
                        if (!selectedApps.contains(packageName)) {
                            selectedApps.add(packageName);
                        }
                    } else {
                        selectedApps.remove(packageName);
                    }
                    saveSelectedApps();
                });
        appListRecyclerView.setAdapter(adapter);
    }

    private void setupPermissionButton() {
        permissionButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Не удалось открыть настройки", Toast.LENGTH_SHORT).show();
                Log.e("SettingsFragment", "Permission error", e);
            }
        });
    }

    private void loadInstalledApps() {
        PackageManager pm = requireContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        installedApps = new ArrayList<>();

        for (ApplicationInfo appInfo : apps) {
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                installedApps.add(appInfo);
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
        preferences.edit()
                .putString("blocked_apps", new Gson().toJson(selectedApps))
                .apply();

        requireContext().startService(new Intent(requireContext(), AppBlockerService.class));
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            Bundle options = ActivityOptions.makeCustomAnimation(
                    requireContext(),
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            ).toBundle();

            clearUserData();

            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                startActivity(intent, options);
            } else {
                startActivity(intent);
            }

            requireActivity().finishAffinity();
        });
    }

    private void clearUserData() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("isLoggedIn");
        editor.remove("currentUser");
        editor.apply();
        stopServices();
    }

    private void stopServices() {
        try {
            requireContext().stopService(new Intent(requireContext(), AppBlockerService.class));
        } catch (Exception e) {
            Log.e("Logout", "Error stopping services", e);
        }
    }
}