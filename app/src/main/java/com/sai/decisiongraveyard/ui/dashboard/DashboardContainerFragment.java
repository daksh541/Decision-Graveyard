package com.sai.decisiongraveyard.ui.dashboard;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.ui.profile.ProfileFragment;

public class DashboardContainerFragment extends Fragment {

    private static final String KEY_SELECTED_TAB = "selected_tab";

    private TabLayout tabLayout;
    private int selectedTabIndex = 0;

    public DashboardContainerFragment() {
        super(R.layout.fragment_dashboard_container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            selectedTabIndex = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
        }

        tabLayout = view.findViewById(R.id.tabLayoutDashboard);
        setupTabs();

        if (savedInstanceState == null) {
            showTab(selectedTabIndex);
        } else {
            TabLayout.Tab tab = tabLayout.getTabAt(selectedTabIndex);
            if (tab != null) {
                tab.select();
            }
        }
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabIndex = tab.getPosition();
                showTab(selectedTabIndex);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        TabLayout.Tab initialTab = tabLayout.getTabAt(selectedTabIndex);
        if (initialTab != null) {
            initialTab.select();
        }
    }

    private void showTab(int index) {
        Fragment fragment = index == 1 ? new ProfileFragment() : new DashboardFragment();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.dashboardTabContainer, fragment)
                .commit();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, selectedTabIndex);
    }
}
