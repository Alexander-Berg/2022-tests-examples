import AutoRuProtoModels
import Foundation

final class ReviewsCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_cell() {
        Step("Отзывы") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

//    func test_empty() {
//        Step("Рейтинг модели (без оценок)") {
//            let layout = makeCellBuilder("ReviewsCell") { (params: inout JSRenderableBuilder.RenderTarget.CustomInjectionParams) in
//                params.enrichJson = EnrichJson(reviewsCount: 0).json
//            }.layout(mockedWith: .cardStub(name: "CardBoughtMileages"))
//            snapshot(name: "ReviewsCell", layout: layout, idSuffix: #function)
//        }
//    }

    func test_hasRating() {
        Step("Рейтинг модели (с оценками)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
