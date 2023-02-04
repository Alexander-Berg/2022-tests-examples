package ru.yandex.realty.adaptor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import ru.auto.test.api.realty.ApiPromo;
import ru.auto.test.api.realty.promocode.Constraints;
import ru.auto.test.api.realty.promocode.CreatePromoBody;
import ru.auto.test.api.realty.promocode.Feature;

import static java.util.Collections.singletonList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.yandex.realty.utils.RealtyUtils.getObjectFromJson;

/**
 * @author kurau (Yuri Kalinin)
 */
public class PromocodeAdaptor extends AbstractModule {

    @Inject
    private ApiPromo apiPromo;

    @Step("Создаём промокод {promoBody}")
    public void createPromocode(CreatePromoBody promoBody) {
        apiPromo.promocode().withDefaults()
                .withCreatePromoBody(promoBody)
                .post(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Применяем промокод «{promoName}»")
    public void applyPromocode(String promoName, String uid) {
        apiPromo.promocode()
                .codeName().withCodeName(promoName)
                .user().userId().withUserId(uid).withDefaults()
                .post(validatedWith(shouldBe200Ok()));
    }

    public static CreatePromoBody defaultPromo() {
        return getObjectFromJson(CreatePromoBody.class, "api/schemas/promo/create_promo_body.json")
                .withConstraints(promoConstrains())
                .withFeatures(singletonList(promoFeature()));
    }

    public static Feature promoFeature() {
        return new Feature()
                .withTag("rising")
                .withCount(3L)
                .withLifetime("3 days")
                .withPayload("region_id=56");
    }

    public static Constraints promoConstrains() {
        return new Constraints()
                .withDeadline("2024-01-15T00:00:00+03:00")
                .withTotalActivations(10000L)
                .withUserActivations(1L)
                .withBlacklist(singletonList(""));
    }

    @Override
    protected void configure() {
    }
}
