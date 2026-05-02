package com.example.avoid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
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

/**
 * No bottom Save button — each card commits itself when the user taps the tick icon.
 * Delete also commits immediately. The fragment leaves the user's saved address list
 * in sync with what they see on screen.
 */
public class EditAddressFragment extends Fragment {

    /** Live edit buffer — copy of the user's saved addresses, mutated as the user edits. */
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
            AutoCompleteTextView province = row.findViewById(R.id.addressFormProvince);
            TextInputEditText country  = row.findViewById(R.id.addressFormCountry);

            label.setText("Address " + (i + 1));

            province.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, Address.PAKISTAN_PROVINCES));

            house.setText(addr.getHouseNumber());
            street.setText(addr.getStreetNumber());
            area.setText(addr.getArea());
            province.setText(addr.getProvince(), false);
            country.setText(Address.DEFAULT_COUNTRY);

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

            editBtn.setOnClickListener(v -> onEditButtonTap(index));

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
                persist();
            });

            container.addView(row);
        }
        addBtn.setVisibility(draft.size() < User.MAX_ADDRESSES ? View.VISIBLE : View.GONE);
    }

    /**
     * Pencil → enter edit mode (no save).
     * Tick (currently editing): if the card is blank, discard it; if incomplete, toast and stay in
     * edit mode; otherwise exit edit mode and persist the full list to Firestore.
     */
    private void onEditButtonTap(int index) {
        if (index >= draft.size()) return;

        boolean wasEditing = editing.get(index);
        if (!wasEditing) {
            editing.set(index, true);
            renderForms();
            return;
        }

        syncDraftFromUI();
        Address current = draft.get(index);
        if (current.isBlank()) {
            draft.remove(index);
            editing.remove(index);
            if (draft.isEmpty()) {
                draft.add(new Address());
                editing.add(true);
            }
            renderForms();
            persist();
            return;
        }
        if (!current.isComplete()) {
            Toast.makeText(requireContext(),
                    "Please fill in all fields, or delete this address.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        editing.set(index, false);
        renderForms();
        persist();
    }

    /** Push the current draft (excluding any blank placeholder cards) to Firestore. */
    private void persist() {
        User user = UserSession.getInstance().getCurrentUser();
        List<Address> toSave = new ArrayList<>();
        for (Address a : draft) {
            if (a.isComplete()) toSave.add(a);
        }
        user.setAddresses(toSave);
        UserRepository.getInstance().saveAddresses(user, new UserRepository.Callback<User>() {
            @Override public void onSuccess(User result) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Address saved", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(@NonNull Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Save failed",
                        Toast.LENGTH_SHORT).show();
            }
        });
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
            a.setCountry(Address.DEFAULT_COUNTRY);
        }
    }

    private static String text(View row, int id) {
        EditText input = row.findViewById(id);
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
