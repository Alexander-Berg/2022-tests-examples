import Foundation

// MARK: - ReferralProgramMockHelper

protocol ReferralProgramMockHelper: AnyObject {
    /// Generates string representation of the date
    /// - Returns: Dates in "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" and "d MMMM" formats
    func makeStringRepresentation(of date: Date) -> (ru: String, en: String)
}

extension ReferralProgramMockHelper {

    func makeStringRepresentation(of date: Date) -> (ru: String, en: String) {
        let enFormatter: DateFormatter = {
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            dateFormatter.locale = Locale(identifier: "en_US_POSIX")
            dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
            return dateFormatter
        }()

        let ruFormatter: DateFormatter = {
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "d MMMM"
            dateFormatter.locale = Locale(identifier: "ru_RU")
            dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
            return dateFormatter
        }()

        return (ru: ruFormatter.string(from: date), en: enFormatter.string(from: date))
    }
}
