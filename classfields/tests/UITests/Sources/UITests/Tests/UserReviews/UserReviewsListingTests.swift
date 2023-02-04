import XCTest
import AutoRuProtoModels

final class UserReviewsListingTests: BaseTest {
    private let deeplink = "https://auto.ru/my/reviews/"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    private func setupServer() {
        mocker
            .mock_base()
            .mock_user()
            .setForceLoginMode(.forceLoggedIn)
    }

    func testBigAddButton() {
        let requestPostWasCalled = api.reviews.subject(.auto).post.expect { request, _ in
            request.item.auto.category == .cars ? .ok : .fail(reason: "В запросе отсутствует категория")
        }

        mocker
            .mock_getEmptyUserReviews()
            .mock_makeUserReview()
            .mock_wizardReferenceCatalogCarsSuggest()
            .startMock()
        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink(deeplink)))
            .tap(.addReviewBigButton)
            .should(provider: .categoryPicker, .exist)
            .focus { picker in
                picker.tap(.auto)
            }
            .should(provider: .wizardMarkPicker, .exist)
            .wait(for: [requestPostWasCalled])
    }

    func testAddButtonInNavBar() {
        let requestPostWasCalled = api.reviews.subject(.auto).post.expect { request, _ in
            request.item.auto.category == .cars ? .ok : .fail(reason: "В запросе отсутствует категория")
        }

        mocker
            .mock_getUserReviews()
            .mock_makeUserReview()
            .mock_wizardReferenceCatalogCarsSuggest()
            .startMock()
        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink(deeplink)))
            .tap(.addReviewButton)
            .should(provider: .categoryPicker, .exist)
            .focus { picker in
                picker.tap(.auto)
            }
            .should(provider: .wizardMarkPicker, .exist)
            .wait(for: [requestPostWasCalled])
    }
}
