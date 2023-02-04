//
//  CarReprotsListScreen.swift
//  AutoRu
//
//  Created by Sergey An. Sergeev on 30.01.2021.
//

import XCTest
import Snapshots

class CarReportsListScreen: BaseScreen, Scrollable {
    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }

    lazy var searchField = find(by: "Госномер или VIN").firstMatch
    lazy var buyReportsBundleButton = find(by: "10 отчётов за 990 ₽").firstMatch

    func reportSnippet(withVin vin: String) -> XCUIElement {
        let predicate = NSPredicate(format: "label MATCHES[cd] %@", vin)
        return find(by: "report_body_row_\(vin)").descendants(matching: .textView).matching(predicate).firstMatch
    }
}
