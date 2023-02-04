import XCTest

// MARK: - Общее

extension UIElementProviderWitness where T == SwitchCell {
    /// Ячейка с свитчером
    static let switchCell = Self()
}

extension UIElementProviderWitness where T == StylizedButton {
    /// Стандартная кнопка с заголовком и опциональным подзаголовком (AutoRuViews.StylizedButton)
    static let stylizedButton = Self()
}

extension UIElementProviderWitness where T == GenericModalPopup {
    /// Произвольный попап
    static let genericModalPopup = Self()
}

extension UIElementProviderWitness where T == NavigationBar {
    /// Общий навбар
    static let navBar = Self()
}

extension UIElementProviderWitness where T == ContextTooltipMenu {
    /// Контекстное меню над текстом (Скопировать/Вставить/...)
    static let contextMenuTooltip = Self()
}

// MARK: - Пикер категории транспорта

extension UIElementProviderWitness where T == TransportCategoryPicker {
    ///  Пикер трех видов транспорта
    static let categoryPicker = Self()
}

// MARK: - Авторизация

extension UIElementProviderWitness where T == LoginScreen_ {
    /// Авторизация
    static let loginScreen = Self()
}

extension UIElementProviderWitness where T == CodeInputScreen {
    /// Ввод кода подтверждения авторизации
    static let codeInputScreen = Self()
}

extension UIElementProviderWitness where T == LinkPhoneScreen {
    /// Привязка телефона
    static let linkPhoneScreen = Self()
}

// MARK: - Профиль

extension UIElementProviderWitness where T == UserProfileScreen_ {
    /// Профиль
    static let userProfileScreen = Self()
}

extension UIElementProviderWitness where T == WalletScreen_ {
    /// Кошелёк
    static let walletScreen = Self()
}

extension UIElementProviderWitness where T == UserOfferSnippetActiveActionsCell {
    static let userOfferActiveActionsSnippetCell = Self()
}

// MARK: - Звонки

extension UIElementProviderWitness where T == CallOptionPicker {
    /// Пикер телефонных номеров
    static let callOptionPicker = Self()
}

extension UIElementProviderWitness where T == CallScreen {
    static let callScreen = Self()
}

// MARK: - Гараж

extension UIElementProviderWitness where T == GarageScreen_ {
    /// Таб Гараж
    static let garageScreen = Self()
}

extension UIElementProviderWitness where T == GarageListingScreen_ {
    /// Листинг гаража
    static let garageListingScreen = Self()
}

extension UIElementProviderWitness where T == GarageLandingScreen_ {
    /// Лендинг гаража
    static let garageLanding = Self()
}

extension UIElementProviderWitness where T == GarageCardScreen_ {
    static let garageCardScreen = Self()
}

extension UIElementProviderWitness where T == GarageFormScreen_ {
    static let garageFormScreen = Self()
}

extension UIElementProviderWitness where T == GaragePromoPopup {
    /// Промо в гаражной карточке
    static let garagePromoPopup = Self()
}

extension UIElementProviderWitness where T == GarageAddNewCardInSwipeScreen {
    /// Экран добавления карточки в свайпе раздела гаража
    static let garageAddNewCardInSwipeScreen = Self()
}

extension UIElementProviderWitness where T == AddCarScreen {
    /// Экран добавления в Гараж
    static let garageAddCarScreen = Self()
}

extension UIElementProviderWitness where T == GarageSearchScreen {
    /// Добавление в гараж по ГРЗ или VIN
    static let garageSearchScreen = Self()
}

extension UIElementProviderWitness where T == RegionPickerScreen {
    ///  Пикер региона
    static let regionPickerScreen = Self()
}

extension UIElementProviderWitness where T == GarageAllPromosScreen {
    ///  Экран "Все акции" в гараже
    static let garageAllPromosScreen = Self()
}

// MARK: - Экран характеристики

