package org.example;

public enum CodeValidity {
    ONE_HOUR("1_HOUR", 1),
    TWO_HOURS("2_HOURS", 2),
    ONE_DAY("1_DAY", 24),
    ONE_WEEK("1_WEEK", 168);

    private final String value;
    private final int hours;

    CodeValidity(String value, int hours) {
        this.value = value;
        this.hours = hours;
    }

    public String getValue() {
        return value;
    }

    public int getHours() {
        return hours;
    }

    public static CodeValidity fromValue(String value) {
        for (CodeValidity validity : values()) {
            if (validity.value.equalsIgnoreCase(value)) {
                return validity;
            }
        }
        throw new IllegalArgumentException("Invalid validity value: " + value);
    }
}