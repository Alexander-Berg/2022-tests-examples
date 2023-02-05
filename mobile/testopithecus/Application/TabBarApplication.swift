//
//  TabBarApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 01.04.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class TabBarApplication: TabBar, TabBarIOS {
    private let tabBarPage = TabBarPage()
    
    public func isShown() throws -> Bool {
        XCTContext.runActivity(named: "Checking is tabbar shown") { _ in
            return self.tabBarPage.view.exists && self.tabBarPage.view.isHittable
        }
    }

    public func getCurrentItem() throws -> TabBarItem {
        XCTContext.runActivity(named: "Getting current tabbar item") { _ in
            return self.tabBarPage.selectedItem
        }
    }

    public func tapOnItem(_ item: TabBarItem) throws {
        try XCTContext.runActivity(named: "Tapping on \(item) in tabbar") { _ in
            switch item {
            case .mail:
                try self.tabBarPage.mail.tapCarefully()
            case .calendar:
                try self.tabBarPage.calendar.tapCarefully()
            case .documents:
                try self.tabBarPage.documents.tapCarefully()
            case .telemost:
                try self.tabBarPage.telemost.tapCarefully()
            case .more:
                try self.tabBarPage.more.tapCarefully()
            default:
                throw YSError("There is no item \(item) in tabbar")
            }
        }
    }
    
    public func getItems() throws -> YSArray<TabBarItem> {
        XCTContext.runActivity(named: "Getting tabbar items") { _ in
            return YSArray(array: self.tabBarPage.tabBarItems)
        }
    }
    
    public func getCalendarIconDate() throws -> String {
        XCTContext.runActivity(named: "Getting calendar date label") { _ in
            return self.tabBarPage.calendarDate.label
        }
    }
}

public final class ShtorkaApplication: Shtorka, ShtorkaIOS {
    private let shtorkaPage = ShtorkaPage()

    public func closeBySwipe() throws {
        XCTContext.runActivity(named: "Closing shtorka by swipe") { _ in
            self.shtorkaPage.view.swipeDown()
        }
    }
    
    public func closeByTapOver() throws {
        XCTContext.runActivity(named: "Closing shtorka by tap over") { _ in
            self.shtorkaPage.grayBackArea.tap()
        }
    }

    public func getShownBannerType() throws -> ShtorkaBannerType! {
        XCTContext.runActivity(named: "Getting shown banner type") { _ in
            if self.shtorkaPage.bannerMail360.shown {
                return .mail360
            }
            if self.shtorkaPage.bannerDocs.shown {
                return .docs
            }
            if self.shtorkaPage.bannerScanner.shown {
                return .scanner
            }
            return nil
        }
    }

    public func tapOnBanner() throws {
        try XCTContext.runActivity(named: "Tapping on banner 'Try it' button") { _ in
            try self.shtorkaPage.bannerTryItButton.tapCarefully()
        }
    }

    public func closeBanner() throws {
        try XCTContext.runActivity(named: "Closing banner") { _ in
            try self.shtorkaPage.bannerCloseButton.tapCarefully()
        }
    }

    public func tapOnItem(_ item: TabBarItem) throws {
        try XCTContext.runActivity(named: "Tapping on \(item) in shtorka") { _ in
            switch item {
            case .mail:
                try self.shtorkaPage.mail.tapCarefully()
            case .calendar:
                try self.shtorkaPage.calendar.tapCarefully()
            case .documents:
                try self.shtorkaPage.documents.tapCarefully()
            case .telemost:
                try self.shtorkaPage.telemost.tapCarefully()
            case .disk:
                try self.shtorkaPage.disk.tapCarefully()
            case .notes:
                try self.shtorkaPage.notes.tapCarefully()
            case .scanner:
                try self.shtorkaPage.scanner.tapCarefully()
            case .subscriptions:
                try self.shtorkaPage.subscriptions.tapCarefully()
            default:
                throw YSError("There is no item \(item) in shtorka")
            }
        }
    }
    
    public func getItems() throws -> YSArray<TabBarItem> {
        XCTContext.runActivity(named: "Getting shtorka items") { _ in
            return YSArray(array: self.shtorkaPage.items)
        }
    }
}