extension UIElementProviderWitness where T == SpecificationsScreen {
    static let specificationsScreen = Self()
}

// MARK: - Пикеры визарда (гараж, отзывы)

extension UIElementProviderWitness where T == WizardVINPicker {
    /// Пикер VIN
    static let wizardVINPicker = Self()
}

extension UIElementProviderWitness where T == WizardGovNumberPicker {
    /// Пикер ГРЗ
    static let wizardGovNumberPicker = Self()
}

extension UIElementProviderWitness where T == WizardMarkPicker {
    /// Пикер марки
    static let wizardMarkPicker = Self()
}

extension UIElementProviderWitness where T == WizardModelPicker {
    /// Пикер модели
    static let wizardModelPicker = Self()
}

extension UIElementProviderWitness where T == WizardYearPicker {
    /// Пикер года
    static let wizardYearPicker = Self()
}

extension UIElementProviderWitness where T == WizardGenerationPicker {
    /// Пикер поколения
    static let wizardGenerationPicker = Self()
}

extension UIElementProviderWitness where T == WizardBodyTypePicker {
    /// Пикер типа кузова
    static let wizardBodyTypePicker = Self()
}

extension UIElementProviderWitness where T == WizardEngineTypePicker {
    /// Пикер двигателя
    static let wizardEngineTypePicker = Self()
}

extension UIElementProviderWitness where T == WizardDriveTypePicker {
    /// Пикер привода
    static let wizardDriveTypePicker = Self()
}

extension UIElementProviderWitness where T == WizardTransmissionTypePicker {
    /// Пикер коробки
    static let wizardTransmissionTypePicker = Self()
}

extension UIElementProviderWitness where T == WizardModificationPicker {
    /// Пикер модификации
    static let wizardModificationPicker = Self()
}

extension UIElementProviderWitness where T == WizardColorPicker {
    /// Пикер цвета
    static let wizardColorPicker = Self()
}

extension UIElementProviderWitness where T == WizardOwnersCountPicker {
    /// Пикер владельцев
    static let wizardOwnersCountPicker = Self()
}

extension UIElementProviderWitness where T == WizardPTSPicker {
    /// Пикер оригинала птс
    static let wizardPTSPicker = Self()
}

extension UIElementProviderWitness where T == WizardPhotoPicker {
    /// Пикер фоток
    static let wizardPhotoPicker = Self()
}

extension UIElementProviderWitness where T == WizardDescriptionPanoramaPicker {
    /// Пикер панорам
    static let wizardDescriptionPanoramaPicker = Self()
}

extension UIElementProviderWitness where T == WizardDescriptionPicker {
    /// Пикер описания оффера
    static let wizardDescriptionPicker = Self()
}

extension UIElementProviderWitness where T == WizardMileagePicker {
    /// Пикер пробега
    static let wizardMileagePicker = Self()
}

extension UIElementProviderWitness where T == WizardPhonesPicker {
    /// Пикер телефонов
    static let wizardPhonesPicker = Self()
}

extension UIElementProviderWitness where T == WizardContactsPicker {
    /// Пикер контактов
    static let wizardContactsPicker = Self()
}

extension UIElementProviderWitness where T == WizardProvenOwnerPicker {
    /// Пикер проверенного собственника
    static let wizardProvenOwnerPicker = Self()
}

extension UIElementProviderWitness where T == WizardPricePicker {
    /// Пикер цены
    static let wizardPricePicker = Self()
}

extension UIElementProviderWitness where T == WizardOwningTimePicker {
    /// Пикер срока владения в отзывах
    static let wizardOwningTimePicker = Self()
}

extension UIElementProviderWitness where T == WizardPlusesPicker {
    /// Пикер плюсов в отзывах
    static let wizardPlusesPicker = Self()
}

extension UIElementProviderWitness where T == WizardMinusesPicker {
    /// Пикер минусов в отзывах
    static let wizardMinusesPicker = Self()
}

