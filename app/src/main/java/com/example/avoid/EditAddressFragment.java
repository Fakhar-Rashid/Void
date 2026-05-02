package com.example.avoid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.avoid.model.Address;
import com.example.avoid.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class EditAddressFragment extends Fragment {

    /** Live edit buffer — the user's saved addresses are only persisted on Save. */
    private final List<Address> draft = new ArrayList<>();
    /** Per-card edit mode: true means input fields are visible. */
    private final List<Boolean> editing = new ArrayList<>();

    private LinearLayout container;
    private MaterialButton addBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_address, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton back = view.findViewById(R.id.editAddressBack);
        back.setOnClickListener(v -> close());

        container = view.findViewById(R.id.editAddressContainer);
        addBtn = view.findViewById(R.id.editAddressAdd);
        MaterialButton save = view.findViewById(R.id.editAddressSave);

        User user = UserSession.getInstance().getCurrentUser();
        draft.clear();
        editing.clear();
        for (Address a : user.getAddresses()) {
            draft.add(copy(a));
            editing.add(false);
        }
        if (draft.isEmpty()) {
            draft.add(new Address());
            editing.add(true);
        }

        addBtn.setOnClickListener(v -> {
            syncDraftFromUI();
            if (draft.size() >= User.MAX_ADDRESSES) {
                Toast.makeText(requireContext(), "Maximum 3 addresses", Toast.LENGTH_SHORT).show();
                return;
            }
            draft.add(new Address());
            editing.add(true);
            renderForms();
        });

        save.setOnClickListener(v -> {
            syncDraftFromUI();
            List<Address> toSave = new ArrayList<>();
            for (Address a : draft) {
                if (!a.isComplete()) {
                    Toast.makeText(requireContext(),
                            "Please fill in all fields for every address (or delete it).",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                toSave.add(a);
            }
            user.setAddresses(toSave);
            save.setEnabled(false);
            UserRepository.getInstance().saveAddresses(user, new UserRepository.Callback<User>() {
                @Override public void onSuccess(User result) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Addresses saved", Toast.LENGTH_SHORT).show();
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

        renderForms();
    }

    private void renderForms() {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < draft.size(); i++) {
            Address addr = draft.get(i);
            boolean isEditing = editing.get(i);
            View row = inflater.inflate(R.layout.item_edit_address_form, container, false);

            TextView label = row.findViewById(R.id.addressFormLabel);
            ImageButton editBtn = row.findViewById(R.id.addressFormEdit);
            ImageButton delete = row.findViewById(R.id.addressFormDelete);
            TextView viewBody = row.findViewById(R.id.addressViewBody);
            View editContainer = row.findViewById(R.id.addressEditContainer);

            TextInputEditText house    = row.findViewById(R.id.addressFormHouse);
            TextInputEditText street   = row.findViewById(R.id.addressFormStreet);
            TextInputEditText area     = row.findViewById(R.id.addressFormArea);
            TextInputEditText province = row.findViewById(R.id.addressFormProvince);
            TextInputEditText country  = row.findViewById(R.id.addressFormCountry);

            label.setText("Address " + (i + 1));

            // Hydrate inputs from the draft (always — so toggling shows the right values).
            house.setText(addr.getHouseNumber());
            street.setText(addr.getStreetNumber());
            area.setText(addr.getArea());
            province.setText(addr.getProvince());
            country.setText(addr.getCountry() == null || addr.getCountry().isEmpty()
                    ? Address.DEFAULT_COUNTRY : addr.getCountry());

            // View body shows formatted address, or a placeholder for brand-new empty cards.
            if (addr.isComplete()) {
                viewBody.setText(addr.getMultiLine());
            } else {
                viewBody.setText("Tap edit to fill in this address.");
            }

            if (isEditing) {
                viewBody.setVisibility(View.GONE);
                editContainer.setVisibility(View.VISIBLE);
                editBtn.setImageResource(R.drawable.ic_check);
            } else {
                viewBody.setVisibility(View.VISIBLE);
                editContainer.setVisibility(View.GONE);
                editBtn.setImageResource(R.drawable.ic_edit);
            }

            final int index = i;

            editBtn.setOnClickListener(v -> {
                syncDraftFromUI();
                editing.set(index, !editing.get(index));
                renderForms();
            });

            delete.setOnClickListener(v -> {
                syncDraftFromUI();
                if (index < draft.size()) {
                    draft.remove(index);
                    editing.remove(index);
                }
                if (draft.isEmpty()) {
                    draft.add(new Address());
                    editing.add(true);
                }
                renderForms();
            });

            container.addView(row);
        }
        addBtn.setVisibility(draft.size() < User.MAX_ADDRESSES ? View.VISIBLE : View.GONE);
    }

    /** Pull current input-field values back into the draft list. Only edit-mode cards have inputs. */
    private void syncDraftFromUI() {
        for (int i = 0; i < container.getChildCount() && i < draft.size(); i++) {
            if (i >= editing.size() || !editing.get(i)) continue;
            View row = container.getChildAt(i);
            Address a = draft.get(i);
            a.setHouseNumber(text(row, R.id.addressFormHouse));
            a.setStreetNumber(text(row, R.id.addressFormStreet));
            a.setArea(text(row, R.id.addressFormArea));
            a.setProvince(text(row, R.id.addressFormProvince));
            a.setCountry(text(row, R.id.addressFormCountry));
        }
    }

    private static String text(View row, int id) {
        TextInputEditText input = row.findViewById(id);
        return input != null && input.getText() != null ? input.getText().toString().trim() : "";
    }

    private static Address copy(Address a) {
        return new Address(a.getHouseNumber(), a.getStreetNumber(), a.getArea(),
                a.getProvince(), a.getCountry());
    }

    private void close() {
        if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
    }
}
