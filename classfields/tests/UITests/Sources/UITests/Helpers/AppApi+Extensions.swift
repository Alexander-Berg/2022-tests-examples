import AppServer

extension AppApi {
    func setUserInterfaceStyleBlocking(_ style: UserInterfaceStyle) {
        performBlocking {
            try await setUserInterfaceStyle(style)
        }
    }

    func newAnalyticsEventsBlocking() -> [AnalyticsEvent] {
        performBlocking {
            try await newAnalyticsEvents()
        }
    }

    private func performBlocking<Result>(_ action: @escaping () async throws -> Result) -> Result {
        let condition = Condition(nil as Result?)
        Task {
            let result = try! await action()
            condition.updateValueAndSignal(result)
        }

        return condition.wait { $0 != nil }!
    }
}