extension UIElementProviderWitness where T == WizardAppearancePicker {
    /// Пикер внешность в отзывах
    static let wizardAppearancePicker = Self()
}

extension UIElementProviderWitness where T == WizardComfortPicker {
    /// Пикер комфорт в отзывах
    static let wizardComfortPicker = Self()
}

extension UIElementProviderWitness where T == WizardSafetyPicker {
    /// Пикер безопасность в отзывах
    static let wizardSafetyPicker = Self()
}

extension UIElementProviderWitness where T == WizardDriveabilityPicker {
    /// Пикер управляемость в отзывах
    static let wizardDriveabilityPicker = Self()
}

extension UIElementProviderWitness where T == WizardReliabilityPicker {
    /// Пикер надежность в отзывах
    static let wizardReliabilityPicker = Self()
}

extension UIElementProviderWitness where T == GarageWizardContainer {
    static let garageWizardContainer = Self()
}

// MARK: - Страховки

extension UIElementProviderWitness where T == InsuranceRowCell {
    static let insuranceRowCell = Self()
}

extension UIElementProviderWitness where T == InsuranceCardScreen {
    static let insuranceCardScreen = Self()
}

extension UIElementProviderWitness where T == InsuranceFormScreen {
    static let insuranceFormScreen = Self()
}

extension UIElementProviderWitness where T == GarageParametersListScreen {
    /// Список параметров карточки
    static let garageParametersListScreen = Self()
}

// MARK: - Вебвью

extension UIElementProviderWitness where T == WebViewPicker {
    static let webViewPicker = Self()
}

extension UIElementProviderWitness where T == WebControllerScreen_ {
    static let webControllerScreen = Self()
}

// MARK: - Сниппет листинга

extension UIElementProviderWitness where T == OfferSnippet {
    /// Сниппет оффера
    static let offerSnippet = Self()
}

// MARK: - Гео чипсы

extension UIElementProviderWitness where T == GeoRadiusBubbleItem {
    /// Чипс с гео-радиусом
    static let geoRadiusBubbleItem = Self()
}

extension UIElementProviderWitness where T == GeoRadiusBubblesCell {
    /// Чипсы с гео-радиусом
    static let geoRadiusBubblesCell = Self()
}

// MARK: - Пикер вариантов трейдин

extension UIElementProviderWitness where T == TradeInPicker {
    /// Пикер выкупа в трейд-ин после размещения
    static let tradeInPicker = Self()
}

extension UIElementProviderWitness where T == TradeInPickerOptionCell {
    /// Опция в пикере трейд-ина
    static let tradeInOptionCell = Self()
}

// MARK: - Морда

extension UIElementProviderWitness where T == MainScreen_ {
    /// Морда (без указания таба)
    static let mainScreen = Self()
}

extension UIElementProviderWitness where T == TransportScreen {
    /// Таб Транспорт морды
    static let transportScreen = Self()
}

extension UIElementProviderWitness where T == CreditsTabScreen {
    /// Таб Кредиты морды
    static let creditsTabScreen = Self()
}

extension UIElementProviderWitness where T == JournalTabScreen {
    /// Таб Журнал морды
    static let journalTabScreen = Self()
}

extension UIElementProviderWitness where T == ReviewsTabScreen {
    /// Таб Отзывы морды
    static let reviewsTabScreen = Self()
}

extension UIElementProviderWitness where T == StoriesCarouselCell {
    /// Карусель со сторисами
    static let storiesCarouselCell = Self()
}

extension UIElementProviderWitness where T == UserOfferStatBubble {
    /// Баббл со статистикой оффера
    static let userOfferStatBubble = Self()
}

extension UIElementProviderWitness where T == OfferPriceScreen_ {
    /// Боттомшит цены
    static let offerPriceScreen = Self()
}

extension UIElementProviderWitness where T == PhonesListPopupScreen {
    /// Боттомшит выбора телефона для трейд-ина
    static let phonesListPopupScreen = Self()
}

