//
//  History.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 03.06.2021.
//

import Foundation

extension Mocker {
    @discardableResult
    func mock_offerFromHistoryLastAll() -> Self {
        server.api.offer.category(.cars).offerID("1098252972-99d8c274").get.ok(mock: .file("offer_CARS_1098252972-99d8c274_ok"))
        return self
    }
}
