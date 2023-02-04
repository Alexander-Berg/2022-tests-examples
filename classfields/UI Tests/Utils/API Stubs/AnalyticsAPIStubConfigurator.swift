//
//  AnalyticsAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 10.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest

final class AnalyticsAPIStubConfigurator {
    static func setupMetrica(
        using dynamicStubs: HTTPDynamicStubs,
        completion: @escaping (MetricaAnalyticsEvent) -> Void
    ) {
        dynamicStubs.register(
            method: .POST,
            path: "/analytics/metrica",
            middleware: self.makeCallbackMiddleware(completion: { json in
                guard let eventName = json["eventName"]?.asString else { assertionFailure(); return }
                let payload = json["payload"]
                let event = MetricaAnalyticsEvent(name: eventName, payload: payload)

                XCTContext.runActivity(named: "Отправка события \"\(eventName)\" в Metrica", block: { activity in
                    let attachment = XCTAttachment(string: event.description)
                    attachment.name = "Событие"
                    activity.add(attachment)
                })
                completion(event)
            })
        )
    }

    static func setupEventLog(
        using dynamicStubs: HTTPDynamicStubs,
        completion: @escaping (EventLogAnalyticsEvent) -> Void
    ) {
        dynamicStubs.register(
            method: .POST,
            path: "/analytics/eventLog",
            middleware: self.makeCallbackMiddleware(completion: { json in
                guard let eventName = json["eventName"]?.asString else { assertionFailure(); return }
                let payload = json["payload"]
                let event = EventLogAnalyticsEvent(name: eventName, payload: payload)

                XCTContext.runActivity(named: "Отправка события \"\(eventName)\" в Event Log", block: { activity in
                    let attachment = XCTAttachment(string: event.description)
                    attachment.name = "Событие"
                    activity.add(attachment)
                })
                completion(event)
            })
        )
    }

    static func setupAdjust(
        using dynamicStubs: HTTPDynamicStubs,
        completion: @escaping (AdjustAnalyticsEvent) -> Void
    ) {
        dynamicStubs.register(
            method: .POST,
            path: "/analytics/adjust",
            middleware: self.makeCallbackMiddleware(completion: { json in
                guard let eventName = json["eventName"]?.asString else { assertionFailure(); return }
                let payload = json["payload"]
                let event = AdjustAnalyticsEvent(name: eventName, payload: payload)

                XCTContext.runActivity(named: "Отправка события \"\(eventName)\" в Adjust", block: { activity in
                    let attachment = XCTAttachment(string: event.description)
                    attachment.name = "Событие"
                    activity.add(attachment)
                })
                completion(event)
            })
        )
    }

    private static func makeCallbackMiddleware(
        completion: @escaping (JSONObject) -> Void
    ) -> MiddlewareProtocol {
        let middleware = MiddlewareBuilder
            .chainOf([
                .callback({ request in
                    DispatchQueue.main.async {
                        let data = Data(request.body)
                        guard let json = JSONObject(data: data) else { return }
                        completion(json)
                    }
                }),
                .respondWith(.ok(.contentsOfJSON("commonEmpty.debug"))),
            ])
            .build()
        return middleware
    }
}
