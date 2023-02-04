#include "null_alice_capability.h"

using namespace YandexIO;

void NullAliceCapability::startRequest(
    std::shared_ptr<VinsRequest> request,
    std::shared_ptr<IAliceRequestEvents> events)
{
    Y_UNUSED(request);
    Y_UNUSED(events);
}

void NullAliceCapability::cancelDialog()
{
}

void NullAliceCapability::cancelDialogAndClearQueue()
{
}

void NullAliceCapability::startConversation(const VinsRequest::EventSource& /*eventSource*/)
{
}

void NullAliceCapability::stopConversation()
{
}

void NullAliceCapability::toggleConversation(const VinsRequest::EventSource& /*eventSource*/)
{
}

void NullAliceCapability::finishConversationVoiceInput()
{
}

void NullAliceCapability::addListener(std::weak_ptr<IAliceCapabilityListener> listener)
{
    Y_UNUSED(listener);
}

void NullAliceCapability::removeListener(std::weak_ptr<IAliceCapabilityListener> listener)
{
    Y_UNUSED(listener);
}
