import MarketModels
import MarketProtocols

final class AddressChooserStub: ServicesAddressChooser {

    // MARK: - Properties

    var suggestedAddress: Address?
    var availableAddresses: [Address] = []
    var hyperLocalAddress: Address?

    // MARK: - Lifecycle

    init(
        suggestedAddress: Address? = nil,
        availableAddresses: [Address] = [],
        hyperLocalAddress: Address? = nil
    ) {
        self.suggestedAddress = suggestedAddress
        self.availableAddresses = availableAddresses
        self.hyperLocalAddress = hyperLocalAddress
    }

    // MARK: - ServicesAddressChooser

    func suggestedAddress(in region: YMTRegion) -> Address? {
        suggestedAddress
    }

    func sortedAddresses(by region: YMTRegion) -> [Address] {
        availableAddresses
    }

    func hyperlocalAddress() -> Address? {
        hyperLocalAddress
    }
}
