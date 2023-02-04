package ru.auto.tests.realtyapi.matcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.ProvidedBy;
import org.assertj.core.api.Assertions;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import ru.auto.tests.realtyapi.provider.RealtyApiV1ProdProvider;
import ru.auto.tests.realtyapi.v1.ApiClient;

import java.util.function.Function;

import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;

@ProvidedBy(RealtyApiV1ProdProvider.class)
public class Sites extends TypeSafeMatcher<JsonElement> {

    private ApiClient api;

    private static final String DESCRIPTION = "Все офферы должны иметь уникальный id";

    private Sites(ApiClient api) {
        this.api = api;
    }

    @Override
    protected boolean matchesSafely(JsonElement site) {
        String siteId = site.getAsJsonObject().get("id").getAsString();
        String siteIdFromCard = api.search().siteWithOffersStatRoute().reqSpec(authSpec())
                .siteIdQuery(siteId)
                .execute(Function.identity())
                .as(JsonObject.class)
                .getAsJsonObject("response")
                .getAsJsonObject("site")
                .get("id").getAsString();

        Assertions.assertThat(siteId).describedAs(DESCRIPTION)
                .isEqualTo(siteIdFromCard);
        return siteId.equals(siteIdFromCard);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(DESCRIPTION);
    }

    public static TypeSafeMatcher<JsonElement> isSite(ApiClient api) {
        return new Sites(api);
    }
}
