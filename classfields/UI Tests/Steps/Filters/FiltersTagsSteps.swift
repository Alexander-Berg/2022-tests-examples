//
//  FiltersTagsSteps.swift
//  UI Tests
//
//  Created by Fedor Solovev on 16.09.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import YREAccessibilityIdentifiers

final class FiltersTagsSteps {
    init(cellIdentifier: String) {
        self.cellIdentifier = cellIdentifier
    }

    @discardableResult
    func tapOnAddMoreButton() -> Self {
        self.addMoreButton
            .yreEnsureExistsWithTimeout()
            .yreEnsureHittable()
            .yreTap()

        return self
    }

    @discardableResult
    func tapOnResetButton() -> Self {
        self.resetButton
            .yreEnsureExistsWithTimeout()
            .yreEnsureHittable()
            .yreTap()

        return self
    }

    @discardableResult
    func resetButtonExists() -> Self {
        self.resetButton
            .yreEnsureExists()
        return self
    }

    @discardableResult
    func resetButtonNotExists() -> Self {
        self.resetButton
            .yreEnsureNotExists()
        return self
    }

    @discardableResult
    func ensureBubbleExists(title: String) -> Self {
        let identifier = Identifiers.tagsListBubbleViewIdentifierPrefix + title
        let bubble = ElementsProvider.obtainElement(identifier: identifier, in: self.tagsCell)
        bubble.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func ensureBubbleNotExists(title: String) -> Self {
        let identifier = Identifiers.tagsListBubbleViewIdentifierPrefix + title
        let bubble = ElementsProvider.obtainElement(identifier: identifier, in: self.tagsCell)
        bubble.yreEnsureNotExists()
        return self
    }

    @discardableResult
    func removeBubble(title: String) -> Self {
        let identifier = Identifiers.tagsListBubbleViewIdentifierPrefix + title

        let bubble = ElementsProvider.obtainElement(identifier: identifier, in: self.tagsCell)
        bubble
            .yreEnsureExistsWithTimeout()
            .yreEnsureHittable()
            .yreTap()

        return self
    }

    private let cellIdentifier: String

    private lazy var tagsCell = ElementsProvider.obtainElement(identifier: self.cellIdentifier)
    private lazy var addMoreButton = ElementsProvider.obtainElement(
        identifier: Identifiers.addMoreButtonIdentifier,
        type: .button,
        in: self.tagsCell
    )
    private lazy var resetButton = ElementsProvider.obtainElement(
        identifier: Identifiers.resetButtonIdentifier,
        type: .button,
        in: self.tagsCell
    )

    private typealias Identifiers = FiltersTagsAccessibilityIdentifiers
}
