import UIUtils
import XCTest

final class ComparisonListEmptyTest: ComparisonBaseTest {

    func testEmpty() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3526")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран списков сравнения")
        Allure.addTitle("Незалогин/залогин, без товаров")

        "Мокаем пустой список сравнений".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_Empty")
        }

        checkEmptyState()

        "Перейти в профиль".ybm_run { _ in
            let profile = goToProfile()
            profile.auth.tap()

            completeAuthFlow()
        }

        checkEmptyState()
    }

    private func checkEmptyState() {
        var comparisonList: ComparisonListPage!

        "Перейти в сравнения".ybm_run { _ in
            comparisonList = goToComparison()

            wait(forVisibilityOf: comparisonList.emptyView.element)

            XCTAssertEqual(
                comparisonList.emptyView.title.label,
                "Сравним что-нибудь?"
            )

            XCTAssertEqual(
                comparisonList.emptyView.subtitle.label,
                "Нажимайте на кнопку  , и все товары для сравнения появятся здесь"
            )

            XCTAssertEqual(
                comparisonList.emptyView.actionButton.label,
                "Посмотреть товары"
            )
        }

        "Тапнуть на Посмотреть товары".ybm_run { _ in
            comparisonList.emptyView.actionButton.tap()
            wait(forVisibilityOf: RootPage(element: app).tabBar.catalogPage.element)
        }
    }
}
