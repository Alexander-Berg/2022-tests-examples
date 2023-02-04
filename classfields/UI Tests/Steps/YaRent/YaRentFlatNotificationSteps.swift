//
//  YaRentFlatNotificationSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 29.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.YaRentFlatCardAccessibilityIdentifiers

final class YaRentFlatNotificationSteps {
    enum NotificationType {
        // Common
        case fallback
        case appUpdate
        case netPromoterScore

        // UserNotifications
        case ownerWithoutINN
        case ownerWithoutCard
        case ownerWithManyCards

        // Payment
        case tenantFirstPayment

        // Owner before rent
        case ownerDraftNeedToFinish
        case ownerDraftNeedConfirmation
        case ownerWaitingForArendaTeamContact
        case ownerPrepareFlatForMeeting
        case ownerNeedToAddPassport
        case ownerKeysStillWithYou
        case ownerKeysHandedOverToManager
        case ownerPreparingFlatForExposition
        case ownerLookingForTenants
        case ownerCheckTenantCandidates
        case ownerRequestDeclined
        case ownerRequestCanceled

        case ownerRentOver

        // Tenant
        case tenantRentEnded

        // HouseUtilities Tenant
        case sendMeterReadings
        case meterReadingsDeclined
        case sendReceipts
        case receiptsDeclined
        case sendPaymentConfirmation
        case paymentConfirmationDeclined
        case houseUtilitiesBillsReceived

        // HouseUtilities Owner
        case receivedAllMeterReadings
        case receiptsReceived
        case paymentConfirmationReceived
        case timeToSendBills
        case billsDeclined

        // Inventory owner
        case ownerNeedToFillOutInventory
        case ownerNeedToConfirmInventory
        case tenantNeedToConfirmInventory

        // RentContract owner
        case ownerSignRentContract
    }

    init(_ type: NotificationType) {
        self.type = type
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие нотификации '\(self.type.title)'") { _ -> Void in
            self.notificationView
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSheetPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие подложки нотификации '\(self.type.title)'") { _ -> Void in
            self.sheetView
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func action() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку на нотификации '\(self.type.title)'") { _ -> Void in
            self.actionButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSheetLink() -> Self {
        XCTContext.runActivity(named: "Нажимаем на ссылку из подложки нотификации '\(self.type.title)'") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: AccessibilityIdentifiers.sheetLink,
                               in: self.sheetView)
                .yreEnsureExists()
                .yreForceTap()
        }
        return self
    }

    private typealias AccessibilityIdentifiers = YaRentFlatCardAccessibilityIdentifiers.Notification

    private let type: NotificationType

    private lazy var notificationView = ElementsProvider.obtainElement(identifier: self.type.accessibilityIdentifier)
    private lazy var sheetView = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.sheet)

    private lazy var actionButton = ElementsProvider.obtainButton(identifier: AccessibilityIdentifiers.actionButton,
                                                                  in: self.notificationView)
}

extension YaRentFlatNotificationSteps.NotificationType {
    var accessibilityIdentifier: String {
        let result: String
        switch self {
            // Common
            case .fallback:
                result = AccessibilityIdentifiers.Common.fallback
            case .appUpdate:
                result = AccessibilityIdentifiers.Common.appUpdateFallback
            case .netPromoterScore:
                result = AccessibilityIdentifiers.Common.netPromoterScore

            // UserNotifications
            case .ownerWithoutINN:
                result = AccessibilityIdentifiers.User.rentOwnerWithoutINN
            case .ownerWithoutCard:
                result = AccessibilityIdentifiers.User.rentOwnerWithoutCard
            case .ownerWithManyCards:
                result = AccessibilityIdentifiers.User.rentOwnerWithManyCards

            // Payment
            case .tenantFirstPayment:
                result = AccessibilityIdentifiers.Payment.first

            // Owner before rent
            case .ownerDraftNeedToFinish:
                result = AccessibilityIdentifiers.Owner.draftNeedToFinish
            case .ownerDraftNeedConfirmation:
                result = AccessibilityIdentifiers.Owner.draftNeedConfirmation
            case .ownerWaitingForArendaTeamContact:
                result = AccessibilityIdentifiers.Owner.waitingForArendaTeamContact
            case .ownerPrepareFlatForMeeting:
                result = AccessibilityIdentifiers.Owner.prepareFlatForMeeting
            case .ownerNeedToAddPassport:
                result = AccessibilityIdentifiers.Owner.needToAddPassport
            case .ownerKeysStillWithYou:
                result = AccessibilityIdentifiers.Owner.keysStillWithYou
            case .ownerKeysHandedOverToManager:
                result = AccessibilityIdentifiers.Owner.keysHandedOverToManager
            case .ownerPreparingFlatForExposition:
                result = AccessibilityIdentifiers.Owner.preparingFlatForExposition
            case .ownerLookingForTenants:
                result = AccessibilityIdentifiers.Owner.lookingForTenants
            case .ownerCheckTenantCandidates:
                result = AccessibilityIdentifiers.Owner.checkTenantCandidates
            case .ownerRequestDeclined:
                result = AccessibilityIdentifiers.Owner.requestDeclined
            case .ownerRequestCanceled:
                result = AccessibilityIdentifiers.Owner.requestCanceled
            case .ownerRentOver:
                result = AccessibilityIdentifiers.Owner.rentOver

            // Tenant
            case .tenantRentEnded:
                result = AccessibilityIdentifiers.Tenant.rentEnded

            // HouseUtilities Tenant
            case .sendMeterReadings:
                result = AccessibilityIdentifiers.HouseUtilities.sendMeterReadings
            case .meterReadingsDeclined:
                result = AccessibilityIdentifiers.HouseUtilities.declinedMeterReadings
            case .sendReceipts:
                result = AccessibilityIdentifiers.HouseUtilities.sendReceipts
            case .receiptsDeclined:
                result = AccessibilityIdentifiers.HouseUtilities.receiptsDeclined
            case .sendPaymentConfirmation:
                result = AccessibilityIdentifiers.HouseUtilities.sendPaymentConfirmation
            case .paymentConfirmationDeclined:
                result = AccessibilityIdentifiers.HouseUtilities.paymentConfirmationDeclined
            case .houseUtilitiesBillsReceived:
                result = AccessibilityIdentifiers.HouseUtilities.billsReceived

            // HouseUtilities Owner
            case .receivedAllMeterReadings:
                result = AccessibilityIdentifiers.HouseUtilities.allMeterReadingsReceived
            case .receiptsReceived:
                result = AccessibilityIdentifiers.HouseUtilities.receiptsReceived
            case .paymentConfirmationReceived:
                result = AccessibilityIdentifiers.HouseUtilities.paymentConfirmationReceived
            case .timeToSendBills:
                result = AccessibilityIdentifiers.HouseUtilities.timeToSendBills
            case .billsDeclined:
                result = AccessibilityIdentifiers.HouseUtilities.billsDeclined

            // Inventory Owner
            case .ownerNeedToFillOutInventory:
                result = AccessibilityIdentifiers.Inventory.ownerNeedToFillOutInventory
            case .ownerNeedToConfirmInventory:
                result = AccessibilityIdentifiers.Inventory.ownerNeedToConfirmInventory

            // Inventory Tenant
            case .tenantNeedToConfirmInventory:
                result = AccessibilityIdentifiers.Inventory.tenantNeedToConfirmInventory

            // RentContract Owner
            case .ownerSignRentContract:
                result = AccessibilityIdentifiers.RentContract.ownerSignRentContract
        }
        return result
    }

