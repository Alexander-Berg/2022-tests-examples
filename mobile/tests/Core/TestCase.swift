import AutoMate
import MarketRegionSelectFeature
import MarketUITestMessaging
import MarketUITestMocks
import Metrics
import XCTest

class TestCase: XCTestCase {

    /// Используются при прошагивании шагов авторизации
    struct LoginCredentials {

        let login: String
        let password: String

        static let yandexCredentials = LoginCredentials(
            login: "yandex-team-96857.75256",
            password: "F03k.Y7Ym"
        )

        /*
         "account": {
             "country": "ru",
             "delete_at": "None",
             "firstname": "Default-\u0418\u043c\u044f",
             "language": "ru",
             "lastname": "Default \u0424\u0430\u043c\u0438\u043b\u0438\u044f",
             "login": "yandex-team-91656.95801",
             "password": "wgMk.J4yv",
             "uid": "1337085646"
         },
         "passport_environment": "production",
         "saved": true,
         "status": "ok"
         Проблемы с Я.Плюс решаются в чатике: https://t.me/joinchat/BsZR6QmF-ZGRkiwvKZiGjQ
         */
        static let yandexPlusCredentials = LoginCredentials(
            login: "yandex-team-91656.95801",
            password: "wgMk.J4yv"
        )
    }

    var app = XCUIApplication()

    /// Пользователь, который будет подменяться в АМ приложения
    var user: UserAuthState { .unauthorized }

    /// Локаль, которая будет форситься в приложении при его запуске перед каждым кейсом.
    static var testLocale = Locale(identifier: "ru_RU")

    override func setUp() {
        super.setUp()

        continueAfterFailure = false

        app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITests] = String(true)

        let additionalArguments: [TestLaunchArguments] = [
            .clearKeychain,
            .clearFiles,
            .clearUserDefaults,
            .completeOnboarding
        ]
        app.launchArguments += additionalArguments.map { $0.rawValue }

        app = TestLauncher(options: [
            SystemLanguages([.Russian]),
            SystemLocale(localeIdentifier: TestCase.testLocale.identifier),
            SoftwareKeyboards([.RussianRussia, .EnglishUnitedStates])
        ]).configure(app)

