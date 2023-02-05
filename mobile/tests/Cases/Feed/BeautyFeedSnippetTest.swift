import MarketUITestMocks
import XCTest

final class BeautyFeedSnippetTest: LocalMockTestCase {

    func testWithVendorAndTitlesWithoutVendor() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5524")
        Allure.addEpic("Выдача")
        Allure.addFeature("Бренд отображается отдельно от названия товара на сниппете")
        Allure
            .addTitle(
                """
                Бренд отображается отдельно от названия товара \
                на сниппете на выдаче, если у товара указан \
                вендор и заполнено поле titleWithoutVendor
                """
            )

        let search = "Протеин"

        "Мокаем состояние".ybm_run { _ in
            setupFeedState(showVendorNameSeparately: true, hasVendor: true, hasTitleWithoutVendor: true)
        }

        var feedPage: FeedPage!

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
        }

        "Отображаются найденные товары: проверка карточки первого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssertTrue(snippetPage.titleLabel.isVisible)
            XCTAssertTrue(snippetPage.vendorLabel.isVisible)
            XCTAssertEqual(snippetPage.vendorLabel.label, "CMTech")
            XCTAssertEqual(snippetPage.titleLabel.label, "Протеин Whey Protein Клубничный крем, 30 порций")
        }
    }

    func testWithVendor() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5527")
        Allure.addEpic("Выдача")
        Allure.addFeature("Бренд отображается отдельно от названия товара на сниппете")
        Allure
            .addTitle(
                """
                Бренд отображается отдельно от названия товара \
                на сниппете на выдаче, если у товара указан вендор \
                и нет названия товара без бренда (поле titleWithoutVendor)
                """
            )

        let search = "Протеин"

        "Мокаем состояние".ybm_run { _ in
            setupFeedState(showVendorNameSeparately: true, hasVendor: true, hasTitleWithoutVendor: false)
        }

        var feedPage: FeedPage!

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
        }

        "Отображаются найденные товары: проверка карточки первого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssertTrue(snippetPage.titleLabel.isVisible)
            XCTAssertTrue(snippetPage.vendorLabel.isVisible)
            XCTAssertEqual(snippetPage.vendorLabel.label, "CMTech")
            XCTAssertEqual(
                snippetPage.titleLabel.label,
                "Протеин CMTech Whey Protein Клубничный крем, 30 порций"
            )
        }
    }

    func testWithoutVendor() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5528")
        Allure.addEpic("Выдача")
        Allure.addFeature("Бренд не отображается отдельно от названия товара на сниппете")
        Allure
            .addTitle(
                """
                Бренд не отображается отдельно от названия \
                товара на сниппете на выдаче, если у товара \
                не указан вендор
                """
            )

        let search = "Протеин"

        "Мокаем состояние".ybm_run { _ in
            setupFeedState(showVendorNameSeparately: true, hasVendor: false, hasTitleWithoutVendor: true)
        }

        var feedPage: FeedPage!

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
        }

        "Отображаются найденные товары: проверка карточки первого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssertTrue(snippetPage.titleLabel.isVisible)
            XCTAssertFalse(snippetPage.vendorLabel.isVisible)
            XCTAssertEqual(snippetPage.titleLabel.label, "Протеин CMTech Whey Protein Клубничный крем, 30 порций")
        }
    }

    func testWithoutShowVendorNameSeparately() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5529")
        Allure.addEpic("Выдача")
        Allure.addFeature("Бренд не отображается отдельно от названия товара на сниппете")
        Allure
            .addTitle(
                """
                Бренд не отображается отдельно от названия \
                товара на сниппете на выдаче, если в СMS \
                отключен параметр отдельного отображения
                """
            )

        let search = "Протеин"

        "Мокаем состояние".ybm_run { _ in
            setupFeedState(showVendorNameSeparately: false, hasVendor: true, hasTitleWithoutVendor: true)
        }

        var feedPage: FeedPage!

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
        }

        "Отображаются найденные товары: проверка карточки первого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssertTrue(snippetPage.titleLabel.isVisible)
            XCTAssertFalse(snippetPage.vendorLabel.isVisible)
            XCTAssertEqual(snippetPage.titleLabel.label, "Протеин CMTech Whey Protein Клубничный крем, 30 порций")
        }
    }
}

private extension BeautyFeedSnippetTest {
    func setupFeedState(showVendorNameSeparately: Bool, hasVendor: Bool, hasTitleWithoutVendor: Bool) {

        let searchConfigurations: [
            ResolveSearch
                .SearchConfiguration
        ] = showVendorNameSeparately ? [.showVendorNameSeparately] : [.default]

        let offers: [FAPIOffer] = hasTitleWithoutVendor ? [.withTitlesWithoutVendor] : [.protein]

        let vendors: [FAPIVendor] = hasVendor ? [.protein] : []

        var feedState = FeedState()
        feedState.setSearchOrRedirectState(mapper: .init(
            offers: offers,
            intent: ResolveSearch.Intent.chocolates,
            vendors: vendors,
            searchConfigurations: searchConfigurations

        ))

        stateManager?.setState(newState: feedState)
    }
}
