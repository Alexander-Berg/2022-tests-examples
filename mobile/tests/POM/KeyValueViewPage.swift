import XCTest

final class KeyValueViewPage: PageObject {

    // MARK: - Properties

    var key: String {
        element.label
    }

    var value: String? {
        element.value as? String
    }
}
