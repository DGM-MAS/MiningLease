package com.mas.gov.bt.mas.primary.utility;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates temporary passwords for auto-provisioned applicant accounts.
 * Mirrors mas-backend-masters' PasswordGenerator (duplicated here since
 * MiningLease is a separate deployable module with no shared dependency on masters).
 *
 * Guarantees at least one lowercase, one uppercase, one digit, and one special
 * character rather than leaving it to chance, so the result always satisfies
 * the same complexity rule enforced on user-chosen passwords (see masters'
 * StrongPasswordValidator) instead of occasionally missing a category.
 */
public class PasswordGenerator {
    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String SPECIAL = "@#$%!";
    private static final String PASSWORD_ALLOW =
            CHAR_LOWER + CHAR_UPPER + NUMBER + SPECIAL;
    private static final SecureRandom random = new SecureRandom();

    public static String generatePassword(int length) {
        int bodyLength = Math.max(length, 8) - 4;

        List<Character> chars = new ArrayList<>();
        chars.add(CHAR_LOWER.charAt(random.nextInt(CHAR_LOWER.length())));
        chars.add(CHAR_UPPER.charAt(random.nextInt(CHAR_UPPER.length())));
        chars.add(NUMBER.charAt(random.nextInt(NUMBER.length())));
        chars.add(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        for (int i = 0; i < bodyLength; i++) {
            chars.add(PASSWORD_ALLOW.charAt(random.nextInt(PASSWORD_ALLOW.length())));
        }

        Collections.shuffle(chars, random);

        StringBuilder sb = new StringBuilder("MAS_");
        chars.forEach(sb::append);
        return sb.toString();
    }
}
