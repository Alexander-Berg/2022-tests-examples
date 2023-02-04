#include "test_voice_dialog.h"

#include "test_audio_player.h"

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <boost/algorithm/string/predicate.hpp>

#include <speechkit/PhraseSpotter.h>

#include <chrono>

using namespace quasar;
using namespace quasar::TestUtils;

QuasarVoiceDialogTestImpl::QuasarVoiceDialogTestImpl(std::shared_ptr<SpeechKit::VoiceService::VoiceServiceListener> listener,
                                                     std::shared_ptr<TestVins> vins)
    : weakListener_{listener}
    , localVins_{std::move(vins)}
{
}

QuasarVoiceDialogTestImpl::~QuasarVoiceDialogTestImpl() {
    stop();
}

void QuasarVoiceDialogTestImpl::waitForVoiceInputStarted() {
    voiceInputStartedEvent_.wait();
}

void QuasarVoiceDialogTestImpl::stop() {
    if (stopped_.load()) {
        return;
    }

    stopped_.store(true);

    callbackQueue_.destroy();
}

bool QuasarVoiceDialogTestImpl::IsPrepared() const {
    std::unique_lock<std::mutex> lock(mutex_);
    return gotPrepared_;
}

void QuasarVoiceDialogTestImpl::Say(const std::string& phrase, bool is_last)
{
    auto listener = weakListener_.lock();
    if (listener == nullptr) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }
    YIO_LOG_INFO("User said: " << phrase);
    std::unique_lock<std::mutex> lock(mutex_);
    if (state_ == SPOTTER || state_ == SPEAKING_WITH_SPOTTER) {
        if (isSpotter(phrase)) {
            updateState(IDLE);

            lock.unlock();
            listener->onPhraseSpotted(nullptr, phrase);
        }
    } else if (state_ == LISTENING) {
        if (!phraseBuffer_.empty()) {
            phraseBuffer_ += " ";
        }
        phraseBuffer_ += phrase;
        SpeechKit::Recognition recognition({SpeechKit::RecognitionHypothesis({}, phraseBuffer_, 1.0f)}, "", {});
        lock.unlock();
        listener->onRecognitionResults(nullptr, requestId_, recognition, is_last);
        if (is_last) {
            lock.lock();
            updateState(IDLE);

            if (phraseBuffer_.empty()) {
                lock.unlock();
                SpeechKit::Error error(SpeechKit::Error::ErrorNoSpeechDetected, "no speech");
                listener->onRecognitionError(nullptr, requestId_, error);
            } else {
                lock.unlock();
                listener->onRecognitionEnd(nullptr, requestId_);
                listener->onVinsRequestBegin(nullptr, requestId_);
                if (onVinsRequestStarted) {
                    onVinsRequestStarted();
                }
                lock.lock();
                auto resp = localVins_->getResponse(lastPayload_, phraseBuffer_, requestId_);
                phraseBuffer_ = "";
                lock.unlock();
                callVinsResponse(resp);
            }
        }
    }
}

void QuasarVoiceDialogTestImpl::SayCommand(const std::string& phrase)
{
    if (weakListener_.expired()) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }

    if (stopped_.load()) {
        return;
    }

    callbackQueue_.add([=] {
        auto listener = weakListener_.lock();
        if (listener != nullptr) {
            YIO_LOG_INFO("SayCommand: " << phrase);
            listener->onCommandPhraseSpotted(nullptr, phrase);
        }
    });
}

void QuasarVoiceDialogTestImpl::raiseModelError() {
    if (weakListener_.expired()) {
        YIO_LOG_WARN("listener expired");
        return;
    }
    if (stopped_.load()) {
        return;
    }

    callbackQueue_.add([=] {
        auto listener = weakListener_.lock();
        if (listener != nullptr) {
            SpeechKit::Error error(SpeechKit::Error::ErrorModel, "invalid model file");
            listener->onPhraseSpotterError(nullptr, error);
        }
    });
}

void QuasarVoiceDialogTestImpl::raiseCommandModelError() {
    if (weakListener_.expired()) {
        YIO_LOG_WARN("listener expired");
        return;
    }

    if (stopped_.load()) {
        return;
    }

    callbackQueue_.add([=] {
        auto listener = weakListener_.lock();
        if (listener != nullptr) {
            SpeechKit::Error error(SpeechKit::Error::ErrorModel, "invalid model file");
            listener->onCommandSpotterError(nullptr, error);
        }
    });
}

void QuasarVoiceDialogTestImpl::raiseOnInterruptionPhraseSpotted()
{
    if (stopped_.load()) {
        return;
    }

    callbackQueue_.add([=] {
        auto listener = weakListener_.lock();
        if (listener != nullptr) {
            listener->onInterruptionPhraseSpotted(nullptr, "алиса");
        }
    });
}

void QuasarVoiceDialogTestImpl::raiseOnInvalidOAuthToken()
{
    if (stopped_.load()) {
        return;
    }

    if (auto listener = weakListener_.lock()) {
        listener->onInvalidOAuthToken(nullptr);
    }
}

void QuasarVoiceDialogTestImpl::onUniProxyDirective(const std::string& header, const std::string& payload) const {
    auto listener = weakListener_.lock();

    if (listener) {
        listener->onUniProxyDirective(SpeechKit::VoiceService::SharedPtr(nullptr), header, payload);
    } else {
        YIO_LOG_WARN("Listener expired");
    }
}

/* under mutes */

bool QuasarVoiceDialogTestImpl::isSpotter(const std::string& phrase) const {
    const auto& activationPhraseSpotter = settings_.activationPhraseSpotter;
    YIO_LOG_INFO(activationPhraseSpotter.modelPath << " " << phrase);
    const bool isAlisaActivation = boost::algorithm::ends_with(activationPhraseSpotter.modelPath, "alisa") && phrase == "алиса";
    const bool isYandexActivation = boost::algorithm::ends_with(activationPhraseSpotter.modelPath, "yandex") && phrase == "яндекс";
    return isAlisaActivation || isYandexActivation;
}

void QuasarVoiceDialogTestImpl::startVoiceInput(const SpeechKit::UniProxy::Header& /*header*/, const std::string& payload, bool /* jingle */)
{
    voiceInputStartedEvent_.set();

    if (weakListener_.expired()) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }

    if (stopped_.load()) {
        return;
    }
    activationSpotterStarted_.store(false);

    removeDeadProviders();

    callbackQueue_.add([=]() {
        auto listener = weakListener_.lock();
        requestId_ = parseRequestId(payload);
        if (listener != nullptr) {
            listener->onRecognitionBegin(nullptr, requestId_);
        }

        std::lock_guard<std::mutex> lock(mutex_);
        lastPayload_ = payload;
        updateState(LISTENING);
    });
}

void QuasarVoiceDialogTestImpl::startMusicInput(const SpeechKit::UniProxy::Header& /*header*/, const std::string& payload, bool /* jingle */)
{
    if (weakListener_.expired()) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }

    if (stopped_.load()) {
        return;
    }

    removeDeadProviders();

    callbackQueue_.add([=]() {
        requestId_ = parseRequestId(payload);
        if (auto listener = weakListener_.lock(); listener != nullptr) {
            listener->onRecognitionBegin(nullptr, requestId_);
        }

        std::lock_guard lock(mutex_);
        lastPayload_ = payload;
        updateState(LISTENING);
    });
}

void QuasarVoiceDialogTestImpl::startPhraseSpotter()
{
    startPhraseSpotterCnt_++;
    startPhraseSpotterCntCV_.notify_all();

    YIO_LOG_INFO("startPhraseSpotter");

    if (weakListener_.expired()) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }

    if (stopped_.load()) {
        return;
    }
    if (activationSpotterStarted_.load()) {
        return;
    }

    activationSpotterStarted_.store(true);

    callbackQueue_.add([=]() {
        auto listener = weakListener_.lock();
        if (listener != nullptr) {
            listener->onPhraseSpotterBegin(nullptr);
        }

        std::lock_guard<std::mutex> lock(mutex_);
        updateState(SPOTTER);
    });
}

void QuasarVoiceDialogTestImpl::startCommandSpotter(SpeechKit::PhraseSpotterSettings settings)
{
    if (weakListener_.expired()) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }

    if (stopped_.load()) {
        return;
    }

    callbackQueue_.add([this, settings] {
        auto listener = weakListener_.lock();
        if (listener != nullptr) {
            listener->onCommandSpotterBegin(nullptr);
        }

        std::lock_guard<std::mutex> guard(mutex_);
        const auto spotter = getFileName(settings.modelPath);
        activeCommandSpotter_ = spotter;
        activeCommandSpotterSettings_ = settings;
        YIO_LOG_INFO("Current command spotter " << spotter);
        speechWakeUpVar_.notify_all();
    });
}