extension UIElementProviderWitness where T == TradeInOfferPickerScreen {
    /// Боттомшит  выбора оффера юзера для трейд-ина
    static let tradeInOfferPickerScreen = Self()
}

extension UIElementProviderWitness where T == AutoruOnlyPopupScreen {
    /// Попап с информацией про бейдж Только на Авто.ру
    static let autoruOnlyPopupScreen = Self()
}

extension UIElementProviderWitness where T == OfferPriceGarageCarPickerScreen {
    /// Боттомшит выбора авто из гаража
    static let offerPriceGarageCarPickerScreen = Self()
}

// MARK: - Чаты

extension UIElementProviderWitness where T == ChatScreen_ {
    /// Экран чата
    static let chatScreen = Self()
}

extension UIElementProviderWitness where T == ChatsScreen_ {
    /// Список чатов
    static let chatsScreen = Self()
}

extension UIElementProviderWitness where T == AttachmentPicker {
    /// Новый пикер аттачей
    static let attachmentPicker = Self()
}

extension UIElementProviderWitness where T == PHPicker {
    /// Фото пикер
    static let phPicker = Self()
}

extension UIElementProviderWitness where T == ChatReportWidgetCell {
    /// Сообщение-виджет об отчете
    static let chatReportWidgetCell = Self()
}

extension UIElementProviderWitness where T == ChatAntifraudWidgetCell {
    /// Сообщение-виджет антифрод
    static let chatAntifraudWidgetCell = Self()
}

extension UIElementProviderWitness where T == ChatIncomingMessageCell {
    /// Сообщение входящее
    static let chatIncomingMessageCell = Self()
}

extension UIElementProviderWitness where T == ChatOutcomingMessageCell {
    /// Сообщение исходящее
    static let chatOutcomingMessageCell = Self()
}

extension UIElementProviderWitness where T == ChatInputBar {
    /// Бар снизу в чате
    static let chatInputBar = Self()
}

extension UIElementProviderWitness where T == ChatOfferPanel {
    /// Панель оффера в чате
    static let chatOfferPanel = Self()
}

extension UIElementProviderWitness where T == ChatUserPanel {
    /// Панель юзера в чате
    static let chatUserPanel = Self()
}

// MARK: - Кредиты

extension UIElementProviderWitness where T == CreditBannerPopup {
    /// Попап с кредитным калькулятором
    static let creditBannerPopup = Self()
}

extension UIElementProviderWitness where T == CreditLKScreen {
    /// Список кредитов
    static let creditLKScreen = Self()
}

extension UIElementProviderWitness where T == CreditWizardScreen {
    /// Кредитный визард
    static let creditWizardScreen = Self()
}

extension UIElementProviderWitness where T == CreditFormScreen {
    /// Кредитная форма
    static let creditFormScreen = Self()
}

// MARK: - Фильтры

extension UIElementProviderWitness where T == FiltersScreen_ {
    static let filtersScreen = Self()
}

// MARK: - Фуллскрин галерея

extension UIElementProviderWitness where T == GalleryScreen {
    static let galleryScreen = Self()
}

// MARK: - Геопикер

extension UIElementProviderWitness where T == GeoRegionPicker {
    static let geoRegionPicker = Self()
}

extension UIElementProviderWitness where T == GeoRegionPickerCell {
    static let geoRegionPickerCell = Self()
}

// MARK: - Сторис

extension UIElementProviderWitness where T == StoryScreen {
    /// Сторис
    static let storyScreen = Self()
}

// MARK: - Супер-меню

extension UIElementProviderWitness where T == SuperMenuScreen {
    /// Новое главное меню для недилеров
    static let superMenuScreen = Self()
}

extension UIElementProviderWitness where T == SuperMenuHeaderCell {
    /// Верхняя ячейка нового меню
    static let superMenuHeaderCell = Self()
}

