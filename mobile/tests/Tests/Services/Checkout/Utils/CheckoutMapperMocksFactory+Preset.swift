import MarketCodableUtils
import XCTest

@testable import BeruServices
@testable import MarketDTO

// MARK: - Preset

extension CheckoutMapperMocksFactory {
    func makePreset(
        presetId: PresetDeliverability.Identifier = Constants.PresetDeliverability.id,
        kind: DeliveryType,
        parcels: PresetDeliverability.Parcels = [:]
    ) -> PresetDeliverability {
        PresetDeliverability(
            presetId: presetId,
            kind: kind,
            parcels: parcels
        )
    }

    // MARK: - DTO

    func makePresetResult(
        presetId: String = Constants.PresetDeliverability.id,
        type: DeliveryTypeResult,
        parcels: [PresetResult.Parcel] = []
    ) -> PresetResult {
        PresetResult(
            presetId: presetId,
            type: type,
            parcels: parcels
        )
    }

    func makePresetResultParcel(
        label: String,
        deliveryAvailable: Bool
    ) -> PresetResult.Parcel {
        PresetResult.Parcel(
            label: label,
            deliveryAvailable: deliveryAvailable,
            isTryingAvailable: false
        )
    }
}

// MARK: - Nested Types

private extension CheckoutMapperMocksFactory {
    enum Constants {
        enum PresetDeliverability {
            static let id = "stub_id"
        }
    }
}
