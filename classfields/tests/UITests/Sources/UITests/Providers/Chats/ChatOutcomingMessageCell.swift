final class ChatOutcomingMessageCell: BaseSteps, UIElementProvider {
    enum Element {
        case bubble
        case errorButton
        case text
        case date
        case status(MessageStatus)
    }

    enum MessageStatus: String {
        case sent
        case read
        case sending
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .bubble: return "outcoming_message_bubble"
        case .errorButton: return "outcoming_message_error"
        case .text: return "outcoming_message_text"
        case .date: return "outcoming_message_date"
        case .status(let type): return "outcoming_message_status_\(type.rawValue)"
        }
    }
}
