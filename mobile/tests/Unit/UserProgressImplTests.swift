//
//  UserProgressImplTests.swift
//  YandexMapsPersonalAccount
//
//  Created by Yury Potapov on 08/04/2019.
//

import XCTest
@testable import YandexMapsPersonalAccount

class UserProgressImplTests: XCTestCase {
    private let levelPoints: [Int] = [0, 10, 20, 30, 40]
    
    func testZero() {
        let progress = UserProgressImpl(totalScore: 0, levelPoints: levelPoints)
        
        assert(progress.score == 0)
        assert(progress.level == 1)
        assert(progress.progress == 0.0)
        assert(progress.nextLevelScore == 10)
    }
    
    func testOverMax() {
        let progress = UserProgressImpl(totalScore: 55, levelPoints: levelPoints)
        
        assert(progress.score == 15)
        assert(progress.level == 5)
        assert(progress.progress == 1.0)
        assert(progress.nextLevelScore == nil)
    }
    
    func testAddScoreInsideLevel() {
        let progress = UserProgressImpl(totalScore: 10, levelPoints: levelPoints)
        
        assert(progress.score == 0)
        assert(progress.level == 2)
        assert(progress.progress == 0.0)
        assert(progress.nextLevelScore == 10)
        
        progress.addScore(5)
        
        assert(progress.score == 5)
        assert(progress.level == 2)
        assert(progress.progress == 0.5)
        assert(progress.nextLevelScore == 10)
    }
    
    func testAddScoreIncreaseLevel() {
        let progress = UserProgressImpl(totalScore: 5, levelPoints: levelPoints)
        
        assert(progress.score == 5)
        assert(progress.level == 1)
        assert(progress.progress == 0.5)
        assert(progress.nextLevelScore == 10)
        
        progress.addScore(10)
        
        assert(progress.score == 5)
        assert(progress.level == 2)
        assert(progress.progress == 0.5)
        assert(progress.nextLevelScore == 10)
    }
    
    func testAddScoreIncreaseMultipleLevels() {
        let progress = UserProgressImpl(totalScore: 15, levelPoints: levelPoints)
        
        assert(progress.score == 5)
        assert(progress.level == 2)
        assert(progress.progress == 0.5)
        assert(progress.nextLevelScore == 10)
        
        progress.addScore(15)
        
        assert(progress.score == 0)
        assert(progress.level == 4)
        assert(progress.progress == 0.0)
        assert(progress.nextLevelScore == 10)
    }
    
}
