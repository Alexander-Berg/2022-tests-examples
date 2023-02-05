import MarketCodableUtils
import XCTest

@testable import BeruServices
@testable import MarketDTO

// MARK: - Preset

extension CheckoutMapperMocksFactory {
    func makeConsolidation(
        labels: [ParcelInfo.Label] = Constants.labels,
        dates: [String] = Constants.dates
    ) -> [ConsolidationGroup] {
        let targetDates: [DateInterval] = dates.map {
            guard let date = Constants.dateFormatter.date(from: $0) else {
                XCTFail("Can't create date result from mocks")
                return DateInterval()
            }
            return DateInterval(start: date, end: date)
        }
        return [ConsolidationGroup(labels: labels, dates: targetDates)]
    }

    // MARK: - DTO

    func makeConsolidationResult(
        labels: [String] = Constants.labels,
        dates: [String] = Constants.dates
    ) -> [ConsolidationResult] {
        [ConsolidationResult(labels: labels, dates: dates)]
    }
}

// MARK: - Nested types

private extension CheckoutMapperMocksFactory {
    enum Constants {
        static let firstLabel = "stub_parcel_1"
        static let secondLabel = "stub_parcel_2"

        static let firstDateString = "2022-03-25"
        static let secondDateString = "2022-03-26"

        static let labels = [Constants.firstLabel, Constants.secondLabel]
        static let dates = [Constants.firstDateString, Constants.secondDateString]

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
