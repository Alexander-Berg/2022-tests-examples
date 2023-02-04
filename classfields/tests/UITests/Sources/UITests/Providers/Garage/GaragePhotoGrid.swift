import XCTest

typealias GaragePhotoGridScreen_ = GaragePhotoGridSteps

extension GaragePhotoGridScreen_: UIRootedElementProvider {
    static var rootElementID = "garage_photo_grid"
    static var rootElementName = "Гараж. Грид с фотографиями"

    enum Element: String {
        case addPhotos = "Добавить"
    }
}