        addUIInterruptionMonitor(withDescription: "SCW") { alert -> Bool in
            if alert.buttons["Not Now"].exists {
                alert.buttons["Not Now"].tap()
                return true
            }

            return false
        }
    }

    @discardableResult
    func appWithOnboarding() -> RootPage {
        if let index = app.launchArguments.firstIndex(of: TestLaunchArguments.completeOnboarding.rawValue) {
            app.launchArguments.remove(at: index)
        }

        app.launchArguments.append(TestLaunchArguments.restoreOnboarding.rawValue)
        app.launch()

        let root = RootPage(element: app)

        return root
    }

    /// Запускает приложение и скипает онбординг.
    @discardableResult
    func appAfterOnboardingAndPopups() -> RootPage {
        app.launch()

        let root = RootPage(element: app)

        if root.onboarding.element.exists {
            skipOnboarding()
        }

        app.activate()
        app.tap()

        return root
    }

    /// Перезапуск приложения
    func relaunchApp(clearData: Bool = true) {
        app.terminate()

        let clearDataArguments: [TestLaunchArguments] = [
            .clearKeychain,
            .clearFiles,
            .clearUserDefaults
        ]

        for argument in clearDataArguments {
            if clearData {
                if !app.launchArguments.contains(argument.rawValue) {
                    app.launchArguments.insert(argument.rawValue, at: 0)
                }
            } else {
                if let index = app.launchArguments.firstIndex(of: argument.rawValue) {
                    app.launchArguments.remove(at: index)
                }
            }
        }
    }

    /// Свернуть и развернуть приложение
    func collapseAndExpandApp() {
        XCUIDevice.shared.press(.home)
        app.activate()
    }

    /// Закрыть алерт SWC
    func closeSWCAlert(_ element: XCUIElement) {
        "Close Shared web credentials alert".ybm_run { _ in
            element.tap() // just tap on random element for closing SWC alert
        }
    }

    /// Закрытие системного алерта
    func dismissAlert() {
        XCTContext.runActivity(named: "Dismiss alert") { _ in
            let monitor = addUIInterruptionMonitor(withDescription: "System Alert") { alert in
                alert.buttons["Don’t Allow"].tap()
                return true
            }
            removeUIInterruptionMonitor(monitor)
        }
    }

    /// Проверка открытия ссылки по тапу в вебвью
    func openLinkAndCloseWebview(
        _ link: XCUIElement,
        expectedTitle: String
    ) {
        link.tap()

        let webview = WebViewPage.current
        ybm_wait { webview.element.isVisible }
        ybm_wait { webview.navigationBar.element.identifier == expectedTitle }

        XCTAssertTrue(webview.navigationBar.closeButton.isVisible)
        webview.navigationBar.closeButton.tap()
    }

    /// Открывает экран с мордой из дефолтного мока
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану морды
    @discardableResult
    func goToMorda(root: RootPage? = nil) -> MordaPage {
        let root = root ?? appAfterOnboardingAndPopups()

        let tabBar = root.tabBar
        let mordaTabItem = tabBar.mordaTabItem

        wait(forVisibilityOf: mordaTabItem.element)
        mordaTabItem.tap()

        let morda = tabBar.mordaPage
        wait(forVisibilityOf: morda.element)

        return morda
    }

    /// Метод для перехода в промо хаб
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану промо хаба
    @discardableResult
    func goToPromoHub(root: RootPage? = nil) -> PromoHubPage {
        let morda = goToMorda(root: root)

        let promoHub = morda.promoHubButton.tap()
        wait(forVisibilityOf: promoHub.element)

        return promoHub
    }

    /// Метод для перехода в каталог по таббару
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану каталога
    @discardableResult
    func goToCatalog(root: RootPage? = nil) -> CatalogPage {
        let root = root ?? appAfterOnboardingAndPopups()

        let tabBar = root.tabBar
        let catalogTabItem = tabBar.catalogTabItem

        wait(forVisibilityOf: catalogTabItem.element)

        catalogTabItem.tap()

        let catalog = tabBar.catalogPage
        wait(forVisibilityOf: catalog.element)

        return catalog
    }

    /// Открывает экран департамента с каталога
    ///
    /// - Parameters:
    ///     - fromCatalog: Путь к контейнеру каталога
    ///     - indexCell: Индекс департамента в каталоге
    /// - Returns: Путь к экрану департамента
    func goToDepartament(fromCatalog catalog: CatalogPage, atIndex index: Int = 1) -> SubcategoryPage {

        let someDepartment = catalog.departmentCell(at: IndexPath(item: index, section: 0))
        ybm_wait(forFulfillmentOf: { someDepartment.element.isVisible })

        let subcategory = someDepartment.tap()
        ybm_wait(forFulfillmentOf: { subcategory.element.isVisible })

        return subcategory
    }

    /// Открывает экран категории с департамента
    ///
    /// - Parameters:
    ///     - fromCatalog: Путь к контейнеру департамента
    ///     - indexCell: Индекс категории в департаменте
    /// - Returns: Путь к экрану категории
    func goToSubcategory(fromDepartment department: SubcategoryPage, atIndex index: Int = 1) -> SubcategoryPage {

        let subcategoryCell = department.subcategoryTreeCell(index: index)
        ybm_wait(forFulfillmentOf: { subcategoryCell.element.isVisible })
        subcategoryCell.element.tap()

        let subcategory = SubcategoryPage.current
        ybm_wait(forFulfillmentOf: { subcategory.element.isVisible })

        return subcategory
    }

    /// Открывает экран с пустой корзиной
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану пустой корзины
    func goToEmptyCart(root: RootPage? = nil) -> EmptyCartPage {
        let root = root ?? appAfterOnboardingAndPopups()

        let tabBar = root.tabBar
        let cartTabItem = tabBar.cartTabItem

        wait(forVisibilityOf: cartTabItem.element)
        cartTabItem.tap()

        let cart = tabBar.emptyCartPage
        wait(forVisibilityOf: cart.collection.element)

        return cart
    }

    /// Открывает экран с новой корзиной
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану новой корзины
    @discardableResult
    func goToCart(root: RootPage? = nil) -> CartPage {
        let root = root ?? appAfterOnboardingAndPopups()

        let tabBar = root.tabBar
        let cartTabItem = tabBar.cartTabItem

        wait(forVisibilityOf: cartTabItem.element)
        cartTabItem.tap()

        let cart = tabBar.cartPage
        wait(forVisibilityOf: cart.collectionView)

        return cart
    }

    /// Переход в "Профиль"
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану профиля
    @discardableResult
    func goToProfile(root: RootPage? = nil) -> ProfilePage {
        let root = root ?? appAfterOnboardingAndPopups()
        let tabBar = root.tabBar

        let profileTabItem = tabBar.profileTabItem
        wait(forVisibilityOf: profileTabItem.element)

        let profile = tabBar.profilePage
        ybm_wait {
            guard !profile.element.isVisible else { return true }

            profileTabItem.tap()
            return false
        }

        return profile
    }

    /// Метод для поиска товара в поиске сверху
    ///
    /// - Parameter text: текст для поиска товара, без \\n
    /// - Returns: страницу с результатами поиска
    func goToFeed(root: RootPage? = nil, with text: String = "abcsd") -> FeedPage {
        let morda = goToMorda(root: root)

        let searchPage = morda.searchButton.tap()

        ybm_wait(forVisibilityOf: [searchPage.navigationBar.searchTextField])

        searchPage.navigationBar.searchTextField.tap()
        searchPage.navigationBar.searchTextField.typeText(text + "\n")

        let feedElement = XCUIApplication().otherElements[FeedAccessibility.root]

        wait(forVisibilityOf: feedElement)

        return FeedPage(element: feedElement)
    }

    /// Переход в "Экспресс"
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану экспресса
    @discardableResult
    func goToExpress(root: RootPage? = nil) -> ExpressPage {
        let root = root ?? appAfterOnboardingAndPopups()
        let tabBar = root.tabBar

        let expressTabItem = tabBar.expressTabItem
        wait(forVisibilityOf: expressTabItem.element)
        expressTabItem.tap()

        let expressPage = tabBar.expressPage
        ybm_wait { expressPage.element.isVisible }

        return expressPage
    }

    /// Открывает экран с карточкой SKU из дефолтного мока (тапает первый элемент сниппета истории).
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`.
    /// - Returns: Путь к экрану SKU.
    @discardableResult
    func goToDefaultSKUPage(root: RootPage? = nil) -> SKUPage {
        let morda = goToMorda(root: root)

        let widget = morda.historyWidget
        wait(forVisibilityOf: widget.element)

        let snippet = widget.container.cellPage(at: IndexPath(item: 0, section: 0))
        widget.collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.element)

        let sku = snippet.tap()
        wait(forExistanceOf: sku.element)

        return sku
    }

    /// Проходит авторизацию на уже открытом экране АМ.
    ///
    /// - Parameter data: данные для входа, при отсутствии будут использованы данные по умолчанию.
    func completeAuthFlow(with credentials: LoginCredentials = .yandexCredentials) {

        // Проверяем, не перекинулись ли мы случайно на экран "Выберите аккаунт"
        let authAddAccountButton = app.buttons["Добавить аккаунт"].firstMatch
        if authAddAccountButton.exists {
            wait(forVisibilityOf: authAddAccountButton)
            authAddAccountButton.tap()
        }

        let yandexLogin = YandexLoginPage(element: app.webViews.firstMatch)

        // Достаем текстфилд логина
        let login = yandexLogin.login
        login.element.shouldExist()

        // Закрываем SWC
        closeSWCAlert(login.element)

        // После закрытия SWC алерта ждем visiblity текстфилда
        ybm_wait(forVisibilityOf: [login.element])

        // Вводим логин
        login.typeText(credentials.login)

        // Нажимаем кнопку "Войти"
        let next = yandexLogin.next
        next.tap()
        ybm_recoverableWait(
            forFulfillmentOf: { yandexLogin.password.element.isVisible },
            timeout: 2,
            recoverBlock: { next.tap() },
            recoversCount: 2
        )

        // Достаем текстфилд пароля
        let password = yandexLogin.password
        password.element.shouldExist()

        // Вводим пароль
        password.typeText(credentials.password)
        ybm_wait { password.element.text.count == credentials.password.count }

        // Отказываемся от сохранения пароля
        dismissAlert()

        // Нажимаем кнопку "Войти"
        next.tap()
        yandexLogin.element.tap()
        ybm_wait(forFulfillmentOf: { !password.element.exists })

        // Нажимаем кнопку "Нет"
        let dontSaveButton = app.buttons["Нет"].firstMatch
        ybm_wait(forVisibilityOf: [dontSaveButton])
        dontSaveButton.shouldExist()
        dontSaveButton.tap()

        ybm_wait(forFulfillmentOf: { !dontSaveButton.exists })
    }

    /// Открывает экран моих заказов
    ///
    /// - Parameters:
    ///   - root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`.
    /// - Returns: POM мои заказов
    func goToOrdersListPage(root: RootPage? = nil) -> OrdersListPage {
        let profilePage = goToProfile(root: root)

        let ordersListPage = profilePage.myOrders.tap()
        ybm_wait(forFulfillmentOf: {
            ordersListPage.element.isVisible
        })

        return ordersListPage
    }

    /// Метод для перехода на экран купонов
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану купонов
    @discardableResult
    func goToMyBonuses(root: RootPage? = nil) -> SmartshoppingPage {
        let profilePage = goToProfile(root: root)

        let myBonuses = profilePage.myBonuses.tap()
        ybm_wait(forFulfillmentOf: {
            myBonuses.element.isVisible
        })

        return myBonuses
    }

    /// Открывает экран с новыми деталями по заказу
    ///
    /// - Parameters:
    ///   - root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`.
    ///   - orderId: Номер заказа
    /// - Returns: POM деталей заказа
    func goToOrderDetailsPage(root: RootPage? = nil, orderId: String) -> OrderDetailsPage {
        let ordersListPage = goToOrdersListPage(root: root)
        let detailsButton = ordersListPage.detailsButton(orderId: orderId)

        ordersListPage.element.ybm_swipeCollectionView(toFullyReveal: detailsButton.element)

        let orderDetailsPage = detailsButton.tap()
        ybm_wait(forFulfillmentOf: {
            orderDetailsPage.element.isVisible
        })

        return orderDetailsPage
    }

    /// Переход в "Настройки"
    func goToSettings(root: RootPage? = nil, profile: ProfilePage? = nil) -> SettingsPage {
        let profile = profile ?? goToProfile(root: root)
        wait(forVisibilityOf: profile.element)

        profile.collectionView.ybm_swipeCollectionView(
            toFullyReveal: profile.settings.element,
            inset: .zero
        )

        let cell = profile.settings
        return cell.tap()
    }

    /// Метод открывает safari, печатает передаваемый диплинк и осуществляет переход по нему
    ///
    /// - Parameters:
    ///   - deeplink: url диплинка
    func goToDeeplink(deeplink: String) {
        // Открываем Safari
        let safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
        safari.launch()

        // Ждем, пока не запустится safari
        _ = safari.wait(for: .runningForeground, timeout: 10)

        XCTAssertTrue(safari.isVisible)

        // Скипаем экран "What's new" если есть
        let firstLaunchContinueButton = safari.buttons["Continue"]
        if firstLaunchContinueButton.exists {
            firstLaunchContinueButton.tap()
        }

        let urlButton = safari.textFields["TabBarItemTitle"]
        if urlButton.exists {
            urlButton.tap()
        }

        // Скипаем Keyboard Tutorial
        let safariContiuneButtonQuery = safari.buttons.matching(identifier: "Continue")
        if safariContiuneButtonQuery.count == 2 {
            let keyboardTutorialContinueButton = safariContiuneButtonQuery.element(boundBy: 1)
            keyboardTutorialContinueButton.tap()
        }

        // Вставляем в поисковую строку диплинк и нажимаем на return
        safari.typeText(deeplink)
        safari.typeText("\n")

        // Нажимаем на "Открыть" для перехода в приложение
        let openButton = safari.buttons.matching(identifier: "Open").firstMatch
        wait(forVisibilityOf: openButton)
        openButton.tap()

        // Ждем, пока приложение не откроется
        _ = app.wait(for: .runningForeground, timeout: 10)
    }

    private static let appMessagingDir: URL = {
        // Генерим разные директории, т.к гоняем тесты параллельно.
        let baseURL = FileManager().temporaryDirectory
        let uniqueDir = UUID().uuidString
        return baseURL.appendingPathComponent(uniqueDir)
    }()

    let appMessagingService: AppMessagingService = AppMessagingServiceImpl(
        baseURL: TestCase.appMessagingDir
    )

    func open(
        market link: Link
    ) {
        let state = XCUIApplication().state
        switch state {
        case .runningForeground, .runningBackground:
            XCUIApplication().activate()

        case .unknown, .notRunning, .runningBackgroundSuspended:
            _ = appAfterOnboardingAndPopups()

        @unknown default:
            assertionFailure("Unexpected state value: \(state.rawValue)")
            _ = appAfterOnboardingAndPopups()
        }

        XCTContext.runActivity(named: "Открываем URL '\(link.rawValue)'") { _ in
            appMessagingService.openURL(beru: link)
        }
    }

    /**
     Метод, который заходит на страницу настроек из root и меняет город в секции
     "Настройки", удаляет предыдущий выбранный город и печатает новый в textField

     Город должен быть в выпадающем списке городов (т.е валидным), иначе кнопка "Готово"
     не сработает. Json _UserSuggests_ не удалять!

     - Parameter root: страница, на которой происходят переходы
     - Parameter for city: город, который будет установлен
     */
    func changeCity(root: RootPage? = nil, for city: String) {
        let profile = goToProfile(root: root)

        let cell = profile.settings
        wait(forExistanceOf: cell.element)

        cell.element.tap()

        let cityCell = app.any.matching(identifier: SettingsAccessibility.regionValue).firstMatch
        wait(forExistanceOf: cityCell)

        cityCell.tap()

        let townTextField = app.textFields.firstMatch
        let clearButton = app.buttons.matching(identifier: RegionSelectAccessibility.regionInputClearButton).firstMatch

        wait(forExistanceOf: townTextField)
        wait(forExistanceOf: clearButton)

        clearButton.tap()
        townTextField.typeText(city)

        let buttonDone = app.buttons.matching(identifier: RegionSelectAccessibility.doneChoosingRegionButton).firstMatch
        wait(forExistanceOf: buttonDone)
        buttonDone.tap()
    }

    /// Метод используется в корзине, свайпает корзину до указанного айтема и меняет его количество
    ///
    /// - Parameters:
    ///   - cart: Корзина
    ///   - pickerWheelValue: Количество товара которое мы хотим выставить
    ///   - itemIndex: Индекс товара количество которого хотим поменять
    ///   - swipeDirection: Направление свайпа (зависит от того где находится тест в данный момент)
    func changeQuantityCartItem(
        cart: CartPage,
        pickerWheelValue: String,
        itemIndex: Int,
        swipeDirection: SwipeDirection
    ) {
        let pickerWheel = cart.pickerWheel
        cart.element.ybm_swipeCollectionView(to: swipeDirection, toFullyReveal: cart.cartItem(at: itemIndex).element)
        cart.cartItem(at: itemIndex).countPicker.tap()

        wait(forVisibilityOf: pickerWheel)

        pickerWheel.adjust(toPickerWheelValue: pickerWheelValue)
        cart.countPickerDoneButton.tap()

        wait(forInvisibilityOf: pickerWheel)
    }

    /// Метод используется для скролла к элементу коллекции на странице и выполнения проверки
    ///
    /// - Parameters:
    ///   - page: Страница с коллекцией
    ///   - element: Элемент, к которому скроллим
    ///   - check: Проверка
    func swipeAndCheck(
        page: XCUIElement,
        element: XCUIElement,
        check: (XCUIElement) -> Void
    ) {
        page.ybm_swipeCollectionView(toFullyReveal: element)
        check(element)
    }

    // MARK: - Private

    private func skipOnboarding(toNotifications: Bool = false) {
        let root = RootPage(element: app)

        let welcomeOnboarding = root.onboarding

        wait(forVisibilityOf: welcomeOnboarding.geoCell)
        welcomeOnboarding.geoActionButton.tap()

        let popup = UnlockCoinPopupPage.currentPopup
        if popup.element.waitForExistence(timeout: 10) {
            popup.continueButton.tap()
        }
    }
}

