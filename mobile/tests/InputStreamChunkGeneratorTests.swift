//
//  InputStreamChunkGeneratorTests.swift
//  YandexDisk
//
//  Created by Alexander Sinusov on 17.08.16.
//  Copyright © 2016 Yandex. All rights reserved.
//

import XCTest
#if !DEV_TEST
@testable import YandexDisk
#endif

class InputStreamChunkGeneratorTests: XCTestCase {
    let testData = "String for hashing".data(using: String.Encoding.utf8)!
    var stream: InputStream!

    override func setUp() {
        super.setUp()
        stream = InputStream(data: testData)
        stream.open()
    }

    override func tearDown() {
        stream.close()
        super.tearDown()
    }

    func testCalculateHashes() {
        let chunkSequence =
            AnySequence(
                AnyIterator(
                    InputStreamChunkGenerator(inputStream: stream, maxChunkSize: 4, stopCondition: { return false })
                )
            )
        let dataFromGenerator = chunkSequence.reduce(NSMutableData()) {
            switch $1 {
            case let .success(data):
                $0.append(data, length: data.count)
            case let .failure(error):
                XCTAssertThrowsError(error)
            }
            return $0
        }
        XCTAssertEqual(dataFromGenerator as Data, testData)
    }
}
