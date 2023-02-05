import MarketUITestMocks
import XCTest

class AgitationsUnauthorizedTestCase: AgitationsTestCase {

    // MARK: - Helper Methods

    func setup(bundleName: String) {
        "Мокаем данные".ybm_run { _ in
            mockStateManager?.pushState(bundleName: bundleName)
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: .default
                )
            )
            stateManager?.setState(newState: skuState)
        }

        "Открываем приложение".ybm_run { _ in
            appAfterOnboardingAndPopups()
        }
    }
}
