//
//  UITestCommunicationService.swift
//  YREServiceLayer
//
//  Created by Leontyev Saveliy on 18.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import UserNotifications
import Starscream
import YRECoreUtils
import YREAppState
import enum YREAppConfig.UITestCommunicationModel
import enum YREAppConfig.UITestCommunicationSettings

public final class UITestCommunicationService {
    public init(
        connectionSettingsReader: YREConnectionSettingsReader,
        appEnvironmentManager: AppEnvironmentManagerProtocol
    ) {
        self.connectionSettingsReader = connectionSettingsReader
        self.appEnvironmentManager = appEnvironmentManager
    }

    public func activate() {
        guard Self.isEnabled(appTargetType: self.appEnvironmentManager.appTargetType) else { return }

        let urlString = self.connectionSettingsReader.localBackendServerEndpoint + "/" + UITestCommunicationSettings.socketName
        guard let url = URL(string: urlString) else { return }

        let request = URLRequest(url: url)
        let socket = WebSocket(request: request)

        socket.delegate = self
        socket.callbackQueue = DispatchQueue(label: "ru.yandex.realty.testCommunicationService")
        self.webSocket = socket

        socket.connect()
    }

    private let connectionSettingsReader: YREConnectionSettingsReader
    private let appEnvironmentManager: AppEnvironmentManagerProtocol
    private var webSocket: WebSocket?

    private static func isEnabled(appTargetType: AppTargetType) -> Bool {
        switch appTargetType {
            case .common, .unitTests:
                 return false
            case .uiTests:
                return true
        }
    }
}

extension UITestCommunicationService {
    private static func send(_ model: UITestCommunicationModel) {
        switch model {
            case let .push(payload):
                self.sendLocalPush(payload)
            case let .deeplink(url):
                self.openDeeplink(url)
        }
    }

    private static func sendLocalPush(_ push: UITestCommunicationModel.Push) {
        let content = UNMutableNotificationContent()
        content.title = push.title
        content.body = push.body
        content.userInfo = push.userInfo

        let date = Date().addingTimeInterval(1)
        let components = Calendar.current.dateComponents([.second], from: date)

        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)

        let uuidString = UUID().uuidString
        let request = UNNotificationRequest(identifier: uuidString, content: content, trigger: trigger)

        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }

    private static func openDeeplink(_ url: URL) {
        DispatchQueue.main.async {
            UIApplication.shared.open(url, options: [:], completionHandler: nil)
        }
    }
}

extension UITestCommunicationService: WebSocketDelegate {
    public func didReceive(event: WebSocketEvent, client: WebSocket) {
        guard case let .binary(payloadData) = event else { return }

        guard let payload = try? JSONDecoder().decode(UITestCommunicationModel.self, from: payloadData) else {
            assertionFailure("Unable to parse UITestCommunicationModel")
            return
        }
        Self.send(payload)
    }
}
