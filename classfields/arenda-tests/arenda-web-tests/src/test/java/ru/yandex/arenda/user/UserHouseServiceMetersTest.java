package ru.yandex.arenda.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.EDIT;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.HOUSE_SERVICE;
import static ru.yandex.arenda.constants.UriPath.HOUSE_SERVICES;
import static ru.yandex.arenda.constants.UriPath.HOUSE_SERVICE_LIST;
import static ru.yandex.arenda.constants.UriPath.LK;
import static ru.yandex.arenda.constants.UriPath.OWNER;
import static ru.yandex.arenda.constants.UriPath.SETTINGS;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.BY_MYSELF_LABEL;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.METERS_DATA;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.NO_LABEL;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.SAVE_SETTINGS;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.COUNTER_NUMBER_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.COUNTER_PHOTO_1_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.COUNTER_PHOTO_2_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.COUNTER_PHOTO_3_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.CURRENT_VALUE_1_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.CURRENT_VALUE_2_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.CURRENT_VALUE_3_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.DELIVER_FROM_DAY_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.DELIVER_TO_DAY_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.INSTALLED_PLACE_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage.TARIFF_ID;
import static ru.yandex.arenda.steps.LkSteps.getDefaultImagePath;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-2040")
@DisplayName("ЖКХ. Флоу собственника")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserHouseServiceMetersTest {

    private static final String GAS_METER_TEXT = "Газ";
    private static final String COUNTER_TYPE_PARAM = "counterType";
    private static final String GAS_VALUE = "GAS";
    private static final String SAVE_BUTTON = "Сохранить";
    private static final String NECESSARY_FIELD_ERROR = "Обязательное поле";
    private static final String CURRENT_VALUE_ERROR = "Необходимо добавить показание";
    private static final String COUNTER_PHOTO_ERROR = "Необходимо добавить фотографии счётчика со значением для каждого тарифа";
    private static final String DELIVER_DAY_ERROR = "Некорректное значение для дня месяца";

    String createdFlatId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        String ownerRequestId = retrofitApiSteps.getOwnerRequestID(createdFlatId);
        retrofitApiSteps.updateStatusInProgress(createdFlatId, ownerRequestId);
        passportSteps.login(account);
    }

    @Test
    @DisplayName("Есть услуги, которые жилец должен оплачивать самостоятельно? -> Я сам -> " +
            "Показания счётчиков -> появляется экран счетчиков")
    public void shouldSeeMetersPage() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(BY_MYSELF_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().divWithLabel(METERS_DATA).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantPaidByTenantBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantRefundBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SAVE_SETTINGS).click();

        lkSteps.onLkOwnerHouseServiceMetersPage().addMeterButton().click();
        lkSteps.onLkOwnerHouseServiceMetersPage().metersPopup().meterItem(GAS_METER_TEXT).click();
        lkSteps.onLkOwnerHouseServiceMetersPage().metersTitle().should(hasText(equalTo(GAS_METER_TEXT)));
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(HOUSE_SERVICE).path(EDIT).queryParam(COUNTER_TYPE_PARAM, GAS_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Экран счетчиков -> видим ошибки")
    public void shouldAddMetersErrors() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(HOUSE_SERVICE).path(EDIT).queryParam(COUNTER_TYPE_PARAM, GAS_VALUE).open();
        lkSteps.onLkOwnerHouseServiceMetersPage().button(SAVE_BUTTON).click();

        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(COUNTER_NUMBER_ID)
                .errorDescription(NECESSARY_FIELD_ERROR).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(CURRENT_VALUE_1_ID)
                .errorDescription(CURRENT_VALUE_ERROR).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(COUNTER_PHOTO_1_ID)
                .errorDescription(COUNTER_PHOTO_ERROR).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(DELIVER_FROM_DAY_ID)
                .errorDescription(DELIVER_DAY_ERROR).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(DELIVER_TO_DAY_ID)
                .errorDescription(DELIVER_DAY_ERROR).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(INSTALLED_PLACE_ID)
                .errorDescription(NECESSARY_FIELD_ERROR).should(isDisplayed());
    }

    @Test
    @DisplayName("Экран счетчиков -> сохраняем показания")
    public void shouldAddMeters() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(HOUSE_SERVICE).path(EDIT).queryParam(COUNTER_TYPE_PARAM, GAS_VALUE).open();

        String testText = getRandomString();
        String testMeterValue = "200000";
        String testDateFrom = "13";
        String testDateTo = "30";

        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(COUNTER_NUMBER_ID).input().sendKeys(testText);
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(CURRENT_VALUE_1_ID).input().sendKeys(testMeterValue);
        lkSteps.setFileDetector();
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(COUNTER_PHOTO_1_ID).input()
                .sendKeys(getDefaultImagePath());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(DELIVER_FROM_DAY_ID).inputId(DELIVER_FROM_DAY_ID)
                .sendKeys(testDateFrom);
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(DELIVER_TO_DAY_ID).inputId(DELIVER_TO_DAY_ID)
                .sendKeys(testDateTo);
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(INSTALLED_PLACE_ID).input().sendKeys(testText);
        lkSteps.onLkOwnerHouseServiceMetersPage().button(SAVE_BUTTON).click();
        lkSteps.onLkOwnerHouseServiceMetersPage().successToast();
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(HOUSE_SERVICE_LIST).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Экран счетчиков ->  Формата счетчика (у 2-3тарифных добавляются поля)")
    public void shouldMetersAdditionFields() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(HOUSE_SERVICE).path(EDIT).queryParam(COUNTER_TYPE_PARAM, GAS_VALUE).open();

        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(CURRENT_VALUE_1_ID).waitUntil(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(COUNTER_PHOTO_1_ID).waitUntil(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().selector(TARIFF_ID).click();
        lkSteps.onLkOwnerHouseServiceMetersPage().option("Трёхтарифный").click();
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(CURRENT_VALUE_2_ID).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(COUNTER_PHOTO_2_ID).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(CURRENT_VALUE_3_ID).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceMetersPage().metersInput(COUNTER_PHOTO_3_ID).should(isDisplayed());
    }
}
