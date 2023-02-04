package ru.auto.tests.forms.user;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;

import javax.inject.Inject;
import java.io.IOException;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@DisplayName("Частник, мотоциклы -  блок «Место осмотра»")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesAddressTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        formsSteps.createMotorcyclesForm();
        formsSteps.setReg(false);

        urlSteps.testing().path(MOTO).path(ADD).addXRealIP(MOSCOW_IP).open();
        formsSteps.fillForm(formsSteps.getCategory().getBlock());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор региона из саджеста")
    public void shouldSelectRegionFromSuggest() {
        String block = formsSteps.getPlace().getBlock();
        String city = "Химки";

        formsSteps.onFormsPage().foldedBlock(block).click();

        formsSteps.onFormsPage().unfoldedBlock(block).input("Город продажи", city);
        formsSteps.selectAddressFromSuggest(city);
        formsSteps.onFormsPage().unfoldedBlock(block).input("Город продажи").waitUntil(hasValue(city));

        formsSteps.onFormsPage().unfoldedBlock(block).input("Место осмотра", "Юбилейный");
        System.out.println(formsSteps.onFormsPage().unfoldedBlock(block).geoSuggest().getItem(2).getText());
        formsSteps.selectAddressFromSuggest("Юбилейный проспект, 60Химки, Московская область");
        formsSteps.onFormsPage().unfoldedBlock(block).input("Место осмотра")
                .waitUntil(hasValue("Юбилейный проспект, 60"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Стоимость услуг меняется при смене региона")
    public void shouldSeeDifferentPricesInDifferentRegions() {
        String block = formsSteps.getPlace().getBlock();
        String city = "Красноярск";

        formsSteps.onFormsPage().foldedBlock(block).click();
        formsSteps.onFormsPage().userVas().getSnippet(0).should(hasText("Экспресс-продажа\n55 ₽/ день\n" +
                "Добавьте цвета и на 6 дней окажитесь почти на каждой странице сайта, чтобы не ускользнуть от " +
                "глаз будущего владельца вашей машины.\nВыделение цветом\nСпецпредложение\nРазместить за " +
                "327 ₽ на 30 дней\nВместо 545 ₽\n-40%"));
        formsSteps.waitForSuggest(block, "Город продажи", city);
        formsSteps.selectAddressFromSuggest(city);

        formsSteps.onFormsPage().userVas().getSnippet(0).waitUntil(hasText("Экспресс-продажа\n66 ₽/ день\n" +
                "Добавьте цвета и на 6 дней окажитесь почти на каждой странице сайта, чтобы не ускользнуть от " +
                "глаз будущего владельца вашей машины.\nВыделение цветом\nСпецпредложение\nРазместить за " +
                "397 ₽ на 30 дней\nВместо 661 ₽\n-40%"));
    }
}