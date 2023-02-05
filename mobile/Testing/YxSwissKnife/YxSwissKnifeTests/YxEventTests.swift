import XCTest
@testable import YxSwissKnife

class YxEventTests: XCTestCase {

    override func setUp() {
        super.setUp()
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }

    func testInvokation() {
        let exp = expectation(description: "event invokation")
        let event = YxEvent<Int>()
        event += { val in
            XCTAssert(val == 17)
            exp.fulfill()
        }
        event.invoke(17)
        wait(for: [exp], timeout: 1)
    }

    func testNonInvokation() {
        let event = YxEvent<Int>()

        func innerScope() {
            autoreleasepool {
                let handlerHolder = NSObject()
                event += YxEventHandler([handlerHolder]) { _ in
                    // should not be invoked
                    XCTAssert(false)
                }
                // handlerHolder is guaranted to be released here
            }
        }

        innerScope()
        event.invoke(17)
    }

    func testMemory() {
        autoreleasepool {
            let god = God()
            let herd = Herd(sheeps: god.sheeps.flatMap { $0 })
            RunLoop.main.run(until: Date().addingTimeInterval(10))
        }
        _ = 1
    }
}

class Sheep {
    let name: String
    private let jumpTimer = YxTimer(interval: 1, repeats: true)
    let onJump = YxEvent<Float>()

    init(name: String) {
        self.name = name
        jumpTimer.onTick += { [weak self] _ in
            if arc4random() % 10 == 0 {
                self?.jump()
            }
        }
    }

    deinit {
        print("\(name) died.")
    }

    private func jump() {
        onJump.invoke(17 + (Float(arc4random() % 100) / 100))
    }
}

let sheepsName1 = [
    "Dolly",
    "Nora",
    "Buffy",
    "Roxy",
    "Dorie",
    "Finigan",
    "Juliet",
    "Rosanna",
    "Puffy"
]

let sheepsName2 = [
    "Sparkles",
    "Power",
    "Space",
    "Rain",
    "Banana",
    "Sunny",
    "Thunder"
]

func inventSheepName() -> String {
    let name1 = sheepsName1[Int(arc4random_uniform(UInt32(Int32.max))) % sheepsName1.count]
    let name2 = sheepsName2[Int(arc4random_uniform(UInt32(Int32.max))) % sheepsName2.count]
    return name1 + " " + name2
}

class God {
    var sheeps = [Sheep?]()
    let changeMoodTimer = YxTimer(interval: 1, repeats: true)
    let onSheepBorn = YxEvent<Sheep>()

    init() {
        changeMoodTimer.onTick += { [weak self] _ in
            if arc4random() & 1 == 0 { // 50/50
                self?.killASheep()
            } else {
                self?.createASheep()
            }
        }
        // In the beginning, God created a sheep array and filled it with N sheeps. And N was 50.
        for _ in 1...50 {
            let sheep = Sheep(name: inventSheepName())
            sheeps.append(sheep)
        }
    }

    private func killASheep() {
        let unluckyIndex = Int(arc4random_uniform(UInt32(Int32.max))) % sheeps.count
        sheeps[unluckyIndex] = nil
    }

    private func createASheep() {
        // Let there be a sheep
        let sheep = Sheep(name: inventSheepName())
        sheeps.append(sheep)
        onSheepBorn.invoke(sheep)
    }
}

class Herd {
    let sheeps: YxWeakSet<Sheep>
    init(sheeps: [Sheep]) {
        self.sheeps = YxWeakSet<Sheep>(sheeps)
        for sheep in sheeps {
            sheep.onJump += YxEventHandler([self]) { [weak self, weak sheep] (height: Float) -> Void in
                guard let sheep = sheep else { return }
                guard let sself = self else { return }
                sself.onSheepJumpHandler(sheep: sheep, height: height)
            }
        }
    }

    func onSheepJumpHandler(sheep: Sheep, height: Float) {
        print("\(sheep.name) jumped \(height) m")
    }
}
