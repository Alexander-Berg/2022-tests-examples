import Foundation
import MarketUITestMessaging

#if DEBUG
/// Протокол работы с межпроцессными сообщениями, отправляемыми в тестируемое приложение.
protocol AppMessagingService {

    var baseURL: URL { get set }

    func sendMessage(_ message: UITestMessage)
}

extension AppMessagingService {

    func sendMessage(_ identifier: TestMessage, userInfo: [String: Any] = [:]) {
        sendMessage(
            UITestMessage(
                identifier: identifier,
                userInfo: userInfo
            )
        )
    }

    /// Отправляем сообщение с открытием диплинка
    func openURL(beru link: Link) {
        sendMessage(.openUrl, userInfo: [UITestMessage.paramURL: link.rawValue])
    }
}
#endif
