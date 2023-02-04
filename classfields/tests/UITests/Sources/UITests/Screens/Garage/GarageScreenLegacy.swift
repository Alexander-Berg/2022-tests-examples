import Foundation
import XCTest
import Snapshots

final class GarageScreen: BaseScreen, Scrollable {
    lazy var profileButton = find(by: "user_profile_button").firstMatch
    lazy var walletButton = find(by: "wallet_button").firstMatch
    lazy var scrollableElement: XCUIElement = findAll(.collectionView).firstMatch
    lazy var garagePromo = find(by: "garage_promo").firstMatch
    lazy var garageAddCarButton = find(by: "add_car").firstMatch
    lazy var garageBannerAddCarButton = find(by: "Поставить").firstMatch

    func garageCar(id: String) -> XCUIElement { find(by: "car_\(id)").firstMatch }

    func item(_ item: String) -> XCUIElement {
        return findAll(.staticText)[item]
    }
}

final class GarageLandingScreen: BaseScreen, Scrollable {
    lazy var garageAddCarButton = find(by: "Поставить в Гараж").firstMatch
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    func item(_ item: String) -> XCUIElement {
        find(by: "garage_landing").staticTexts[item]
    }
}
