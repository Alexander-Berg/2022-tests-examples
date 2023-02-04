import AutoRuProtoModels
import Foundation

final class VehiclePhotosCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_hasPhotos() {
        Step("Фото автолюбителей") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_emptyUpdating() {
        Step("Фото автолюбителей (пустой, обновляется)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
