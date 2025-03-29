package solo.lev.lvl.adapters;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import solo.lev.lvl.R;

public class AppSelectAdapter extends RecyclerView.Adapter<AppSelectAdapter.ViewHolder> {
    private Context context;
    private List<ApplicationInfo> apps;
    private List<String> selectedApps;
    private PackageManager packageManager;
    private OnAppSelectedListener listener;

    public interface OnAppSelectedListener {
        void onAppSelected(String packageName, boolean isSelected);
    }

    public AppSelectAdapter(Context context, List<ApplicationInfo> apps, 
                          List<String> selectedApps, OnAppSelectedListener listener) {
        this.context = context;
        this.apps = apps;
        this.selectedApps = selectedApps;
        this.listener = listener;
        this.packageManager = context.getPackageManager();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_app_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationInfo app = apps.get(position);
        holder.appName.setText(app.loadLabel(packageManager));
        holder.appIcon.setImageDrawable(app.loadIcon(packageManager));
        holder.checkBox.setChecked(selectedApps.contains(app.packageName));
        
        holder.itemView.setOnClickListener(v -> {
            boolean isChecked = !holder.checkBox.isChecked();
            holder.checkBox.setChecked(isChecked);
            if (listener != null) {
                listener.onAppSelected(app.packageName, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
} 