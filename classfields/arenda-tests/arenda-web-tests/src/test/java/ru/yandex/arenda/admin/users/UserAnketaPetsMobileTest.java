package ru.yandex.arenda.admin.users;

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
import ru.yandex.arenda.module.ArendaTouchModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USER;
import static ru.yandex.arenda.element.lk.admin.StepModal.DONE_BUTTON;
import static ru.yandex.arenda.element.lk.admin.StepModal.NEXT_BUTTON;
import static ru.yandex.arenda.matcher.AttributeMatcher.isChecked;
import static ru.yandex.arenda.pages.LkPage.ADDITIONAL_TENANT_ID;
import static ru.yandex.arenda.pages.LkPage.PETS_INFO;
import static ru.yandex.arenda.pages.LkPage.REASON_FOR_RELOCATION_ID;
import static ru.yandex.arenda.pages.LkPage.TELL_ABOUT_YOURSELF_ID;
import static ru.yandex.arenda.pages.LkPage.WITH_CHILDREN;
import static ru.yandex.arenda.pages.LkPage.WITH_PETS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1734")
@DisplayName("[Админка] Редактирование анкеты пользователя")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaTouchModule.class)
public class UserAnketaPetsMobileTest {

    private static final String ANKETA = "Анкета";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private Account account;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("Проверяем валидацию на поле «С животными» и «Описание питомца». Заполнена галочка и описание, " +
            "при убирании галочки нет в пошаговости описания")
    public void shouldNotSeePetsInfo() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());

        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(isDisplayed());
        lkSteps.onLkPage().label(WITH_PETS).click();
        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(not(isDisplayed()));

        lkSteps.onLkPage().textAreaId(REASON_FOR_RELOCATION_ID).click();
        lkSteps.onLkPage().stepModal().textAreaId(REASON_FOR_RELOCATION_ID).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().textAreaId(TELL_ABOUT_YOURSELF_ID).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().selector(ADDITIONAL_TENANT_ID).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().label(WITH_CHILDREN).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().label(WITH_PETS).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(DONE_BUTTON).click();
    }

    @Test
    @DisplayName("Проверяем валидацию на поле «С животными» и «Описание питомца». При выставленной галке описание " +
            "есть в пошаговости")
    public void shouldSeeWithPets() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());

        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(isDisplayed());

        lkSteps.onLkPage().textAreaId(REASON_FOR_RELOCATION_ID).click();
        lkSteps.onLkPage().stepModal().textAreaId(REASON_FOR_RELOCATION_ID).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().textAreaId(TELL_ABOUT_YOURSELF_ID).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().selector(ADDITIONAL_TENANT_ID).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().label(WITH_CHILDREN).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().label(WITH_PETS).waitUntil(isDisplayed());
        lkSteps.onLkPage().stepModal().button(NEXT_BUTTON).click();
        lkSteps.onLkPage().stepModal().textAreaId(PETS_INFO).waitUntil(isDisplayed());
    }
}
