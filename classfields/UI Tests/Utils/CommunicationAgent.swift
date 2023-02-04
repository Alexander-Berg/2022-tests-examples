//
//  CommunicationAgent.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 18.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import Swifter
import XCTest
import YRETestsUtils
import YREAppConfig

/// Allows to send a message to the App via a socket.
/// Currently supports triggering local push notifications and deeplinks opening via `openURL` call.
final class CommunicationAgent {
    enum PushPayload: String {
        case techSupportNewMessage = "push-techSupportNewMessage.debug"
    }

    func setUp(usingRunningServer dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.registerWebSocket(path: UITestCommunicationSettings.socketName) { session in
            self.webSocketSession = session
            self.expectation.fulfill()
        }
    }

    func sendPush(_ payload: PushPayload) {
        let payloadData = ResourceProvider.jsonData(from: payload.rawValue)
        guard
            let payload = try? JSONSerialization.jsonObject(with: payloadData, options: []) as? [AnyHashable: Any]
        else {
            XCTFail("Couldn't parse the payload object")
            return
        }

        let push = UITestCommunicationModel.Push(userInfo: payload)
        let model = UITestCommunicationModel.push(push)
        self.send(model)
    }

    func sendDeeplink(_ url: URL, shouldAdjustURL: Bool = true) {
        let adjustedURL: URL
        if shouldAdjustURL {
            guard var urlComponents = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
                XCTFail("Couldn't obtain URL components")
                return
            }
            urlComponents.scheme = "yandexrealty"
            
            guard let url = urlComponents.url else {
                XCTFail("Couldn't construct an URL from components")
                return
            }
            adjustedURL = url
        }
        else {
            adjustedURL = url
        }
        let model = UITestCommunicationModel.deeplink(adjustedURL)
        self.send(model)
    }

    func sendDeeplink(_ urlString: String, shouldAdjustURL: Bool = true) {
        guard let url = URL(string: urlString) else { XCTFail("Incorrect deeplink URL is given"); return }
        self.sendDeeplink(url, shouldAdjustURL: shouldAdjustURL)
    }

    // MARK: Private

    private var webSocketSession: WebSocketSession?
    private let waiter = XCTWaiter()
    private let expectation = XCTestExpectation()

    private func send(_ model: UITestCommunicationModel) {
        self.waiter.wait(for: [self.expectation], timeout: Constants.timeout)

        guard let session = self.webSocketSession else { XCTFail("App isn't connected via socket"); return }
        guard let data = try? JSONEncoder().encode(model) else { XCTFail("Couldn't encode the data"); return }

        session.writeBinary([UInt8](data))
    }
}
