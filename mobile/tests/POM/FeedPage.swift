import MarketUI
import UIUtils
import XCTest

class FeedPage: PageObject {
    /// Элемент в иерархии
    static var current: FeedPage {
        let el = XCUIApplication().otherElements[FeedAccessibility.root]
        return FeedPage(element: el)
    }

    /// Класс для navigationBar на странице с уже найденным товаром через поиск
    class FeedNavigationBarPage: NavigationBarPage {
        /// Кнопка, содержащая поискововой запрос, отображенный на экране
        ///
        /// Текст из поискового запроса достается по `.label`
        var searchedTextButton: XCUIElement {
            element.buttons.matching(identifier: NavigationBarAccessibility.searchViewSearchButton).firstMatch
        }

        /// Clear button для очистки поискового запроса и перехода в режим поиска
        var clearButton: XCUIElement {
            element.buttons.matching(identifier: NavigationBarAccessibility.clearButton).firstMatch
        }
    }

    class CollectionView: PageObject, UniformCollectionViewPage {
        typealias AccessibilityIdentifierProvider = ResultCollectionViewCellsAccessibility
        typealias CellPage = FeedSnippetPage

        var collectionView: XCUIElement {
            element
        }

        func snippetFirstMatchingCell() -> FeedSnippetPage {
            let element = cellUniqueElement(withIdentifier: FeedSnippetAccessibility.cell)
            return FeedSnippetPage(element: element)
        }

        typealias CarouselCell = LegacyScrollBoxWidgetPage<SnippetPage>

        /// Класс заголовка таблицы с товарами, найденными по запросу, для фильтрации
        class HeaderFeed: PageObject {

            /// Класс для кнопки фильтров
            class FiltersButton: PageObject, FiltersEntryPoint {}

            class QuickFilterButton: PageObject, FilterEntryPoint {
                var resetButton: XCUIElement {
                    element.buttons.firstMatch
                }
            }

            /// Класс для скроллбокса с быстрыми фильтрами
            class QuickFiltersCollectionView: PageObject, CollectionViewPage {
                typealias AccessibilityIdentifierProvider = ScrollBoxCellsAccessibility

                var collectionView: XCUIElement {
                    element
                }

                func filter(at index: Int) -> QuickFilterButton {
                    let element = cellElement(at: IndexPath(row: index, section: 0))
                    return QuickFilterButton(element: element)
                }
            }

            /// Кнопка СЛЕВА "Сначала популярные" с возможностью выбора порядка
            var sortButton: XCUIElement {
                element.buttons.matching(identifier: FeedAccessibility.sortButton).firstMatch
            }

            /// Кнопка СПРАВА "Фильтры"
            var filterButton: FiltersButton {
                let elem = element.buttons.matching(identifier: FeedAccessibility.filterButton).firstMatch
                return FiltersButton(element: elem)
            }

            /// Кружок с цифрой - количество фильтров
            var filterBadge: XCUIElement {
                element.staticTexts.matching(identifier: FeedAccessibility.filterBadge).firstMatch
            }

            /// Быстрые фильтры
            var quickFilterView: QuickFiltersCollectionView {
                QuickFiltersCollectionView(
                    element: element.collectionViews
                        .matching(identifier: FeedAccessibility.quickFiltersCollectionView).firstMatch
                )
            }
        }

        class CategoriesCollectionView: PageObject, UniformCollectionViewPage {
            typealias AccessibilityIdentifierProvider = ScrollBoxCellsAccessibility
            typealias CellPage = CategoryCarouselPage

            var collectionView: XCUIElement { element }

            func snippetFirstMatchingCell() -> CategoryCarouselPage {
                let element = cellUniqueElement(withIdentifier: ScrollBoxCellsAccessibility.baseIdentifier)
                return CategoryCarouselPage(element: element)
            }

            func category(atIndex index: Int) -> XCUIElement {
                cellElement(at: IndexPath(item: index, section: 0))
            }
        }

        /// Класс информации о категории
        class CategoryInfo: PageObject {

            /// Класс для кнопки изменения категории
            class ChangeButton: PageObject, FeedEntryPoint {}

            /// Кнопка СЛЕВА "Сначала популярные" с возможностью выбора порядка
            var name: XCUIElement {
                element.staticTexts.matching(identifier: FeedAccessibility.categoryName).firstMatch
            }
        }

