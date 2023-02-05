import FormKit
import XCTest

final class SKUCardModelServicesTest: ServicesTestCase {

    func testAddingAndRemovingService() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4745")
        Allure.addEpic("КМ")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Удаление доп.услуги")

        var sku: SKUPage!
        var addServicePopup: ServicesPopupPage!

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Добавляем товар в корзину".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)
            setupAddToCartState()
            sku.addToCartButton.element.tap()
        }

        "Проверяем отображение кнопки и бейдж в таб баре".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "1 товар в корзине" })
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })

            wait(forVisibilityOf: sku.addToCartButton.plusButton)
            wait(forVisibilityOf: sku.addToCartButton.minusButton)
        }

        "Нажимаем Добавить установку".ybm_run { _ in
            XCTAssertEqual(sku.addServiceButton.element.label, "Доступна услуга установки")
            addServicePopup = sku.addServiceButton.tap()
            wait(forVisibilityOf: addServicePopup.element)
        }

        "Выбираем первую установку".ybm_run { _ in
            addServicePopup.selectService(at: 1)

            XCTAssertTrue(addServicePopup.selectedService.element.exists, "Нет селектора услуг")

            addServicePopup.saveButton.tap()
            wait(forInvisibilityOf: addServicePopup.element)
            wait(forVisibilityOf: sku.element)

            XCTAssertEqual(sku.addServiceButton.element.label, "Установка 5 000 ₽")
        }

        "Выбираем установка не нужна".ybm_run { _ in
            addServicePopup = sku.addServiceButton.tap()
            wait(forVisibilityOf: addServicePopup.element)
            addServicePopup.selectService(at: 0)

            XCTAssertTrue(addServicePopup.selectedService.element.exists, "Нет селектора услуг")
            XCTAssertEqual(addServicePopup.selectedService.title.label, "Не нужна")

            addServicePopup.saveButton.tap()
            wait(forInvisibilityOf: addServicePopup.element)
            wait(forVisibilityOf: sku.element)

            XCTAssertEqual(sku.addServiceButton.element.label, "Доступна услуга установки")
        }
    }
}
