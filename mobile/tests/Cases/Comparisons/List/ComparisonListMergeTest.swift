import XCTest

final class ComparisonListMergeTest: ComparisonBaseTest {

    func testMerge() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3503")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран списков сравнения")
        Allure.addTitle("Мердж сравнения.")

        var comparisonList: ComparisonListPage!
        let comparison = { index in comparisonList.comparisonCell(with: index) }

        "Мокаем пустой список сравнений".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_BeforeMerge")
        }

        "Перейти в сравнения".ybm_run { _ in
            comparisonList = goToComparison()
        }

        "Мокаем список сравнений".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_AfterMerge")
        }

        "Проверяем что после авторизации в списке есть сравнения".ybm_run { _ in
            wait(forVisibilityOf: comparisonList.fistComparisonCell.element)

            XCTAssertEqual(comparison(0).title.label, "Аксессуары для портативных радиостанций")
            XCTAssertEqual(comparison(0).count.label, "1 товар")
        }

        "По клику на кнопку открывается экран авторизации".ybm_run { _ in
            wait(forVisibilityOf: comparisonList.loginButton)
            comparisonList.loginButton.tap()

            completeAuthFlow()

            wait(forInvisibilityOf: comparisonList.loginButton)
        }

        "Проверяем что после авторизации в списке есть сравнения".ybm_run { _ in
            wait(forVisibilityOf: comparisonList.fistComparisonCell.element)

            XCTAssertEqual(comparison(0).title.label, "Автомобильные петли")
            XCTAssertEqual(comparison(0).count.label, "1 товар")

            XCTAssertEqual(comparison(1).title.label, "Аккумуляторные батареи")
            XCTAssertEqual(comparison(1).count.label, "1 товар")

            XCTAssertEqual(comparison(2).title.label, "Аксессуары для портативных радиостанций")
            XCTAssertEqual(comparison(2).count.label, "1 товар")
        }
    }
}
