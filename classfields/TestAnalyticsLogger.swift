#if DEBUG

import AutoRuAnalyticsCore
import AutoRuSynchronization

final class TestAnalyticsLogger: AnalyticsLogger {
    static let shared = TestAnalyticsLogger()

    @ThreadSafe
    private var events: [AnalyticsLoggerEvent] = []

    func logEvent(_ event: AnalyticsLoggerEvent) {
        events.append(event)
    }

    func logError(_ event: AnalyticsLoggerErrorEvent) {
    }

    func userPropertiesChanged(_ event: UserPropertiesChangedEvent) {
    }
}

extension TestAnalyticsLogger {
    func getEventsAndReset() -> [AnalyticsLoggerEvent] {
        _events.update([])
    }
}

#endif
