//
//  VehiclesForecastTests.swift
//  YandexTransport
//
//  Created by Konstantin Kiselev on 24/03/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class VehiclesForecastTests: XCTestCase {
    
    fileprivate struct Static {
        struct Info {
            static let XmlFile = VehiclesForecastXMLParserTests.Static.Info.SimpleTest.XmlFile
            static let VehiclesCount = VehiclesForecastXMLParserTests.Static.Info.SimpleTest.VehiclesCount
            
            static let defaultPoint = YMK.Point(lat: 55.751443, lon: 37.560998)
            static let defaultSpan = YMK.Span(lat: 0.1, lon: 0.1)
            
            static let placeholderError = NSError(domain: "VehiclesForecastTests", code: 0, userInfo: nil)
        }
    }
    
    func testThatClientWorksWithCorrectResponse() {
        let requestPerformer = NetworkRequestPerformerPlaceholder()
        requestPerformer.response = { r in
            let xmlPath = Bundle(for: type(of: self)).path(forResource: Static.Info.XmlFile, ofType: "xml")!
            let xmlData = NSData(contentsOfFile: xmlPath)
            
            return (xmlData, nil)
        }
        
        let cOpt = Masstransit.VehiclesForecastClient.Options(origin: "", lang: "")
        let client = Masstransit.VehiclesForecastClient(requestPerformer: requestPerformer, options: cOpt)
        
        let p = Static.Info.defaultPoint
        let spn = Static.Info.defaultSpan
        
        let expectationFetch = expectation(description: "")
        
        let rOpt = Masstransit.VehiclesForecastClient.RequestOptions(
            location: p, span: spn, routeIds: nil, threadIds: nil, vtypes: nil)
        
        let fetchOp = client.fetchVehicles(options: rOpt)
        
        fetchOp.completion = { res in
            expectationFetch.fulfill()
        }
        fetchOp.start()
        
        waitForExpectations(timeout: 0.5, handler: nil)
        
        XCTAssertNotNil(fetchOp.result)
        
        switch fetchOp.result! {
        case .ok(let vehicles):
            XCTAssertEqual(vehicles.count, Static.Info.VehiclesCount)
        case .err(_):
            XCTFail("result is expected tp contain vehicles")
        }
    }
    
    func testThatClientWorksWithBadResponse() {
        let requestPerformer = NetworkRequestPerformerPlaceholder()
        requestPerformer.response = { r in
            return (nil, Static.Info.placeholderError)
        }
        
        let cOpt = Masstransit.VehiclesForecastClient.Options(origin: "", lang: "")
        let client = Masstransit.VehiclesForecastClient(requestPerformer: requestPerformer, options: cOpt)
        
        let p = Static.Info.defaultPoint
        let spn = Static.Info.defaultSpan
        
        let expectationFetch = expectation(description: "")
        
        let rOpt = Masstransit.VehiclesForecastClient.RequestOptions(
            location: p, span: spn, routeIds: nil, threadIds: nil, vtypes: nil)
        
        let fetchOp = client.fetchVehicles(options: rOpt)
        
        fetchOp.completion = { res in
            expectationFetch.fulfill()
        }
        fetchOp.start()
        
        waitForExpectations(timeout: 0.5, handler: nil)
        
        XCTAssertNotNil(fetchOp.result)
        
        switch fetchOp.result! {
        case .ok(_):
            XCTFail("result is expected to contain error")
        case .err(let er):
            XCTAssertEqual(er as NSError?, Static.Info.placeholderError)
        }
    }
}
