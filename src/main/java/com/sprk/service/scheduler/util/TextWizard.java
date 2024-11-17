package com.sprk.service.scheduler.util;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


@Component
public class TextWizard {

    public boolean isBlank(Collection<?> collection) {
        if (null == collection)
            return true;

        for (Object obj : collection) {
            if (obj != null) return false;
        }

        return true;
    }

    public boolean isBlank(String str) {
        if (null == str || str.trim().isEmpty())
            return true;

        int strLen = str.length();
        for (int i = 0; i < strLen; ++i) {
            if (!isAsciiWhitespace(str.charAt(i)))
                return false;
        }

        return true;
    }

    public boolean isNonBlank(String str) {
        return !isBlank(str);
    }

    public boolean isNonBlank(Collection<?> collection) {
        return !isBlank(collection);
    }

    public String buildFullName(String... nameFields) {
        if (null == nameFields || nameFields.length == 0)
            return null;

        return Arrays
                .stream(nameFields)
                .filter(str -> !isBlank(str))
                .collect(Collectors.joining(" "));
    }

    public boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

    public List<String> retainAllNonBlankStrings(
            Collection<String> collection
    ) {
        if (collection.isEmpty())
            return new ArrayList<>();

        if (collection.size() <= 1_000) {
            List<String> list = new ArrayList<>(collection.size());
            for (String str : collection) {
                if (isNonBlank(str)) list.add(str);
            }

            return list;
        }

        else if (collection.size() <= 10_000)
            return collection
                    .stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(this::isNonBlank)
                    .collect(Collectors.toList());
        else
            return collection
                    .parallelStream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(this::isNonBlank)
                    .collect(Collectors.toList());
    }

    public Collection<?> retainAllNonNull(
            Collection<?> collection
    ) {
        while (collection.remove(null));
        return collection;
    }

    public <T extends Enum<T>> T stringToEnum(
            Class<T> enumType,
            String str
    ) {
        try {
            return Enum.valueOf(enumType, str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public <T extends Enum<T>> String enumClassToString(
            Class<T> enumType
    ) {
        return EnumSet
                .allOf(enumType)
                .stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }


    public String concatenateStrings(
            String delimiter,
            String... strings
    ) {
        if (strings.length == 0)
            return null;

        return Arrays
                .stream(strings)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(this::isNonBlank)
                .collect(Collectors.joining(delimiter));
    }

    public String removeArrayBrackets(String inputString) {
        int startIndex = inputString.indexOf('[');
        int endIndex = inputString.indexOf(']');
        if (startIndex == -1 || endIndex == -1 || startIndex > endIndex)
            throw new IllegalArgumentException("Error: `inputString` may not contain array brackets in it.");

        return inputString.substring(startIndex + 1, endIndex);
    }

    public List<String> parseList(
            String inputString,
            String delimiter
    ) {
        String removedArrayBracketsIfExists = Optional
                .of(inputString)
                .map(str -> {
                    try {
                        return removeArrayBrackets(str);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .orElse(inputString);

        return Arrays
                .stream(removedArrayBracketsIfExists.split(delimiter))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(this::isNonBlank)
                .collect(Collectors.toList());
    }

//    public Map<String, String> parseJsonObjectString(
//            String jsonObjectAsString,
//            String[] jsonFields
//    ) {
//        JSONObject jsonObject = new JSONObject(jsonObjectAsString);
//        Map<String, String> fieldsMap = new LinkedHashMap<>(jsonFields.length);
//        for (String field : jsonFields) {
//            String fieldValue = jsonObject.optString(field, null);
//            if (isNonBlank(fieldValue)) fieldsMap.put(field, fieldValue.trim());
//        }
//
//        return fieldsMap.isEmpty() ? null : fieldsMap;
//    }




    private static final String[] ORDINAL_SUFFIXES = {"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
    private static final String LOCALIZATION_BUNDLE_NAME = "OrdinalLabels";
    private static final String CACHE_SEPARATOR = "-";

    /**
     * Converts a number to its ordinal form (e.g., 1st, 2nd, 3rd).
     *
     * @param number The input number.
     * @return The ordinal representation of the number.
     * @throws IllegalArgumentException if the input number is negative.
     */
    public String convertToOrdinal(int number) {
        validateInput(number);

        if (number < CACHED_ORDINALS.length)
            return CACHED_ORDINALS[number];

        return computeOrdinal(number);
    }

    /**
     * Converts a number to its ordinal form with localization support.
     *
     * @param number The input number.
     * @param locale The locale for localization.
     * @return The localized ordinal representation of the number.
     * @throws IllegalArgumentException if the input number is negative.
     */
    public String convertToOrdinal(int number, Locale locale) {
        validateInput(number);

        String key = Integer.toString(number);
        ResourceBundle bundle = ResourceBundle.getBundle(LOCALIZATION_BUNDLE_NAME, locale);
        String suffix = bundle.getString(key);

        return number + suffix;
    }



    private void validateInput(int number) {
        if (number < 0)
            throw new IllegalArgumentException("Input number must be non-negative.");
    }

    private static final String[] CACHED_ORDINALS = new String[40];

    static {
        for (int i = 0; i < CACHED_ORDINALS.length; i++) {
            CACHED_ORDINALS[i] = computeOrdinal(i);
        }
    }

    private static String computeOrdinal(int number) {
        int lastDigit = number % 10;
        return number + ORDINAL_SUFFIXES[lastDigit];
    }

    private boolean isAsciiWhitespace(char ch) {
        return ch == 32 || ch == 9 || ch == 10 || ch == 12 || ch == 13;
    }

}
