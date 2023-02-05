import MarketUITestMocks
import XCTest

final class ReferralProgramFeedbackForBonusYaPlusTest: ReferralProgramFeedbackForBonusTest {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testFeedbackForBonusBlockBefore3000() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4486")
        Allure.addEpic("Реферальная программа")
        Allure.addFeature("Отзыв за баллы")
        Allure.addTitle("Блок на странице отзыва")

        var button: ReferralButton!

        "Проходим флоу написания отзыва за баллы".ybm_run { _ in
            let page = opinionCreationForBonusFlow(
                enabledToggles: [
                    FeatureNames.product_review_agitation,
                    FeatureNames.referralProgram
                ],
                bundles: ["OpinionAgitation_withCashback", "Experiments_ReferralProgram", "ReferralProgram"]
            )
            button = page.referralButton

            ybm_wait(forVisibilityOf: [button.element])
        }

        "Переходим на экран реферальной программы".ybm_run { _ in
            XCTAssertEqual(button.title, "Получить 300 баллов за друга")
            let referralPromocodePage = button.tapReferral()
            wait(forVisibilityOf: referralPromocodePage.element)
        }
    }

    func testFeedbackForBonusBlockAfter3000() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4487")
        Allure.addEpic("Реферальная программа")
        Allure.addFeature("Отзыв за баллы")
        Allure.addTitle("Блок на странице отзыва пользователя достигшего 3000 баллов")

        var button: ReferralButton!

        "Проходим флоу написания отзыва за баллы".ybm_run { _ in
            let page = opinionCreationForBonusFlow(
                enabledToggles: [
                    FeatureNames.product_review_agitation,
                    FeatureNames.referralProgram
                ],
                bundles: ["OpinionAgitation_withCashback", "Experiments_ReferralProgram", "ReferralProgram"],
                isReachedBonusLimit: true
            )
            button = page.referralButton

            ybm_wait(forVisibilityOf: [button.element])
        }

        "Переходим на экран реферальной программы".ybm_run { _ in
            XCTAssertEqual(button.title, "Рекомендовать Маркет друзьям")
            let parthnerPage = button.tapPartner()
            wait(forVisibilityOf: parthnerPage.element)
        }
    }
}
