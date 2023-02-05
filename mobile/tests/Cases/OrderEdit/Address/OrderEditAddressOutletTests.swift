import MarketUITestMocks
import XCTest

final class OrderEditAddressOutletTests: OrderEditAddressTestCase {

    func testEditDeliveryAddressServiceToOutlet() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5766")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение адреса доставки-2")
        Allure.addTitle("Смена курьера на самовывоз")

        var editPage: OrderEditPage!
        var editMapPage: OrderEditAddressMapPage!
        var outletMapInfoPage: OutletMapInfoPage!
        var editFinishedPage: OrderEditFinishedPage!

        enable(toggles: FeatureNames.editAddressV2)
        openMap(address: .novinskiy)
        checkSegmentedButtons()

        "Проверяем адрес".ybm_run { _ in
            editMapPage = OrderEditAddressMapPage.current
            ybm_wait(forFulfillmentOf: { editMapPage.summary.doneButton.isEnabled })
            XCTAssertEqual(editMapPage.summary.addressTextField.text, "Москва, Новинский бульвар, д. 8")
        }

        "Нажимаем на кнопку `Самовывоз` в верхней части карты".ybm_run { _ in
            editMapPage.segmentedButtons.outletButton.tap()
        }

        "Нажимаем на пункт самовывоза и проверяем информацию".ybm_run { _ in
            editPage = OrderEditPage.current
            wait(forVisibilityOf: editPage.element)
            editPage.element.ybm_tapCenter()
        }

        "Нажимаем на кнопку `Продолжить` в шторке и проверяем попап".ybm_run { _ in
            outletMapInfoPage = OutletMapInfoPage.current
            wait(forVisibilityOf: outletMapInfoPage.element)
            wait(forVisibilityOf: outletMapInfoPage.storagePeriod)

            XCTAssertEqual(outletMapInfoPage.headerTitle.label, "Доставка 10 декабря к 18:00")
            XCTAssertEqual(outletMapInfoPage.name.label, "Постамат Яндекс.Маркет")
            XCTAssertEqual(outletMapInfoPage.address.label, "Луговая улица, д.3")
            XCTAssertEqual(outletMapInfoPage.storagePeriod.label, "Срок хранения 10 дней")
            XCTAssertEqual(outletMapInfoPage.scheduleDay.label, "Ежедневно")
            XCTAssertEqual(outletMapInfoPage.scheduleTime.label, "10:00 - 22:00")
        }

        "Нажимаем на кнопку `Понятно` для сохранения изменений и проверяем попап".ybm_run { _ in
            outletMapInfoPage.continueButton.tap()
            editFinishedPage = OrderEditFinishedPage.current
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

    func testEditDeliveryAddressOutletUnavailable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6155")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение адреса доставки-2")
        Allure.addTitle("Cамовывоз недоступен")

        var editMapPage: OrderEditAddressMapPage!

        enable(toggles: FeatureNames.editAddressV2)
        openMap(
            address: .novinskiy,
            redeliveryInfo: (polygon: [.defaultArea], outlets: [])
        )

        checkHint(title: "Доступна только курьерская доставка — выберите удобный адрес")
        checkSegmentedButtons()

        "Проверяем адрес".ybm_run { _ in
            editMapPage = OrderEditAddressMapPage.current
            ybm_wait(forFulfillmentOf: { editMapPage.summary.doneButton.isEnabled })
            XCTAssertEqual(editMapPage.summary.addressTextField.text, "Москва, Новинский бульвар, д. 8")
        }

        "Проверяем доступность кнопок `Самовывоз` и `Курьер`".ybm_run { _ in
            XCTAssertTrue(editMapPage.segmentedButtons.serviceButton.isSelected)
            XCTAssertFalse(editMapPage.segmentedButtons.outletButton.isEnabled)
        }
    }

    func testEditDeliveryAddressServiceUnavailable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6156")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение адреса доставки-2")
        Allure.addTitle("Курьер недоступен")

        enable(toggles: FeatureNames.editAddressV2)
        openMap(
            address: .novinskiy,
            redeliveryInfo: (polygon: [], outlets: [.defaultOutlet])
        )

        checkHint(title: "Доступна только доставка в пункты выдачи")
        checkSegmentedButtons()

        "Проверяем доступность кнопок `Самовывоз` и `Курьер`".ybm_run { _ in
            let editMapPage = OrderEditAddressMapPage.current
            XCTAssertTrue(editMapPage.segmentedButtons.outletButton.isSelected)
            XCTAssertFalse(editMapPage.segmentedButtons.serviceButton.isEnabled)
        }
    }

    // MARK: - Private

    private func checkSegmentedButtons() {
        "Проверяем кнопки `Самовывоз` и `Курьер`".ybm_run { _ in
            let editMapPage = OrderEditAddressMapPage.current
            wait(forVisibilityOf: editMapPage.segmentedButtons.outletButton)
            XCTAssertEqual(editMapPage.segmentedButtons.count, 2)
            XCTAssertEqual(editMapPage.segmentedButtons.outletButton.label, "Самовывоз")
            XCTAssertEqual(editMapPage.segmentedButtons.serviceButton.label, "Курьер")
        }
    }

    private func checkHint(title: String) {
        "Проверяем хинт для самовывоза".ybm_run { _ in
            let editMapPage = OrderEditAddressMapPage.current
            wait(forVisibilityOf: editMapPage.hintView)
            XCTAssertEqual(editMapPage.hintView.label, title)
        }
    }
}
