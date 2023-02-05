import XCTest

class ComparisonBaseTest: LocalMockTestCase {

    override func setUp() {
        super.setUp()

        mockStateManager?.pushState(bundleName: "Comparisons_Basic")
    }

    /// Переход в "Список сравнений"
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану сравнений
    @discardableResult
    func goToComparison(
        root: RootPage? = nil,
        file: StaticString = #file,
        line: UInt = #line
    ) -> ComparisonListPage {
        let profile = goToProfile(root: root)

        wait(forVisibilityOf: profile.comparison.element, file: file, line: line)

        let comparison = profile.comparison.tap()
        wait(forVisibilityOf: comparison.element, file: file, line: line)

        return comparison
    }

    /// Переход в экран сравнения
    ///
    /// - Parameter root: Путь к главному контейнеру, по умолчанию будет вызываться `appAfterOnboardingAndPopups`
    /// - Returns: Путь к экрану сравнений
    @discardableResult
    func goToComparisonScreen(root: RootPage? = nil) -> ComparisonPage {
        let list = goToComparison(root: root)

        let comparison = list.fistComparisonCell.tap()

        wait(forVisibilityOf: comparison.element)

        return comparison
    }

}
