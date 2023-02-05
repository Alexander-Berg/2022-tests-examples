//
//  ApplyLabelApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 02.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class ApplyLabelApplication: ApplyLabel {
    private let applyLabelPage = ApplyLabelPage()
    
    public func selectLabelsToAdd(_ labelNames: YSArray<LabelName>) throws {
        try XCTContext.runActivity(named: "Selecting labels \(labelNames) to add") { _ in
            for labelName in labelNames {
                try self.tapOnLabelToAdd(labelName)
            }
        }
    }
    
    public func deselectLabelsToRemove(_ labelNames: YSArray<LabelName>) throws {
        try XCTContext.runActivity(named: "Deselecting labels \(labelNames) to remove") { _ in
            for labelName in labelNames {
                try self.tapOnLabelToRemove(labelName)
            }
        }
    }
    
    public func tapOnDoneButton() throws {
        try XCTContext.runActivity(named: "Tap on Done button") { _ in
            try self.applyLabelPage.doneButton.tapCarefully()
        }
    }
    
    public func tapOnCreateLabel() throws {
        try XCTContext.runActivity(named: "Tap on FAB") { _ in
            try self.applyLabelPage.fab.tapCarefully()
        }
    }
    
    public func getSelectedLabels() throws -> YSArray<LabelName> {
        XCTContext.runActivity(named: "Getting selected labels") { _ in
            return YSArray(array: self.applyLabelPage.selectedLabelsName)
        }
    }
    
    public func getLabelList() throws -> YSArray<LabelName> {
        XCTContext.runActivity(named: "Getting label list") { _ in
            return YSArray(array: self.applyLabelPage.labelsNameList)
        }
    }
    
    private func tapOnLabelToAdd(_ label: LabelName) throws {
        guard let labelToTap = self.applyLabelPage.findFirstLabelByName(label) else {
            throw YSError("Unable to find label with name \"\(label)\"")
        }
        try labelToTap.tapCarefully()
    }

    private func tapOnLabelToRemove(_ label: LabelName) throws {
        guard let labelToTap = self.applyLabelPage.findLastLabelByName(label) else {
            throw YSError("Unable to find label with name \"\(label)\"")
        }
        try labelToTap.tapCarefully()
    }
}
