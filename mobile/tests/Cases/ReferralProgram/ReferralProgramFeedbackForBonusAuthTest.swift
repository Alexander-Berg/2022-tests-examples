import MarketUITestMocks
import XCTest

final class ReferralProgramFeedbackForBonusAuthTest: ReferralProgramFeedbackForBonusTest {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testFeedbackForBonusBlockNotPlusUser() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4488")
        Allure.addEpic("Реферальная программа")
        Allure.addFeature("Отзыв за баллы")
        Allure.addTitle("Блок на странице отзыва пользователя плюса, который не достиг 3000 баллов")

        let referralProgramStatusChangedBundle = "ReferralProgramStatusChanged"
        "Изменяем доступность реферальной программы".ybm_run { _ in
            mockStateManager?.changeMock(
                bundleName: "ReferralProgram",
                newBundleName: referralProgramStatusChangedBundle,
                filename: "POST_api_v2_resolveReferralProgramStatus",
                changes: [
                    (
                        #""isPurchased" : true"#,
                        #""isPurchased" : false"#
                    )
                ]
            )
        }

        var button: XCUIElement!

        "Проходим флоу написания отзыва за баллы".ybm_run { _ in
            let page = opinionCreationForBonusFlow(
                enabledToggles: [
                    FeatureNames.product_review_agitation,
                    FeatureNames.referralProgram
                ],
                bundles: [
                    "OpinionAgitation_withCashback",
                    "Experiments_ReferralProgram",
                    "ReferralProgram",
                    referralProgramStatusChangedBundle
                ]
            )
            button = page.plusLinkButton

            ybm_wait(forVisibilityOf: [button])
        }

        "Проверяем заголовок нопки информации о Плюсе".ybm_run { _ in
            XCTAssertEqual(button.label, "Читать про Плюс")
        }
    }
}
