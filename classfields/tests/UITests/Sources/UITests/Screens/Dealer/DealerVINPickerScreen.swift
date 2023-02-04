import XCTest
import Snapshots

final class DealerVINPickerScreen: BaseScreen {
    lazy var input = find(by: "app.pickers.vin").firstMatch
    lazy var skipButton = find(by: "Пропустить").firstMatch
}
