package ru.yandex.arenda.create;

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
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.FLATS_SEARCH;
import static ru.yandex.arenda.constants.UriPath.LK;
import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA;
import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.constants.UriPath.LK_TENANT_QUESTIONNAIRE;
import static ru.yandex.arenda.constants.UriPath.LK_TENANT_QUESTIONNAIRE_EDIT;
import static ru.yandex.arenda.constants.UriPath.TENANT;
import static ru.yandex.arenda.pages.BasePage.ANKETA_LINK;
import static ru.yandex.arenda.pages.LkPage.ADDITIONAL_TENANT_ID;
import static ru.yandex.arenda.pages.LkPage.PERSONAL_ACTIVITY_TYPE;
import static ru.yandex.arenda.pages.LkPage.PETS_INFO;
import static ru.yandex.arenda.pages.LkPage.REASON_FOR_RELOCATION_ID;
import static ru.yandex.arenda.pages.LkPage.SEND_BUTTON;
import static ru.yandex.arenda.pages.LkPage.SEND_TO_CHECK_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1723")
@DisplayName("Личные данные")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class PersonalDataQuestionnaireValidatingTest {

    private static final String PASSPORT_TEXT = "9999999999";
    private static final String PASSPORT_ISSUE_BY = getRandomString();
    private static final String ISSUE_DATE_TEXT = "01012000";
    private static final String DEPARTMENT_TEXT = "123 - 456";
    private static final String BIRTHDAY_TEXT = "01022000";
    private static final String BIRTHPLACE_TEXT = getRandomString();
    private static final String NAME = "Валерий";
    private static final String SURNAME = "Валеридзе";
    private static final String PATRONYMIC = "Валерыч";
    private static final String DISABLED_ATTRIBUTE = "disabled";
    private static final String TRUE_VALUE = "true";
    private static final String WITH_PETS = "Есть домашние животные";
    private static final String PET_NAME = "Шарик";
    private static final String HEADER_TEXT_PERSONAL_DATA = "Личные данные";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
        lkSteps.fillFormPersonalData(NAME, SURNAME, PATRONYMIC);
        lkSteps.fillPhonePersonalData();
        lkSteps.fillFormPassportData(PASSPORT_TEXT, PASSPORT_ISSUE_BY, ISSUE_DATE_TEXT, DEPARTMENT_TEXT, BIRTHDAY_TEXT,
                BIRTHPLACE_TEXT);
        lkSteps.onLkPage().editPhoneButton().waitUntil(isDisplayed());
        lkSteps.onLkPage().button(SEND_TO_CHECK_BUTTON).click();
        lkSteps.onLkPage().headerText(HEADER_TEXT_PERSONAL_DATA).waitUntil(isDisplayed());
        urlSteps.testing().path(LK_PERSONAL_DATA).open();
        lkSteps.refreshUntil(() -> {
            lkSteps.onLkPage().myCabinet().click();
            return lkSteps.onBasePage().myCabinetPopupDesktop().link(ANKETA_LINK);
        }, isDisplayed(), 20);
        lkSteps.onBasePage().myCabinetPopupDesktop().link(ANKETA_LINK).click();
        urlSteps.testing().path(LK_TENANT_QUESTIONNAIRE_EDIT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Обязательны место работы и с кем проживать")
    public void shouldSeeQuestionnaireValidation() {
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.onLkPage().invalidInputQuestionnaire(PERSONAL_ACTIVITY_TYPE).should(hasText("Укажите чем занимаетесь"));
        lkSteps.onLkPage().invalidInputQuestionnaire(ADDITIONAL_TENANT_ID)
                .should(hasText("Укажите, с кем планируете проживать"));
    }

    @Test
    @DisplayName("Если не выставлена галочка есть животные поле «О питомце» недоступно. Заполняем «О питомце»")
    public void shouldSeeQuestionnaireAboutPet() {
        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().divWithLabel(WITH_PETS).click();
        lkSteps.onLkPage().textAreaId(PETS_INFO).isDisplayed();
        lkSteps.onLkPage().textAreaId(PETS_INFO).sendKeys(PET_NAME);
        lkSteps.fillAnketa();
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        urlSteps.testing().path(LK).path(TENANT).path(FLATS_SEARCH).shouldNotDiffWithWebDriverUrl();
        urlSteps.testing().path(LK_TENANT_QUESTIONNAIRE_EDIT).open();
        lkSteps.onLkPage().textAreaId(PETS_INFO).should(hasText(PET_NAME));
    }

    @Test
    @DisplayName("После обновления анкеты происходит редирект на презентационную страницу")
    public void shouldRedirectToPresentation() {
        lkSteps.fillAnketa();
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        urlSteps.testing().path(LK).path(TENANT).path(FLATS_SEARCH).shouldNotDiffWithWebDriverUrl();
        urlSteps.testing().path(LK_TENANT_QUESTIONNAIRE_EDIT).open();
        lkSteps.onLkPage().textAreaId(REASON_FOR_RELOCATION_ID).sendKeys(getRandomString());
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        urlSteps.testing().path(LK_TENANT_QUESTIONNAIRE).queryParam("withoutCustomRedirect", "true")
                .shouldNotDiffWithWebDriverUrl();
    }
}
