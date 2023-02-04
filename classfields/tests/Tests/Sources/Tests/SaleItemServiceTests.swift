//
//  SaleItemServiceTests.swift
//  Tests
//
//  Created by Vitalii Stikhurov on 15.03.2021.
//

import XCTest
import AutoRuProtobuf
import AutoRuProtoModels
import AutoRuModels
import SwiftProtobuf

@testable import AutoRuServices
class SaleItemServiceTests: BaseUnitTest {
    struct Distance: Equatable {
        var count: Int
        var radius: Int
        init(_ count: Int, _ radius: Int) {
            self.count = count
            self.radius = radius
        }
    }

    func testCompress_AllUniq() throws {
        let distances: [Distance] = [
            .init(1, 100),
            .init(2, 200),
            .init(3, 300),
            .init(4, 400),
            .init(5, 500),
            .init(6, 600),
            .init(7, 700),
            .init(8, 800),
            .init(9, 900),
            .init(10, 1000)
        ]
        XCTAssertEqual(distances.compress(by: \.count,
                                          deleteAnyWay: nil,
                                          neverDelete: nil),
                       distances)
    }

    func testCompress_checkDelete() throws {
        let geoRadius = 100
        let distances: [Distance] = [
            .init(1, 100),
            .init(2, 200),
            .init(3, 300),
            .init(4, 400),
            .init(5, 500),
            .init(6, 600),
            .init(7, 700),
            .init(8, 800),
            .init(9, 900),
            .init(10, 1000)
        ]
        XCTAssert(!distances.compress(by: \.count,
                                      deleteAnyWay: ({ $0.count == 6 }),
                                      neverDelete: ({ $0.radius == geoRadius }))
                    .contains(.init(6, 600))
        )
        XCTAssert(distances.compress(by: \.count,
                                     deleteAnyWay: ({ $0.count == 6 }),
                                     neverDelete: ({ $0.radius == geoRadius })).count == 9
        )
    }

    func testCompress_checkCompress() throws {
        let distances: [Distance] = [
            .init(1, 100),
            .init(2, 200),
            .init(2, 300),
            .init(2, 400),
            .init(2, 500),
            .init(6, 600),
            .init(7, 700),
            .init(8, 800),
            .init(9, 900),
            .init(10, 1000)
        ]
        XCTAssertEqual(distances.compress(by: \.count,
                                          deleteAnyWay: nil,
                                          neverDelete: nil),
                       [
                        .init(1, 100),
                        .init(2, 200),
                        .init(6, 600),
                        .init(7, 700),
                        .init(8, 800),
                        .init(9, 900),
                        .init(10, 1000)
                       ])

        XCTAssertEqual(distances.compress(by: \.count,
                                          deleteAnyWay: nil,
                                          neverDelete: {$0.radius == 400}),
                       [
                        .init(1, 100),
                        .init(2, 400),
                        .init(6, 600),
                        .init(7, 700),
                        .init(8, 800),
                        .init(9, 900),
                        .init(10, 1000)
                       ])
        XCTAssertEqual(distances.compress(by: \.count,
                                          deleteAnyWay: nil,
                                          neverDelete: {$0.radius == 200}),
                       [
                        .init(1, 100),
                        .init(2, 200),
                        .init(6, 600),
                        .init(7, 700),
                        .init(8, 800),
                        .init(9, 900),
                        .init(10, 1000)
                       ])

        XCTAssertEqual(distances.compress(by: \.count,
                                          deleteAnyWay: nil,
                                          neverDelete: {$0.radius == 500}),
                       [
                        .init(1, 100),
                        .init(2, 500),
                        .init(6, 600),
                        .init(7, 700),
                        .init(8, 800),
                        .init(9, 900),
                        .init(10, 1000)
                       ])

        XCTAssertEqual(distances.compress(by: \.count,
                                          deleteAnyWay: nil,
                                          neverDelete: {$0.radius == 100}),
                       [
                        .init(1, 100),
                        .init(2, 200),
                        .init(6, 600),
                        .init(7, 700),
                        .init(8, 800),
                        .init(9, 900),
                        .init(10, 1000)
                       ])
    }

    func testCompress_checkCompress_neverDeletedFirst() throws {
        let distances: [Distance] = [.init(2, 100),
                                     .init(2, 200),
                                     .init(2, 300),
                                     .init(2, 400),
                                     .init(2, 500),
                                     .init(6, 600),
                                     .init(7, 700),
                                     .init(8, 800),
                                     .init(9, 900),
                                     .init(10, 1000)]

        XCTAssertEqual(
            distances.compress(by: \.count,
                               deleteAnyWay: nil,
                               neverDelete: {$0.radius == 100}),
            [.init(2, 100),
             .init(6, 600),
             .init(7, 700),
             .init(8, 800),
             .init(9, 900),
             .init(10, 1000)]
        )
    }

    func testCompress_checkCompress_neverDeletedLast() throws {
        let distances: [Distance] = [.init(1, 100),
                                     .init(2, 200),
                                     .init(2, 300),
                                     .init(2, 400),
                                     .init(2, 500),
                                     .init(6, 600),
                                     .init(7, 700),
                                     .init(8, 800),
                                     .init(9, 900),
                                     .init(2, 1000)]

        XCTAssertEqual(
            distances.compress(by: \.count,
                               deleteAnyWay: nil,
                               neverDelete: {$0.radius == 1000}),
            [.init(1, 100),
             .init(6, 600),
             .init(7, 700),
             .init(8, 800),
             .init(9, 900),
             .init(2, 1000)]
        )
    }
}
