import BeruServices
import MarketModels
import MarketProtocols

final class DummyRegionSettings: SettingsRegionProtocol, SettingsHyperlocalAddress {
    var selectedRegion: YMTRegion?
    var pendingConfirmationRegion: YMTRegion?
    var selectedAddress: Address?
    var addressSelectDate: Date?
    var isAddressChanged = false
    var addressSessionEndDate: Date?
    var isAddressExpired = false
}
