package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.MortgageTouchOffer;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.step.CommonSteps.FIRST;

public interface MortgageTouchPage extends BasePage {

    String REGISTER_BUTTON = "Оформить";

    @Name("Блок калькулятора ипотеки")
    @FindBy(".//div[@class='MortgageSearchFilters']")
    Button mortgageCalc();

    @Name("Список офферов")
    @FindBy(".//div[contains(@class,'OfferSliderSnippet__container')]")
    ElementsCollection<MortgageTouchOffer> mortgageOffersList();

    @Name("Список ипотечных программ")
    @FindBy(".//div[contains(@class,'MortgageProgramSnippetSearch__container')]")
    ElementsCollection<Button> mortgagesPrograms();

    default Button firstMortgageProgram() {
        return mortgagesPrograms().waitUntil(hasSize(greaterThan(0))).get(FIRST);
    }

    default MortgageTouchOffer offer(int i) {
        return mortgageOffersList().should(hasSize(greaterThan(i))).get(i);
    }
}
