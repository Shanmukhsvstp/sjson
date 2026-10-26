package com.psssvst.sjson;

public class Main {
    public static void main(String[] args) {
        SJson json = SJson.parse("""
            {
              "name": "Rahul",
              "age": 22,
              "skills": ["Java", "Python", "SQL"],
              "address": {
                "city": "Delhi",
                "country": "India"
              }
            }
            """);

        System.out.println(json.path("address.city").asString());
        // Delhi

        System.out.println(json.path("skills[0]").asString());
        // Java

//        System.out.println(json.getOrDefault("age", 18).asInt());
        // 22

        System.out.println(
                json.path("profile.contact.email").asStringOrNull()
        );
        // null

//        json.get("skills").add("Rust");

        System.out.println(json.get("skills").size());
        // 4

        System.out.println(json.toJson());
        // Includes Rust in the skills array

        System.out.println(json.get("skills").asArray().get(0).asString());

        SJsonArray sJsonArray = SJsonArray.create().add("1").add("2").add("3");
        SJsonArray sJsonArray1 = SJsonArray.create().add("2").add(true).add(2).addNull();
        SJson sJson = SJson.create().set("d", sJsonArray).set("w", 2);
        sJson.set("w", 5);
        sJson.set("idkk",sJsonArray1);
        System.out.println(sJson.toJson());




    }
}