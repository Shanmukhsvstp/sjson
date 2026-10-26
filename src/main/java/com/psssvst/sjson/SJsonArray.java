package com.psssvst.sjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A JSON array. Mutable, not thread-safe once mutated.
 */
public final class SJsonArray {

    private final JsonNode node;
    private final String location;

    SJsonArray(JsonNode node) {
        this(node, "$");
    }

    SJsonArray(JsonNode node, String location) {
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException(
                    "Expected a JSON array"
            );
        }

        this.node = node;
        this.location = location;
    }

    public static SJsonArray create() {
        return new SJsonArray(
                JsonNodeFactory.instance.arrayNode()
        );
    }

    /*
     * Reading
     * */

    public int size() {
        return node.size();
    }

    public boolean isEmpty() {
        return node.isEmpty();
    }

    public SJson get(int index) {
        if (index < 0 || index >= node.size()) {
            throw new IndexOutOfBoundsException(
                    "Invalid JSON array index: " + index
                            + " (size " + node.size() + ")"
            );
        }

        return SJson.wrap(node.get(index), location + "[" + index + "]");
    }

    public List<SJson> asList() {
        List<SJson> result = new ArrayList<>();

        for (int i = 0; i < node.size(); i++) {
            result.add(SJson.wrap(node.get(i), location + "[" + i + "]"));
        }

        return result;
    }

    public List<String> asStrings() {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < node.size(); i++) {
            JsonNode element = node.get(i);

            if (!element.isTextual()) {
                throw new IllegalStateException(
                        "Expected a string at " + location + "[" + i + "], got: "
                                + element.getNodeType()
                );
            }
            result.add(element.textValue());
        }

        return result;
    }

    public List<Integer> asInts() {
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < node.size(); i++) {
            JsonNode element = node.get(i);

            if (!element.isIntegralNumber() || !element.canConvertToInt()) {
                throw new IllegalStateException(
                        "Expected an integer at " + location + "[" + i + "], got: "
                                + element.getNodeType()
                );
            }
            result.add(element.intValue());
        }

        return result;
    }

    public List<Boolean> asBooleans() {
        List<Boolean> result = new ArrayList<>();

        for (int i = 0; i < node.size(); i++) {
            JsonNode element = node.get(i);

            if (!element.isBoolean()) {
                throw new IllegalStateException(
                        "Expected a boolean at " + location + "[" + i + "], got: "
                                + element.getNodeType()
                );
            }
            result.add(element.booleanValue());
        }

        return result;
    }

    /*
     * Mutation
     * */

    private ArrayNode requireArray() {
        if (!(node instanceof ArrayNode)) {
            throw new IllegalStateException("Expected a JSON array");
        }
        return (ArrayNode) node;
    }

    public SJsonArray add(String value) {
        requireArray().add(value);
        return this;
    }

    public SJsonArray add(int value) {
        requireArray().add(value);
        return this;
    }

    public SJsonArray add(long value) {
        requireArray().add(value);
        return this;
    }

    public SJsonArray add(double value) {
        requireArray().add(value);
        return this;
    }

    public SJsonArray add(boolean value) {
        requireArray().add(value);
        return this;
    }

    // Stores a deep copy, so later changes to `value` don't affect this array.
    public SJsonArray add(SJson value) {
        if (value != null && value.rawNode().isMissingNode()) {
            throw new IllegalArgumentException("Cannot add a missing value");
        }

        requireArray().add(
                value == null
                        ? JsonNodeFactory.instance.nullNode()
                        : value.rawNode().deepCopy()
        );
        return this;
    }

    // Stores a deep copy, so later changes to `value` don't affect this array.
    public SJsonArray add(SJsonArray value) {
        requireArray().add(
                value == null
                        ? JsonNodeFactory.instance.nullNode()
                        : value.rawNode().deepCopy()
        );
        return this;
    }

    public SJsonArray addNull() {
        requireArray().addNull();
        return this;
    }

    public SJsonArray remove(int index) {
        if (index < 0 || index >= node.size()) {
            throw new IndexOutOfBoundsException(
                    "Invalid JSON array index: " + index
                            + " (size " + node.size() + ")"
            );
        }

        requireArray().remove(index);
        return this;
    }

    /*
     * Output
     * */

    public String toJson() {
        return node.toString();
    }

    @Override
    public String toString() {
        return toJson();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof SJsonArray && node.equals(((SJsonArray) other).node);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    // Internal access for composing JSON values
    JsonNode rawNode() {
        return node;
    }
}