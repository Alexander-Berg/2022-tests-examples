//
//  DeviceLogSettingsPage.swift
//  YandexMaps
//
//  Created by Alexander Goremykin on 28.04.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import UIKit

protocol DeviceLogSettingsDelegate: class {
    func logSettingsLevelsInfo() -> (available: [LogLevel], forbidden: [LogLevel])
    func logSettingsScopesInfo() -> (available: [String], forbidden: [String])
    func logSettingsUpdateForbiddenEntries(_ levels: [LogLevel], scopes: [String])
}

final class DeviceLogSettingsPage: UIView, UITableViewDelegate, UITableViewDataSource {
    
    weak var delegate: DeviceLogSettingsDelegate?
    
    // MARK: -
    
    init() {
        super.init(frame: CGRect.zero)
        
        setupUI()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: - UIView
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        tableView.frame.origin = CGPoint(x: 0.0, y: 20.0)
        tableView.frame.size = CGSize(width: bounds.width, height: bounds.height - 20.0)
        
        separator.frame.inset(from: .bottom, in: bounds)
        separator.frame.inset(from: .left, in: bounds)
        separator.frame.size = CGSize(width: bounds.width, height: 1.0 / UIScreen.main.scale)
    }
    
    func reloadData() {
        guard let delegate = delegate else { return }
        
        let levels = delegate.logSettingsLevelsInfo()
        
        levelsInfo = (available: levels.available.map { $0.customRawValue },
            forbidden: levels.forbidden.map { $0.customRawValue })
        
        scopesInfo = delegate.logSettingsScopesInfo()
        
        tableView.reloadData()
    }
    
    // MARK: - Private
    
    private let tableView = UITableView()
    private let separator = UIView()
    
    private var levelsInfo = (available: [String](), forbidden: [String]())
    private var scopesInfo = (available: [String](), forbidden: [String]())
    
    private func setupUI() {
        backgroundColor = UIColor.white
        
        apply(tableView) {
            addSubview($0)
            $0.delegate = self
            $0.dataSource = self
        }

        apply(separator) {
            addSubview($0)
            $0.backgroundColor = .lightGray
        }
    }
    
    // MARK: - UITableViewDelegate, UITableViewDataSource
    
    func numberOfSections(in tableView: UITableView) -> Int {
        let levelsAvailable = levelsInfo.available.isEmpty ? 0 : 1
        let scopesAvailable = scopesInfo.available.isEmpty ? 0 : 1
        return levelsAvailable + scopesAvailable
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if section == 0 {
            return !levelsInfo.available.isEmpty ? levelsInfo.available.count : scopesInfo.available.count
        } else if section == 1 {
            return scopesInfo.available.count
        }
        
        return 0
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let identifier = "SettingsCell"
        
        var cell = tableView.dequeueReusableCell(withIdentifier: identifier)
        if cell == nil {
            cell = UITableViewCell(style: .default, reuseIdentifier: identifier)
        }
        
        let getInfoFromLevels = (indexPath as NSIndexPath).section == 0 && !levelsInfo.available.isEmpty
        let info = getInfoFromLevels ? levelsInfo : scopesInfo
        
        let entry = info.available[(indexPath as NSIndexPath).row]
        cell?.textLabel?.text = entry
        cell?.textLabel?.lineBreakMode = .byTruncatingMiddle
        cell?.backgroundColor = info.forbidden.contains(entry) ? UIColor(rgb: 0xE40303) : UIColor(rgb: 0x008026)
        
        return cell!
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if section == 0 {
            return !levelsInfo.available.isEmpty ? "Levels" : "Scopes"
        } else if section == 1 {
            return "Scopes"
        }
        
        return nil
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let getInfoFromLevels = (indexPath as NSIndexPath).section == 0 && !levelsInfo.available.isEmpty
        var info = getInfoFromLevels ? levelsInfo : scopesInfo
        let entry = info.available[(indexPath as NSIndexPath).row]
        
        if let index = info.forbidden.index(of: entry) {
            info.forbidden.remove(at: index)
        } else {
            info.forbidden.append(entry)
        }
        
        if getInfoFromLevels {
            levelsInfo = info
        } else {
            scopesInfo = info
        }
        
        tableView.deselectRow(at: indexPath, animated: true)
        
        let levels: [LogLevel] = levelsInfo.forbidden.compactMap { LogLevel.make(fromCustomRawValue: $0) }
        delegate?.logSettingsUpdateForbiddenEntries(levels, scopes: scopesInfo.forbidden)
        tableView.reloadData()
    }
    
}

private extension LogLevel {

    var customRawValue: String {
        switch self {
        case .error: return "error"
        case .warning: return "warning"
        case .info: return "info"
        case .debug: return "debug"
        }
    }
    
    static func make(fromCustomRawValue value: String) -> LogLevel? {
        switch value {
        case "error": return .error
        case "warning": return .warning
        case "info": return .info
        case "debug": return .debug
        default: return nil
        }
    }

}
