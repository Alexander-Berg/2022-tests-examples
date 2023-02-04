package ru.auto.tests.poffer.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MULTIPOSTING;
import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ADD_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.element.poffer.DescriptionBlock.ADD_AVITO_DESCRIPTION;
import static ru.auto.tests.desktop.element.poffer.DescriptionBlock.ADD_DROM_DESCRIPTION;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Проверка блока описания в мультипостинге в Авито/Дром")
@Feature(POFFER)
@Story(MULTIPOSTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class MultipostingAlternateDescriptionTest {

    private static final String OFFER_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with(
                "desktop/SessionAuthDealer",
                "desktop/ReferenceCatalogCarsAllOptions",
                "poffer/ReferenceCatalogCarsSuggestLifanSolano",
                "poffer/dealer/DealerInfoMultipostingEnabled",
                "poffer/dealer/UserDraftCarsDraftIdGetUsedWithMultiposting",
                "poffer/dealer/UserDraftCarsUsed",
                "poffer/dealer/UserDraftCarsDraftIdPublishUsed"
        ).post();

        urlSteps.testing().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не отображается блок «Описание для Авито» без активного мультипостинга в авито")
    public void shouldNotSeeAvitoDescriptionBlockWithoutMultiposting() {
        pofferSteps.onPofferPage().descriptionBlock().avitoDescription().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не отображается блок «Описание для Дрома» без активного мультипостинга в дроме")
    public void shouldNotSeeDromDescriptionBlockWithoutMultiposting() {
        pofferSteps.onPofferPage().descriptionBlock().dromDescription().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст блока «Описание для Авито»")
    public void shouldSeeAvitoDescriptionBlockText() {
        pofferSteps.onPofferPage().multiposting().avito().click();
        pofferSteps.onPofferPage().descriptionBlock().avitoDescription().button(ADD_AVITO_DESCRIPTION).click();

        pofferSteps.onPofferPage().descriptionBlock().avitoDescription().should(hasText("Описание на Авито\nТекст " +
                "должен соответствовать правилам Авито и по объёму не превышать 5000 символов. Форматировать текст " +
                "можно с помощью HTML-тегов из указанного списка: p, br, strong, em, ul, ol, li."));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст блока «Описание для Дрома»")
    public void shouldSeeDromDescriptionBlockText() {
        pofferSteps.onPofferPage().multiposting().drom().click();
        pofferSteps.onPofferPage().descriptionBlock().dromDescription().button(ADD_DROM_DESCRIPTION).click();

        pofferSteps.onPofferPage().descriptionBlock().dromDescription().should(hasText("Описание на Дром\n" +
                "Не указывайте в этом поле электронную почту, номер телефона, цену, адрес места осмотра и не " +
                "предлагайте какие-либо услуги — такое объявление не пройдет модерацию."));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавляем «Описание для Авито»")
    public void shouldAddAvitoDescription() {
        mockRule.with("poffer/dealer/UserDraftCarsDraftIdPutUsedWithAvitoDescription").update();
        pofferSteps.onPofferPage().multiposting().avito().click();
        pofferSteps.onPofferPage().descriptionBlock().avitoDescription().button(ADD_AVITO_DESCRIPTION).click();
        pofferSteps.onPofferPage().descriptionBlock().avitoDescription().input().sendKeys("Описание Авито");
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody("offers/cars_used_dealer_multiposting_avito_offer_with_description.json")
        ));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID)
                .addParam(PAGE_FROM, ADD_PAGE).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавляем «Описание для Дрома»")
    public void shouldAddDromDescription() {
        mockRule.with("poffer/dealer/UserDraftCarsDraftIdPutUsedWithDromDescription").update();
        pofferSteps.onPofferPage().multiposting().drom().click();
        pofferSteps.onPofferPage().descriptionBlock().dromDescription().button(ADD_DROM_DESCRIPTION).click();
        pofferSteps.onPofferPage().descriptionBlock().dromDescription().input().sendKeys("Описание Дром");
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody("offers/cars_used_dealer_multiposting_drom_offer_with_description.json")
        ));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID)
                .addParam(PAGE_FROM, ADD_PAGE).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавляем «Описание для Авито» и «Описание для Дрома»")
    public void shouldAddAvitoAndDromDescription() {
        mockRule.with("poffer/dealer/UserDraftCarsDraftIdPutUsedWithAvitoAndDromDescription").update();
        pofferSteps.onPofferPage().multiposting().avito().click();
        pofferSteps.onPofferPage().multiposting().drom().click();
        pofferSteps.onPofferPage().descriptionBlock().avitoDescription().button(ADD_AVITO_DESCRIPTION).click();
        pofferSteps.onPofferPage().descriptionBlock().avitoDescription().input().sendKeys("Описание Авито");
        pofferSteps.onPofferPage().descriptionBlock().dromDescription().button(ADD_DROM_DESCRIPTION).click();
        pofferSteps.onPofferPage().descriptionBlock().dromDescription().input().sendKeys("Описание Дром");
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody("offers/cars_used_dealer_multiposting_avito_and_drom_offer_with_description.json")
        ));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID)
                .addParam(PAGE_FROM, ADD_PAGE).shouldNotSeeDiff();
    }

}
