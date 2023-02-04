#pragma once

#include <yandex_io/capabilities/alice/interfaces/i_alice_capability.h>

namespace YandexIO {

    class NullAliceCapability: public IAliceCapability {
    public:
        void startRequest(
            std::shared_ptr<VinsRequest> request,
            std::shared_ptr<IAliceRequestEvents> events) override;
        void cancelDialog() override;
        void cancelDialogAndClearQueue() override;
        void startConversation(const VinsRequest::EventSource& eventSource) override;
        void stopConversation() override;
        void toggleConversation(const VinsRequest::EventSource& eventSource) override;
        void finishConversationVoiceInput() override;

        void addListener(std::weak_ptr<IAliceCapabilityListener> listener) override;
        void removeListener(std::weak_ptr<IAliceCapabilityListener> listener) override;
    };

} // namespace YandexIO
