import Foundation

final class VehicleQuickSearchSteps: BaseSteps {
    func onScreen() -> VehicleQuickSearchScreen {
        return baseScreen.on(screen: VehicleQuickSearchScreen.self)
    }
}
