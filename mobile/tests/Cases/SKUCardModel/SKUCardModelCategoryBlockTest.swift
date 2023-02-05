import MarketUITestMocks
import XCTest

final class SKUCardModelCategoryBlockTest: LocalMockTestCase {

    func testCategoryBlock() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-955")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-925")
        Allure.addEpic("КМ")
        Allure.addFeature("Блок категорий")
        Allure.addTitle("Проверяем базовое поведение блока категорий")

        var sku: SKUPage!

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение бейджа, показывающего, что пользователи рекомендуют товар".ybm_run { _ in
            XCTAssertTrue(sku.compactReason.isVisible, "compactReason is visible")
            XCTAssertTrue(sku.compactReason.text.contains("87% рекомендуют"))
        }

        "Долистываем до блока категорий, проверяем его содержание".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                sku.didFinishLoadingInfo
            })

            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.categoryLink.element, withVelocity: .slow)
            XCTAssertTrue(sku.vendorLink.element.isVisible, "vendorLink is visible")
            XCTAssertEqual(sku.vendorLink.element.text, "Все товары Apple")
            XCTAssertEqual(sku.categoryLink.element.text, "Все товары категории «Мобильные телефоны»")
        }
    }

}
