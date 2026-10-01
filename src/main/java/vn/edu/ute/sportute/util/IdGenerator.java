package vn.edu.ute.sportute.util;

import java.util.UUID;

public class IdGenerator {
    public static String generateId(String prefix) {
        int randomLength = 10 - prefix.length();
        String randomPart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, randomLength)
                .toUpperCase();
        return prefix + randomPart;
    }
}