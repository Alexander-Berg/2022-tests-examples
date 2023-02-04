import AutoRuProtoModels
import Snapshots
@testable import AutoRuGarageCard

final class GarageAddVINCellModentLayoutTests: BaseUnitTest {
    func test_ModernLayoutCell() {
        let layout = GarageAddVINCell() { }
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
