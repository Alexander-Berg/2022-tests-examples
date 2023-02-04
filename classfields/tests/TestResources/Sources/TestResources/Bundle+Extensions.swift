import Foundation

extension Bundle {
    public static var testResourcesBundle: Bundle {
        #if SWIFT_PACKAGE
        return .module
        #else
        final class Klass {}
        return Bundle(for: Klass.self)
        #endif
    }
}
