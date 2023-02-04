package ru.yandex.realty.documents;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOGOVOR_KUPLI_PRODAZHI;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.RealtyFeatures.DOCUMENTS_FEATURE;

@Feature(DOCUMENTS_FEATURE)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница шаблона")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DocTemplateMetaTest {
    private static final String TITLE_TEXT = "Образец договора купли-продажи недвижимости, скачать бланк " +
            "договора купли-продажи недвижимого имущества – Яндекс.Недвижимость";
    private static final String DESCRIPTION_TEXT = "✅ Бесплатно скачать договор купли-продажи недвижимости " +
            "составленный юристами на сайте Яндекс.Недвижимость. Типовой договор купли-продажи недвижимого " +
            "имущества – готовый бланк";
    private static final String H1_TEXT = "Договор купли-продажи недвижимости";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(DOKUMENTY).path(DOGOVOR_KUPLI_PRODAZHI);
    }

    @Ignore("Не ловит текст почему-то")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим title")
    public void shouldSeeTitle() {
        urlSteps.open();
        System.out.println(basePageSteps.onDocumentsPage().pageTitle().getText());
        basePageSteps.onDocumentsPage().pageTitle().should(hasText(TITLE_TEXT));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим description")
    public void shouldSeeDescription() {
        urlSteps.open();
        basePageSteps.onDocumentsPage().pageDescription().should(hasAttribute("content", DESCRIPTION_TEXT));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим h1")
    public void shouldSeeH1() {
        urlSteps.open();
        basePageSteps.onDocumentsPage().pageH1().should(hasText(H1_TEXT));
    }
}
