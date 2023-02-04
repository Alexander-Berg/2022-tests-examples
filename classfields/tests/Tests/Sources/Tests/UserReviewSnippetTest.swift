import Foundation
import XCTest
import AutoRuAppConfig
import AutoRuAppearance
import AutoRuProtoModels
import AutoRuYogaLayout
import Snapshots
@testable import AutoRuMyReviews
import AutoRuColorSchema

final class UserReviewSnippetTest: BaseUnitTest {

    override func setUp() {
        super.setUp()

        setReplaceImagesWithStub()
    }
    
    func testDraftSnippet() throws {
        Step("Отзыв в драфте")
        let userReview = makeUserReview(status: .draft, moderation: .new)
        let model = try XCTUnwrap(makeViewState(model: userReview),
                                  "ViewState shouldn't be nil")
        let layout = MyReviewSnippetLayoutProvider(model: model).makeLayout()

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func testOnModerationSnippet() throws {
        Step("Отзыв на модерации")
        let userReview = makeUserReview(status: .enabled, moderation: .inProgress)
        let model = try XCTUnwrap(makeViewState(model: userReview),
                                  "ViewState shouldn't be nil")
        let layout = MyReviewSnippetLayoutProvider(model: model).makeLayout()

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func testBannedSnippet() throws {
        Step("Отзыв заблокирован")
        let userReview = makeUserReview(status: .disabled, isBanned: true)
        let model = try XCTUnwrap(makeViewState(model: userReview),
                                  "ViewState shouldn't be nil")
        let layout = MyReviewSnippetLayoutProvider(model: model).makeLayout()

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func testBannedEditableSnippet() throws {
        Step("Отзыв заблокирован, с возможностью редактирования")
        let userReview = makeUserReview(status: .draft, moderation: .declined, isBanned: true)
        let model = try XCTUnwrap(makeViewState(model: userReview),
                                  "ViewState shouldn't be nil")
        let layout = MyReviewSnippetLayoutProvider(model: model).makeLayout()

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func testPublishedSnippet() throws {
        Step("Отзыв опубликован")
        let userReview = makeUserReview(status: .enabled, moderation: .accepted)
        let model = try XCTUnwrap(makeViewState(model: userReview),
                                  "ViewState shouldn't be nil")
        let layout = MyReviewSnippetLayoutProvider(model: model).makeLayout()

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    private func makeViewState(model: Auto_Api_Review) -> MyReviewSnippetViewState? {
        let actions = ViewStateTransformer.Actions()
        let model = ViewStateTransformer.makeViewStateFromAPI(
            model: model,
            actions: actions,
            addTopSpace: false)
        return model
    }

    private func makeUserReview(status: Auto_Api_Review.Status,
                                moderation: Auto_Api_Review.Moderation.Status? = nil,
                                isBanned: Bool = false) -> Auto_Api_Review {
        let response = makeReviewsResponse()
        var firstReview = response.reviews[0]
        firstReview.status = status
        if let moderation = moderation {
            firstReview.moderationHistory[0].status = moderation
        }
        if isBanned {
            let reasonBan: Auto_Api_BanReason = .with {
                $0.title = "Отзыв заблокирован"
                $0.textApp = "Мы посчитали его содержание неуместным для нашего сервиса"
            }
            firstReview.moderationHistory[0].humanReasonsBan.append(reasonBan)
        }
        if moderation == .accepted {
            firstReview.countComments = 23
            firstReview.viewsCount = 56
        }
        return firstReview
    }

    private func makeReviewsResponse(_ fileName: String = "user_reviews") -> Auto_Api_ReviewListingResponse {
        let url = Bundle.current.url(forResource: fileName, withExtension: "json")
        let response = try! Auto_Api_ReviewListingResponse(jsonUTF8Data: Data(contentsOf: url!))
        XCTAssertNotNil(url, "File \(fileName).json doesn't exists in the bundle")
        return response
    }
}
