import MarketUITestMocks
import XCTest

final class OrderEditRecipientTests: OrderEditTestCase {

    typealias Recipient = Order.Delivery.Recipient
    typealias Name = Recipient.Name

    // MARK: - Public

    let defaultRecipient: Recipient = .defaultRecipient

    let changedName = Name(firstName: "Иванов", lastName: "Ивак")
    let changedPhone = "89277747475"
    let changedFormattedPhone = "+7 (927) 774-74-75"

    let changedName2 = Name(firstName: "Петров", lastName: "Петр")
    let changedPhone2 = "89278788787"
    let changedFormattedPhone2 = "+7 (927) 878-87-87"

    func testEditRecipientNameWhenCheckFeilds() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3067")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение ФИО")
        Allure.addTitle("Проверка форм")

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

        "Нажимаем `Изменить` получателя".ybm_run { _ in
            editPage = orderDetailsPage.recipient.editButton.tap()
            wait(forVisibilityOf: editPage.element)
        }

        "Проверяем поля в попапе редактирования получателя".ybm_run { _ in
            XCTAssertEqual(editPage.nameField.field.text, defaultRecipient.recipientName?.fullName())
            XCTAssertEqual(editPage.phoneField.field.text, defaultRecipient.phone)
        }

        changeRecipientName(in: editPage, name: changedName.fullName())
        changeRecipientPhone(in: editPage, phone: changedPhone, formattedPhone: changedFormattedPhone)

        "Мокаем  состояние изменения данных получателя со статусом APPLIED".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [
                    .changeRecipientRequest(
                        orderId: orderId,
                        status: .applied,
                        recipientName: changedName,
                        phone: changedFormattedPhone
                    )
                ],
                buyer: .customBuyer(phone: changedFormattedPhone, name: changedName)
            )
        }

        "Сохраняем изменения".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            wait(forVisibilityOf: editFinishedPage.element)
        }

        "Проверяем экран завершения изменения при ответе APPLIED ".ybm_run { _ in
            XCTAssertEqual(editFinishedPage.titleCell.text, "Получатель изменён")
            XCTAssertEqual(
                editFinishedPage.subtitleCell.text,
                "Новый получатель \(changedName.fullName()). Курьер позвонит в день доставки на номер \(changedFormattedPhone.withNonBreakingSpace())"
            )
        }

        "Нажимаем `Продолжить` и закрываем попап".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем обновленные данные в деталях заказа".ybm_run { _ in
            orderDetailsPage.element
                .ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.recipient.editButton.element)
            XCTAssertEqual(
                orderDetailsPage.recipient.value.label,
                "\(changedName.fullName()), \(changedFormattedPhone)"
            )
        }

        "Нажимаем `Изменить` получателя".ybm_run { _ in
            editPage = orderDetailsPage.recipient.editButton.tap()
            wait(forVisibilityOf: editPage.element)
        }

        "Проверяем поля в попапе редактирования получателя".ybm_run { _ in
            XCTAssertEqual(editPage.nameField.field.text, changedName.fullName())
            XCTAssertEqual(editPage.phoneField.field.text, changedFormattedPhone)
        }

        changeRecipientName(in: editPage, name: changedName2.fullName())
        changeRecipientPhone(in: editPage, phone: changedPhone2, formattedPhone: changedFormattedPhone2)

        "Мокаем  состояние изменения данных получателя со статусом APPLIED".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [
                    .changeRecipientRequest(
                        orderId: orderId,
                        status: .applied,
                        recipientName: changedName2,
                        phone: changedFormattedPhone2
                    )
                ],
                buyer: .customBuyer(phone: changedFormattedPhone2, name: changedName2)
            )
        }

        "Сохраняем изменения".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            wait(forVisibilityOf: editFinishedPage.element)
        }

        "Проверяем экран завершения изменения при ответе APPLIED ".ybm_run { _ in
            XCTAssertEqual(editFinishedPage.image.identifier, "EditApplied")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Получатель изменён")
            XCTAssertEqual(
                editFinishedPage.subtitleCell.text,
                "Новый получатель \(changedName2.fullName()). Курьер позвонит в день доставки на номер \(changedFormattedPhone2.withNonBreakingSpace())"
            )
        }

        "Нажимаем `Продолжить` и закрываем попап".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем обновленные данные в деталях заказа".ybm_run { _ in
            orderDetailsPage.element
                .ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.recipient.editButton.element)
            XCTAssertEqual(
                orderDetailsPage.recipient.value.label,
                "\(changedName2.fullName()), \(changedFormattedPhone2)"
            )
        }
    }

    func testEditRecipientNameWhenApplied() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3067")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение ФИО")
        Allure.addTitle("Изменить ФИО - доставка курьером")

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

        "Нажимаем \"Изменить\" получателя".ybm_run { _ in
            editPage = orderDetailsPage.recipient.editButton.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.element.isVisible
                    && editPage.recipientTitle.isVisible
                    && editPage.nameField.element.isVisible
                    && editPage.phoneField.element.isVisible
                    && editPage.disclaimer.isVisible
                    && editPage.saveButton.element.isVisible
                    && editPage.cancelButton.element.isVisible
            })
        }

        "Проверяем данные в попапе редактирования получателя".ybm_run { _ in
            XCTAssertEqual(editPage.recipientTitle.text, "Изменение получателя")
            XCTAssertEqual(editPage.nameField.placeholder.label, "Имя и фамилия по паспорту")
            XCTAssertEqual(editPage.phoneField.placeholder.label, "Телефон")
            XCTAssertEqual(editPage.saveButton.button.label, "Сохранить изменения")
            XCTAssertEqual(editPage.cancelButton.button.label, "Отменить")
            XCTAssertFalse(editPage.saveButton.button.isEnabled)
        }

        "Нажимаем \"Отменить\"".ybm_run { _ in
            orderDetailsPage = editPage.cancelButton.tap()
            ybm_wait(forFulfillmentOf: {
                orderDetailsPage.element.isVisible
            })
        }

        "Снова нажимаем \"Изменить\" получателя".ybm_run { _ in
            editPage = orderDetailsPage.recipient.editButton.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.element.isVisible
            })
        }

        "Тап на поле \"Имя и Фамилия по паспорту\"".ybm_run { _ in
            editPage.nameField.element.tap()
            ybm_wait { editPage.nameField.clearButton.isVisible }
        }

        "Нажимаем на крестик".ybm_run { _ in
            editPage.nameField.clearButton.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.nameField.field.text.isEmpty
            })
        }

        "Вводим корректные Имя и Фамилию и тапаем на поле Телефон".ybm_run { _ in
            editPage.nameField.field.typeText(changedName.fullName())
            KeyboardAccessoryPage.current.doneButton.tap()
            editPage.phoneField.element.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.nameField.validationMark.isVisible
                    && !editPage.nameMessage.isVisible
                    && editPage.saveButton.button.isEnabled
            })
        }

        "Мокаем  состояние изменения данных получателя со статусом APPLIED".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [
                    .changeRecipientRequest(
                        orderId: orderId,
                        status: .applied,
                        recipientName: changedName,
                        phone: changedFormattedPhone
                    )
                ],
                buyer: .customBuyer(phone: changedFormattedPhone, name: changedName)
            )
        }

        "Сохраняем изменения".ybm_run { _ in
            KeyboardAccessoryPage.current.doneButton.tap()
            editFinishedPage = editPage.saveButton.tap()
            ybm_wait(forFulfillmentOf: {
                editFinishedPage.element.isVisible
                    && editFinishedPage.nextButton.isVisible
            })
        }

        "Проверяем экран завершения изменения при ответе APPLIED ".ybm_run { _ in
            XCTAssertEqual(editFinishedPage.image.identifier, "EditApplied")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Получатель изменён")
            XCTAssertEqual(
                editFinishedPage.subtitleCell.text,
                "Новый получатель \(changedName.fullName()). Курьер позвонит в день доставки на номер \(changedFormattedPhone.withNonBreakingSpace())"
            )
        }

        "Нажимаем \"Продолжить\" и закрываем попап".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем нового получателя в деталях заказа".ybm_run { _ in
            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.recipient.element)
            XCTAssertEqual(orderDetailsPage.recipient.title.label, "Получатель")
            XCTAssertEqual(
                orderDetailsPage.recipient.value.label,
                "\(changedName.fullName()), \(changedFormattedPhone)"
            )
        }

        checkChangeInfo(
            in: orderDetailsPage,
            orderId: orderId,
            image: "PushEnabled",
            title: "Получатель изменён"
        )
    }

    func testRecipientChangeWhenInvalid() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3317")
        Allure.addEpic("Проверка статуса заказа")
        Allure.addFeature("Изменение ФИО")
        Allure.addTitle("Изменение в статусе INVALID")

        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeRecipientRequest(orderId: orderId, status: .invalid)]
            )
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        checkChangeInfo(
            in: orderDetailsPage,
            orderId: orderId,
            image: "CommonBell",
            title: "Не получилось изменить данные получателя. Обратитесь в поддержку, и мы что-нибудь придумаем."
        )
    }

    func testRecipientChangeErrors() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3068")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение ФИО")
        Allure.addTitle("Ошибки")

        var orderDetailsPage: OrderDetailsPage!
        var editPage: OrderEditPage!
        var editFinishedPage: OrderEditFinishedPage!
        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
            ybm_wait(forFulfillmentOf: {
                orderDetailsPage.element.isVisible
            })
        }

        "Нажимаем \"Изменить\" получателя".ybm_run { _ in
            editPage = orderDetailsPage.recipient.editButton.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.element.isVisible
                    && editPage.recipientTitle.isVisible
                    && editPage.nameField.element.isVisible
                    && editPage.phoneField.element.isVisible
                    && editPage.disclaimer.isVisible
                    && editPage.saveButton.element.isVisible
                    && editPage.cancelButton.element.isVisible
            })
        }

        "Тапаем на поле Имя и Фамилия".ybm_run { _ in
            editPage.nameField.element.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.nameField.clearButton.isVisible
            })
        }

        "Очищаем поле и вводим 2 буквы через пробел".ybm_run { _ in
            editPage.nameField.clearButton.tap()
            editPage.nameField.field.typeText("A A")
            editPage.phoneField.element.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.nameMessage.isVisible
                    && editPage.phoneField.clearButton.isVisible
            })
            XCTAssertEqual(editPage.nameMessage.text, "Имя и фамилия должны состоять хотя бы из двух букв")
        }

        "Очищаем поле Имени и Фамилии".ybm_run { _ in
            editPage.nameField.element.tap()
            editPage.nameField.clearButton.tap()
            editPage.phoneField.element.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.nameMessage.isVisible
            })
            XCTAssertEqual(editPage.nameMessage.text, "Укажите имя и фамилию")
        }

        "Вводим корректную Имя и Фамилию".ybm_run { _ in
            editPage.nameField.element.tap()
            editPage.nameField.field.typeText(changedName.fullName())
            editPage.phoneField.element.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.nameField.validationMark.isVisible
                    && editPage.saveButton.button.isEnabled
            })
        }

        "Мокаем состояние изменения данных получателя со статусом PROCESSING".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeRecipientRequest(orderId: orderId, status: .processing)],
                buyer: .customBuyer(phone: changedFormattedPhone, name: changedName)
            )

        }

        "Сохраняем изменения".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            ybm_wait(forFulfillmentOf: {
                editFinishedPage.element.isVisible
            })
        }

        "Проверяем экран завершения изменения при ответе PROCESSING".ybm_run { _ in
            XCTAssertTrue(editFinishedPage.image.exists)
            XCTAssertTrue(editFinishedPage.titleCell.isVisible)
            XCTAssertTrue(editFinishedPage.subtitleCell.isVisible)
            XCTAssertTrue(editFinishedPage.nextButton.isVisible)

            XCTAssertEqual(editFinishedPage.image.identifier, "EditProcessing")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Ждём подтверждения\nот службы доставки")
            XCTAssertEqual(editFinishedPage.subtitleCell.text, "Мы напишем вам в ближайшие 30 минут.")
            XCTAssertEqual(editFinishedPage.nextButton.label, "Продолжить")
        }

        "Нажимаем \"Продолжить\" и закрываем попап".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        checkChangeInfo(
            in: orderDetailsPage,
            orderId: orderId,
            image: "EditRequestTimer",
            title: "Заказ ожидает изменения данных получателя"
        )
    }

    func testEditRecipientPhoneWhenApplied() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3069")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение ФИО")
        Allure.addTitle("Изменить телефон")

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

        "Нажимаем \"Изменить\" получателя".ybm_run { _ in
            editPage = orderDetailsPage.recipient.editButton.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.element.isVisible
            })
        }

        "Тап на поле \"Телефон\"".ybm_run { _ in
            editPage.phoneField.element.tap()
            ybm_wait { editPage.phoneField.clearButton.isVisible }
        }

        "Нажимаем на крестик".ybm_run { _ in
            editPage.phoneField.clearButton.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.phoneField.field.text.isEmpty
            })
        }

        "Вводим корректное значение телефона".ybm_run { _ in
            editPage.phoneField.field.typeText("89991232424")
            KeyboardAccessoryPage.current.doneButton.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.phoneField.validationMark.isVisible
                    && editPage.phoneMessage.isVisible == false
                    && editPage.saveButton.button.isEnabled
            })
            XCTAssertEqual(editPage.phoneField.field.text, "+7 (999) 123-24-24")
        }

        "Мокаем  состояние изменения данных получателя со статусом APPLIED".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [
                    .changeRecipientRequest(
                        orderId: orderId,
                        status: .applied,
                        recipientName: changedName,
                        phone: changedFormattedPhone
                    )
                ],
                buyer: .customBuyer(phone: changedFormattedPhone, name: changedName)
            )
        }

        "Сохраняем изменения".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            ybm_wait(forFulfillmentOf: {
                editFinishedPage.element.isVisible
                    && editFinishedPage.nextButton.isVisible
            })
        }

        "Проверяем экран завершения изменения при ответе APPLIED ".ybm_run { _ in
            XCTAssertEqual(editFinishedPage.image.identifier, "EditApplied")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Получатель изменён")
            XCTAssertEqual(
                editFinishedPage.subtitleCell.text,
                "Новый получатель \(changedName.fullName()). Курьер позвонит в день доставки на номер \(changedFormattedPhone.withNonBreakingSpace())"
            )
        }

        "Нажимаем \"Продолжить\" и закрываем попап".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем нового получателя в деталях заказа".ybm_run { _ in
            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.recipient.element)
            XCTAssertEqual(orderDetailsPage.recipient.title.label, "Получатель")
            XCTAssertEqual(
                orderDetailsPage.recipient.value.label,
                "\(changedName.fullName()), \(changedFormattedPhone)"
            )
        }

        checkChangeInfo(
            in: orderDetailsPage,
            orderId: orderId,
            image: "PushEnabled",
            title: "Получатель изменён"
        )
    }

    func testCantChangeRecipientWhenOrderDelivered() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3070")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение ФИО")
        Allure.addTitle("Невозможность изменить получателя, когда заказ доставлен")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            setupState(orderId: orderId, status: .delivered, needOrderEditPossibilities: false)
        }

        "Переходим в \"Мои заказы\"".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
        }

        "Проверяем статус заказа".ybm_run { _ in
            XCTAssertEqual(ordersListPage.status(orderId: orderId).label, "Уже у вас")
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
            ybm_wait(forFulfillmentOf: {
                orderDetailsPage.element.isVisible
            })
        }

        "Проверяем отсуствие кнопки \"Изменить\" у получателя".ybm_run { _ in
            XCTAssertFalse(orderDetailsPage.recipient.editButton.element.isVisible)
        }
    }

    func testRejectedChangeInfoAfterRecipientEditProcessing() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3074")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение ФИО")
        Allure.addTitle("Заявка не подтверждена")

        var ordersListPage: OrdersListPage!
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

        "Нажимаем \"Изменить\" получателя".ybm_run { _ in
            editPage = orderDetailsPage.recipient.editButton.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.element.isVisible
            })
        }

        "Вводим новые данные получателя".ybm_run { _ in
            editPage.nameField.element.tap()
            editPage.nameField.field.typeText("\(XCUIKeyboardKey.delete.rawValue)к")
            KeyboardAccessoryPage.current.nextButton.tap()

            editPage.phoneField.element.tap()
            editPage.phoneField.field.typeText("\(XCUIKeyboardKey.delete.rawValue)5")
            KeyboardAccessoryPage.current.doneButton.tap()
        }

        "Мокаем состояние изменения данных получателя со статусом PROCESSING".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeRecipientRequest(orderId: orderId, status: .processing)]
            )
        }

        "Сохраняем нового получателя".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            ybm_wait(forFulfillmentOf: {
                editFinishedPage.element.isVisible
            })
        }

        "Проверяем экран завершения изменения при ответе PROCESSING".ybm_run { _ in
            XCTAssertTrue(editFinishedPage.image.exists)
            XCTAssertTrue(editFinishedPage.titleCell.isVisible)
            XCTAssertTrue(editFinishedPage.subtitleCell.isVisible)

            XCTAssertEqual(editFinishedPage.image.identifier, "EditProcessing")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Ждём подтверждения\nот службы доставки")
            XCTAssertEqual(editFinishedPage.subtitleCell.text, "Мы напишем вам в ближайшие 30 минут.")
        }

        "Закрываем попап изменения получателя".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем плашку изменения получателя заказа".ybm_run { _ in
            guard let changeInfo = orderDetailsPage.editRequests.first else {
                XCTFail("couldn't be nil")
                return
            }
            XCTAssertTrue(changeInfo.image.exists)
            XCTAssertEqual(changeInfo.image.label, "EditRequestTimer")
            XCTAssertTrue(changeInfo.title.isVisible)
            XCTAssertEqual(changeInfo.title.label, "Заказ ожидает изменения данных получателя")
        }

        "Мокаем состояние изменения данных получателя со статусом REJECTED".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeRecipientRequest(orderId: orderId, status: .rejected)]
            )
        }

        "Переходим назад в \"Мои заказы\"".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            ordersListPage = OrdersListPage.current
            ybm_wait(forFulfillmentOf: {
                ordersListPage.element.isVisible
            })
        }

        "Проверяем плашку изменения получателя заказа в \"Моих заказах\"".ybm_run { _ in
            guard let editRequest = ordersListPage.editRequests(orderId: orderId).first else {
                XCTFail("No edit requests")
                return
            }
            XCTAssertTrue(editRequest.image.exists)
            XCTAssertEqual(editRequest.image.label, "CommonBell")
            XCTAssertTrue(editRequest.title.isVisible)
            XCTAssertEqual(
                editRequest.title.label,
                "Не получилось изменить данные получателя. Обратитесь в поддержку, и мы что-нибудь придумаем."
            )
        }

        "Переходим детали заказа".ybm_run { _ in
            let detailsButton = ordersListPage.detailsButton(orderId: orderId)
            ordersListPage.element.ybm_swipeCollectionView(toFullyReveal: detailsButton.element)
            orderDetailsPage = detailsButton.tap()
            ybm_wait(forFulfillmentOf: {
                orderDetailsPage.element.isVisible
            })
        }

        "Проверяем плашку изменения получателя заказа".ybm_run { _ in
            guard let changeInfo = orderDetailsPage.editRequests.first else {
                XCTFail("couldn't be nil")
                return
            }
            XCTAssertTrue(changeInfo.image.exists)
            XCTAssertEqual(changeInfo.image.label, "CommonBell")
            XCTAssertTrue(changeInfo.title.isVisible)
            XCTAssertEqual(
                changeInfo.title.label,
                "Не получилось изменить данные получателя. Обратитесь в поддержку, и мы что-нибудь придумаем."
            )
        }
    }

    func testRejectedChangeInfoWhenRecipientEditRejected() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3076")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение ФИО")
        Allure.addTitle("Чекаут ответил REJECTED")

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

        "Нажимаем \"Изменить\" получателя".ybm_run { _ in
            editPage = orderDetailsPage.recipient.editButton.tap()
            ybm_wait(forFulfillmentOf: {
                editPage.element.isVisible
            })
        }

        "Вводим новые данные получателя".ybm_run { _ in
            editPage.nameField.element.tap()
            editPage.nameField.field.typeText("\(XCUIKeyboardKey.delete.rawValue)к")
            KeyboardAccessoryPage.current.nextButton.tap()

            editPage.phoneField.element.tap()
            editPage.phoneField.field.typeText("\(XCUIKeyboardKey.delete.rawValue)5")
            KeyboardAccessoryPage.current.doneButton.tap()
        }

        "Мокаем состояние изменения данных получателя со статусом REJECTED".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changeRecipientRequest(orderId: orderId, status: .rejected)]
            )
        }

        "Сохраняем нового получателя".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            ybm_wait(forFulfillmentOf: {
                editFinishedPage.element.isVisible
            })
        }

        "Проверяем экран завершения изменения при ответе REJECTED".ybm_run { _ in
            XCTAssertTrue(editFinishedPage.image.exists)
            XCTAssertTrue(editFinishedPage.titleCell.isVisible)
            XCTAssertTrue(editFinishedPage.subtitleCell.isVisible)

            XCTAssertEqual(editFinishedPage.image.identifier, "EditProcessing")
            XCTAssertEqual(editFinishedPage.titleCell.text, "Не получилось изменить данные получателя")
            XCTAssertEqual(editFinishedPage.subtitleCell.text, "Обратитесь в поддержку, и мы что-нибудь придумаем")
        }

        "Закрываем попап изменения получателя".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        let title = "Не получилось изменить данные получателя. Обратитесь в поддержку, и мы что-нибудь придумаем."
        checkChangeInfo(
            in: orderDetailsPage,
            orderId: orderId,
            image: "CommonBell",
            title: title
        )
    }

}

private extension String {
    func withNonBreakingSpace() -> String {
        replacingOccurrences(of: " ", with: "\u{00a0}")
    }
}
