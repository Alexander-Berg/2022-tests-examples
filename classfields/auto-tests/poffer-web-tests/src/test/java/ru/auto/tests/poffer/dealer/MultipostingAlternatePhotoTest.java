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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;

import javax.inject.Inject;
import java.io.File;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MULTIPOSTING;
import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Проверка блока фото в мультипостинге в Авито/Дром")
@Feature(POFFER)
@Story(MULTIPOSTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MultipostingAlternatePhotoTest {

    private static final String PHOTO_PATH = "src/main/resources/images/lifan_solano.jpg";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "desktop/ReferenceCatalogCarsAllOptions",
                "poffer/ReferenceCatalogCarsSuggestLifanSolano",
                "poffer/dealer/DealerInfoMultipostingEnabled",
                "poffer/dealer/UserDraftCarsDraftIdGetUsedWithMultiposting",
                "poffer/dealer/UserDraftCarsUsed",
                "poffer/dealer/UserDraftCarsDraftIdPublishUsed").post();

        urlSteps.testing().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не отображается блок «Фотографии для Авито» без активного мультипостинга в авито")
    public void shouldNotSeeAvitoPhotoBlockWithoutMultiposting() {
        pofferSteps.onPofferPage().photoBlock().avitoPhotos().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не отображается блок «Фотографии для Дрома» без активного мультипостинга в дром")
    public void shouldNotSeeDromPhotoBlockWithoutMultiposting() {
        pofferSteps.onPofferPage().photoBlock().dromPhotos().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображается блок «Фотографии для Авито» с активным мультипостингом в авито")
    public void shouldSeeAvitoPhotoBlockWithMultiposting() {
        pofferSteps.onPofferPage().multiposting().avito().click();

        pofferSteps.onPofferPage().photoBlock().avitoPhotos().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображается блок «Фотографии для Дрома» с активным мультипостингом в дром")
    public void shouldSeeDromPhotoBlockWithMultiposting() {
        pofferSteps.onPofferPage().multiposting().drom().click();

        pofferSteps.onPofferPage().photoBlock().dromPhotos().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Загружаем фото в «Фотографии для Авито»")
    public void shouldAddAvitoPhoto() {
        pofferSteps.onPofferPage().multiposting().avito().click();
        pofferSteps.onPofferPage().photoBlock().avitoPhotos().button("Добавить фотографии для Авито").click();

        pofferSteps.onPofferPage().photoBlock().avitoPhotos().photo()
                .sendKeys(new File(PHOTO_PATH).getAbsolutePath());
        pofferSteps.onPofferPage().photoEditor().button("Готово").waitUntil(isDisplayed()).click();

        pofferSteps.onPofferPage().photoBlock().avitoPhotos().getPhoto(0).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Загружаем фото в «Фотографии для Дрома»")
    public void shouldAddDromPhoto() {
        pofferSteps.onPofferPage().multiposting().drom().click();
        pofferSteps.onPofferPage().photoBlock().dromPhotos().button("Добавить фотографии для Дрома").click();

        pofferSteps.onPofferPage().photoBlock().dromPhotos().photo()
                .sendKeys(new File(PHOTO_PATH).getAbsolutePath());
        pofferSteps.onPofferPage().photoEditor().button("Готово").waitUntil(isDisplayed()).click();

        pofferSteps.onPofferPage().photoBlock().dromPhotos().getPhoto(0).should(isDisplayed());
    }

}
