//
//  PersonalizationAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 3/9/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import Swifter

final class PersonalizationAPIStubConfigurator {
    static func setupUpsertNote(using dynamicStubs: HTTPDynamicStubs, offerID: String = "157352957988240017") {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.upsertNote(offerID: offerID),
            filename: Stubs.upsertNote
        )
    }

    static func setupDeleteNote(using dynamicStubs: HTTPDynamicStubs, offerID: String = "157352957988240017") {
        dynamicStubs.register(
            method: .DELETE,
            path: Paths.deleteNote(offerID: offerID),
            filename: Stubs.deleteNote
        )
    }

    static func setupHideOffer(using dynamicStubs: HTTPDynamicStubs, offerID: String = "157352957988240017") {
        dynamicStubs.register(
            method: .DELETE,
            path: Paths.hideOffer(offerID: offerID),
            filename: Stubs.hideOffer
        )
    }

    // MARK: - Private

    private enum Paths {
        static func upsertNote(offerID: String) -> String { "/2.0/user/me/personalization/offer/\(offerID)/note" }
        static func deleteNote(offerID: String) -> String { "/2.0/user/me/personalization/offer/\(offerID)/note" }
        static func hideOffer(offerID: String) -> String { "/1.0/user/me/personalization/hideOffers" }
    }

    private enum Stubs {
        static let upsertNote = "commonEmpty.debug"
        static let deleteNote = "commonEmpty.debug"
        static let hideOffer = "commonEmptyWithoutResponseObject.debug"
    }
}
