package ru.auto.tests.realtyapi.matcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.assertj.core.api.Assertions;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UniqueIds extends TypeSafeMatcher<JsonArray> {

    private String element;
    private static final String DESCRIPTION = "Все офферы должны иметь уникальный id";

    private UniqueIds(String element) {
        this.element = element;
    }

    @Override
    protected boolean matchesSafely(JsonArray offers) {
        List<String> idList = new ArrayList<>();
        Set<String> uniques = new HashSet<>();

        for (JsonElement offer : offers) {
            String id = offer.getAsJsonObject().get(element).getAsString();
            idList.add(id);
            uniques.add(id);
        }

        Assertions.assertThat(idList.toArray()).describedAs(DESCRIPTION)
                .containsOnlyOnce(uniques.toArray());

        return idList.size() == uniques.size();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(DESCRIPTION);
    }

    public static TypeSafeMatcher<JsonArray> hasUniqueIds(String element) {
        return new UniqueIds(element);
    }
}
