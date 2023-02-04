protocol AnalyticsEventService {
    func shouldEventsBeReported(
        _ events: [(name: String, properties: [String: Any])],
        file: StaticString,
        line: UInt
    )
}
