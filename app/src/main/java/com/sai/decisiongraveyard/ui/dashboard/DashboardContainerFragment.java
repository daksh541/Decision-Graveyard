package com.sai.decisiongraveyard.ui.dashboard;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sai.decisiongraveyard.R;

public class DashboardContainerFragment extends Fragment {

    public DashboardContainerFragment() {
        super(R.layout.fragment_dashboard_container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.dashboardTabContainer, new DashboardFragment())
                    .commit();
        }
    }
}
