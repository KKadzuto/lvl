package solo.lev.lvl.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import solo.lev.lvl.R;
import solo.lev.lvl.models.Goal;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {
    private static final String TAG = "GoalAdapter";
    private List<Goal> goals;
    private OnGoalListener onGoalListener;
    private SimpleDateFormat dateFormat;

    public interface OnGoalListener {
        void onDeleteClick(Goal goal);
        void onGoalChecked(Goal goal, boolean isChecked);
    }

    public GoalAdapter(List<Goal> goals, OnGoalListener onGoalListener) {
        this.goals = goals != null ? goals : new ArrayList<>();
        this.onGoalListener = onGoalListener;
        this.dateFormat = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        if (position >= 0 && position < goals.size()) {
            Goal goal = goals.get(position);
            holder.bind(goal);
        } else {
            Log.e(TAG, "Invalid position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return goals != null ? goals.size() : 0;
    }

    public List<Goal> getGoals() {
        return goals;
    }

    public void updateGoals(List<Goal> newGoals) {
        this.goals = newGoals != null ? newGoals : new ArrayList<>();
        notifyDataSetChanged();
    }

    class GoalViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewGoal;
        private TextView textViewDeadline;
        private CheckBox checkBoxGoal;
        private ImageButton buttonDelete;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewGoal = itemView.findViewById(R.id.textViewGoal);
            textViewDeadline = itemView.findViewById(R.id.textViewDeadline);
            checkBoxGoal = itemView.findViewById(R.id.checkBoxGoal);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        void bind(Goal goal) {
            if (goal != null) {
                String text = goal.getText();
                textViewGoal.setText(text != null ? text : "");
                String deadline = "до " + dateFormat.format(new Date(goal.getDeadline()));
                textViewDeadline.setText(deadline);
                checkBoxGoal.setChecked(goal.isCompleted());
                checkBoxGoal.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (onGoalListener != null) {
                        onGoalListener.onGoalChecked(goal, isChecked);
                    }
                });

                buttonDelete.setOnClickListener(v -> {
                    if (onGoalListener != null) {
                        onGoalListener.onDeleteClick(goal);
                    }
                });
            } else {
                Log.e(TAG, "Attempted to bind null goal");
                textViewGoal.setText("");
                textViewDeadline.setText("");
                checkBoxGoal.setChecked(false);
                checkBoxGoal.setOnCheckedChangeListener(null);
                buttonDelete.setOnClickListener(null);
            }
        }
    }
} 