//
//  FilterStateTests.swift
//  
//
//  Created by Dmitrii Chikovinskii on 29.04.2022.
//

import XCTest
@testable import MedicineCore

class FilterStateTests: XCTestCase {
    var store: EnvironmentStore!

    var state: AppState {
        self.store.store.state
    }

    override func setUp() {
        super.setUp()
        self.store = makeEnvStore(middleware: [

        ])
    }
    
    func testFilterLevel() throws {
//        store.graph.dispatch(DoctorFilterAction.selectExp(idx: [0]))
//        XCTAssertTrue(store.graph.filterNode.exp.active)
//        
//        store.graph.dispatch(DoctorFilterAction.selectExp(idx: []))
//        XCTAssertFalse(store.graph.filterNode.exp.active)
    }
}
