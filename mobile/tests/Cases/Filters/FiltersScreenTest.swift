import MarketUITestMocks
import XCTest

final class FiltersScreenTest: LocalMockTestCase {

    // MARK: - Constants

    let filterOptionLabels = ["1-2 часа", "Сегодня", "Сегодня или завтра", "До 5 дней", "Любой"]

    func testDeliveryOptionIntervalsInCommonContext() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6359")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6369")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6370")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6371")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6372")
        Allure.addEpic("Фильтр Срок доставки")
        Allure.addFeature("Смешанная выдача. Опция 1-2ч")
        Allure.addTitle(
            """
            На смешанной выдаче после поиска есть фильтр "Срок доставки" в быстрых фильтрах
            - 1-2 часа
            - сегодня
            - сегодня или завтра
            - до 5 дней
            - любой
            """
        )

        var feedPage: FeedPage!
        var filtersPage: FiltersPage!
        var filterPage: FilterPage!

        let search = "смартфон"

        "Мокаем поиск".ybm_run { _ in
            setupFeedState()
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: search)
        }

        "Переходим в фильтры".ybm_run { _ in
            filtersPage = feedPage.collectionView.headerFeedFilter.filterButton.tap()
            wait(forVisibilityOf: filtersPage.element)
        }

        filterOptionLabels.enumerated().forEach { index, _ in

            "Жмем фильтр \"Срок доставки\"".ybm_run { _ in
                filterPage = filtersPage.snippet(named: "Срок доставки")?.tap()
                wait(forVisibilityOf: filterPage.element)
            }

            "Тапаем по первой опции".ybm_run { _ in
                XCTAssertEqual(
                    filterPage.snippets.dropFirst(index).first?.name.label
                        .trimmingCharacters(in: .whitespacesAndNewlines),
                    filterOptionLabels[index]
                )
                filterPage = filterPage.snippets.dropFirst(index).first?.tap()
            }

            checkFilterOptions(page: filterPage)

            "Проверяем активацию опции 1-2 часа".ybm_run { _ in
                XCTAssertEqual(
                    filterPage.snippets.dropFirst(index).first?.checkBox.identifier,
                    FiltersSnippetAccessibility.checkBoxSelected
                )
            }

            "Проверяем кнопку".ybm_run { _ in
                XCTAssertEqual(filterPage.doneButton.label, "Готово")
            }

            "Жмем Готово, возвращаемся к фильтрам".ybm_run { _ in
                filterPage.doneButton.tap()
                wait(forVisibilityOf: filtersPage.element)
            }

            "Проверяем кнопку".ybm_run { _ in
                XCTAssertEqual(filtersPage.doneButton.element.label, "Показать 2 товара")
            }
        }
    }

    func testDeliveryOptionIntervalsInExpressContext() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6360")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6373")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6374")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6375")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6376")
        Allure.addEpic("Фильтр Срок доставки")
        Allure.addFeature("Express выдача. Опция 1-2ч")
        Allure.addTitle(
            """
            На смешанной выдаче после поиска есть фильтр "Срок доставки" в быстрых фильтрах
            - 1-2 часа
            - сегодня
            - сегодня или завтра
            - до 5 дней
            - любой
            """
        )

        var feedPage: FeedPage!
        var filtersPage: FiltersPage!
        var filterPage: FilterPage!

        let search = "смартфон"

        "Настраиваем стейт".ybm_run { _ in
            setupExpressState()
        }

        feedPage = openExpressFeed(search: search)

        "Переходим в фильтры".ybm_run { _ in
            filtersPage = feedPage.collectionView.headerFeedFilter.filterButton.tap()
            wait(forVisibilityOf: filtersPage.element)
        }

        filterOptionLabels.enumerated().forEach { index, _ in

            "Жмем фильтр \"Срок доставки\"".ybm_run { _ in
                filterPage = filtersPage.snippet(named: "Срок доставки")?.tap()
                wait(forVisibilityOf: filterPage.element)
            }

            "Тапаем по первой опции".ybm_run { _ in
                XCTAssertEqual(
                    filterPage.snippets.dropFirst(index).first?.name.label
                        .trimmingCharacters(in: .whitespacesAndNewlines),
                    filterOptionLabels[index]
                )
                filterPage = filterPage.snippets.dropFirst(index).first?.tap()
            }

            checkFilterOptions(page: filterPage)

            "Проверяем активацию опции 1-2 часа".ybm_run { _ in
                XCTAssertEqual(
                    filterPage.snippets.dropFirst(index).first?.checkBox.identifier,
                    FiltersSnippetAccessibility.checkBoxSelected
                )
            }

            "Проверяем кнопку".ybm_run { _ in
                XCTAssertEqual(filterPage.doneButton.label, "Готово")
            }

            "Жмем Готово, возвращаемся к фильтрам".ybm_run { _ in
                filterPage.doneButton.tap()
                wait(forVisibilityOf: filtersPage.element)
            }

            "Проверяем кнопку".ybm_run { _ in
                XCTAssertEqual(filtersPage.doneButton.element.label, "Показать 2 товара")
            }
        }
    }

    func testResetDeliveryOptionIntervalInExpressContext() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6377")
        Allure.addEpic("Фильтр Срок доставки")
        Allure.addFeature("Сброс значения при повторном или дргуом запросе")
        Allure.addTitle(
            """
            На смешанной выдаче после поиска есть фильтр "Срок доставки" в быстрых фильтрах
            - 1-2 часа
            - сегодня
            - сегодня или завтра
            - до 5 дней
            - любой
            """
        )

        var feedPage: FeedPage!
        var filtersPage: FiltersPage!
        var filterPage: FilterPage!

        let search = "смартфон"

        "Настраиваем стейт".ybm_run { _ in
            setupExpressState()
        }

        feedPage = openExpressFeed(search: search)

        "Переходим в фильтры".ybm_run { _ in
            filtersPage = feedPage.collectionView.headerFeedFilter.filterButton.tap()
            wait(forVisibilityOf: filtersPage.element)
        }

        "Жмем фильтр \"Срок доставки\"".ybm_run { _ in
            filterPage = filtersPage.snippet(named: "Срок доставки")?.tap()
            wait(forVisibilityOf: filterPage.element)
        }

        "Тапаем по первой опции".ybm_run { _ in
            filterPage = filterPage.snippets.first?.tap()
            XCTAssertEqual(
                filterPage.snippets.first?.name.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "1-2 часа"
            )
            XCTAssertEqual(
                filterPage.snippets.first?.checkBox.identifier,
                FiltersSnippetAccessibility.checkBoxSelected
            )
        }

        "Проверяем кнопку".ybm_run { _ in
            XCTAssertEqual(filterPage.doneButton.label, "Готово")
        }

        "Жмем Готово, возвращаемся к фильтрам".ybm_run { _ in
            filterPage.doneButton.tap()
            wait(forVisibilityOf: filtersPage.element)
        }

        "Проверяем кнопку".ybm_run { _ in
            XCTAssertEqual(filtersPage.doneButton.element.label, "Показать 2 товара")
        }

        "Жмем кнопку, возвращаемся на выдачу".ybm_run { _ in
            feedPage = filtersPage.doneButton.tap()
            wait(forVisibilityOf: feedPage.collectionView.element)
        }

        "Жмем на поисковую кнопку, вызываем новый запрос".ybm_run { _ in
            feedPage.navigationBar.searchedTextButton.tap()
            let searchPage = SearchPage.current
            wait(forVisibilityOf: searchPage.navigationBar.searchTextField)

            searchPage.navigationBar.searchTextField.tap()
            searchPage.navigationBar.searchTextField.ybm_clearAndEnterText(search + "\n")

            let feedElement = XCUIApplication().otherElements[FeedAccessibility.root]
            feedPage = FeedPage(element: feedElement)
            ybm_wait(forVisibilityOf: [feedElement])
        }

        "Переходим в фильтры".ybm_run { _ in
            filtersPage = feedPage.collectionView.headerFeedFilter.filterButton.tap()
            wait(forVisibilityOf: filtersPage.element)
        }

        "Жмем фильтр \"Срок доставки\"".ybm_run { _ in
            filterPage = filtersPage.snippet(named: "Срок доставки")?.tap()
            wait(forVisibilityOf: filterPage.element)
        }

        "Проверяем список опций и что все выключены".ybm_run { _ in
            filterPage.snippets.enumerated().forEach { index, filterSnippet in
                XCTAssertEqual(
                    filterSnippet.name.label.trimmingCharacters(in: .whitespacesAndNewlines),
                    filterOptionLabels[index]
                )
                XCTAssertEqual(
                    filterSnippet.checkBox.identifier,
                    FiltersSnippetAccessibility.checkBox
                )
            }
        }
    }

    func testResetFilterValuesOnResetButtonTappedInCommonContext() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6378")
        Allure.addEpic("Фильтр Срок доставки")
        Allure.addFeature("Сброс значения фильтра по кнопке на странице всех фильтров")
        Allure.addTitle(
            """
            Проверить что кнопка сброса фильтров реально их сбрасывает
            """
        )

        var feedPage: FeedPage!
        var filtersPage: FiltersPage!
        var filterPage: FilterPage!

        let search = "смартфон"

        "Мокаем поиск".ybm_run { _ in
            setupFeedState()
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: search)
        }

        "Переходим в фильтры".ybm_run { _ in
            filtersPage = feedPage.collectionView.headerFeedFilter.filterButton.tap()
            wait(forVisibilityOf: filtersPage.element)
        }

        "Жмем фильтр \"Срок доставки\"".ybm_run { _ in
            filterPage = filtersPage.snippet(named: "Срок доставки")?.tap()
            wait(forVisibilityOf: filterPage.element)
        }

        "Жмем первую опцию 1-2 часа".ybm_run { _ in
            XCTAssertEqual(
                filterPage.snippets.first?.name.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "1-2 часа"
            )
            filterPage = filterPage.snippets.first?.tap()
            XCTAssertEqual(
                filterPage.snippets.first?.checkBox.identifier,
                FiltersSnippetAccessibility.checkBoxSelected
            )
            filterPage.countSnippet.dropButton.tap()
        }

        "Проверяем состояние после сброса".ybm_run { _ in
            XCTAssertEqual(
                filterPage.snippets.first?.checkBox.identifier,
                FiltersSnippetAccessibility.checkBox
            )
            XCTAssertFalse(filterPage.countSnippet.dropButton.isVisible)
        }
    }

}

