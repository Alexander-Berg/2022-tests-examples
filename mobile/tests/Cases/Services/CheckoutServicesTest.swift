import MarketUITestMocks
import UIUtils
import XCTest

class CheckoutServicesTest: ServicesTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testChangeServiceInCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5202")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Информация о партнере")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var merchantPopupPage: MerchantPopupPage!
        var servicesPopup: ServicesPopupPage!
        var serviceSelector: CheckoutPage.AdditionalServiceCell!

        "Мокаем состояния".run {
            setupCheckoutStateFirstService()
        }

        "Открываем корзину".run {
            cartPage = goToCart()
        }

        "Переходим в чекаут".run {
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Открываем поп-ап мерчанта".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.merchantInfoDisclaimerCell.element)
            merchantPopupPage = checkoutPage.merchantInfoDisclaimerCell.tap()
            wait(forVisibilityOf: merchantPopupPage.element)
        }

        "Проверяем информацию о партнере-установщике".run {
            checkPartnerInfo(merchantPopupPage: merchantPopupPage, itemNumber: 1)
        }

        "Закрываем попап мерчанта".run {
            XCUIApplication().otherElements.matching(identifier: PopupEmdeddingAccessibility.backgroundView).element
                .swipe(to: .up, until: !merchantPopupPage.element.isVisible)
        }

        "Мокаем пустые услуги".run {
            setupCheckoutStateWithoutServices()
        }

        "Скроллим до первого селектора выбора услуги".run {
            serviceSelector = checkoutPage.additionalServiceSelectorCell(at: 0)
            checkoutPage.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: serviceSelector.element)
        }

        "Сбрасываем услугу".run {
            servicesPopup = serviceSelector.tap()
            wait(forVisibilityOf: servicesPopup.element)
            servicesPopup.selectService(at: 0)
            servicesPopup.saveButton.tap()
            wait(forInvisibilityOf: servicesPopup.element)
        }

        "Свайпаем до второго товара".run {
            serviceSelector = checkoutPage.additionalServiceSelectorCell(at: 1)
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: serviceSelector.element)
        }

        "Мокаем услуги для второго товара".run {
            setupCheckoutStateSecondService()
        }

        "Выбираем услугу у второго товара".run {
            servicesPopup = serviceSelector.tap()
            servicesPopup.selectService(at: 3)
            servicesPopup.saveButton.tap()
            wait(forInvisibilityOf: servicesPopup.element)
        }

        "Открываем поп-ап мерчанта".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.merchantInfoDisclaimerCell.element)
            merchantPopupPage = checkoutPage.merchantInfoDisclaimerCell.tap()
            wait(forVisibilityOf: merchantPopupPage.element)
        }

        "Проверяем информацию о партнере-установщике".run {
            checkPartnerInfo(merchantPopupPage: merchantPopupPage, itemNumber: 2)
        }
    }

    func testFinishPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4744")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Спасибка")

        var rootPage: RootPage!
        var skuPage: SKUPage!
        var addServicePopup: ServicesPopupPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var serviceSelector: CheckoutPage.AdditionalServiceCell!
        var finishPage: FinishMultiorderPage!

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
            setupCheckoutStateFirstService()
        }

        "Открываем карточку товара".ybm_run { _ in
            rootPage = appAfterOnboardingAndPopups()
            skuPage = goToDefaultSKUPage(root: rootPage)
        }

        "Добавляем товар в корзину".ybm_run { _ in
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.addToCartButton.element)
            setupAddToCartState()
            skuPage.addToCartButton.element.tap()
        }

        "Нажимаем Добавить установку".ybm_run { _ in
            XCTAssertEqual(skuPage.addServiceButton.element.label, "Доступна услуга установки")
            addServicePopup = skuPage.addServiceButton.tap()
            wait(forVisibilityOf: addServicePopup.element)
        }

        "Выбираем первую установку".ybm_run { _ in
            addServicePopup.selectService(at: 1)

            XCTAssertTrue(addServicePopup.selectedService.element.exists, "Нет селектора услуг")

            addServicePopup.saveButton.tap()
            wait(forInvisibilityOf: addServicePopup.element)
            wait(forVisibilityOf: skuPage.element)

            XCTAssertEqual(skuPage.addServiceButton.element.label, "Установка 5 000 ₽")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart(root: rootPage)
        }

        "Переходим в чекаут".run {
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Скроллим до первого селектора выбора услуги".run {
            serviceSelector = checkoutPage.additionalServiceSelectorCell(at: 0)
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: serviceSelector.element)
        }

        "Проверяем селектор".run {
            XCTAssertEqual(serviceSelector.title.label, "Комплексная установка, 3 000 ₽")
            XCTAssertTrue(serviceSelector.image.exists, "Нет иконки гаечного ключа")
        }

        "Выбираем постоплатный способ оплаты".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            let paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            wait(forVisibilityOf: paymentMethodPopupPage.element)

            paymentMethodPopupPage.selectPaymentMethod(with: "CASH_ON_DELIVERY")

            paymentMethodPopupPage.continueButton.tap()
            wait(forInvisibilityOf: paymentMethodPopupPage.element)
        }

        "Проверяем цену".run {
            let total = checkoutPage.summaryTotalCell
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: total.element)
            XCTAssertEqual(checkoutPage.summaryItemsCell.details.label, "46 089 ₽")
            XCTAssertEqual(checkoutPage.summaryServiceCell.details.label, "3 000 ₽")
            XCTAssertEqual(checkoutPage.summaryDeliveryCell.details.label, "550 ₽")
            XCTAssertEqual(total.details.label, "49 639 ₽")
        }

        "Листаем вниз и \"Подтверждаем заказ\"".ybm_run { _ in
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            finishPage = checkoutPage.paymentButton.tap()
            wait(forExistanceOf: finishPage.element)
        }

        "Проверяем данные на спасибке".run {
            finishPage.collectionView.ybm_swipeCollectionView(toFullyReveal: finishPage.serviceTitle())
            XCTAssertEqual(finishPage.serviceTitle().label, "Установка")
            XCTAssertEqual(finishPage.serviceName().label, "Комплексная установка\n1 шт.")
            XCTAssertEqual(
                finishPage.serviceDescription().label,
                "Запланирована на 23 декабря. Мастер позвонит вам, чтобы уточнить детали."
            )
        }
    }

    func testChangeServiceDateTimeslotsWithDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4747")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Изменение даты и времени получения заказа и услуги (таймслоты + курьерка")

        var checkoutPage: CheckoutPage!
        var selectorPage: SelectorPage!
        var pickerPage: PickerPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.currentTimeIntervalSince1970] = "1640204318.855895"

        "Мокаем состояние (слоты + курьерка)".run {
            setupCheckoutStateFirstService()
        }

        "Переходим в чекаут".run {
            let cartPage = goToCart()
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем дату доставки".run {
            let dateSelector = checkoutPage.dateSelectorCell(at: 0)
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: dateSelector.element)
            XCTAssertEqual(dateSelector.value.label, "чт, 23 декабря, 275 ₽")
        }

        "Проверяем селектор выбора услуг".run {
            let serviceSelector = checkoutPage.additionalServiceSelectorCell()
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: serviceSelector.element)
            XCTAssertEqual(serviceSelector.title.label, "Комплексная установка, 3 000 ₽")
            XCTAssertTrue(serviceSelector.image.exists, "Нет иконки гаечного ключа")
        }

        "Открываем селектор таймслота".run {
            selectorPage = checkoutPage.timeslotSelectorCell()
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: selectorPage.element)
            XCTAssertEqual(selectorPage.value.label, "30 декабря в 10:00")
            pickerPage = PickerPage.current
            selectorPage.element.tap()
            wait(forVisibilityOf: pickerPage.element)
        }

        "Проверяем данные в пикере".run {
            XCTAssertEqual(pickerPage.pickerWheel(at: 0).value, "30 декабря")
            XCTAssertEqual(pickerPage.pickerWheel(at: 1).value, "10:00")
        }

        "Меняем дату установки".run {
            pickerPage.pickerWheel(at: 0).adjust(toPickerWheelValue: "31 декабря")
            pickerPage.pickerWheel(at: 1).adjust(toPickerWheelValue: "12:00")
            KeyboardPage.current.tapDone()
        }

        "Проверяем селектор таймслота".run {
            XCTAssertEqual(selectorPage.value.label, "31 декабря в 12:00")
        }
    }

    func testChangeServiceDateTimeIntervalsWithDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4747")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Изменение даты и времени получения заказа и услуги (интервалы + курьерка")

        var checkoutPage: CheckoutPage!
        var selectorPage: SelectorPage!
        var pickerPage: PickerPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.currentTimeIntervalSince1970] = "1640204318.855895"

        "Меняем стейт (интервалы + курьерка)".run {
            setupCheckoutStateFirstService(timeslotId: nil)
        }

        "Переходим в чекаут".run {
            let cartPage = goToCart()
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Открываем селектор таймслота".run {
            selectorPage = checkoutPage.timeslotSelectorCell()
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: selectorPage.element)
            XCTAssertEqual(selectorPage.value.label, "25 декабря, с 16:00 до 20:00")
            pickerPage = PickerPage.current
            selectorPage.element.tap()
            wait(forVisibilityOf: pickerPage.element)
        }

        "Проверяем данные в пикере".run {
            XCTAssertEqual(pickerPage.pickerWheel(at: 0).value, "25 декабря")
            XCTAssertEqual(pickerPage.pickerWheel(at: 1).value, "с 16:00 до 20:00")
        }

        "Меняем дату установки".run {
            pickerPage.pickerWheel(at: 0).adjust(toPickerWheelValue: "26 декабря")
            pickerPage.pickerWheel(at: 1).adjust(toPickerWheelValue: "с 12:00 до 16:00")
            KeyboardPage.current.tapDone()
        }

        "Проверяем селектор таймслота".run {
            XCTAssertEqual(selectorPage.value.label, "26 декабря, с 12:00 до 16:00")
        }
    }

    func testChangeServiceDateWithSameDeliveryDay() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4747")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Изменение даты и времени получения заказа и услуги (установка в день доставки)")

        var checkoutPage: CheckoutPage!
        var selectorPage: SelectorPage!
        var pickerPage: PickerPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.currentTimeIntervalSince1970] = "1640204318.855895"

        "Мокаем состояние".run {
            setupCheckoutStateFirstService(timeslotId: nil)
        }

        "Переходим в чекаут".run {
            let cartPage = goToCart()
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Открываем селектор таймслота".run {
            selectorPage = checkoutPage.timeslotSelectorCell()
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: selectorPage.element)
            pickerPage = PickerPage.current
            selectorPage.element.tap()
            wait(forVisibilityOf: pickerPage.element)
        }

        "Выбираем доставку в день установки и меняем дату".run {
            app.switches[CheckoutAccessibility.deliveryWithServiceSwitch].tap()
            pickerPage.pickerWheel(at: 0).adjust(toPickerWheelValue: "27 декабря")
            KeyboardPage.current.tapDone()
        }

        "Проверяем попап, подтвержаем".run {
            let element = XCUIApplication().otherElements[BarrierViewAccessibility.root]
            let popup = BarrierViewPage(element: element)
            XCTAssertEqual(popup.title.label, "Изменить дату доставки с 23 декабря на 27 декабря?")
            XCTAssertEqual(
                popup.subtitle.label,
                "В этом случае доставка будет в первой половине дня, а установка во второй"
            )
            XCTAssertEqual(popup.actionButton.label, "Изменить дату доставки")
            XCTAssertEqual(popup.extraButton.label, "Отмена")
            popup.actionButton.tap()
            wait(forInvisibilityOf: popup.element)
        }

        "Проверяем дату доставки".run {
            selectorPage = checkoutPage.dateSelectorCell(at: 0)
            checkoutPage.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: selectorPage.element)
            XCTAssertEqual(selectorPage.value.label, "пн, 27 декабря, 275 ₽")
        }

        "Меняем дату доставки".run {
            pickerPage = PickerPage.current
            selectorPage.element.tap()
            wait(forVisibilityOf: pickerPage.element)
            pickerPage.pickerWheel(at: 0).adjust(toPickerWheelValue: "сб, 25 декабря, 275 ₽")
            KeyboardPage.current.tapDone()
            XCTAssertEqual(selectorPage.value.label, "сб, 25 декабря, 275 ₽")
        }
    }
}

