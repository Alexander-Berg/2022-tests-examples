//
//  PanoramaTests.swift
//  UITests
//
//  Created by Dmitry Sinev on 8/4/20.
//

import Foundation
import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

class PanoramaTests: BaseTest {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)
    lazy var mainSteps = MainSteps(context: self)

    static var draftResponse: Auto_Api_DraftResponse = {
        let filePath = Bundle.resources.url(forResource: "offer_edit_get_draft", withExtension: "json")
        let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
        let response = body.flatMap { try? Auto_Api_DraftResponse(jsonUTF8Data: $0) }
        return response!
    }()

    override func setUp() {
        super.setUp()
        let videoDirectory = URL(fileURLWithPath: #file)
        .deletingLastPathComponent()
        .deletingLastPathComponent()
        .appendingPathComponent("Resources")
        .appendingPathComponent("testPanoramaFull")
        .appendingPathExtension("mov")

        app.launchArguments.append("panoramaVideo:\(videoDirectory)")

        setupServer()
        launch()
    }

    func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/cars/all-options") { (request, _) -> Response? in
            return Response.okResponse(fileName: "offer_edit_get_equipment", userAuthorized: false)
        }
        server.addHandler("GET /reference/catalog/CARS/suggest *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "offer_edit_get_suggestions", userAuthorized: false)
        }
        server.addHandler("GET /user/draft/CARS") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! PanoramaTests.draftResponse.jsonUTF8Data(), userAuthorized: false)
        }

        try! server.start()
    }

    @discardableResult
    func realTestFixedTimeScreenshoot(snapshotId: String) -> PanoramaSteps {
        let steps = mainSteps
        .openOffersTab()
        .tapAddOffer()
        .tapToCarsCategory()
        .tapPanorama()
        .wait(for: 2)
        .closeHelp()
        .wait(for: 1)
        .rotateToPortrait()
        .wait(for: 1)
        .rotateToLandscape()
        .wait(for: 1)
        .tapRecordButton()
        .wait(for: 3)

        validateSnapshots(snapshotId: snapshotId)

        return steps
    }

    @discardableResult
    func realTestRecordScreenshoot(snapshotId: String, finalWaitTime: UInt32) -> PanoramaSteps {
        let steps = mainSteps
        .openOffersTab()
        .tapAddOffer()
        .tapToCarsCategory()
        .tapPanorama()
        .wait(for: 2)
        .closeHelp()
        .wait(for: 1)
        .rotateToPortrait()
        .wait(for: 1)
        .rotateToLandscape()
        .wait(for: 1)
        .tapRecordButton()
        .wait(for: finalWaitTime)

        validateSnapshots(snapshotId: snapshotId)

        return steps
    }

    private func validateSnapshots(snapshotId: String) {
        let snapshotId = SnapshotIdentifier(suite: suiteName, identifier: snapshotId)
        let fullScreenshot = XCUIScreen.main.screenshot().image

        Snapshot.compareWithSnapshot(image: fullScreenshot, identifier: snapshotId)
    }
}

/// @depends_on AutoRuWizardPickers
final class Panorama4SecondsTests: PanoramaTests {
    override func setUp() {
        super.setUp()
        app.launchArguments.append("panoramaTestSpecificTime:\(4.0)")

        launch()
    }

    func test_PanoramaFullRotation4Seconds() {
        realTestFixedTimeScreenshoot(snapshotId: #function)

        let labelElement = app.staticTexts["panorama.record.carEdgeLabel"]
        XCTAssertEqual(labelElement.label, "side")
    }
}

/// @depends_on AutoRuWizardPickers
final class Panorama11SecondsTests: PanoramaTests {
    override func setUp() {
        super.setUp()
        app.launchArguments.append("panoramaTestSpecificTime:\(11.0)")

        launch()
    }

    func test_PanoramaFullRotation11Seconds() {
        realTestFixedTimeScreenshoot(snapshotId: #function)

        let labelElement = app.staticTexts["panorama.record.carEdgeLabel"]
        XCTAssertEqual(labelElement.label, "front_3_4")
    }
}

/// @depends_on AutoRuWizardPickers
final class Panorama19SecondsTests: PanoramaTests {
    override func setUp() {
        super.setUp()
        app.launchArguments.append("panoramaTestSpecificTime:\(19.0)")

        launch()
    }

    func test_PanoramaFullRotation19Seconds() {
        realTestFixedTimeScreenshoot(snapshotId: #function)

        let labelElement = app.staticTexts["panorama.record.carEdgeLabel"]
        XCTAssertEqual(labelElement.label, "front")
    }
}

/// @depends_on AutoRuWizardPickers
final class Panorama23SecondsTests: PanoramaTests {
    override func setUp() {
        super.setUp()
        app.launchArguments.append("panoramaTestSpecificTime:\(23.0)")

        launch()
    }

