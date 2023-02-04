import MarketCodableUtils
import XCTest

@testable import BeruServices
@testable import MarketDTO

// MARK: - DeliveryOption

extension CheckoutMapperMocksFactory {
    func makeDeliveryOption(
        id: String = Constants.DeliveryOption.id,
        type: DeliveryOption.DeliveryOptionType,
        deliveryServiceId: Int = Constants.DeliveryOption.deliveryServiceId,
        price: Int = Constants.DeliveryOption.price,
        extraCharge: Int? = Constants.DeliveryOption.extraCharge,
        beginDate: String = Constants.DeliveryOption.beginDate,
        endDate: String = Constants.DeliveryOption.endDate,
        deliveryPartnerType: DeliveryPartnerType = Constants.DeliveryOption.deliveryPartnerType
    ) -> DeliveryOption! {
        guard let start = Constants.DeliveryOption.dateFormatter.date(from: beginDate),
              let end = Constants.DeliveryOption.dateFormatter.date(from: endDate) else {
            XCTFail("Invalid dates")
            return nil
        }
        let decimalExtraCharge: Decimal? = {
            guard let extraCharge = extraCharge else {
                return nil
            }
            return Decimal(extraCharge)
        }()
        let deliveryOption = DeliveryOption(
            id: id,
            type: type,
            deliveryPartnerType: deliveryPartnerType,
            deliveryServiceId: deliveryServiceId,
            price: Decimal(price),
            extraCharge: decimalExtraCharge,
            date: DateInterval(start: start, end: end),
            paymentMethods: [],
            availableCustomizers: [],
            isTryingAvailable: false,
            isEstimated: false
        )

        return deliveryOption
    }

    func makeOutletDeliveryOptionType(
        isMarketBranded: Bool = false,
        outletIds: [Int] = [],
        timeIntervals: [Outlet.Identifier: DeliveryTime] = [:],
        isEstimated: Bool = false
    ) -> DeliveryOption.DeliveryOptionType {
        .outlet(
            OutletDeliveryInfo(
                outletIds: outletIds,
                isMarketBranded: isMarketBranded,
                timeIntervals: timeIntervals,
                isEstimated: isEstimated
            )
        )
    }

    func makePostDeliveryOptionType(outletId: Int? = nil) -> DeliveryOption.DeliveryOptionType {
        .post(
            PostDeliveryInfo(outletId: outletId)
        )
    }

    func makeServiceDeliveryOptionType(
        intervals: [DeliveryTime] = [],
        isOnDemand: Bool = false,
        liftPrice: Int = 0,
        liftingOptions: LiftingOptions? = nil,
        isDeferredCourier: Bool = false,
        isLeaveAtTheDoor: Bool = false,
        isExpress: Bool = false,
        isEstimated: Bool = false
    ) -> DeliveryOption.DeliveryOptionType {
        .service(
            ServiceDeliveryInfo(
                timeIntervals: intervals,
                isOnDemand: isOnDemand,
                isDeferredCourier: isDeferredCourier,
                isLeaveAtTheDoor: isLeaveAtTheDoor,
                isExpress: isExpress,
                liftPrice: liftPrice,
                liftingOptions: liftingOptions,
                isEstimated: isEstimated
            )
        )
    }

    func makeDeliveryTime(
        fromTime: String,
        toTime: String,
        isDefault: Bool,
        price: Int? = nil
    ) -> DeliveryTime? {
        DeliveryTime(
            fromTime: fromTime,
            toTime: toTime,
            isDefault: isDefault,
            price: price
        )
    }

    func makeLiftingOptions(
        type: LiftingOptions.AvailabilityType,
        manualLiftPerFloorCost: Int,
        elevatorLiftCost: Int,
        cargoElevatorLiftCost: Int,
        unloadCost: Int
    ) -> LiftingOptions {
        LiftingOptions(
            type: type,
            manualLiftPerFloorCost: manualLiftPerFloorCost,
            elevatorLiftCost: elevatorLiftCost,
            cargoElevatorLiftCost: cargoElevatorLiftCost,
            unloadCost: unloadCost
        )
    }

    // MARK: - DTO

