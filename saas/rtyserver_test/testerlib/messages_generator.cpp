#include "messages_generator.h"

TAtomic IMessageGenerator::GlobalCountMessages = 0;

NRTYServer::TMessage CreateSimpleKVMessage(const TString& key, const TString& value, const TString& attr, int kps) {
    NRTYServer::TMessage message;

    message.SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
    message.SetMessageId(IMessageGenerator::CreateMessageId());
    auto& document = *message.MutableDocument();
    document.SetUrl(key);
    if (kps >= 0) {
        document.SetKeyPrefix(kps);
    }
    auto props = document.AddDocumentProperties();
    props->SetName(attr);
    props->SetValue(value);
    return message;
}
