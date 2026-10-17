package com.ticketrush.service;

import java.util.regex.Pattern;

public final class VisitorIds {
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    private VisitorIds() {}

    public static boolean isValid(String id) {
        return id != null && VALID.matcher(id).matches();
    }
}
