package com.evo.evo_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class BusinessInsightService {

    // =========================
    // 🚀 BUSINESS TYPE (SMART)
    // =========================
    public String detectBusinessType(Document doc) {

        // 1️⃣ JSON-LD (STRONGEST SIGNAL)
        for (Element script : doc.select("script[type=application/ld+json]")) {
            try {
                String jsonStr = script.html().trim();

                if (jsonStr.startsWith("[")) {
                    JSONArray arr = new JSONArray(jsonStr);

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);

                        String type = obj.optString("@type", "").toLowerCase();
                        String mapped = mapSchemaType(type);

                        if (!mapped.equals("business")) return mapped;
                    }

                } else {
                    JSONObject obj = new JSONObject(jsonStr);

                    String type = obj.optString("@type", "").toLowerCase();
                    String mapped = mapSchemaType(type);

                    if (!mapped.equals("business")) return mapped;
                }

            } catch (Exception ignored) {}
        }

        // 2️⃣ META + TITLE
        String meta = (
                doc.title() + " " +
                doc.select("meta[name=description]").attr("content")
        ).toLowerCase();

        if (containsAny(meta, "hospital", "clinic", "doctor"))
            return "hospital";

        if (containsAny(meta, "restaurant", "food", "menu"))
            return "restaurant";

        if (containsAny(meta, "software", "platform", "saas"))
            return "software company";

        if (containsAny(meta, "shop", "store", "buy", "cart"))
            return "store";

        // 3️⃣ FULL TEXT FALLBACK
        String text = doc.text().toLowerCase();

        if (containsAny(text, "hospital", "clinic")) return "hospital";
        if (containsAny(text, "restaurant", "food")) return "restaurant";
        if (containsAny(text, "software", "platform")) return "software company";
        if (containsAny(text, "shop", "store")) return "store";

        return "business"; // 🔥 final fallback
    }

    // =========================
    // 🔁 SCHEMA MAPPING
    // =========================
    private String mapSchemaType(String type) {

        if (type.contains("hospital") || type.contains("medical"))
            return "hospital";

        if (type.contains("restaurant"))
            return "restaurant";

        if (type.contains("product") || type.contains("store"))
            return "store";

        if (type.contains("software") || type.contains("organization"))
            return "software company";

        return "business";
    }

    // =========================
    // 📍 LOCATION (SMART)
    // =========================
    public String detectLocation(Document doc) {

        // 1️⃣ JSON-LD
        for (Element script : doc.select("script[type=application/ld+json]")) {
            try {
                JSONObject obj = new JSONObject(script.html());

                if (obj.has("address")) {
                    JSONObject addr = obj.getJSONObject("address");

                    String city = addr.optString("addressLocality");
                    String country = addr.optString("addressCountry");

                    if (!city.isEmpty() && !country.isEmpty())
                        return city + ", " + country;

                    if (!city.isEmpty())
                        return city;

                    if (!country.isEmpty())
                        return country;
                }

            } catch (Exception ignored) {}
        }

        // 2️⃣ TEXT DETECTION
        String text = doc.text().toLowerCase();

        if (text.contains("chennai")) return "Chennai";
        if (text.contains("bangalore")) return "Bangalore";
        if (text.contains("mumbai")) return "Mumbai";
        if (text.contains("coimbatore")) return "Coimbatore";
        if (text.contains("delhi")) return "Delhi";

        if (text.contains("india")) return "India";

        return "India"; // 🔥 safe fallback
    }

    // =========================
    // 🔥 SEARCH KEYWORD
    // =========================
    public String buildSearchKeyword(String businessType) {

        switch (businessType) {
            case "hospital":
                return "hospital";

            case "restaurant":
                return "restaurant";

            case "software company":
                return "software company";

            case "store":
                return "shopping store";

            default:
                return "company";
        }
    }

    // =========================
    // 🚀 GROWTH IDEAS
    // =========================
    public List<String> generateGrowthIdeas(String type, int score) {

        List<String> ideas = new ArrayList<>();

        // 🔥 BASE
        ideas.add("Create and optimize your Google Business Profile");
        ideas.add("Improve customer reviews and ratings");

        // 🔥 SCORE BASED
        if (score < 50) {
            ideas.add("Fix SEO basics before running ads");
            ideas.add("Improve website content depth and structure");
        }

        // 🔥 TYPE BASED
        switch (type) {

            case "hospital":
                ideas.add("Get listed on healthcare platforms like Practo");
                ideas.add("Publish health blogs to build authority");
                ideas.add("Show doctor profiles and patient testimonials");
                break;

            case "software company":
                ideas.add("Write blogs targeting problem-solving keywords");
                ideas.add("Create demo/product videos");
                ideas.add("Build landing pages for each service");
                break;

            case "store":
                ideas.add("Optimize product pages with SEO keywords");
                ideas.add("Use Instagram reels for product discovery");
                ideas.add("Run targeted ads on Google & Meta");
                break;

            case "restaurant":
                ideas.add("List your business on Zomato and Swiggy");
                ideas.add("Upload menu, photos, and offers regularly");
                ideas.add("Encourage customer reviews");
                break;
        }

        return ideas;
    }

    // =========================
    // 🔧 HELPER
    // =========================
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}