import MarketUITestMocks
import XCTest

final class ReferralProgramOnboardingTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testReferallProgramBefore3000() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4484")
        Allure.addEpic("Реферальная программа")
        Allure.addFeature("Блок на экране Спасибо")
        Allure.addTitle("Наличие экрана у пользователя с баллами до 3000")

        var button: ReferralButton!
        var referallPage: ReferralPromocodePage!

        let endDate = makeStringRepresentation(of: Date().addingTimeInterval(.week))

        disable(toggles: FeatureNames.cartRedesign)

        "Изменяем дату окончания промокода на валидную".ybm_run { _ in
            mockStateManager?.changeMock(
                bundleName: "ReferralProgram",
                newBundleName: "ReferralProgramPromocodeDateUpdate",
                filename: "POST_api_v1_resolveReferralPromocode",
                changes: [
                    (
                        #""refererPromoCodeExpiredDate" : "2021-06-28T13:36:36.562790Z""#,
                        "\"refererPromoCodeExpiredDate\" : \"\(endDate.en)\""
                    )
                ]
            )
        }

        "Мокаем реферальную программу".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgram")
            mockStateManager?.pushState(bundleName: "ReferralProgramPromocodeDateUpdate")
        }

        "Авторизуемся и заказываем товар".ybm_run { _ in
            button = cartCheckoutFlow()

            XCTAssertEqual(
                button.title,
                "Получить 300 баллов за друга"
            )
        }

        "Открываем окно реферальной программы".ybm_run { _ in
            referallPage = button.tapReferral()
            wait(forVisibilityOf: referallPage.element)

            XCTAssertEqual(
                referallPage.title.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "Ваш промокод\nSOME PROMO"
            )
            XCTAssertEqual(
                referallPage.text.label,
                "Друзьям — скидка 500 ₽ на первый заказ в приложении от 5 000 ₽, а вам —  300 баллов Плюса. Промокод действует до \(endDate.ru). "
            )
            XCTAssertEqual(referallPage.button.label, "Отправить промокод другу")
        }
    }

    func testReferallProgramAfter3000() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4485")
        Allure.addEpic("Реферальная программа")
        Allure.addFeature("Блок на экране Спасибо")
        Allure.addTitle("Наличие экрана у пользователя, который достиг 3000")

        var button: ReferralButton!
        var partnerPage: PartnerProgramPopupPage!

        let endDate = makeStringRepresentation(of: Date().addingTimeInterval(.week))

        disable(toggles: FeatureNames.cartRedesign)

        "Изменяем дату окончания промокода на валидную".ybm_run { _ in
            mockStateManager?.changeMock(
                bundleName: "ReferralProgram",
                newBundleName: "ReferralProgramPromocodeDateUpdate",
                filename: "POST_api_v1_resolveReferralPromocode",
                changes: [
                    (
                        #""refererPromoCodeExpiredDate" : "2021-06-28T13:36:36.562790Z""#,
                        "\"refererPromoCodeExpiredDate\" : \"\(endDate.en)\""
                    )
                ]
            )
        }

        "Изменяем поле получения максимального вознаграждения за программу в статусе программы".ybm_run { _ in
            mockStateManager?.changeMock(
                bundleName: "ReferralProgram",
                newBundleName: "ReferralProgramStatusChanged",
                filename: "POST_api_v2_resolveReferralProgramStatus",
                changes: [
                    (
                        #""isGotFullReward" : false"#,
                        #""isGotFullReward" : true"#
                    )
                ]
            )
        }

        "Изменяем поле получения максимального вознаграждения за программу в промокоде".ybm_run { _ in
            mockStateManager?.changeMock(
                bundleName: "ReferralProgram",
                newBundleName: "ReferralProgramPromocodeChanged",
                filename: "POST_api_v1_resolveReferralPromocode",
                changes: [
                    (
                        #""isGotFullReward" : false"#,
                        #""isGotFullReward" : true"#
                    )
                ]
            )
        }

        "Мокаем реферальную программу".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgram")
            mockStateManager?.pushState(bundleName: "ReferralProgramPromocodeDateUpdate")
            mockStateManager?.pushState(bundleName: "ReferralProgramStatusChanged")
            mockStateManager?.pushState(bundleName: "ReferralProgramPromocodeChanged")
        }

        "Авторизуемся и заказываем товар".ybm_run { _ in
            button = cartCheckoutFlow()

            XCTAssertEqual(
                button.title,
                "Рекомендовать Маркет друзьям"
            )
        }

        "Открываем окно реферальной программы".ybm_run { _ in
            partnerPage = button.tapPartner()
            wait(forVisibilityOf: partnerPage.element)
            XCTAssertEqual(
                partnerPage.title.label,
                "Вы получили 3 000 баллов — максимум для этой акции"
            )
            XCTAssertEqual(partnerPage.button.label, "Узнать про партнёрство")
        }
    }

    func testReferralProgramIsFinished() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4504")
        Allure.addEpic("Профиль")
        Allure.addFeature("Реферальная программа")
        Allure.addTitle("Блок в Профиле после получения 3000 баллов (без subtitle)")

        var profile: ProfilePage!

        "Настраиваем FT и мокаем startup для получения эксперимента".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.referralProgram
            mockStateManager?.pushState(bundleName: "Experiments_ReferralProgram")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgramFinished")
        }

        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.noMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Авторизуемся, открываем профиль".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
        }

        "Находим пункт в профиле и смотрим, что заполнен только title".ybm_run { _ in
            profile.collectionView.swipe(to: .down, untilVisible: profile.referral.element)
            XCTAssertEqual(profile.referral.title.label, "Приглашайте друзей")
            XCTAssertFalse(profile.referral.subtitle.exists)
        }
    }

    func testReferralProgramOnboarding() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4505")
        Allure.addEpic("Профиль")
        Allure.addFeature("Реферальная программа")
        Allure.addTitle("Онбоардинг реферальной программы после получения 3000 баллов")

        var profilePage: ProfilePage!
        var referralOnboardPage: PartnerProgramPopupPage!
        var referralPromocodePage: ReferralPromocodePage!

        "Настраиваем FT и мокаем startup для получения эксперимента".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.referralProgram
            mockStateManager?.pushState(bundleName: "Experiments_ReferralProgram")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgramOnboarding")
        }

        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.noMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Авторизуемся, открываем профиль".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            profilePage = goToProfile(root: root)
        }

        "Открываем окно реферальной программы, проверяем наличие onboard на экране".ybm_run { _ in
            openReferralProgram(on: profilePage)
            referralOnboardPage = PartnerProgramPopupPage.current
            wait(forVisibilityOf: referralOnboardPage.element)
        }

        "Тапаем на \"Узнать про партнерство\"".ybm_run { _ in
            wait(forVisibilityOf: referralOnboardPage.button)
            referralOnboardPage.openDetails()
            waitForOpeningBrowserThenClose()
        }

        "Возвращаемся на окно профиля".ybm_run { _ in
            referralPromocodePage = ReferralPromocodePage.current
            wait(forVisibilityOf: referralPromocodePage.element)
            referralPromocodePage.button.tap()
            wait(forVisibilityOf: profilePage.element)
        }

        "Снова открываем окно реферальной программы, должно открыться окно \"Пригласите друзей\""
            .ybm_run { _ in
                openReferralProgram(on: profilePage)
                referralPromocodePage = ReferralPromocodePage.current
                wait(forVisibilityOf: referralPromocodePage.element)
            }

        "Проверяем работу ссылки при тапе на \"Узнать про партнерство\"".ybm_run { _ in
            wait(forVisibilityOf: referralPromocodePage.partnerLink)
            referralPromocodePage.openPartnerSite()
            waitForOpeningBrowserThenClose()
        }
    }
}

