package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.general.consts.GeneralFeatures.PRODUCT_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.VACANCY_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.BasePage.PRODUCT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;

@Epic(SEO_FEATURE)
@Feature(PRODUCT_SEO_MARK)
@DisplayName("Нет разметки «Product» на карточке вакансии")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoNoProductOfferMarkingVacancyTest {

    private static final String ID = "12345";

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Rule
    @Inject
    public MockRuleWithoutWebdriver mockRule;

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(VACANCY_CARD).setId(ID).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.testing().path(CARD).path(ID).setMockritsaImposter(mockRule.getPort())
                .setDesktopUserAgent().get();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет Schema.org разметки «Product» на карточке вакансии")
    public void shouldNotSeeProductSchemaOrgMarkingOnVacancyCard() {
        Assert.assertThat("Нет разметки ShemaOrg AggregateOffer",
                jSoupSteps.select("div[itemtype='http://schema.org/Product']").html(),
                equalTo(""));    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет Ld-Json разметки «Product» на карточке вакансии")
    public void shouldNotSeeProductLdJsonMarkingOnVacancyCard() {
        jSoupSteps.noLdJsonMark(PRODUCT);
    }

}
