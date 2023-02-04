//
//  YaRentFlatsAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Alexey Salangin on 12.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation

enum YaRentFlatsAPIStubConfiguration {
    static func setupSMSForSigning(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.askSMS,
            filename: "rent-flats-request-sms.debug"
        )
    }

    static func setupSignSMS(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.signSMS,
            filename: "rent-flats-submit-sms.debug"
        )
    }

    private enum Paths {
        static let askSMS = "/2.0/rent/user/me/flats/draft/confirmation-code/request"
        static let signSMS = "/2.0/rent/user/me/flats/draft/confirmation-code/submit"
    }
}
