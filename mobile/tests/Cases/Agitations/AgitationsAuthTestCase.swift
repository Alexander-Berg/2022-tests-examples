import MarketUITestMocks
import XCTest

class AgitationsAuthTestCase: AgitationsTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    // MARK: - Helper Methods

    func setup(bundleName: String) {
        "Открываем приложение и логинимся".ybm_run { _ in
            appAfterOnboardingAndPopups()
        }

        "Мокаем данные".ybm_run { _ in
            mockStateManager?.pushState(bundleName: bundleName)
        }
    }
}
