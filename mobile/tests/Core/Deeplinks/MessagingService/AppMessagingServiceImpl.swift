import Foundation
import MarketUITestMessaging
import XCTest

#if DEBUG
final class AppMessagingServiceImpl: AppMessagingService {

    // MARK: - Properties

    var baseURL: URL

    private var service: UITestMessagingService

    // MARK: - Lifecycle

    init(baseURL: URL) {
        self.baseURL = baseURL
        service = UITestMessagingServiceImpl(
            inboxPath: baseURL.appendingPathComponent(UITestMessagingConstants.testsInbox).path,
            outboxPath: baseURL.appendingPathComponent(UITestMessagingConstants.beruInbox).path
        )

        service.addListener(self)
        service.startWatchingInbox()
    }

    // MARK: - Public

    func sendMessage(_ message: UITestMessage) {
        XCTContext.runActivity(named: "Отправляем сообщение \(message.identifier)", block: { _ in
            service.sendMessage(message)

            XCTContext.runActivity(named: "Дожидаемся, пока сообщение не будет обработано", block: { _ in
                awaitFor(fulfillmentOf: { self.service.isOutboxEmpty })
            })
        })
    }
}

extension AppMessagingServiceImpl: UITestMessagesListener {
    func uiTestMessagingService(_ service: UITestMessagingService, didReceive message: UITestMessage) {}
}
#endif
