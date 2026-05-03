package com.example.avoid.util;

import androidx.annotation.Nullable;

/**
 * Sign-up password rules:
 * <ul>
 *   <li>11–20 characters total</li>
 *   <li>≥ 8 digits</li>
 *   <li>≥ 1 uppercase letter</li>
 *   <li>≥ 1 lowercase letter</li>
 *   <li>≥ 1 special (non-alphanumeric, non-whitespace) character</li>
 * </ul>
 *
 * <p>Returns {@code null} when the password passes; otherwise a human-readable error string
 * suitable for the inline form error text.
 */
public final class PasswordValidator {

    public static final int MIN_LENGTH = 11;
    public static final int MAX_LENGTH = 20;
    public static final int MIN_DIGITS = 8;

    private PasswordValidator() {}

    @Nullable
    public static String validate(@Nullable String password) {
        if (password == null) password = "";
        int len = password.length();
        if (len < MIN_LENGTH) return "Password must be at least " + MIN_LENGTH + " characters.";
        if (len > MAX_LENGTH) return "Password must be at most "  + MAX_LENGTH + " characters.";

        int digits = 0, upper = 0, lower = 0, special = 0;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isDigit(c))                        digits++;
            else if (Character.isUpperCase(c))               upper++;
            else if (Character.isLowerCase(c))               lower++;
            else if (!Character.isWhitespace(c))             special++;
        }
        if (digits  < MIN_DIGITS) return "Password must contain at least " + MIN_DIGITS + " digits.";
        if (upper   < 1)          return "Password must contain at least one uppercase letter.";
        if (lower   < 1)          return "Password must contain at least one lowercase letter.";
        if (special < 1)          return "Password must contain at least one special character.";
        return null;
    }
}
