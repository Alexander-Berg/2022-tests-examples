//
//  DeviceExperimentsEndpointMapperTests.swift
//  YREWeb-Unit-Tests
//
//  Created by Leontyev Saveliy on 14.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREWeb

// swiftlint:disable line_length

final class DeviceExperimentsEndpointMapperTests: XCTestCase {
    func testDefaultResult() {
        let jsonObject: [AnyHashable: Any] = [
            "encodedFlags": [
                "W3siSEFORExFUiI6IlJFQUxUWV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiUkVBTFRZX0FQUCI6eyJzYW1lX3NpemVfYmx1ZV95ZWxsb3ciOiJ0cnVlIn19fSwiVEVTVElEIjpbIjQzNzM0NCJdfV0=",
                "W3siSEFORExFUiI6IlJFQUxUWV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiUkVBTFRZX0FQUCI6eyJleHRlbmRlZF9zZWFyY2giOiJ0cnVlIn19fSwiVEVTVElEIjpbIjM5OTk5MCJdfV0=",
                "W3siSEFORExFUiI6IlJFQUxUWV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiUkVBTFRZX0FQUCI6eyJuZXdfc25pcHBldF8xc3Rfc3RhZ2UiOiJ0cnVlIn19fSwiVEVTVElEIjpbIjM5ODg3NCJdfV0=",
            ],
            "configVersion": "666",
            "boxes": [
                "437344,0,-1",
                "399990,0,-1",
                "398874,0,-1",
            ]
        ]

        let boxesIDs: Set<Int> = [437344, 399990, 398874]

        let mapper = DeviceExperimentsEndpointMapper()
        let result = mapper.responseObject(jsonObject: jsonObject)

        switch result {
            case .success(let set):
                XCTAssertEqual(set.configVersion, "666")
                set.boxes.forEach { box in
                    XCTAssertTrue(boxesIDs.contains(box.id.rawValue))
                }
            case .failure(let error):
                XCTFail("Mapper failed with error \(error.localizedDescription)")
        }
    }

    func testExtraFlagsResult() {
        let jsonObject: [AnyHashable: Any] = [
            "encodedFlags": [
                "W3siSEFORExFUiI6IlJFQUxUWV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiUkVBTFRZX0FQUCI6eyJzYW1lX3NpemVfYmx1ZV95ZWxsb3ciOiJ0cnVlIn19fSwiVEVTVElEIjpbIjQzNzM0NCJdfV0=",
                "W3siSEFORExFUiI6IlJFQUxUWV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiUkVBTFRZX0FQUCI6eyJleHRlbmRlZF9zZWFyY2giOiJ0cnVlIn19fSwiVEVTVElEIjpbIjM5OTk5MCJdfV0=",
                "W3siSEFORExFUiI6IlJFQUxUWV9BUFAiLCJDT05URVhUIjp7Ik1BSU4iOnsiUkVBTFRZX0FQUCI6eyJuZXdfc25pcHBldF8xc3Rfc3RhZ2UiOiJ0cnVlIn19fSwiVEVTVElEIjpbIjM5ODg3NCJdfV0=",
            ],
            "configVersion": "666",
            "boxes": [
                "437344,0,-1",
                "398874,0,-1",
            ]
        ]

        let boxesIDs: Set<Int> = [437344, 398874]

        let mapper = DeviceExperimentsEndpointMapper()
        let result = mapper.responseObject(jsonObject: jsonObject)

        switch result {
            case .success(let set):
                XCTAssertEqual(set.configVersion, "666")
                set.boxes.forEach { box in
                    XCTAssertTrue(boxesIDs.contains(box.id.rawValue))
                }
                XCTAssertNil(set.boxes.first(where: { $0.id.rawValue == 399990 }))

            case .failure(let error):
                XCTFail("Mapper failed with error \(error.localizedDescription)")
        }
    }
}
