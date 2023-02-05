import MarketUITestMocks
import XCTest

class CashbackProfilePlusTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testYandexPlusBadgeExistsInProfile() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3577")
        Allure.addEpic("Авторизация под Я+")
        Allure.addFeature("Кешбэк")
        Allure.addTitle("Отображение плашки Я+ и переход на лэндинг")

        disable(toggles: FeatureNames.plusSdk)

        var root: RootPage!
        var profile: ProfilePage!
        var promo: WebViewPage!

        setState()

        "Включаем кешбэк".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CashbackAvailableInYandexPlus")
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Идем в профиль".ybm_run { _ in
            profile = goToProfile(root: root)
        }

        "Ищем ячейку с Яндекс+".ybm_run { _ in
            wait(forVisibilityOf: profile.yandexPlus.element)
            XCTAssertEqual(
                profile.yandexPlus.title.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "Яндекс Плюс"
            )
            promo = profile.yandexPlus.tap()
        }

        "Ищем название промостраницы".ybm_run { _ in
            wait(forVisibilityOf: promo.navigationBar.title)
            XCTAssertEqual(
                promo.navigationBar.title.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "Яндекс Плюс - фильмы, музыка и кешбэк баллами"
            )
        }
    }

    private func setState() {
        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withBalance(1_458))
            stateManager?.setState(newState: authState)
        }
    }
}
