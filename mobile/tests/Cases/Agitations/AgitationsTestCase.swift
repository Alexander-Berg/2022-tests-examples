import XCTest

class AgitationsTestCase: LocalMockTestCase {

    typealias PopupPage = AgitationPopupPage

    var popupPage: PopupPage!

    override func setUp() {
        super.setUp()
        enable(toggles: FeatureNames.orderConsultation)
    }

    override func tearDown() {
        popupPage = nil
        super.tearDown()
    }

    // MARK: - Helper Methods

    func openPopup(description: String) {
        "Ждем попап".ybm_run { _ in
            popupPage = PopupPage.currentPopup
            ybm_wait(forVisibilityOf: [popupPage.descriptionLabel])
            XCTAssertEqual(popupPage.descriptionLabel.label, description)
        }

        "Мокаем пустой ответ резолвера агитаций".run {
            let agitationsRule = MockMatchRule(
                id: "EMPTY_AGITATIONS_RULE",
                matchFunction:
                isPOSTRequest &&
                    isFAPIRequest &&
                    hasExactFAPIResolvers(["resolveOrderAgitations"]),
                mockName: "resolveOrderAgitations_empty"
            )
            mockServer?.addRule(agitationsRule)
        }
    }

    func acceptPopup(description: String) {
        "Подтверждаем агитацию".ybm_run { _ in
            popupPage.firstButton.tap()
            ybm_wait(forFulfillmentOf: { self.popupPage.descriptionLabel.label == description })
        }
    }

    func declinePopup(description: String) {
        "Подтверждаем агитацию".ybm_run { _ in
            popupPage.lastButton.tap()
            ybm_wait(forFulfillmentOf: { self.popupPage.descriptionLabel.label == description })
        }
    }

    func openChat() {
        "Открываем чат".ybm_run { _ in
            popupPage.firstButton.tap()
            ybm_wait(forFulfillmentOf: { !self.popupPage.descriptionLabel.isVisible })
            ybm_wait(forVisibilityOf: [ConsultationChatPage.current.element])
        }
    }

    func closePopup() {
        "Закрываем попап".ybm_run { _ in
            popupPage.lastButton.tap()
            ybm_wait(forFulfillmentOf: { !self.popupPage.descriptionLabel.isVisible })
        }
    }
}
