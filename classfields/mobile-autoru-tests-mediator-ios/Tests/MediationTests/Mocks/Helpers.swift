@testable import Mediation

extension TeamcityBuildID {
    static func mock() -> TeamcityBuildID { Int.random(in: 0...1000) }
}

extension TeamcityBuildNumber {
    static func mock() -> TeamcityBuildNumber { "\(Int.random(in: 0...1000))" }
}

extension TestCase {
    static func mock() -> TestCase {
        TestCase("test_\(Int.random(in: 0...1000))")
    }
}
