//
//  OfferIdentifier.swift
//  UITests
//
//  Created by Dmitry Sinev on 12/1/21.
//

import XCTest

enum OfferIdentifier {
    case alias(_ offer: BackendState.Offer)
    case custom(_ offerID: String)

    var id: String {
        switch self {
        case .alias(let offer):
            return offer.rawValue
        case .custom(let value):
            return value
        }
    }
}
