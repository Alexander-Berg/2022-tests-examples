import MarketUITestMocks
import XCTest

final class OrderEditAddressTests: OrderEditAddressTestCase {

    func testEditDeliveryAddressInAvailableArea() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6153")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение адреса доставки")
        Allure.addTitle("Выбор точки в зоне покрытия")

        var editMapPage: OrderEditAddressMapPage!
        var editPage: OrderEditAddressPage!
        var editFinishedPage: OrderEditFinishedPage!

        openMap(address: .novinskiy)

        "В строке адреса вводим новый адрес".ybm_run { _ in
            editMapPage = OrderEditAddressMapPage.current
            wait(forVisibilityOf: editMapPage.summary.doneButton)
            ybm_wait(forFulfillmentOf: { editMapPage.summary.doneButton.isEnabled })

            XCTAssertEqual(editMapPage.summary.addressTextField.text, "Москва, Новинский бульвар, д. 8")
            editMapPage.summary.addressTextField.tap()
            wait(forVisibilityOf: editMapPage.search.addressField.element)

            setupUserState(address: .shabolovka)
            editMapPage.search.addressField.textField.ybm_clearText()
            editMapPage.search.addressField.textField.typeText("Москва, улица Шаболовка, д. 23к1")

            wait(forVisibilityOf: editMapPage.search.addressSuggestCell(at: 0))
            editMapPage.search.addressSuggestCell(at: 0).tap()
            wait(forVisibilityOf: editMapPage.summary.addressTextField)
            ybm_wait(forFulfillmentOf: { editMapPage.summary.doneButton.isEnabled })
            XCTAssertEqual(editMapPage.summary.addressTextField.text, "Москва, улица Шаболовка, д. 23к1")
        }

        "Нажимаем на кнопку `Продолжить` внизу попапа".ybm_run { _ in
            wait(forVisibilityOf: editMapPage.summary.doneButton)
            editMapPage.summary.doneButton.tap()
            editPage = OrderEditAddressPage.current
        }

        "Заполняем детали адреса".ybm_run { _ in
            editPage.apartmentInput.textField.tap()
            editPage.apartmentInput.textField.typeText("42")

            editPage.floorInput.textField.tap()
            editPage.floorInput.textField.typeText("6")

            editPage.entranceInput.textField.tap()
            editPage.entranceInput.textField.typeText("2")

            editPage.intercomInput.textField.tap()
            editPage.intercomInput.textField.typeText("К4211")

            editPage.commentInput.element.tap()
            editPage.commentInput.typeText("Оставить у консьержа")
            KeyboardAccessoryPage.current.doneButton.tap()
        }

        "Выбираем новую дату и интервал доставки".ybm_run { _ in
            XCTAssertEqual(editPage.dateSelector.value.label, "Выбрать")
            XCTAssertEqual(editPage.timeSelector.value.label, "Выбрать")

            editPage.dateSelector.element.tap()
            KeyboardPage.current.tapDone()
            wait(forVisibilityOf: editPage.dateSelector.element)

            XCTAssertEqual(editPage.dateSelector.value.label, "Вторник, 10 декабря")
            XCTAssertEqual(editPage.timeSelector.value.label, "с 10:00 до 18:00")
        }

        "Нажимаем на кнопку `Продолжить` внизу экрана и проверяем попап".ybm_run { _ in
            editFinishedPage = editPage.saveButton.tap()
            wait(forVisibilityOf: editFinishedPage.image)
            XCTAssertEqual(editFinishedPage.titleCell.label, "Уже связываемся со службой доставки")
            XCTAssertEqual(editFinishedPage.subtitleCell.label, "Напишем вам, когда адрес доставки изменится")
            XCTAssertEqual(editFinishedPage.nextButton.label, "Понятно")
        }

        "Нажимаем на кнопку `Понятно` в попапе и возвращаемся в детали заказа".ybm_run { _ in
            editFinishedPage.nextButton.tap()
            let orderDetailsPage = OrderDetailsPage.current
            wait(forVisibilityOf: orderDetailsPage.element)
        }
    }

    func testEditDeliveryAddressInUnavailableArea() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5767")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение адреса доставки")
        Allure.addTitle("Выбор точки вне зоны покрытия")

        var editMapPage: OrderEditAddressMapPage!

        openMap(address: .rublevskoye)

        "Проверяем, текст о недоступности адреса".ybm_run { _ in
            editMapPage = OrderEditAddressMapPage.current
            wait(forVisibilityOf: editMapPage.summary.messageText)
            XCTAssertEqual(editMapPage.summary.addressTextField.text, "Москва, Рублёвское шоссе, д. 18к1")
            XCTAssertEqual(
                editMapPage.summary.messageText.label,
                "Курьер не сможет привезти заказ сюда. Пожалуйста, выберите удобный адрес в зоне доставки"
            )
            XCTAssertFalse(editMapPage.summary.doneButton.isEnabled)
        }
    }

    func testEditDeliveryAddressSuccessNotice() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5768")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение адреса доставки")
        Allure.addTitle("Плашка в моих заказах")

        var orderDetailsPage: OrderDetailsPage!

        "Настраиваем стейт".ybm_run { _ in
            setupInitialState(address: .rublevskoye, withEditRequest: true)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: Constants.orderId)
        }

        "Проверяем плашку изменения даты доставки".ybm_run { _ in
            guard let editInfo = orderDetailsPage.editRequests.first else {
                XCTFail("couldn't be nil")
                return
            }
            XCTAssertTrue(editInfo.image.exists)
            XCTAssertEqual(editInfo.image.label, "EditRequestTimer")
            XCTAssertTrue(editInfo.title.isVisible)
            XCTAssertEqual(editInfo.title.label, "Согласовываем новый адрес и дату доставки")
        }
    }
}
