//
//  MainViewController+AlertsDebug.swift
//  MetroKitTestApp
//
//  Created by Konstantin Kiselev on 27.06.18.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension MainViewController {

    private func makeStringFromAlerts(_ alerts: [YMLAlert]) -> String {
        let a = alerts.compactMap { $0.title.getStringUsingSystemLanguage() }
        return a.joined(separator: "\n")
    }

    func checkAlerts(for infoService: YMLSchemeInfoService) {
        let session = infoService.makeSearchSession(with: YMLLanguage(value: YXPlatformCurrentState.currentLanguage()))
        
        let query = "Fili"
        let results = session.searchResults(withQuery: query)
        guard let station = results.first?.station else { return }
        
        print("\nAlerts for station from query ('\(query)'):\n\(makeStringFromAlerts(station.stationDisplayItem.alerts))\n")
    }

}

extension MainViewController: YMLSchemeAlertsManagerListener {

    func onAlertsUpdated() {
        print("🆘 Alerts updated !!!")
        
//        printDescription(forStationId: "st(246)")
    }

}
