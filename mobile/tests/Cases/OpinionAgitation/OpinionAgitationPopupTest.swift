import MarketUITestMocks
import XCTest

final class OpinionAgitationPopupTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testOpinionAgitationPopupWhenSingleActionContainerWidgetIsVisiable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4203")
        Allure.addEpic("Морда")
        Allure.addFeature("Попап агитации")
        Allure.addTitle("Проверяем корректность работы попапа агитации, когда есть singleActionContainerWidget")

        var root: RootPage!
        var morda: MordaPage!
        var opinonAgitationPopup: OpinionAgitationPopupPage!

        enable(toggles: FeatureNames.product_review_agitation, FeatureNames.softUpdateWidget)

        "Мокаем агитацию".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OpinionAgitation")
            mockStateManager?.pushState(bundleName: "MordaSet_SoftUpdateOutdatedVersion")
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Проверяем наличие singleActionContainerWidget".ybm_run { _ in
            wait(forVisibilityOf: morda.singleActionContainerWidget.element)
        }

        "Скролим до истории просмотров. Проверяем, что агитация появилась".ybm_run { _ in
            opinonAgitationPopup = swipeDownToMakeOpinionAgitationAppear()
        }

        "Скролим вверх. Проверяем, что агитация исчезла".ybm_run { _ in
            let widget = morda.historyWidget
            widget.collectionView.ybm_checkingSwipe(
                to: .up,
                until: !opinonAgitationPopup.element.isVisible,
                checkConditionPerCycle: { morda.singleActionContainerWidget.element.isVisible }
            )
            XCTAssertFalse(opinonAgitationPopup.element.isVisible)
        }

        "Проверяем исчезновение/появление попапа при тапе на другой таб".ybm_run { _ in
            swipeDownToMakeOpinionAgitationAppear()
            goToCatalog(root: root)
            XCTAssertFalse(opinonAgitationPopup.element.isVisible)
            goToMorda(root: root)
            wait(forVisibilityOf: opinonAgitationPopup.element)
        }
    }

    func testOpinionAgitationPopupWhenSingleActionContainerWidgetIsNotVisiable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4204")
        Allure.addEpic("Морда")
        Allure.addFeature("Попап агитации")
        Allure.addTitle("Проверяем корректность работы попапа агитации, когда нет singleActionContainerWidget")

        var root: RootPage!
        var opinonAgitationPopup: OpinionAgitationPopupPage!

        enable(toggles: FeatureNames.product_review_agitation)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OpinionAgitation_MordaWihoutSingleActionWidget")
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            goToMorda(root: root)
        }

        "Проверяем, что агитация появилась".ybm_run { _ in
            opinonAgitationPopup = OpinionAgitationPopupPage.currentPopup
            wait(forVisibilityOf: opinonAgitationPopup.element)
        }

        "Проверяем исчезновение/появление попапа при тапе на другой таб".ybm_run { _ in
            goToCatalog(root: root)
            XCTAssertFalse(opinonAgitationPopup.element.isVisible)
            goToMorda(root: root)
            wait(forVisibilityOf: opinonAgitationPopup.element)
        }
    }

    func testCreateOpinionFromOpinionAgitationPopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4205")
        Allure.addEpic("Морда")
        Allure.addFeature("Попап агитации")
        Allure.addTitle("Проверяем сохранение отзыва через агитационный попап")

        var root: RootPage!
        var opinonAgitationPopup: OpinionAgitationPopupPage!

        enable(toggles: FeatureNames.product_review_agitation)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OpinionAgitation_MordaWihoutSingleActionWidget")
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            goToMorda(root: root)
        }

        "Проверяем, что агитация появилась".ybm_run { _ in
            opinonAgitationPopup = OpinionAgitationPopupPage.currentPopup
            wait(forVisibilityOf: opinonAgitationPopup.element)
        }

        "Тапаем на звезды. Ожидаем появление флоу создания отзывов".ybm_run { _ in
            opinonAgitationPopup.rateProduct()

            let leaveOpinion = LeaveOpinionPopUp.currentPopup
            wait(forVisibilityOf: leaveOpinion.element)
            leaveOpinion.consText.typeText("Чушь")
            leaveOpinion.commentText.typeText("Зря потратил деньги :(")

            let opinionCreationMultifactorPage = leaveOpinion.tapContinueButton()
            let opinionCreationSucceededPage = opinionCreationMultifactorPage.tapContinueButton()
            wait(forVisibilityOf: opinionCreationSucceededPage.element)
        }
    }

    func testOpinionAgitationPopupWithCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4211")
        Allure.addEpic("Морда")
        Allure.addFeature("Попап агитации")
        Allure.addTitle("Проверяем агитационный попап с кешбеком")

        var root: RootPage!
        var opinonAgitationPopup: OpinionAgitationPopupPage!
        var opinionCreationSucceededPage: OpinionCreationSucceededPage!
        var webViewPage: WebViewPage!

        enable(toggles: FeatureNames.product_review_agitation)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OpinionAgitation_withCashback")
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
            leaveOpinion.prosText.typeText("Норм")
            leaveOpinion.consText.typeText("Не заметил")
            leaveOpinion.commentText.typeText("Хочу кешбек")

            let opinionCreationMultifactorPage = leaveOpinion.tapContinueButton()
            opinionCreationSucceededPage = opinionCreationMultifactorPage.tapContinueButton()
        }

        "Тестируем переходы по кешбечным элементам".ybm_run { _ in
            let plusBadgeCell = opinionCreationSucceededPage.plusBadgeCell
            let plusLinkButton = opinionCreationSucceededPage.plusLinkButton

            ybm_wait(forVisibilityOf: [plusBadgeCell, plusLinkButton])

            plusLinkButton.tap()
            webViewPage = WebViewPage.current
            wait(forVisibilityOf: WebViewPage.current.element)
            webViewPage.navigationBar.closeButton.tap()
            wait(forVisibilityOf: opinionCreationSucceededPage.element)

            plusBadgeCell.tap()
            wait(forVisibilityOf: HomePlusPage.current.element)
        }
    }

    @discardableResult
    private func swipeDownToMakeOpinionAgitationAppear() -> OpinionAgitationPopupPage {
        let opinonAgitationPopup = OpinionAgitationPopupPage.currentPopup
        MordaPage.current.element.swipe(to: .down, until: opinonAgitationPopup.element.isVisible)
        return opinonAgitationPopup
    }

}
