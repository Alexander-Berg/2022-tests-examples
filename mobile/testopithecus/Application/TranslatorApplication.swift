//
//  TranslatorApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 01.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class TranslatorApplication: TranslatorBar,
                                          TranslatorLanguageList,
                                          TranslatorLanguageListSearch,
                                          TranslatorSettings {
    private let generalSettingsPage = GeneralSettingsPage()
    private let messageViewPage = MessageViewPage()
    private let messageActionsPage = MessageActionsPage()
    private let sourceLanguagePage = SourceLanguagePage()
    private let targetLanguagePage = TargetLanguagePage()
    
    private var languageListType: LanguageListType?
    
    private enum LanguageListType {
        case sourceLanguageList
        case targetLanguageList
    }

    // MARK: TranslatorBar
    public func isTranslatorBarShown() throws -> Bool {
        XCTContext.runActivity(named: "Check is translator bar shown") { _ in
            return self.messageViewPage.submitButton.exists
            && self.messageViewPage.sourceLanguage.exists
            && self.messageViewPage.targetLanguage.exists
            && self.messageViewPage.closeButton.exists
        }
    }
    
    public func isMessageTranslated() throws -> Bool {
        XCTContext.runActivity(named: "Check is message translated") { _ in
            return self.messageViewPage.submitButton.label == "Revert"
        }
    }
    
    public func getSourceLanguage() throws -> LanguageName {
        XCTContext.runActivity(named: "Getting source language") { _ in
            return self.messageViewPage.sourceLanguage.label
        }
    }
    
    public func tapOnSourceLanguage() throws {
        try XCTContext.runActivity(named: "Tap on source language button") { _ in
            self.languageListType = .sourceLanguageList
            try self.messageViewPage.sourceLanguage.tapCarefully()
        }
    }
    
    public func getTargetLanguage() throws -> LanguageName {
        XCTContext.runActivity(named: "Getting target language") { _ in
            return self.messageViewPage.targetLanguage.label
        }
    }
    
    public func tapOnTargetLanguage() throws {
        try XCTContext.runActivity(named: "Tap on target language button") { _ in
            self.languageListType = .targetLanguageList
            try self.messageViewPage.targetLanguage.tapCarefully()
        }
    }
    
    public func tapOnTranslateButton() throws {
        try XCTContext.runActivity(named: "Tap on Translate button") { _ in
            try self.messageViewPage.submitButton.tapCarefully()
        }
    }
    
    public func tapOnRevertButton() throws {
        try XCTContext.runActivity(named: "Tap on Revert button") { _ in
            try self.messageViewPage.submitButton.tapCarefully()
        }
    }
    
    public func getSubmitButtonLabel() throws -> String {
        XCTContext.runActivity(named: "Getting Submit button label") { _ in
            return self.messageViewPage.submitButton.label
        }
    }
    
    public func tapOnCloseBarButton(_ hideTranslatorForThisLanguage: Bool) throws {
        try XCTContext.runActivity(named: "Tap on Close button \(hideTranslatorForThisLanguage ? "and Hide translator bar" : "")") { _ in
            try self.messageViewPage.closeButton.tapCarefully()
            let sourceLanguageElement = self.messageViewPage.sourceLanguage
            if sourceLanguageElement.exists && sourceLanguageElement.label != "auto" {
                hideTranslatorForThisLanguage ? self.messageViewPage.alertButtonHide.tap() : self.messageViewPage.alertButtonCancel.tap()
            }
        }
    }

    // MARK: TranslatorLanguageList
    public func getAllSourceLanguages() -> YSArray<LanguageName> {
        XCTContext.runActivity(named: "Getting all source languages") { _ in
            return YSArray(array: self.sourceLanguagePage.languageNameList)
        }
    }
    
    public func setSourceLanguage(_ language: LanguageName) throws {
        try XCTContext.runActivity(named: "Setting \(language) as source language") { _ in
            self.languageListType = nil
            try self.sourceLanguagePage.findLanguage(language).tap()
        }
    }
    
    public func getCurrentSourceLanguage() throws -> LanguageName! {
        XCTContext.runActivity(named: "Getting current source language") { _ in
            return self.sourceLanguagePage.currentLanguageName
        }
    }
    
    public func getDeterminedAutomaticallySourceLanguage() throws -> LanguageName {
        XCTContext.runActivity(named: "Getting determined automatically source language") { _ in
            return self.sourceLanguagePage.determinedAutomaticallyLanguageName
        }
    }
    
    public func getRecentSourceLanguages() throws -> YSArray<LanguageName> {
        XCTContext.runActivity(named: "Getting recent source languages") { _ in
            return YSArray(array: self.sourceLanguagePage.recentLanguagesNames)
        }
    }
    
    public func getAllTargetLanguages() -> YSArray<LanguageName> {
        XCTContext.runActivity(named: "Getting all target languages") { _ in
            return YSArray(array: self.targetLanguagePage.languageNameList)
        }
    }
    
    public func setTargetLanguage(_ language: LanguageName, _ addToRecent: Bool) throws {
        try XCTContext.runActivity(named: "Setting target language") { _ in
            self.languageListType = nil
            try self.targetLanguagePage.findLanguage(language).tap()
        }
    }
    
    public func getCurrentTargetLanguage() throws -> LanguageName! {
        XCTContext.runActivity(named: "Getting current target language") { _ in
           return self.targetLanguagePage.currentLanguageName
        }
    }
    
    public func getDefaultTargetLanguage() throws -> LanguageName {
        XCTContext.runActivity(named: "Getting default target language") { _ in
            return self.targetLanguagePage.defaultLanguageName
        }
    }
    
    public func getRecentTargetLanguages() throws -> YSArray<LanguageName> {
        XCTContext.runActivity(named: "Getting recent target languages") { _ in
            return YSArray(array: self.targetLanguagePage.recentLanguagesNames)
        }
    }
    
    // MARK: TranslatorLanguageListSearch
    public func tapOnSearchTextField() throws {
        try XCTContext.runActivity(named: "Tap on search text field") { _ in
            switch self.languageListType {
            case .sourceLanguageList:
                try self.sourceLanguagePage.searchBar.tapCarefully()
            case .targetLanguageList:
                try self.targetLanguagePage.searchBar.tapCarefully()
            default:
                throw YSError("Language list is not open")
            }
        }
    }
    
    public func isSearchTextFieldFocused() throws -> Bool {
        try XCTContext.runActivity(named: "Check is search text field focused") { _ in
            switch self.languageListType {
            case .sourceLanguageList:
                return self.sourceLanguagePage.searchBar.value(forKey: "hasKeyboardFocus") as? Bool ?? false
            case .targetLanguageList:
                return self.targetLanguagePage.searchBar.value(forKey: "hasKeyboardFocus") as? Bool ?? false
            default:
                throw YSError("Language list is not open")
            }
        }
    }
    
    public func tapOnCancelButton() throws {
        try XCTContext.runActivity(named: "Tap on Cancel button in search navigationBar") { _ in
            switch self.languageListType {
            case .sourceLanguageList:
                try self.sourceLanguagePage.cancelButton.tapCarefully()
            case .targetLanguageList:
                try self.targetLanguagePage.cancelButton.tapCarefully()
            default:
                throw YSError("Language list is not open")
            }
        }
    }
    
    public func enterSearchQuery(_ query: String) throws {
        try XCTContext.runActivity(named: "Entering search query") { _ in
            switch self.languageListType {
            case .sourceLanguageList:
                self.sourceLanguagePage.searchBar.typeText(query)
            case .targetLanguageList:
                self.targetLanguagePage.searchBar.typeText(query)
            default:
                throw YSError("Language list is not open")
            }
        }
    }
    
    public func getSearchQuery() throws -> String {
        try XCTContext.runActivity(named: "Getting search query") { _ in
            switch self.languageListType {
            case .sourceLanguageList:
                return self.sourceLanguagePage.searchBar.value as! String
            case .targetLanguageList:
                return self.targetLanguagePage.searchBar.value as! String
            default:
                throw YSError("Language list is not open")
            }
        }
    }
    
    public func tapOnClearSearchFieldButton() throws {
        XCTContext.runActivity(named: "Tap on Clear search field button") { _ in
            YOXCTFail("Method not implemented")
        }
    }
    
    public func getSearchedLanguageList() throws -> YSArray<LanguageName> {
        try XCTContext.runActivity(named: "Tap on Clear search field button") { _ in
            switch self.languageListType {
            case .sourceLanguageList:
                return YSArray(array: self.sourceLanguagePage.languageNameList)
            case .targetLanguageList:
                return YSArray(array: self.targetLanguagePage.languageNameList)
            default:
                throw YSError("Language list is not open")
            }
        }
    }
    
    // MARK: TranslatorSettings
    private var ignoredTranslationLanguagesCellExist = false

    public func switchTranslator() throws {
        try XCTContext.runActivity(named: "Switching translator") { _ in
            try self.generalSettingsPage.translationEnableSwitcher.tapCarefully()
        }
    }
    
    public func isTranslatorEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if translator is enabled") { _ in
            return self.generalSettingsPage.translationEnable.value as! String == "1"
        }
    }
    
    public func openIgnoredTranslationLanguageList() throws {
        try XCTContext.runActivity(named: "Opening ignored translation languages list") { _ in
            if self.generalSettingsPage.translationIgnoredLanguages.exists {
                try self.generalSettingsPage.translationIgnoredLanguages.tapCarefully()
                self.ignoredTranslationLanguagesCellExist = true
            }
        }
    }
    
    public func removeTranslationLanguageFromIgnored(_ language: String) throws {
        try XCTContext.runActivity(named: "Removing \(language) language from ignored") { _ in
            try self.generalSettingsPage.deleteLanguageFromIgnoredList(language)
        }
    }
    
    public func getIgnoredTranslationLanguages() throws -> YSArray<String> {
        XCTContext.runActivity(named: "Getting ignored translation languages") { _ in
            if self.ignoredTranslationLanguagesCellExist {
                return YSArray(array: self.generalSettingsPage.translationIgnoredLanguagesList)
            }
            return YSArray()
        }
    }
    
    public func closeIgnoredTranslationLanguageList() throws {
        try XCTContext.runActivity(named: "Closing ignored translation languages list") { _ in
            try self.generalSettingsPage.backButton.tapCarefully()
        }
    }
    
    public func openDefaultTranslationLanguageList() throws {
        try XCTContext.runActivity(named: "Opening default translation languages list") { _ in
            try self.generalSettingsPage.translationDefaultLanguage.tapCarefully()
        }
    }
    
    public func setDefaultTranslationLanguage(_ language: String) {
        XCTContext.runActivity(named: "Setting \(language) as default translation language") { _ in
            self.generalSettingsPage.findDefaultLanguage(language).tap()
        }
    }
    
    public func getDefaultTranslationLanguage() -> String {
        XCTContext.runActivity(named: "Getting default translation language") { _ in
            return self.generalSettingsPage.translationDefaultLanguage.value as! String
        }
    }
    
    public func closeDefaultTranslationLanguageList() throws {
        try XCTContext.runActivity(named: "Closing default translation languages list") { _ in
            try self.generalSettingsPage.backButton.tapCarefully()
        }
    }
    
    public func isIgnoredLanguageCellShown() throws -> Bool {
        XCTContext.runActivity(named: "Check is ignored language cell shown") { _ in
            return self.generalSettingsPage.translationIgnoredLanguageCell.exists
        }
    }
    
    public func getDefaultTranslationLanguageFromGeneralSettingsPage() -> LanguageName {
        XCTContext.runActivity(named: "Getting default language name") { _ in
            return self.generalSettingsPage.translationDefaultLanguage.value as! String
        }
    }
}
