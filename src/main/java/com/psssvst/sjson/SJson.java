package com.psssvst.sjson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A small wrapper around a Jackson tree.
 *
 * <p>Navigation ({@link #get}, {@link #path}) never throws. A missing key just gives you
 * a "missing" value, and the typed accessors ({@code asString}, {@code asInt}, ...) are
 * where errors surface, with the failing location in the message.
 *
 * <p>Instances are mutable, and values returned by {@link #get} and {@link #path} are
 * views into the same tree, so mutating them mutates the parent. Not thread-safe once
 * mutated.
 */
public final class SJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode node;
    private final String location;

    private SJson(JsonNode node, String location) {
        this.node = node;
        this.location = location;
    }

    /*
     * Parsing and creation
     * */

    public static SJson parse(String json) {
        Objects.requireNonNull(json, "json");

        try {
            JsonNode root = MAPPER.readTree(json);

            // Jackson returns a MissingNode for empty input instead of throwing.
            if (root == null || root.isMissingNode()) {
                throw new IllegalArgumentException("Invalid JSON: input is empty");
            }

            return new SJson(root, "$");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Invalid JSON: " + e.getOriginalMessage(), e
            );
        }
    }

    public static SJson create() {
        return new SJson(JsonNodeFactory.instance.objectNode(), "$");
    }

    /*
     * Navigation
     * */

    // Access a direct child of an object.
    public SJson get(String key) {
        return new SJson(node.path(key), child(location, key));
    }

    // Access an element of an array.
    public SJson get(int index) {
        return new SJson(node.path(index), location + "[" + index + "]");
    }

    // Navigate nested objects and arrays.
    // Examples: address.city, skills[0], profile.contact.email
    // Limitation: keys that contain dots or brackets can't be addressed here, use get().
    public SJson path(String path) {
        JsonNode current = node;

        String normalized = path.replaceAll(
                "\\[(\\d+)]", ".$1"
        );

        for (String part : normalized.split("\\.")) {
            if (part.isEmpty()) {
                continue;
            }

            if (current.isArray() && part.matches("\\d+")) {
                try {
                    current = current.path(Integer.parseInt(part));
                } catch (NumberFormatException e) {
                    // Digits only, so this is an int overflow like 99999999999.
                    return new SJson(MissingNode.getInstance(), child(location, path));
                }
            } else {
                current = current.path(part);
            }
        }

        return new SJson(current, child(location, path));
    }

    /*
     * State checks
     * */

    public boolean has(String key) {
        return node.has(key);
    }

    public boolean isMissing() {
        return node.isMissingNode();
    }

    public boolean isNull() {
        return node.isNull();
    }

    public boolean isObject() {
        return node.isObject();
    }

    public boolean isArray() {
        return node.isArray();
    }

    // Property names of an object, in document order.
    public List<String> keys() {
        if (!node.isObject()) {
            throw typeError("an object");
        }

        List<String> result = new ArrayList<>();
        node.fieldNames().forEachRemaining(result::add);
        return result;
    }

    // Number of elements in an array or properties in an object.
    public int size() {
        return node.size();
    }

    /*
     * Typed value accessors
     * The no-arg versions throw if the value is the wrong type (or missing/null).
     * The fallback versions return the fallback when the value is missing or JSON null,
     * but still throw if the value exists with the wrong type.
     * */

    public String asString() {
        if (!node.isTextual()) {
            throw typeError("a string");
        }

        return node.textValue();
    }

    public String asString(String fallback) {
        return isAbsent() ? fallback : asString();
    }

    public String asStringOrNull() {
        return asString(null);
    }

    public int asInt() {
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            throw typeError("an integer");
        }

        return node.intValue();
    }

    public int asInt(int fallback) {
        return isAbsent() ? fallback : asInt();
    }

    public long asLong() {
        if (!node.isIntegralNumber() || !node.canConvertToLong()) {
            throw typeError("a long");
        }

        return node.longValue();
    }

    public long asLong(long fallback) {
        return isAbsent() ? fallback : asLong();
    }

    public double asDouble() {
        if (!node.isNumber()) {
            throw typeError("a number");
        }

        return node.doubleValue();
    }

    public double asDouble(double fallback) {
        return isAbsent() ? fallback : asDouble();
    }

    public boolean asBoolean() {
        if (!node.isBoolean()) {
            throw typeError("a boolean");
        }

        return node.booleanValue();
    }

    public boolean asBoolean(boolean fallback) {
        return isAbsent() ? fallback : asBoolean();
    }

    public SJsonArray asArray() {
        if (!node.isArray()) {
            throw typeError("an array");
        }

        return new SJsonArray(node, location);
    }

    /*
     * Mutation Methods
     * */

    private ObjectNode requireObject() {
        if (!(node instanceof ObjectNode object)) {
            throw new IllegalStateException(
                    "Cannot set a property on a non-object JSON value at " + location
            );
        }
        return object;
    }

    public SJson set(String key, String value) {
        requireObject().put(key, value);
        return this;
    }

    public SJson set(String key, int value) {
        requireObject().put(key, value);
        return this;
    }

    public SJson set(String key, long value) {
        requireObject().put(key, value);
        return this;
    }

    public SJson set(String key, double value) {
        requireObject().put(key, value);
        return this;
    }

    public SJson set(String key, boolean value) {
        requireObject().put(key, value);
        return this;
    }

    // Stores a deep copy, so later changes to `value` don't affect this tree.
    public SJson set(String key, SJson value) {
        requireObject().set(key, copyOf(value));
        return this;
    }

    // Stores a deep copy, so later changes to `value` don't affect this tree.
    public SJson set(String key, SJsonArray value) {
        requireObject().set(
                key,
                value == null
                        ? JsonNodeFactory.instance.nullNode()
                        : value.rawNode().deepCopy()
        );
        return this;
    }

    public SJson setNull(String key) {
        requireObject().putNull(key);
        return this;
    }

    public SJson remove(String key) {
        requireObject().remove(key);
        return this;
    }

    /*
     * Output
     * */

    // Serialize this value back to JSON.
    public String toJson() {
        return node.toString();
    }

    public String toPrettyJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    @Override
    public String toString() {
        return toJson();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SJson that && node.equals(that.node);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    /*
     * Internals
     * */

    // Internal access for composing JSON values
    JsonNode rawNode() {
        return node;
    }

    static SJson wrap(JsonNode node, String location) {
        return new SJson(node, location);
    }

    private boolean isAbsent() {
        return node.isMissingNode() || node.isNull();
    }

    private IllegalStateException typeError(String expected) {
        return new IllegalStateException(
                "Expected " + expected + " at " + location
                        + ", got: " + node.getNodeType()
        );
    }

    private static JsonNode copyOf(SJson value) {
        if (value == null) {
            return JsonNodeFactory.instance.nullNode();
        }

        if (value.node.isMissingNode()) {
            throw new IllegalArgumentException(
                    "Cannot set a missing value (from " + value.location + ")"
            );
        }

        return value.node.deepCopy();
    }

    private static String child(String parent, String segment) {
        return segment.startsWith("[")
                ? parent + segment
                : parent + "." + segment;
    }
}
