final class ChatInputBar: BaseSteps, UIElementProvider {
    enum Element: String {
        case attach = "chat_message_attach_button"
        case send = "chat_message_send_button"
        case text = "chat_message_input"
        case hint = "Сообщение"
    }
}
