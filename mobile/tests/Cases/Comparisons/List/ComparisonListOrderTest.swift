import XCTest

final class ComparisonListOrderTest: ComparisonBaseTest {

    func testOrder() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3505")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран списков сравнения")
        Allure.addTitle("Алфавитный порядок списков")

        var comparisonList: ComparisonListPage!
        let comparison = { index in comparisonList.comparisonCell(with: index) }

        "Мокаем список сравнений".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_Order")
        }

        "Перейти в сравнения".ybm_run { _ in
            comparisonList = goToComparison()
        }

        "Проверяем что после авторизации в списке есть сравнения".ybm_run { _ in
            wait(forVisibilityOf: comparisonList.fistComparisonCell.element)

            XCTAssertEqual(comparison(0).title.label, "Аккумуляторные батареи")
            XCTAssertEqual(comparison(0).count.label, "1 товар")

            XCTAssertEqual(comparison(1).title.label, "Велосипеды для взрослых и детей")
            XCTAssertEqual(comparison(1).count.label, "1 товар")

            XCTAssertEqual(comparison(2).title.label, "Мужские бритвы и лезвия")
            XCTAssertEqual(comparison(2).count.label, "1 товар")
        }
    }
}