extension UIElementProviderWitness where T == SuperMenuPromoCell {
    /// Промо-ячейка нового меню
    static let superMenuPromoCell = Self()
}

extension UIElementProviderWitness where T == EstimateFormScreen {
    /// Оценка автомобиля
    static let estimateFormScreen = Self()
}

extension UIElementProviderWitness where T == EstimationResultScreen {
    /// Оценка автомобиля результат
    static let estimateResultScreen = Self()
}

extension UIElementProviderWitness where T == NotificationSettingsScreen {
    /// Настройки уведомлений
    static let notificationSettingsScreen = Self()
}

extension UIElementProviderWitness where T == AboutScreen {
    /// О приложении
    static let aboutScreen = Self()
}

// MARK: - Карточка

extension UIElementProviderWitness where T == SaleCardScreen_ {
    /// Карточка оффера
    static let saleCardScreen = Self()
}

extension UIElementProviderWitness where T == SafeDealSaleCardCell {
    /// Провязка БС в карточку
    static let safeDealSaleCardCell = Self()
}

extension UIElementProviderWitness where T == SafeDealStatusCell {
    /// Статус БС в карточке сверху
    static let safeDealStatusCell = Self()
}

extension UIElementProviderWitness where T == ActionButtonsCell {
    /// Кнопки действий на карточке
    static let actionButtonsCell = Self()
}

extension UIElementProviderWitness where T == CharacteristicCell {
    /// Характеристики в карточке
    static let characteristicCell = Self()
}

extension UIElementProviderWitness where T == FeedbackModalScreen {
    /// Алерт: модалка заполнения отзыва для незалогина
    static let feedbackScreen = Self()
}

extension UIElementProviderWitness where T == OfferAdvantagesModalPopup {
    /// Попап преимуществ
    static let offerAdvantagesModalPopup = Self()
}

extension UIElementProviderWitness where T == LargeGalleryCell {
    /// Большая галерея с карточки
    static let largeCardGalleryCell = Self()
}

extension UIElementProviderWitness where T == SaleCardBottomContainer {
    /// Контейнер с кнопками внизу
    static let saleCardBottomContainer = Self()
}

// MARK: - Слайды полноэкранной галереи

extension UIElementProviderWitness where T == BestPriceGallerySlide {
    /// Слайд "лучшая цена" в галереи
    static let bestPriceGallerySlide = Self()
}

// MARK: - Листинг

extension UIElementProviderWitness where T == SaleListScreen_ {
    /// Листинг
    static let saleListScreen = Self()
}

// MARK: - Отчёты

extension UIElementProviderWitness where T == CarReportScreen {
    static let carReportScreen = Self()
}

extension UIElementProviderWitness where T == CarReportStandAloneScreen {
    static let carReportStandAloneScreen = Self()
}

extension UIElementProviderWitness where T == CarReportStandAloneCell {
    static let carReportStandAloneCell = Self()
}

extension UIElementProviderWitness where T == CarReportPreviewCell {
    static let carReportPreviewCell = Self()
}

extension UIElementProviderWitness where T == PaymentOptionsScreen_ {
    static let paymentOptionsScreen = Self()
}

// MARK: - Форма размещения

extension UIElementProviderWitness where T == OfferEditScreen_ {
    /// Форма создания/редактирования оффера
    static let offerEditScreen = Self()
}

// MARK: - Действия с оффером

extension UIElementProviderWitness where T == ActionsMenuPopup {
    /// Действия с оффером
    static let actionsMenuPopup = Self()
}

// MARK: - Жалоба

extension UIElementProviderWitness where T == ComplainMenuPopup {
    /// Попап с причиной жалобы
    static let complainMenuPopup = Self()
}

// MARK: - VAS ловушка

extension UIElementProviderWitness where T == VASTrapScreen_ {
    static let vasTrapScreen = Self()
}

// MARK: - ЛК

extension UIElementProviderWitness where T == UserSaleListScreen {
    static let userSaleListScreen = Self()
}

