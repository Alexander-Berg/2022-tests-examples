import Foundation

extension Bundle {
    static var current: Bundle {
        #if SWIFT_PACKAGE
        return Bundle.module
        #else
        final class Klass {}
        return Bundle(for: Klass.self)
        #endif
    }
}
