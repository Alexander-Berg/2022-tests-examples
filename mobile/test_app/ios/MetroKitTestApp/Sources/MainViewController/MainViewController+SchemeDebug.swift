//
//  MainViewController+SchemeDebug.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 23/03/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation


extension MainViewController {

    func printDescription(forStationId sid: String) {
        guard let st = infoService?.resolveDetails(withStationId: sid) else { return }
        print(st.stationDisplayItem.station.station.customDescription)
    }

    func printDescription(for infoService: YMLSchemeInfoService) {
        let stations: [YMLStation] = infoService.stations
        let services: [YMLService] = infoService.services
        
        print("Stations (\(stations.count)):")
        stations.forEach {
            print($0.customDescription)
        }
        
        print("\n")
        print("Services (\(services.count)):")
        
        services.forEach {
            print($0.customDescription)
        }
        
        let st = stations[4]
        
        print("\n")
        print("Services for station (\(st.name))")
        
        (infoService.services(withStationId: st.id)).forEach {
            print($0.customDescription)
        }
        
        let stDet = infoService.resolveDetails(withStationId: st.id)
        let loc = stDet?.location
        let _ = loc
        
        if let st2 = infoService.stations.first(where: { $0.name.getStringUsingSystemLanguage().contains("Уралмаш") }) {
            print("\n")
            print("Details for (\(st2.name)")


            if let xts = st2.exitsDescription {
                let title = xts.title.getStringUsingSystemLanguage()
                let items = xts.rows.map { "\t\t\($0.title.getStringUsingSystemLanguage()) — \($0.content.getStringUsingSystemLanguage())" }.joined(separator: "\n")
                print("Exits (\(title)):\n\(items)")
            }

            let details = infoService.resolveDetails(withStationId: st2.id)
            print(String(describing: details))
        }
    }
    
    func printDescription(for summary: YMLSchemeSummary, title: String) {
        let description =
            """
            Scheme Summary Info:
                defaultAlias: \(summary.defaultAlias)
                schemeId: \(summary.schemeId.value)
                version: \(summary.version)
                tags: \(summary.tags)
            """
        
        print(title)
        print(description)
    }

}