extension UIElementProviderWitness where T == AuctionSnippetActionsCell {
    /// Сниппет аукциона в лк (кнопки)
    static let auctionSnippetActionsCell = Self()
}

// MARK: - Карточка своего оффера

extension UIElementProviderWitness where T == UserSaleCardScreen {
    static let userSaleCardScreen = Self()
}

extension UIElementProviderWitness where T == UserSaleCardOfferVASCell {
    static let userSaleCardOfferVAS = Self()
}

// MARK: - Экран пользовательских отзывов

extension UIElementProviderWitness where T == UserReviewsScreen {
    static let userReviewsScreen = Self()
}

// MARK: - Экран отзывов на фичу авто

extension UIElementProviderWitness where T == ReviewFeatureScreen {
    static let reviewFeatureScreen = Self()
}

// MARK: - Алерт после удаления пользовательского отзыва

extension UIElementProviderWitness where T == UserReviewAlert {
    static let userReviewAlert = Self()
}

// MARK: - Алерт при закрытии визарда отзывов

extension UIElementProviderWitness where T == UserReviewCloseAlert {
    static let userReviewCloseAlert = Self()
}

// MARK: - Алерт при ошибке редактирования отзыва

extension UIElementProviderWitness where T == UserReviewErrorAlert {
    static let userReviewErrorAlert = Self()
}

// MARK: - Алерт при снятии оффера

extension UIElementProviderWitness where T == DeactivateUserOfferAlert {
    static let deactivateUserOfferAlert = Self()
}

// MARK: - Попап "Продам позже"

extension UIElementProviderWitness where T == ActivateSaleLaterPopup {
    static let activateSaleLaterPopup = Self()
}

// MARK: - Попап успех "Объявление будет опубликовано позже"

extension UIElementProviderWitness where T == ActivateSaleLaterSuccessPopup {
    static let activateSaleLaterSuccessPopup = Self()
}

// MARK: - Карточка отзыва

extension UIElementProviderWitness where T == ReviewCardScreen {
    static let reviewCardSreen = Self()
}

extension UIElementProviderWitness where T == ReviewCommentsScreen {
    static let reviewCommentsScreen = Self()
}

// MARK: - Визард создания отзыва

extension UIElementProviderWitness where T == WizardReviewScreen {
    static let wizardReviewScreen = Self()
}

// MARK: - Экран редактирования отзыва

extension UIElementProviderWitness where T == UserReviewEditorScreen {
    static let userReviewEditorScreen = Self()
}

// MARK: - Элементы экрана редактирования отзыва

extension UIElementProviderWitness where T == UserReviewContentEditorCell {
    static let userReviewContentEditorCell = Self()
}

extension UIElementProviderWitness where T == UserOfferSnippetDraftActionsCell {
    static let userOfferDraftActionsSnippetCell = Self()
}

// MARK: - Безопасная сделка

extension UIElementProviderWitness where T == SafeDealSellingPricePopup {
    static let safeDealSellingPricePopup = Self()
}

extension UIElementProviderWitness where T == SafeDealRequestCancelPopup {
    static let safeDealRequestCancelPopup = Self()
}

extension UIElementProviderWitness where T == SafeDealListScreen {
    static let safeDealListScreen = Self()
}

extension UIElementProviderWitness where T == SafeDealSellerConfirmationPopup {
    static let safeDealSellerConfirmationPopup = Self()
}

extension UIElementProviderWitness where T == SafeDealOnboardingPopup {
    static let safeDealOnboardingPopup = Self()
}

extension UIElementProviderWitness where T == SafeDealOverlayPopup {
    static let safeDealOverlayPopup = Self()
}

extension UIElementProviderWitness where T == SafeDealCell {
    static let safeDealCell = Self()
}

// MARK: - Пользовательская заметка в карточке оффера

extension UIElementProviderWitness where T == UserNoteCell {
    static let userNoteCell = Self()
}