// MARK: - Helper methods

private extension FiltersScreenTest {
    func setupFeedState() {
        stateManager?.mockingStrategy = .dtoMock
        var feedState = FeedState()
        feedState.setSearchOrUrlTransformState(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        feedState.setSearchOrRedirectState(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        feedState.setSearchStateFAPI(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        stateManager?.setState(newState: feedState)
    }

    func setupExpressState() {
        stateManager?.mockingStrategy = .dtoMock
        var feedState = FeedState()
        feedState.setSearchOrRedirectState(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        feedState.setSearchStateFAPI(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        stateManager?.setState(newState: feedState)

        var cmsState = CMSState()
        cmsState.setCMSState(with: CMSState.CMSCollections.expressCollections)
        stateManager?.setState(newState: cmsState)

        let expressState = ExpressState()
        stateManager?.setState(newState: expressState)

        var authState = UserAuthState()
        authState.setAddressesState(addresses: [.default])
        authState.setPlusBalanceState(.withZeroMarketCashback)
        stateManager?.setState(newState: authState)
    }

    func checkFilterOptions(page filterPage: FilterPage) {
        "Проверяем список опций".ybm_run { _ in
            filterPage.snippets.enumerated().forEach { index, filterSnippet in
                XCTAssertEqual(
                    filterSnippet.name.label.trimmingCharacters(in: .whitespacesAndNewlines),
                    filterOptionLabels[index]
                )
            }
        }
    }

    func openExpressFeed(search: String) -> FeedPage {
        var feedPage: FeedPage!
        "Открываем экспресс-выдачу".ybm_run { _ in
            let expressPage: ExpressPage!
            expressPage = goToExpress()
            wait(forVisibilityOf: expressPage.collectionView)

            let searchPage = expressPage.goToSearch()

            ybm_wait(forVisibilityOf: [searchPage.navigationBar.searchTextField])

            searchPage.navigationBar.searchTextField.tap()
            searchPage.navigationBar.searchTextField.typeText(search + "\n")

            let feedElement = XCUIApplication().otherElements[FeedAccessibility.root]
            feedPage = FeedPage(element: feedElement)
            ybm_wait(forVisibilityOf: [feedElement])
        }
        return feedPage
    }
}

// MARK: - Nested Types

private extension FiltersScreenTest {

    enum Constants {
        static let feedOffers = [
            CAPIOffer.protein,
            CAPIOffer.protein1
        ]
    }
}
