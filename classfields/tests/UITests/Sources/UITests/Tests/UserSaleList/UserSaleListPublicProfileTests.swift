import XCTest

final class UserSaleListPublicProfileTests: BaseTest {
    private let encryptedUserID = "SxKXJ-yVbqA-_neifxQC_A"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_becomeResellerUsingPromoBanner() {
        let successExpectation = api.user.profile
            .post
            .expect { request, _ in
                .okIf(request.allowOffersShow == true)
            }

        showUserSaleList()
            .should(provider: .publicProfileOnboardingPopup, .exist)
            .focus { $0.tap(.laterButton) }
            .should(provider: .publicProfileOnboardingPopup, .be(.hidden))
            .should(provider: .userSaleListScreen, .exist)
            .focus {
                $0.focus(on: .publicProfileTooltipPopup, ofType: .publicProfileTooltipPopup) {
                    $0.tap(.settingsButton)
                }
            }
            .should(provider: .userProfileScreen, .exist)
            .should(provider: .navBar, .exist)
            .focus { $0.tap(.close) }
            .wait(for: 1)
            .should(provider: .userSaleListScreen, .exist)
            .focus { $0.tap(.publicProfilePromoBanner) }
            .should(provider: .publicProfileOnboardingPopup, .exist)
            .focus { $0.tap(.confirmButton) }
            .wait(for: [successExpectation])
            .should(provider: .publicProfileOnboardingPopup, .be(.hidden))
            .should(provider: .publicProfileSuccessPopup, .exist)
            .focus { $0.tap(.publicProfileButton) }
            .should(provider: .publicProfileScreen, .exist)
            .wait(for: 1)
    }

    func test_closePromoBanner() {
        showUserSaleList()
            .should(provider: .publicProfileOnboardingPopup, .exist)
            .focus { $0.tap(.laterButton) }
            .should(provider: .publicProfileOnboardingPopup, .be(.hidden))
            .should(provider: .userSaleListScreen, .exist)
            .focus {
                $0.focus(on: .publicProfilePromoBanner, ofType: .publicProfilePromoBanner) {
                    $0.tap(.closeButton)
                }
            }
            .should(.publicProfilePromoBanner, .be(.hidden))
    }

    private func showUserSaleList() -> UserSaleListScreen {
        launchMain() { screen in
            screen
                .toggle(to: \.offers)
                .should(provider: .userSaleListScreen, .exist)
        }
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