    func makeDeliveryOptionResult(
        id: String = Constants.DeliveryOption.id,
        type: DeliveryOptionResult.DeliveryOptionType,
        deliveryPartnerType: DeliveryOptionResult.DeliveryPartnerType = Constants.DeliveryOption
            .deliveryPartnerTypeResult,
        deliveryServiceId: Int = Constants.DeliveryOption.deliveryServiceId,
        price: Int = Constants.DeliveryOption.price,
        extraCharge: DeliveryExtraCharge? = Constants.DeliveryOption.extraChargeResult,
        beginDate: String = Constants.DeliveryOption.beginDate,
        endDate: String = Constants.DeliveryOption.endDate,
        features: [DeliveryOptionResult.Feature] = [],
        isMarketBranded: Bool = false,
        outlets: [DeliveryOptionResult.Outlet] = [],
        outletTimeIntervals: [DeliveryOptionResult.OutletTimeInterval] = [],
        outlet: DeliveryOptionResult.Outlet? = nil,
        intervals: [TimeIntervalResult] = [],
        liftPrice: Int = 0,
        liftingOptions: DeliveryOptionResult.LiftingOptions? = nil,
        isLeaveAtTheDoor: Bool = false,
        isTryingAvailable: Bool = false,
        estimated: Bool = false
    ) -> DeliveryOptionResult {
        DeliveryOptionResult(
            id: id,
            type: type,
            deliveryPartnerType: deliveryPartnerType,
            deliveryServiceId: deliveryServiceId,
            price: price,
            extraCharge: extraCharge,
            beginDate: beginDate,
            endDate: endDate,
            isTryingAvailable: isTryingAvailable,
            estimated: estimated,
            features: features,
            paymentMethods: [],
            customizers: [],
            isMarketBranded: isMarketBranded,
            outlets: outlets,
            outletTimeIntervals: outletTimeIntervals,
            outlet: outlet,
            intervals: intervals,
            isLeaveAtTheDoor: isLeaveAtTheDoor,
            liftPrice: liftPrice,
            presentationFields: .init(liftingOptions: liftingOptions)
        )
    }

    func makeDeliveryOptionResultOutlet(id: Int) -> DeliveryOptionResult.Outlet {
        DeliveryOptionResult.Outlet(id: id)
    }

    func makeDeliveryOptionResultTimeInterval(
        fromTime: String,
        toTime: String,
        isDefault: Bool,
        price: Int? = nil
    ) -> TimeIntervalResult {
        TimeIntervalResult(
            fromTime: fromTime,
            toTime: toTime,
            isDefault: isDefault,
            price: price
        )
    }

    func makeDeliveryOptionResultLiftingOptions(
        manualLiftPerFloorCost: Int,
        elevatorLiftCost: Int,
        cargoElevatorLiftCost: Int,
        unloadCost: Int,
        type: DeliveryOptionResult.LiftingOptions.LiftingOptionsType
    ) -> DeliveryOptionResult.LiftingOptions! {
        DeliveryOptionResult.LiftingOptions(
            manualLiftPerFloorCost: manualLiftPerFloorCost,
            elevatorLiftCost: elevatorLiftCost,
            cargoElevatorLiftCost: cargoElevatorLiftCost,
            unloadCost: unloadCost,
            type: type
        )
    }
}

// MARK: - Nested Types

private extension CheckoutMapperMocksFactory {
    enum Constants {
        enum DeliveryOption {
            static let id = "stub_id"
            static let deliveryServiceId = 100
            static let price = 99
            static let extraCharge = 39
            static let extraChargeResult: DeliveryExtraCharge = .init(value: 39)
            static let beginDate = "2020-01-01"
            static let endDate = "2020-01-03"
            static let deliveryPartnerType: DeliveryPartnerType = .yandex
            static let deliveryPartnerTypeResult: DeliveryOptionResult.DeliveryPartnerType = .yandex

            static let dateFormatter: DateFormatter = {
                // swiftlint:disable:next no_direct_use_date_formatter
                let formatter = DateFormatter()
                formatter.locale = Locale(identifier: "en_US_POSIX")
                formatter.timeZone = TimeZone(secondsFromGMT: 0)
                formatter.dateFormat = "yyyy-MM-dd"

                return formatter
            }()
        }
    }
}
