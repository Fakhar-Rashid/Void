package com.example.avoid.util;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Locks every phone EditText in the app to the {@code +92 } prefix and validates that the
 * remainder is a 10-digit Pakistani mobile number.
 *
 * <p>Implementation: a single {@link TextWatcher} watches every change. If after the change
 * the text doesn't start with {@code +92 }, it pulls every digit out of the field, strips a
 * leading {@code 92} if present (to avoid duplicating the country code), and rewrites the
 * field as {@code +92 <digits>}. The cursor is parked at the end. No {@link android.text.InputFilter}
 * is used — filters can silently swallow {@code setText} calls (their {@code dstart=0} edit
 * looks identical to a user trying to delete the prefix), which is what was crashing the
 * Edit Profile screen when seeding an existing phone value.
 */
public final class PhoneInputHelper {

    public static final String PREFIX = "+92 ";
    /** {@code +92} followed by exactly 10 digits — Pakistani mobile / fixed format. */
    private static final java.util.regex.Pattern PK_PHONE =
            java.util.regex.Pattern.compile("\\+92\\d{10}");

    private PhoneInputHelper() {}

    public static void attach(@NonNull EditText input) {
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        if (input.getText() == null || !input.getText().toString().startsWith(PREFIX)) {
            input.setText(PREFIX);
            input.setSelection(input.length());
        }

        input.addTextChangedListener(new TextWatcher() {
            boolean self = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (self) return;
                String txt = s.toString();
                if (txt.startsWith(PREFIX)) return;
                self = true;
                String digits = txt.replaceAll("[^0-9]", "");
                if (digits.startsWith("92")) digits = digits.substring(2);
                s.replace(0, s.length(), PREFIX + digits);
                input.setSelection(s.length());
                self = false;
            }
        });

        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && input.getSelectionStart() < PREFIX.length()) {
                input.setSelection(input.length());
            }
        });
    }

    /** Strict — must be {@code +92} + 10 digits (whitespace ignored). */
    public static boolean isValid(@Nullable String text) {
        if (text == null) return false;
        return PK_PHONE.matcher(text.replaceAll("\\s", "")).matches();
    }

    /** Same as {@link #isValid} but accepts the bare prefix as "not filled in" — for
     *  optional phone fields like the seller onboarding contact phone. */
    public static boolean isValidOrEmpty(@Nullable String text) {
        if (text == null) return true;
        String compact = text.replaceAll("\\s", "");
        if (compact.isEmpty() || compact.equals("+92")) return true;
        return PK_PHONE.matcher(compact).matches();
    }

    /** Returns the compact value typed (with prefix, no spaces) or null when empty / prefix-only. */
    @Nullable
    public static String trimmedValueOrNull(@Nullable String text) {
        if (text == null) return null;
        String compact = text.replaceAll("\\s", "");
        if (compact.isEmpty() || compact.equals("+92")) return null;
        return compact;
    }

    /**
     * Prefills an EditText with an existing phone value, normalising it to the {@code +92 }
     * convention so the prefix-lock logic doesn't break on legacy data:
     * <ul>
     *   <li>{@code +923001234567}  → {@code +92 3001234567}</li>
     *   <li>{@code 03001234567}    → {@code +92 3001234567}</li>
     *   <li>{@code 3001234567}     → {@code +92 3001234567}</li>
     *   <li>{@code null / ""}      → {@code +92 } (just the prefix)</li>
     * </ul>
     */
    public static void setValue(@NonNull EditText input, @Nullable String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("92")) digits = digits.substring(2);
        else if (digits.startsWith("0")) digits = digits.substring(1);
        input.setText(PREFIX + digits);
        input.setSelection(input.length());
    }
}
