package solo.lev.lvl.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import solo.lev.lvl.R;
import solo.lev.lvl.MainActivity;
import solo.lev.lvl.models.Goal;
import android.os.Handler;
import android.os.Looper;
import android.content.ComponentName;
import android.widget.Toast;
import android.app.AlertDialog;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class AppBlockerService extends Service {
    private static final String TAG = "AppBlockerService";
    private static final String GOALS_KEY = "goals";
    private static final String BLOCKED_APPS_KEY = "blocked_apps";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AppBlockerChannel";
    
    private ScheduledExecutorService scheduler;
    private SharedPreferences preferences;
    private Gson gson;
    private List<String> blockedApps;
    private Handler handler;
    private boolean isRunning = false;
    private static final long CHECK_INTERVAL = 1000; // Проверять каждую секунду
    private WindowManager windowManager;
    private View blockView;

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences("GoalsPrefs", Context.MODE_PRIVATE);
        gson = new Gson();
        blockedApps = loadBlockedApps();
        handler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        startMonitoring();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "App Blocker Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Сервис блокировки приложений");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Блокировка приложений")
            .setContentText("Сервис блокировки активен")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startMonitoring() {
        if (isRunning) return;
        isRunning = true;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                String currentApp = getCurrentApp();
                if (currentApp != null && shouldBlockApp(currentApp)) {
                    blockApp();
                }

                handler.postDelayed(this, CHECK_INTERVAL);
            }
        }, CHECK_INTERVAL);
    }

    private String getCurrentApp() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time);

        if (stats == null || stats.isEmpty()) {
            return null;
        }

        SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
        for (UsageStats usageStats : stats) {
            sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
        }

        if (!sortedMap.isEmpty()) {
            return sortedMap.get(sortedMap.lastKey()).getPackageName();
        }

        return null;
    }

    private boolean shouldBlockApp(String packageName) {
        blockedApps = loadBlockedApps();
        Log.d(TAG, "Проверка приложения: " + packageName);
        Log.d(TAG, "Список заблокированных приложений: " + blockedApps.toString());
        if (!blockedApps.contains(packageName)) {
            Log.d(TAG, "Приложение не в списке заблокированных");
            return false;
        }
        List<Goal> goals = loadGoals();
        Log.d(TAG, "Количество целей: " + goals.size());
        
        if (goals.isEmpty()) {
            Log.d(TAG, "Нет активных целей");
            return false;
        }
        long currentTime = System.currentTimeMillis();
        for (Goal goal : goals) {
            Log.d(TAG, String.format(
                "Цель: %s, выполнена: %b, дедлайн: %d, текущее время: %d",
                goal.getText(), goal.isCompleted(), goal.getDeadline(), currentTime
            ));
            
            if (!goal.isCompleted() && goal.getDeadline() < currentTime) {
                Log.d(TAG, "Найдена просроченная невыполненная цель - блокируем приложение");
                return true;
            }
        }
        
        Log.d(TAG, "Нет просроченных невыполненных целей");
        return false;
    }

    private void blockApp() {
        Log.d(TAG, "Блокировка приложения");
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);

        handler.post(() -> {
            if (blockView != null) {
                try {
                    windowManager.removeView(blockView);
                } catch (Exception e) {
                    Log.e(TAG, "Error removing view", e);
                }
            }

            LayoutInflater inflater = LayoutInflater.from(this);
            blockView = inflater.inflate(R.layout.dialog_app_blocked, null);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.CENTER;
            Button btnOk = blockView.findViewById(R.id.btnOk);
            btnOk.setOnClickListener(v -> {
                try {
                    windowManager.removeView(blockView);
                } catch (Exception e) {
                    Log.e(TAG, "Error removing view", e);
                }
            });

            try {
                windowManager.addView(blockView, params);
            } catch (Exception e) {
                Log.e(TAG, "Error showing dialog", e);
                Toast.makeText(this, 
                    "Это приложение заблокировано, пока не выполните просроченные цели!", 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private List<Goal> loadGoals() {
        String goalsJson = preferences.getString(GOALS_KEY, "[]");
        Type type = new TypeToken<List<Goal>>(){}.getType();
        List<Goal> goals = gson.fromJson(goalsJson, type);
        return goals != null ? goals : new ArrayList<>();
    }

    private List<String> loadBlockedApps() {
        String json = preferences.getString(BLOCKED_APPS_KEY, "[]");
        Type type = new TypeToken<List<String>>(){}.getType();
        return new Gson().fromJson(json, type);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (blockView != null) {
            try {
                windowManager.removeView(blockView);
            } catch (Exception e) {
                Log.e(TAG, "Error removing view on destroy", e);
            }
        }
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        stopForeground(true);
    }
} 