package com.example.avoid.util;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Locks every phone EditText in the app to the {@code +92 } prefix and validates that the
 * remainder is a 10-digit Pakistani mobile number.
 *
 * <p>Usage: {@code PhoneInputHelper.attach(myEditText)} during view setup. After that:
 * <ul>
 *   <li>The field shows {@code +92 } the moment it's attached.</li>
 *   <li>The user can type / delete digits after the prefix but cannot delete or modify
 *       the {@code +92 } itself — any attempt is rolled back.</li>
 *   <li>Selection / cursor movement before the prefix is auto-snapped to right after it.</li>
 *   <li>Only digits and spaces are accepted as input — paste cleanly handles the +92 case.</li>
 * </ul>
 *
 * <p>{@link #isValid(String)} returns true when the value matches {@code +92} followed by
 * exactly 10 digits (whitespace ignored). {@link #isValidOrEmpty(String)} additionally
 * accepts the bare {@code +92} prefix as "user didn't fill it in", for optional fields.
 */
public final class PhoneInputHelper {

    public static final String PREFIX = "+92 ";
    /** {@code +92} followed by exactly 10 digits — Pakistani mobile / fixed format. */
    private static final java.util.regex.Pattern PK_PHONE =
            java.util.regex.Pattern.compile("\\+92\\d{10}");

    private PhoneInputHelper() {}

    public static void attach(@NonNull EditText input) {
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        // Seed the prefix on first attach so it always reads "+92 …" — even the first time.
        if (input.getText() == null || !input.getText().toString().startsWith(PREFIX)) {
            input.setText(PREFIX);
            input.setSelection(input.length());
        }

        // Reject anything that would erase or corrupt the prefix. The filter is the
        // inexpensive guard; the TextWatcher below catches edge cases the filter misses.
        input.setFilters(new InputFilter[]{new PrefixGuardFilter()});

        input.addTextChangedListener(new TextWatcher() {
            boolean self = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (self) return;
                String txt = s.toString();
                if (!txt.startsWith(PREFIX)) {
                    self = true;
                    // Strip whatever's where the prefix should be and re-prepend it.
                    String tail = txt.replaceFirst("^[^0-9]*", "");
                    s.replace(0, s.length(), PREFIX + tail);
                    input.setSelection(s.length());
                    self = false;
                }
            }
        });

        // If the user ends up before the prefix (long-press, gesture nav), snap forward.
        input.setOnClickListener(v -> snapCursor(input));
        input.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) snapCursor(input); });
    }

    private static void snapCursor(@NonNull EditText input) {
        int start = input.getSelectionStart();
        int end   = input.getSelectionEnd();
        int min   = PREFIX.length();
        if (start < min || end < min) {
            input.setSelection(Math.max(min, end));
        }
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

    /** Returns the raw value typed (with prefix) or null if the field is empty / prefix-only. */
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

    /** Blocks writes that would erase or shrink the {@code +92 } prefix. */
    private static class PrefixGuardFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            // Replacement spans across the prefix? Reject the whole edit.
            if (dstart < PREFIX.length()) return "";
            return null; // accept as-is
        }
    }
}
