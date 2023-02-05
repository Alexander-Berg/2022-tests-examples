//
//  NotificationsStateServiceImplTests.swift
//  YandexMapsShutter
//
//  Created by Mikhail Kurenkov on 7/24/19.
//

import XCTest
import YandexMapsUtils
import YandexMapsShutter

class NotificationsStateServiceImplTests: XCTestCase {
    
    // MARK: Test NotificationsStateService
    
    func testAddOneViewedNotification() {
        let notificationsStateService = NotificationsStateServiceImpl(storage: TemporalKeyValueStorage())
        
        let notificationId = UUID().uuidString
        notificationsStateService.setViewedNotification(with: notificationId)
        
        assert(notificationsStateService.isViewedNotification(with: notificationId))
    }
    
    
    func testAddManyViewedNotificationWithoutExceedingMaxMumsOfStoredItems() {
        let notificationsStateService = NotificationsStateServiceImpl(storage: TemporalKeyValueStorage())
        
        let notificationIds: [String] = (0..<30).map { _ in UUID().uuidString }

        for notificationId in notificationIds {
            notificationsStateService.setViewedNotification(with: notificationId)
        }
        
        for notificationId in notificationIds {
            assert(notificationsStateService.isViewedNotification(with: notificationId))
        }
    }
    
    func testAddManyViewedNotificationWithExceedingMaxMumsOfStoredItems() {
        let notificationsStateService = NotificationsStateServiceImpl(storage: TemporalKeyValueStorage())
        
        let notificationIds: [String] = (0..<40).map { _ in UUID().uuidString }
        
        for notificationId in notificationIds {
            notificationsStateService.setViewedNotification(with: notificationId)
        }
        
        for notificationId in notificationIds.suffix(30) {
            assert(notificationsStateService.isViewedNotification(with: notificationId))
        }
        
        for notificationId in notificationIds.prefix(10) {
            assert(!notificationsStateService.isViewedNotification(with: notificationId))
        }
    }

    
}
