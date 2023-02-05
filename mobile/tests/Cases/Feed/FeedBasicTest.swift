import MarketUITestMocks
import XCTest

final class FeedBasicTest: LocalMockTestCase {

    /// Базовый тест на выдачу - тестируем честный поиск через `goToFeed(with: search)`
    func testBasicFunctionality() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-976")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1068")
        Allure.addEpic("Выдача")
        Allure.addFeature("Базовый функционал")
        Allure.addTitle("Проверяем поиск, блок с категориями, отображение найденных товаров. Скрол страницы")

        let search = "iphone 7"

        var feedState = FeedState()

        "Мокаем состояние".ybm_run { _ in
            feedState.setSearchOrRedirectState(mapper: .init(
                offers: FAPIOffer.all,
                intent: ResolveSearch.Intent.chocolates
            ))
            stateManager?.setState(newState: feedState)
        }

        var feedPage: FeedPage!
        var header: FeedPage.CollectionView.HeaderFeed!

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
            header = feedPage.collectionView.headerFeedFilter
        }

        "Проверяем поисковую строку: наличие кнопки `назад`, поискового запроса, кнопки очистки поискового запроса"
            .ybm_run { _ in
                wait(forVisibilityOf: NavigationBarPage.current.backButton)
                wait(forVisibilityOf: feedPage.navigationBar.searchedTextButton)
                XCTAssertEqual(search, feedPage.navigationBar.searchedTextButton.label)
                wait(forVisibilityOf: feedPage.navigationBar.clearButton)
            }

        "Отображается блок с категориями".ybm_run { _ in
            wait(forVisibilityOf: feedPage.collectionView.categoriesCollectionView.element)
        }

        "Отображается кнопка `сначала подешевле`".ybm_run { _ in
            wait(forVisibilityOf: feedPage.collectionView.headerFeedFilter.sortButton)
            XCTAssertEqual("Сначала подешевле", feedPage.collectionView.headerFeedFilter.sortButton.label)
        }

        "Отображается кнопка фильтров".ybm_run { _ in
            wait(forVisibilityOf: feedPage.collectionView.headerFeedFilter.filterButton.element)
            XCTAssertEqual("Фильтры", feedPage.collectionView.headerFeedFilter.filterButton.element.label)
        }

        "Отображаются найденные товары: проверка карточки первого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssertTrue(snippetPage.addToCartButton.element.isVisible)
            XCTAssertTrue(snippetPage.oldPriceLabel.isVisible)
            XCTAssertTrue(snippetPage.titleLabel.isVisible)
            XCTAssertTrue(snippetPage.imageView.isVisible)
            XCTAssertTrue(snippetPage.discountLabel.isVisible)
            XCTAssertTrue(snippetPage.currentPrice.isVisible)
            XCTAssertTrue(snippetPage.ratingLabel.isVisible)
            XCTAssertTrue(snippetPage.wishListButton.isVisible)
        }

        "Скролл вниз, проверка что хэдер все еще видно".ybm_run { _ in
            XCTAssertTrue(header.element.isVisible)
            feedPage.element.swipe(to: .down, times: 1, until: false)
            XCTAssertTrue(header.element.isVisible)
        }
    }

    func testAlcoholBasic() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2758")
        Allure.addEpic("Выдача")
        Allure.addFeature("Базовый функционал")
        Allure.addTitle("Алкоголь - содержания блока КМ")

        let search = "amatore bianco, 2018"
        var snippetPage: FeedSnippetPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_Wine")
        }

        var feedPage: FeedPage!

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
        }

        "Отображается вино".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
        }

        "Проверка данных на карточке первого товара".ybm_run { _ in
            XCTAssertTrue(snippetPage.addToCartButton.element.isVisible)
            XCTAssertEqual(snippetPage.addToCartButton.element.label, "В корзину")

            XCTAssertTrue(snippetPage.titleLabel.isVisible)
            XCTAssertEqual(snippetPage.titleLabel.label, "Пиво безалкогольное светлое Жигули Барное 0.45 л х 20 шт")

            XCTAssertTrue(snippetPage.ratingLabel.isVisible)
            XCTAssertEqual(snippetPage.ratingLabel.label, "4.4\u{202f}/\u{202f}12")

            XCTAssertTrue(snippetPage.wishListButton.isVisible)
            XCTAssertEqual(snippetPage.wishListButton.label, "MarketUIFavoriteUnselected")

            XCTAssertTrue(snippetPage.currentPrice.isVisible)
            XCTAssertEqual(snippetPage.currentPrice.label, "831\u{202f}₽")
        }
    }
}
