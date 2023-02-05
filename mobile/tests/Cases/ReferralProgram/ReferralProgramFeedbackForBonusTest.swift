import MarketUITestMocks
import XCTest

class ReferralProgramFeedbackForBonusTest: LocalMockTestCase {

    // MARK: - Public

    func opinionCreationForBonusFlow(
        enabledToggles: [String],
        bundles: [String] = [],
        isReachedBonusLimit: Bool = false
    ) -> OpinionCreationSucceededPage {

        var root: RootPage!
        var opinonAgitationPopup: OpinionAgitationPopupPage!
        var opinionCreationSucceededPage: OpinionCreationSucceededPage!

        var baseBundles = ["ReferralProgram", "SaveOpinion"] + bundles

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = [
            FeatureNames.product_review_agitation,
            FeatureNames.referralProgram
        ].joined(separator: ",")

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

        "Авторизуемся, открываем морду".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            goToMorda(root: root)
        }

        "Скролим до истории просмотров. Проверяем, что агитация появилась".ybm_run { _ in
            opinonAgitationPopup = swipeDownToMakeOpinionAgitationAppear()
        }

        "Тапаем на звезды. Отправляем отзыв".ybm_run { _ in
            opinonAgitationPopup.rateProduct()

            let leaveOpinion = LeaveOpinionPopUp.currentPopup
            wait(forVisibilityOf: leaveOpinion.element)
            typeOpinion(leaveOpinion)

            let opinionCreationMultifactorPage = leaveOpinion.tapContinueButton()
            opinionCreationSucceededPage = opinionCreationMultifactorPage.tapContinueButton()
        }

        return opinionCreationSucceededPage
    }

    // MARK: - Private

    @discardableResult
    private func swipeDownToMakeOpinionAgitationAppear() -> OpinionAgitationPopupPage {
        let opinonAgitationPopup = OpinionAgitationPopupPage.currentPopup
        MordaPage.current.element.swipe(to: .down, until: opinonAgitationPopup.element.isVisible)
        return opinonAgitationPopup
    }

    private func typeOpinion(_ opinionCreation: LeaveOpinionPopUp) {
        wait(forVisibilityOf: opinionCreation.prosText.element)
        opinionCreation.prosText.typeText("Норм")

        opinionCreation.collectionView.ybm_swipeCollectionView(toFullyReveal: opinionCreation.consText.element)
        opinionCreation.consText.typeText("Не заметил")

        opinionCreation.collectionView.ybm_swipeCollectionView(toFullyReveal: opinionCreation.commentText.element)
        opinionCreation.commentText.typeText("Хочу кешбек")
    }
}

// MARK: - ReferralProgramMockHelper

extension ReferralProgramFeedbackForBonusTest: ReferralProgramMockHelper {}
