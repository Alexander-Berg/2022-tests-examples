//
//  YMLStation+Utils.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 23/03/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation


extension YMLStation {
    
    var customDescription: String {
        let stopStr = stops.map { "\($0.id)" }.joined(separator: ", ")
        let exitStr = exits.map { "\($0.id)" }.joined(separator: ", ")
    
        let name = self.name.getStringUsingSystemLanguage().replacingOccurrences(of: "\n", with: "")
        
        var historyTitle = ""
        var historyContent = ""
        
        if let h = historyInfo {
            historyTitle = h.title.getStringUsingSystemLanguage()
            historyContent = h.content.getStringUsingSystemLanguage()
        }

        return "\tID(\(id))\t\t\(name) [ STOPS(\(stopStr)), EXITS(\(exitStr)) ]\n\t• history: \t\t[\(historyTitle)] : \(historyContent)\n"
    }

}

extension YMLService {

    func customDescription(withStyle style: String) -> String {
        let names = (([name.getStringUsingSystemLanguage(), shortName?.getStringUsingSystemLanguage()]) as [String?])
            .compactMap { $0 }.joined(separator: " / ")
    
        let rgba = (styles.serviceStyles[style]?.color).map {
            return String(format: "%2x", $0)
        } ?? "none"

        let threadCount = threads.count
        return "\tID(\(id))\t\t\(names), color(\(rgba)), threads(\(threadCount))"
    }
    
}

extension YMLSchemeSearchSessionStationItem {
    
    var customDescription: String {
        let title = self.title.text + " —> " + self.title.highlights.map { "[\($0.begin), \($0.length)]" }.joined(separator: ", ")
        let subtitle = self.stationDisplayItem.station.service.map { $0.name.getStringUsingSystemLanguage() }.joined(separator: " | ")
        
        return "STATION ITEM\n\t• title: \t\t'\(title)'\n\t• services: \t\t'\(subtitle)\n"
    }
    
}

extension YMLSchemeSearchSessionServiceItem {
    
    var customDescription: String {
        let title = self.title.text
        let serviceDescr = service.customDescription
        return "SERVICE ITEM\n\t• title: \t\t'\(title)'\n\t• service info: \t\t\(serviceDescr)\n"
    }
    
}

extension YMLSchemeSearchSessionItem {
    
    var customDescription: String {
        if let stationItem = station {
            return stationItem.customDescription
        } else if let serviceItem = service {
            return serviceItem.customDescription
        } else {
            assert(false)
            return ""
        }
    }
    
}
