import XCTest

final class UserProfilePublicProfileTests: BaseTest {
    private let encryptedUserID = "SxKXJ-yVbqA-_neifxQC_A"
    private let onboardingShown = "PublicProfileOnboardingWasShown"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_becomeResellerUsingSwitch() {
        let successExpectation = api.user.profile
            .post
            .expect { request, _ in
                .okIf(request.allowOffersShow.value)
            }

        launchMain(options: .init(userDefaults: [onboardingShown: true])) { screen in
            screen
                .toggle(to: \.offers)
                .should(provider: .userSaleListScreen, .exist)
        }
        .tap(.profileButton)
        .do {
            mocker.mock_reseller(encryptedUserID: encryptedUserID, allowOffersShow: true)
        }
        .should(provider: .userProfileScreen, .exist)
        .focus { screen in
            screen.focus(on: .publicProfileSwitchSnippet, ofType: .publicProfileSwitchSnippet) { snippet in
                snippet
                    .tap(.switch)
                    .wait(for: [successExpectation])
                    .tap(.link)
            }
        }
        .should(provider: .publicProfileScreen, .exist)
    }

    private func setupServer() {
        mocker
            .mock_base()
            .mock_reseller(encryptedUserID: encryptedUserID)
            .mock_userUpdate { response in
                response.allowOffersShow.value = true
            }
            .setForceLoginMode(.forceLoggedIn)
            .mock_userOffersAllWithActive()
            .mock_publicProfileUser(encryptedUserID: encryptedUserID)
            .mock_publicProfileList(encryptedUserID: encryptedUserID)

        mocker.startMock()
    }
}
