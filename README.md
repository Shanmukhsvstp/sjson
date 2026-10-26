# SJson

A small wrapper around Jackson for reading, navigating and building JSON with a cleaner syntax.

Requires Java 17+.

> Click [here](https://sjson.psssvst.com) for the complete documentation 

## Install

Gradle:

```kotlin
implementation("com.psssvst:sjson:0.1.0")
```

Maven:

```xml
<dependency>
  <groupId>com.psssvst</groupId>
  <artifactId>sjson</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Why SJson?

Plain Jackson:

```java
JsonNode root = new ObjectMapper().readTree(json);
String city = root.path("address").path("city").asText(null);
int age = root.path("age").asInt(0); // returns 0 for "abc"
```

SJson:

```java
SJson root = SJson.parse(json);
String city = root.path("address.city").asString("unknown");
int age = root.get("age").asInt(0); // throws if "age" is not a number
```

## Reading

```java
SJson json = SJson.parse("""
        {"name": "Ada", "skills": ["java", "go"], "address": {"city": "London"}}
        """);

json.get("name").asString();            // "Ada"
json.path("address.city").asString();   // "London"
json.path("skills[1]").asString();      // "go"
json.get("skills").asArray().asStrings(); // ["java", "go"]
json.get("nope").isMissing();           // true
json.get("nope").asInt(42);             // 42
json.keys();                            // ["name", "skills", "address"]
```

Navigation (`get`, `path`) never throws. A missing key gives you a missing value, and the typed
accessors are where errors show up, with the location in the message:

```
Expected a string at $.address.zip, got: MISSING
```

Accessors with a fallback (`asInt(0)`, `asString("x")`) return it for missing or `null` values,
but still throw if the value exists with a different or wrong type.

## Building

```java
SJson json = SJson.create()
        .set("name", "Ada")
        .set("age", 36)
        .set("skills", SJsonArray.create().add("java").add("go"))
        .set("address", SJson.create().set("city", "London"));

json.toJson();       // {"name":"Ada","age":36,"skills":["java","go"],"address":{"city":"London"}}
json.toPrettyJson(); // indented
```

## Things to know

- Instances are mutable. `get` and `path` return views into the same tree, so
  `root.get("a").set("b", 1)` changes `root`.
- `set(key, SJson)` and `add(SJson)` store a deep copy.
- Not thread-safe once mutated.
- `path` splits on dots and brackets. For keys that contain those, use `get`.

## License

MIT
