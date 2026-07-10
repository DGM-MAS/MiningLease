package com.mas.gov.bt.mas.primary.utility;

import java.security.SecureRandom;

/**
 * Generates temporary passwords for auto-provisioned applicant accounts.
 * Mirrors mas-backend-masters' PasswordGenerator (duplicated here since
 * MiningLease is a separate deployable module with no shared dependency on masters).
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
        StringBuilder sb = new StringBuilder("MAS_");
        for (int i = 0; i < length - 7; i++) {
            int rndCharAt = random.nextInt(PASSWORD_ALLOW.length());
            char rndChar = PASSWORD_ALLOW.charAt(rndCharAt);
            sb.append(rndChar);
        }
        return sb.toString();
    }
}