class LocalMockTestCase: TestCase {

    // MARK: - Properties

    var mockServer: LocalMockServer?
    var mockStateManager: LocalMockStateManager?
    var stateManager: LocalStateManager?
    var analyticsServer: AnalyticsEventsServer?

    // MARK: - Lifecycle

    override func setUp() {
        let mockStateManager = LocalMockStateManager()
        self.mockStateManager = mockStateManager

        let stateManager = LocalStateManagerImpl()
        self.stateManager = stateManager

        stateManager.setState(newState: user)

        mockServer = LocalMockServer(
            mockStateManager: mockStateManager,
            stateManager: stateManager
        )
        mockServer?.start()
        setupDefaultMocks()

        analyticsServer = AnalyticsEventsServer()

        MetricRecorder.isRecording = true
        MetricRecorder.clear()
        analyticsServer?.start()

        guard let server = mockServer else { return }
        guard let analytics = analyticsServer else { return }

        let mockUrl = server.serverUrl().absoluteString

        app.launchEnvironment[TestLaunchEnvironmentKeys.deeplinkUrl] = appMessagingService.baseURL.absoluteString
        app.launchEnvironment[TestLaunchEnvironmentKeys.stubAuthorization] = String(user.isLoggedIn)
        app.launchEnvironment[TestLaunchEnvironmentKeys.hasYPlus] = String(user.isYandexPlus)
        app.launchEnvironment[TestLaunchEnvironmentKeys.metricsUrl] = analytics.serverUrl()
            .absoluteString + "logMetrics"
        app.launchEnvironment[TestLaunchEnvironmentKeys.capiUrl] = mockUrl + "market"
        app.launchEnvironment[TestLaunchEnvironmentKeys.fapiUrl] = mockUrl + "api/"
        app.launchEnvironment[TestLaunchEnvironmentKeys.geoSuggestUrl] = mockUrl
        app.launchEnvironment[TestLaunchEnvironmentKeys.iTunesLookupUrl] = mockUrl + "lookup.json"
        app.launchEnvironment[TestLaunchEnvironmentKeys.trustUrl] = mockUrl + "api/"

        // 1. Выключаем редизайн для UI тестов, пока не удалим ветку со старым дизайном
        disable(toggles: FeatureNames.mordaRedesign)

        // TODO: удалить после эксперимента BLUEMARKETAPPS-35015
        disable(toggles: FeatureNames.searchSuggestRedesign)

        enable(toggles: FeatureNames.applePay) //  на случай если НЕТ дырки до Firebase

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        super.setUp()
    }

