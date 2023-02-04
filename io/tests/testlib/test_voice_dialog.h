#pragma once

#include "concurrent_queue.h"
#include "test_tts_data_provider.h"

#include <yandex_io/services/aliced/speechkit_facade/quasar_voice_dialog.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>
#include <yandex_io/protos/model_objects.pb.h>

#include <json/json.h>

#include <speechkit/PhraseSpotterSettings.h>
#include <speechkit/UniProxyClient.h>
#include <speechkit/UniProxyClientSettings.h>
#include <speechkit/VoiceService.h>
#include <speechkit/VoiceServiceSettings.h>

#include <chrono>
#include <future>
#include <list>
#include <thread>

namespace quasar {

    namespace TestUtils {

        class TestVins {
        public:
            SpeechKit::VinsResponse getResponse(const std::string& payload, const std::string& speech, const std::string& requestId);
            const Json::Value& getLastPayload() const;
            const std::string sleepTimerTestDirectives = "[{\"name\":\"player_pause\", \"payload\": {\"flag_for_test\": \"value_for_test\"}},{\"name\":\"go_home\"}]";

        private:
            Json::Value lastPayload_;
        };

        class QuasarVoiceDialogTestImpl: public QuasarVoiceDialog,
                                         public std::enable_shared_from_this<QuasarVoiceDialogTestImpl> {
        public:
            std::weak_ptr<SpeechKit::VoiceService::VoiceServiceListener> weakListener_;

            enum State {
                IDLE = 0,
                SPOTTER = 1,
                LISTENING = 2,
                SPEAKING_WITH_SPOTTER = 3
            };

            void genRecognitionError(const SpeechKit::Error& error);
            void GenVinsError(const SpeechKit::Error& error);

            size_t GetPhraseSpotterStartedCnt() const;
            void waitForStartedSpotter(size_t startNumber) const;

            bool IsPrepared() const;

            void Say(const std::string& phrase, bool is_last = true);

            void SayCommand(const std::string& phrase);

            std::string getSpeech(int timeoutSec = 3);

            bool waitForState(State state, std::chrono::milliseconds timeout = std::chrono::seconds(100));
            ::SpeechKit::PhraseSpotterSettings waitForCommandSpotter(const std::string& spotter);

            void waitForYandexUid();

            void raiseModelError();
            void raiseCommandModelError();
            void raiseOnInterruptionPhraseSpotted();
            void raiseOnInvalidOAuthToken();

            ConcurrentQueue<std::string>& getTextRequests();

            std::function<void()> onVinsRequestStarted;

        public:
            explicit QuasarVoiceDialogTestImpl(std::shared_ptr<SpeechKit::VoiceService::VoiceServiceListener> listener,
                                               std::shared_ptr<TestVins> vins);

            ~QuasarVoiceDialogTestImpl();

            void stop();

            void startVoiceInput(const SpeechKit::UniProxy::Header& /*header*/, const std::string& payload, bool jingle) override;

            void startMusicInput(const SpeechKit::UniProxy::Header& /*header*/, const std::string& payload, bool jingle) override;

            void stopInput() override {
            }

            void startPhraseSpotter() override;

            void startCommandSpotter(SpeechKit::PhraseSpotterSettings settings) override;

            void stopCommandSpotter() override;

            void cancel(bool silent) override;
            void playCancelSound() override;

            void prepare(const std::string& uuid, const std::string& yandexUid,
                         const std::string& timezone, const std::string& group, const std::string& subgroup) override;

            void sendEvent(const SpeechKit::UniProxy::Header& /*header*/, const std::string& payload) override;

            void startTextInput(const SpeechKit::UniProxy::Header& /*header*/, const std::string& payload) override;

            void update(const AliceConfig& /*config*/) override {
            }
            void setSettings(::SpeechKit::VoiceServiceSettings settings) override;
            ::SpeechKit::VoiceServiceSettings getSettings() const;

            void setPlayer(::SpeechKit::AudioPlayer::SharedPtr player) override;

            void setLogLevel(const std::string& /*logLevel*/) {
            }
            void setSynchronizeStatePayload(const std::string& /*synchronizeStatePayload*/) override {
            }

            void startInterruptionSpotter() override {
            }
            void stopInterruptionSpotter() override {
            }
            void updatePrevReqId(const std::string& /*requestId*/) override {
            }

            std::string getLastPayload() const {
                return lastPayload_;
            }

            void onTtsStarted(const std::string& /*messageId*/) override {
            }
            void onTtsCompleted(const std::string& /*messageId*/) override {
            }
            void onTtsError(const SpeechKit::Error& /*error*/, const std::string& /*messageId*/) override {
            }

            void waitForVoiceInputStarted();

            void onUniProxyDirective(const std::string& header, const std::string& payload) const;

            void lockCallVinsResponse();
            void unlockCallVinsResponse();

        private:
            bool isSpotter(const std::string& phrase) const;

            void callVinsResponse(const SpeechKit::VinsResponse& response);

            void updateState(State state);
            void removeDeadProviders();
            static std::string parseRequestId(const std::string& payloadStr);

            State state_ = IDLE;
            ::SpeechKit::VoiceServiceSettings settings_{SpeechKit::Language::russian};

            bool gotPrepared_ = false;
            size_t startPhraseSpotterCnt_ = 0;
            mutable SteadyConditionVariable startPhraseSpotterCntCV_;

            std::string yandexUid_;

            mutable std::mutex mutex_;

            std::shared_ptr<TestVins> localVins_;
            std::condition_variable speechWakeUpVar_;
            std::condition_variable stateWakeUpVar_;

            ::SpeechKit::PhraseSpotterSettings activeCommandSpotterSettings_{""};
            std::string activeCommandSpotter_;
            std::string phraseBuffer_;
            std::string lastPayload_;
            std::string outputSpeech_;

            std::string map_[4] = {"IDLE", "SPOTTER", "LISTENING", "SPEAKING_WITH_SPOTTER"};

            std::atomic<bool> stopped_{false};
            NamedCallbackQueue callbackQueue_{"QuasarVoiceDialogTestImpl"};

            std::list<std::weak_ptr<TestTTSDataProvider>> ttsDataProviders_;

            ConcurrentQueue<std::string> textRequests_;
            ConcurrentEvent voiceInputStartedEvent_;
            std::shared_ptr<ConcurrentEvent> callVinsResponseEvent_;

            std::string requestId_;
            std::atomic<bool> activationSpotterStarted_{false};
        };
    } // namespace TestUtils

} // namespace quasar
