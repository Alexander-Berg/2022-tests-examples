//
//  FiltersApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 14.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class FiltersApplication: FiltersList, FilterCreateOrUpdateRule, FilterConditionLogic, FilterUpdateRuleMore {
    private let filterListPage = FilterListPage()
    private let filterEditUpdatePage = FilterEditUpdatePage()
    
    public func isPromoShown() throws -> Bool {
        XCTContext.runActivity(named: "Checking is promo shown") { _ in
            return (self.filterListPage.promoLogoImage.exists &&
                    self.filterListPage.promoTitleLabel.exists &&
                    self.filterListPage.promoTextLabel.exists &&
                    self.filterListPage.promoView.exists)
        }
    }
    
    public func getFilterList() throws -> YSArray<FilterView> {
        XCTContext.runActivity(named: "Getting filter list") { _ in
            // TODO
            // self.filterListPage.filterList
            return YSArray(array: [])
        }
    }
    
    public func tapOnCreateRuleButton() throws {
        try XCTContext.runActivity(named: "Tap on Create rule button") { _ in
            try self.filterListPage.createRuleButton.tapCarefully()
        }
    }
    
    public func tapOnFilterByIndex(_ index: Int32) throws {
        try XCTContext.runActivity(named: "Tap on filter with index \(index)") { _ in
            try self.filterListPage.filterElementsList[Int(index)].tapCarefully()
        }
    }
    
    public func tapOnConditionField(_ conditionField: FilterConditionField) throws {
        try XCTContext.runActivity(named: "Tap on field \(conditionField)") { _ in
            switch conditionField {
            case .from:
                try self.filterEditUpdatePage.senderTextField.tapCarefully()
            case .subject:
                try self.filterEditUpdatePage.subjectTextField.tapCarefully()
            }
        }
    }
    
    public func setConditionField(_ conditionField: FilterConditionField, _ value: String) throws {
        XCTContext.runActivity(named: "Set \(value) value to \(conditionField) field") { _ in
            switch conditionField {
            case .from:
                self.filterEditUpdatePage.senderTextField.typeText(value)
            case .subject:
                self.filterEditUpdatePage.subjectTextField.typeText(value)
            }
            self.filterEditUpdatePage.subjectTextField.tapOnReturnButton()
        }
    }
    
    public func getConditionField(_ conditionField: FilterConditionField) throws -> YSArray<String> {
        XCTContext.runActivity(named: "Getting \(conditionField) field value") { _ in
            // TODO
            return YSArray(array: [])
        }
    }
    
    public func isConditionLogicButtonShown() throws -> Bool {
        XCTContext.runActivity(named: "Checking is condition logic button shown") { _ in
            // TODO
            return true
        }
    }
    
    public func tapOnConditionLogicButton() throws {
        try XCTContext.runActivity(named: "Tap on condition logic button") { _ in
            try self.filterEditUpdatePage.selectLogicButton.tapCarefully()
        }
    }
    
    public func getConditionLogic() throws -> FilterLogicType! {
        XCTContext.runActivity(named: "Getting condition logic") { _ in
            // TODO
            return FilterLogicType.or
        }
    }
    
    public func getActionToggle(_ actionToggle: FilterActionToggle) throws -> Bool {
        XCTContext.runActivity(named: "Getting \(actionToggle) toggle enable state") { _ in
            // TODO
            return true
        }
    }
    
    public func setActionToggle(_ actionToggle: FilterActionToggle, _ value: Bool) throws {
        XCTContext.runActivity(named: "Set \(value) value to \(actionToggle) toggle") { _ in
            // TODO
        }
    }
    
    public func getMoveToFolderValue() throws -> FolderName! {
        XCTContext.runActivity(named: "Getting name of folder to move") { _ in
            // TODO
            return ""
        }
    }
    
    public func tapOnMoveToFolder() throws {
        try XCTContext.runActivity(named: "Tap on Move to folder") { _ in
            try self.filterEditUpdatePage.moveToFolderView.tapCarefully()
        }
    }
    
    public func getApplyLabelValue() throws -> LabelName! {
        XCTContext.runActivity(named: "Getting name of label to apply") { _ in
            // TODO
            return ""
        }
    }
    
    public func tapOnApplyLabel() throws {
        try XCTContext.runActivity(named: "Tap on Apply label") { _ in
            try self.filterEditUpdatePage.applyLabelView.tapCarefully()
        }
    }
    
    public func tapOnCreate() throws {
        try XCTContext.runActivity(named: "Tap on Create button") { _ in
            try self.filterEditUpdatePage.createRuleButton.tapCarefully()
        }
    }
    
    public func tapOnMore() throws {
        XCTContext.runActivity(named: "Tap on More button") { _ in
            // TODO
        }
    }
    
    public func getLogicTypes() throws -> YSArray<FilterLogicType> {
        XCTContext.runActivity(named: "Getting logic type") { _ in
            // TODO
            return YSArray(array: [])
        }
    }
    
    public func setLogicType(_ logicType: FilterLogicType) throws {
        XCTContext.runActivity(named: "Set \(logicType) logic type") { _ in
            // TODO
        }
    }
    
    public func changeEnableStatus(_ enable: Bool) throws {
        XCTContext.runActivity(named: "Change rule enable status to \(enable)") { _ in
            // TODO
        }
    }
    
    public func delete() throws {
        XCTContext.runActivity(named: "Delete rule") { _ in
            // TODO
        }
    }
}
