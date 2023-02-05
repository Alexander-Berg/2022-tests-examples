//
//  ManageLabelsApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 01.06.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class ManageLabelsApplication: ManageableLabel {
    private let messageListPage = MessageListPage()
    private let foldersListPage = FoldersListPage()
    private let manageLabelsPage = ManageLabelsPage()
    private let addLabelPage = AddLabelPage()
    private let editLabelPage = EditLabelPage()
    
    public func openLabelManager() throws {
        try XCTContext.runActivity(named: "Opening label manager") { _ in
            try scrollIfNeeded(view: self.foldersListPage.view, to: self.foldersListPage.unread)
            try self.foldersListPage.manageLabels.forceTap()
            self.manageLabelsPage.tableView.yo_waitForExistence()
        }
    }
    
    public func closeLabelManager() throws {
        try XCTContext.runActivity(named: "Closing label manager") { _ in
            try self.manageLabelsPage.closeButton.tapCarefully()
            self.foldersListPage.view.yo_waitForExistence()
        }
    }
    
    public func deleteLabel(_ labelDisplayName: LabelName, _ deletionMethod: ContainerDeletionMethod) throws {
        try XCTContext.runActivity(named: "Deleting label \(labelDisplayName)") { _ in
            let label = self.manageLabelsPage.findLabel(withName: labelDisplayName)
            
            switch deletionMethod {
            case .longSwipe:
                try label.longSwipe(.left)
            case .shortSwipe:
                try label.swipeLeft()
                try label.deleteRightButton.tapCarefully()
            case .tap:
                try self.manageLabelsPage.editButton.tapCarefully()
                try label.deleteLeftButton.tapCarefully()
                try label.deleteRightButton.tapCarefully()
            }
            
            try self.manageLabelsPage.alertOKButton.tapCarefully()
            
            if self.manageLabelsPage.doneButton.exists {
                try self.manageLabelsPage.doneButton.tapCarefully()
            }
        }
    }
    
    public func isLabelManagerOpened() -> Bool {
        XCTContext.runActivity(named: "Checking if label manager is opened") { _ in
            return self.manageLabelsPage.tableView.yo_waitForExistence()
        }
    }
    
    public func getLabelList() -> YSArray<LabelName> {
        XCTContext.runActivity(named: "Getting label list") { _ in
            return YSArray(array: self.manageLabelsPage.labelNameList.map { labelName in LabelName(labelName) })
        }
    }
    
    public func openCreateLabelScreen() throws {
        try XCTContext.runActivity(named: "Opening create label screen") { _ in
            try self.manageLabelsPage.addButton.tapCarefully()
            self.addLabelPage.formView.yo_waitForExistence()
        }
    }
    
    public func closeCreateLabelScreen() throws {
        try XCTContext.runActivity(named: "Closing create label screen") { _ in
            try self.addLabelPage.closeButton.tapCarefully()
            self.manageLabelsPage.tableView.yo_waitForExistence()
        }
    }
    
    public func enterNameForNewLabel(_ labelName: LabelName) {
        XCTContext.runActivity(named: "Entering name for new label - \(labelName)") { _ in
            self.addLabelPage.formTextField.typeText(labelName)
        }
    }
    
    public func getCurrentNewLabelName() -> LabelName {
        XCTContext.runActivity(named: "Getting current label name") { _ in
            return LabelName(self.addLabelPage.formTextField.value as! String)
        }
    }
    
    public func setNewLabelColor(_ index: Int32) throws {
        try XCTContext.runActivity(named: "Setting color with index \(index)") { _ in
            if !self.addLabelPage.colorBy(index: Int(index)).isSelected {
                try self.addLabelPage.colorBy(index: Int(index)).tapCarefully()
            } else {
                YOXCTLogMessage("Color with index \(index) already selected")
            }
        }
    }
    
    public func getCurrentNewLabelColorIndex() -> Int32 {
        XCTContext.runActivity(named: "Getting color index") { _ in
            let colors = self.addLabelPage.colors
            return Int32(colors.firstIndex(where: { $0.isSelected })!)
        }
    }
    
    public func submitNewLabel() throws {
        try XCTContext.runActivity(named: "Submitting new label") { _ in
            try self.addLabelPage.doneButton.tapCarefully()
            self.manageLabelsPage.tableView.yo_waitForExistence()
        }
    }
    
    public func openEditLabelScreen(_ labelName: LabelName) throws {
        try XCTContext.runActivity(named: "Opening edit screen for label \(labelName)") { _ in
            try self.manageLabelsPage.findLabel(withName: labelName).tap()
            self.editLabelPage.formView.yo_waitForExistence()
        }
    }
    
    public func closeEditLabelScreen() throws {
        try XCTContext.runActivity(named: "Closing edit screen") { _ in
            try self.editLabelPage.backButton.tapCarefully()
            self.manageLabelsPage.tableView.yo_waitForExistence()
        }
    }
    
    public func enterNameForEditedLabel(_ labelName: LabelName) {
        XCTContext.runActivity(named: "Entering new label name - \(labelName)") { _ in
            self.editLabelPage.formTextField.clearField()
            self.editLabelPage.formTextField.typeText(labelName)
        }
    }
    
    public func getCurrentEditedLabelName() -> LabelName {
        XCTContext.runActivity(named: "Getting current label name") { _ in
            return LabelName(self.editLabelPage.formTextField.value as! String)
        }
    }
    
    public func getCurrentEditedLabelColorIndex() -> Int32 {
        XCTContext.runActivity(named: "Getting current label color index") { _ in
            let colors = self.editLabelPage.colors
            return Int32(colors.firstIndex(where: { $0.isSelected })!)
        }
    }
    
    public func setEditedLabelColor(_ index: Int32) throws {
        try XCTContext.runActivity(named: "Setting new color with index \(index)") { _ in
            if !self.editLabelPage.colorBy(index: Int(index)).isSelected {
                try self.editLabelPage.colorBy(index: Int(index)).tapCarefully()
            } else {
                YOXCTLogMessage("Color with index \(index) already selected")
            }
        }
    }
    
    public func submitEditedLabel() throws {
        try XCTContext.runActivity(named: "Applying label changes") { _ in
            try self.editLabelPage.doneButton.tapCarefully()
            self.manageLabelsPage.tableView.yo_waitForExistence()
        }
    }
}
