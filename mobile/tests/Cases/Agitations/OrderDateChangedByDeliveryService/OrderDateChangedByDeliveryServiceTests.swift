import MarketUITestMocks
import XCTest

final class OrderDateChangedByDeliveryServiceTests: LocalMockTestCase {

    var popupPage: PopupPage!
    var orderState = OrdersState()

    func testMisclick() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4959")
        Allure.addEpic("Валидация 46 ЧП")
        Allure.addFeature("Попап подтверждения")
        Allure.addTitle("Пустой клик")

        openPopup(editingOptions: true)

        "Проверяем текст попапа".ybm_run { _ in
            XCTAssertEqual(popupPage.titleLabel.label, "Вы переносили доставку?")
        }

        "Проверяем наличие карусели с изображениями".ybm_run { _ in
            XCTAssertTrue(popupPage.itemsCollectionView.isVisible)
        }

        "Проверяем наличие кнопок".ybm_run { _ in
            XCTAssertTrue(popupPage.firstButton.isVisible)
            XCTAssertTrue(popupPage.lastButton.isVisible)
            XCTAssertEqual(popupPage.firstButton.label, "Да")
            XCTAssertEqual(popupPage.lastButton.label, "Нет, это не я")
        }

        "Тапаем по экрану и ждём скрытия попапа".ybm_run { _ in
            popupPage.dimmingBackgroundView.tap()
            wait(forInvisibilityOf: popupPage.element)
        }
    }

    func testAnswerYes() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4960")
        Allure.addEpic("Валидация 46 ЧП")
        Allure.addFeature("Попап подтверждения")
        Allure.addTitle("Пользователь переносил дату доставки")

        openPopup(editingOptions: true)

        "Подтверждаем агитацию".ybm_run { _ in
            popupPage.firstButton.tap()
        }

        "Проверяем текст попапа".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { self.popupPage.titleLabel.label == "Спасибо, что ответили" })
            ybm_wait(forFulfillmentOf: { self.popupPage.descriptionLabel.label == "Пусть покупка вас порадует" })
        }

        "Проверяем наличие карусели с изображениями".ybm_run { _ in
            XCTAssertTrue(popupPage.itemsCollectionView.isVisible)
        }

        "Проверяем наличие кнопки".ybm_run { _ in
            XCTAssertTrue(popupPage.lastButton.isVisible)
            XCTAssertEqual(popupPage.lastButton.label, "Понятно")
        }

        "Нажимаем на кнопку и ждём скрытия попапа".ybm_run { _ in
            popupPage.lastButton.tap()
            wait(forInvisibilityOf: popupPage.element)
        }
    }

    func testAnswerNoWithNoDeliveryDatesAvailable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4961")
        Allure.addEpic("Валидация 46 ЧП")
        Allure.addFeature("Попап подтверждения")
        Allure.addTitle("Пользователь не переносил дату + доступных дат нет")

        openPopup(editingOptions: false)

        "Отклоняем агитацию".ybm_run { _ in
            popupPage.lastButton.tap()
        }

        "Проверяем текст попапа".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { self.popupPage.titleLabel.label == "Спасибо, что ответили" })
            let label = "Заказ доставят 5 августа с 12:00 до 18:00. Разберёмся, почему служба доставки изменила дату."
            ybm_wait(forFulfillmentOf: { self.popupPage.descriptionLabel.label == label })
        }

        "Проверяем наличие карусели с изображениями".ybm_run { _ in
            XCTAssertTrue(popupPage.itemsCollectionView.isVisible)
        }

        "Проверяем наличие кнопки".ybm_run { _ in
            XCTAssertTrue(popupPage.lastButton.isVisible)
            XCTAssertEqual(popupPage.lastButton.label, "Понятно")
        }

        "Нажимаем на кнопку и ждём скрытия попапа".ybm_run { _ in
            popupPage.lastButton.tap()
            wait(forInvisibilityOf: popupPage.element)
        }
    }

    func testAnswerNoWithoutDeliveryDateChange() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4962")
        Allure.addEpic("Валидация 46 ЧП")
        Allure.addFeature("Попап подтверждения")
        Allure.addTitle("Пользователь не переносил дату и не хочет")

        openPopup(editingOptions: true)

        "Отклоняем агитацию".ybm_run { _ in
            popupPage.lastButton.tap()
        }

        "Проверяем текст попапа".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { self.popupPage.titleLabel.label == "Перенести доставку?" })
        }

        "Проверяем наличие кнопок".ybm_run { _ in
            XCTAssertTrue(popupPage.firstButton.isVisible)
            XCTAssertTrue(popupPage.lastButton.isVisible)
            XCTAssertEqual(popupPage.firstButton.label, "Нет")
            XCTAssertEqual(popupPage.lastButton.label, "Да, перенести")
        }

        "Нажимаем на кнопку `Нет` и ждём скрытия попапа".ybm_run { _ in
            popupPage.firstButton.tap()
            wait(forInvisibilityOf: popupPage.element)
        }
    }

    func testAnswerNoWithDeliveryDateChange() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4963")
        Allure.addEpic("Валидация 46 ЧП")
        Allure.addFeature("Попап подтверждения")
        Allure.addTitle("Пользователь не переносил дату, но хочет")

        var editPage: OrderEditPage!

        openPopup(editingOptions: true)

        "Отклоняем агитацию".ybm_run { _ in
            popupPage.lastButton.tap()
        }

        "Проверяем текст попапа".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { self.popupPage.titleLabel.label == "Перенести доставку?" })
        }

        "Проверяем наличие кнопок".ybm_run { _ in
            XCTAssertTrue(popupPage.firstButton.isVisible)
            XCTAssertTrue(popupPage.lastButton.isVisible)
            XCTAssertEqual(popupPage.firstButton.label, "Нет")
            XCTAssertEqual(popupPage.lastButton.label, "Да, перенести")
        }

        "Нажимаем на кнопку `Да, перенести`".ybm_run { _ in
            editPage = popupPage.orderEditButton.tap()
            wait(forVisibilityOf: editPage.deliveryDateTitle)
        }

        "Проверяем текст попапа с изменением даты доставки".ybm_run { _ in
            wait(forVisibilityOf: editPage.deliveryDateTitle)
            XCTAssertEqual(editPage.deliveryDateTitle.label, "Изменение даты доставки")
        }

        "Изменяем время и дату доставки и проверяем кнопку `Сохранить`".ybm_run { _ in
            editPage.dateSelector.element.tap()
            wait(forVisibilityOf: PickerWheelPage.current.element)

            PickerWheelPage.current.adjust(toPickerWheelValue: "Воскресенье, 19 января")
            KeyboardAccessoryPage.current.doneButton.tap()

            XCTAssertTrue(editPage.saveButton.button.isEnabled)
        }
    }

    // MARK: - Private Methods

    private func openPopup(editingOptions: Bool) {
        "Мокаем состояние".ybm_run { _ in
            setupAgitation(with: editingOptions)
        }

        "Открываем приложение".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
        }

        "Ждем попап".ybm_run { _ in
            popupPage = PopupPage.currentPopup
            wait(forVisibilityOf: popupPage.descriptionLabel)
        }

        "Мокаем пустую агитацию".ybm_run { _ in
            setupEmptyAgitation()
        }
    }

    // MARK: - Helper Methods

    typealias PopupPage = AgitationPopupPage
    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupAgitation(with editingOptions: Bool) {
        let orderId = "32398970"
        let order = SimpleOrder(
            id: orderId,
            status: .delivery,
            delivery: .init(
                deliveryPartnerType: .yandex,
                fromDate: "05-08-2020",
                toDate: "05-08-2020",
                fromTime: "12:00",
                toTime: "18:00",
                type: .service
            )
        )

        orderState.setOrderAgitations(agitations: [.orderDateChangedByDeliveryService(for: orderId)])
        orderState.setOrdersResolvers(mapper: OrdersHandlerMapper(orders: [order]), for: [.byId])
        orderState.setEditVariants(
            orderEditVariants: .boxberry(orderId: Int(orderId) ?? 0, possibilities: [.deliveryDates()])
        )
        orderState.setOrderEditingOptions(
            orderEditingOptions: editingOptions
                ? .defaultEditingOptions(orderId: orderId)
                : .emptyEditingOptions(orderId: orderId)
        )

        stateManager?.setState(newState: orderState)
    }

    private func setupEmptyAgitation() {
        orderState.setOrderAgitations(agitations: [])
        stateManager?.setState(newState: orderState)
    }
}
