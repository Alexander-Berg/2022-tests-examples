import Foundation
import XCTest
import AutoRuYogaLayout
import AutoRuTableController
import Snapshots
import AutoRuFetchableImage
import AutoRuColorSchema
@testable import AutoRuCellHelpers
@testable import AutoRuVehicleReviews

final class ReviewCardAppearanceTests: BaseUnitTest {
    private let date = DateComponents(calendar: Calendar.russian,
                                      year: 1_995, month: 6, day: 7, hour: 9, minute: 10, second: 0).date!
    private let testImage = FetchableImage.testImage(withFixedSize: .init(squareSize: 56))

    func test_header() {
        let model = ReviewHeaderModel(title: "Kia Rio 5-speed III",
                                      subtitle: "5 speed 1.6 MT (123 л.с.)")

        Self.compareWithSnapshot(layout: ReviewHeaderLayout(model: model), id: "review_card_header")
    }

    func test_userInfo() {

        let model = ReviewAuthorModel(name: "NagiBator777",
                                      avatarImage: testImage,
                                      registrationDate: date)

        Self.compareWithSnapshot(layout: ReviewAuthorLayout(model: model), id: "review_card_user_info")
    }

    func test_carRating() {
        var ratingRowsModel = [RatingRowModel]()
        ratingRowsModel.append(RatingRowModel(title: "Безопасность", value: 3))
        ratingRowsModel.append(RatingRowModel(title: "Комфорт", value: 4))
        let model = RatingModel(value: 4.9, owningTime: "Владеет больше 5 лет", rows: ratingRowsModel)

        Self.compareWithSnapshot(layout: RatingLayout(model: model), id: "review_card_rating")
    }

    func test_prosCons() {
        let model = ProsConsModel(pros: ["Красота и грация"], cons: ["Кошмар и разочарование"])

        Self.compareWithSnapshot(layout: ProsConsLayout(model: model), id: "review_card_pros_cons")
    }

    func test_reviewComments() {
        let commonComment = CommentModel(userName: "Владимир", userImage: testImage,
                                 date: date, text: "Обычный отзыв", level: 0, enabled: true)

        let responseComment = CommentModel(userName: "Геннадий", userImage: testImage,
                                           date: date, text: "Ответ на отзыв", level: 1, enabled: true)

        let deletedComment = CommentModel(userName: "Петр", userImage: testImage,
                                          date: date, text: "Мой коммент заблочен", level: 1, enabled: false)

        Self.compareWithSnapshot(layout: CommentLayout(model: commonComment), id: "review_card_comment")
        Self.compareWithSnapshot(layout: CommentLayout(model: responseComment), id: "review_card_comment_response")
        Self.compareWithSnapshot(layout: CommentLayout(model: deletedComment), id: "review_card_comment_deleted")
    }

    private static func compareWithSnapshot(layout: LayoutConvertible, id: String) {
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: id
        )
    }
}
