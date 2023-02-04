//
//  Predicate+AnalyticsEvent.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 08.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation

extension Predicate where Target == AnalyticsEvent {
    static func match(event: AnalyticsEvent) -> Self {
        let namePredicate = self.contains(name: event.name)
        let payloadPredicate = self.contains(payload: event.payload)
        return namePredicate && payloadPredicate
    }

    private static func contains(name: String) -> Self {
        let matcher: (AnalyticsEvent) -> Bool = { event -> Bool in
            return event.name == name
        }
        let description = "Analytics event CONTAINS name = '\(name)'"
        let predicate = Predicate(matcher: matcher, description: description)
        return predicate
    }

    private static func contains(payload: JSONObject?) -> Self {
        let matcher: (AnalyticsEvent) -> Bool = { event -> Bool in
            switch (event.payload, payload) {
                case let (.some(json), .some(jsonPart)):
                    return json.contains(jsonPart)
                case (.some, nil):
                    return true
                case (nil, .some):
                    return false
                case (nil, nil):
                    return false
            }
        }

        let description = "Analytics event CONTAINS payload = '\(String(describing: payload))'"
        let predicate = Predicate(matcher: matcher, description: description)
        return predicate
    }
}
