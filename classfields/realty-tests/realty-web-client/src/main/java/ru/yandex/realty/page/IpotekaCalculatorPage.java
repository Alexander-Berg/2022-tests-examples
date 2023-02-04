package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.ipoteka.MortgageCalc;
import ru.yandex.realty.element.ipoteka.MortgageOffersBlock;
import ru.yandex.realty.element.ipoteka.MortgageProgram;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.step.CommonSteps.FIRST;

/**
 * @author kantemirov
 */
public interface IpotekaCalculatorPage extends BasePage, Link {

    @Name("Блок калькулятора ипотеки")
    @FindBy(".//div[contains(@class,'MortgageCalculator__calculator')]")
    MortgageCalc mortgageCalc();

    @Name("Заголовок блока офферов")
    @FindBy(".//h1")
    AtlasWebElement headerOffers();

    @Name("Блок оферов")
    @FindBy(".//div[contains(@class,'MortgageCalculator__offers')]")
    MortgageOffersBlock mortgageOffersBlock();

    @Name("Блок «Нет предложений»")
    @FindBy(".//div[contains(@class, 'MortgageProgramsSerp__notFound')]")
    AtlasWebElement notFoundOffersBlock();

    @Name("Попап подсказки")
    @FindBy(".//div[@class='Portal']//div[contains(@class,'MortgageResults__popup')]")
    AtlasWebElement hintPopup();

    @Name("Список ипотечных программ")
    @FindBy(".//div[contains(@class,'MortgageProgramSnippetSearch__container')]")
    ElementsCollection<MortgageProgram> mortgagesPrograms();

    default MortgageProgram firstMortgageProgram() {
        return mortgagesPrograms().waitUntil(hasSize(greaterThan(0))).get(FIRST);
    }

    @Name("Баблы на карте похожих предложений")
    @FindBy(".//ymaps[contains(@class,'placemark-overlay')]/ymaps/a")
    ElementsCollection<AtlasWebElement> placemarks();

    @Name("Балун оффера на карте")
    @FindBy(".//ymaps[contains(@class,'balloon-pane')]")
    Link balloon();
}
