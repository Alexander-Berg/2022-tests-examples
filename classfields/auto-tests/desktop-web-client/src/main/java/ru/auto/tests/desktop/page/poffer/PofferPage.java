package ru.auto.tests.desktop.page.poffer;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.element.poffer.AddOptionsPopup;
import ru.auto.tests.desktop.element.poffer.AuctionBanner;
import ru.auto.tests.desktop.element.poffer.BadgesBlock;
import ru.auto.tests.desktop.element.poffer.BannedMessage;
import ru.auto.tests.desktop.element.poffer.Block;
import ru.auto.tests.desktop.element.poffer.BlockMMM;
import ru.auto.tests.desktop.element.poffer.Breadcrumbs;
import ru.auto.tests.desktop.element.poffer.CommentSuggest;
import ru.auto.tests.desktop.element.poffer.ComplectationBlock;
import ru.auto.tests.desktop.element.poffer.DealerVas;
import ru.auto.tests.desktop.element.poffer.DescriptionBlock;
import ru.auto.tests.desktop.element.poffer.FirstStepStsVinBlock;
import ru.auto.tests.desktop.element.poffer.GenerationsBlock;
import ru.auto.tests.desktop.element.poffer.MarkBlock;
import ru.auto.tests.desktop.element.poffer.ModelBlock;
import ru.auto.tests.desktop.element.poffer.Multiposting;
import ru.auto.tests.desktop.element.poffer.Options;
import ru.auto.tests.desktop.element.poffer.PhotoBlock;
import ru.auto.tests.desktop.element.poffer.PhotoEditor;
import ru.auto.tests.desktop.element.poffer.PriceBlock;
import ru.auto.tests.desktop.element.poffer.ProgressBar;
import ru.auto.tests.desktop.element.poffer.ProvenOwnerBanner;
import ru.auto.tests.desktop.element.poffer.PtsBlock;
import ru.auto.tests.desktop.element.poffer.RunBlock;
import ru.auto.tests.desktop.element.poffer.SafeDealBlock;
import ru.auto.tests.desktop.element.poffer.UserVas;
import ru.auto.tests.desktop.element.poffer.VasLanding;
import ru.auto.tests.desktop.element.poffer.VinBlock;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface PofferPage extends BasePage, WithSelect, WithGeoSuggest, WithButton, WithCheckbox, WithRadioButton {

    @Name("Блок СТС/VIN на первом шаге формы")
    @FindBy("//div[contains(@class, 'section_type_first-step')]")
    FirstStepStsVinBlock firstStepStsVinBlock();

    @Name("Форма")
    @FindBy("//form")
    VertisElement form();

    @Name("Форма выбора марки-модели-модификации")
    @FindBy("//div[contains(@class, 'section_type_mmm')]")
    Block mmmForm();

    @Name("Блок «Модификация»")
    @FindBy("//div[contains(@class, 'modifications-list_visible')]")
    Block modificationBlock();

    @Name("Хлебные крошки")
    @FindBy("//div[contains(@class, 'mmm-line ')]")
    Breadcrumbs breadcrumbs();

    @Name("Блок выбора марки")
    @FindBy("//div[contains(@class, 'marks-list')]")
    MarkBlock markBlock();

    @Name("Блок выбора модели")
    @FindBy("//div[contains(@class, 'models-list')]")
    ModelBlock modelBlock();

    @Name("Блок «Год выпуска»")
    @FindBy("//div[contains(@class, 'years-list_visible')]")
    Block yearBlock();

    @Name("Блок «Кузов»")
    @FindBy("//div[contains(@class, 'bodies_visible')]")
    Block bodyBlock();

    @Name("Блок «Поколение»")
    @FindBy("//div[contains(@class, 'generations-list')]")
    GenerationsBlock generationBlock();

    @Name("Блок «Двигатель»")
    @FindBy("//div[contains(@class, 'engines_visible')]")
    Block engineBlock();

    @Name("Блок «Привод»")
    @FindBy("//div[contains(@class, 'drives_visible')]")
    Block driveBlock();

    @Name("Блок «Коробка передач»")
    @FindBy("//div[contains(@class, 'gearboxes_visible')]")
    Block gearboxBlock();

    @Name("Блок «Фотографии»")
    @FindBy("//div[contains(@class, 'section_type_photos')]")
    PhotoBlock photoBlock();

    @Name("Блок «Цвет автомобиля»")
    @FindBy("//div[contains(@class, 'section_type_color')]")
    Block colorBlock();

    @Name("Блок «Описание»")
    @FindBy("//div[@class = 'section-content'][.//div[contains(@class, 'section-title')][text() =  'Описание']]")
    DescriptionBlock descriptionBlock();

    @Name("Цвет «{{ color }}»")
    @FindBy(".//label[.//span[contains(@style, '{{ color }}')]]")
    VertisElement color(@Param("color") String color);

    @Step("Получаем цвет с индексом {i}")
    default VertisElement getColor(int i) {
        return colorList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Блок «Пробег»")
    @FindBy("//div[contains(@class, 'section_type_run')]")
    RunBlock runBlock();

    @Name("Блок «Личные данные и место осмотра»")
    @FindBy("//div[contains(@class, 'section_type_contacts')]")
    Block contactsBlock();

    @Name("Блок «Цена»")
    @FindBy("//div[contains(@class, 'price') and contains(@class, 'i-bem')]")
    PriceBlock priceBlock();

    @Name("Блок «Наличие»")
    @FindBy("//div[contains(@class, 'availability')]")
    Block availabilityBlock();

    @Name("Блок «Паспорт транспортного средства»")
    @FindBy("//div[@class = 'section'][2]")
    PtsBlock ptsBlock();

    @Name("Блок «Госномер и VIN / номер кузова»")
    @FindBy("//div[@class = 'section'][1]")
    VinBlock vinBlock();

    @Name("Опции")
    @FindBy(".//div[contains(@data-bem, '\"options\"')]")
    Options options();

    @Name("Спарсенные из описания опции")
    @FindBy(".//div[contains(@class, 'description-options')]")
    VertisElement parsedOptions();

    @Name("Блок «Комплектация»")
    @FindBy("//div[contains(@class, 'options i-bem')]")
    ComplectationBlock complectationBlock();

    @Name("Выпадушка с вариантами описания")
    @FindBy(".//div[contains(@class, 'popup-suggest__suggestions')]")
    CommentSuggest commentSuggest();

    @Name("Блок «Опции»")
    @FindBy("//div[contains(@class, 'options i-bem')]")
    Options optionsBlock();

    @Name("Фото-редактор")
    @FindBy("//div[contains(@class, 'photos-list_edit')]")
    PhotoEditor photoEditor();

    @Name("Образец фото")
    @FindBy("//div[contains(@class, 'quick-sale-badges-snippet__snippet-photo')]")
    VertisElement photoExample();

    @Name("Стикеры быстрой продажи")
    @FindBy("//div[contains(@class, 'quick-sale-badges__badges-header')]")
    BadgesBlock badges();

    @Name("Открытый чат «Помощник осмотра»")
    @FindBy("div[contains(@class, 'support-chat ')]")
    VertisElement openAssistant();

    @Name("Развёрнутый блок «{{ class }}»")
    @FindBy("//div[contains(@class, '{{ class }}')]")
    Block unfoldedBlock(@Param("class") String Class);

    @Name("Логотип марки")
    @FindBy("//div[contains(@class, 'mmm-line__logo')]")
    VertisElement markLogo();

    @Name("Блок услуг для частника")
    @FindBy(".//div[contains(@class, 'section_type_vas')] | " +
            ".//div[contains(@class, 'VasForm')]")
    UserVas userVas();

    @Name("Блок услуг для дилера")
    @FindBy(".//div[contains(@class, 'VasFormDealer')]")
    DealerVas dealerVas();

    @Name("Плашка про бан пользователя")
    @FindBy("//div[contains(@class, 'banned')]")
    BannedMessage bannedMessage();

    @Name("Пакет услуг «{{ text }}»")
    @FindBy("//div[contains(@class, 'vas-pack ')]//div[.= '{{ text }}']")
    VertisElement vasPack(@Param("text") String text);

    @Name("Кнопка «Разместить»")
    @FindBy("//div[contains(@class, 'vas-submit ')] | " +
            "//button[contains(@class, 'VasFormUserFooter__button')] | " +
            "//button[contains(@class, 'VasFormDealerSubmit__button')] | " +
            ".//div[contains(@class, 'VasFormUserSnippet ')][3]//button[contains(@class, 'VasFormUserSnippet__button')]")
    VertisElement submitButton();

    @Name("Блок выбора МММ")
    @FindBy("//div[contains(@class, 'section_type_mmm')]")
    BlockMMM blockMMM();

    @Name("Блок выбора '{{ name }}'")
    @FindBy("//div[contains(@class, 'section-title')][contains(., '{{ name }}')]")
    VertisElement blockParameters(@Param("name") String name);

    @Name("Блок фото и видео")
    @FindBy("//div[contains(@class, 'section_type_photos')]")
    PhotoBlock blockPhotoVideo();

    @Name("Блок с качеством объявления")
    @FindBy("//div[contains(@class, 'progress-bar')]")
    ProgressBar progressBar();

    @Name("Кнопка вызова онлайн-помощника")
    @FindBy("//div[contains(@class, 'support-chat__open-link')]")
    VertisElement onlineHelperButton();

    @Name("Онлайн-помощник")
    @FindBy("//iframe[contains(@class, 'support-chat__window')]")
    VertisElement onlineHelper();

    @Name("Блок «N человек подключили опцию»")
    @FindBy("//div[contains(@class, 'random-vas-offers')]")
    VertisElement randomVas();

    @Name("Список цветов")
    @FindBy(".//div[contains(@class, 'color-selector')]" +
            "//label[contains(@class, 'radio_type_line')]")
    ElementsCollection<VertisElement> colorList();

    @Name("Баннер «Станьте проверенным собственником»")
    @FindBy("//div[contains(@class, 'ProvenOwner')]")
    ProvenOwnerBanner provenOwnerBanner();

    @Name("Лендинг VAS")
    @FindBy("//div[contains(@class, 'vas-landing')]")
    VasLanding vasLanding();

    @Name("Чекбокс «Разместить в разделе Авто.ру Эксперт»")
    @FindBy("//label[contains(@class, 'autoru-expert__checkbox')]")
    VertisElement autoruExpertCheckbox();

    @Name("Таймер скидки")
    @FindBy("//div[@class = 'VasFormUserTimer']")
    VertisElement discountTimer();

    @Name("Персональный ассистент продажи")
    @FindBy("//div[contains(@class, 'SaleAssistantPromo')]")
    VertisElement personalAssistant();

    @Name("Мультипостинг")
    @FindBy("//div[contains(@class, 'multiposting')]")
    Multiposting multiposting();

    @Name("Поп-ап с найденными по VIN опциями")
    @FindBy("//div[contains(@class, 'Curtain__container_open')]")
    AddOptionsPopup addOptionsPopup();

    @Name("Блок безопасной сделки")
    @FindBy("//div[contains(@class, 'safe-deal')]")
    SafeDealBlock safeDealBlock();

    @Name("Баннер аукциона")
    @FindBy("//div[@class = 'C2bAuctionBanner']")
    AuctionBanner auctionBanner();

    @Name("Список ошибок полей валидации аукциона")
    @FindBy("//div[contains(@class, 'error-text') and not(contains(@class, 'no-complectation-exception'))]")
    ElementsCollection<VertisElement> aucValidationErrorsList();
}