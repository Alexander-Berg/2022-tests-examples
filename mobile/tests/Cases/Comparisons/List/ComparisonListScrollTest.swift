import XCTest

final class ComparisonListScrollTest: ComparisonBaseTest {

    func testScroll() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3523")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран списков сравнения")
        Allure.addTitle("Скролл списков")

        var comparisonList: ComparisonListPage!

        "Мокаем список сравнений".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_Scroll")
        }

        "Перейти в сравнения".ybm_run { _ in
            comparisonList = goToComparison()
        }

        "Проскролить вниз в конец списка".ybm_run { _ in
            comparisonList.collectionView.element.swipe(
                to: .down,
                untilVisible: comparisonList.comparisonCell.element.staticTexts["Умные часы и браслеты"]
            )
        }

        "Проскролить вверх в начало списка".ybm_run { _ in
            comparisonList.collectionView.element.swipe(
                to: .up,
                untilVisible: comparisonList.fistComparisonCell.element
            )
        }
    }
}