        class NoResultsView: PageObject {}

        /// Карусель с похожими категориями для уточнения запроса
        var categoriesCollectionView: CategoriesCollectionView {
            let elem = element.collectionViews.matching(identifier: FeedAccessibility.categoriesCollection).firstMatch
            return CategoriesCollectionView(element: elem)
        }

        /// Информация о категории
        var categoryInfo: CategoryInfo {
            let elem = element
                .cells.matching(identifier: FeedAccessibility.categoryInfo)
                .firstMatch
            return CategoryInfo(element: elem)
        }

        var categorySeparator: XCUIElement {
            element.cells.matching(identifier: FeedAccessibility.categorySeparator)
                .firstMatch
        }

        var recommendedByHistoryCell: CarouselCell {
            let elem = element
                .cells.matching(identifier: FeedAccessibility.recommendedByHistoryCell)
                .firstMatch
            return CarouselCell(element: elem)
        }

        var personalDealsCell: CarouselCell {
            let elem = element
                .cells.matching(identifier: FeedAccessibility.personalDealsCell)
                .firstMatch
            return CarouselCell(element: elem)
        }

        /// "Каталог товаров"
        var catalogCell: CarouselCell {
            let elem = element
                .cells.matching(identifier: FeedAccessibility.catalogCell)
                .firstMatch
            return CarouselCell(element: elem)
        }

        /// Заголовок таблицы товаров с фильтрами
        var headerFeedFilter: HeaderFeed {
            let elem = element.cells.matching(identifier: FeedAccessibility.headerView).firstMatch
            return HeaderFeed(element: elem)
        }

        /// Вьюшка при пустой выдаче - грустная звездочка
        var noResultsView: NoResultsView {
            let elem = cellUniqueElement(withIdentifier: FeedAccessibility.noResultsView)
            return NoResultsView(element: elem)
        }

        /// Метод для взятия хедера из иерархии по индексу
        ///
        /// в основной выдаче индекс всегда 1, в других местах может быть 0
        func getFeedFilter(at index: Int) -> HeaderFeed {
            let el = XCUIApplication().collectionViews.cells.element(boundBy: index)
            return HeaderFeed(element: el)
        }
    }

    /// NavigationBar на экране найденных товаров
    var navigationBar: FeedNavigationBarPage {
        FeedNavigationBarPage(element: NavigationBarPage.current.element)
    }

    /// Вся страничка FeedViewController
    var collectionView: CollectionView {
        let elem = element
            .collectionViews.matching(identifier: FeedAccessibility.collectionView)
            .firstMatch
        return CollectionView(element: elem)
    }

    var mediaSetSnippetCell: MediaSetSnippetCell {
        let elem = element
            .cells
            .matching(identifier: FeedAccessibility.mediaSetSnippetCell)
            .firstMatch
        return MediaSetSnippetCell(element: elem)
    }

    class MediaSetSnippetCell: PageObject {
        func goToFeed() -> FeedPage {
            element.tap()
            let subElem = XCUIApplication().otherElements[FeedAccessibility.root]
            XCTAssertTrue(subElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
            return FeedPage(element: subElem)
        }

        func goToWebView() -> WebViewPage {
            element.tap()
            let subElem = XCUIApplication().otherElements[WebviewAccessibility.webview]
            XCTAssertTrue(subElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
            return WebViewPage(element: subElem)
        }
    }

    /// Дисклеймер опечатки
    var typoDisclaimer: XCUIElement {
        element.textViews.matching(identifier: FeedAccessibility.typoDisclaimer).firstMatch
    }

    /// Заголовок с названием коллекции
    var categoryTitle: XCUIElement {
        element.staticTexts.matching(identifier: FeedAccessibility.categoryTitle).firstMatch
    }

    /// Количество найденных результатов
    var resultsCountLabel: XCUIElement {
        element.staticTexts.matching(identifier: FeedAccessibility.resultsCount).firstMatch
    }

    /// Кнопка отмены редиректа в категорию «Искать везде»
    var redirectButton: XCUIElement {
        element.buttons.matching(identifier: FeedAccessibility.redirectButton).firstMatch
    }
}
