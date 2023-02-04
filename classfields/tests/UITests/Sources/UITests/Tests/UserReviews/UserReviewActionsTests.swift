import XCTest
import Foundation

final class UserReviewActionsTests: BaseTest {
    let reviewId = "4943020671716068231"

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

    func testPublishUserReview() {
        mocker
            .mock_getUserReviews()
            .mock_getUserReview()
            .startMock()

        let requestPutWasCalled = api.reviews.subject(.auto).reviewId(reviewId).put.expect { request, _ in
            request.status == .enabled ? .ok : .fail(reason: "Отзыв не ушел на модерацию")
        }

        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink("https://auto.ru/my/reviews/")))
            .tap(.publishReviewButton)
            .wait(for: [requestPutWasCalled])
    }

    func testEditUserReview() {
        mocker
            .mock_getUserReviews()
            .mock_getUserReview()
            .startMock()

        let requestPutWasCalled = api.reviews.subject(.auto).reviewId(reviewId).put.expect { request, _ in
            request.title == "Хорошая машина тест" ? .ok : .fail(reason: "Заголовок не изменился")
        }

        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink("https://auto.ru/my/reviews/")))
            .tap(.editReviewButton)
            .should(provider: .userReviewEditorScreen, .exist)
            .focus { screen in
                screen
                    .focus(on: .userReviewContentEditorCell, ofType: .userReviewContentEditorCell) { editor in
                        editor.focus(on: .titleReview) { title in
                            title.tap()
                            title.type(" тест")
                        }
                    }
                    .tap(.saveButton)
            }
            .wait(for: [requestPutWasCalled])
    }

    func testDeleteUserReview() {
        mocker
            .mock_getUserReviews()
            .mock_deleteUserReview()
            .startMock()

        let requestGetWasCalled = api.user.reviews
            .get(parameters: .parameters([.page(1), .pageSize(20)])).expect()

        let requestDeleteWasCalled = api.reviews.subject(.auto).reviewId(reviewId)
            .delete.expect()

        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink("https://auto.ru/my/reviews/")))
            .tap(.deleteReviewButton)
            .should(provider: .userReviewAlert, .exist)
            .focus { alert in
                alert.tap(.yes)
            }
            .do {
                makeResponseAfterDeleting()
            }
            .should(provider: .userReviewsScreen, .exist)
            .should(.addReviewBigButton, .exist)
            .wait(for: [requestGetWasCalled, requestDeleteWasCalled])
    }

    private func makeResponseAfterDeleting() {
        mocker.mock_getEmptyUserReviews()
    }
}