    func test_PanoramaFullRotation23Seconds() {
        realTestFixedTimeScreenshoot(snapshotId: #function)
        .exist(selector: "panorama.record.carEdgeLabel")

        let labelElement = app.staticTexts["panorama.record.carEdgeLabel"]
        XCTAssertEqual(labelElement.label, "front_3_4")
    }
}

/// @depends_on AutoRuWizardPickers
final class Panorama37SecondsTests: PanoramaTests {
    override func setUp() {
        super.setUp()
        app.launchArguments.append("panoramaTestSpecificTime:\(37.0)")

    }

    func test_PanoramaFullRotation37Seconds() {
        realTestFixedTimeScreenshoot(snapshotId: #function)
        .exist(selector: "panorama.record.carEdgeLabel")

        let labelElement = app.staticTexts["panorama.record.carEdgeLabel"]
        XCTAssertEqual(labelElement.label, "side")
    }
}

/// @depends_on AutoRuWizardPickers
final class Panorama45SecondsTests: PanoramaTests {
    override func setUp() {
        super.setUp()
        app.launchArguments.append("panoramaTestSpecificTime:\(45.0)")

        launch()
    }

    func test_PanoramaFullRotation45Seconds() {
        realTestFixedTimeScreenshoot(snapshotId: #function)
        .exist(selector: "panorama.record.carEdgeLabel")

        let labelElement = app.staticTexts["panorama.record.carEdgeLabel"]
        XCTAssertEqual(labelElement.label, "back_3_4")
    }
}

/// @depends_on AutoRuWizardPickers
final class Panorama49SecondsTests: PanoramaTests {
    override func setUp() {
        super.setUp()
        app.launchArguments.append("panoramaTestSpecificTime:\(49.0)")

        launch()
    }

    func test_PanoramaFullRotation49Seconds() {
        realTestFixedTimeScreenshoot(snapshotId: #function)
        .exist(selector: "panorama.record.carEdgeLabel")

        let labelElement = app.staticTexts["panorama.record.carEdgeLabel"]
        XCTAssertEqual(labelElement.label, "back")
    }
}

/// @depends_on AutoRuWizardPickers
final class Panorama54SecondsTests: PanoramaTests {
    override func setUp() {
        super.setUp()
        app.launchArguments.append("panoramaTestSpecificTime:\(54.0)")

        launch()
    }

    func test_PanoramaFullRotation54Seconds() {
        realTestFixedTimeScreenshoot(snapshotId: #function)
        .exist(selector: "panorama.record.carEdgeLabel")

        let labelElement = app.staticTexts["panorama.record.carEdgeLabel"]
        XCTAssertEqual(labelElement.label, "back_3_4")
    }
}

/// @depends_on AutoRuWizardPickers
final class Panorama63SecondsTests: PanoramaTests {
    override func setUp() {
        super.setUp()
        app.launchArguments.append("panoramaTestSpecificTime:\(63.0)")

        launch()
    }

    func test_PanoramaFullRotation63Seconds() {
        realTestFixedTimeScreenshoot(snapshotId: #function)
        .exist(selector: "panorama.record.carEdgeLabel")

        let labelElement = app.staticTexts["panorama.record.carEdgeLabel"]
        XCTAssertEqual(labelElement.label, "side")
    }
}
/*
// Такой вариант тестов можно использовать для проверок долгих процессов, например, алгоритма, определяющего ракурсы, фильтруя ошибки ML, или создания множества скринов в течение одной сессии - оставлю для образца. Заготовки в коде приложения для работы с ним есть.
final class PanoramaTestRecord64Seconds: PanoramaTest {
    override func setUp() {
        super.setUp()
        let videoDirectory = URL(fileURLWithPath: #file)
        .deletingLastPathComponent()
        .deletingLastPathComponent()
        .appendingPathComponent("Resources")
        .appendingPathComponent("testPanoramaFull")
        .appendingPathExtension("mov")
        
        app.launchArguments.append("panoramaVideo:\(videoDirectory)")
        app.launchArguments.append("panoramaFrameInterval:\(1.0)")
        app.launchArguments.append("panoramaMaxFrameCounter:\(64)")
        
    }
    
    func test_PanoramaFullRotationRecord64Seconds() {
        realTestRecordScreenshoot(snapshotId: #function, finalWaitTime: 67)
    }
}
*/
