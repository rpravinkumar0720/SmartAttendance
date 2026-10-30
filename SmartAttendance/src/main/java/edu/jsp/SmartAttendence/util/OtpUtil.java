package edu.jsp.SmartAttendence.util;

import java.security.SecureRandom;

public class OtpUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpUtil() {
    }

    /**
     * Generates a secure random 6-digit OTP as a String, e.g. "042951".
     */
    public static String generateOtp() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }
}
