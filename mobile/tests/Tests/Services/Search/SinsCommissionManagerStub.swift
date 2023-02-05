import MarketModels
import MarketProtocols

final class SinsCommissionManagerStub: SinsCommissionManager {

    var isSinsOpened = false

    var sinsBusinessId: Int?

    var currentPage: MetrikaParams.PageId?

    func addDirectSins(businessId: Int) {}

    func getAllDirectSins() -> [Int] { [] }
}
