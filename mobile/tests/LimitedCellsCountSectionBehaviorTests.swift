//
//  LimitedCellsCountSectionBehaviorTests.swift
//  YandexMapsTests
//
//  Created by Dmitry Trimonov on 24/11/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation
import XCTest
import YandexMapsDirections
import YandexMapsUI

fileprivate class TestCellInteractor { }

fileprivate class TestCellViewModel: CommonViewModel, CellViewModel {
    typealias CellInteractorType = TestCellInteractor
    required init(interactor: TestCellInteractor) { }
}

fileprivate class TestLimitedCellsCountSectionBehaviorInteractor: LimitedCellsCountSectionBehaviorInteractor {


    typealias CellInteractorType = TestCellInteractor

    init(onCellTapBlock: ((TestCellInteractor, Int) -> Void)? = nil, openMoreBlock: (() -> Void)? = nil) {
        self.onCellTapBlock = onCellTapBlock
        self.openMoreBlock = openMoreBlock
    }

    func onCellTap(cellInteractor: TestCellInteractor, at position: Int) {
        onCellTapBlock?(cellInteractor, position)
    }

    func openMore() {
        openMoreBlock?()
    }

    private let onCellTapBlock: ((TestCellInteractor, Int) -> Void)?
    private let openMoreBlock: (() -> Void)?
}


class LimitedCellsCountSectionBehaviorTests: XCTestCase {
    fileprivate typealias Sut = LimitedCellsCountSectionBehavior<TestCellViewModel, TestLimitedCellsCountSectionBehaviorInteractor>

    // MARK: - test cases

    func testTapOnLastItemWhenThereIs3ItemsAndLimitIsEqualTo3InvokesOnCellTapWithCorrectParametr() {
        var wasOpenMoreTapped = false
        let behaviorInteractor = TestLimitedCellsCountSectionBehaviorInteractor(openMoreBlock: {
            wasOpenMoreTapped = true
        })
        let sut = Sut(interactor: behaviorInteractor, cellsLimit: 3, openMoreCellTitle: "")
        let cellInteractors = Array<TestCellInteractor>(repeating: TestCellInteractor(), count: 3)
        sut.update(with: cellInteractors)
        sut.onCellTap(index: 2)
        XCTAssert(!wasOpenMoreTapped)
    }

    // MARK: - default tests

    func testMakeViewModelsReturnsVMCountEqualToCellsLimitWhenCellInteractorsCountIsMoreThanCellsLimit() {
        let behaviorInteractor = TestLimitedCellsCountSectionBehaviorInteractor()
        let sut = Sut(interactor: behaviorInteractor, cellsLimit: 3, openMoreCellTitle: "")
        let cellInteractors = Array<TestCellInteractor>(repeating: TestCellInteractor(), count: 4)
        sut.update(with: cellInteractors)
        let actual = sut.makeViewModels().count
        XCTAssertTrue(actual == 3) // 2 cells + 1 cell (open more)
    }

    func testMakeViewModelsReturnsVMCountEqualToCellInteractorsCountWhenCellInteractorsCountIsLessThanCellsLimit() {
        let behaviorInteractor = TestLimitedCellsCountSectionBehaviorInteractor()
        let sut = Sut(interactor: behaviorInteractor, cellsLimit: 11, openMoreCellTitle: "")
        let cellInteractors = Array<TestCellInteractor>(repeating: TestCellInteractor(), count: 9)
        sut.update(with: cellInteractors)
        let actual = sut.makeViewModels().count
        XCTAssertTrue(actual == 9)
    }

    func testMakeViewModelsReturnsVMCountEqualToCellInteractorsCountWhenCellInteractorsCountIsEqualToCellsLimit() {
        let behaviorInteractor = TestLimitedCellsCountSectionBehaviorInteractor()
        let sut = Sut(interactor: behaviorInteractor, cellsLimit: 5, openMoreCellTitle: "")
        let cellInteractors = Array<TestCellInteractor>(repeating: TestCellInteractor(), count: 5)
        sut.update(with: cellInteractors)
        let actual = sut.makeViewModels().count
        XCTAssertTrue(actual == 5)
    }

    func testTapOnOpenMoreCellInvokesOpenMore() {
        var wasOpenMoreTapped = false
        let behaviorInteractor = TestLimitedCellsCountSectionBehaviorInteractor(openMoreBlock: {
            wasOpenMoreTapped = true
        })
        let sut = Sut(interactor: behaviorInteractor, cellsLimit: 5, openMoreCellTitle: "")
        let cellInteractors = Array<TestCellInteractor>(repeating: TestCellInteractor(), count: 100)
        sut.update(with: cellInteractors)

        sut.onCellTap(index: 4)
        XCTAssertTrue(wasOpenMoreTapped)
    }

    func testTapOnDataCellWhenThereIsOpenMoreCellDoesntInvokeOpenMore() {
        var wasOpenMoreTapped = false
        let behaviorInteractor = TestLimitedCellsCountSectionBehaviorInteractor(openMoreBlock: {
            wasOpenMoreTapped = true
        })
        let sut = Sut(interactor: behaviorInteractor, cellsLimit: 5, openMoreCellTitle: "")
        let cellInteractors = Array<TestCellInteractor>(repeating: TestCellInteractor(), count: 100)
        sut.update(with: cellInteractors)

        sut.onCellTap(index: 3)
        XCTAssertFalse(wasOpenMoreTapped)
    }

    func testTapOnDataCellWhenThereIsNoOpenMoreCellInvokesOnCellTapWithCorrectParametr() {
        var expected: TestCellInteractor?
        let behaviorInteractor = TestLimitedCellsCountSectionBehaviorInteractor(onCellTapBlock: { x, position in
            expected = x
        })
        let sut = Sut(interactor: behaviorInteractor, cellsLimit: 50, openMoreCellTitle: "")
        let actual = TestCellInteractor()
        let cellInteractors = [actual] + Array<TestCellInteractor>(repeating: TestCellInteractor(), count: 10)
        sut.update(with: cellInteractors)
        XCTAssertNil(expected)

        sut.onCellTap(index: 0)
        XCTAssertTrue(actual === expected)
    }

    func testTapOnDataCellWhenThereIsOpenMoreCellInvokesOnCellTapWithCorrectParametr() {
        var expected: TestCellInteractor?
        let behaviorInteractor = TestLimitedCellsCountSectionBehaviorInteractor(onCellTapBlock: { x, position in
            expected = x
        })
        let sut = Sut(interactor: behaviorInteractor, cellsLimit: 5, openMoreCellTitle: "")
        let actual = TestCellInteractor()
        let cellInteractors = [actual] + Array<TestCellInteractor>(repeating: TestCellInteractor(), count: 10)
        sut.update(with: cellInteractors)
        XCTAssertNil(expected)

        sut.onCellTap(index: 0)
        XCTAssertTrue(actual === expected)
    }
}
