import Foundation
import YxSwissKnife

final class TestInitializer: NSObject {
    // NOTE: for mass regenerate test cases
    static let recordMode: Bool = false
    static let referenceDirKey = "SNAPSHOT_TEST_REF_DIR"
    static let referenceDirPath = ProcessInfo.processInfo.environment[referenceDirKey]!

    static func snapshotConfiguration(recordMode: Bool = TestInitializer.recordMode) -> YxScreenshotTestConfiguration {
        return YxScreenshotTestConfiguration(
            recordMode: recordMode,
            useDrawHierarchyInRect: true,
            referenceDir: TestInitializer.referenceDirPath
        )
    }
}
