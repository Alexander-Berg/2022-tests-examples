package ru.yandex.realty.offers.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static ru.auto.tests.commons.util.Utils.getRandomShortLong;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.OfferAdd.AREA;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.FLOOR;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.DRAFT;
import static ru.yandex.realty.element.offers.FeatureField.SECOND;
import static ru.yandex.realty.lambdas.WatchException.watchException;
import static ru.yandex.realty.page.ManagementNewPage.REDACT;
import static ru.yandex.realty.page.OfferAddPage.SAVE_CHANGES;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING;

@DisplayName("Заполняем форму оффера, сохраняем черновик?.")
@Feature(OFFERS)
@Story(DRAFT)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class DraftAuthTest {

    private static final String SAVE_QUERY = "/gate/add/save_draft";

    private long price = getRandomShortLong();
    private long floor = getRandomShortLong();
    private long area = 10 + getRandomShortLong();
    private long totalFloor = floor + 1;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private ProxySteps proxy;

    @Before
    public void before() {
        api.createVos2Account(account, AccountType.OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();

        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);

        offerAddSteps.addPhoto(DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputInItem(FIRST, valueOf(floor));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputInItem(SECOND, valueOf(totalFloor));

        offerAddSteps.onOfferAddPage().featureField(AREA).input().sendKeys(valueOf(area));
        offerAddSteps.onOfferAddPage().priceField().priceInput().sendKeys(valueOf(price));

        proxy.proxyServerManager.getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        proxy.clearHarUntilThereAreNoHarEntries();
        offerAddSteps.onOfferAddPage().button(SAVE_CHANGES).click();
    }

    @Test
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    @Description("Проверяем, что черновик сохраняется")
    public void shouldSeeSaveDraft() {
        proxy.shouldSeeRequestInLog(containsString(SAVE_QUERY), greaterThanOrEqualTo(1));

        urlSteps.testing().path(MANAGEMENT_NEW).shouldNotDiffWithWebDriverUrl();

        managementSteps.moveCursor(managementSteps.onManagementNewPage().offer(FIRST));
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().link(REDACT).click();

        offerAddSteps.onOfferAddPage().locationControls().suggest().should(hasValue(DEFAULT_LOCATION));
        offerAddSteps.setFlat("1");
        offerAddSteps.onOfferAddPage().priceField().priceInput()
                .should("Цена должна сохранится", hasValue(valueOf(price)));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(FIRST)
                .should("Этажи должны сохранятся", hasValue(valueOf(floor)));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(SECOND)
                .should("Этажность дома должна сохраняться", hasValue(valueOf(totalFloor)));
        offerAddSteps.onOfferAddPage().featureField(AREA).input()
                .should("Площадь квартиры должна сохранятся", hasValue(valueOf(area)));
    }


    @Test
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    @Description("Проверяем, запрос на сохранения черновика отправляется с нужными параметрами")
    public void shouldSeeSaveDraftParams() {

        JsonObject expected = new GsonBuilder().create()
                .fromJson(getResourceAsString("draft/draftparam.json"), JsonObject.class);
        expected.addProperty("price", price);
        expected.addProperty("floor", floor);
        expected.addProperty("floorsTotal", totalFloor);
        expected.addProperty("area", area);

        watchException(() -> given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .ignoreExceptions().pollInSameThread()
                .pollInterval(1, SECONDS).atMost(10, SECONDS)
                .untilAsserted(() -> {
                    String text = proxy.proxyServerManager.getServer().getHar().getLog().getEntries()
                            .stream().filter(e -> e.getRequest().getUrl().contains(SAVE_QUERY))
                            .findFirst().get().getRequest().getPostData().getText();
                    String actual = new GsonBuilder().create().fromJson(text, JsonObject.class)
                            .getAsJsonPrimitive("data").getAsString();
                    assertThat(actual.contains("photo")).isTrue().isNotNull();
                    assertThatJson(actual).whenIgnoringPaths("location.sign", "photo", "location.coords")
                            .isEqualTo(expected.toString());
                }));
    }
}
