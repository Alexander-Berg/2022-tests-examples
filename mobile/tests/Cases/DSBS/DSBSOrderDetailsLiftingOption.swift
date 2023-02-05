import MarketUITestMocks
import XCTest

final class DSBSOrderDetailsLiftingOption: LocalMockTestCase {

    func testCheckoutLiftingEmptyFloorInput() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4399")
        Allure.addEpic("DSBS КГТ")
        Allure.addFeature("Чекаут. Подъём на этаж")
        Allure.addTitle("Пустое поле этажа")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var liftingDetailsPage: CheckoutLiftingDetailsPage!

        disable(toggles: FeatureNames.cartRedesign)

        mockStateManager?.pushState(bundleName: "DSBSOrderLiftingDetailsRepurchaseFlow")

        "Открываем корзину и оформляем заказ".ybm_run { _ in

            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
        }

        "Оставляем поле этажа пустым".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.element)

            liftingDetailsPage = checkoutPage.liftingDetailsTypeCell.tap()

            wait(forVisibilityOf: liftingDetailsPage.element)

            liftingDetailsPage.selectLiftingType(with: "elevator")

            liftingDetailsPage.doneButton.tap()

            wait(forVisibilityOf: liftingDetailsPage.errorMessage.element)

            XCTAssertTrue(liftingDetailsPage.element.isVisible)
            XCTAssertEqual(
                liftingDetailsPage.errorMessage.textView.label.value,
                "Напишите этаж, на который нужно поднять заказ"
            )
        }
    }

    func testCheckoutLiftingWithKnownFloor() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4399")
        Allure.addEpic("DSBS КГТ")
        Allure.addFeature("Чекаут. Подъём на этаж")
        Allure.addTitle("Адрес известен")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var liftingDetailsPage: CheckoutLiftingDetailsPage!

        let floor = "7"
        let comment = "не звонить в дверь"

        disable(toggles: FeatureNames.cartRedesign)

        mockStateManager?.pushState(bundleName: "DSBSOrderLiftingDetailsRepurchaseFlow")
        mockStateManager?.pushState(bundleName: "DSBSOrderDetailLiftingOptionOrders")

        "Открываем корзину и оформляем заказ".ybm_run { _ in

            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
        }

        "Добавляем подъём на этаж".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.liftingDetailsTypeCell.title.label,
                "Добавить подъём на \(floor) этаж\nЧтобы не делать этого самостоятельно"
            )

            liftingDetailsPage = checkoutPage.liftingDetailsTypeCell.tap()

            wait(forVisibilityOf: liftingDetailsPage.element)

            liftingDetailsPage.selectLiftingType(with: "elevator")

            XCTAssertTrue(liftingDetailsPage.floorInput.element.exists)
        }

        "Добавляем комментарий".ybm_run { _ in
            liftingDetailsPage.element
                .ybm_swipeCollectionView(toFullyReveal: liftingDetailsPage.commentInput.element)
            liftingDetailsPage.commentInput.typeText(comment)

            KeyboardPage.current.tapDone()
            liftingDetailsPage.doneButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
        }

        checkoutAndSummaryLifting(
            checkoutPage: checkoutPage,
            liftingDetailsTitle: "Подъём на лифте, 150 ₽«‎не звонить в дверь»",
            summaryTitle: "Разгрузка, подъём на этаж (DSBS)",
            summaryDetails: "150 ₽"
        )
    }

    func testCheckoutLifting() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4399")
        Allure.addEpic("DSBS КГТ")
        Allure.addFeature("Чекаут. Подъём на этаж")
        Allure.addTitle("Адрес неизвестен")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        let floor = "7"
        let comment = "не звонить в дверь"

        setupEnvironment(with: "DSBSOrderLiftingDetailsRepurchaseFlow")

        "Открываем корзину и оформляем заказ".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
        }

        addLiftingDetails(
            checkoutPage: checkoutPage,
            liftType: "elevator",
            floor: floor,
            comment: comment
        )

        checkoutAndSummaryLifting(
            checkoutPage: checkoutPage,
            liftingDetailsTitle: "Подъём на лифте, 150 ₽«‎не звонить в дверь»",
            summaryTitle: "Разгрузка, подъём на этаж (DSBS)",
            summaryDetails: "150 ₽"
        )
    }

    func testMyOrdersLiftingDetailsElevator() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4400")
        Allure.addEpic("DSBS КГТ")
        Allure.addFeature("Мои заказы, подъем на этаж")

        var myOrdersPage: OrdersListPage!

        setupEnvironment(with: "DSBSOrderLiftingDetailsRepurchaseFlow")

        addMockMatchRuleForUserOrder(liftType: "cargo", orderId: "32678084")
        addMockMatchRuleForUserOrder(liftType: "elevator", orderId: "32678085")
        addMockMatchRuleForUserOrder(liftType: "manual", orderId: "32678086")
        addMockMatchRuleForUserOrder(liftType: "none", orderId: "32678087")

        "Переходим в мои заказы".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .orders)
            myOrdersPage = OrdersListPage.current
            wait(forVisibilityOf: myOrdersPage.element)
        }

        chekMyOrderLiftDetail(
            ordersPage: myOrdersPage,
            orderId: "32678084",
            liftingDetail: "10 этаж, грузовой лифт, 1 500 ₽",
            liftingPrice: "1 500 ₽"
        )

        chekMyOrderLiftDetail(
            ordersPage: myOrdersPage,
            orderId: "32678085",
            liftingDetail: "3 этаж, пассажирский лифт, 450 ₽",
            liftingPrice: "450 ₽"
        )

        chekMyOrderLiftDetail(
            ordersPage: myOrdersPage,
            orderId: "32678086",
            liftingDetail: "5 этаж, без лифта, 750 ₽",
            liftingPrice: "750 ₽"
        )

        chekMyOrderLiftDetail(
            ordersPage: myOrdersPage,
            orderId: "32678087",
            liftingDetail: "Не включён"
        )
    }

    private func addMockMatchRuleForUserOrder(
        liftType: String,
        orderId: String
    ) {
        let lifting = MockMatchRule(
            id: "\(liftType)Lifting",
            matchFunction:
            isPOSTRequest &&
                isFAPIRequest &&
                hasExactFAPIResolvers(["resolveUserOrderByIdFull"]) &&
                hasStringInBody("\"orderId\":\(orderId)"),
            mockName: "resolveUserOrderByIdFull_\(orderId)"
        )

        mockServer?.addRule(lifting)
    }

    private func checkoutAndSummaryLifting(
        checkoutPage: CheckoutPage,
        liftingDetailsTitle: String,
        summaryTitle: String,
        summaryDetails: String
    ) {
        "Проверяем чекаутер и саммари".ybm_run { _ in
            XCTAssertEqual(
                checkoutPage.liftingDetailsTypeCell.title.label.ybm_singleLineString,
                liftingDetailsTitle
            )

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryTotalCell.element)

            XCTAssertEqual(
                checkoutPage.summaryLiftingCell.title.label,
                summaryTitle
            )

            XCTAssertEqual(
                checkoutPage.summaryLiftingCell.details.label,
                summaryDetails
            )
        }

    }

    private func setupEnvironment(with bundleName: String) {
        disable(toggles: FeatureNames.paymentSDK, FeatureNames.cartRedesign)
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = "55.741"
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = "37.432"

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: bundleName)
        }
    }

    private func chekMyOrderLiftDetail(
        ordersPage: OrdersListPage,
        orderId: String,
        liftingDetail: String,
        liftingPrice: String? = nil
    ) {
        var orderDetailsPage: OrderDetailsPage!

        "Открываем детали заказа".ybm_run { _ in
            let order = ordersPage.detailsButton(orderId: orderId)
            ordersPage.element.ybm_swipeCollectionView(
                toFullyReveal: order.element,
                inset: .init(top: 0, left: 0, bottom: 30, right: 0)
            )

            orderDetailsPage = order.tap()
        }

        "Проверяем подъем на этаж".ybm_run { _ in
            let lifting = orderDetailsPage.lifting

            XCTAssertTrue(lifting.element.exists)
            XCTAssertEqual(
                lifting.title.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "Разгрузка, подъём на этаж"
            )
            XCTAssertEqual(
                lifting.value.label.trimmingCharacters(in: .whitespacesAndNewlines),
                liftingDetail
            )
        }

        if let liftingPrice = liftingPrice {
            "Проверяем summary".ybm_run { _ in
                let liftingSummary = orderDetailsPage.liftingSummaryTitle

                orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: liftingSummary.element)

                XCTAssertTrue(liftingSummary.element.exists)
                XCTAssertEqual(
                    liftingSummary.title.label.trimmingCharacters(in: .whitespacesAndNewlines),
                    "Разгрузка, подъём на этаж"
                )
                XCTAssertEqual(
                    liftingSummary.amount.label.trimmingCharacters(in: .whitespacesAndNewlines),
                    liftingPrice
                )
            }
        }

        NavigationBarPage.current.backButton.tap()
    }

    private func makeFirstFlowCheckout(
        mapViewPage: CheckoutMapViewPage,
        fullAddress: String,
        floor: String
    ) {
        var editAddressPage: EditAddressPage!
        var recipientPage: CheckoutRecipientPage!

        "Выбираем любую точку на карте, нажать привезти сюда".ybm_run { _ in
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            mapViewPage.selectChip(at: 1)

            ybm_wait {
                mapViewPage.summary.addressTextField.text == fullAddress
            }

            editAddressPage = mapViewPage.summary.tap()
            XCTAssertEqual(editAddressPage.addressCellView.label, fullAddress)
        }

        "Заполняем все дополнительные поля".ybm_run { _ in
            editAddressPage.apartmentInput.typeText("1")
            editAddressPage.entranceInput.typeText("1")
            editAddressPage.floorInput.typeText(floor)

            KeyboardPage.current.tapDone()
            editAddressPage.continueButton.tap()

            recipientPage = CheckoutRecipientPage.current
            wait(forVisibilityOf: recipientPage.element)
        }

        "Заполнить все поля, нажать \"продолжить\"".ybm_run { _ in
            recipientPage.nameTextField.typeText("Temik Vzorvish")
            recipientPage.emailTextField.typeText("vzorvanii@tip.ok")
            recipientPage.phoneTextField.typeText("88899900999")

            KeyboardPage.current.tapDone()
            recipientPage.continueButton.tap()
        }
    }

    private func addLiftingDetails(
        checkoutPage: CheckoutPage,
        liftType: String,
        floor: String,
        comment: String? = nil
    ) {
        var liftingDetailsPage: CheckoutLiftingDetailsPage!

        "Добавляем подъём на этаж".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.liftingDetailsTypeCell.title.label,
                "Добавить подъём на этаж\nЧтобы не делать этого самостоятельно"
            )

            liftingDetailsPage = checkoutPage.liftingDetailsTypeCell.tap()

            wait(forVisibilityOf: liftingDetailsPage.element)

            liftingDetailsPage.selectLiftingType(with: liftType)

            wait(forVisibilityOf: liftingDetailsPage.floorInput.element)
        }

        "Заполняем этаж".ybm_run { _ in
            liftingDetailsPage.floorInput.typeText(floor)

            if let comment = comment {
                liftingDetailsPage.element
                    .ybm_swipeCollectionView(toFullyReveal: liftingDetailsPage.commentInput.element)
                liftingDetailsPage.commentInput.typeText(comment)
            }

            liftingDetailsPage.floorInput.element.tap()
            KeyboardPage.current.tapDone()
            liftingDetailsPage.doneButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
        }
    }

    func testCheckoutSelectUnload() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5550")
        Allure.addEpic("DSBS КГТ")
        Allure.addFeature("Чекаут. Подъём на этаж")
        Allure.addTitle("Разгрузка")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var liftingDetailsPage: CheckoutLiftingDetailsPage!

        disable(toggles: FeatureNames.paymentSDK, FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            var userState = UserAuthState()
            userState.setAddressesState(addresses: [.default])
            userState.setContactsState(contacts: [.basic])
            stateManager?.setState(newState: userState)

            var cartState = CartState()
            cartState.setUserOrdersState(with: .largeSized500kg)
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Добавляем разгрузку".ybm_run { _ in
            XCTAssertEqual(
                checkoutPage.liftingDetailsTypeCell.title.label,
                "Добавить разгрузку и подъём\nЧтобы не делать этого самостоятельно"
            )

            liftingDetailsPage = checkoutPage.liftingDetailsTypeCell.tap()

            wait(forVisibilityOf: liftingDetailsPage.element)

            liftingDetailsPage.selectLiftingType(with: "onlyUnload")

            liftingDetailsPage.doneButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем выбранную опцию".ybm_run { _ in
            XCTAssertEqual(
                checkoutPage.liftingDetailsTypeCell.title.label,
                "Разгрузка у дома, 1 500 ₽\nПо тарифу «‎Подъем на 1 этаж без лифта»"
            )
        }
    }

    func testCheckoutLockLiftingOption() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5551")
        Allure.addEpic("DSBS КГТ")
        Allure.addFeature("Чекаут. Отображение недоступных опций")
        Allure.addTitle("Блокировка опций подъема")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var liftingDetailsPage: CheckoutLiftingDetailsPage!
        var elevatorOption: RadioButtonPage!

        disable(toggles: FeatureNames.paymentSDK, FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            var userState = UserAuthState()
            userState.setAddressesState(addresses: [.default])
            userState.setContactsState(contacts: [.basic])
            stateManager?.setState(newState: userState)

            var cartState = CartState()
            cartState.setUserOrdersState(with: .largeSized500kg)
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем, что опция на лифте видна".ybm_run { _ in
            XCTAssertEqual(
                checkoutPage.liftingDetailsTypeCell.title.label,
                "Добавить разгрузку и подъём\nЧтобы не делать этого самостоятельно"
            )

            liftingDetailsPage = checkoutPage.liftingDetailsTypeCell.tap()
            wait(forVisibilityOf: liftingDetailsPage.element)

            elevatorOption = liftingDetailsPage.liftingType(with: "elevator")
            XCTAssertEqual(elevatorOption.title.label, "Подъём на пассажирском лифте")
            XCTAssertEqual(elevatorOption.subtitle.label, "Посылка слишком большая")
        }

        "Пытаемся выбрать опцию на лифте и закрываем попап".ybm_run { _ in
            elevatorOption.element.tap()
            liftingDetailsPage.doneButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем, что опция не выбрана".ybm_run { _ in
            XCTAssertEqual(
                checkoutPage.liftingDetailsTypeCell.title.label,
                "Добавить разгрузку и подъём\nЧтобы не делать этого самостоятельно"
            )
        }
    }
}
