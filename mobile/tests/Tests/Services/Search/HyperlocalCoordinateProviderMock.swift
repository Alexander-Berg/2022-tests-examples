import MarketProtocols
@testable import BeruServices

class HyperlocalCoordinateProviderMock: HyperlocalCoordinateProvider {
    var coordinate: LocationCoordinate? { nil }
}