// MARK: - Экран ввода пользовательской заметки

extension UIElementProviderWitness where T == UserNoteSсreen {
    static let userNoteSсreen = Self()
}

// MARK: - Алерт после ввода заметки

extension UIElementProviderWitness where T == UserNoteAlert {
    static let userNoteAlert = Self()
}

// MARK: - Таб избранного

extension UIElementProviderWitness where T == FavoritesScreen_ {
    static let favoritesScreen = Self()
}

// MARK: - Онбординг

extension UIElementProviderWitness where T == OnboardingScreen {
    static let onboardingScreen = Self()
}

extension UIElementProviderWitness where T == OnboardingRoleSelectionSlide {
    static let onboardingRoleSelectionSlide = Self()
}

extension UIElementProviderWitness where T == OnboardingSlide {
    static let onboardingSlide = Self()
}

extension UIElementProviderWitness where T == OnboardingNavigationButton {
    static let onboardingNavigationButton = Self()
}

// MARK: - Модальное окно контактов дилера

extension UIElementProviderWitness where T == DealerContactsModal {
    static let dealerСontactsModal = Self()
}

// MARK: - Экран выдачи по конкретному дилеру из карточки оффера

extension UIElementProviderWitness where T == DealerCardScreen_ {
    static let dealerСardScreen = Self()
}

// MARK: - Анимированное модальное меню с кнопками

extension UIElementProviderWitness where T == SimpleButtonsModalMenu {
    static let simpleButtonsModalMenu = Self()
}

// MARK: - Простой системный алерт

extension UIElementProviderWitness where T == AlertController {
    /// Системный UIAlertController, а не один из кастомных классов.
    static let systemAlert = Self()
}

// MARK: - Cнэкбар

extension UIElementProviderWitness where T == Snackbar {
    /// Снэкбар
    static let snackbar = Self()
}

// MARK: - Грид с фотографиями

extension UIElementProviderWitness where T == GaragePhotoGridScreen_ {
    static let photoGrid = Self()
}

// MARK: - Пикеры фильтров

extension UIElementProviderWitness where T == MarkModelPicker {
    static let mmngPicker = Self()
}

extension UIElementProviderWitness where T == OptionPresetPicker {
    static let optionPresetPicker = Self()
}

extension UIElementProviderWitness where T == ComplectationPicker {
    static let complectationPicker = Self()
}

extension UIElementProviderWitness where T == DatePickerScreen_ {
    static let datePicker = Self()
}

extension UIElementProviderWitness where T == RangePicker {
    static let rangePicker = Self()
}

extension UIElementProviderWitness where T == ColorPicker {
    static let colorPicker = Self()
}

extension UIElementProviderWitness where T == TextRangePicker {
    static let textRangePicker = Self()
}

extension UIElementProviderWitness where T == OptionSelectPicker {
    static let optionSelectPicker = Self()
}

extension UIElementProviderWitness where T == SelectPicker {
    static let selectPicker = Self()
}

extension UIElementProviderWitness where T == SegmentSelectPicker {
    static let segmentSelectPicker = Self()
}

extension UIElementProviderWitness where T == MicrophonePermissionScreen {
    /// Шторка с предложением перейти в настройки для включения микрофона
    static let microphonePermissionScreen = Self()
}

extension UIElementProviderWitness where T == SellerCallPermissionIntroScreen {
    /// Шторка с предложением продавцу включить микрофон
    static let sellerCallPermissionIntroScreen = Self()
}

extension UIElementProviderWitness where T == WizardScreen_ {
    /// Визард размещения тачки
    static let wizardScreen = Self()
}

// MARK: - Таб Бар

extension UIElementProviderWitness where T == TabBar {
    static let tabBar = Self()
}

// MARK: - Шаринг меню

extension UIElementProviderWitness where T == ActivityListView {
    static let activityList = Self()
}

// MARK: - Модальный пикер

extension UIElementProviderWitness where T == ModalPicker {
    static let modalPicker = Self()
}

