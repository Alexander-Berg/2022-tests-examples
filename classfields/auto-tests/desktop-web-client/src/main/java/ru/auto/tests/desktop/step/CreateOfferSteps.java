package ru.auto.tests.desktop.step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;

/**
 * @author kurau (Yuri Kalinin)
 */
public class CreateOfferSteps {

    private JsonObject offerTemplate;

    @Inject
    private PublicApiAdaptor adaptor;

    @Step
    public JsonObject getUserOffer(Account account, AutoApiOffer.CategoryEnum category, String offerId) {
        String session = adaptor.login(account).getSession().getId();
        return adaptor.getUserOffer(session, category, offerId);
    }

    @Step("Должны создать оффер по шаблону")
    public void shouldSeeSameOffer(JsonObject actualOffer, String expectedOffer) {
        assertThat("Не создался нужный оффер", new Gson().toJson(actualOffer),
                jsonEquals(expectedOffer).when(IGNORING_EXTRA_ARRAY_ITEMS).when(IGNORING_ARRAY_ORDER));
    }

    public CreateOfferSteps setOfferTemplate(String templatePath) {
        offerTemplate = new Gson().fromJson(getResourceAsString(templatePath), JsonObject.class);
        return this;
    }

    public String getOffer() {
        return new Gson().toJson(offerTemplate);
    }

    public static <T> T objectFromJson(Class<T> tClass, String path) {
        return new GsonBuilder().create().fromJson(getResourceAsString(path), tClass);
    }
}
