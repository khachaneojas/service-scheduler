package com.sprk.service.scheduler.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprk.commons.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting JSON strings to Java objects and vice versa using Jackson's ObjectMapper.
 */
@Component
@RequiredArgsConstructor
public class JsonConverter {

    private final ObjectMapper objectMapper;
    private static final String ERROR_GENERIC_MESSAGE = "Oops! Something went wrong.";

    /**
     * Converts the JSON string to an object of the specified target class.
     *
     * @param json        the JSON string to convert
     * @param targetClass the target class to convert to
     * @param <T>         the type of the target class
     * @return the converted object, or null if parsing fails
     * @throws IOException if an error occurs during JSON parsing
     */
    public <T> T convert(String json, Class<T> targetClass) throws IOException {
        return objectMapper.readValue(json, targetClass);
    }

    /**
     * Converts the JSON string to a list of objects of the specified target class.
     *
     * @param json        the JSON string to convert
     * @param targetClass the target class to convert to
     * @param <T>         the type of the target class
     * @return the converted list, or an empty list if parsing fails
     * @throws IOException if an error occurs during JSON parsing
     */
    public <T> List<T> convertToList(String json, Class<T> targetClass) throws IOException {
        // Define the TypeReference for the target class
        TypeReference<List<T>> typeReference = new TypeReference<>() {
            @Override
            public Type getType() {
                return objectMapper.getTypeFactory().constructCollectionType(List.class, targetClass);
            }
        };

        // Deserialize the JSON string into a list of objects using the defined TypeReference
        return objectMapper.readValue(json, typeReference);
    }

    public <K, V> Map<K, V> convertToMap(String json, Class<K> keyClass, Class<V> valueClass) throws IOException {
        // Define the TypeReference for the target class
        TypeReference<Map<K, V>> typeReference = new TypeReference<>() {
            @Override
            public Type getType() {
                return objectMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
            }
        };

        // Deserialize the JSON string into a list of objects using the defined TypeReference
        return objectMapper.readValue(json, typeReference);
    }

    /**
     * Converts a list of objects to a JSON array string using Jackson's ObjectMapper.
     *
     * @param list the list of objects to convert
     * @param <T>  the type of objects in the list
     * @return the JSON array string representing the list of objects
     * @throws JsonProcessingException if an error occurs during JSON processing
     */
    public <T> String convertListToJsonString(List<T> list) throws JsonProcessingException {
        return objectMapper.writeValueAsString(list);
    }

    public <K, V> String convertMapToJsonString(Map<K, V> map) throws JsonProcessingException {
        return objectMapper.writeValueAsString(map);
    }

    public <T> String getJsonStringFromList(List<T> list) {
        String str;
        try {
            str = convertListToJsonString(list);
        } catch (IOException e) {
            throw new InvalidDataException(ERROR_GENERIC_MESSAGE);
        }

        return str;
    }
}
