//
//  DeviceLog.swift
//
//
//  Created by Alexander Goremykin on 25.04.16.
//  Copyright © 2016 Alexander Goremykin. All rights reserved.
//

import UIKit

public final class DeviceLog {

    public weak var container: UIWindow? {
        didSet {
            layoutLogView()
        }
    }

    public init() {
        dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "HH:mm:ss"

        logFont = UIFont(name: "Courier", size: 10.0) ?? UIFont.systemFont(ofSize: 10.0)
        
        //
        
        NotificationCenter.default.addObserver(
            self, selector:  #selector(deviceDidRotate),
            name: UIApplication.didChangeStatusBarOrientationNotification, object: nil)
        
        setupLogView()
    }
    
    // MARK: - Public
    
    public func report(level: LogLevel, scope: String, message: String) {
        report(level: level, scope: scope, timestamp: Date(), message: message)
    }
    
    // MARK: - Private
    
    private struct LogEntry {
        var level: LogLevel
        var timestamp: Date
        var scope: String
        var message: String
    }

    private var log = RoundRobinBuffer<LogEntry>(size: DeviceLog.Parameters.logHistorySize)
    private var forbiddenLevels = [LogLevel]()
    private var forbiddenScopes = [String]()
    private let dateFormatter: DateFormatter
    private let logFont: UIFont
    
    private let logView = DeviceLogView()
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    private func layoutLogView() {
        guard let container = container else {
            logView.removeFromSuperview()
            return
        }
        
        container.addSubview(logView)

        var targetFrame = container.bounds
        targetFrame.size.height *= Parameters.logWindowHeightFraction

        logView.frame = targetFrame
    }
    
    private func setupLogView() {
        logView.settingsPageView.delegate = self
    
        logView.onWillOpenCallback = { [weak self] in
            self?.update()
        }
    
        logView.logPageView.onPause = { [weak self] _
            in self?.update()
        }
    
        logView.logPageView.onClear = { [weak self] in
            self?.log.clear()
            self?.update()
        }
    }

    private func update() {
        guard logView.state != .closed && container != nil else { return }

        let string = NSMutableAttributedString()
        
        for i in (0..<log.count).lazy.reversed() {
            if let entry = log[i], let str = makeString(from: entry) {
                string.append(str)
            }
        }

        DispatchQueue.main.async { [weak self] in
            self?.logView.logPageView.log = string
        }
    }
    
    @objc private func deviceDidRotate() {
        layoutLogView()
    }
    
    private func hideLog() {
        DispatchQueue.main.async { [weak self] in
            self?.logView.hide(animated: true)
        }
    }
    
    private func report(level: LogLevel, scope: String, timestamp: Date, message: String) {
        let entry = LogEntry(level: level, timestamp: timestamp, scope: scope,
                             message: DeviceLog.logMessageCleaner(message))
        log.put(entry)

        update()
    }
    
    // MARK: - Private: Formatting
    
    private func makeString(from entry: LogEntry) -> NSAttributedString? {
        let scope = entry.scope.isEmpty ? Parameters.emptyScopeName : entry.scope
    
        if forbiddenLevels.contains(entry.level) || forbiddenScopes.contains(scope) {
            return nil
        }
    
        let ret = NSMutableAttributedString()
    
        ret.append(NSAttributedString(string: "\(dateFormatter.string(from: entry.timestamp))|",
            attributes: [.font: logFont, .foregroundColor: UIColor.lightText]))
    
        ret.append(NSAttributedString(string: "\(entry.scope.isEmpty ? "" : "[\(entry.scope)] ")\(entry.message)\n",
            attributes: [.font: logFont, .foregroundColor: entry.level.color]))
        
        return ret
    }

    private static func formatForReport(_ dict: [AnyHashable: Any]) -> String {
        let data = try? JSONSerialization.data(withJSONObject: dict, options: .prettyPrinted)
        let jsonString = data.flatMap { String(data: $0, encoding: .utf8) } ?? ""
        let correctJson = DeviceLog.regexReplace(in: jsonString, pattern: "\"\\{(.*)\\}\"", withTemplate: "\\{ $1 \\}")
        let result = DeviceLog.regexReplace(in: correctJson, pattern: "\\\\+([/\"])", withTemplate: "$1")

        return result
    }

    private static func logMessageCleaner(_ str: String) -> String {
        let result = DeviceLog.regexReplace(in: str, pattern: "AnyHashable\\(\"(.*?)\"\\)", withTemplate: "\"$1\"")

        return result
    }

    private static func regexReplace(in text: String, pattern: String, withTemplate template: String) -> String {
        let regex = try? NSRegularExpression(pattern: pattern)
        let range = NSMakeRange(0, text.count)
        let result = regex.flatMap {
            $0.stringByReplacingMatches(in: text, options: [], range: range, withTemplate: template)
        }

        return result ?? ""
    }

}

extension DeviceLog: DeviceLogSettingsDelegate {
    
    func logSettingsLevelsInfo() -> (available: [LogLevel], forbidden: [LogLevel]) {
        let availableLevels = LogLevel.all
        return (available: availableLevels, forbidden: forbiddenLevels)
    }

    func logSettingsScopesInfo() -> (available: [String], forbidden: [String]) {
        var availableScopes = [String]()
        for i in 0..<log.count {
            if let scope = log[i]?.scope {
                let fixedScope = scope.isEmpty ? Parameters.emptyScopeName : scope
                if !availableScopes.contains(fixedScope) {
                    availableScopes.append(fixedScope)
                }
            }
        }

        return (available: availableScopes, forbidden: forbiddenScopes)
    }

    func logSettingsUpdateForbiddenEntries(_ levels: [LogLevel], scopes: [String]) {
        forbiddenLevels = levels
        forbiddenScopes = scopes
        update()
    }
    
}

fileprivate extension LogLevel {

    var color: UIColor {
        switch self {
        case .error: return .red
        case .warning: return .yellow
        case .info: return .white
        case .debug: return .white
        }
    }
    
}