void QuasarVoiceDialogTestImpl::stopCommandSpotter()
{
    if (weakListener_.expired()) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }

    if (stopped_.load()) {
        return;
    }

    callbackQueue_.add([this] {
        std::scoped_lock guard(mutex_);
        activeCommandSpotter_ = "";
        activeCommandSpotterSettings_ = ::SpeechKit::PhraseSpotterSettings{""};
        YIO_LOG_INFO("Stop command spotter");
        speechWakeUpVar_.notify_all();
    });
}

ConcurrentQueue<std::string>& QuasarVoiceDialogTestImpl::getTextRequests() {
    return textRequests_;
}

void QuasarVoiceDialogTestImpl::sendEvent(const SpeechKit::UniProxy::Header& /*header*/, const std::string& payload)
{
    textRequests_.push(payload);
}

void QuasarVoiceDialogTestImpl::startTextInput(const SpeechKit::UniProxy::Header& /*header*/, const std::string& payload)
{
    textRequests_.push(payload);

    if (weakListener_.expired()) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }

    if (stopped_.load()) {
        return;
    }

    removeDeadProviders();

    callbackQueue_.add([=]() {
        auto listener = weakListener_.lock();
        if (listener != nullptr) {
            listener->onVinsRequestBegin(nullptr, requestId_);
        }
        std::unique_lock<std::mutex> lock(mutex_);
        requestId_ = parseRequestId(payload);
        auto response = localVins_->getResponse(payload, "", requestId_);
        lock.unlock();

        if (callVinsResponseEvent_ != nullptr) {
            callVinsResponseEvent_->wait();
        }
        callVinsResponse(response);
    });
}

void QuasarVoiceDialogTestImpl::lockCallVinsResponse()
{
    callVinsResponseEvent_ = std::make_shared<ConcurrentEvent>();
}

void QuasarVoiceDialogTestImpl::unlockCallVinsResponse()
{
    if (callVinsResponseEvent_ != nullptr) {
        callVinsResponseEvent_->set();
    }
}

void QuasarVoiceDialogTestImpl::cancel(bool /* silent */)
{
    std::unique_lock<std::mutex> lock(mutex_);
    updateState(IDLE);
    gotPrepared_ = false;
    phraseBuffer_ = "";
    lastPayload_ = "";
    outputSpeech_ = "";
    requestId_ = "";
    activeCommandSpotter_ = "";
    activeCommandSpotterSettings_ = ::SpeechKit::PhraseSpotterSettings{""};
    activationSpotterStarted_.store(false);
    speechWakeUpVar_.notify_all();
    removeDeadProviders();
}

void QuasarVoiceDialogTestImpl::playCancelSound()
{
    // do nothing
}

void QuasarVoiceDialogTestImpl::prepare(const std::string& /*uuid*/, const std::string& yandexUid,
                                        const std::string& /*timezone*/, const std::string& /*group*/, const std::string& /*subgroup*/)
{
    std::scoped_lock lock(mutex_);
    gotPrepared_ = true;
    yandexUid_ = yandexUid;
    speechWakeUpVar_.notify_all();
}

void QuasarVoiceDialogTestImpl::setSettings(::SpeechKit::VoiceServiceSettings settings)
{
    YIO_LOG_INFO("setSettings");
    std::scoped_lock lock(mutex_);
    settings_ = std::move(settings);
    speechWakeUpVar_.notify_all();
}

::SpeechKit::VoiceServiceSettings QuasarVoiceDialogTestImpl::getSettings() const {
    std::scoped_lock lock(mutex_);
    return settings_;
}

void QuasarVoiceDialogTestImpl::setPlayer(::SpeechKit::AudioPlayer::SharedPtr /*player*/)
{
}

void QuasarVoiceDialogTestImpl::callVinsResponse(const SpeechKit::VinsResponse& response)
{
    if (weakListener_.expired()) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }
    if (response.jsonPayload.empty()) {
        YIO_LOG_WARN("\n!!!!!!!!!!!!!!!!!!!\nPayload is empty\n!!!!!!!!!!!!!!!!!!!");
        exit(-1);
    }

    if (stopped_.load()) {
        return;
    }

    callbackQueue_.add([this, response]() {
        try {
            Json::Value payload = parseJson(response.jsonPayload);
            Json::Value voiceResponse = getJson(payload, "voice_response");
            bool hasOutputSpeech = !voiceResponse["output_speech"].isNull();

            std::shared_ptr<TestTTSDataProvider> newTtsDataProvider;
            if (hasOutputSpeech) {
                newTtsDataProvider = std::make_shared<TestTTSDataProvider>();
                ttsDataProviders_.push_back(newTtsDataProvider);
            }

            if (auto listener = weakListener_.lock()) {
                listener->onVinsResponse(nullptr, response, newTtsDataProvider);

                if (hasOutputSpeech) {
                    newTtsDataProvider->waitForListener();
                }
            }

            if (hasOutputSpeech) {
                std::lock_guard lock(mutex_);
                activationSpotterStarted_.store(false);
                updateState(SPEAKING_WITH_SPOTTER);
                outputSpeech_ = getString(getJson(voiceResponse, "output_speech"), "text");
                speechWakeUpVar_.notify_all();
                YIO_LOG_INFO("outputSpeech_ " << outputSpeech_);
            }
        } catch (const std::exception& e) {
            YIO_LOG_WARN("failed to callVinsResponse: " << e.what());
        }
    });
}

size_t QuasarVoiceDialogTestImpl::GetPhraseSpotterStartedCnt() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return startPhraseSpotterCnt_;
}

void QuasarVoiceDialogTestImpl::waitForStartedSpotter(size_t startNumber) const {
    std::unique_lock<std::mutex> lock(mutex_);
    startPhraseSpotterCntCV_.wait(lock, [startNumber, this] {
        return startNumber == startPhraseSpotterCnt_;
    });
}

std::string QuasarVoiceDialogTestImpl::getSpeech(int timeoutSec)
{
    auto listener = weakListener_.lock();
    if (listener == nullptr) {
        YIO_LOG_WARN("weakListener_ expired");
        return "";
    }

    {
        std::unique_lock<std::mutex> lock(mutex_);
        speechWakeUpVar_.wait_for(lock, std::chrono::seconds(timeoutSec), [this] {
            return !outputSpeech_.empty();
        });
        YIO_LOG_INFO("aliced said: " << outputSpeech_);
    }

    removeDeadProviders();
    if (!ttsDataProviders_.empty()) {
        for (auto provider : ttsDataProviders_) {
            if (auto sprovider = provider.lock()) {
                sprovider->setStreamEnd();
            }
        }
    }

    {
        std::lock_guard<std::mutex> lock(mutex_);
        std::string res;
        outputSpeech_.swap(res);
        return res;
    }
}

void QuasarVoiceDialogTestImpl::waitForYandexUid()
{
    std::unique_lock<std::mutex> lock(mutex_);
    speechWakeUpVar_.wait(lock, [=] {
        return !yandexUid_.empty();
    });
}

bool QuasarVoiceDialogTestImpl::waitForState(QuasarVoiceDialogTestImpl::State state, std::chrono::milliseconds timeout)
{
    std::unique_lock<std::mutex> lock(mutex_);
    stateWakeUpVar_.wait_for(lock, timeout, [=]() {
        return state_ == state;
    });
    if (state_ != state) {
        YIO_LOG_WARN("QuasarVoiceDialogTestImpl requiredState=" << map_[state] << ", actualState=" << map_[state_]);
    }
    return state_ == state;
}

::SpeechKit::PhraseSpotterSettings QuasarVoiceDialogTestImpl::waitForCommandSpotter(const std::string& spotter)
{
    std::unique_lock<std::mutex> lock(mutex_);
    speechWakeUpVar_.wait(lock, [&] {
        return activeCommandSpotter_ == spotter;
    });

    return activeCommandSpotterSettings_;
}

void QuasarVoiceDialogTestImpl::updateState(QuasarVoiceDialogTestImpl::State state)
{
    YIO_LOG_INFO("State changed: " << map_[state_] << " -> " << map_[state]);
    state_ = state;
    stateWakeUpVar_.notify_all();
}

void QuasarVoiceDialogTestImpl::removeDeadProviders()
{
    for (auto it = ttsDataProviders_.begin(); it != ttsDataProviders_.end();) {
        if (it->expired()) {
            it = ttsDataProviders_.erase(it);
        } else {
            ++it;
        }
    }
}

void QuasarVoiceDialogTestImpl::genRecognitionError(const SpeechKit::Error& error)
{
    auto listener = weakListener_.lock();
    if (listener == nullptr) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }
    listener->onRecognitionError(nullptr, requestId_, error);
}

void QuasarVoiceDialogTestImpl::GenVinsError(const SpeechKit::Error& error)
{
    auto listener = weakListener_.lock();
    if (listener == nullptr) {
        YIO_LOG_WARN("weakListener_ expired");
        return;
    }
    listener->onVinsError(nullptr, requestId_, error);
}

