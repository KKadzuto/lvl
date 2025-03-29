package solo.lev.lvl.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import solo.lev.lvl.R;
import solo.lev.lvl.models.AppInfo;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {
    private List<AppInfo> apps;
    private OnAppSelectedListener listener;

    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo app, boolean isSelected);
    }

    public AppAdapter(OnAppSelectedListener listener) {
        this.apps = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public void setApps(List<AppInfo> apps) {
        this.apps = apps;
        notifyDataSetChanged();
    }

    class AppViewHolder extends RecyclerView.ViewHolder {
        private ImageView appIcon;
        private TextView appName;
        private CheckBox appCheckbox;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appCheckbox = itemView.findViewById(R.id.appCheckbox);
        }

        public void bind(AppInfo app) {
            appIcon.setImageDrawable(app.getIcon());
            appName.setText(app.getAppName());
            appCheckbox.setChecked(app.isSelected());

            appCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                app.setSelected(isChecked);
                if (listener != null) {
                    listener.onAppSelected(app, isChecked);
                }
            });

            itemView.setOnClickListener(v -> appCheckbox.toggle());
        }
    }
} 