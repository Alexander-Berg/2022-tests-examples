import Foundation

/// Описание теста, которое подается на вход (из аппа)
public struct TestCase: Hashable {
    public let name: String

    public init(_ name: String) {
        self.name = name
    }
}

extension TestCase: ExpressibleByStringLiteral {
    public init(stringLiteral value: String) {
        self.init(value)
    }
}
