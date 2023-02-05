import MarketUITestMocks
import XCTest

final class SKUCardModelGiftsTest: LocalMockTestCase {

    func testOpenGiftSKU() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2862")
        Allure.addEpic("КМ")
        Allure.addFeature("Акционные товары")
        Allure.addTitle("Акционный блок")

        var sku: SKUPage!
        var newSku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_Gifts_SKU")
        }

        "Открываем КМ".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Мокаем состояние для перехода к КТ подарка".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_Gifts_GiftSKU")
        }

        "Переходим на КМ товара из подборки".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.giftView.item.element)
            newSku = sku.giftView.item.tap()
            wait(forVisibilityOf: newSku.element)
        }

        "Проверяем КМ подарка".ybm_run { _ in
            let title =
                "Jundo средство для мытья посуды и детских принадлежностей с гиалуроновой кислотой Juicy Lemon, 0.8 л"
            XCTAssertEqual(sku.title.label, title)
        }
    }

    func testSwitchSKU() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2823")
        Allure.addEpic("КМ")
        Allure.addFeature("Акционные товары")
        Allure.addTitle("Переключение свойств 2 рода")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_Gifts_SKU")
        }

        "Открываем КМ".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение ДО с подарком".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.giftView.element)
            let addToCartButton = sku.addToCartButton.element
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: addToCartButton)
            XCTAssertTrue(sku.price.price.isVisible)
            XCTAssertTrue(addToCartButton.isVisible)
            XCTAssertTrue(sku.giftView.item.element.isVisible)
        }

        "Мокаем состояние для альтернативного свойства товара".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_Gifts_SKUOption")
        }

        "Выбираем второй вариант цвета".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: sku.filter.element)
            let secondColorCell = sku.filter.collectionView.cellElement(at: .init(row: 1, section: 0))
            secondColorCell.tap()
            wait(forVisibilityOf: sku.element)
            sku.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: sku.title)
            ybm_wait {
                sku.title
                    .label == "PROSEPT Концентрированный кондиционер для белья с ароматом альпийской свежести, 2 л"
            }
        }

        "Проверяем отсутствие блока комплекта".ybm_run { _ in
            sku.collectionView.swipeUp()
            XCTAssertFalse(sku.giftView.element.exists)
        }
    }

    // MARK: - Constants

    private let mainSkuId = "100210864680"

    func testSkuPageWithGiftPromo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4029")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2863")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2866")
        Allure.addEpic("КМ")
        Allure.addFeature("Подарки")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_Gifts_SKU")
        }

        "Открываем КМ".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение ДО с подарком".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.giftView.element)
            let addToCartButton = sku.addToCartButton.element
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: addToCartButton)
            XCTAssertTrue(sku.price.price.isVisible)
            XCTAssertTrue(addToCartButton.isVisible)
            XCTAssertTrue(sku.giftView.item.element.isVisible)
        }

        "Открываем условия акции".ybm_run { _ in
            sku.giftView.infoButton.tap()
            let webViewHeader = app.navigationBars["Яндекс.Маркет - покупки с быстрой доставкой"]
            wait(forVisibilityOf: webViewHeader)
        }

        "Закрываем условия акции".ybm_run { _ in
            WebViewPage.current.navigationBar.backButton.tap()
            wait(forVisibilityOf: sku.element)
        }

        "Открываем все товары по акции".ybm_run { _ in
            sku.giftView.allProductButton.tap()
            let webViewHeader = app.navigationBars["Подарок за покупку"]
            wait(forVisibilityOf: webViewHeader)
        }

        "Закрываем все товары по акции".ybm_run { _ in
            WebViewPage.current.navigationBar.backButton.tap()
            wait(forVisibilityOf: sku.element)
        }

        "Настраиваем стейт для добавления в корзину товара с ДО".ybm_run { _ in
            stateManager?.setState(
                newState: CartState.makeStateWithItemsInCart([
                    ("100260690098", "VfMspw4m36qIAQsF8rIrJg"),
                    ("100577253283", "edrHcIOIkOj3y76eCV8Wfw")
                ])
            )
        }

        "Нажимаем на кнопку \"Добавить в корзину\"".ybm_run { _ in
            sku.addToCartButton.element.tap()
        }

        "Проверяем состояние кнопки \"Добавить в корзину\"".ybm_run { _ in
            let addToCartButton = sku.addToCartButton
            ybm_wait { addToCartButton.element.label == "1 товар в корзине" }
            XCTAssertTrue(addToCartButton.plusButton.isVisible)
        }

        "Проверяем badge у таба корзины".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина2" })
        }
    }
}
