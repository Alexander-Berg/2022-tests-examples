//
//  GetBestPriceScreen.swift
//  UITests
//
//  Created by Roman Bevza on 10/16/20.
//
import XCTest
import Snapshots

final class GetBestPriceScreen: BaseScreen, Scrollable {
    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }

    var sendButton: XCUIElement {
        return find(by: "Жду предложений").firstMatch
    }

    var pickMarkButton: XCUIElement {
        return find(by: "params_auto").firstMatch
    }

    var succesHUD: XCUIElement {
        return find(by: "Заявка отправлена!").firstMatch
    }

}
