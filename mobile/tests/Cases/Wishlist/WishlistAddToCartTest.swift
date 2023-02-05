import MarketUITestMocks
import XCTest

final class WishlistAddToCartTest: LocalMockTestCase {

    func testAddToCartUnauthorized() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3868")
        Allure.addEpic("Вишлист")
        Allure.addFeature("Добавление в корзину незалогином")
        Allure.addTitle("Проверяем, что кнопка добавления отработала нормально")

        var profile: ProfilePage!
        var wishlist: WishlistPage!
        var cell: FeedSnippetPage!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: .default
                )
            )
            stateManager?.setState(newState: skuState)

            var wishlistState = WishlistState()
            wishlistState.setWishlistItems(items: [.default])
            stateManager?.setState(newState: wishlistState)
        }

        "Запускаем приложение и переходим в профиль".ybm_run { _ in
            profile = goToProfile()
        }

        "Переходим в вишлист".ybm_run { _ in
            wait(forExistanceOf: profile.wishlist.element)

            wishlist = profile.wishlist.tap()
            wait(forExistanceOf: wishlist.collectionView)
        }

        "Мокаем состояние добавления товара в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "WithlistSet_AddedToCart")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            cell = wishlist.wishlistItem(at: 0)
            wait(forExistanceOf: cell.element)

            cell.addToCartButton.element.tap()
        }

        "Проверяем отображение кнопки и бейдж в таб баре".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { cell.addToCartButton.element.label == "1" })
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })
            XCTAssertTrue(cell.addToCartButton.plusButton.isVisible)
            XCTAssertTrue(cell.addToCartButton.minusButton.isVisible)
        }
    }
}
