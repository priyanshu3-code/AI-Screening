package com.resumescreener.util;

import com.google.gson.*;
import com.resumescreener.model.ResumeExtractionResult;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ResumeExtractionResultDeserializer implements JsonDeserializer<ResumeExtractionResult> {

    @Override
    public ResumeExtractionResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        ResumeExtractionResult result = new ResumeExtractionResult();

        // Handle skills
        if (jsonObject.has("skills") && !jsonObject.get("skills").isJsonNull()) {
            result.setSkills(context.deserialize(jsonObject.get("skills"), List.class));
        }

        // Handle experience_years
        if (jsonObject.has("experience_years")) {
            result.setExperienceYears(jsonObject.get("experience_years").getAsInt());
        }

        // Handle education - can be string or array
        if (jsonObject.has("education") && !jsonObject.get("education").isJsonNull()) {
            JsonElement educationElement = jsonObject.get("education");
            if (educationElement.isJsonArray()) {
                JsonArray eduArray = educationElement.getAsJsonArray();
                if (eduArray.size() > 0 && eduArray.get(0).isJsonObject()) {
                    JsonObject firstEdu = eduArray.get(0).getAsJsonObject();
                    if (firstEdu.has("degree")) {
                        result.setEducation(firstEdu.get("degree").getAsString());
                    } else if (firstEdu.has("university")) {
                        result.setEducation(firstEdu.get("university").getAsString());
                    } else {
                        result.setEducation(firstEdu.toString());
                    }
                } else if (eduArray.size() > 0) {
                    result.setEducation(eduArray.get(0).getAsString());
                }
            } else if (educationElement.isJsonPrimitive()) {
                result.setEducation(educationElement.getAsString());
            }
        }

        // Handle achievements
        if (jsonObject.has("achievements") && !jsonObject.get("achievements").isJsonNull()) {
            result.setAchievements(context.deserialize(jsonObject.get("achievements"), List.class));
        }

        // Handle strengths
        if (jsonObject.has("strengths") && !jsonObject.get("strengths").isJsonNull()) {
            result.setStrengths(context.deserialize(jsonObject.get("strengths"), List.class));
        }

        // Handle missing_requirements
        if (jsonObject.has("missing_requirements") && !jsonObject.get("missing_requirements").isJsonNull()) {
            result.setMissingRequirements(context.deserialize(jsonObject.get("missing_requirements"), List.class));
        }

        // Handle tech_stack
        if (jsonObject.has("tech_stack") && !jsonObject.get("tech_stack").isJsonNull()) {
            result.setTechStack(context.deserialize(jsonObject.get("tech_stack"), List.class));
        }

        // Handle match_score
        if (jsonObject.has("match_score")) {
            result.setMatchScore(jsonObject.get("match_score").getAsInt());
        }

        // Handle confidence
        if (jsonObject.has("confidence")) {
            result.setConfidence(jsonObject.get("confidence").getAsDouble());
        }

        // Handle summary
        if (jsonObject.has("summary") && !jsonObject.get("summary").isJsonNull()) {
            result.setSummary(jsonObject.get("summary").getAsString());
        }

        return result;
    }
}
