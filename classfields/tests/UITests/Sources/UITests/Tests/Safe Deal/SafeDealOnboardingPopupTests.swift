import XCTest
import Foundation

/// @depends_on AutoRuUserSaleCard AutoRuUserSaleList AutoRuSafeDeal
final class SafeDealOnboardingPopupTests: BaseTest {

    private let kShownDate = "safeDealOnboardingShownDate"
    private let kShowAfterActivation = "shouldShowOnboardingAfterOfferActivation"
    private let activeOfferId = "1113952136-02c53c64"
    private let inactiveOfferId = "1113842976-29696508"

    override func setUp() {
        super.setUp()

        setupServer()
    }

    func test_activateAndShowInProfile() {
        mockActivateOffer()

        showUserSaleList()
            .scroll(to: .offer(id: inactiveOfferId), maxSwipes: 3)
            .should(.draftSnippetActions(id: inactiveOfferId), ofType: .userOfferDraftActionsSnippetCell, .exist)
            .focus(on: .draftSnippetActions(id: inactiveOfferId), ofType: .userOfferDraftActionsSnippetCell) { snippet in
                snippet.tap(.activateButton)
            }
            .should(provider: .vasTrapScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .doneButton)
                    .tap(.doneButton)
            }
            .base
            .toggle(to: \.transport)
            .toggle(to: \.offers)
            .should(provider: .safeDealOnboardingPopup, .exist)
            .focus { $0.tap(.understand) }
    }

    func test_activateAndShowInOffer() {
        mockActivateOffer()

        showUserSaleList()
            .scroll(to: .offer(id: inactiveOfferId), maxSwipes: 3)
            .should(.draftSnippetActions(id: inactiveOfferId), ofType: .userOfferDraftActionsSnippetCell, .exist)
            .focus(on: .draftSnippetActions(id: inactiveOfferId), ofType: .userOfferDraftActionsSnippetCell) { snippet in
                snippet.tap(.activateButton)
            }
            .should(provider: .vasTrapScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .doneButton)
                    .tap(.doneButton)
            }
            .scroll(to: .offer(id: inactiveOfferId), maxSwipes: 3)
            .tap(.offer(id: inactiveOfferId))
            .should(provider: .userSaleCardScreen, .exist)
            .should(provider: .safeDealOnboardingPopup, .exist)
            .focus { popup in
                popup
                    .focus(on: .carousel) { carousel in
                        carousel
                            .swipe(.left)
                            .swipe(.right)
                    }
                    .tap(.details)
            }
            .should(provider: .webViewPicker, .exist)
    }

    func test_timoutFinishedShowInOffer() {
        mockActiveOffer(id: activeOfferId)

        showUserSaleList(userDefaults: [
            kShownDate: "2021-09-26T16:10:06+06:00",
            kShowAfterActivation: true
        ])
            .tap(.offer(id: activeOfferId))
            .should(provider: .userSaleCardScreen, .exist)
            .should(provider: .safeDealOnboardingPopup, .exist)
    }

    func test_hasTimeoutNotShowInOffer() {
        let dateFormatter = DateFormatter()
        dateFormatter.locale = Locale(identifier: "en_US_POSIX")
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZZZZZ"
        let dateString = dateFormatter.string(from: Date())

        mockActiveOffer(id: activeOfferId)

        showUserSaleList(userDefaults: [
            kShownDate: dateString,
            kShowAfterActivation: true
        ])
            .tap(.offer(id: activeOfferId))
            .should(provider: .userSaleCardScreen, .exist)
            .should(provider: .safeDealOnboardingPopup, .be(.hidden))
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .mock_userOffersAllWithActive()

        mocker.startMock()
    }

    private func mockActiveOffer(id: String) {
        mocker
            .mock_userOffer(id: id, hasActivationWithAutoprolongation: true)
    }

    private func mockActivateOffer() {
        mocker
            .mock_activateUserOfferFrom(category: "cars", id: inactiveOfferId)
            .mock_userOffer(id: inactiveOfferId, hasActivationWithAutoprolongation: true)
    }

    private func showUserSaleList(userDefaults: [String: Any] = [:]) -> UIElementProviderHost<MainScreen_, OffersSteps> {
        launchMain(options: .init(userDefaults: userDefaults)).toggle(to: \.offers)
    }
}