    override func tearDown() {
        super.tearDown()
        mockServer?.stop()

        MetricRecorder.clear()

        mockStateManager = nil
        stateManager = nil
        mockServer = nil
    }

    // MARK: - Public

    /// Stub Feature Toggles for test envorinment
    /// - Parameter toggles: Use FeatureNames static variables
    func enable(toggles: String...) {
        var disabledToggles = existingToggles(key: TestLaunchEnvironmentKeys.disabledToggles)
        toggles.forEach { disabledToggles.remove($0) }
        var enabledToggles = existingToggles(key: TestLaunchEnvironmentKeys.enabledToggles)
        toggles.forEach { enabledToggles.insert($0) }
        setToggles(
            key: TestLaunchEnvironmentKeys.enabledToggles,
            value: enabledToggles
        )
        setToggles(
            key: TestLaunchEnvironmentKeys.disabledToggles,
            value: disabledToggles
        )
    }

    func disable(toggles: String...) {
        var disabledToggles = existingToggles(key: TestLaunchEnvironmentKeys.disabledToggles)
        toggles.forEach { disabledToggles.insert($0) }
        var enabledToggles = existingToggles(key: TestLaunchEnvironmentKeys.enabledToggles)
        toggles.forEach { enabledToggles.remove($0) }
        setToggles(
            key: TestLaunchEnvironmentKeys.enabledToggles,
            value: enabledToggles
        )
        setToggles(
            key: TestLaunchEnvironmentKeys.disabledToggles,
            value: disabledToggles
        )
    }

