import MarketUITestMocks
import XCTest

final class OrderEditDeliveryDateTests: OrderEditTestCase {

    // MARK: - Nested Types

    private enum Data {
        static let title = "Изменение даты доставки"
        static let date = "Дата доставки"
        static let time = "Время доставки"
        static let save = "Сохранить изменения"
        static let cancel = "Отменить"
    }

    // MARK: - Public

    func testEditDeliveryDateFields() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2974")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение даты доставки")
        Allure.addTitle("Проверка формы")

        var orderDetailsPage: OrderDetailsPage!
        var editPage: OrderEditPage!
        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Нажимаем `Изменить` дату доставки".ybm_run { _ in
            editPage = orderDetailsPage.deliveryDate.editButton.tap()
            wait(forVisibilityOf: editPage.element)
        }

        "Проверяем данные в попапе редактирования даты".ybm_run { _ in
            XCTAssertEqual(editPage.deliveryDateTitle.text, Data.title)
            XCTAssertEqual(editPage.dateSelector.placeholder.label, Data.date)
            XCTAssertEqual(editPage.timeSelector.placeholder.label, Data.time)
            XCTAssertEqual(editPage.saveButton.button.label, Data.save)
            XCTAssertEqual(editPage.cancelButton.button.label, Data.cancel)
            XCTAssertFalse(editPage.saveButton.button.isEnabled)
        }

        "Нажимаем на селектор `Дата доставки` и проверяем пикер".ybm_run { _ in
            editPage.dateSelector.element.tap()
            wait(forVisibilityOf: PickerWheelPage.current.element)

            XCTAssertEqual(PickerWheelPage.current.element.text, "Вторник, 10 декабря")
            XCTAssertGreaterThan(PickerWheelPage.current.element.otherElements.count, 1)
        }

        "Изменяем дату и проверяем кнопку `Сохранить`".ybm_run { _ in
            PickerWheelPage.current.adjust(toPickerWheelValue: "Пятница, 17 января")
            KeyboardAccessoryPage.current.doneButton.tap()

            XCTAssertEqual(editPage.dateSelector.value.label, "Пятница, 17 января")
            XCTAssertTrue(editPage.saveButton.button.isEnabled)
        }

        "Воозвращаем дату на исходную и проверяем кнопку `Сохранить`".ybm_run { _ in
            editPage.dateSelector.element.tap()
            wait(forVisibilityOf: PickerWheelPage.current.element)

            PickerWheelPage.current.adjust(toPickerWheelValue: "Вторник, 10 декабря")
            KeyboardAccessoryPage.current.doneButton.tap()

            XCTAssertEqual(editPage.dateSelector.value.label, "Вторник, 10 декабря")
            XCTAssertFalse(editPage.saveButton.button.isEnabled)
        }

        "Нажимаем на селектор `Время доставки` и проверяем пикер".ybm_run { _ in
            editPage.timeSelector.element.tap()
            wait(forVisibilityOf: PickerWheelPage.current.element)

            XCTAssertEqual(PickerWheelPage.current.element.text, "с 10:00 до 18:00")
            XCTAssertGreaterThan(PickerWheelPage.current.element.otherElements.count, 1)
        }

        "Изменяем время и проверяем кнопку `Сохранить`".ybm_run { _ in
            PickerWheelPage.current.adjust(toPickerWheelValue: "с 18:00 до 22:00")
            KeyboardAccessoryPage.current.doneButton.tap()

            XCTAssertEqual(editPage.timeSelector.value.label, "с 18:00 до 22:00")
            XCTAssertTrue(editPage.saveButton.button.isEnabled)
        }

        "Воозвращаем время на исходную и проверяем кнопку `Сохранить`".ybm_run { _ in
            editPage.timeSelector.element.tap()
            wait(forVisibilityOf: PickerWheelPage.current.element)

            PickerWheelPage.current.adjust(toPickerWheelValue: "с 10:00 до 18:00")
            KeyboardAccessoryPage.current.doneButton.tap()

            XCTAssertEqual(editPage.timeSelector.value.label, "с 10:00 до 18:00")
            XCTAssertFalse(editPage.saveButton.button.isEnabled)
        }

        "Изменяем время и дату доставки и проверяем кнопку `Сохранить`".ybm_run { _ in
            editPage.dateSelector.element.tap()
            wait(forVisibilityOf: PickerWheelPage.current.element)

            PickerWheelPage.current.adjust(toPickerWheelValue: "Суббота, 18 января")
            KeyboardAccessoryPage.current.doneButton.tap()

            editPage.timeSelector.element.tap()
            wait(forVisibilityOf: PickerWheelPage.current.element)

            PickerWheelPage.current.adjust(toPickerWheelValue: "с 18:00 до 22:00")
            KeyboardAccessoryPage.current.doneButton.tap()

            XCTAssertTrue(editPage.saveButton.button.isEnabled)
        }

        "Нажимаем `Отменить` и проверяем детали заказа".ybm_run { _ in
            orderDetailsPage = editPage.cancelButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)

            XCTAssertTrue(orderDetailsPage.deliveryDate.editButton.element.isVisible)
            XCTAssertEqual(orderDetailsPage.deliveryDate.value.label, "10 декабря, вторник, к 18:00")
        }
    }

    func test_OpenDeeplink_EditDeliveryDatePopupShows() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4340")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Открытие диплинка")
        Allure.addTitle("Проверка наличия попапа")

        var editPage: OrderEditPage!
        let orderId = "4815230"
        var deeplinkString: String {
            "yamarket://my/order/\(orderId)#changeDateTime"
        }

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId)
        }

        "Открываем приложение".ybm_run { _ in
            let morda = goToMorda()
            wait(forVisibilityOf: morda.element)
        }

        "Переходим в детали заказа по диплинку".ybm_run { _ in
            goToDeeplink(deeplink: deeplinkString)
        }

        "Проверяем данные в попапе редактирования даты".ybm_run { _ in
            editPage = OrderEditPage.current
            wait(forVisibilityOf: editPage.element)
            XCTAssertEqual(editPage.deliveryDateTitle.text, Data.title)
            XCTAssertEqual(editPage.dateSelector.placeholder.label, Data.date)
            XCTAssertEqual(editPage.timeSelector.placeholder.label, Data.time)
            XCTAssertEqual(editPage.saveButton.button.label, Data.save)
            XCTAssertEqual(editPage.cancelButton.button.label, Data.cancel)
            XCTAssertFalse(editPage.saveButton.button.isEnabled)
        }
    }

    func testEditDeliveryDateWhenApplied() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2975")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение даты доставки")
        Allure.addTitle("Изменение в статусе PENDING или UNPAID")

        var orderDetailsPage: OrderDetailsPage!
        var editPage: OrderEditPage!
        var editFinishedPage: OrderEditFinishedPage!
        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Нажимаем `Изменить` дату доставки".ybm_run { _ in
            editPage = orderDetailsPage.deliveryDate.editButton.tap()
            wait(forVisibilityOf: editPage.element)
        }

        changeDeliveryDate(in: editPage)

        "Мокаем состояние изменения даты доставки со статусом APPLIED".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [
                    .changeDeliveryDatesRequest(
                        orderId: orderId,
                        status: .applied,
                        fromDate: "19-01-2020",
                        toDate: "19-01-2020",
                        deliveryTimeInterval: newDeliveryTimeInterval
                    )
                ]
            )
        }

        "Нажимаем `Сохранить изменения`".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            wait(forVisibilityOf: editFinishedPage.element)
        }

        "Проверяем экран завершения изменения при ответе APPLIED".ybm_run { _ in
            XCTAssertEqual(editFinishedPage.image.identifier, "EditApplied")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Дата изменилась")
            XCTAssertEqual(
                editFinishedPage.subtitleCell.text,
                "Заказ №4815230 приедет Воскресенье, 19 января с 14:00 до 18:00"
            )
            XCTAssertTrue(editFinishedPage.nextButton.isVisible)
        }

        "Нажимаем `Продолжить`".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        checkChangeInfo(
            in: orderDetailsPage,
            orderId: orderId,
            image: "PushEnabled",
            title: "Дата доставки изменилась"
        )
    }

    func testDateChangeWhenInvalid() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3318")
        Allure.addEpic("Проверка статуса заказа")
        Allure.addFeature("Изменение даты доставки")
        Allure.addTitle("Изменение в статусе INVALID")

        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .invalid)]
            )
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        checkChangeInfo(
            in: orderDetailsPage,
            orderId: orderId,
            image: "CommonBell",
            title: "Перенести доставку не получилось. Обратитесь в поддержку, и мы что-нибудь придумаем"
        )
    }

    func testEditDeliveryDateWhenOrderDelivered() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2977")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение даты доставки")
        Allure.addTitle("Заказ доставлен")

        let orderId = "4815230"
        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId, status: .delivered, needOrderEditPossibilities: false)
        }

        "Переходим в \"Мои заказы\"".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Проверяем статус заказа".ybm_run { _ in
            XCTAssertEqual(ordersListPage.status(orderId: orderId).label, "Уже у вас")
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем отсуствие кнопки \"Изменить\" у даты доставки".ybm_run { _ in
            XCTAssertFalse(orderDetailsPage.deliveryDate.editButton.element.isVisible)
        }
    }

    func testEditDeliveryDateWhenOrderInDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2989")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение даты доставки")
        Allure.addTitle("Изменение в статусе PROCESSING или DELIVERY")

        let orderId = "4815230"
        var orderDetailsPage: OrderDetailsPage!
        var editPage: OrderEditPage!
        var editFinishedPage: OrderEditFinishedPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Нажимаем `Изменить` дату доставки".ybm_run { _ in
            editPage = orderDetailsPage.deliveryDate.editButton.tap()
            wait(forVisibilityOf: editPage.element)
        }

        changeDeliveryDate(in: editPage)

        "Мокаем состояние изменения даты доставки со статусом PROCESSING".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .processing)]
            )
        }

        "Нажимаем `Сохранить изменения`".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            wait(forVisibilityOf: editFinishedPage.element)
        }

        "Проверяем экран завершения изменения при ответе PROCESSING".ybm_run { _ in
            XCTAssertEqual(editFinishedPage.image.identifier, "EditProcessing")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Ждём подтверждения\nот службы доставки")
            XCTAssertEqual(editFinishedPage.subtitleCell.text, "Мы напишем вам в ближайшие 30 минут.")
            XCTAssertTrue(editFinishedPage.nextButton.isVisible)
        }

        "Нажимаем `Продолжить`".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        checkChangeInfo(
            in: orderDetailsPage,
            orderId: orderId,
            image: "EditRequestTimer",
            title: "Переносим дату доставки"
        )
    }

    func testEditDeliveryDateWhenEditRequestRejected() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2990")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение даты доставки")
        Allure.addTitle("Чекаут ответил REJECTED")

        let orderId = "4815230"
        var orderDetailsPage: OrderDetailsPage!
        var editPage: OrderEditPage!
        var editFinishedPage: OrderEditFinishedPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Нажимаем `Изменить` дату доставки".ybm_run { _ in
            editPage = orderDetailsPage.deliveryDate.editButton.tap()
            wait(forVisibilityOf: editPage.element)
        }

        changeDeliveryDate(in: editPage)

        "Мокаем состояние изменения даты доставки со статусом REJECTED".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .rejected)]
            )
        }

        "Нажимаем `Сохранить изменения`".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            wait(forVisibilityOf: editFinishedPage.element)
        }

        "Проверяем экран завершения изменения при ответе REJECTED".ybm_run { _ in
            XCTAssertEqual(editFinishedPage.image.identifier, "EditProcessing")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Перенести доставку не получилось")
            XCTAssertEqual(editFinishedPage.subtitleCell.text, "Обратитесь в поддержку, и мы что-нибудь придумаем")
            XCTAssertTrue(editFinishedPage.nextButton.isVisible)
        }

        "Нажимаем `Продолжить`".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        checkChangeInfo(
            in: orderDetailsPage,
            orderId: orderId,
            image: "CommonBell",
            title: "Перенести доставку не получилось. Обратитесь в поддержку, и мы что-нибудь придумаем"
        )
    }

    func testEditDeliveryDateWhenNoDatesAvailable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2991")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение даты доставки")
        Allure.addTitle("Доступных дат нет")

        let orderId = "4815230"
        var orderDetailsPage: OrderDetailsPage!
        var editPage: OrderEditPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId, needOrderEditingOptions: false)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Нажимаем `Изменить` дату доставки".ybm_run { _ in
            editPage = orderDetailsPage.deliveryDate.editButton.tap()
            wait(forVisibilityOf: editPage.element)
            wait(forVisibilityOf: editPage.warning)
        }

        "Проверяем данные в попапе редактирования даты".ybm_run { _ in
            editPage.dateSelector.element.tap()
            XCTAssertFalse(PickerWheelPage.current.element.isVisible)

            editPage.timeSelector.element.tap()
            XCTAssertFalse(PickerWheelPage.current.element.isVisible)

            XCTAssertEqual(editPage.warning.label, "Нет доступных дат для переноса")
            XCTAssertFalse(editPage.saveButton.button.isEnabled)
            XCTAssertTrue(editPage.cancelButton.button.isEnabled)
        }
    }

    func testEditDeliveryDateWhenEditRequestBecameRejected() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2995")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение даты доставки")
        Allure.addTitle("Заявка не подтверждена")

        let orderId = "4815230"
        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!
        var editPage: OrderEditPage!
        var editFinishedPage: OrderEditFinishedPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Нажимаем `Изменить` дату доставки".ybm_run { _ in
            editPage = orderDetailsPage.deliveryDate.editButton.tap()
            wait(forVisibilityOf: editPage.element)
        }

        changeDeliveryDate(in: editPage)

        "Мокаем состояние изменения даты доставки со статусом PROCESSING".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .processing)]
            )
        }

        "Нажимаем `Сохранить изменения`".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            wait(forVisibilityOf: editFinishedPage.element)
        }

        "Проверяем экран завершения изменения при ответе REJECTED".ybm_run { _ in
            XCTAssertEqual(editFinishedPage.image.identifier, "EditProcessing")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Ждём подтверждения\nот службы доставки")
            XCTAssertEqual(editFinishedPage.subtitleCell.text, "Мы напишем вам в ближайшие 30 минут.")
            XCTAssertTrue(editFinishedPage.nextButton.isVisible)
        }

        "Нажимаем `Продолжить`".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем плашку изменения даты доставки".ybm_run { _ in
            guard let editInfo = orderDetailsPage.editRequests.first else {
                XCTFail("couldn't be nil")
                return
            }
            XCTAssertTrue(editInfo.image.exists)
            XCTAssertEqual(editInfo.image.label, "EditRequestTimer")
            XCTAssertTrue(editInfo.title.isVisible)
            XCTAssertEqual(editInfo.title.label, "Переносим дату доставки")
        }

        "Мокаем состояние изменения даты доставки со статусом REJECTED".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .rejected)],
                needOrderEditPossibilities: true,
                enableEditPossibilites: false
            )
        }

        "Переходим назад в `Мои заказы`".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            ordersListPage = OrdersListPage.current
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Проверяем плашку изменения даты доставки в `Моих заказах`".ybm_run { _ in
            guard let editRequest = ordersListPage.editRequests(orderId: orderId).first else {
                XCTFail("No edit requests")
                return
            }
            XCTAssertTrue(editRequest.image.exists)
            XCTAssertEqual(editRequest.image.label, "CommonBell")
            XCTAssertTrue(editRequest.title.isVisible)
            XCTAssertEqual(
                editRequest.title.label,
                "Перенести доставку не получилось. Обратитесь в поддержку, и мы что-нибудь придумаем"
            )
        }

        "Переходим детали заказа".ybm_run { _ in
            let detailsButton = ordersListPage.detailsButton(orderId: orderId)
            ordersListPage.element.ybm_swipeCollectionView(toFullyReveal: detailsButton.element)
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
            ybm_wait(forFulfillmentOf: {
                orderDetailsPage.element.isVisible
            })
        }

        "Проверяем данные в `Деталях заказа`".ybm_run { _ in
            guard let editInfo = orderDetailsPage.editRequests.first else {
                XCTFail("couldn't be nil")
                return
            }
            XCTAssertTrue(editInfo.image.exists)
            XCTAssertTrue(editInfo.title.isVisible)
            XCTAssertEqual(
                editInfo.title.label,
                "Перенести доставку не получилось. Обратитесь в поддержку, и мы что-нибудь придумаем"
            )
            XCTAssertFalse(orderDetailsPage.deliveryDate.editButton.element.isVisible)
        }
    }

    func testEditDeliveryDateWhenNoConnection() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2992")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение даты доставки")
        Allure.addTitle("Отключить интернет")

        let orderId = "4815230"
        var orderDetailsPage: OrderDetailsPage!
        var editPage: OrderEditPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Нажимаем `Изменить` дату доставки".ybm_run { _ in
            editPage = orderDetailsPage.deliveryDate.editButton.tap()
            wait(forVisibilityOf: editPage.element)
        }

        changeDeliveryDate(in: editPage)

        "Нажимаем `Сохранить изменения` и проверям алерт интернета".ybm_run { _ in
            editPage.saveButton.element.tap()
            let popup = DefaultToastPopupPage.currentPopup
            wait(forVisibilityOf: popup.element)
            XCTAssertTrue(popup.text.label.starts(with: "Что-то пошло не так Код ошибки:"))
        }
    }

}
