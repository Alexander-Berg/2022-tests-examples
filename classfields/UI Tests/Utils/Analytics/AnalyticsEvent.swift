//
//  AnalyticsEvent.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 08.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation

protocol AnalyticsEvent: CustomStringConvertible {
    var name: String { get }
    var payload: JSONObject? { get }
}

struct MetricaAnalyticsEvent: AnalyticsEvent, Equatable {
    let name: String
    let payload: JSONObject?

    init(name: String, payload: JSONObject? = nil) {
        self.name = name
        self.payload = payload
    }

    var description: String {
        let payloadDescription = String(describing: payload)
        let message = "Metrica event '\(name)' with payload '\(payloadDescription)'"
        return message
    }
}

struct EventLogAnalyticsEvent: AnalyticsEvent, Equatable {
    let name: String
    let payload: JSONObject?

    init(name: String, payload: JSONObject? = nil) {
        self.name = name
        self.payload = payload
    }

    var description: String {
        let payloadDescription = String(describing: payload)
        let message = "EventLog event '\(name)' with payload '\(payloadDescription)'"
        return message
    }
}

struct AdjustAnalyticsEvent: AnalyticsEvent, Equatable {
    let name: String
    let payload: JSONObject?

    init(name: String, payload: JSONObject? = nil) {
        self.name = name
        self.payload = payload
    }

    var description: String {
        let payloadDescription = String(describing: payload)
        let message = "Adjust event '\(name)' with payload '\(payloadDescription)'"
        return message
    }
}