// MARK: - ReferralProgramMockHelper

extension ReferralProgramOnboardingTests: ReferralProgramMockHelper {}

// MARK: - Private

private extension ReferralProgramOnboardingTests {

    func openReferralProgram(on profilePage: ProfilePage) {
        profilePage.collectionView.swipe(to: .down, untilVisible: profilePage.referral.element)
        profilePage.referral.tap()
    }

    func waitForOpeningBrowserThenClose() {
        let browser = WebViewPage.current
        wait(forVisibilityOf: browser.element)
        browser.navigationBar.closeButton.tap()
        wait(forInvisibilityOf: browser.element)
    }

    func cartCheckoutFlow() -> ReferralButton {
        var root: RootPage!
        var cart: CartPage!
        var checkoutPage: CheckoutPage!
        var finishPage: FinishMultiorderPage!
        var finishButton: ReferralButton!

        "Настраиваем FT".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.referralProgram
        }

        "Мокаем эксперимент".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_ReferralProgram")
        }

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralPrepareCart")
        }

        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.noMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Идем в корзину".ybm_run { _ in
            cart = goToCart(root: root)
            wait(forExistanceOf: cart.compactSummary.orderButton.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cart.compactSummary.orderButton.tap()
        }

        "Меняем оплату, так как картой оплатить не получается в тесте".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.paymentMethodCell.element)
            let paymentTypePage = checkoutPage.paymentMethodCell.tap()
            paymentTypePage.selectPaymentMethod(with: "CASH_ON_DELIVERY")
            paymentTypePage.continueButton.tap()

            wait(forVisibilityOf: checkoutPage.element)
        }

        "Ищем кнопку реферальной программы на последнем шаге".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.paymentButton.element)
            finishPage = checkoutPage.paymentButton.tap()

            wait(forVisibilityOf: finishPage.element)

            finishButton = finishPage.referalButton
            finishPage.element.swipe(to: .down, untilVisible: finishButton.element)
        }

        return finishButton
    }
}