extension CheckoutServicesTest {
    func checkPartnerInfo(merchantPopupPage: MerchantPopupPage, itemNumber: Int) {
        let lowerIndex = 1 * itemNumber
        let higherIndex = 2 * itemNumber

        merchantPopupPage.element
            .ybm_swipeCollectionView(toFullyReveal: merchantPopupPage.schedule(at: lowerIndex).element)
        XCTAssertEqual(merchantPopupPage.supplierSubtitle(at: lowerIndex).label, "Компания-исполнитель установки")

        XCTAssertEqual(
            merchantPopupPage.fullName(at: higherIndex).header.label,
            "Полное название"
        )
        XCTAssertEqual(
            merchantPopupPage.fullName(at: higherIndex).caption.label,
            "Общество с ограниченной ответственностью «УСТАНОВКА.РУ»"
        )

        XCTAssertEqual(
            merchantPopupPage.actualAddress(at: higherIndex).header.label,
            "Фактический адрес"
        )
        XCTAssertEqual(
            merchantPopupPage.actualAddress(at: higherIndex).caption.label,
            "423124, Россия, Москва, Кутузовский проспект, 4"
        )

        XCTAssertEqual(
            merchantPopupPage.juridicalAddress(at: higherIndex).header.label,
            "Юридический адрес"
        )
        XCTAssertEqual(
            merchantPopupPage.juridicalAddress(at: higherIndex).caption.label,
            "312456, Россия, Самара, Колотушкина, 8"
        )

        XCTAssertEqual(merchantPopupPage.ogrn(at: higherIndex).header.label, "ОГРН")
        XCTAssertEqual(merchantPopupPage.ogrn(at: higherIndex).caption.label, "1137746771360")

        XCTAssertEqual(merchantPopupPage.schedule(at: lowerIndex).header.label, "Часы работы")
        XCTAssertEqual(merchantPopupPage.schedule(at: lowerIndex).caption.label, "Пн-Вс с 09:00 до 20:00")
    }

