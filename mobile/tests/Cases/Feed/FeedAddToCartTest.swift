import MarketUITestMocks
import XCTest

final class FeedAddToCartTest: LocalMockTestCase {

    func testAddToCart() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3827")
        Allure.addEpic("Выдача")
        Allure.addFeature("Добавление товара")
        Allure.addTitle("Проверка каунтера на выдаче")

        let search = "Протеин"
        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!

        "Мокаем выдачу".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_Basics")
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: search)
        }

        "Скроллим к первому товару".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
        }

        "Мокаем состояние".ybm_run { _ in
            var cartState = CartState()
            cartState.setThresholdInfoState(with: .init(
                info: .no_free_delivery,
                forReason: .regionWithoutThreshold
            ))
            mockStateManager?.pushState(bundleName: "FeedSet_AddedToCart")
        }

        "Нажимаем кнопку В корзину".ybm_run { _ in
            snippetPage.addToCartButton.element.tap()
        }

        "Проверяем состояние кнопки и бейдж корзины".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { snippetPage.addToCartButton.element.label == "1" })
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })
            XCTAssertTrue(snippetPage.addToCartButton.minusButton.isVisible)
            XCTAssertTrue(snippetPage.addToCartButton.plusButton.isVisible)
        }
    }

}