    /// Открываем диплинк перехода на выдачу, ходим в `resolveUrlTransform`
    /// - Parameter search: текст поиска на выдаче
    /// - Returns: страница выдачи
    func open(search: String) -> FeedPage {
        /// deeplink
        open(market: .feed(text: search))

        let feedPage = FeedPage.current
        ybm_wait(forVisibilityOf: [feedPage.element])

        return feedPage
    }

    // MARK: - Private

    private func existingToggles(key: String) -> Set<String> {
        guard let value = app.launchEnvironment[key] else { return [] }
        let splitValue = value.split(separator: ",").map(String.init)
        return Set<String>(splitValue)
    }

    private func setToggles(key: String, value: Set<String>) {
        app.launchEnvironment[key] = value.joined(separator: ",")
    }

    private func setupDefaultMocks() {
        let skuCMSMatchingRule = MockMatchRule(
            id: "SKU_CMS",
            matchFunction:
            isPOSTRequest &&
                isFAPIRequest &&
                hasExactFAPIResolvers(["resolveCms"]) &&
                hasStringInBody("\"type\":\"mp_product_card_app\""),
            mockName: "POST_api_v1_resolveCms_SKU"
        )

        mockServer?.addRule(skuCMSMatchingRule)

        let imagesCMSMatchingRule = MockMatchRule(
            id: "NAVIGATION_IMAGES_CMS",
            matchFunction:
            isPOSTRequest &&
                isFAPIRequest &&
                hasExactFAPIResolvers(["resolveCms"]) &&
                hasStringInBody("\"type\":\"mp_navigation_node_images\""),
            mockName: "POST_api_v1_resolveCms_navigation_images"
        )

        mockServer?.addRule(imagesCMSMatchingRule)
    }
}
