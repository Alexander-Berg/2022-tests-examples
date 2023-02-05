import MarketUITestMocks
import UIUtils
import XCTest

final class CheckoutFirstFlowTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    // swiftlint:disable function_body_length
    func testWithUsualCart() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3869")
        Allure.addEpic("Чекаут 2.1")
        Allure.addFeature("Флоу первой покупки")
        Allure.addTitle("Обычная корзина")

        var cartPage: CartPage!
        var mapViewPage: CheckoutMapViewPage!
        var recipientPage: CheckoutRecipientPage!
        var checkoutPage: CheckoutPage!
        var checkoutContactsPage: CheckoutContactsPage!
        var finishMultiorderPage: FinishMultiorderPage!

        var checkoutState = CheckoutState()
        var ordersState = OrdersState()

        "Мокаем новые моки".ybm_run { _ in
            ordersState.setOrdersResolvers(mapper: .empty, for: [.recent()])
            checkoutState.setUserContactState(handlers: [.add, .delete, .update])
            checkoutState.setPaymentOrdersIdsState(payment: [.basic])
            checkoutState.setRegion(region: .moscow)

            stateManager?.setState(newState: checkoutState)
            stateManager?.setState(newState: ordersState)
        }

        setupEnvironment(with: "Checkout_FirstFlowUsualCart")

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            mapViewPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: mapViewPage.element)
        }

        "Выбрать любой ПВЗ на карте, нажать продолжить".ybm_run { _ in
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            mapViewPage.tapToCurrentLocation()

            wait(forVisibilityOf: mapViewPage.element)

            mapViewPage.element.tap()

            wait(forVisibilityOf: OutletMapInfoPage.current.element)

            OutletMapInfoPage.current.continueButton.tap()

            recipientPage = CheckoutRecipientPage.current
            wait(forVisibilityOf: recipientPage.element)
        }

        "Нажать \"Продолжить\" не заполняя данные".ybm_run { _ in
            recipientPage.continueButton.tap()

            XCTAssertEqual(recipientPage.message(at: 0).label, "Укажите имя и фамилию")
            XCTAssertEqual(recipientPage.message(at: 1).label, "Укажите почту")
            XCTAssertEqual(recipientPage.message(at: 2).label, "Укажите номер телефона")
        }

        "Заполнить все поля, нажать \"продолжить\"".ybm_run { _ in
            recipientPage.nameTextField.typeText("Temik Vzorvish")
            recipientPage.emailTextField.typeText("vzorvanii@tip.ok")
            recipientPage.phoneTextField.typeText("88899900999")

            KeyboardPage.current.tapDone()
            recipientPage.continueButton.tap()

            checkoutPage = CheckoutPage.current
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.outletChooserButton().titleLabel.label,
                "Пункт выдачи посылок PickPoint"
            )
            XCTAssertEqual(
                checkoutPage.outletChooserButton().addressLabel.label,
                "Москва, Мытная ул., д. 48"
            )
        }

        "Сравнить стоимость доставки для заказа в саммари, в заголовке и на карточке ПВЗ".ybm_run { _ in
            XCTAssertEqual(
                checkoutPage.shipmentHeaderCell(at: 0).title.label,
                "Доставка в пункт выдачи 14 марта, бесплатно"
            )

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryDeliveryCell.element)
            XCTAssertEqual(checkoutPage.summaryDeliveryCell.details.label, "бесплатно")
        }

        "Нажать на данные получателя".ybm_run { _ in
            checkoutContactsPage = checkoutPage.recipientInfoCell.tap()
            ybm_wait(forVisibilityOf: [
                checkoutContactsPage.element,
                checkoutContactsPage.selectedContactCell.editButton,
                checkoutContactsPage.doneButton,
                checkoutContactsPage.addButton
            ])

            XCTAssertEqual(checkoutContactsPage.selectedContactCell.title.label, "Temik Vzorvish")
            XCTAssertEqual(
                checkoutContactsPage.selectedContactCell.subtitle.label,
                "vzorvanii@tip.ok, +7 (889) 990-09-99"
            )
        }

        "Нажать на карандаш".ybm_run { _ in
            recipientPage = checkoutContactsPage.selectedContactCell.tapEdit()
            wait(forVisibilityOf: recipientPage.element)
        }

        "Отредактировать какие-нибудь данные, нажать сохранить".ybm_run { _ in
            recipientPage.nameTextField.setText("Temik Kaifarik")

            KeyboardPage.current.tapDone()
            recipientPage.continueButton.tap()

            ybm_wait {
                checkoutPage.recipientInfoCell.title.label == "Temik Kaifarik\nvzorvanii@tip.ok, +7 (889) 990-09-99"
            }
        }

        "Снова перейти к редактированию и нажать \"Удалить\"".ybm_run { _ in
            checkoutContactsPage = checkoutPage.recipientInfoCell.tap()
            wait(forVisibilityOf: checkoutContactsPage.element)

            recipientPage = checkoutContactsPage.selectedContactCell.tapEdit()
            wait(forVisibilityOf: recipientPage.element)

            recipientPage.deleteButton.tap()
            XCTAssertEqual(app.alerts.firstMatch.label, "И правда хотите удалить этого получателя?")
        }

        "Нажать \"Отменить\"".ybm_run { _ in
            app.alerts.firstMatch.descendants(matching: .button).element(boundBy: 0).tap()
            wait(forVisibilityOf: recipientPage.element)
        }

        "Удалить получателя".ybm_run { _ in
            recipientPage.deleteButton.tap()
            app.alerts.firstMatch.descendants(matching: .button).element(boundBy: 1).tap()
            wait(forInvisibilityOf: recipientPage.element)

            wait(forVisibilityOf: checkoutPage.element)
            XCTAssertEqual(checkoutPage.recipientInfoCell.title.label, "Выбрать получателя")
        }

        "Добавить нового получателя, заполнив все данные, нажать \"сохранить\"".ybm_run { _ in
            recipientPage = checkoutPage.recipientInfoCell.tap()

            recipientPage.nameTextField.typeText("Osobo Opasen")
            recipientPage.emailTextField.typeText("vzorvanii@tip.ok")
            recipientPage.phoneTextField.typeText("88899900999")

            KeyboardPage.current.tapDone()
            recipientPage.continueButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
            XCTAssertEqual(
                checkoutPage.recipientInfoCell.title.label,
                "Osobo Opasen\nvzorvanii@tip.ok, +7 (889) 990-09-99"
            )
        }

        "Нажать кнопку Перейти к оплате".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            checkoutPage.paymentButton.tap()
        }

        "Закрыть окно для ввода данных карты".ybm_run { _ in
            let trustPage = TrustPage.current

            ybm_wait(forVisibilityOf: [trustPage.element])
            XCTAssertEqual(trustPage.navigationBar.title.label, "Оплата картой")

            trustPage.navigationBar.closeButton.tap()

            finishMultiorderPage = FinishMultiorderPage.current
            ybm_wait {
                finishMultiorderPage.element.isVisible &&
                    NavigationBarPage.current.title.isVisible
            }
            XCTAssertEqual(NavigationBarPage.current.title.label, "Спасибо")

            let firstItem = finishMultiorderPage.titleOfOrderItem(at: 0)
            XCTAssertEqual(firstItem.label, "Беспроводные наушники Apple AirPods Pro, white\n1 шт")
        }
    }

    func testWithClickAndCollect() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3870")
        Allure.addEpic("Чекаут 2.1")
        Allure.addFeature("Флоу первой покупки")
        Allure.addTitle("C&C")

        var cartPage: CartPage!
        var mapViewPage: CheckoutMapViewPage!
        var recipientPage: CheckoutRecipientPage!
        var checkoutPage: CheckoutPage!
        var checkoutPaymentPage: CheckoutPaymentMethodPopupPage!
        var merchantPopupPage: MerchantPopupPage!
        var finishMultiorderPage: FinishMultiorderPage!

        var checkoutState = CheckoutState()

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем новые моки".ybm_run { _ in
            checkoutState.setSupplier(suppliers: [.asna, .solos])
            checkoutState.setRegion(region: .moscow)

            stateManager?.setState(newState: checkoutState)
        }

        setupEnvironment(with: "Checkout_FirstFlowCC")

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            mapViewPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: mapViewPage.element)
        }

        "Выбираем любой ПВЗ на карте, нажать продолжить".ybm_run { _ in
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            ybm_wait(forVisibilityOf: [mapViewPage.toCurrentLocationButton])
            mapViewPage.toCurrentLocationButton.tap()
            mapViewPage.element.ybm_tapCenter()

            wait(forVisibilityOf: OutletMapInfoPage.current.element)
            OutletMapInfoPage.current.continueButton.tap()
        }

        "Выбираем любой ПВЗ, проверяем наличие лицензии в карточке ПВЗ".ybm_run { _ in
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            ybm_wait(forVisibilityOf: [mapViewPage.toCurrentLocationButton])
            mapViewPage.toCurrentLocationButton.tap()
            mapViewPage.element.ybm_tapCenter()

            wait(forVisibilityOf: OutletMapInfoPage.current.element)
            OutletMapInfoPage.current.element.swipe(to: OutletMapInfoPage.current.legalText)

            XCTAssertEqual(
                OutletMapInfoPage.current.legalText.label,
                "ООО «Торговый Дом Солос-М», юр.адрес: 121359, г. Москва, ул. Бобруйская, д.22, корп.1, офис 1, ОГРН 1037700088271. Лицензия №77РПА0003328 от 24 июня 2016 г."
            )

            OutletMapInfoPage.current.continueButton.tap()

            recipientPage = CheckoutRecipientPage.current
            wait(forVisibilityOf: recipientPage.element)
        }

        "Заполнить все поля, нажать \"продолжить\"".ybm_run { _ in
            recipientPage.nameTextField.typeText("Temik Vzorvish")
            recipientPage.emailTextField.typeText("vzorvanii@tip.ok")
            recipientPage.phoneTextField.typeText("88899900999")

            KeyboardPage.current.tapDone()
            recipientPage.continueButton.tap()

            checkoutPage = CheckoutPage.current
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем наличие надписей, лицензии, получателя, способ оплаты".ybm_run { _ in
            let cashonlyCell = checkoutPage.onlyCashCell()
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: cashonlyCell.element)
            XCTAssertEqual(
                cashonlyCell.title.label,
                "Оплата только наличными"
            )

            let postpaidCell = checkoutPage.postpaidCell()
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: postpaidCell.element)
            XCTAssertEqual(
                postpaidCell.title.label,
                "Оплата при получении"
            )

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.legalInfoCell().element)
            XCTAssertEqual(
                checkoutPage.legalInfoCell().title.label,
                "ООО «Торговый Дом Солос-М», юр.адрес: 121359, г. Москва, ул. Бобруйская, д.22, корп.1, офис 1, ОГРН 1037700088271. Лицензия №77РПА0003328 от 24 июня 2016 г."
            )

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.recipientInfoCell.element)
            XCTAssertEqual(
                checkoutPage.recipientInfoCell.title.label,
                "Temik Vzorvish\nvzorvanii@tip.ok, +7 (889) 990-09-99"
            )

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            checkoutPaymentPage = checkoutPage.paymentMethodCell.tap()

            checkoutPaymentPage.selectPaymentMethod(with: "CARD_ON_DELIVERY")
            XCTAssertEqual(checkoutPaymentPage.currentPaymentDetails, "Доступен только для части заказа")
            checkoutPaymentPage.selectPaymentMethod(with: "CASH_ON_DELIVERY")
            XCTAssertEqual(checkoutPaymentPage.currentPaymentDetails, "")

            checkoutPaymentPage.selectPaymentMethod(with: "CARD_ON_DELIVERY")
            checkoutPaymentPage.continueButton.tap()
        }

        "Нажимаем на условия использования".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.termsDisclaimerCell.element)
            checkoutPage.termsDisclaimerCell.element.tap()
            ybm_wait(forVisibilityOf: [NavigationBarPage.current.element])
            WebViewPage.current.navigationBar.closeButton.tap()
        }

        "Нажимаем на \"Информация о товарах и продавцах\"".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.merchantInfoDisclaimerCell.element)
            merchantPopupPage = checkoutPage.merchantInfoDisclaimerCell.tap()
            XCTAssertEqual(merchantPopupPage.fullName().caption.label, "ПОЛАРТ-ФАРМ")
            XCTAssertEqual(merchantPopupPage.ogrn().caption.label, "5087746234725")
            XCTAssertEqual(
                merchantPopupPage.actualAddress().caption.label,
                "г. Москва, шоссе Дмитровское, д. 30 корп. 1 этаж 1 пом. II ком. 1-10"
            )
            XCTAssertEqual(
                merchantPopupPage.juridicalAddress().caption.label,
                "г. Москва, шоссе Дмитровское, д. 30 корп. 1 этаж 1 пом. II ком. 1-10"
            )
            XCTAssertEqual(merchantPopupPage.schedule().caption.label, "Пн-Пт: 08:00-22:00, Сб-Вс: 09:00-21:00")
            XCTAssertEqual(merchantPopupPage.support().caption.label, "+7 (495) 482-49-55")

            merchantPopupPage.element
                .ybm_swipeCollectionView(toFullyReveal: merchantPopupPage.alcoWarning())

            XCTAssertEqual(merchantPopupPage.fullName(at: 1).caption.label, "ООО «Торговый Дом Солос-М»")
            XCTAssertEqual(merchantPopupPage.licenseNumber().caption.label, "77РПА0003328 от 24 июня 2016 г.")
            XCTAssertEqual(merchantPopupPage.ogrn(at: 1).caption.label, "1037700088271")
            XCTAssertEqual(
                merchantPopupPage.actualAddress(at: 1).caption.label,
                "121359, г. Москва, ул. Бобруйская, д.22, корп.1, офис 1"
            )
            XCTAssertEqual(
                merchantPopupPage.juridicalAddress(at: 1).caption.label,
                "121359, г. Москва, ул. Бобруйская, д.22, корп.1, офис 1"
            )
            XCTAssertEqual(merchantPopupPage.schedule(at: 1).caption.label, "Пн-Вс: 09:00-18:00")
            XCTAssertEqual(merchantPopupPage.support(at: 1).caption.label, "+7 (915) 116-36-06")
            XCTAssertEqual(
                merchantPopupPage.alcoWarning().label,
                "Чрезмерное употребление алкоголя вредит вашему здоровью. Приобретение алкогольной продукции осуществляется только в торговом зале магазина."
            )

            XCUIApplication().otherElements.matching(identifier: PopupEmdeddingAccessibility.backgroundView).element
                .swipe(to: .up, until: !merchantPopupPage.element.isVisible)
        }

        "Нажимаем кнопку Перейти к оплате и авторизовываемся".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой при получении")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            checkoutPage.paymentButton.tap()
        }

        "Проверяем экран Оформленного заказа".ybm_run { _ in
            finishMultiorderPage = FinishMultiorderPage.current
            ybm_wait {
                finishMultiorderPage.element.isVisible &&
                    NavigationBarPage.current.title.isVisible
            }
            XCTAssertEqual(NavigationBarPage.current.title.label, "Спасибо")

            XCTAssertEqual(
                finishMultiorderPage.deliveryStatus().label,
                "Заказ 44285455 22-26 апреля доставим в торговый зал"
            )
            finishMultiorderPage.element
                .ybm_swipeCollectionView(toFullyReveal: finishMultiorderPage.deliveryStatus(at: 1))
            XCTAssertEqual(
                finishMultiorderPage.deliveryStatus(at: 1).label,
                "Заказ 44285454 23-26 апреля доставим в торговый зал"
            )
        }
    }

    func testWithClickAndCollectAndKGT() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3871")
        Allure.addEpic("Чекаут 2.1")
        Allure.addFeature("Флоу первой покупки")
        Allure.addTitle("КГТ + C&C")

        enable(toggles: FeatureNames.applePay)

        var cartPage: CartPage!
        var mapViewPage: CheckoutMapViewPage!
        var editAddressPage: EditAddressPage!
        var recipientPage: CheckoutRecipientPage!
        var checkoutPage: CheckoutPage!
        var checkoutPaymentPage: CheckoutPaymentMethodPopupPage!
        var finishMultiorderPage: FinishMultiorderPage!

        let fullAddress = "Москва, Рублёвское шоссе, д. 18к1"
        let apart = "34"
        let entrance = "3"
        let intercom = "34K3432"
        let floor = "33"
        let comment = "Don't worry, be happy"
        let paymentMethods = ["APPLE_PAY": true, "YANDEX": true, "CARD_ON_DELIVERY": false, "CASH_ON_DELIVERY": false]

        var checkoutState = CheckoutState()

        "Мокаем новые моки".ybm_run { _ in
            checkoutState.resolveEnrichedAddressWithGps(address: .rublevskoye)

            stateManager?.setState(newState: checkoutState)
        }

        setupEnvironment(with: "Checkout_FirstFlowCCAndKGT")

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            mapViewPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: mapViewPage.element)
        }

        "Выбираем любую точку на карте, нажать привезти сюда".ybm_run { _ in
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            ybm_wait {
                mapViewPage.summary.addressTextField.text == fullAddress
            }
            ybm_wait {
                mapViewPage.pinTitle.label == "550 ₽"
            }

            editAddressPage = mapViewPage.summary.tap()
            XCTAssertEqual(editAddressPage.addressCellView.label, fullAddress)
        }

        "Заполняем все дополнительные поля".ybm_run { _ in
            editAddressPage.apartmentInput.typeText(apart)
            editAddressPage.entranceInput.typeText(entrance)
            editAddressPage.intercomInput.typeText(intercom)
            editAddressPage.floorInput.typeText(floor)
            editAddressPage.commentInput.typeText(comment)

            KeyboardPage.current.tapDone()
            editAddressPage.continueButton.tap()
        }

        "Выбираем любой ПВЗ на карте, нажать продолжить".ybm_run { _ in
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            mapViewPage.tapToCurrentLocation()
            mapViewPage.element.ybm_tapCenter()

            wait(forVisibilityOf: OutletMapInfoPage.current.element)
            OutletMapInfoPage.current.continueButton.tap()

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

        "Проверяем наличие надписей, получателя, способ оплаты".ybm_run { _ in
            checkoutPage = CheckoutPage.current
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.shipmentHeaderCell().title.label,
                "Доставка курьером 12 мая, 550 ₽"
            )
            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "\(fullAddress), кв. \(apart)\n\(entrance) подъезд, \(floor) этаж, домофон \(intercom), \"\(comment)\""
            )
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.dateSelectorCell(at: 0).element)
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "ср, 12 мая, 550 ₽")

            let timeSlot = checkoutPage.deliverySlotsCell(at: 0).slot(at: 0)
            XCTAssertTrue(timeSlot.isSelected)
            XCTAssertEqual(timeSlot.title, "09:00–18:00")

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.onlyCashCell().element)
            XCTAssertEqual(
                checkoutPage.onlyCashCell().title.label,
                "Оплата только наличными"
            )

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.recipientInfoCell.element)
            XCTAssertEqual(
                checkoutPage.recipientInfoCell.title.label,
                "Temik Vzorvish\nvzorvanii@tip.ok, +7 (889) 990-09-99"
            )

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")
        }

        "Обращаем внимание на саммари".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryItemsCell.element)
            let countFromItems = checkoutPage.summaryItemsCell.title.label.split(separator: " ")
            XCTAssertEqual(checkoutPage.summaryItemsCell.details.label, "15 995 ₽")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryDeliveryCell.element)
            let countFromDelivery = checkoutPage.summaryDeliveryCell.title.label.split(separator: " ")
            XCTAssertEqual(checkoutPage.summaryDeliveryCell.details.label, "550 ₽")

            XCTAssertEqual(countFromItems.last, "(7)")
            XCTAssertEqual(countFromDelivery.last, "(1)")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryTotalCell.element)
            XCTAssertEqual(checkoutPage.summaryTotalCell.details.label, "16 545 ₽")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryTotalPostpaidCell.element)
            XCTAssertEqual(checkoutPage.summaryTotalPostpaidCell.details.label, "96 ₽")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryTotalPrepaidCell.element)
            XCTAssertEqual(checkoutPage.summaryTotalPrepaidCell.details.label, "16 449 ₽")
        }

        "Нажимаем на способ оплаты".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(to: .up, toFullyReveal: checkoutPage.paymentMethodCell.element)

            checkoutPaymentPage = checkoutPage.paymentMethodCell.tap()

            for paymentMethod in paymentMethods {
                checkoutPaymentPage.selectPaymentMethod(with: paymentMethod.key)
                if paymentMethod.value {
                    XCTAssertEqual(
                        checkoutPaymentPage.currentPaymentDetails,
                        "Оплата 1 посылки онлайн: 16 449 ₽\nОплата 2 посылки при получении: 96 ₽"
                    )
                } else if paymentMethod.key == "CARD_ON_DELIVERY" {
                    XCTAssertEqual(checkoutPaymentPage.currentPaymentDetails, "Доступен только для части заказа")
                } else {
                    XCTAssertEqual(checkoutPaymentPage.currentPaymentDetails, "")
                }
            }

            checkoutPaymentPage.selectPaymentMethod(with: "YANDEX")
            checkoutPaymentPage.continueButton.tap()
        }

        "Нажимаем кнопку Перейти к оплате и авторизовываемся".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            checkoutPage.paymentButton.tap()
        }

        "Закрыть окно для ввода данных карты".ybm_run { _ in
            let trustPage = TrustPage.current

            ybm_wait(forVisibilityOf: [trustPage.element])
            XCTAssertEqual(trustPage.navigationBar.title.label, "Оплата картой")

            trustPage.navigationBar.closeButton.tap()
        }

        "Проверяем экран Оформленного заказа".ybm_run { _ in
            finishMultiorderPage = FinishMultiorderPage.current
            ybm_wait {
                finishMultiorderPage.element.isVisible &&
                    NavigationBarPage.current.title.isVisible
            }
            XCTAssertEqual(NavigationBarPage.current.title.label, "Спасибо")

            let detailsExpander = finishMultiorderPage.orderDetailsExpander()
            finishMultiorderPage.element.ybm_swipeCollectionView(toFullyReveal: detailsExpander)
            detailsExpander.tap()
            finishMultiorderPage.element
                .ybm_swipeCollectionView(toFullyReveal: finishMultiorderPage.paymentMethodOfOrderItem().element)
            XCTAssertEqual(finishMultiorderPage.paymentMethodOfOrderItem().textView.label, "Картой онлайн")

            finishMultiorderPage.element
                .ybm_swipeCollectionView(toFullyReveal: finishMultiorderPage.orderDetailsExpander(at: 1))
            finishMultiorderPage.orderDetailsExpander(at: 1).tap()
            finishMultiorderPage.element
                .ybm_swipeCollectionView(toFullyReveal: finishMultiorderPage.paymentMethodOfOrderItem(at: 1).element)
            XCTAssertEqual(
                finishMultiorderPage.paymentMethodOfOrderItem(at: 1).textView.label,
                "Наличными при получении"
            )
        }
    }

    private func setupEnvironment(with bundleName: String) {
        disable(
            toggles: FeatureNames.paymentSDK,
            FeatureNames.checkoutPresetsRedesign,
            FeatureNames.checkoutFirstFlowLikeRepeatedV2,
            FeatureNames.cartRedesign
        )
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = "55.741"
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = "37.432"

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: bundleName)
        }
    }
}
