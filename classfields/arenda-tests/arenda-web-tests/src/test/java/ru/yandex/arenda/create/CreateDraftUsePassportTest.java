package ru.yandex.arenda.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.arenda.module.ArendaWithPhoneWebModule;
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.MainSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static ru.yandex.arenda.constants.AttributeLocator.VALUE_ATTRIBUTE;
import static ru.yandex.arenda.constants.UriPath.LK_SDAT_KVARTIRY;
import static ru.yandex.arenda.pages.LkPage.NAME_ID;
import static ru.yandex.arenda.pages.LkPage.PHONE_ID;
import static ru.yandex.arenda.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.arenda.utils.UtilsWeb.makePhoneFormatted;

@Link("https://st.yandex-team.ru/VERTISTEST-1662")
@DisplayName("Создание заявки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWithPhoneWebModule.class)
public class CreateDraftUsePassportTest {

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

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
        urlSteps.testing().path(LK_SDAT_KVARTIRY).open();
    }

    @Test
    @DisplayName("Создаем нового юзера, видим что подтянулись данные из паспорта (фамилия, имя и номер телефона)")
    public void shouldSeePassportUserData() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mainSteps.onLkPage().inputId(NAME_ID).getAttribute(VALUE_ATTRIBUTE))
                    .isEqualTo(account.getName());
            softly.assertThat(mainSteps.onLkPage().inputId(PHONE_ID).getAttribute(VALUE_ATTRIBUTE))
                    .isEqualTo(makePhoneFormatted(account.getPhone().get(), PHONE_PATTERN_BRACKETS));
        });
    }
}
