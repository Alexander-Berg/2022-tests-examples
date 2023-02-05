//
//  FetchOperationTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 16/06/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class FetchOperationTests: XCTestCase {

   typealias SimpleFetchOp = FetchOperation<String, String>

   private func makeSimpleOperation(produceError error: Bool = false) -> SimpleFetchOp {
       return SimpleFetchOp(
           fetch: { fullfill in
               let impl = BlockOperation {
                   if error {
                       fullfill(.err("error"))
                   }
                   else {
                       fullfill(.ok("success"))
                   }
               }
               OperationQueue.main.addOperation(impl)
               return impl
           },
           cancel: { impl in
               (impl as! BlockOperation).cancel()
           }
       )
   }

   func testThatFetchOperationDeletesImplementationInCompletion() {
       for error in [true, false] {
           let exp = expectation(description: "")

           let op = makeSimpleOperation(produceError: error)

           op.completion = { [weak op] res in
               XCTAssertNotNil(op)
               XCTAssertNil(op!.privateInterface._operation)

               exp.fulfill()
           }
           op.start()
           waitForExpectations(timeout: 0.5, handler: nil)

           XCTAssertNil(op.privateInterface._operation)
           XCTAssertNotNil(op.result)
       }
   }

   func testThatFetchOperationDoesNotIntroduceRetainCycles() {
       for err in [true, false] {
           var op: SimpleFetchOp? = makeSimpleOperation(produceError: err)
           weak var weakOp = op

           let exp = expectation(description: "")
           op?.completion = { _ in
               exp.fulfill()
           }
           op?.start()
           waitForExpectations(timeout: 0.5, handler: nil)

           XCTAssertNotNil(weakOp)
           op = nil
           XCTAssertNil(weakOp)
       }
   }

   func testThatFetchOperationDoesNotCallComletionWhenCanceled() {
       for err in [true, false] {
           var op: SimpleFetchOp? = makeSimpleOperation(produceError: err)
           weak var weakOp = op

           let exp = expectation(description: "")
           op?.completion = { _ in
               XCTFail()
               exp.fulfill()
           }
           dispatch(async: .main) {
               op!.cancel()

               XCTAssertNil(op!.privateInterface._operation)
               exp.fulfill()
           }
           op?.start()
           waitForExpectations(timeout: 0.5, handler: nil)

           XCTAssertNotNil(weakOp)
           op = nil
           XCTAssertNil(weakOp)
       }
   }
}
