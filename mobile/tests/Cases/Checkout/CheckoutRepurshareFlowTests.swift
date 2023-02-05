import MarketUITestMocks
import UIUtils
import XCTest

final class CheckoutRepurshareFlowTests: LocalMockTestCase {

    // swiftlint:disable function_body_length
    func testWithMulticartAndKGT() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3873")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Флоу повторной покупки")
        Allure.addTitle("Мультикорзина + КГТ")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var shipmentPopupPage: CheckoutShipmentPopupPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!
        var mapViewPage: CheckoutMapViewPage!
        var editAddressPage: EditAddressPage!

        let city = "Москва"
        let street = "улица Льва Толстого"
        let house = "16"
        let fullAddress = "\(city), \(street), д. \(house)"
        let deliveryDate = "вт, 2 февраля"
        let deliveryTime = "09:00–22:00"
        let editedDeliveryDate = "сб, 6 февраля"
        let editedDeliveryTime = "09:00–18:00"
        let apart = "34"
        let entrance = "3"
        let intercom = "34K3432"
        let floor = "33"
        let comment = "Don't worry, be happy"

        let addressId = "324be7ed-dfa5-4aba-af04-65bd6652c810"

        var userState = UserAuthState()
        var checkoutState = CheckoutState()

        "Мокаем ручки".ybm_run { _ in
            userState.setAddressesState(addresses: [.init(region: .moscow, address: .shabolovka)])
            userState.setUserAddressByGpsCoordinate(
                result: .rublevskoye,
                byGps: [
                    .init(
                        region: .moscow,
                        address: .tmpAddress
                    )
                ]
            )
            checkoutState.setUserAddressState(id: addressId, handlers: [.add, .delete, .update])

            stateManager?.setState(newState: checkoutState)
            mockStateManager?.pushState(bundleName: "Checkout_RepurshareFlowMulticartAndKGT")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Нажимаем на имидж товаров и сверяем".ybm_run { _ in
            shipmentPopupPage = checkoutPage.shipmentCell(at: 0).tap()

            XCTAssertEqual(
                shipmentPopupPage.shipmentItem(at: 0).title.label,
                "Холодильник Daewoo Electronics FRN-X22 H5CW"
            )
            XCTAssertEqual(shipmentPopupPage.shipmentItem(at: 0).price.label, "55 990 ₽")
            XCTAssertEqual(shipmentPopupPage.shipmentItem(at: 0).count.label, "1 шт.")

            XCUIApplication().otherElements.matching(identifier: PopupEmdeddingAccessibility.dimmingView).element.tap()
            wait(forVisibilityOf: checkoutPage.element)

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.shipmentCell(at: 1).element)

            shipmentPopupPage = checkoutPage.shipmentCell(at: 1).tap()
            wait(forVisibilityOf: shipmentPopupPage.element)

            XCTAssertEqual(
                shipmentPopupPage.shipmentItem(at: 0).title.label,
                "Беспроводные наушники JBL Tune 120 TWS, черный"
            )
            XCTAssertEqual(shipmentPopupPage.shipmentItem(at: 0).price.label, "3 186 ₽")
            XCTAssertEqual(shipmentPopupPage.shipmentItem(at: 0).count.label, "1 шт.")

            XCUIApplication().otherElements.matching(identifier: PopupEmdeddingAccessibility.dimmingView).element.tap()
        }

        "Нажимаем \"выбрать адрес доставки\"".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: checkoutPage.addressChooserButton().element
            )

            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            XCTAssertEqual(checkoutPresetSelectorPage.headerStackView.selectedChipButton.label, "Курьер")

            mapViewPage = checkoutPresetSelectorPage.tapAddAddressFromHeader()
        }

        "Выбираем адрес: Москва, ул.Льва Толстого 16".ybm_run { _ in
            ybm_wait {
                mapViewPage.summary.addressTextField.text == "Москва, улица Льва Толстого, д. 16"
            }
            ybm_wait {
                mapViewPage.pinTitle.label == "550 ₽"
            }
        }

        "Нажимаем \"Привезти сюда\"".ybm_run { _ in
            editAddressPage = mapViewPage.summary.tap()

            XCTAssertEqual(editAddressPage.addressCellView.label, fullAddress)
        }

        "Нажимаем \"продолжить\"".ybm_run { _ in
            editAddressPage.continueButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(checkoutPage.addressChooserButton().title.label, "Курьером\n" + fullAddress)

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.dateSelectorCell(at: 0).element)
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "\(deliveryDate), 550 ₽")

            checkTimeSlot(checkoutPage, title: deliveryTime, slot: 1)

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.dateSelectorCell(at: 1).element)
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 1).value.label, "\(deliveryDate), 0 ₽")

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.deliverySlotsCell(at: 1).element)
            checkTimeSlot(checkoutPage, title: deliveryTime, row: 1, slot: 0)

            checkoutPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: checkoutPage.dateSelectorCell(at: 0).element
            )
            checkoutPage.dateSelectorCell(at: 0).element.tap()

            PickerPage.current.pickerWheel(at: 0).adjust(toPickerWheelValue: "сб, 6 февраля, 550 ₽")
            KeyboardPage.current.tapDone()

            checkoutPage.deliverySlotsCell(at: 0).slot(at: 0).element.tap()
            checkTimeSlot(checkoutPage, title: editedDeliveryTime)

            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "\(editedDeliveryDate), 550 ₽")
        }

        "Переходим к редактированию выбранного ранее адреса".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: checkoutPage.addressChooserButton().element
            )

            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            wait(forVisibilityOf: checkoutPresetSelectorPage.element)

            editAddressPage = checkoutPresetSelectorPage.selectedAddressCell.tapEdit()
            wait(forVisibilityOf: editAddressPage.element)
        }

        "Изменяем заполненные поля, а также заполняем все пустые".ybm_run { _ in
            editAddressPage.apartmentInput.typeText(apart)
            editAddressPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: editAddressPage.commentInput.element
            )
            editAddressPage.entranceInput.typeText(entrance)
            editAddressPage.intercomInput.typeText(intercom)
            editAddressPage.floorInput.typeText(floor)
            editAddressPage.commentInput.typeText(comment)

            KeyboardPage.current.tapDone()
        }

        "Мокаем ручки для изменения адреса".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Checkout_RepurshareFlowMulticartAndKGT_updatePreset")
        }

        "Нажимаем сохранить".ybm_run { _ in
            editAddressPage.continueButton.tap()
            wait(forVisibilityOf: checkoutPresetSelectorPage.element)
            checkoutPresetSelectorPage.doneButton.tap()
            wait(forVisibilityOf: checkoutPage.addressChooserButton().element)

            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Курьером\n" +
                    "\(fullAddress), кв. \(apart)\n\(entrance) подъезд, \(floor) этаж, домофон \(intercom), \"\(comment)\""
            )
        }

        "Снова перейти к редактированию адреса и удаляем его".ybm_run { _ in
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            XCTAssertEqual(checkoutPresetSelectorPage.headerStackView.selectedChipButton.label, "Курьер")

            editAddressPage = checkoutPresetSelectorPage.selectedAddressCell.tapEdit()
            editAddressPage.deleteButton.tap()

            app.alerts.firstMatch.descendants(matching: .button).element(boundBy: 1).tap()
            editAddressPage.continueButton.tap()

            XCTAssertEqual(checkoutPage.addressChooserButton().title.label, "Выбрать адрес доставки")
        }

        "Выбираем любой адрес".ybm_run { _ in
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            checkoutPresetSelectorPage.addressCell(at: 0).element.tap()
            checkoutPresetSelectorPage.doneButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.dateSelectorCell(at: 0).element)
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "\(editedDeliveryDate), 550 ₽")

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.dateSelectorCell(at: 1).element)
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 1).value.label, "\(deliveryDate), 0 ₽")
        }
    }

    func testWithMulticartAndCC() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3874")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Флоу повторной покупки")
        Allure.addTitle("Мультикорзина + С&С")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!

        let addressForNoCC = "Москва, Нагорная улица, д. 15к5, кв. 7554\n4 подъезд"
        let deliveryTime = "09:00–18:00"

        disable(toggles: FeatureNames.checkoutPresetsRedesign, FeatureNames.cartRedesign)

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Checkout_RepurshareFlowMulticartAndCC")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(checkoutPage.addressChooserButton().title.label, addressForNoCC)

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.addressChooserButton(at: 1).element)
            XCTAssertEqual(checkoutPage.addressChooserButton(at: 1).title.label, addressForNoCC)

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.outletChooserButton().element)
            XCTAssertEqual(checkoutPage.outletChooserButton().title.label, "Выбрать пункт выдачи")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.recipientInfoCell.element)
            XCTAssertEqual(checkoutPage.recipientInfoCell.title.label, "Ge Gege\n99@99.ru, +79999999999")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")
        }

        "Выбираем ПВЗ для C&C-посылки".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(to: .up, toFullyReveal: checkoutPage.outletChooserButton().element)

            checkoutPresetSelectorPage = checkoutPage.outletChooserButton().tap()
            wait(forVisibilityOf: checkoutPresetSelectorPage.element)

            checkoutPresetSelectorPage.doneButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            checkoutPage.element.ybm_swipeCollectionView(
                to: .down,
                toFullyReveal: checkoutPage.postpaidCell().element
            )
            XCTAssertEqual(
                checkoutPage.postpaidCell().title.label,
                "Оплата при получении"
            )
        }

        "Для одной из обычных посылок изменяем адрес на другой регион".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(to: .up, toFullyReveal: checkoutPage.addressChooserButton().element)
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()

            checkoutPresetSelectorPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPresetSelectorPage.addressCell(at: 7).element)
            checkoutPresetSelectorPage.addressCell(at: 7).element.tap()
            checkoutPresetSelectorPage.doneButton.tap()
        }

        "Для одной из обычных посылок выбраем адрес \"Льва Толстого 16\"".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.addressChooserButton(at: 1).element)
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton(at: 1).tap()

            checkoutPresetSelectorPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPresetSelectorPage.addressCell(at: 5).element)
            checkoutPresetSelectorPage.addressCell(at: 5).element.tap()
            checkoutPresetSelectorPage.doneButton.tap()
        }

        "Проверяем, что стоимость доставки для посылки в офис Яндекса изменилась на 0".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(to: .up, toFullyReveal: checkoutPage.shipmentHeaderCell(at: 1).element)
            XCTAssertEqual(
                checkoutPage.shipmentHeaderCell(at: 1).title.label,
                "Доставка курьером 28 февраля, бесплатно"
            )

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.dateSelectorCell(at: 1).element)
            XCTAssertEqual(
                checkoutPage.dateSelectorCell(at: 1).value.label,
                "вс, 28 февраля, 0 ₽"
            )

            checkTimeSlot(checkoutPage, title: deliveryTime, row: 1)
        }

        "Изменяем ПВЗ для C&C товара".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.outletChooserButton().element)

            checkoutPresetSelectorPage = checkoutPage.outletChooserButton().tap()
            checkoutPresetSelectorPage.outletCell(at: 1).element.tap()
            checkoutPresetSelectorPage.doneButton.tap()
        }

        "Проверяем наличие подписей о раздельной оплате для онлайн-способов оплаты".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")
        }
    }

    // MARK: - Private:

    private func checkTimeSlot(
        _ checkoutPage: CheckoutPage,
        title: String,
        row: Int = 0,
        slot: Int = 0
    ) {
        let timeSlot = checkoutPage.deliverySlotsCell(at: row).slot(at: slot)
        XCTAssertTrue(timeSlot.isSelected)
        XCTAssertEqual(timeSlot.title, title)
    }
}
