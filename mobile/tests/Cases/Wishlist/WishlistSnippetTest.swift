import MarketUITestMocks
import XCTest

final class WishlistSnippetTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testBasic() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-984")
        Allure.addEpic("Вишлист")
        Allure.addFeature("Базовый тест")
        Allure.addTitle("Проверяем, что на экране отображаются избранные товары")

        var profile: ProfilePage!
        var wishlist: WishlistPage!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .several(SKUFAPIResults.default, SKUFAPIResults.protein),
                    collections: .several(.default, .protein)
                )
            )
            stateManager?.setState(newState: skuState)

            var wishlistState = WishlistState()
            wishlistState.setWishlistItems(items: [.default, .protein])
            stateManager?.setState(newState: wishlistState)
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            profile = goToProfile()
        }

        "Переходим в вишлист".ybm_run { _ in
            wait(forExistanceOf: profile.wishlist.element)

            wishlist = profile.wishlist.tap()
            wait(forExistanceOf: wishlist.collectionView)
        }

        "Проверяем два сниппета товаров".ybm_run { _ in
            for (i, data) in generateSnippetsInfo().enumerated() {
                let cell = wishlist.wishlistItem(at: i)
                wait(forExistanceOf: cell.element)

                // опциональная скидка и бэйдж скидки
                if let oldPrice = data.snippet.oldPrice,
                   let badge = data.snippet.priceBadge {
                    XCTAssertEqual(cell.oldPriceLabel.label, oldPrice)
                    XCTAssertEqual(cell.discountLabel.label, badge)
                }

                XCTAssertEqual(cell.addToCartButton.element.label, data.snippet.addToCart)
                XCTAssertEqual(cell.titleLabel.label, data.snippet.title)
                XCTAssertEqual(cell.ratingLabel.label, data.snippet.raiting)
                XCTAssertEqual(cell.wishListButton.isSelected, data.snippet.inWishlist)
                XCTAssertEqual(cell.currentPrice.label, data.snippet.currPrice)
            }
        }
    }

    private func generateSnippetsInfo() -> [(indexPath: IndexPath, snippet: Snippet)] {
        [
            (
                IndexPath(item: 0, section: 0),
                Snippet(
                    oldPrice: "94\u{202f}990₽",
                    addToCart: "В корзину",
                    title: "Смартфон Apple iPhone 12 256GB, синий",
                    raiting: "4.7\u{202f}/\u{202f}89",
                    priceBadge: "–14\u{202f}%",
                    inWishlist: true,
                    currPrice: "81\u{202f}990\u{202f}₽",
                    reviews: "89 отзывов"
                )
            ),
            (
                IndexPath(item: 2, section: 0),
                Snippet(
                    oldPrice: nil,
                    addToCart: "В корзину",
                    title: "Протеин CMTech Whey Protein Клубничный крем, 30 порций",
                    raiting: "5.0\u{202f}/\u{202f}2",
                    priceBadge: nil,
                    inWishlist: true,
                    currPrice: "1\u{202f}413\u{202f}₽",
                    reviews: "2 отзыва"
                )
            )
        ]
    }
}

// MARK: - Nested Types

private extension WishlistSnippetTest {

    struct Snippet {
        let oldPrice: String?
        let addToCart: String
        let title: String
        let raiting: String
        let priceBadge: String?
        let inWishlist: Bool
        let currPrice: String
        let reviews: String
    }
}
