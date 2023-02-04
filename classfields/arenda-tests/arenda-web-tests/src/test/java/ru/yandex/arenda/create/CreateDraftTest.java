package ru.yandex.arenda.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.MainSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.arenda.constants.UriPath.LK_SDAT_KVARTIRY;
import static ru.yandex.arenda.element.lk.OwnerFlatSnippet.CONTINUE_FILLING;
import static ru.yandex.arenda.pages.LkPage.ADDRESS_ID;
import static ru.yandex.arenda.pages.LkPage.FLAT_NUMBER_ID;
import static ru.yandex.arenda.pages.LkPage.NAME_ID;
import static ru.yandex.arenda.pages.LkPage.SAVE_DRAFT_AND_CONTINUE;
import static ru.yandex.arenda.pages.LkPage.SURNAME_ID;
import static ru.yandex.arenda.pages.LkPage.TEST_ADDRESS;
import static ru.yandex.arenda.pages.LkPage.TEST_FLAT_NUMBER;
import static ru.yandex.arenda.pages.LkPage.TEST_NAME;
import static ru.yandex.arenda.pages.LkPage.TEST_SURNAME;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1662")
@DisplayName("Создание заявки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class CreateDraftTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MainSteps mainSteps;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
        urlSteps.testing().path(LK_SDAT_KVARTIRY).open();
        mainSteps.onLkPage().inputId(ADDRESS_ID).sendKeys(TEST_ADDRESS);
        mainSteps.onLkPage().inputId(FLAT_NUMBER_ID).sendKeys(TEST_FLAT_NUMBER);
        mainSteps.onLkPage().inputId(NAME_ID).clearInputCross().clickIf(isDisplayed());
        mainSteps.onLkPage().inputId(NAME_ID).sendKeys(TEST_NAME);
        mainSteps.onLkPage().inputId(SURNAME_ID).clearInputCross().clickIf(isDisplayed());
        mainSteps.onLkPage().inputId(SURNAME_ID).sendKeys(TEST_SURNAME);
        mainSteps.onLkPage().spanLink(SAVE_DRAFT_AND_CONTINUE).click();
        mainSteps.onLkPage().buttonRequest(CONTINUE_FILLING).waitUntil(isDisplayed());
    }

    @Test
    @DisplayName("Создаем нового юзера в паспорте, меняем данные, создаем черновик заявки, данные юзера не изменились")
    public void shouldNotSeeUserData() {
        JsonObject userInfo = retrofitApiSteps.getUserByUid(account.getId());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(userInfo.getAsJsonObject("person").entrySet().isEmpty())
                    .as("Проверяем что поле «person» пусто").isTrue();
            softly.assertThat(userInfo.has("phone"))
                    .as("Проверяем что поле «phone» отсутствует").isFalse();
        });
    }

    @Test
    @DisplayName("Создаем нового юзера в паспорте, меняем данные, создаем черновик заявки, проверяем что черновик создался")
    public void shouldSeeUserDraft() {
        JsonObject userFlats = retrofitApiSteps.getUserFlats(account.getId());
        assertThatJson(userFlats).whenIgnoringPaths("flats[0].flatId", "flats[0].assignedUsers[0].uid",
                "flats[0].assignedUsers[0].userId", "flats[0].email", "flats[0].code")
                .isEqualTo(getResourceAsString("testresources/flatsDraft.json"));
    }
}
