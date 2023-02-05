//
//  AsyncTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 24.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class AsyncTests: XCTestCase {
    
    typealias SimpleAsync = Async<String>
    
//    private func makeSimpleAsync(produceError error: Bool = false) -> SimpleAsync {
//        return SimpleAsync(
//            fetch: { fullfill in
//                let impl = NSBlockOperation {
//                    if error {
//                        fullfill("error")
//                    }
//                    else {
//                        fullfill("success")
//                    }
//                }
//                NSOperationQueue.mainQueue().addOperation(impl)
//                return impl
//            },
//            cancel: { impl in
//                (impl as! NSBlockOperation).cancel()
//            }
//        )
//
//    }
//    
//    
//    func testThatAsyncnDeletesImplementationInCompletion() {
//        for error in [true, false] {
//            let exp = expectation(description: "")
//            
//            let op = makeSimpleAsync(produceError: error)
//            
//            op.completion = { [weak op] res in
//                XCTAssertNotNil(op)
//                XCTAssertNil(op!.privateInterface._operation)
//                
//                exp.fulfill()
//            }
//            op.start()
//            waitForExpectationsWithTimeout(0.5, handler: nil)
//            
//            XCTAssertNil(op.privateInterface._operation)
//            XCTAssertNotNil(op.result)
//        }
//    }
//    
//    func testThatAsyncDoesNotIntroduceRetainCycles() {
//        for err in [true, false] {
//            var op: SimpleAsync? = makeSimpleAsync(produceError: err)
//            weak var weakOp = op
//            
//            let exp = expectation(description: "")
//            op?.completion = { _ in
//                exp.fulfill()
//            }
//            op?.start()
//            waitForExpectationsWithTimeout(0.5, handler: nil)
//            
//            XCTAssertNotNil(weakOp)
//            op = nil
//            XCTAssertNil(weakOp)
//        }
//    }
//    
//    func testThatAsyncDoesNotCallComletionWhenCanceled() {
//        for err in [true, false] {
//            var op: SimpleAsync? = makeSimpleAsync(produceError: err)
//            weak var weakOp = op
//            
//            let exp = expectation(description: "")
//            op?.completion = { _ in
//                XCTFail()
//                exp.fulfill()
//            }
//            dispatch(async: .Main) {
//                op!.cancel()
//                
//                XCTAssertNil(op!.privateInterface._operation)
//                exp.fulfill()
//            }
//            op?.start()
//            waitForExpectationsWithTimeout(0.5, handler: nil)
//            
//            XCTAssertNotNil(weakOp)
//            op = nil
//            XCTAssertNil(weakOp)
//        }
//    }
    
}
