//
//  VehiclesForecastXMLParserTests.swift
//  YandexTransport
//
//  Created by Konstantin Kiselev on 24/03/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class VehiclesForecastXMLParserTests: XCTestCase {
    
    struct Static {
        struct Info {
            struct SimpleTest {
                static let XmlFile = "masstransit_forecast_trajectories_1"
                static let VehiclesCount = 6
                
                fileprivate static let FirstVehicle: () -> Masstransit.Vehicle = {
                    let stop1_meta = Masstransit.StopMeta(id: "stop__9669062", name: "Ферма-2")

                    let stop2_meta = Masstransit.StopMeta(id: "stop__9998644",
                        name: "Ул. Можайского")
                    
                    let meta = Masstransit.Vehicle.Meta(id: "839785|2184", transportId: "43A_22_bus_default",
                        transportName: "22", transportType: .bus, essentialStops: [stop1_meta, stop2_meta])
                    
                    let comps = NSDateComponents()
                    comps.calendar = Calendar(identifier: .gregorian)
                    comps.year = 2016
                    comps.month = 3
                    comps.day = 24
                    comps.hour = 12
                    comps.minute = 59
                    comps.second = 28
                    comps.timeZone = TimeZone(secondsFromGMT: 0)
                    
                    let p1 = YMK.Point(latitude: 55.7971776, longitude: 49.1144765)!
                    let p2 = YMK.Point(latitude: 55.7973699, longitude: 49.1123380)!
                    let p3 = YMK.Point(latitude: 55.7975828, longitude: 49.1099710)!
                    
                    let seg = Masstransit.Vehicle.TrajectorySegment(time: comps.date!,
                        duration: 21, points: [p1, p2, p3])
                    return Masstransit.Vehicle(meta: meta, trajectory: [seg])
                }
            }
            
            fileprivate struct TestEmptyTrajectory {
                static let XmlFile = "masstransit_forecast_trajectories_2"
            }
        }
    }
    
    func testThatParsingWorksForNullData() {
        XCTAssertNil(Masstransit.VehiclesForecastClient.XMLParser(nil).parse().result)
    }
    
    func testThatParsingWorksForIncorrectData() {
        XCTAssertNil(Masstransit.VehiclesForecastClient.XMLParser(NSData()).parse().result)
    }
    
    func testThatParsingReturnsEmptyArrayForUnsupportedXml() {
        let data = "<root attr = \"1\">Text <br /></root>".data(using: .utf8)!
        
        let res = Masstransit.VehiclesForecastClient.XMLParser(data as NSData).parse().result
        
        XCTAssertNotNil(res)
        XCTAssertEqual(res!.count, 0)
    }
    
    func testThatThereIsNoVehiclesWithEmptyTrajectory() {
        let xmlPath = Bundle(for: type(of: self)).path(forResource: Static.Info.TestEmptyTrajectory.XmlFile, ofType: "xml")!
        let xmlData = NSData(contentsOfFile: xmlPath)
        
        let xmlParser = Masstransit.VehiclesForecastClient.XMLParser(xmlData)
        let result = xmlParser.parse().result

        XCTAssertNotNil(result)
        XCTAssertEqual(result!.count, 0)
    }
    
    func testThatParsingOfSimpleXmlResponseWorks() {
        let xmlPath = Bundle(for: type(of: self)).path(forResource: Static.Info.SimpleTest.XmlFile, ofType: "xml")!
        let xmlData = NSData(contentsOfFile: xmlPath)
        
        let xmlParser = Masstransit.VehiclesForecastClient.XMLParser(xmlData, fixServerTime: false)
        let result = xmlParser.parse().result
        
        XCTAssertNotNil(result)
        XCTAssertEqual(result!.count, Static.Info.SimpleTest.VehiclesCount)
        
        let vehicle = result!.first!
        let correct = Static.Info.SimpleTest.FirstVehicle()
        
        XCTAssertEqual(vehicle.meta.id, correct.meta.id)
        XCTAssertEqual(vehicle.meta.transportId, correct.meta.transportId)
        XCTAssertEqual(vehicle.meta.transportName, correct.meta.transportName)
        XCTAssertEqual(vehicle.meta.transportType, correct.meta.transportType)
        
        XCTAssertEqual(vehicle.meta.essentialStops?.count ?? 0, correct.meta.essentialStops?.count ?? 0)
        for i in 0..<(vehicle.meta.essentialStops?.count ?? 0) {
            let s = vehicle.meta.essentialStops![i]
            let c = correct.meta.essentialStops![i]
            
            XCTAssertEqual(s.id, c.id)
            XCTAssertEqual(s.name, c.name)
        }
        
        XCTAssertEqual(vehicle.trajectory.count, correct.trajectory.count)
        for i in 0..<vehicle.trajectory.count {
            let s = vehicle.trajectory[i]
            let c = correct.trajectory[i]
            
            XCTAssertEqual(s.duration, c.duration)
            XCTAssertEqual(s.time.timeIntervalSince1970, c.time.timeIntervalSince1970)

            XCTAssertEqual(s.points.count, c.points.count)
            for j in 0..<s.points.count {
                let p1 = s.points[j]
                let p2 = c.points[j]
                
                XCTAssertEqual(p1.latitude, p2.latitude)
                XCTAssertEqual(p1.longitude, p2.longitude)
            }
        }
    }
    
}
