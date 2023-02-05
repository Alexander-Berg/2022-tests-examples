import MarketUITestMocks
import XCTest

final class GrowingCashbackTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    override func setUp() {
        super.setUp()
        stateManager?.mockingStrategy = .dtoMock
        enable(toggles: FeatureNames.mordaRedesign)
    }

    func testWidgetOnMordaActivePromo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5288")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Главная")
        Allure.addTitle("Акция активна")

        var morda: MordaPage!
        var snippet: HoveringSnippetPage!

        "Мокаем стейт".ybm_run { _ in
            mockGrowingCashback(statusHandler: .activePromo, stagesHandler: .base)
            mockMordaState()
        }

        "Авторизуемся, открываем главную".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Проверяем наличие и содержимое виджета".ybm_run { _ in
            snippet = morda.singleActionContainerWidget.container.growingCashbackWidget.snippet
            ybm_wait(forVisibilityOf: [snippet.element])
            XCTAssertEqual(snippet.titleLabel.label, "Получите до 1 550 баллов")
            XCTAssertEqual(snippet.subtitleLabel.label, "за первые 3 заказа в приложении")
            XCTAssertEqual(snippet.actionButton.element.label, "Подробнее")
        }

        "Нажимаем на кнопку получить, проверяем открытие экрана акции".ybm_run { _ in
            let page: GrowingCashbackPromoPage = snippet.actionButton.tap()
            ybm_wait(forVisibilityOf: [page.element])
        }
    }

    func testWidgetOnMordaFinishedPromo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5289")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Главная")
        Allure.addTitle("Акция активна, но завершилась для пользователя")

        var root: RootPage!
        var morda: MordaPage!
        var profile: ProfilePage!
        var snippet: HoveringSnippetPage!

        "Мокаем стейт".ybm_run { _ in
            mockGrowingCashback(statusHandler: .finishedPromo)
            mockMordaState()
        }

        "Авторизуемся, открываем главную".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Проверяем наличие и содержимое виджета".ybm_run { _ in
            snippet = morda.singleActionContainerWidget.container.growingCashbackWidget.snippet
            ybm_wait(forVisibilityOf: [snippet.element])
            XCTAssertEqual(snippet.titleLabel.label, "Получите до 1 550 баллов")
            XCTAssertEqual(snippet.subtitleLabel.label, "за первые 3 заказа в приложении")
            XCTAssertEqual(snippet.actionButton.element.label, "Подробнее")
            XCTAssertEqual(snippet.additionalActionButton.element.label, "Закрыть")
        }

        "Тапаем на закрыть, проверяем скрытие виджета".ybm_run { _ in
            snippet.additionalActionButton.element.tap()
            ybm_wait(forFulfillmentOf: { !snippet.element.isVisible })
        }

        "Переходим в профиль".ybm_run { _ in
            profile = goToProfile(root: root)
            ybm_wait(forVisibilityOf: [profile.element])
        }

        "Проверяем отсутствие виджета в профиле".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { !profile.growingCashback.element.exists })
        }
    }

    func testWidgetInProfileActivePromo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5290")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Блок в профиле")
        Allure.addTitle("Акция активна")

        var profile: ProfilePage!
        var growingCashbackCell: ProfilePage.GrowingCashbackCell!

        "Мокаем стейт".ybm_run { _ in
            mockGrowingCashback(statusHandler: .activePromo, stagesHandler: .base)
        }

        "Авторизуемся, переходим в профиль".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            ybm_wait(forVisibilityOf: [profile.element])
        }

        "Проверяем наличие и содержимое блока".ybm_run { _ in
            growingCashbackCell = profile.growingCashback
            XCTAssertTrue(growingCashbackCell.iconImageView.exists)
            XCTAssertEqual(growingCashbackCell.title.label, "Получите до 1 550 баллов")
            XCTAssertFalse(growingCashbackCell.closeButton.isVisible)
        }

        "Тапаем, проверяем что открылся экран акции".ybm_run { _ in
            let growingCashbackPage = growingCashbackCell.tap()
            ybm_wait(forVisibilityOf: [growingCashbackPage.element])
        }
    }

    func testWidgetInProfileFinishedPromo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5291")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Блок в профиле")
        Allure.addTitle("Акция активна, но завершилась для пользователя")

        var root: RootPage!
        var profile: ProfilePage!
        var morda: MordaPage!
        var growingCashbackCell: ProfilePage.GrowingCashbackCell!

        "Мокаем стейт".ybm_run { _ in
            mockGrowingCashback(statusHandler: .finishedPromo)
            mockMordaState()
        }

        "Авторизуемся, переходим в профиль".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            ybm_wait(forVisibilityOf: [profile.element])
        }

        "Проверяем наличие и содержимое блока".ybm_run { _ in
            growingCashbackCell = profile.growingCashback
            XCTAssertTrue(growingCashbackCell.iconImageView.exists)
            XCTAssertEqual(growingCashbackCell.title.label, "Получите до 1 550 баллов")
            XCTAssertTrue(growingCashbackCell.closeButton.isVisible)
        }

        "Тапаем на крестик, проверяем скрытие виджета".ybm_run { _ in
            growingCashbackCell.closeButton.tap()
            ybm_wait(forFulfillmentOf: { !growingCashbackCell.closeButton.isVisible })
        }

        "Переходим на главную".ybm_run { _ in
            morda = goToMorda(root: root)
            ybm_wait(forVisibilityOf: [morda.element])
        }

        "Проверяем отсутствие виджета на главной".ybm_run { _ in
            let snippet = morda.singleActionContainerWidget.container.growingCashbackWidget.snippet
            ybm_wait(forFulfillmentOf: { !snippet.element.isVisible })
        }
    }

    func testEntryPointsPromoIsOver() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5292")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Точки входа")
        Allure.addTitle("Акция неактивна")

        var root: RootPage!
        var morda: MordaPage!
        var profile: ProfilePage!

        "Мокаем стейт".ybm_run { _ in
            mockGrowingCashback(statusHandler: .unactivePromo)
            mockMordaState()
        }

        "Авторизуемся, открываем главную".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
            ybm_wait(forVisibilityOf: [morda.element])
        }

        "Проверяем, что виджет на главной не отображается".ybm_run { _ in
            let snippet = morda.singleActionContainerWidget.container.growingCashbackWidget.snippet
            ybm_wait(forFulfillmentOf: { !snippet.element.exists })
        }

        "Открываем профиль".ybm_run { _ in
            profile = goToProfile(root: root)
            ybm_wait(forVisibilityOf: [profile.element])
        }

        "Проверяем, что блок в профиле не отображается".ybm_run { _ in
            let growingCashbackCell = profile.growingCashback
            ybm_wait(forFulfillmentOf: { !growingCashbackCell.element.exists })
        }
    }

    func testWidgetInCartOrderAmountIsNotEnoughForPromo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5296")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Блок в корзине")
        Allure.addTitle("Акция активна. Суммы заказа недостаточно для получения кешбэка")

        var root: RootPage!
        var cart: CartPage!

        "Мокаем стейт".ybm_run { _ in
            mockCartState(.basic)
        }

        "Авторизуемся, открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cart = goToCart(root: root)
            ybm_wait(forVisibilityOf: [cart.element])
        }

        "Проверяем наличие блока".ybm_run { _ in
            let growingCashbackBlock = cart.advertisingCampaignThreshold
            cart.element.ybm_swipe(toFullyReveal: growingCashbackBlock.element)
            XCTAssertEqual(
                growingCashbackBlock.descriptionText.element.label,
                "Ещё  ﻿﻿ 500 баллов за заказ, если добавите товаров ещё на 900 ₽"
            )
        }
    }

    func testWidgetInCartOrderAmountIsEnoughForPromo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5297")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Блок в корзине")
        Allure.addTitle("Акция активна. Суммы заказа достаточно для получения кешбэка")

        var root: RootPage!
        var cart: CartPage!

        "Мокаем стейт".ybm_run { _ in
            mockCartState(.full)
        }

        "Авторизуемся, открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cart = goToCart(root: root)
            ybm_wait(forVisibilityOf: [cart.element])
        }

        "Проверяем наличие блока".ybm_run { _ in
            let growingCashbackBlock = cart.advertisingCampaignThreshold
            cart.element.ybm_swipe(toFullyReveal: growingCashbackBlock.element)
            XCTAssertEqual(
                growingCashbackBlock.descriptionText.element.label,
                "Ещё  ﻿﻿ 500 баллов за заказ Баллы придут вместе с заказом"
            )
        }
    }

    func testWidgetInProfileFinishedPromoReferralPromocode() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5298")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Блок в профиле")
        Allure.addTitle("Акция активна, пользователь достиг лимита по акции")

        var root: RootPage!
        var profile: ProfilePage!
        var growingCashbackCell: ProfilePage.GrowingCashbackCell!
        var infoScreen: GrowingCashbackInfoPage!

        "Мокаем стейт".ybm_run { _ in
            enable(toggles: FeatureNames.referralProgram)
            mockGrowingCashback(statusHandler: .finishedPromo)
            mockPromocodeState()
        }

        "Авторизуемся, переходим в профиль".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            ybm_wait(forVisibilityOf: [profile.element])
        }

        "Проверяем наличие и содержимое блока".ybm_run { _ in
            growingCashbackCell = profile.growingCashback
            XCTAssertTrue(growingCashbackCell.iconImageView.exists)
            XCTAssertEqual(growingCashbackCell.title.label, "Получите до 1 550 баллов")
            XCTAssertTrue(growingCashbackCell.closeButton.isVisible)
        }

        "Переходим на экран акции".ybm_run { _ in
            growingCashbackCell.element.tap()
            infoScreen = GrowingCashbackInfoPage.current
            ybm_wait(forVisibilityOf: [infoScreen.element])
        }

        "Проверяем отображение информационного экрана".ybm_run { _ in
            XCTAssertEqual(infoScreen.title.label, "Вы получили 1 550 баллов – максимум для этой акции")
            XCTAssertEqual(
                infoScreen.subtitle.label,
                "Порекомендуйте нас друзьям и получите еще  баллов Плюса за каждого"
            )
            XCTAssertEqual(infoScreen.actionButton.label, "Порекомендовать Маркет")
        }

        "Открываем экран с промокодом".ybm_run { _ in
            infoScreen.actionButton.tap()
            let promocodeScreen = ReferralPromocodePage.current
            ybm_wait(forFulfillmentOf: { promocodeScreen.element.exists })
        }
    }

    func testPromoScreenNoOrders() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5364")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Экран акции")
        Allure.addTitle("У пользователя нет заказов по акции")

        var root: RootPage!
        var morda: MordaPage!
        var promoScreen: GrowingCashbackPromoPage!
        let stagesHandler = ResolveGrowingCashbackStages.notStarted

        "Мокаем стейт".ybm_run { _ in
            mockGrowingCashback(statusHandler: .activePromo, stagesHandler: stagesHandler)
            mockMordaState()
        }

        "Авторизуемся, открываем главную".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
            ybm_wait(forVisibilityOf: [morda.element])
        }

        "Переходим на экран акции".ybm_run { _ in
            let widget = morda.singleActionContainerWidget.container.growingCashbackWidget.snippet
            promoScreen = widget.actionButton.tap()
            ybm_wait(forVisibilityOf: [promoScreen.element])
        }

        "Проверяем отображение экрана акции".ybm_run { _ in
            checkPromoScreen(promoScreen, stages: stagesHandler.stages)
        }

        "Тапаем на кнопку 'За покупками', переходим на главный экран".ybm_run { _ in
            promoScreen.purchaseButton.tap()
            ybm_wait(forVisibilityOf: [morda.element])
        }
    }

    func testPromoScreenOrderInProgress() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5372")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Экран акции")
        Allure.addTitle("Баллы по акции начислены")

        var root: RootPage!
        var morda: MordaPage!
        var promoScreen: GrowingCashbackPromoPage!
        let stagesHandler = ResolveGrowingCashbackStages.inProgress

        "Мокаем стейт".ybm_run { _ in
            mockGrowingCashback(statusHandler: .activePromo, stagesHandler: stagesHandler)
            mockMordaState()
        }

        "Авторизуемся, открываем главную".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
            ybm_wait(forVisibilityOf: [morda.element])
        }

        "Переходим на экран акции".ybm_run { _ in
            let widget = morda.singleActionContainerWidget.container.growingCashbackWidget.snippet
            promoScreen = widget.actionButton.tap()
            ybm_wait(forVisibilityOf: [promoScreen.element])
        }

        "Проверяем отображение экрана акции".ybm_run { _ in
            checkPromoScreen(promoScreen, stages: stagesHandler.stages)
        }

        "Тапаем на кнопку 'Подробнее', проверяем открытие браузера".ybm_run { _ in
            promoScreen.detailsButton.tap()
            let browser = WebViewPage.current
            ybm_wait(forVisibilityOf: [browser.element])
        }
    }

    func testPromoScreenUserMadeAllOrders() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5374")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Экран акции")
        Allure.addTitle("Пользователь сделал 3 заказа по акции")

        var root: RootPage!
        var morda: MordaPage!
        var infoScreen: GrowingCashbackInfoPage!

        "Мокаем стейт".ybm_run { _ in
            enable(toggles: FeatureNames.referralProgram)
            mockGrowingCashback(statusHandler: .finishedPromo)
            mockMordaState()
            mockPromocodeState()
        }

        "Авторизуемся, открываем главную".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Переходим на экран акции".ybm_run { _ in
            let snippet = morda.singleActionContainerWidget.container.growingCashbackWidget.snippet
            infoScreen = snippet.actionButton.tap()
            ybm_wait(forVisibilityOf: [infoScreen.element])
        }

        "Проверяем отображение информационного экрана".ybm_run { _ in
            XCTAssertEqual(infoScreen.title.label, "Вы получили 1 550 баллов – максимум для этой акции")
            XCTAssertEqual(
                infoScreen.subtitle.label,
                "Порекомендуйте нас друзьям и получите еще  баллов Плюса за каждого"
            )
            XCTAssertEqual(infoScreen.actionButton.label, "Порекомендовать Маркет")
        }

        "Открываем экран с промокодом".ybm_run { _ in
            infoScreen.actionButton.tap()
            let promocodeScreen = ReferralPromocodePage.current
            ybm_wait(forFulfillmentOf: { promocodeScreen.element.exists })
        }
    }

    func testPromoScreenUserMadeAllOrdersWithoutReferralPromocode() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5625")
        Allure.addEpic("Растущий кешбэк")
        Allure.addFeature("Экран акции")
        Allure.addTitle("Пользователь сделал 3 заказа по акции и рефералка для него недоступна")

        var root: RootPage!
        var morda: MordaPage!
        var infoScreen: GrowingCashbackInfoPage!

        "Мокаем стейт".ybm_run { _ in
            mockGrowingCashback(statusHandler: .finishedPromo)
            mockMordaState()
        }

        "Авторизуемся, открываем главную".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Переходим на экран акции".ybm_run { _ in
            let snippet = morda.singleActionContainerWidget.container.growingCashbackWidget.snippet
            infoScreen = snippet.actionButton.tap()
            ybm_wait(forVisibilityOf: [infoScreen.element])
        }

        "Проверяем отображение информационного экрана".ybm_run { _ in
            XCTAssertEqual(infoScreen.title.label, "Вы получили 1 550 баллов – максимум для этой акции")
            XCTAssertEqual(infoScreen.subtitle.label, "Спасибо за участие в акции! Желаем приятных покупок")
            XCTAssertEqual(infoScreen.actionButton.label, "Отлично")
        }

        "Нажимаем на кнопку 'Отлично', проверяем скрытие экрана".ybm_run { _ in
            infoScreen.actionButton.tap()
            ybm_wait(forVisibilityOf: [morda.element])
        }
    }

    // MARK: - Private

    private func mockMordaState() {
        var cmsState = CMSState()
        cmsState.setCMSState(with: .growingCashbackCollections)
        var authState = UserAuthState(isLoggedIn: true, isYandexPlus: true)
        authState.setPlusBalanceState(.withZeroMarketCashback)
        stateManager?.setState(newState: cmsState)
        stateManager?.setState(newState: authState)
        mockStateManager?.pushState(bundleName: "PlusUserMock")
    }

    private func mockGrowingCashback(
        statusHandler: ResolveGrowingCashbackStatus,
        stagesHandler: ResolveGrowingCashbackStages? = nil
    ) {
        let growingCashbackState = GrowingCashbackState(statusHandler: statusHandler, stagesHandler: stagesHandler)
        stateManager?.setState(newState: growingCashbackState)
    }

    private func mockCartState(_ growingCashback: OrderOptionsCashback.GrowingCashback) {
        var state = CartState()
        let userOrderOptions = modify(ResolveUserOrderOptions.UserOrderOptions.basic) {
            let cashback = modify(OrderOptionsCashback.default) {
                $0.growingCashback = growingCashback
            }
            $0.cashback = cashback
        }
        state.setUserOrdersState(with: userOrderOptions)
        state.setCartStrategy(with: [.default1])
        stateManager?.setState(newState: state)

        mockStateManager?.pushState(bundleName: "PlusUserMock")
    }

    private func mockPromocodeState() {
        let statusHandler = ResolveReferralProgramStatus(result: .basic)
        let promocodeHandler = ResolveReferralPromocode(result: .basic)
        let state = ReferralPromocodeState(statusHandler: statusHandler, promocodeHandler: promocodeHandler)
        stateManager?.setState(newState: state)
    }

    private func checkPromoScreen(
        _ promoScreen: GrowingCashbackPromoPage,
        stages: [ResolveGrowingCashbackStages.Stage]
    ) {
        XCTAssertEqual(promoScreen.title.label, "Получите до 1 550 баллов Плюса")
        XCTAssertEqual(promoScreen.inAppLabel.label, "в приложении")
        XCTAssertEqual(promoScreen.date.label, "До 24 февраля")
        XCTAssertEqual(promoScreen.subtitle.label, "Сделайте 3 заказа, каждый от 3 500 ₽")
        let orders = promoScreen.promoOrders
        let stagesCount = stages.count
        XCTAssertEqual(orders.count, stagesCount)
        for index in 0 ..< stagesCount {
            let orderElement = orders[index]
            let stage = stages[index]
            XCTAssertEqual(orderElement.title.label, stage.label)
            // Убираем первые 3 пробельных символа из строки
            XCTAssertEqual(orderElement.cashback.label.dropFirst(3), "\(stage.reward)")
            XCTAssertEqual(orderElement.checkmark.exists, stage.stage == .finished)
            if stage.stage == .inProgress {
                XCTAssertEqual(orderElement.subtitle.label, "Баллы придут вместе с заказом")
            }
        }
        XCTAssertTrue(promoScreen.closeButton.isVisible)
        promoScreen.element.ybm_swipeCollectionView(toFullyReveal: promoScreen.purchaseButton)
        XCTAssertTrue(promoScreen.purchaseButton.isVisible)
        XCTAssertTrue(promoScreen.detailsButton.isVisible)
    }
}
