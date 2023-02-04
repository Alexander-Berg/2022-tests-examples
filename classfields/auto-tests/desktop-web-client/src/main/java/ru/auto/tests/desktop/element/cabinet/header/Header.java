package ru.auto.tests.desktop.element.cabinet.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 19.04.18
 */
public interface Header extends VertisElement {

    @Name("Логотип")
    @FindBy(".//a[contains(@class, 'HeaderLogo__container')]")
    VertisElement logo();

    @Name("Программа лояльности")
    @FindBy(".//div[contains(@class, 'HeaderLoyaltyLink__container')]")
    VertisElement loyalty();

    @Name("Финансовый виджет")
    @FindBy(".//div[contains(@class, 'Header__balance')]")
    FinancialWidget financialWidget();

    @Name("Раскрытый финансовый виджет")
    @FindBy(".//div[contains(@class, 'HeaderBalanceForm__container')]")
    FinancialWidget financialWidgetOpen();

    @Name("Добавить объявление")
    @FindBy(".//div[contains(@class, 'Header__addOffer')]")
    VertisElement addOffer();

    @Name("Задать вопрос")
    @FindBy(".//div[contains(@class, 'HeaderFeedback__button')]")
    VertisElement askQuestions();

    @Name("Чат")
    @FindBy(".//div[contains(@class, 'HeaderChat__container')]")
    VertisElement chat();

    @Name("Персональное меню клиента")
    @FindBy(".//div[contains(@class, 'Header__user')]")
    PersonalMenuOfDealer personalMenuOfDealer();

    @Name("Поп-ап «Задайте вопрос»")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__content')]")
    FeedbackPopup feedbackPopup();

    @Name("Поп-ап «Задайте вопрос (без фона)»")
    @FindBy("//div[contains(@class, 'HeaderFeedback__form')]")
    FeedbackPopup feedbackPopupWithoutBackground();

    @Name("Поп-ап «Программа лояльности»")
    @FindBy(".//div[contains(@class, 'Header__loyaltyMenu')]")
    VertisElement loyaltyPopup();
}
