import MarketUITestMocks
import XCTest

class FavoritesTransitionToComparisonListTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testTransitionToComparisonList() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3543")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Избранное")
        Allure.addTitle("Добавление \"Разобрали\" из избранного")

        var profile: ProfilePage!
        var wishlist: WishlistPage!
        var cell: FeedSnippetPage!
        var comparisonPage: ComparisonPage!
        let popup = AddToComparsionToastPopupPage.currentPopup

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_FavoritesTransitionToList")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: modify(SKUInfoCollections.default) { $0.offer = [] }
                )
            )
            stateManager?.setState(newState: skuState)

            var wishlistState = WishlistState()
            wishlistState.setWishlistItems(items: [.default])
            stateManager?.setState(newState: wishlistState)
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            profile = goToProfile()
        }

        "Переходим в вишлист".ybm_run { _ in
            wait(forExistanceOf: profile.wishlist.element)
            wishlist = profile.wishlist.tap()
            wait(forVisibilityOf: wishlist.collectionView)
        }

        "Проверяем сниппет товара".ybm_run { _ in
            cell = wishlist.wishlistItem(at: 0)

            XCTAssertTrue(cell.soldOutView.isVisible)
            XCTAssertTrue(cell.comparsionButton.isVisible)
            XCTAssertFalse(cell.comparsionButton.isSelected)
        }

        "Добавляем товар в список сравнения".ybm_run { _ in
            cell.comparsionButton.tap()
            wait(forExistanceOf: popup.element)
            wait(forExistanceOf: popup.titleLabel)
            XCTAssertEqual(popup.titleLabel.label, "Товар теперь в списке сравнения")
            XCTAssertTrue(cell.comparsionButton.isSelected)
        }

        "Переходим в список сравнения".ybm_run { _ in
            popup.actionButton.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: ComparisonAccessibility.root)
                .firstMatch
            wait(forExistanceOf: elem)
            comparisonPage = ComparisonPage(element: elem)
        }

        "Проверяем данные в КМ".ybm_run { _ in
            wait(forExistanceOf: comparisonPage.collectionView.element)
            let cellPage = comparisonPage.collectionView.modelCell(with: 0)
            wait(forExistanceOf: cellPage.element)
            wait(forExistanceOf: cellPage.title.element)
            XCTAssertEqual(cellPage.title.element.label, "Смартфон Apple iPhone 11 64GB желтый (MHDE3RU/A) Slimbox")
        }
    }
}