    var title: String {
        let result: String
        switch self {
            // Common
            case .fallback:
                result = "Фолбэк"
            case .appUpdate:
                result = "Обновись"
            case .netPromoterScore:
                result = "Оцените нас"

            // UserNotifications
            case .ownerWithoutINN:
                result = "Укажите ИНН"
            case .ownerWithoutCard:
                result = "Привяжите карту"
            case .ownerWithManyCards:
                result = "Не получается перевести деньги"

            // Payment
            case .tenantFirstPayment:
                result = "Поздравляем с переездом"

            // Owner before rent
            case .ownerDraftNeedToFinish:
                result = "Анкета почти готова"
            case .ownerDraftNeedConfirmation:
                result = "Осталось подтвердить заявку"
            case .ownerWaitingForArendaTeamContact:
                result = "Приняли вашу заявку"
            case .ownerPrepareFlatForMeeting:
                result = "Подготовка к встрече"
            case .ownerNeedToAddPassport:
                result = "Укажите паспортные данные"
            case .ownerKeysStillWithYou:
                result = "Если вы не передали нам ключи"
            case .ownerKeysHandedOverToManager:
                result = "Вы передали нам ключи"
            case .ownerPreparingFlatForExposition:
                result = "Готовим объявление"
            case .ownerLookingForTenants:
                result = "Ищем жильцов"
            case .ownerCheckTenantCandidates:
                result = "Новые кандидаты"
            case .ownerRequestDeclined:
                result = "Заявка отклонена"
            case .ownerRequestCanceled:
                result = "Мы всегда рядом"
            case .ownerRentOver:
                result = "соб: Аренда завершена"

            // Tenant
            case .tenantRentEnded:
                result = "жилец: Aренда завершена"

            // HouseUtilities Tenant
            case .sendMeterReadings:
                result = "Пора передавать показания"
            case .meterReadingsDeclined:
                result = "Показания счётчиков отклонены"
            case .sendReceipts:
                result = "Пора передать фото квитанций"
            case .receiptsDeclined:
                result = "Фото квитанций отклонены"
            case .sendPaymentConfirmation:
                result = "Пора передать подтверждение оплаты"
            case .paymentConfirmationDeclined:
                result = "Подверждения оплаты отклонены"
            case .houseUtilitiesBillsReceived:
                result = "Перейти к оплате"

            // HouseUtilities Owner
            case .receivedAllMeterReadings:
                result = "Получены показания по всем счётчикам"
            case .receiptsReceived:
                result = "Получены фотографии квитанций"
            case .paymentConfirmationReceived:
                result = "Получены фотографии подтверждения оплаты"
            case .timeToSendBills:
                result = "Пора выставить счёт"
            case .billsDeclined:
                result = "Счёт отклонен"

            // Inventory Owner
            case .ownerNeedToFillOutInventory:
                result = "Нужно заполнить опись"
            case .ownerNeedToConfirmInventory:
                result = "Соб: Нужно подписать опись"

            // Inventory Tenant
            case .tenantNeedToConfirmInventory:
                result = "Жилец: Нужно подписать опись"

            // RentContract Owner
            case .ownerSignRentContract:
                result = "Договор готов"
        }
        return result
    }

    private typealias AccessibilityIdentifiers = YaRentFlatCardAccessibilityIdentifiers.Notification
}
