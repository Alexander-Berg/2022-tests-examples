import XCTest

class SmartbonusDetailsTestCase: LocalMockTestCase {

    // MARK: - Public

    private let format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private lazy var enFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = format
        dateFormatter.locale = Locale(identifier: "en_US_POSIX")
        dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
        return dateFormatter
    }()

    private lazy var ruFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "d MMMM"
        dateFormatter.locale = Locale(identifier: "ru_RU")
        dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
        return dateFormatter
    }()

    func changeCoinsEndDateInMock(
        bundleName: String,
        newDate date: Date
    ) {
        let endDateString = enFormatter.string(from: date)
        let newBundleName = bundleName + "1"

        mockStateManager?.changeMock(
            bundleName: bundleName,
            newBundleName: newBundleName,
            filename: "POST_api_v1_resolveBonusesForPerson",
            changes: [
                (
                    #""endDate" : "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}Z""#,
                    "\"endDate\" : \"\(endDateString)\""
                )
            ]
        )

        mockStateManager?.pushState(bundleName: bundleName)
        mockStateManager?.pushState(bundleName: newBundleName)
    }

    func getDateString(from date: Date) -> String {
        ruFormatter.string(from: date)
    }

    func getEnFormattedDateString(from date: Date) -> String {
        enFormatter.string(from: date)
    }

    func getNewDate(byAddingDays days: TimeInterval) -> Date {
        let secondsInDay: TimeInterval = 86_400
        let endDateInSeconds = Date().timeIntervalSince1970 + days * secondsInDay
        return Date(timeIntervalSince1970: endDateInSeconds)
    }

}
