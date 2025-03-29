package solo.lev.lvl.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import solo.lev.lvl.R;
import solo.lev.lvl.adapters.GoalAdapter;
import solo.lev.lvl.models.Goal;
import android.content.SharedPreferences;
import android.content.Context;
import android.util.Log;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;
    
    private RecyclerView goalsRecyclerView;
    private GoalAdapter goalAdapter;
    private Button uploadPhotoButton;
    private Uri photoUri;
    private Goal selectedGoal;
    private SharedPreferences preferences;
    private Gson gson = new Gson();
    private Button addGoalButton;
    private List<Goal> goals;
    private static final String GOALS_KEY = "goals";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = requireContext().getSharedPreferences("GoalsPrefs", Context.MODE_PRIVATE);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        goalsRecyclerView = view.findViewById(R.id.goalsRecyclerView);
        uploadPhotoButton = view.findViewById(R.id.uploadPhotoButton);
        addGoalButton = view.findViewById(R.id.addGoalButton);
        goals = new ArrayList<>();
        
        setupRecyclerView();
        setupPhotoButton();
        loadGoals();

        if (addGoalButton != null) {
            addGoalButton.setOnClickListener(v -> {
                AddGoalFragment addGoalFragment = new AddGoalFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, addGoalFragment)
                    .addToBackStack(null)
                    .commit();
            });
        } else {
            Log.e(TAG, "Кнопка не найдена!");
        }
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setupRecyclerView() {
        goalsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        goalAdapter = new GoalAdapter(goals, new GoalAdapter.OnGoalListener() {
            @Override
            public void onDeleteClick(Goal goal) {
                goals.remove(goal);
                goalAdapter.notifyDataSetChanged();
                saveGoals();
                Toast.makeText(requireContext(), "Цель удалена", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGoalChecked(Goal goal, boolean isChecked) {
                goal.setCompleted(isChecked);
                saveGoals();
                String message = isChecked ? "Цель выполнена!" : "Цель отмечена как невыполненная";
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        goalsRecyclerView.setAdapter(goalAdapter);
    }

    private void setupPhotoButton() {
        if (uploadPhotoButton != null) {
            uploadPhotoButton.setVisibility(View.GONE); // Скрываем кнопку загрузки фото
        }
    }

    private void loadGoals() {
        String goalsJson = preferences.getString(GOALS_KEY, "[]");
        Type type = new TypeToken<List<Goal>>(){}.getType();
        goals = gson.fromJson(goalsJson, type);
        if (goals == null) {
            goals = new ArrayList<>();
        }
        if (goalAdapter != null) {
            goalAdapter.updateGoals(goals);
        }
        Log.d(TAG, "Загруженные цели: " + goalsJson);
    }

    private void saveGoals() {
        String goalsJson = gson.toJson(goals);
        preferences.edit().putString(GOALS_KEY, goalsJson).apply();
    }

    public void addGoal(Goal goal) {
        if (goals == null) {
            goals = new ArrayList<>();
        }
        goals.add(goal);
        if (goalAdapter != null) {
            goalAdapter.notifyItemInserted(goals.size() - 1);
        }
        saveGoals();
    }

    public static class ItemClickSupport {
        private final RecyclerView mRecyclerView;
        private OnItemClickListener mOnItemClickListener;
        private View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);
                    mOnItemClickListener.onItemClicked(mRecyclerView,
                            holder.getAdapterPosition(), v);
                }
            }
        };

        private RecyclerView.OnChildAttachStateChangeListener mAttachListener
                = new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                if (mOnItemClickListener != null) {
                    view.setOnClickListener(mOnClickListener);
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
            }
        };

        private ItemClickSupport(RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
            mRecyclerView.setTag(R.id.item_click_support, this);
            mRecyclerView.addOnChildAttachStateChangeListener(mAttachListener);
        }

        public static ItemClickSupport addTo(RecyclerView view) {
            ItemClickSupport support = (ItemClickSupport) view.getTag(R.id.item_click_support);
            if (support == null) {
                support = new ItemClickSupport(view);
            }
            return support;
        }

        public ItemClickSupport setOnItemClickListener(OnItemClickListener listener) {
            mOnItemClickListener = listener;
            return this;
        }

        public interface OnItemClickListener {
            void onItemClicked(RecyclerView recyclerView, int position, View v);
        }
    }
} 