    func setupUserState() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        userState.setContactsState(contacts: [.basic])
        userState.setFavoritePickupPoints(favoritePickups: [.rublevskoye])
        stateManager?.setState(newState: userState)
    }

    func setupOrdersState() {
        var ordersState = OrdersState()
        ordersState.setOutlet(outlets: [.rublevskoye])
        stateManager?.setState(newState: ordersState)
    }

    func setupCheckoutState(timeslotId: String?) {
        var checkoutState = CheckoutState()
        checkoutState.setSupplier(suppliers: [.custom(id: 431_782), .custom(id: 1_466_421)])
        checkoutState.setServicesTimeslots(timeslotId: timeslotId)
        checkoutState.setUserOrderState(orderResponse: .init())
        stateManager?.setState(newState: checkoutState)
    }

    func setupCheckoutStateFirstService(
        bundleName: String = "ServicesSet_CheckoutFirstService",
        timeslotId: String? = "ESvVjsV0mtaEQMPSSjD52w_11"
    ) {
        setupUserState()
        setupOrdersState()
        setupCheckoutState(timeslotId: timeslotId)
        mockStateManager?.pushState(bundleName: bundleName)
    }

    func setupCheckoutStateSecondService() {
        setupUserState()
        setupCheckoutState(timeslotId: "o3xkIAVj-PyhGtTi8znPiw_11")
        mockStateManager?.pushState(bundleName: "ServicesSet_CheckoutSecondService")
    }

    func setupCheckoutStateWithoutServices() {
        setupUserState()
        var checkoutState = CheckoutState()
        checkoutState.setSupplier(suppliers: [.custom(id: 431_782), .custom(id: 1_466_421)])
        stateManager?.setState(newState: checkoutState)
        mockStateManager?.pushState(bundleName: "ServicesSet_CheckoutWithoutServices")
    }
}
