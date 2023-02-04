import XCTest
import Snapshots

final class GaragePhotoGridScreen: BaseScreen, NavigationControllerContent {
    func deleteButton(forPhoto photoID: String) -> XCUIElement {
        find(by: "delete \(photoID)").firstMatch
    }

    func photo(withID id: String) -> XCUIElement {
        find(by: "photo \(id)").firstMatch
    }

    func photo(byIndex index: Int) -> XCUIElement {
        app.descendants(matching: .any).withIdentifierPrefix("photo ").element(boundBy: index)
    }
}