// MARK: - Текст инпут попап

extension UIElementProviderWitness where T == TextReasonPopup {
    static let textReasonPopup = Self()
}

// MARK: - C2B аукцион

extension UIElementProviderWitness where T == AuctionWelcomeScreen {
    static let auctionWelcomeScreen = Self()
}

extension UIElementProviderWitness where T == AuctionClaimScreen {
    static let auctionClaimScreen = Self()
}

extension UIElementProviderWitness where T == AuctionSuccessClaimScreen {
    static let auctionSuccessClaimScreen = Self()
}

extension UIElementProviderWitness where T == AuctionSmallClaimCell {
    /// Провязка аукциона в карточку или ЛК
    static let auctionSmallClaimCell = Self()
}

extension UIElementProviderWitness where T == AuctionBuybackPreviewScreen {
    /// Превью поданной заявки на выкуп (новый флоу)
    static let auctionBuybackPreviewScreen = Self()
}

extension UIElementProviderWitness where T == AuctionPreOffersScreen {
    /// Список преофферов дилеров (новый флоу)
    static let auctionPreOffersScreen = Self()
}

extension UIElementProviderWitness where T == DealerPreOfferCell {
    /// Преоффер дилера (новый флоу)
    static let dealerPreOfferCell = Self()
}

extension UIElementProviderWitness where T == AuctionInspectionConfirmationScreen {
    /// Подтверждение заявки (новый флоу)
    static let auctionInspectionConfirmationScreen = Self()
}

extension UIElementProviderWitness where T == AuctionWaitManagerCallScreen {
    /// Успех поданной заявки (новый флоу)
    static let auctionWaitManagerCallScreen = Self()
}

// MARK: - Групповая карточка

extension UIElementProviderWitness where T == StockCardScreen_ {
    static let stockCardScreen = Self()
}

// MARK: - Васы в ЛК

extension UIElementProviderWitness where T == UserSaleListOfferVASCell {
    static let userSaleListOfferVAS = Self()
}

extension UIElementProviderWitness where T == UserSaleListOfferVASHeaderCell {
    static let userSaleListOfferVASHeader = Self()
}

// MARK: - карусель с васами

extension UIElementProviderWitness where T == VASDescriptionContainerScreen {
    static let vasDescriptionContainerScreen = Self()
}

// MARK: - карточка в карусели с васами

extension UIElementProviderWitness where T == VASDescriptionCard {
    static let vasDescriptionCard = Self()
}

// MARK: - Электромобили

extension UIElementProviderWitness where T == ElectroCarsMainScreen {
    static let electroCarsMainScreen = Self()
}

// MARK: - Пикер фото для проверки документов

extension UIElementProviderWitness where T == PhotoSetScreen {
    static let photoSetScreen = Self()
}

// MARK: - Листинг отзывов

extension UIElementProviderWitness where T == ReviewListScreen {
    static let reviewListScreen = Self()
}

// MARK: - Преимущества на карточке

extension UIElementProviderWitness where T == AdvantagesCell {
    static let advantagesCell = Self()
}

// MARK: - Профиль профессионального продавца

extension UIElementProviderWitness where T == PublicProfileScreen {
    static let publicProfileScreen = Self()
}

extension UIElementProviderWitness where T == PublicProfilePromoBanner {
    static let publicProfilePromoBanner = Self()
}

extension UIElementProviderWitness where T == PublicProfileOnboardingPopup {
    static let publicProfileOnboardingPopup = Self()
}

extension UIElementProviderWitness where T == PublicProfileTooltipPopup {
    static let publicProfileTooltipPopup = Self()
}

extension UIElementProviderWitness where T == PublicProfileSuccessPopup {
    static let publicProfileSuccessPopup = Self()
}

extension UIElementProviderWitness where T == PublicProfileSwitchSnippet {
    static let publicProfileSwitchSnippet = Self()
}
