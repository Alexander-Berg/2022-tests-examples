//
//  HashCalculatorTests.swift
//  YandexDisk
//
//  Created by Alexander Sinusov on 17.08.16.
//  Copyright © 2016 Yandex. All rights reserved.
//

import XCTest
#if !DEV_TEST
@testable import YandexDisk
#endif

class HashCalculatorTests: XCTestCase {
    let testString = "String for hashing"
    let testStringMD5 = "94d49f8845d5f628fb030b2d7fc3b8be"
    let testStringSHA1 = "76adbf82e8ebbe4e3f37ec3837ca4144f7e46ed0"
    let testStringSHA256 = "7f18f714dd192910c51c52e455a66731e4da365b25dca8031df6eb840c4b5466"

    func testChunkGeneratorHashing() {
        let chunkSequence = AnySequence(AnyIterator(DummyChunkGenerator(data: testString.data(using: .utf8)!)))
        let hashes = try? HashCalculator.calculate(
            chunkSequence,
            algorithm: HashingAlgorithm(hashTypes: [.MD5, .SHA1, .SHA256]),
            stopCondition: { return false }
        ) { _ in }
        compareResult(hashes)
    }

    func testMd5AndSha256ChunkGeneratorHashing() {
        let chunkSequence = AnySequence(AnyIterator(DummyChunkGenerator(data: testString.data(using: .utf8)!)))
        let hashes = try? HashCalculator.calculateMD5AndSHA256(
            chunkSequence,
            stopCondition: { return false }
        ) { _ in }
        compareObjCResult(hashes)
    }

    func testChunkCallback() {
        let chunkSequence = AnySequence(AnyIterator(DummyChunkGenerator(data: testString.data(using: .utf8)!)))
        var callbackData = Data()
        _ = try? HashCalculator.calculateMD5AndSHA256(chunkSequence, stopCondition: { return false }) { data in
            callbackData.append(Data(bytes: data, count: data.count))
        }
        XCTAssertEqual(callbackData, testString.data(using: .utf8))
    }

    func testChunkGeneratorHashingError() {
        let expectation = self.expectation(description: "Should throw error")
        let chunkSequence = AnySequence(AnyIterator(DummyErrorChunkGenerator()))
        do {
            _ = try HashCalculator.calculate(
                chunkSequence,
                algorithm: HashingAlgorithm(hashTypes: [.MD5, .SHA1, .SHA256]),
                stopCondition: { return false }
            ) { _ in }
        } catch {
            checkError(error)
            expectation.fulfill()
        }

        waitForExpectations(timeout: 0.5.seconds, handler: nil)
    }

    func testObjCChunkGeneratorHashingError() {
        let expectation = self.expectation(description: "Should throw error")
        let chunkSequence = AnySequence(AnyIterator(DummyErrorChunkGenerator()))
        do {
            _ = try HashCalculator.calculateMD5AndSHA256(chunkSequence, stopCondition: { return false }) { _ in }
        } catch {
            checkError(error)
            expectation.fulfill()
        }

        waitForExpectations(timeout: 0.5.seconds, handler: nil)
    }

    fileprivate func checkError(_ error: Error) {
        guard let error = error as? HashError else {
            XCTAssertTrue(false)
            return
        }

        XCTAssertEqual(error, HashError.dataReadingError)
    }

    fileprivate func compareResult(_ result: [HashType: HashingResult]?) {
        XCTAssertNotNil(result, "Result is nil!")
        guard let result = result else {
            return
        }

        XCTAssertEqual(result[.MD5]?.type, .MD5)
        XCTAssertEqual(result[.MD5]?.digest, testStringMD5)
        XCTAssertEqual(result[.MD5]?.size, UInt64(testString.lengthOfBytes(using: .utf8)))

        XCTAssertEqual(result[.SHA1]?.type, .SHA1)
        XCTAssertEqual(result[.SHA1]?.digest, testStringSHA1)
        XCTAssertEqual(result[.SHA1]?.size, UInt64(testString.lengthOfBytes(using: .utf8)))

        XCTAssertEqual(result[.SHA256]?.type, .SHA256)
        XCTAssertEqual(result[.SHA256]?.digest, testStringSHA256)
        XCTAssertEqual(result[.SHA256]?.size, UInt64(testString.lengthOfBytes(using: .utf8)))
    }

    fileprivate func compareObjCResult(_ result: [String: HashingResult]?) {
        XCTAssertNotNil(result, "Result is nil!")
        guard let result = result else {
            return
        }

        XCTAssertEqual(result[YDHashTypeHelper.MD5]?.type, .MD5)
        XCTAssertEqual(result[YDHashTypeHelper.MD5]?.digest, testStringMD5)
        XCTAssertEqual(result[YDHashTypeHelper.MD5]?.size, UInt64(testString.lengthOfBytes(using: .utf8)))

        XCTAssertEqual(result[YDHashTypeHelper.SHA256]?.type, .SHA256)
        XCTAssertEqual(result[YDHashTypeHelper.SHA256]?.digest, testStringSHA256)
        XCTAssertEqual(result[YDHashTypeHelper.SHA256]?.size, UInt64(testString.lengthOfBytes(using: .utf8)))
    }

    struct DummyChunkGenerator: IteratorProtocol {
        fileprivate let chunkSize = 3
        fileprivate var data: Data
        fileprivate var offset = 0

        init(data: Data) {
            self.data = data
        }

        mutating func next() -> Result<[UInt8], HashError>? {
            if offset >= data.count {
                return nil
            }

            let len = offset + chunkSize < data.count ? chunkSize : ((offset + chunkSize) - data.count + 1)
            let buffer = data.subdata(in: offset ..< offset + len)
            let bytesRead = buffer.count
            offset = offset + bytesRead
            let array = buffer.withUnsafeBytes {
                [UInt8](UnsafeBufferPointer(start: $0, count: buffer.count))
            }
            return .success(array)
        }
    }

    struct DummyErrorChunkGenerator: IteratorProtocol {
        func next() -> Result<[UInt8], HashError>? {
            return .failure(HashError.dataReadingError)
        }
    }
}
