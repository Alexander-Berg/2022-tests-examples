#pragma once

#include <speechkit/TTSDataProvider.h>

#include <memory>
#include <mutex>

namespace quasar {

    namespace TestUtils {

        class TestTTSDataProvider
            : public SpeechKit::TTSDataProvider,
              public std::enable_shared_from_this<TestTTSDataProvider> {
        public:
            void init(Listener::WeakPtr listener) override;
            void start() override;
            void pause() override;

            void setStreamEnd();

            void waitForListener();

        private:
            std::mutex mutex_;
            std::condition_variable condVar_;
            Listener::WeakPtr listener_;
        };

    } // namespace TestUtils

} // namespace quasar
