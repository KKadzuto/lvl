package solo.lev.lvl.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import solo.lev.lvl.MainActivity;
import solo.lev.lvl.R;
import solo.lev.lvl.models.Goal;
import solo.lev.lvl.services.ModerationServ;

public class AddGoalFragment extends Fragment {
    private TextInputEditText titleEditText;
    private TextInputEditText descriptionEditText;
    private TimePicker timePicker;
    private Button addGoalButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_goal, container, false);
        titleEditText = view.findViewById(R.id.titleEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        timePicker = view.findViewById(R.id.timePicker);
        addGoalButton = view.findViewById(R.id.addGoalButton);
        addGoalButton.setOnClickListener(v -> validateGoal());
        return view;
    }

    private void validateGoal() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (title.isEmpty()) {
            titleEditText.setError("Введите название цели");
            return;
        }
        if (description.isEmpty()) {
            descriptionEditText.setError("Введите описание цели");
            return;
        }

        ModerationServ.moderateGoal(title, description, new ModerationServ.ModerationCallback() {
            @Override
            public void onSuccess(boolean isAppropriate, String feedback) {
                if (isAppropriate) {
                    saveGoal(title, description);
                } else {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Проверка не пройдена: " + feedback, Toast.LENGTH_LONG).show()
                    );
                }
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Ошибка модерации: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void saveGoal(String title, String description) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
        calendar.set(Calendar.MINUTE, timePicker.getMinute());
        Goal newGoal = new Goal(title, description, calendar.getTimeInMillis());
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.addGoal(newGoal);
            getParentFragmentManager().popBackStack();
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Цель добавлена", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
