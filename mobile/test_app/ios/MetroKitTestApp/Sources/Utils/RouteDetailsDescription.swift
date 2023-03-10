//
//  RouteDetailsDescription.swift
//  MetroKitTestApp
//
//  Created by Konstantin Kiselev on 02.04.18.
//  Copyright Â© 2018 Yandex LLC. All rights reserved.
//

import Foundation

class RouteDetailsDescription {
    private let details: YMLRouteDetails
    private let style: String

    init(details: YMLRouteDetails, style: String) {
        self.details = details
        self.style = style
    }

    func printDescription() {
        let timestr: (Int64) -> String = { v in
            var ret = "\(v/60%60):\(v % 60)"

            if v / 60 / 60 > 0 {
                ret = "\(v / 60 / 60)" + ret
            }
            return "(\(ret))"
        }

        for s in details.sections {
            if let e = s.kind.enter {
                print("ð¶ââï¸ ENTER\n\t\t- (\(e.enterStation.station.name.getStringUsingSystemLanguage()))")
            }
            else if let e = s.kind.exit {
                print("ð¶ââï¸ EXIT\n\t\t- (\(e.exitStation.station.name.getStringUsingSystemLanguage()))")
                let alerts = "[" + e.alerts.compactMap { $0.title.getStringUsingSystemLanguage() }.joined(separator: " | ") + "]"
                print("\t\t\t-> (\(alerts))")
            }
            if let w = s.kind.wait {
                print("â± WAIT\n\t\t- (\(w.waitOnStop.station.station.name.getStringUsingSystemLanguage()))")
            }
            else if let t = s.kind.transfer {
                let fromServices = t.from.service.map { $0.name.getStringUsingSystemLanguage() }.joined(separator: " | ")
                let toServices = t.to.service.map { $0.name.getStringUsingSystemLanguage() }.joined(separator: " | ")

                print("ð¶ââï¸TRANSFER (\(s.time) sec " + (t.longWalk || t.overgroundWalk ? " / LONG ð" : "") + ")")
                print("\t\tâ from (\(t.from.station.name.getStringUsingSystemLanguage()) / \(fromServices))")

                let alerts = "[" + t.alerts.compactMap { $0.title.getStringUsingSystemLanguage() }.joined(separator: " | ") + "]"
                print("\t\t\t-> (\(alerts))")

                for v in t.via {
                    let vServices = v.service.map { $0.name.getStringUsingSystemLanguage() }.joined(separator: " | ")
                    print("\t\t\t\t via (\(v.station.name.getStringUsingSystemLanguage()) / \(vServices))")
                }
                print("\t\tâ to (\(t.to.station.name.getStringUsingSystemLanguage()) / \(toServices))")
            }
            else if let r = s.kind.ride {
                let serviceName = (r.service.name.getStringUsingSystemLanguage()) + ", alternatives (\(r.alternativeServices.count))"

                let oi = r.service.styles.serviceStyles[style]?.originalIcon?.id
                let ti = r.service.styles.serviceStyles[style]?.templateIcon?.id

                print("ð RIDE ('\(serviceName)': \(s.time) sec / \(r.stationStops.count) stations)")

                let alerts = "[" + r.alerts.compactMap { $0.title.getStringUsingSystemLanguage() }.joined(separator: " | ") + "]"
                print("\t\t\t-> (\(alerts))")

                for stationStop in r.stationStops {
                    print("\t\tâ¢ \(timestr(stationStop.timeOffset)))" + (stationStop.title.getStringUsingSystemLanguage() ?? "[Untitled station]") + " / Cars (\(r.cars)")
                }
            }
        }
    }
}
