//
//  UserProfileScreen.swift
//  AutoRu
//
//  Created by Vitalii Stikhurov on 02.03.2020.
//

import XCTest
import Snapshots

class OfferEditScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    lazy var premiumOfferAssistantInactiveBanner = find(by: "SaleSnippetPremiumAssistantInactiveBanner").firstMatch

    lazy var noActivationButton = find(by: "Не публиковать сразу").firstMatch

    enum FormElement: String {
        case precontrolsSeparator = "precontrols_separator"
        case vipVAS = "VIPVAS"
        case turboVAS = "turboVAS"
        case activation = "activation"
        case publish = "publish"
        case publishDescription = "publish_description"
    }

     func cellFor(_ element: FormElement) -> XCUIElement {
         return XCUIApplication.make().collectionViews.cells.containing(.any, identifier: element.rawValue).firstMatch
     }
}
