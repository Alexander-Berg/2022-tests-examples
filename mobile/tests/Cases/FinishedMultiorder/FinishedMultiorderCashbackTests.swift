import MarketUITestMocks
import XCTest

final class FinishedMultiorderCashbackTests: OrderWithCashbackFlow {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    override func setUp() {
        super.setUp()
        disable(toggles: FeatureNames.cartRedesign)
        enable(toggles: FeatureNames.showPlus)
    }

    func testNoPlusNoBonusesForOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3784")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Кэшбэк для НЕПЛЮСОВИКА")
        Allure.addTitle("Заказ без кэшбэка. Тир1")

        let finishPage = makeOrder(with: "FinishedMultiorderNoPlusNoBonus")

        "Смотрим текст на блоке кешбэка на последнем шаге".ybm_run { _ in
            wait(forVisibilityOf: finishPage.plusBadgeText)
            finishPage.element.swipe(to: .down, untilVisible: finishPage.plusBadgeText)
            XCTAssertEqual(
                finishPage.plusBadgeText.label,
                "Подключите Плюс — это кешбэк баллами,\nбесплатная доставка заказов от 699 ₽\n(зависит от города) и многое другое"
            )
        }

        "Проверяем наличие ссылки и нажимаем на нее".ybm_run { _ in
            finishPage.element.swipe(to: .down, untilVisible: finishPage.plusBadgeLink)
            XCTAssertEqual(
                finishPage.plusBadgeLink.label,
                "Читать про Плюс"
            )
            finishPage.plusBadgeLink.tap()
        }

        "Ждем открытия дома плюса".ybm_run { _ in
            wait(forExistanceOf: HomePlusPage.current.element)
        }
    }

    func testNoPlusWithBonusesForOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3785")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Кэшбэк для НЕПЛЮСОВИКА")
        Allure.addTitle("Заказ с кэшбэком. Тир1")

        checkSubscribeCaseWithBonuses(
            beforeMock: "FinishedMultiorderNoPlusWithBonus",
            beforeText: "Баллы придут вместе с заказом.\nЕсли подключите Плюс, сможете\nих тратить и получите бесплатную доставку",
            afterMock: "FinishedMultiorderPlusWithBonus",
            afterText: "Баллы придут вместе с заказом.\nПотратьте их на следующую покупку."
        )
    }

    func testNoPlusNoBonusesForOrderTier3() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3784")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Кэшбэк для НЕПЛЮСОВИКА")
        Allure.addTitle("Заказ без кэшбэка. Тир3")

        let finishPage = makeOrder(with: "FinishedMultiorderNoPlusNoBonus_Tier3")

        "Смотрим текст на блоке кешбэка на последнем шаге".ybm_run { _ in
            wait(forVisibilityOf: finishPage.plusBadgeText)
            finishPage.element.swipe(to: .down, untilVisible: finishPage.plusBadgeText)
            XCTAssertEqual(
                finishPage.plusBadgeText.label,
                "Подключите Плюс — это кешбэк баллами\nна Маркете и в других сервисах Яндекса,\nа ещё подписка на кино и музыку"
            )
        }

        "Проверяем наличие ссылки и нажимаем на нее".ybm_run { _ in
            finishPage.element.swipe(to: .down, untilVisible: finishPage.plusBadgeLink)
            XCTAssertEqual(
                finishPage.plusBadgeLink.label,
                "Читать про Плюс"
            )
            finishPage.plusBadgeLink.tap()
        }

        "Ждем открытия дома плюса".ybm_run { _ in
            wait(forExistanceOf: HomePlusPage.current.element)
        }
    }

    func testNoPlusWithBonusesForOrderTier3() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4038")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Кэшбэк для НЕПЛЮСОВИКА")
        Allure.addTitle("Заказ с кэшбэком. Тир3")

        checkSubscribeCaseWithBonuses(
            beforeMock: "FinishedMultiorderNoPlusWithBonus_Tier3",
            beforeText: "Баллы придут вместе с заказом.\nЕсли подключите Плюс,\nсможете их тратить",
            afterMock: "FinishedMultiorderPlusWithBonus_Tier3",
            afterText: "Баллы придут вместе с заказом.\nПотратьте их на следующую покупку."
        )

    }

    func testNoPlusWithBonusesSummaryInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4039")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Кэшбэк для НЕПЛЮСОВИКА")
        Allure.addTitle("Заказ с кэшбэком. Инфо в \"Подробности\"")

        setCashBackDetailsState()

        let finishPage = makeOrder(with: "FinishedMultiorderNoPlusWithBonus")

        "Открываем детальную информацию".ybm_run { _ in
            finishPage.element.swipe(to: .down, untilVisible: finishPage.detailSection)
            finishPage.detailSection.tap()
        }

        "Ищем информацию о кешбэке".ybm_run { _ in
            finishPage.element.swipe(to: .down, untilVisible: finishPage.cashbackSummary)
            XCTAssertEqual(
                finishPage.cashbackSummary.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "39 баллов"
            )
        }
    }

    func testPlusSpendBonusesOnFinishCheckout() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4233")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Кэшбэк")
        Allure.addTitle("Списание баллов")

        enable(toggles: FeatureNames.plusBenefits)

        var root: RootPage!
        var cart: CartPage!
        var checkoutPage: CheckoutPage!
        var finishPage: FinishMultiorderPage!

        setState()

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FinishedMultiorderPlusWithBonusSpend")
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Идем в корзину".ybm_run { _ in
            cart = goToCart(root: root)
            wait(forExistanceOf: cart.compactSummary.orderButton.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cart.compactSummary.orderButton.tap()
        }

        "Смотрим что с текущим способом оплаты списание не доступно".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.cashbackSpendOptionCell.element)
            XCTAssertEqual(checkoutPage.cashbackSpendOptionCell.title.label, "Cписание недоступно")
        }

        "Меняем способ оплаты на поддерживающий кешбэк".ybm_run { _ in
            checkoutPage.element.swipe(to: .up, untilVisible: checkoutPage.paymentMethodCell.element)
            let paymentMethodPage = checkoutPage.paymentMethodCell.tap()
            paymentMethodPage.selectPaymentMethod(with: "YANDEX")
            paymentMethodPage.continueButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
        }

        "Смотрим что с текущим способом оплаты списание доступно".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.cashbackSpendOptionCell.element)

            XCTAssertEqual(checkoutPage.cashbackSpendOptionCell.title.label, "Списать")

            checkoutPage.cashbackSpendOptionCell.element.tap()
        }

        "Меняем оплату, так как картой оплатить не получается в тесте".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            let paymentMethodPage = checkoutPage.paymentMethodCell.tap()
            paymentMethodPage.selectPaymentMethod(with: "CASH_ON_DELIVERY")
            paymentMethodPage.continueButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
        }

        "Смотрим текст на блоке кешбэка на последнем шаге".ybm_run { _ in
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            finishPage = checkoutPage.paymentButton.tap()

            wait(forVisibilityOf: finishPage.element)

            finishPage.element.swipe(to: .down, untilVisible: finishPage.plusBadgeText)
            XCTAssertEqual(
                finishPage.plusBadgeText.label,
                "Отлично! Вы сэкономили 18 ₽ на покупке\nблагодаря Плюсу."
            )
        }
    }

    private func checkSubscribeCaseWithBonuses(
        beforeMock: String,
        beforeText: String,
        afterMock: String,
        afterText: String
    ) {
        app.launchEnvironment[TestLaunchEnvironmentKeys.enablePlusSubscription] = String(true)

        setCashBackDetailsState()

        let finishPage = makeOrder(with: beforeMock)

        "Смотрим текст на блоке кешбэка на последнем шаге когда пользователь не плюсовик".ybm_run { _ in
            wait(forVisibilityOf: finishPage.plusBadgeText)
            finishPage.element.swipe(to: .down, untilVisible: finishPage.plusBadgeText)
            XCTAssertEqual(
                finishPage.plusBadgeText.label,
                beforeText
            )
        }

        "Проверяем наличие ссылки и нажимаем на нее".ybm_run { _ in
            finishPage.element.swipe(to: .down, untilVisible: finishPage.plusBadgeLink)
            XCTAssertEqual(
                finishPage.plusBadgeLink.label,
                "Читать про Плюс"
            )
        }

        "Открываем дом плюса".ybm_run { _ in
            finishPage.plusBadgeLink.tap()
            wait(forExistanceOf: HomePlusPage.current.element)
        }

        "Мокаем подписку плюса при закрытии дома плюса".ybm_run { _ in
            mockStateManager?.pushState(bundleName: afterMock)
        }

        "Закрываем дом плюса".ybm_run { _ in
            HomePlusPage.current.element.swipeDown()
            wait(forExistanceOf: finishPage.element)
        }

        "Смотрим текст на блоке кешбэка на последнем шаге когда пользователь плюсовик".ybm_run { _ in
            wait(forVisibilityOf: finishPage.plusBadgeText)
            finishPage.element.swipe(to: .down, untilVisible: finishPage.plusBadgeText)
            XCTAssertEqual(
                finishPage.plusBadgeText.label,
                afterText
            )
        }

        "Открываем детальную информацию".ybm_run { _ in
            finishPage.element.swipe(to: .down, untilVisible: finishPage.detailSection)
            finishPage.detailSection.tap()
        }

        "Ищем информацию о кешбэке".ybm_run { _ in
            finishPage.element.swipe(to: .down, untilVisible: finishPage.cashbackSummary)
            XCTAssertEqual(
                finishPage.cashbackSummary.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "39 баллов"
            )
        }
    }

    private func setCashBackDetailsState() {
        var state = MultiorderCashbackDetailsState()
        state.setOrdersCashbackDetails(with: 39, orderId: 32_753_872)
        stateManager?.setState(newState: state)
    }
}
