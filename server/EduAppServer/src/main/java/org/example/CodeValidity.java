package org.example;

/**
 * @brief
 * Enumeracja definiująca predefiniowane czasy ważności dla kodów rejestracyjnych.
 * Każda wartość przechowuje swój tekstowy identyfikator i odpowiadającą mu liczbę godzin ważności.
 */
public enum CodeValidity {
    /**
     * Kod ważny przez jedną godzinę.
     */
    ONE_HOUR("1_HOUR", 1),
    /**
     * Kod ważny przez dwie godziny.
     */
    TWO_HOURS("2_HOURS", 2),
    /**
     * Kod ważny przez jeden dzień (24 godziny).
     */
    ONE_DAY("1_DAY", 24),
    /**
     * Kod ważny przez jeden tydzień (168 godzin).
     */
    ONE_WEEK("1_WEEK", 168);

    /**
     * Tekstowa reprezentacja wartości ważności.
     */
    private final String value;
    /**
     * Liczba godzin, przez które kod jest ważny.
     */
    private final int hours;

    /**
     * Konstruktor dla wartości enumeracji CodeValidity.
     * @param value Tekstowa reprezentacja wartości.
     * @param hours Liczba godzin ważności.
     */
    CodeValidity(String value, int hours) {
        this.value = value;
        this.hours = hours;
    }

    /**
     * Zwraca tekstową reprezentację wartości ważności.
     * @return Wartość tekstowa.
     */
    public String getValue() {
        return value;
    }

    /**
     * Zwraca liczbę godzin, przez które kod jest ważny.
     * @return Liczba godzin.
     */
    public int getHours() {
        return hours;
    }

    /**
     * Tworzy obiekt CodeValidity na podstawie podanej wartości tekstowej.
     * @param value Wartość tekstowa (np. "1_HOUR").
     * @return Odpowiadający obiekt CodeValidity.
     * @throws IllegalArgumentException Jeśli podana wartość nie odpowiada żadnej zdefiniowanej ważności.
     */
    public static CodeValidity fromValue(String value) {
        for (CodeValidity validity : values()) {
            if (validity.value.equalsIgnoreCase(value)) {
                return validity;
            }
        }
        throw new IllegalArgumentException("Invalid validity value: " + value);
    }
}