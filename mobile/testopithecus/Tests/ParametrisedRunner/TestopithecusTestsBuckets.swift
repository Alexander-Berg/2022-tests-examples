//
// Created by Artem Zoshchuk on 06.05.2020.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import testopithecus

open class TestopithecusTestsParallel0: TestopithecusTests {

    public override class var bucketIndexIncrement: Int32 {
        return 0
    }
}

open class TestopithecusTestsParallel1: TestopithecusTests {

    public override class var bucketIndexIncrement: Int32 {
        return 1
    }
}

open class TestopithecusTestsParallel2: TestopithecusTests {

    public override class var bucketIndexIncrement: Int32 {
        return 2
    }
}

open class TestopithecusTestsParallel3: TestopithecusTests {

    public override class var bucketIndexIncrement: Int32 {
        return 3
    }
}

open class TestopithecusTestsParallel4: TestopithecusTests {

    public override class var bucketIndexIncrement: Int32 {
        return 4
    }
}

open class TestopithecusTestsParallel5: TestopithecusTests {

    public override class var bucketIndexIncrement: Int32 {
        return 5
    }
}