std::string QuasarVoiceDialogTestImpl::parseRequestId(const std::string& payloadStr) {
    try {
        YIO_LOG_INFO("payload=" << payloadStr);
        const Json::Value payload = parseJson(payloadStr);
        return payload["header"]["request_id"].asString();
    } catch (const std::exception& e) {
        YIO_LOG_WARN("Failed to parseRequestId: " << e.what());
        return "";
    }
}

SpeechKit::VinsResponse TestVins::getResponse(const std::string& payloadStr, const std::string& userSpeech, const std::string& requestId)
{
    YIO_LOG_INFO("#####################");
    YIO_LOG_INFO(payloadStr);
    Json::Value payload = lastPayload_ = parseJson(payloadStr);
    std::string dialogId;
    Json::Value request = getJson(payload, "request");
    Json::Value event = getJson(request, "event");
    std::string eventType = getString(event, "type");
    if (event.isMember("payload") && event["payload"].isMember("dialog_id")) {
        dialogId = event["payload"]["dialog_id"].asString();
        YIO_LOG_INFO("Found dialog with id " << dialogId);
    }

    std::string jsonPayload;
    std::string recognizedSpeech = std::string(userSpeech);
    if (eventType == "text_input") {
        recognizedSpeech = event["text"].asString();
    }
    YIO_LOG_INFO("eventType: " << eventType << ", recognizedSpeech: " << recognizedSpeech << ", requestId=" << requestId);

    if (!dialogId.empty()) {
        jsonPayload = "{\"header\":{\"dialog_id\":\"" + dialogId + "\",\"prev_req_id\":\"6e79a770-8f4a-4f84-a1c5-20e429dc9be4\",\"request_id\":\"" + requestId + R"("},"response":{"card":{"text":"Привет из залипающего навыка","type":"simple_text"},"cards":[{"text":"Привет из залипающего навыка","type":"simple_text"}],"directives":[]},"voice_response":{"directives":[],"output_speech":{"text":"Привет из залипающего навыка!.","type":"simple"},"should_listen":true}})";
        YIO_LOG_INFO(jsonPayload);
    } else if (eventType == "voice_input" || eventType == "text_input") {
        if (recognizedSpeech == "привет") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"6e79a770-8f4a-4f84-a1c5-20e429dc9be4\",\"request_id\":\"" + requestId + "\",\"response_id\":\"ef7fc4b3d4d8459281e9723342abd718\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"Хеллоу.\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"Хеллоу.\",\"type\":\"simple_text\"}],\"directives\":[],\"meta\":[],\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Давай поболтаем\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Давай поболтаем\",\"suggest_block\":{\"data\":{\"text\":\"Давай поболтаем\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Давай поболтаем\"},\"type\":\"server_action\"}],\"title\":\"Давай поболтаем\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Погода\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Погода\",\"suggest_block\":{\"data\":{\"text\":\"Погода\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Погода\"},\"type\":\"server_action\"}],\"title\":\"Погода\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сыграем в игру?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сыграем в игру?\",\"suggest_block\":{\"data\":{\"text\":\"Сыграем в игру?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сыграем в игру?\"},\"type\":\"server_action\"}],\"title\":\"Сыграем в игру?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сколько ехать до работы?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сколько ехать до работы?\",\"suggest_block\":{\"data\":{\"text\":\"Сколько ехать до работы?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сколько ехать до работы?\"},\"type\":\"server_action\"}],\"title\":\"Сколько ехать до работы?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сколько сейчас времени?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сколько сейчас времени?\",\"suggest_block\":{\"data\":{\"text\":\"Сколько сейчас времени?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сколько сейчас времени?\"},\"type\":\"server_action\"}],\"title\":\"Сколько сейчас времени?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Свежие новости\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Свежие новости\",\"suggest_block\":{\"data\":{\"text\":\"Свежие новости\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Свежие новости\"},\"type\":\"server_action\"}],\"title\":\"Свежие новости\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Как проехать в ближайший супермаркет?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Как проехать в ближайший супермаркет?\",\"suggest_block\":{\"data\":{\"text\":\"Как проехать в ближайший супермаркет?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Как проехать в ближайший супермаркет?\"},\"type\":\"server_action\"}],\"title\":\"Как проехать в ближайший супермаркет?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Куда сходить постричься?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Куда сходить постричься?\",\"suggest_block\":{\"data\":{\"text\":\"Куда сходить постричься?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Куда сходить постричься?\"},\"type\":\"server_action\"}],\"title\":\"Куда сходить постричься?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Ты всё знаешь?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Ты всё знаешь?\",\"suggest_block\":{\"data\":{\"text\":\"Ты всё знаешь?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Ты всё знаешь?\"},\"type\":\"server_action\"}],\"title\":\"Ты всё знаешь?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Стоимость доллара\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Стоимость доллара\",\"suggest_block\":{\"data\":{\"text\":\"Стоимость доллара\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Стоимость доллара\"},\"type\":\"server_action\"}],\"title\":\"Стоимость доллара\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Какой сегодня день?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Какой сегодня день?\",\"suggest_block\":{\"data\":{\"text\":\"Какой сегодня день?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Какой сегодня день?\"},\"type\":\"server_action\"}],\"title\":\"Какой сегодня день?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Что ты умеешь?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Что ты умеешь?\",\"suggest_block\":{\"data\":null,\"form_update\":null,\"suggest_type\":\"onboarding__what_can_you_do\",\"type\":\"suggest\"},\"utterance\":\"Что ты умеешь?\"},\"type\":\"server_action\"}],\"title\":\"Что ты умеешь?\",\"type\":\"action\"}]}},\"voice_response\":{\"directives\":[],\"output_speech\":{\"text\":\"Хеллоу.\",\"type\":\"simple\"},\"should_listen\":true}}";
        } else if (recognizedSpeech == "хватит") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"375b19dd-8655-4ad3-b235-3b5cecd7ad54\",\"request_id\":\"" + requestId + "\",\"response_id\":\"ac22a5669ca241b3b957e95c356de32d\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"ОК\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"ОК\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"player_pause\",\"payload\":null,\"type\":\"client_action\"}],\"meta\":[],\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Погода\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Погода\",\"suggest_block\":{\"data\":{\"text\":\"Погода\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Погода\"},\"type\":\"server_action\"}],\"title\":\"Погода\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Поболтаем?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Поболтаем?\",\"suggest_block\":{\"data\":{\"text\":\"Поболтаем?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Поболтаем?\"},\"type\":\"server_action\"}],\"title\":\"Поболтаем?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Пробки\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Пробки\",\"suggest_block\":{\"data\":{\"text\":\"Пробки\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Пробки\"},\"type\":\"server_action\"}],\"title\":\"Пробки\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Погода в воскресенье утром\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Погода в воскресенье утром\",\"suggest_block\":{\"data\":{\"text\":\"Погода в воскресенье утром\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Погода в воскресенье утром\"},\"type\":\"server_action\"}],\"title\":\"Погода в воскресенье утром\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сколько калорий в тёмном пиве?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сколько калорий в тёмном пиве?\",\"suggest_block\":{\"data\":{\"text\":\"Сколько калорий в тёмном пиве?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сколько калорий в тёмном пиве?\"},\"type\":\"server_action\"}],\"title\":\"Сколько калорий в тёмном пиве?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Смотреть Фиксики онлайн\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Смотреть Фиксики онлайн\",\"suggest_block\":{\"data\":{\"text\":\"Смотреть Фиксики онлайн\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Смотреть Фиксики онлайн\"},\"type\":\"server_action\"}],\"title\":\"Смотреть Фиксики онлайн\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Как доехать до ближайшего торгового центра?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Как доехать до ближайшего торгового центра?\",\"suggest_block\":{\"data\":{\"text\":\"Как доехать до ближайшего торгового центра?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Как доехать до ближайшего торгового центра?\"},\"type\":\"server_action\"}],\"title\":\"Как доехать до ближайшего торгового центра?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Где сейчас пообедать?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Где сейчас пообедать?\",\"suggest_block\":{\"data\":{\"text\":\"Где сейчас пообедать?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Где сейчас пообедать?\"},\"type\":\"server_action\"}],\"title\":\"Где сейчас пообедать?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"У тебя есть друзья?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"У тебя есть друзья?\",\"suggest_block\":{\"data\":{\"text\":\"У тебя есть друзья?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"У тебя есть друзья?\"},\"type\":\"server_action\"}],\"title\":\"У тебя есть друзья?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Курс доллара по ЦБ на сегодня\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Курс доллара по ЦБ на сегодня\",\"suggest_block\":{\"data\":{\"text\":\"Курс доллара по ЦБ на сегодня\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Курс доллара по ЦБ на сегодня\"},\"type\":\"server_action\"}],\"title\":\"Курс доллара по ЦБ на сегодня\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Что в новостях?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Что в новостях?\",\"suggest_block\":{\"data\":{\"text\":\"Что в новостях?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Что в новостях?\"},\"type\":\"server_action\"}],\"title\":\"Что в новостях?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Какой день в воскресенье?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Какой день в воскресенье?\",\"suggest_block\":{\"data\":{\"text\":\"Какой день в воскресенье?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Какой день в воскресенье?\"},\"type\":\"server_action\"}],\"title\":\"Какой день в воскресенье?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Точное время в Екатеринбурге\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Точное время в Екатеринбурге\",\"suggest_block\":{\"data\":{\"text\":\"Точное время в Екатеринбурге\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Точное время в Екатеринбурге\"},\"type\":\"server_action\"}],\"title\":\"Точное время в Екатеринбурге\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Что ты умеешь?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Что ты умеешь?\",\"suggest_block\":{\"data\":null,\"form_update\":null,\"suggest_type\":\"onboarding__what_can_you_do\",\"type\":\"suggest\"},\"utterance\":\"Что ты умеешь?\"},\"type\":\"server_action\"}],\"title\":\"Что ты умеешь?\",\"type\":\"action\"}]}},\"voice_response\":{\"directives\":[],\"output_speech\":null,\"should_listen\":false}}";
        } else if (recognizedSpeech == "дальше" || recognizedSpeech == "следующий") {
            jsonPayload = R"({"header": {"dialog_id": null, "prev_req_id": "85fd00ab-c715-4dc8-bac1-c425616d2552", "request_id": ")" + requestId + R"(", "response_id": "e0762c91135245e696319edf1576c589", "sequence_number": null}, "response": {"card": null, "cards": [], "directives": [{"name": "player_next_track", "payload": {"player": "music"}, "sub_name": "player_next_track", "type": "client_action"}], "experiments": {"alarm_how_long": "1", "alarm_snooze": "1", "ambient_sound": "1", "biometry_like": "1", "biometry_remove": "1", "change_alarm_sound": "1", "change_alarm_sound_music": "1", "change_alarm_sound_radio": "1", "change_alarm_with_sound": "1", "debug_mode": "1", "dialog_4178_newcards": "1", "disable_interruption_spotter": "1", "dj_service_for_games_onboarding": "1", "drm_tv_stream": "1", "enable_biometry_scoring": "1", "enable_ner_for_skills": "1", "enable_partials": "1", "enable_reminders_todos": "1", "enable_timers_alarms": "1", "enable_tts_gpu": "1", "ether": "https://yandex.ru/portal/station/main", "fairytale_fallback": "1", "fairytale_search_text_noprefix": "1", "film_gallery": "1", "fm_radio_recommend": "1", "forbidden_intents": "personal_assistant.scenarios.music_play,personal_assistant.scenarios.video_play,personal_assistant.scenarios.video_play_entity,personal_assistant.scenarios.quasar.select_video_from_gallery,personal_assistant.scenarios.quasar.goto_video_screen,personal_assistant.scenarios.quasar.payment_confirmed,personal_assistant.scenarios.quasar.authorize_video_provider,personal_assistant.scenarios.quasar.open_current_video,personal_assistant.scenarios.quasar.select_video_from_gallery_by_text,personal_assistant.scenarios.quasar.select_video_from_gallery,", "general_conversation": "1", "how_much": "1", "hw_gcp_proactivity_check_yandex_plus": "1", "hw_gcp_proactivity_timeout=0": "1", "ignore_trash_classified_results": "1", "iot": "1", "kv_saas_activation_experiment": "1", "market_beru_disable": "1", "market_disable": "1", "market_orders_status_disable": "1", "medium_ru_explicit_content": "1", "mm_enable_protocol_scenario=GameSuggest": "1", "mm_enable_protocol_scenario=GeneralConversationProactivity": "1", "mm_enable_protocol_scenario=HollywoodHardcodedMusic": "1", "mm_enable_protocol_scenario=MovieSuggest": "1", "mm_proactivity_dont_sort_targets": "1", "mm_proactivity_experimental_phrase=hdmi": "1", "mordovia": "1", "mordovia_long_listening": "1", "mordovia_support_channels": "1", "music": "1", "music_biometry": "1", "music_check_plus_promo": "1", "music_partials": "1", "music_personalization": "1", "music_recognizer": "1", "music_session": "1", "music_show_first_track": "1", "music_sing_song": "1", "music_use_websearch": "1", "new_fairytale_quasar": "1", "new_music_radio_nlg": "1", "new_nlg": "1", "new_special_playlists": "1", "personal_tv_channel": "1", "personal_tv_help": "1", "personalization": "1", "podcasts": "1", "pure_general_conversation": "1", "quasar": "1", "quasar_biometry_limit_users": "1", "quasar_gc_instead_of_search": "1", "quasar_tv": "1", "radio_fixes": "1", "radio_play_in_quasar": "1", "radio_play_onboarding": "1", "read_factoid_source": "1", "recurring_purchase": "1", "shopping_list": "1", "sleep_timers": "1", "smart_home_asr_help": "1", "supress_multi_activation": "1", "taxi": "1", "taxi_nlu": "1", "translate": "1", "tts_domain_music": "1", "tv": "1", "tv_stream": "1", "tv_vod_translation": "1", "tv_without_channel_status_check": "1", "ugc_enabled": "1", "uniproxy_vins_sessions": "1", "use_trash_talk_classifier": "1", "username_auto_insert": "1", "video_enable_telemetry": "1", "video_not_use_native_youtube_api": "1", "video_omit_youtube_restriction": "1", "video_qproxy_players": "1", "vins_e2e_partials": "1", "weather_precipitation": "1", "weather_precipitation_starts_ends": "1", "weather_precipitation_type": "1"}, "features": {"form_info": {"intent": "personal_assistant.scenarios.player_next_track", "is_continuing": false}}, "megamind_actions": null, "meta": [{"bass_search_result": null, "form": {"form": "personal_assistant.scenarios.player_next_track", "is_ellipsis": false, "shares_slots_with_previous_form": false, "slots": [{"active": false, "allow_multiple": false, "concatenation": "forbid", "disabled": false, "expected_values": null, "import_entity_pronouns": [], "import_entity_tags": [], "import_entity_types": [], "import_tags": [], "matching_type": "exact", "normalize_to": null, "optional": true, "share_tags": [], "slot": "player_type", "source_text": null, "types": ["player_type"], "value": null, "value_type": null}]}, "intent": "personal_assistant.scenarios.player_next_track", "proactivity_info": null, "type": "analytics_info"}], "quality_storage": {"post_predicts": {"alice.vins": 5.41257429}, "post_win_reason": "WR_PRIORITY", "pre_predicts": {"FindPoi": 0, "HardcodedResponse": 0, "HollywoodMusic": 1.14855111, "Video": -6.14278507, "Wizard": 0, "alice.iot_do": 0, "alice.vins": 0, "personal_assistant.scenarios.player.next_track": 0}}, "suggest": null}, "voice_response": {"directives": [], "output_speech": null, "should_listen": false}})";
        } else if (recognizedSpeech == "привет как дела") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"a06e5300-7ab9-4a41-9cb4-1727270bdc98\",\"request_id\":\"" + requestId + "\",\"response_id\":\"787c301a844a49afb9e15d0d2f1072ff\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"Здравствуйте.\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"Здравствуйте.\",\"type\":\"simple_text\"}],\"directives\":[],\"meta\":[],\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Давай поболтаем\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Давай поболтаем\",\"suggest_block\":{\"data\":{\"text\":\"Давай поболтаем\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Давай поболтаем\"},\"type\":\"server_action\"}],\"title\":\"Давай поболтаем\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Погода\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Погода\",\"suggest_block\":{\"data\":{\"text\":\"Погода\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Погода\"},\"type\":\"server_action\"}],\"title\":\"Погода\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сыграем в игру?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сыграем в игру?\",\"suggest_block\":{\"data\":{\"text\":\"Сыграем в игру?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сыграем в игру?\"},\"type\":\"server_action\"}],\"title\":\"Сыграем в игру?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сколько ехать до работы?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сколько ехать до работы?\",\"suggest_block\":{\"data\":{\"text\":\"Сколько ехать до работы?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сколько ехать до работы?\"},\"type\":\"server_action\"}],\"title\":\"Сколько ехать до работы?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сколько сейчас времени?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сколько сейчас времени?\",\"suggest_block\":{\"data\":{\"text\":\"Сколько сейчас времени?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сколько сейчас времени?\"},\"type\":\"server_action\"}],\"title\":\"Сколько сейчас времени?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Свежие новости\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Свежие новости\",\"suggest_block\":{\"data\":{\"text\":\"Свежие новости\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Свежие новости\"},\"type\":\"server_action\"}],\"title\":\"Свежие новости\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Как проехать в ближайший супермаркет?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Как проехать в ближайший супермаркет?\",\"suggest_block\":{\"data\":{\"text\":\"Как проехать в ближайший супермаркет?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Как проехать в ближайший супермаркет?\"},\"type\":\"server_action\"}],\"title\":\"Как проехать в ближайший супермаркет?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Куда сходить постричься?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Куда сходить постричься?\",\"suggest_block\":{\"data\":{\"text\":\"Куда сходить постричься?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Куда сходить постричься?\"},\"type\":\"server_action\"}],\"title\":\"Куда сходить постричься?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Ты всё знаешь?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Ты всё знаешь?\",\"suggest_block\":{\"data\":{\"text\":\"Ты всё знаешь?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Ты всё знаешь?\"},\"type\":\"server_action\"}],\"title\":\"Ты всё знаешь?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Стоимость доллара\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Стоимость доллара\",\"suggest_block\":{\"data\":{\"text\":\"Стоимость доллара\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Стоимость доллара\"},\"type\":\"server_action\"}],\"title\":\"Стоимость доллара\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Какой сегодня день?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Какой сегодня день?\",\"suggest_block\":{\"data\":{\"text\":\"Какой сегодня день?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Какой сегодня день?\"},\"type\":\"server_action\"}],\"title\":\"Какой сегодня день?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Что ты умеешь?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Что ты умеешь?\",\"suggest_block\":{\"data\":null,\"form_update\":null,\"suggest_type\":\"onboarding__what_can_you_do\",\"type\":\"suggest\"},\"utterance\":\"Что ты умеешь?\"},\"type\":\"server_action\"}],\"title\":\"Что ты умеешь?\",\"type\":\"action\"}]}},\"voice_response\":{\"directives\":[],\"output_speech\":{\"text\":\"Здравствуйте.\",\"type\":\"simple\"},\"should_listen\":true}}";
        } else if (recognizedSpeech == "включи музыку") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"1c6df5e0-0e8e-4b0d-b6ea-93f15c31a19d\",\"request_id\":\"" + requestId + "\",\"response_id\":\"714fae8e3dd342d9bb97dfd4ba2fc978\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"Включаю.\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"Включаю.\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"music_play\",\"payload\":{\"first_track_id\":\"418017\",\"session_id\":\"UnaeQVcm\",\"uri\":\"\"},\"type\":\"client_action\"}],\"meta\":[],\"suggest\":null},\"voice_response\":{\"directives\":[],\"output_speech\":{\"text\":\"<[domain music]> Послушайте Skillet, песня \\\"Hero\\\" <[/domain]>\",\"type\":\"simple\"},\"should_listen\":false}}";
        } else if (recognizedSpeech == "что играет") {
            jsonPayload = R"({"header":{"dialog_id":null,"prev_req_id":null,"request_id":")" + requestId + R"(","response_id":"a45cae390d8f40318d70ece50783a093","sequence_number":0},"response":{"card":{"tag":null,"text":"Дайте-ка прислушаться...","type":"simple_text"},"cards":[{"tag":null,"text":"Дайте-ка прислушаться...","type":"simple_text"}],"directives":[{"name":"start_music_recognizer","payload":null,"sub_name":"music_start_recognizer","type":"client_action"}],"features":{"form_info":{"intent":"personal_assistant.scenarios.music_what_is_playing","is_continuing":false}}},"voice_response":{"directives":[],"output_speech":{"text":"Дайте-ка прислушаться...","type":"simple"},"should_listen":false}})";
        } else if (recognizedSpeech == "удали все таймеры") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"12f6fdfa-2481-4802-88ef-e6f72a4074e6\",\"request_id\":\"" + requestId + "\",\"response_id\":\"6d18a7ecc33148fca3c06959346a3555\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"Хорошо, удалила все таймеры.\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"Хорошо, удалила все таймеры.\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"cancel_timer\",\"payload\":{\"timer_id\":\"4b360f3d-6d45-444b-8ff5-a0e27b54c84a\"},\"type\":\"client_action\"},{\"name\":\"cancel_timer\",\"payload\":{\"timer_id\":\"e11796d6-8f70-455f-865f-8a4a885ce35c\"},\"type\":\"client_action\"},{\"name\":\"cancel_timer\",\"payload\":{\"timer_id\":\"2a6029ae-869a-4ff8-8faa-e29632ef7cca\"},\"type\":\"client_action\"}],\"meta\":[],\"suggest\":null},\"voice_response\":{\"directives\":[],\"output_speech\":{\"text\":\"Хорошо, удалила все т+аймеры.\",\"type\":\"simple\"},\"should_listen\":false}}";
        } else if (recognizedSpeech == "выключи музыку") {
            jsonPayload = "";
        } else if (recognizedSpeech == "таймер на выключение через 1 секунду") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"a;sdkfj;qewjr;kjdf;sdfj;sfj\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"Поставила таймер на 1 секунду.\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"Поставила таймер на 1 секунду.\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"set_timer\",\"payload\":{\"duration\":1,\"directives\":[{\"name\":\"player_pause\"},{\"name\":\"go_home\"}]},\"type\":\"client_action\"}],\"meta\":[],\"suggest\":null},\"voice_response\":{\"directives\":[],\"output_speech\":{\"text\":\"Поставила т+аймер на #acc 1 секунду.\",\"type\":\"simple\"},\"should_listen\":false}}";
        } else if (recognizedSpeech == "таймер на выключение через 100 секунд") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"a;kdjf;sadjkf;asdjkf;adjksf;\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"Поставила таймер на 1 секунду.\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"Поставила таймер на 1 секунду.\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"set_timer\",\"payload\":{\"duration\":100,\"directives\": " + sleepTimerTestDirectives + "},\"type\":\"client_action\"}],\"meta\":[],\"suggest\":null},\"voice_response\":{\"directives\":[],\"output_speech\":{\"text\":\"Поставила т+аймер на #acc 1 секунду.\",\"type\":\"simple\"},\"should_listen\":false}}";
        } else if (recognizedSpeech == "дай паровоз №1") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"7f2e57b0-61aafbeb-3167783a-c3838e11\",\"sequence_number\":1,\"session_id\":\"5bf93369-308b-42aa-88e4-5fc07bab7238\"},\"response\":{\"card\":{\"text\":\"hi\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"hi\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"audio_play\",\"payload\":{\"background_mode\":\"Ducking\",\"callbacks\":{\"on_failed\":{\"ignore_answer\":true,\"name\":\"on_play_failed\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"},\"on_finished\":{\"ignore_answer\":true,\"name\":\"on_play_finished\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"},\"on_started\":{\"ignore_answer\":true,\"name\":\"on_play_started\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"},\"on_stopped\":{\"ignore_answer\":true,\"name\":\"on_play_stopped\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"}},\"metadata\":{\"art_image_url\":\"https://jazzquad.ru/img/pages/2787.jpg?ver=135179126414?w=250\",\"subtitle\":\"hi\",\"title\":\"hi\"},\"scenario_meta\":{\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"stream\":{\"format\":\"MP3\",\"id\":\"token\",\"offset_ms\":0,\"url\":\"https://quasar.s3.yandex.net/audioclient/test/record1.mp3\"}},\"sub_name\":\"audio_play\",\"type\":\"client_action\"},{\"name\":\"last_directive\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"}],\"directives_execution_policy\":\"BeforeSpeech\",\"experiments\":{},\"meta\":[{\"form\":{\"form\":\"Dialogovo\",\"slots\":[]},\"intent\":\"Dialogovo\",\"type\":\"analytics_info\"}],\"quality_storage\":{\"pre_predicts\":{\"Dialogovo\":0,\"FindPoi\":0,\"GeneralConversation\":0,\"HardcodedResponse\":0,\"HollywoodMusic\":-2.5953352500000002,\"Search\":-3.4804604100000001,\"Video\":-1.0056351400000001,\"Wizard\":0,\"alice.iot_do\":0,\"alice.vins\":0}},\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"hi.\"},\"sub_name\":\"external_skill__type_text\",\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"alice.vins\",\"button_id\":\"21d2873f-51583301-b11997d1-8e64a360\",\"caption\":\"hi\",\"request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"scenario_name\":\"Dialogovo\"},\"type\":\"server_action\"}],\"title\":\"hi\",\"type\":\"action\"}]},\"templates\":{}},\"voice_response\":{\"output_speech\":{\"text\":\"text\",\"type\":\"simple\"},\"should_listen\":false}}";
        } else if (recognizedSpeech == "дай паровоз №2") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"7f2e57b0-61aafbeb-3167783a-c3838e11\",\"sequence_number\":1,\"session_id\":\"5bf93369-308b-42aa-88e4-5fc07bab7238\"},\"response\":{\"card\":{\"text\":\"hi\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"hi\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"audio_play\",\"payload\":{\"background_mode\":\"Ducking\",\"callbacks\":{\"on_failed\":{\"ignore_answer\":true,\"name\":\"on_play_failed\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"},\"on_finished\":{\"ignore_answer\":true,\"name\":\"on_play_finished\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"},\"on_started\":{\"ignore_answer\":true,\"name\":\"on_play_started\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"},\"on_stopped\":{\"ignore_answer\":true,\"name\":\"on_play_stopped\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"}},\"metadata\":{\"art_image_url\":\"https://jazzquad.ru/img/pages/2787.jpg?ver=135179126414?w=250\",\"subtitle\":\"hi\",\"title\":\"hi\"},\"scenario_meta\":{\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"stream\":{\"format\":\"MP3\",\"id\":\"token\",\"offset_ms\":0,\"url\":\"https://quasar.s3.yandex.net/audioclient/test/record1.mp3\"}},\"sub_name\":\"audio_play\",\"type\":\"client_action\"},{\"name\":\"last_directive\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"}],\"directives_execution_policy\":\"BeforeSpeech\",\"experiments\":{},\"meta\":[{\"form\":{\"form\":\"Dialogovo\",\"slots\":[]},\"intent\":\"Dialogovo\",\"type\":\"analytics_info\"}],\"quality_storage\":{\"pre_predicts\":{\"Dialogovo\":0,\"FindPoi\":0,\"GeneralConversation\":0,\"HardcodedResponse\":0,\"HollywoodMusic\":-2.5953352500000002,\"Search\":-3.4804604100000001,\"Video\":-1.0056351400000001,\"Wizard\":0,\"alice.iot_do\":0,\"alice.vins\":0}},\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"hi.\"},\"sub_name\":\"external_skill__type_text\",\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"alice.vins\",\"button_id\":\"21d2873f-51583301-b11997d1-8e64a360\",\"caption\":\"hi\",\"request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"scenario_name\":\"Dialogovo\"},\"type\":\"server_action\"}],\"title\":\"hi\",\"type\":\"action\"}]},\"templates\":{}},\"voice_response\":{\"output_speech\":{\"text\":\"text\",\"type\":\"simple\"},\"should_listen\":false}}";
        } else if (recognizedSpeech == "дай паровоз c get_next") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"7f2e57b0-61aafbeb-3167783a-c3838e11\",\"sequence_number\":1,\"session_id\":\"5bf93369-308b-42aa-88e4-5fc07bab7238\"},\"response\":{\"card\":{\"text\":\"hi\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"hi\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"audio_play\",\"payload\":{\"background_mode\":\"Ducking\",\"callbacks\":{\"on_failed\":{\"ignore_answer\":true,\"name\":\"on_play_failed\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"},\"on_finished\":{\"ignore_answer\":true,\"name\":\"on_play_finished\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"},\"on_started\":{\"ignore_answer\":true,\"name\":\"on_play_started\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"},\"on_stopped\":{\"ignore_answer\":true,\"name\":\"on_play_stopped\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"}},\"metadata\":{\"art_image_url\":\"https://jazzquad.ru/img/pages/2787.jpg?ver=135179126414?w=250\",\"subtitle\":\"hi\",\"title\":\"hi\"},\"scenario_meta\":{\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"stream\":{\"format\":\"MP3\",\"id\":\"token\",\"offset_ms\":0,\"url\":\"https://quasar.s3.yandex.net/audioclient/test/record1.mp3\"}},\"sub_name\":\"audio_play\",\"type\":\"client_action\"},{\"name\":\"get_next_audio_play_item\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"}],\"directives_execution_policy\":\"BeforeSpeech\",\"experiments\":{},\"meta\":[{\"form\":{\"form\":\"Dialogovo\",\"slots\":[]},\"intent\":\"Dialogovo\",\"type\":\"analytics_info\"}],\"quality_storage\":{\"pre_predicts\":{\"Dialogovo\":0,\"FindPoi\":0,\"GeneralConversation\":0,\"HardcodedResponse\":0,\"HollywoodMusic\":-2.5953352500000002,\"Search\":-3.4804604100000001,\"Video\":-1.0056351400000001,\"Wizard\":0,\"alice.iot_do\":0,\"alice.vins\":0}},\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"hi.\"},\"sub_name\":\"external_skill__type_text\",\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"alice.vins\",\"button_id\":\"21d2873f-51583301-b11997d1-8e64a360\",\"caption\":\"hi\",\"request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"scenario_name\":\"Dialogovo\"},\"type\":\"server_action\"}],\"title\":\"hi\",\"type\":\"action\"}]},\"templates\":{}},\"voice_response\":{\"output_speech\":{\"text\":\"text\",\"type\":\"simple\"},\"should_listen\":false}}";
        } else if (recognizedSpeech == "дай паровоз c server_action") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"7f2e57b0-61aafbeb-3167783a-c3838e11\",\"sequence_number\":1,\"session_id\":\"5bf93369-308b-42aa-88e4-5fc07bab7238\"},\"response\":{\"card\":{\"text\":\"hi\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"hi\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"server_action1\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"}],\"directives_execution_policy\":\"BeforeSpeech\",\"experiments\":{},\"meta\":[{\"form\":{\"form\":\"Dialogovo\",\"slots\":[]},\"intent\":\"Dialogovo\",\"type\":\"analytics_info\"}],\"quality_storage\":{\"pre_predicts\":{\"Dialogovo\":0,\"FindPoi\":0,\"GeneralConversation\":0,\"HardcodedResponse\":0,\"HollywoodMusic\":-2.5953352500000002,\"Search\":-3.4804604100000001,\"Video\":-1.0056351400000001,\"Wizard\":0,\"alice.iot_do\":0,\"alice.vins\":0}},\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"hi.\"},\"sub_name\":\"external_skill__type_text\",\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"alice.vins\",\"button_id\":\"21d2873f-51583301-b11997d1-8e64a360\",\"caption\":\"hi\",\"request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"scenario_name\":\"Dialogovo\"},\"type\":\"server_action\"}],\"title\":\"hi\",\"type\":\"action\"}]},\"templates\":{}},\"voice_response\":{\"should_listen\":false}}";
        } else if (recognizedSpeech == "прерывание паровоза") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"6e79a770-8f4a-4f84-a1c5-20e429dc9be4\",\"request_id\":\"" + requestId + "\",\"response_id\":\"ef7fc4b3d4d8459281e9723342abd718\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"Хеллоу.\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"Хеллоу.\",\"type\":\"simple_text\"}],\"directives\":[],\"meta\":[],\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Давай поболтаем\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Давай поболтаем\",\"suggest_block\":{\"data\":{\"text\":\"Давай поболтаем\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Давай поболтаем\"},\"type\":\"server_action\"}],\"title\":\"Давай поболтаем\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Погода\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Погода\",\"suggest_block\":{\"data\":{\"text\":\"Погода\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Погода\"},\"type\":\"server_action\"}],\"title\":\"Погода\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сыграем в игру?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сыграем в игру?\",\"suggest_block\":{\"data\":{\"text\":\"Сыграем в игру?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сыграем в игру?\"},\"type\":\"server_action\"}],\"title\":\"Сыграем в игру?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сколько ехать до работы?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сколько ехать до работы?\",\"suggest_block\":{\"data\":{\"text\":\"Сколько ехать до работы?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сколько ехать до работы?\"},\"type\":\"server_action\"}],\"title\":\"Сколько ехать до работы?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сколько сейчас времени?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сколько сейчас времени?\",\"suggest_block\":{\"data\":{\"text\":\"Сколько сейчас времени?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сколько сейчас времени?\"},\"type\":\"server_action\"}],\"title\":\"Сколько сейчас времени?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Свежие новости\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Свежие новости\",\"suggest_block\":{\"data\":{\"text\":\"Свежие новости\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Свежие новости\"},\"type\":\"server_action\"}],\"title\":\"Свежие новости\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Как проехать в ближайший супермаркет?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Как проехать в ближайший супермаркет?\",\"suggest_block\":{\"data\":{\"text\":\"Как проехать в ближайший супермаркет?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Как проехать в ближайший супермаркет?\"},\"type\":\"server_action\"}],\"title\":\"Как проехать в ближайший супермаркет?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Куда сходить постричься?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Куда сходить постричься?\",\"suggest_block\":{\"data\":{\"text\":\"Куда сходить постричься?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Куда сходить постричься?\"},\"type\":\"server_action\"}],\"title\":\"Куда сходить постричься?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Ты всё знаешь?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Ты всё знаешь?\",\"suggest_block\":{\"data\":{\"text\":\"Ты всё знаешь?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Ты всё знаешь?\"},\"type\":\"server_action\"}],\"title\":\"Ты всё знаешь?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Стоимость доллара\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Стоимость доллара\",\"suggest_block\":{\"data\":{\"text\":\"Стоимость доллара\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Стоимость доллара\"},\"type\":\"server_action\"}],\"title\":\"Стоимость доллара\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Какой сегодня день?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Какой сегодня день?\",\"suggest_block\":{\"data\":{\"text\":\"Какой сегодня день?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Какой сегодня день?\"},\"type\":\"server_action\"}],\"title\":\"Какой сегодня день?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Что ты умеешь?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Что ты умеешь?\",\"suggest_block\":{\"data\":null,\"form_update\":null,\"suggest_type\":\"onboarding__what_can_you_do\",\"type\":\"suggest\"},\"utterance\":\"Что ты умеешь?\"},\"type\":\"server_action\"}],\"title\":\"Что ты умеешь?\",\"type\":\"action\"}]}},\"voice_response\":{\"directives\":[],\"output_speech\":{\"text\":\"Хеллоу.\",\"type\":\"simple\"},\"should_listen\":false}}";
        } else if (recognizedSpeech == "дай audio_stop") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"a;dkfj;askjdf;asjkdf;sajkd;ajk\",\"sequence_number\":1,\"session_id\":\"5bf93369-308b-42aa-88e4-5fc07bab7238\"},\"response\":{\"card\":{\"text\":\"hi\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"hi\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"audio_stop\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"client_action\"}],\"directives_execution_policy\":\"BeforeSpeech\",\"experiments\":{},\"meta\":[{\"form\":{\"form\":\"Dialogovo\",\"slots\":[]},\"intent\":\"Dialogovo\",\"type\":\"analytics_info\"}],\"quality_storage\":{\"pre_predicts\":{\"Dialogovo\":0,\"FindPoi\":0,\"GeneralConversation\":0,\"HardcodedResponse\":0,\"HollywoodMusic\":-2.5953352500000002,\"Search\":-3.4804604100000001,\"Video\":-1.0056351400000001,\"Wizard\":0,\"alice.iot_do\":0,\"alice.vins\":0}},\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"hi.\"},\"sub_name\":\"external_skill__type_text\",\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"alice.vins\",\"button_id\":\"21d2873f-51583301-b11997d1-8e64a360\",\"caption\":\"hi\",\"request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"scenario_name\":\"Dialogovo\"},\"type\":\"server_action\"}],\"title\":\"hi\",\"type\":\"action\"}]},\"templates\":{}},\"voice_response\":{\"output_speech\":{\"text\":\"text\",\"type\":\"simple\"},\"should_listen\":false}}";
        }
    } else if (eventType == "server_action") {
        if (event["name"] == "get_next_audio_play_item") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"7f2e57b0-61aafbeb-3167783a-c3838e11\",\"sequence_number\":1,\"session_id\":\"5bf93369-308b-42aa-88e4-5fc07bab7238\"},\"response\":{\"card\":{\"text\":\"hi\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"hi\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"prefetched_directive\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"}],\"directives_execution_policy\":\"BeforeSpeech\",\"experiments\":{},\"meta\":[{\"form\":{\"form\":\"Dialogovo\",\"slots\":[]},\"intent\":\"Dialogovo\",\"type\":\"analytics_info\"}],\"quality_storage\":{\"pre_predicts\":{\"Dialogovo\":0,\"FindPoi\":0,\"GeneralConversation\":0,\"HardcodedResponse\":0,\"HollywoodMusic\":-2.5953352500000002,\"Search\":-3.4804604100000001,\"Video\":-1.0056351400000001,\"Wizard\":0,\"alice.iot_do\":0,\"alice.vins\":0}},\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"hi.\"},\"sub_name\":\"external_skill__type_text\",\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"alice.vins\",\"button_id\":\"21d2873f-51583301-b11997d1-8e64a360\",\"caption\":\"hi\",\"request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"scenario_name\":\"Dialogovo\"},\"type\":\"server_action\"}],\"title\":\"hi\",\"type\":\"action\"}]},\"templates\":{}},\"voice_response\":{\"should_listen\":false}}";
        } else if (event["name"] == "last_directive") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"asdfasdfsdfsadfasdf\",\"request_id\":\"" + requestId + "\",\"response_id\":\"ac22a5669ca241b3b957e95c356de32d\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"ОК\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"ОК\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"player_pause\",\"payload\":null,\"type\":\"client_action\"}],\"meta\":[],\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Погода\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Погода\",\"suggest_block\":{\"data\":{\"text\":\"Погода\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Погода\"},\"type\":\"server_action\"}],\"title\":\"Погода\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Поболтаем?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Поболтаем?\",\"suggest_block\":{\"data\":{\"text\":\"Поболтаем?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Поболтаем?\"},\"type\":\"server_action\"}],\"title\":\"Поболтаем?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Пробки\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Пробки\",\"suggest_block\":{\"data\":{\"text\":\"Пробки\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Пробки\"},\"type\":\"server_action\"}],\"title\":\"Пробки\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Погода в воскресенье утром\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Погода в воскресенье утром\",\"suggest_block\":{\"data\":{\"text\":\"Погода в воскресенье утром\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Погода в воскресенье утром\"},\"type\":\"server_action\"}],\"title\":\"Погода в воскресенье утром\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сколько калорий в тёмном пиве?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сколько калорий в тёмном пиве?\",\"suggest_block\":{\"data\":{\"text\":\"Сколько калорий в тёмном пиве?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сколько калорий в тёмном пиве?\"},\"type\":\"server_action\"}],\"title\":\"Сколько калорий в тёмном пиве?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Смотреть Фиксики онлайн\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Смотреть Фиксики онлайн\",\"suggest_block\":{\"data\":{\"text\":\"Смотреть Фиксики онлайн\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Смотреть Фиксики онлайн\"},\"type\":\"server_action\"}],\"title\":\"Смотреть Фиксики онлайн\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Как доехать до ближайшего торгового центра?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Как доехать до ближайшего торгового центра?\",\"suggest_block\":{\"data\":{\"text\":\"Как доехать до ближайшего торгового центра?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Как доехать до ближайшего торгового центра?\"},\"type\":\"server_action\"}],\"title\":\"Как доехать до ближайшего торгового центра?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Где сейчас пообедать?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Где сейчас пообедать?\",\"suggest_block\":{\"data\":{\"text\":\"Где сейчас пообедать?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Где сейчас пообедать?\"},\"type\":\"server_action\"}],\"title\":\"Где сейчас пообедать?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"У тебя есть друзья?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"У тебя есть друзья?\",\"suggest_block\":{\"data\":{\"text\":\"У тебя есть друзья?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"У тебя есть друзья?\"},\"type\":\"server_action\"}],\"title\":\"У тебя есть друзья?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Курс доллара по ЦБ на сегодня\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Курс доллара по ЦБ на сегодня\",\"suggest_block\":{\"data\":{\"text\":\"Курс доллара по ЦБ на сегодня\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Курс доллара по ЦБ на сегодня\"},\"type\":\"server_action\"}],\"title\":\"Курс доллара по ЦБ на сегодня\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Что в новостях?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Что в новостях?\",\"suggest_block\":{\"data\":{\"text\":\"Что в новостях?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Что в новостях?\"},\"type\":\"server_action\"}],\"title\":\"Что в новостях?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Какой день в воскресенье?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Какой день в воскресенье?\",\"suggest_block\":{\"data\":{\"text\":\"Какой день в воскресенье?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Какой день в воскресенье?\"},\"type\":\"server_action\"}],\"title\":\"Какой день в воскресенье?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Точное время в Екатеринбурге\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Точное время в Екатеринбурге\",\"suggest_block\":{\"data\":{\"text\":\"Точное время в Екатеринбурге\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Точное время в Екатеринбурге\"},\"type\":\"server_action\"}],\"title\":\"Точное время в Екатеринбурге\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Что ты умеешь?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Что ты умеешь?\",\"suggest_block\":{\"data\":null,\"form_update\":null,\"suggest_type\":\"onboarding__what_can_you_do\",\"type\":\"suggest\"},\"utterance\":\"Что ты умеешь?\"},\"type\":\"server_action\"}],\"title\":\"Что ты умеешь?\",\"type\":\"action\"}]}},\"voice_response\":{\"directives\":[],\"output_speech\":null,\"should_listen\":false}}";
        } else if (event["name"] == "prefetched_directive") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"375b19dd-8655-4ad3-b235-3b5cecd7ad54\",\"request_id\":\"" + requestId + "\",\"response_id\":\"ac22a5669ca241b3b957e95c356de32d\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"ОК\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"ОК\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"player_pause\",\"payload\":null,\"type\":\"client_action\"}],\"meta\":[],\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Погода\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Погода\",\"suggest_block\":{\"data\":{\"text\":\"Погода\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Погода\"},\"type\":\"server_action\"}],\"title\":\"Погода\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Поболтаем?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Поболтаем?\",\"suggest_block\":{\"data\":{\"text\":\"Поболтаем?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Поболтаем?\"},\"type\":\"server_action\"}],\"title\":\"Поболтаем?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Пробки\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Пробки\",\"suggest_block\":{\"data\":{\"text\":\"Пробки\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Пробки\"},\"type\":\"server_action\"}],\"title\":\"Пробки\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Погода в воскресенье утром\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Погода в воскресенье утром\",\"suggest_block\":{\"data\":{\"text\":\"Погода в воскресенье утром\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Погода в воскресенье утром\"},\"type\":\"server_action\"}],\"title\":\"Погода в воскресенье утром\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Сколько калорий в тёмном пиве?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Сколько калорий в тёмном пиве?\",\"suggest_block\":{\"data\":{\"text\":\"Сколько калорий в тёмном пиве?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Сколько калорий в тёмном пиве?\"},\"type\":\"server_action\"}],\"title\":\"Сколько калорий в тёмном пиве?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Смотреть Фиксики онлайн\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Смотреть Фиксики онлайн\",\"suggest_block\":{\"data\":{\"text\":\"Смотреть Фиксики онлайн\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Смотреть Фиксики онлайн\"},\"type\":\"server_action\"}],\"title\":\"Смотреть Фиксики онлайн\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Как доехать до ближайшего торгового центра?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Как доехать до ближайшего торгового центра?\",\"suggest_block\":{\"data\":{\"text\":\"Как доехать до ближайшего торгового центра?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Как доехать до ближайшего торгового центра?\"},\"type\":\"server_action\"}],\"title\":\"Как доехать до ближайшего торгового центра?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Где сейчас пообедать?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Где сейчас пообедать?\",\"suggest_block\":{\"data\":{\"text\":\"Где сейчас пообедать?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Где сейчас пообедать?\"},\"type\":\"server_action\"}],\"title\":\"Где сейчас пообедать?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"У тебя есть друзья?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"У тебя есть друзья?\",\"suggest_block\":{\"data\":{\"text\":\"У тебя есть друзья?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"У тебя есть друзья?\"},\"type\":\"server_action\"}],\"title\":\"У тебя есть друзья?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Курс доллара по ЦБ на сегодня\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Курс доллара по ЦБ на сегодня\",\"suggest_block\":{\"data\":{\"text\":\"Курс доллара по ЦБ на сегодня\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Курс доллара по ЦБ на сегодня\"},\"type\":\"server_action\"}],\"title\":\"Курс доллара по ЦБ на сегодня\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Что в новостях?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Что в новостях?\",\"suggest_block\":{\"data\":{\"text\":\"Что в новостях?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Что в новостях?\"},\"type\":\"server_action\"}],\"title\":\"Что в новостях?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Какой день в воскресенье?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Какой день в воскресенье?\",\"suggest_block\":{\"data\":{\"text\":\"Какой день в воскресенье?\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Какой день в воскресенье?\"},\"type\":\"server_action\"}],\"title\":\"Какой день в воскресенье?\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Точное время в Екатеринбурге\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Точное время в Екатеринбурге\",\"suggest_block\":{\"data\":{\"text\":\"Точное время в Екатеринбурге\"},\"form_update\":null,\"suggest_type\":\"from_microintent\",\"type\":\"suggest\"},\"utterance\":\"Точное время в Екатеринбурге\"},\"type\":\"server_action\"}],\"title\":\"Точное время в Екатеринбурге\",\"type\":\"action\"},{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"Что ты умеешь?\"},\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"caption\":\"Что ты умеешь?\",\"suggest_block\":{\"data\":null,\"form_update\":null,\"suggest_type\":\"onboarding__what_can_you_do\",\"type\":\"suggest\"},\"utterance\":\"Что ты умеешь?\"},\"type\":\"server_action\"}],\"title\":\"Что ты умеешь?\",\"type\":\"action\"}]}},\"voice_response\":{\"directives\":[],\"output_speech\":null,\"should_listen\":false}}";
        } else if (event["name"] == "server_action1") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"7f2e57b0-61aafbeb-3167783a-c3838e11\",\"sequence_number\":1,\"session_id\":\"5bf93369-308b-42aa-88e4-5fc07bab7238\"},\"response\":{\"card\":{\"text\":\"hi\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"hi\",\"type\":\"simple_text\"}],\"directives\":[{\"name\":\"server_action2\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"Dialogovo\",\"skillId\":\"a1921c40-60af-4390-96b1-ea5d369c4c4a\"},\"type\":\"server_action\"}],\"directives_execution_policy\":\"BeforeSpeech\",\"experiments\":{},\"meta\":[{\"form\":{\"form\":\"Dialogovo\",\"slots\":[]},\"intent\":\"Dialogovo\",\"type\":\"analytics_info\"}],\"quality_storage\":{\"pre_predicts\":{\"Dialogovo\":0,\"FindPoi\":0,\"GeneralConversation\":0,\"HardcodedResponse\":0,\"HollywoodMusic\":-2.5953352500000002,\"Search\":-3.4804604100000001,\"Video\":-1.0056351400000001,\"Wizard\":0,\"alice.iot_do\":0,\"alice.vins\":0}},\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"hi.\"},\"sub_name\":\"external_skill__type_text\",\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"alice.vins\",\"button_id\":\"21d2873f-51583301-b11997d1-8e64a360\",\"caption\":\"hi\",\"request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"scenario_name\":\"Dialogovo\"},\"type\":\"server_action\"}],\"title\":\"hi\",\"type\":\"action\"}]},\"templates\":{}},\"voice_response\":{\"should_listen\":false}}";
        } else if (event["name"] == "server_action2") {
            jsonPayload = "{\"header\":{\"dialog_id\":null,\"request_id\":\"" + requestId + "\",\"response_id\":\"7f2e57b0-61aafbeb-3167783a-c3838e11\",\"sequence_number\":1,\"session_id\":\"5bf93369-308b-42aa-88e4-5fc07bab7238\"},\"response\":{\"card\":{\"text\":\"hi\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"hi\",\"type\":\"simple_text\"}],\"directives\":[],\"directives_execution_policy\":\"BeforeSpeech\",\"experiments\":{},\"meta\":[{\"form\":{\"form\":\"Dialogovo\",\"slots\":[]},\"intent\":\"Dialogovo\",\"type\":\"analytics_info\"}],\"quality_storage\":{\"pre_predicts\":{\"Dialogovo\":0,\"FindPoi\":0,\"GeneralConversation\":0,\"HardcodedResponse\":0,\"HollywoodMusic\":-2.5953352500000002,\"Search\":-3.4804604100000001,\"Video\":-1.0056351400000001,\"Wizard\":0,\"alice.iot_do\":0,\"alice.vins\":0}},\"suggest\":{\"items\":[{\"directives\":[{\"name\":\"type\",\"payload\":{\"text\":\"hi.\"},\"sub_name\":\"external_skill__type_text\",\"type\":\"client_action\"},{\"ignore_answer\":true,\"name\":\"on_suggest\",\"payload\":{\"@request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"@scenario_name\":\"alice.vins\",\"button_id\":\"21d2873f-51583301-b11997d1-8e64a360\",\"caption\":\"hi\",\"request_id\":\"08418093-02b7-4bec-bcc3-2e28e2e6da93\",\"scenario_name\":\"Dialogovo\"},\"type\":\"server_action\"}],\"title\":\"hi\",\"type\":\"action\"}]},\"templates\":{}},\"voice_response\":{\"should_listen\":false}}";
        } else if (!event["payload"].isNull()) {
            if (!event["payload"]["form_update"].isNull() &&
                !event["payload"]["form_update"]["name"].isNull() &&
                event["payload"]["form_update"]["name"].asString() == "personal_assistant.scenarios.alarm_reminder") {
                jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"1c31317e-27c4-4318-b368-d1bcf05fd83e\",\"request_id\":\"" + requestId + "\",\"response_id\":\"3fceb8534af04d53a6c396c34419da87\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"Сегодня в 15:04 вы хотели поесть.\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"Сегодня в 15:04 вы хотели поесть.\",\"type\":\"simple_text\"}],\"directives\":[],\"meta\":[],\"suggest\":null},\"voice_response\":{\"directives\":[],\"output_speech\":{\"text\":\"Сегодня в 15:04 вы хотели поесть.\",\"type\":\"simple\"},\"should_listen\":false}}";
            } else if (event["payload"]["name"] == "quasar.next_video_track") {
                jsonPayload = "{\"header\":{\"dialog_id\":null,\"prev_req_id\":\"1c31317e-27c4-4318-b368-d1bcf05fd83e\",\"request_id\":\"" + requestId + "\",\"response_id\":\"3fceb8534af04d53a6c396c34419da87\",\"sequence_number\":null},\"response\":{\"card\":{\"text\":\"Сегодня в 15:04 вы хотели поесть.\",\"type\":\"simple_text\"},\"cards\":[{\"text\":\"Сегодня в 15:04 вы хотели поесть.\",\"type\":\"simple_text\"}],\"directives\":[],\"meta\":[],\"suggest\":null},\"voice_response\":{\"directives\":[],\"output_speech\":{\"text\":\"Сегодня в 15:04 вы хотели поесть.\",\"type\":\"simple\"},\"should_listen\":false}}";
            }
        }
    }
    if (jsonPayload.empty()) {
        YIO_LOG_WARN("\n!!!!!!!!!!!!!!!!!!!\n Request not recognised: << \n!!!!!!!!!!!!!!!!!!!");
        throw std::runtime_error("Request not recognised");
    }

    Json::Value header;
    header["refMessageId"] = "123";

    return SpeechKit::VinsResponse(jsonToString(header), jsonPayload, requestId);
}

const Json::Value& TestVins::getLastPayload() const {
    return lastPayload_;
}
