import FormKit
import MarketUI
import MarketUITestMocks
import UIUtils
import XCTest

final class CheckoutDeliveryOptionsTests: LocalMockTestCase {

    override func setUp() {
        super.setUp()

        enable(toggles: FeatureNames.applePay)
        disable(toggles: FeatureNames.cartRedesign)
    }

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testHourIntervalsPaymentMethods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4506")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Доставка по часовым интервалам")
        Allure.addTitle("Пересчет доступных способов оплаты. Часовые интервалы")

        let checkoutPage = setupCheckoutConditions(with: "Checkout_DeliveryOptions")

        selectPostpaidPaymentMethod(checkoutPage)

        "Выбираем день с часовым интервалом".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: checkoutPage.dateSelectorCell(at: 0).element
            )
            checkoutPage.dateSelectorCell(at: 0).element.tap()

            PickerPage.current.pickerWheel(at: 0).adjust(toPickerWheelValue: "пн, 12 июля, 99 ₽")
            KeyboardPage.current.tapDone()

            checkTimeSlot(checkoutPage, title: "12:00–13:00")
        }

        "Проверяем, что способ оплаты сброшен до дефолтного".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPage.paymentMethodCell.element
            )
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Apple Pay")
        }

        "Проверяем, что постоплатные опции задизейблены".ybm_run { _ in
            let paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            wait(forVisibilityOf: paymentMethodPopupPage.element)

            let cashPayment = paymentMethodPopupPage.paymentMethod(with: "CASH_ON_DELIVERY")
            XCTAssertEqual(cashPayment.subtitle.label, "Недоступно для часовых интервалов доставки")
            XCTAssertEqual(cashPayment.image.identifier, RadioButtonViewCellAccessibility.Selectability.locked)

            let cardOnDeliveryPayment = paymentMethodPopupPage.paymentMethod(with: "CARD_ON_DELIVERY")
            XCTAssertEqual(cardOnDeliveryPayment.subtitle.label, "Недоступно для часовых интервалов доставки")
            XCTAssertEqual(
                cardOnDeliveryPayment.image.identifier,
                RadioButtonViewCellAccessibility.Selectability.locked
            )
        }
    }

    func testOnDemandPaymentMethods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4507")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Ондеманд")
        Allure.addTitle("Пересчет доступных способов оплаты. Ондеманд")

        let checkoutPage = setupCheckoutConditions(with: "Checkout_DeliveryOptions")

        selectPostpaidPaymentMethod(checkoutPage)

        "Выбираем доставку по клику".ybm_run { _ in
            let onDemandSlot = checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element
            checkoutPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: onDemandSlot
            )
            mockStateManager?.pushState(bundleName: "Checkout_OnDemandOptions")
            onDemandSlot.tap()
        }

        "Проверяем, что способ оплаты сброшен до дефолтного".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPage.paymentMethodCell.element
            )
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Apple Pay")
        }

        "Проверяем, что постоплатные опции задизейблены".ybm_run { _ in
            let paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            wait(forVisibilityOf: paymentMethodPopupPage.element)

            let cashPayment = paymentMethodPopupPage.paymentMethod(with: "CASH_ON_DELIVERY")
            XCTAssertEqual(cashPayment.subtitle.label, "Недоступно для доставки по клику")
            XCTAssertEqual(cashPayment.image.identifier, RadioButtonViewCellAccessibility.Selectability.locked)

            let cardOnDeliveryPayment = paymentMethodPopupPage.paymentMethod(with: "CARD_ON_DELIVERY")
            XCTAssertEqual(cardOnDeliveryPayment.subtitle.label, "Недоступно для доставки по клику")
            XCTAssertEqual(
                cardOnDeliveryPayment.image.identifier,
                RadioButtonViewCellAccessibility.Selectability.locked
            )
        }
    }

    func testHourIntervalsMultiorderPaymentMethods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4508")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Часовые интервалы")
        Allure.addTitle("Пересчет доступных способов оплаты. Мультизаказ")

        var paymentMethodPopupPage: CheckoutPaymentMethodPopupPage!
        let checkoutPage = setupCheckoutConditions(with: "Checkout_MultiorderDeliveryOptions")

        "Проверяем, что для второго товара отображается способ доставки по временному интервалу".ybm_run { _ in
            let intervalLabel = "сб, 17 июля, 0 ₽"
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPage.dateSelectorCell(at: 1).element
            )
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 1).value.label, intervalLabel)

            checkTimeSlot(checkoutPage, title: "09:00–18:00", row: 1)
        }

        "Выбираем доставку с часовым интервалом".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: checkoutPage.dateSelectorCell(at: 0).element
            )
            mockStateManager?.pushState(bundleName: "Checkout_DefaultServiceOptions")

            checkoutPage.deliverySlotsCell(at: 0).defaultServiceSelectorCell.element.tap()
            let hourIntervalLabel = "чт, 15 июля, 0 ₽"
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, hourIntervalLabel)
            checkTimeSlot(checkoutPage, title: "12:00–13:00", row: 0)
        }

        selectPostpaidPaymentMethodMultiorder(checkoutPage)

        "Выбираем доставку по клику в удобный момент".ybm_run { _ in
            let onDemandSlot = checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element
            checkoutPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: onDemandSlot
            )
            mockStateManager?.pushState(bundleName: "Checkout_MultiorderDeliveryOptions")
            onDemandSlot.tap()
        }

        "Проверяем текст в блоке способа оплаты".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPage.paymentMethodCell.element
            )
            let paymentLabel = "Наличными при получении"
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, paymentLabel)
        }

        "Проверяем, что оба способа оплаты доступны (предоплата и постоплата)".ybm_run { _ in
            paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            wait(forVisibilityOf: paymentMethodPopupPage.element)

            let prepaidPayment = paymentMethodPopupPage.paymentMethod(with: "YANDEX")
            let postpaidPayment = paymentMethodPopupPage.paymentMethod(with: "CARD_ON_DELIVERY")

            XCTAssertEqual(prepaidPayment.image.identifier, RadioButtonViewCellAccessibility.Selectability.selectable)
            XCTAssertEqual(postpaidPayment.image.identifier, RadioButtonViewCellAccessibility.Selectability.selectable)
        }

        "Проверяем, что под выбранным постоплатным способом есть поясняющий текст".ybm_run { _ in
            let cashPayment = paymentMethodPopupPage.paymentMethod(with: "CASH_ON_DELIVERY")
            let paymentSubtitle = "Оплата 2 посылки при получении: 8 751 ₽\nОплата 1 посылки онлайн: 219 ₽"

            XCTAssertEqual(cashPayment.image.identifier, RadioButtonViewCellAccessibility.Selectability.selected)
            XCTAssertEqual(paymentMethodPopupPage.currentPaymentDetails, paymentSubtitle)
        }
    }

    func testOnDemandMultiorderPaymentMethodsAddressChanged() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4517")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Ондеманд")
        Allure.addTitle("Пересчет доступных способов оплаты при смене адреса. Мультизаказ")

        var paymentMethodPopupPage: CheckoutPaymentMethodPopupPage!
        let checkoutPage = setupCheckoutConditions(with: "Checkout_MultiorderDeliveryOptions")

        "Проверяем, что для второго товара отображается способ доставки по временному интервалу".ybm_run { _ in
            let intervalLabel = "сб, 17 июля, 0 ₽"
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 1).value.label, intervalLabel)

            let deliverySlots = checkoutPage.deliverySlotsCell(at: 1)
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: deliverySlots.element
            )
            checkTimeSlot(checkoutPage, title: "09:00–18:00", row: 1)
        }

        selectPostpaidPaymentMethodMultiorder(checkoutPage)

        changeAddress(checkoutPage, addressCell: 5, bundleName: "Checkout_MultiorderDeliveryOptions_AddressChanged")

        "Проверяем, что для каждого товара отображается способ доставки в выбранный интервал".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.dateSelectorCell(at: 0).element)
            wait(forVisibilityOf: checkoutPage.dateSelectorCell(at: 1).element)
        }

        "Открываем попап со способами оплаты".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            wait(forVisibilityOf: paymentMethodPopupPage.element)
        }

        "Проверяем, что оба способа оплаты доступны (предоплата и постоплата)".ybm_run { _ in
            let prepaidPayment = paymentMethodPopupPage.paymentMethod(with: "YANDEX")
            let postpaidPayment = paymentMethodPopupPage.paymentMethod(with: "CARD_ON_DELIVERY")

            XCTAssertEqual(prepaidPayment.image.identifier, RadioButtonViewCellAccessibility.Selectability.selectable)
            XCTAssertEqual(postpaidPayment.image.identifier, RadioButtonViewCellAccessibility.Selectability.selectable)
        }

        "Проверяем, что под постоплатными способами нет никакого дополнительного текста".ybm_run { _ in
            let postpaidPayment = paymentMethodPopupPage.paymentMethod(with: "CARD_ON_DELIVERY")
            XCTAssertFalse(postpaidPayment.subtitle.isVisible)
            XCTAssertTrue(paymentMethodPopupPage.currentPaymentDetails.isEmpty)

            paymentMethodPopupPage.continueButton.tap()
            wait(forInvisibilityOf: paymentMethodPopupPage.element)
        }

        changeAddress(checkoutPage, addressCell: 3, bundleName: "Checkout_MultiorderDeliveryOptions")

        "Проверяем, что отображаются два способа доставки: по клику и в выбранный интервал".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element)
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).defaultServiceSelectorCell.element)
        }

        "Проверяем, что для второго товара отображается способ доставки по временному интервалу".ybm_run { _ in
            let intervalLabel = "сб, 17 июля, 0 ₽"
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPage.dateSelectorCell(at: 1).element
            )
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 1).value.label, intervalLabel)

            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPage.deliverySlotsCell(at: 1).element
            )
            checkTimeSlot(checkoutPage, title: "09:00–18:00", row: 1)
        }
    }

    // MARK: - Private

    private func setupCheckoutConditions(with bundleName: String) -> CheckoutPage {
        var rootPage: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        disable(toggles: FeatureNames.checkoutPresetsRedesign)

        var userState = UserAuthState()

        "Мокаем состояние".ybm_run { _ in
            userState.setAddressesState(addresses: [.init(region: .moscow, address: .shabolovka)])
            stateManager?.setState(newState: userState)
            mockStateManager?.pushState(bundleName: bundleName)
        }

        "Открываем корзину".ybm_run { _ in
            rootPage = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: rootPage)
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем, что отображаются два способа доставки: по клику и в выбранный интервал".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).onDemandSelectorCell.element)
            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).defaultServiceSelectorCell.element)
        }

        return checkoutPage
    }

    private func selectPostpaidPaymentMethod(_ checkoutPage: CheckoutPage) {
        "Выбираем день без часовых интервалов".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Checkout_DefaultServiceOptions")

            checkoutPage.deliverySlotsCell(at: 0).slot(at: 1).element.tap()

            let intervalLabel = "вс, 11 июля, 99 ₽"
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, intervalLabel)

            checkTimeSlot(checkoutPage, title: "12:00–16:30")
        }

        "Выбираем постоплатный способ оплаты".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            let paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            wait(forVisibilityOf: paymentMethodPopupPage.element)

            paymentMethodPopupPage.selectPaymentMethod(with: "CASH_ON_DELIVERY")

            paymentMethodPopupPage.continueButton.tap()
            wait(forInvisibilityOf: paymentMethodPopupPage.element)
        }
    }

    private func selectPostpaidPaymentMethodMultiorder(_ checkoutPage: CheckoutPage) {
        var paymentMethodPopupPage: CheckoutPaymentMethodPopupPage!

        "Открываем попап со способами оплаты".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            wait(forVisibilityOf: paymentMethodPopupPage.element)
        }

        "Проверяем, что оба способа оплаты доступны (предоплата и постоплата)".ybm_run { _ in
            let prepaidPayment = paymentMethodPopupPage.paymentMethod(with: "APPLE_PAY")
            let postpaidPayment = paymentMethodPopupPage.paymentMethod(with: "CARD_ON_DELIVERY")

            XCTAssertEqual(prepaidPayment.image.identifier, RadioButtonViewCellAccessibility.Selectability.selectable)
            XCTAssertEqual(postpaidPayment.image.identifier, RadioButtonViewCellAccessibility.Selectability.selectable)
        }

        "Выбираем оплату при получении и проверяем поясняющий текст".ybm_run { _ in
            paymentMethodPopupPage.selectPaymentMethod(with: "CASH_ON_DELIVERY")

            let paymentSubtitle = "Оплата 2 посылки при получении: 8 751 ₽\nОплата 1 посылки онлайн: 219 ₽"
            XCTAssertEqual(paymentMethodPopupPage.currentPaymentDetails, paymentSubtitle)

            paymentMethodPopupPage.continueButton.tap()
            wait(forInvisibilityOf: paymentMethodPopupPage.element)
        }
    }

    private func changeAddress(_ checkoutPage: CheckoutPage, addressCell: Int, bundleName: String) {
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!

        "Меняем адрес".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: checkoutPage.addressChooserButton().element
            )
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()

            checkoutPresetSelectorPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPresetSelectorPage.addressCell(at: addressCell).element
            )
            checkoutPresetSelectorPage.addressCell(at: addressCell).element.tap()
        }

        "Мокаем новое состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: bundleName)
        }

        "Закрываем шторку с выбором адреса".ybm_run { _ in
            checkoutPresetSelectorPage.doneButton.tap()
            wait(forInvisibilityOf: checkoutPresetSelectorPage.element)
        }
    }

    private func checkTimeSlot(
        _ checkoutPage: CheckoutPage,
        title: String,
        row: Int = 0
    ) {
        let timeSlot = checkoutPage.deliverySlotsCell(at: row).defaultServiceSelectorCell
        XCTAssertTrue(timeSlot.isSelected)
        XCTAssertEqual(timeSlot.title, title)
    }
}
