import AutoRuProtoModels
import Foundation

final class FeaturesCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_cell() {
        Step("Рейтинг модели") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

//    func test_empty() {
//        Step("Рейтинг модели (без оценок)") {
//            let layout = makeCellBuilder("FeaturesCell") { (params: inout JSRenderableBuilder.RenderTarget.CustomInjectionParams) in
//                params.enrichJson = EnrichJson(reviewsRating: 0).json
//            }.model { (model: inout Auto_Api_RawVinReportResponse) in
//                model.report.carInfo.markInfo.code = "MERCEDES"
//                model.report.carInfo.modelInfo.code = "GL_KLASSE"
//                model.report.carInfo.superGen.id = 8_225_665
//            }.layout(mockedWith: .none)
//            snapshot(name: "FeaturesCell", layout: layout, idSuffix: #function)
//        }
//    }

    func test_hasRating() {
        Step("Рейтинг модели (с оценками)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
