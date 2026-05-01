package com.example.avoid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.avoid.model.Settings;
import com.example.avoid.model.User;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton back = view.findViewById(R.id.settingsBackButton);
        back.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) return;
        Settings settings = user.getSettings();

        bindSwitch(view, R.id.switchNotifications,      settings.isNotificationsEnabled(),
                settings::setNotificationsEnabled);
        bindSwitch(view, R.id.switchEmailNotifications, settings.isEmailNotifications(),
                settings::setEmailNotifications);
        bindSwitch(view, R.id.switchPushNotifications,  settings.isPushNotifications(),
                settings::setPushNotifications);
        bindSwitch(view, R.id.switchOrderUpdates,       settings.isOrderUpdates(),
                settings::setOrderUpdates);
        bindSwitch(view, R.id.switchPromotional,        settings.isPromotionalAlerts(),
                settings::setPromotionalAlerts);
        bindSwitch(view, R.id.switchDarkMode,           settings.isDarkMode(),
                settings::setDarkMode);

        ((TextView) view.findViewById(R.id.settingsLanguageValue)).setText(settings.getLanguage());
        ((TextView) view.findViewById(R.id.settingsCurrencyValue)).setText(settings.getCurrency());
    }

    private interface BoolSetter { void set(boolean value); }

    private void bindSwitch(View root, int switchId, boolean initial, BoolSetter setter) {
        MaterialSwitch sw = root.findViewById(switchId);
        sw.setChecked(initial);
        sw.setOnCheckedChangeListener((button, checked) -> {
            setter.set(checked);
            UserRepository.getInstance().saveSettingsForCurrentUser();
        });
    }
}
