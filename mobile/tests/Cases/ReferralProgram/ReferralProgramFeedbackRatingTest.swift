import MarketUITestMocks
import XCTest

final class ReferralProgramFeedbackRatingTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testFeedbackRatingBlockWithPlusBefore3000() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4489")
        Allure.addEpic("Реферальная программа")
        Allure.addFeature("Отзыв")
        Allure.addTitle("Блок на странице отзыва у пользователя с Плюсом")

        var button: ReferralButton!

        "Проходим флоу написания отзыва".ybm_run { _ in
            button = opinionCreationFlow(enabledToggles: [FeatureNames.referralProgram])
        }

        "Переходим на экран реферальной программы".ybm_run { _ in
            XCTAssertEqual(button.title, "Получить 300 баллов за друга")
            let referralPromocodePage = button.tapReferral()
            wait(forVisibilityOf: referralPromocodePage.element)
        }
    }

    func testFeedbackRatingBlockWithPlusAfter3000() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4490")
        Allure.addEpic("Реферальная программа")
        Allure.addFeature("Отзыв")
        Allure.addTitle("Блок на странице отзыва у пользователя с Плюсом, достигшим лимит в 3000 баллов")

        var button: ReferralButton!

        "Проходим флоу написания отзыва".ybm_run { _ in
            button = opinionCreationFlow(
                enabledToggles: [FeatureNames.referralProgram],
                isReachedBonusLimit: true
            )
        }

        "Переходим на экран реферальной программы".ybm_run { _ in
            XCTAssertEqual(button.title, "Рекомендовать Маркет друзьям")
            let parthnerPage = button.tapPartner()
            wait(forVisibilityOf: parthnerPage.element)
        }
    }
}

// MARK: - ReferralProgramMockHelper

extension ReferralProgramFeedbackRatingTest: ReferralProgramMockHelper {}

extension ReferralProgramFeedbackRatingTest {
    func writeFeedback() -> OpinionCreationSucceededPage {
        let opinionGrade = OpinionGradePage.current
        wait(forVisibilityOf: opinionGrade.element)
        opinionGrade.tapStars()
        let opinionCreation = LeaveOpinionPopUp.currentPopup
        wait(forVisibilityOf: opinionCreation.element)

        typeOpinion(opinionCreation)

        let opinionCreationMultifactorPage = opinionCreation.tapContinueButton()
        wait(forVisibilityOf: opinionCreationMultifactorPage.element)

        let popUpPage = OpinionCreationPopupPage.currentPopup

        opinionCreationMultifactorPage.tapCloseButton()

        wait(forVisibilityOf: popUpPage.element)

        let opinionCreationSucceededPage = popUpPage.tapCancelButton()

        return opinionCreationSucceededPage
    }

    func typeOpinion(_ opinionCreation: LeaveOpinionPopUp) {
        wait(forVisibilityOf: opinionCreation.prosText.element)
        opinionCreation.prosText.typeText(Constants.prosText)

        opinionCreation.collectionView.ybm_swipeCollectionView(toFullyReveal: opinionCreation.consText.element)
        opinionCreation.consText.typeText(Constants.consText)

        opinionCreation.collectionView.ybm_swipeCollectionView(toFullyReveal: opinionCreation.commentText.element)
        opinionCreation.commentText.typeText(Constants.commentText)
    }

    func opinionCreationFlow(
        enabledToggles: [String],
        bundles: [String] = [],
        isReachedBonusLimit: Bool = false
    ) -> ReferralButton {

        var ordersListPage: OrdersListPage!
        var baseBundles = ["ReferralProgram", "SaveOpinion"] + bundles

        "Настраиваем FT и мокаем startup для получения эксперимента".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = enabledToggles.joined(separator: ",")
            mockStateManager?.pushState(bundleName: "Experiments_ReferralProgram")
        }

        let endDate = makeStringRepresentation(of: Date().addingTimeInterval(.week))

        let promocodeUpdateBundle = "ReferralProgramPromocodeDateUpdate"
        "Изменяем дату окончания промокода на валидную".ybm_run { _ in
            mockStateManager?.changeMock(
                bundleName: "ReferralProgram",
                newBundleName: promocodeUpdateBundle,
                filename: "POST_api_v1_resolveReferralPromocode",
                changes: [
                    (
                        #""refererPromoCodeExpiredDate" : "2021-06-28T13:36:36.562790Z""#,
                        "\"refererPromoCodeExpiredDate\" : \"\(endDate.en)\""
                    )
                ]
            )
            baseBundles.append(promocodeUpdateBundle)
        }

        if isReachedBonusLimit {
            let programStatusChangedBundleName = "ReferralProgramStatusChanged"
            "Изменяем поле получения максимального вознаграждения за программу в статусе программы".ybm_run { _ in
                mockStateManager?.changeMock(
                    bundleName: "ReferralProgram",
                    newBundleName: programStatusChangedBundleName,
                    filename: "POST_api_v2_resolveReferralProgramStatus",
                    changes: [
                        (
                            #""isGotFullReward" : false"#,
                            #""isGotFullReward" : true"#
                        )
                    ]
                )
                baseBundles.append(programStatusChangedBundleName)
            }

            let programPromocodeChangedBundleName = "ReferralProgramPromocodeChanged"
            "Изменяем поле получения максимального вознаграждения за программу в промокоде".ybm_run { _ in
                mockStateManager?.changeMock(
                    bundleName: "ReferralProgram",
                    newBundleName: programPromocodeChangedBundleName,
                    filename: "POST_api_v1_resolveReferralPromocode",
                    changes: [
                        (
                            #""isGotFullReward" : false"#,
                            #""isGotFullReward" : true"#
                        )
                    ]
                )
                baseBundles.append(programPromocodeChangedBundleName)
            }
        }

        "Настраиваем стейт".ybm_run { _ in
            let orderMapper = OrdersState.UserOrdersHandlerMapper(
                orders: [
                    Order.Mapper(
                        id: "51457525",
                        status: .delivered,
                        msku: ["101077347763"]
                    )
                ]
            )
            var orderState = OrdersState()
            orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])
            stateManager?.setState(newState: orderState)

            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: .default
                )
            )
            stateManager?.setState(newState: skuState)
        }

        "Мокаем состояние".ybm_run { _ in
            baseBundles.forEach { mockStateManager?.pushState(bundleName: $0) }
        }

        "Авторизуемся, открываем список покупок".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            ordersListPage = goToOrdersListPage(root: root)
        }

        var opinionCreationSucceededPage: OpinionCreationSucceededPage!

        "Создаем отзыв".ybm_run { _ in
            ordersListPage.rate()
            opinionCreationSucceededPage = writeFeedback()
        }

        return opinionCreationSucceededPage.referralButton
    }
}

// MARK: - Nested types

private extension ReferralProgramFeedbackRatingTest {

    enum Constants {
        static let prosText = "Достоинства"
        static let consText = "Недостатки"
        static let commentText = "Комментарий"
    }
}
