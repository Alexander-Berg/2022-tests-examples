import XCTest
import Foundation

final class ReviewCardTests: BaseTest {
    private let deeplink = "https://auto.ru/review/cars/volkswagen/passat/3493011/5034086768384195311/"
    private let reviewId = "5034086768384195311"
    override func setUp() {
        super.setUp()
        setupServer()
    }

    override func tearDown() {
        super.tearDown()
        mocker.stopMock()
    }

    private func setupServer() {
        mocker
            .mock_base()
            .mock_reviewCard(reviewId)
            .mock_reviewComments(reviewId)
            .mock_catalogSpecifications()
    }

    func test_reviewCardButtonsActions() {
        mocker
            .startMock()
        
        launch(on: .reviewCardSreen,
               options: AppLaunchOptions(launchType: .deeplink(deeplink)))
            .should(provider: .reviewCardSreen, .exist)
            .tap(.specificationsButton)
            .should(provider: .specificationsScreen, .exist)
            .should(provider: .navBar, .exist)
            .focus { $0.tap(.back) }

            .tap(.offersButton)
            .should(provider: .saleListScreen, .exist)
            .should(provider: .navBar, .exist)
            .focus { $0.tap(.back) }
        
            .scroll(to: .commentsButton)
            .tap(.commentsButton)
            .should(provider: .reviewCommentsScreen, .exist)
    }
}
