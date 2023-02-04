import Snapshots
@testable import AutoRuCommonViews

final class SnackbarTests: BaseUnitTest {
    func test_onlyTitle() {
        let model = ActivityHUD.Mode.SnackbarModel(title: "Заголовок снекбара")
        let layout = AutoRuCommonViews.SnackbarLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(layoutSpec: layout)
    }

    func test_titleAndButton() {
        let model = ActivityHUD.Mode.SnackbarModel(title: "Заголовок снекбара", button: "Кнопка")
        let layout = AutoRuCommonViews.SnackbarLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(layoutSpec: layout)
    }

    func test_longTitle() {
        let model = ActivityHUD.Mode.SnackbarModel(title: "Заголовок снекбара заголовок снекбара Заголовок снекбара")
        let layout = AutoRuCommonViews.SnackbarLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(layoutSpec: layout, maxWidth: 320)
    }
}
