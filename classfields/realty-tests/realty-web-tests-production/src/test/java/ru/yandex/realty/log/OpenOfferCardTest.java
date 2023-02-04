package ru.yandex.realty.log;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.yandex.realty.module.RealtyWebProdModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import java.text.SimpleDateFormat;
import java.util.Date;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.NO_VALUE;

@DisplayName("Проверяем что события доехали до логов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebProdModule.class)
public class OpenOfferCardTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private WebDriverManager webDriverManager;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Открытие карточки оффера. Клик по показать телефон. Видим в логах")
    public void shouldSeeLogs() {
        urlSteps.production().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(NEW_FLAT_URL_PARAM, NO_VALUE).open();
        String yandexUid = basePageSteps.getCookieBy("yandexuid").getValue();
        String offerId = basePageSteps.onOffersSearchPage().offer(FIRST).getOfferId();
        urlSteps.production().path(OFFER).path(offerId).open();
        basePageSteps.onOfferCardPage().offerCardSummary().showPhoneButton().click();
        basePageSteps.onOfferCardPage().offerCardSummary().phone();
        webDriverManager.stopDriver();

        String dateDirectory = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String operation = getResourceAsString("yql/operation.json")
                .replace("#YANDEX_UID#", yandexUid)
                .replace("#DATE#", dateDirectory);
        String operationId = retrofitApiSteps.runOperation(operation);
        String actual = retrofitApiSteps.getOperationData(operationId);
        String expected = getResourceAsString("yql/expected.json")
                .replaceAll("#YANDEX_UID#", yandexUid).replaceAll("#OFFER_ID#", offerId);
        assertThatJson(actual).isEqualTo(expected);
    }
}