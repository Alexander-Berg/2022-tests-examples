import XCTest
import AutoRuAppearance
import Snapshots
@testable import AutoRuViews
import AutoRuColorSchema

final class EnablePushTests: BaseUnitTest {
    func test_blockLayout() {
        let layout = EnablePush.blockLayout(source: .favorites, onDismissTap: { _ in })

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }
}
