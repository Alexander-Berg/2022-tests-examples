package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SALE_PHOTOS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка объявления - загрузка фотографий")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class AddPhotosTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/OfferCarsUsedUserOwnerActiveNoPhoto"),
                stub("mobile/UserOffersCarsEdit"),
                stub("mobile/UserDraftCarsDraftId"),
                stub("mobile/UserDraftCarsPut"),
                stub("mobile/UserDraftCarsPublish")
        ).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Добавить фото»")
    public void shouldClickAddPhotoButton() {
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();

        basePageSteps.onCardPage().galleryStub().button("Добавить фото").click();

        urlSteps.testing().path(SALE_PHOTOS).path(CARS).path(SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение формы загрузки фотографий")
    public void shouldSeeAddPhotosForm() {
        urlSteps.testing().path(SALE_PHOTOS).path(CARS).path(SALE_ID).open();

        basePageSteps.onCardPhotosPage().addPhotosForm().should(hasText("УАЗ Patriot I Рестайлинг 2\n" +
                "Добавить фотографии\nНе более 30 штук\nСохранить"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Сохранить»")
    public void shouldClickSaveButton() {
        urlSteps.testing().path(SALE_PHOTOS).path(CARS).path(SALE_ID).open();

        basePageSteps.onCardPhotosPage().saveButton().click();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/").path(SALE_ID)
                .shouldNotSeeDiff();
    }
}
