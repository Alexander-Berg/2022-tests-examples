//
//  MainViewController+SearchPrompt.swift
//  MetroKitTestApp
//
//  Created by Konstantin Kiselev on 24.04.18.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension MainViewController {

    func checkSearchPrompt(for scheme: YMLScheme) {
        let prompt = scheme.makeSearchPrompt()
        print("🐝 DESCRIPTION FOR SEARCH PROMPT")
        
        if let req = prompt.lastRoutingRequest() {
            print("Last route: \(req.fromStationId) → \(req.toStationId)\n")
        } else {
            print("No last route")
        }
        
        print("🦋 MOST USED STATIONS:")
        let mostUsedStations = prompt.mostUsedStations();
        
        mostUsedStations.enumerated().forEach { e in
            print(
                "\t \(e.offset + 1).\t"
                + e.element.station.station.customDescription
                    + " (" + e.element.station.service.compactMap { $0.name.getStringUsingSystemLanguage() }.joined(separator: " | ") + ")")
        }
    }

}
