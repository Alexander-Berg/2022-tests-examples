//
//  FiltersMetroDistancePickerSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 19.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YRETestsUtils
import struct YREAccessibilityIdentifiers.TableBasedPickerAccessibilityIdentifier

final class FiltersMetroDistancePickerSteps {
    enum DistanceType: Int {
        case byFoot
        case transport
    }

    @discardableResult
    func tapOnRow(_ value: String) -> Self {
        self.pickerView.yreEnsureExistsWithTimeout()

        let item = ElementsProvider.obtainElement(identifier: value, in: self.pickerView)
        item.yreEnsureExistsWithTimeout()

        self.pickerView.scrollToElement(element: item, direction: .up)

        item.yreEnsureHittable()
            .yreTap()

        return self
    }

    @discardableResult
    func switchTo(distanceType: DistanceType) -> Self {
        ElementsProvider
            .obtainElement(
                identifier: "FluidSegment_\(distanceType.rawValue)",
                in: self.pickerView
            )
            .yreEnsureExists(message: "Segment with index \(distanceType.rawValue) doesn't exist")
            .yreForceTap()

        return self
    }

    @discardableResult
    func isPickerPresented() -> Self {
        self.pickerView.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func isPickerClosed() -> Self {
        self.pickerView.yreEnsureNotExistsWithTimeout()
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        self.pickerView.yreEnsureExistsWithTimeout()

        let closeButton = ElementsProvider.obtainElement(identifier: Identifiers.closeButton, in: self.pickerView)
        closeButton
            .yreEnsureExistsWithTimeout()
            .yreEnsureHittable()
            .yreTap()

        return self
    }

    @discardableResult
    func tapOnApplyButton() -> Self {
        self.pickerView.yreEnsureExistsWithTimeout()

        let applyButton = ElementsProvider.obtainElement(identifier: Identifiers.applyButton, in: self.pickerView)
        applyButton
            .yreEnsureExistsWithTimeout()
            .yreEnsureHittable()
            .yreTap()

        return self
    }


    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        let screenshot = self.pickerView.yreWaitAndScreenshot()
        Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier, overallTolerance: Constants.overallToleranceForMap)
        return self
    }

    private enum Identifiers {
        static let identifierPrefix = "MetroDistanceSingleSelectPicker."
        static let pickerView = TableBasedPickerAccessibilityIdentifier.SingleSelectionPicker.view
        static let selectionImage = TableBasedPickerAccessibilityIdentifier.SingleSelectionPicker.cellSelectionImageView
        static let toolbar = TableBasedPickerAccessibilityIdentifier.SingleSelectionPicker.toolbar
        static let closeButton = TableBasedPickerAccessibilityIdentifier.SingleSelectionPicker.toolbarLeftItem
        static let applyButton = TableBasedPickerAccessibilityIdentifier.SingleSelectionPicker.toolbarRightItem
        static let segmentedControl = "MetroDistanceSingleSelectPicker.distanceSegmentControl"
    }

    private lazy var pickerView = ElementsProvider.obtainElement(identifier: Identifiers.pickerView)
    private lazy var segmentedControl = ElementsProvider.obtainElement(identifier: Identifiers.segmentedControl)
}
