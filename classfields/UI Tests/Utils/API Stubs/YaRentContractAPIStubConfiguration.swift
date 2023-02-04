//
//  YaRentContractAPIStubConfiguration.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation

enum YaRentContractAPIStubConfiguration {
    static func setupRentContract(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.rentContract,
            filename: "rent-ownerRentContract.debug"
        )
    }

    static func setupOwnerRentContractWithComments(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.rentContract,
            filename: "rent-ownerRentContractWithComments.debug"
        )
    }

    static func setupInputChanges(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.inputChanges,
            filename: "rent-ownerRentContract.debug"
        )
    }

    static func setupDownloadLink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.inputChanges,
            filename: "rent-ownerRentContract.debug"
        )
    }

    static func setupSMSForSigning(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.askSMS,
            filename: "rent-ownerContract-smsInfo.debug"
        )
    }

    static func setupSignSMS(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.signSMS,
            filename: "rent-ownerContract-smsInfo.debug"
        )
    }

    static func setupSignSMSWithPaymentID(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.signSMS,
            filename: "rent-ownerContract-smsSign.debug"
        )
    }

    private enum Paths {
        static let rentContract = "/2.0/rent/user/me/contracts/\(Self.commonContractID)/summary"
        static let inputChanges = "/2.0/rent/user/me/contracts/\(Self.commonContractID)/request-changes"
        static let askSMS = "/2.0/rent/user/me/contracts/\(Self.commonContractID)/confirmation-code/request"
        static let signSMS = "/2.0/rent/user/me/contracts/\(Self.commonContractID)/confirmation-code/submit"

        private static let commonContractID = "contractID"
    }
}
