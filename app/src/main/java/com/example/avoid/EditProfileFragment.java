package com.example.avoid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.avoid.model.User;
import com.example.avoid.util.PhoneInputHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton back = view.findViewById(R.id.editProfileBack);
        back.setOnClickListener(v -> close());

        TextInputEditText nameInput  = view.findViewById(R.id.editProfileName);
        TextInputEditText phoneInput = view.findViewById(R.id.editProfilePhone);

        User user = UserSession.getInstance().getCurrentUser();
        nameInput.setText(user.getName());
        PhoneInputHelper.attach(phoneInput);
        PhoneInputHelper.setValue(phoneInput, user.getPhone());

        MaterialButton save = view.findViewById(R.id.editProfileSave);
        save.setOnClickListener(v -> {
            String name = text(nameInput);
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            String phoneRaw = text(phoneInput);
            if (!PhoneInputHelper.isValidOrEmpty(phoneRaw)) {
                Toast.makeText(requireContext(),
                        "Phone must be +92 followed by 10 digits.", Toast.LENGTH_LONG).show();
                return;
            }
            user.setName(name);
            user.setPhone(PhoneInputHelper.trimmedValueOrNull(phoneRaw));
            save.setEnabled(false);
            UserRepository.getInstance().saveProfile(user, new UserRepository.Callback<User>() {
                @Override public void onSuccess(User result) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                    close();
                }
                @Override public void onFailure(@NonNull Exception e) {
                    if (!isAdded()) return;
                    save.setEnabled(true);
                    Toast.makeText(requireContext(),
                            e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Save failed",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private static String text(TextInputEditText input) {
        return input != null && input.getText() != null ? input.getText().toString().trim() : "";
    }

    private void close() {
        if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
    }
}
