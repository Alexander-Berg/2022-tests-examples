#include <library/cpp/testing/unittest/registar.h>
#include <yandex_io/tests/testlib/unittest_helper/logging_test_fixture.h>

#include <yandex_io/metrica/teleme3d/batcher.h>

#include <future>

using namespace quasar;

namespace {
    using EventEnv = std::tuple<YandexIO::EventsDB::Event, YandexIO::EventsDB::Environment>;

    // batch which always can hold next requested event
    struct ClckBatch {
        std::vector<EventEnv> content;
        static bool hasUnsent() {
            return false;
        }

        bool append(YandexIO::EventsDB::Event event, YandexIO::EventsDB::Environment env) {
            if (!event.has_name() || event.name() != "skip") {
                content.emplace_back(std::move(event), std::move(env));
            }
            return size() < 10;
        }

        void reset() {
            content.clear();
        }

        std::uint32_t size() const {
            return content.size();
        }
    };

    struct AppBatch: public ClckBatch {
        std::optional<EventEnv> unsent;

        bool append(YandexIO::EventsDB::Event event, YandexIO::EventsDB::Environment env) {
            if (content.size() == 10) {
                unsent = EventEnv{std::move(event), std::move(env)};
                return false;
            }
            ClckBatch::append(std::move(event), std::move(env));
            return true;
        }

        bool hasUnsent() {
            return unsent.has_value();
        }

        void reset() {
            ClckBatch::reset();
            if (unsent.has_value()) {
                content.push_back(std::move(unsent.value()));
                unsent = std::nullopt;
            }
        }
    };

    template <BatchContainer Batch>
    class TestBatcher: public Batcher<Batch> {
    public:
        std::chrono::seconds maxCollectingTime_{10000}; // never
        std::function<bool()> onSend;

        TestBatcher()
            : Batcher<Batch>("testBatcher")
            , onSend([]() { return true; })
        {
        }

    private:
        bool sendBatch() override {
            return onSend();
        }

        std::chrono::seconds getMaxBatchCollectingTime() override {
            return maxCollectingTime_;
        }
    };

    class TestSrcControl: public YandexIO::EventsDB::ISourceControl {
    public:
        int generateEvents{0};
        std::function<YandexIO::EventsDB::Event()> eventGenerator;
        std::shared_ptr<YandexIO::EventsDB::Sink> sink;
        std::promise<void> releasedBeforeLast;
        std::promise<void> releasedIncludingLast;

        TestSrcControl(int numEvents)
            : generateEvents(numEvents)
            , eventGenerator([]() { return YandexIO::EventsDB::Event(); })
        {
        }

        void readyForNext() override {
            if (generateEvents > 0) {
                --generateEvents;
                sink->handleDbEvent(*this, eventGenerator(), YandexIO::EventsDB::Environment());
            }
        }

        void releaseBeforeLast() override {
            releasedBeforeLast.set_value();
        }

        void releaseIncludingLast() override {
            releasedIncludingLast.set_value();
        }
    };
} // namespace

Y_UNIT_TEST_SUITE_F(TestBatcher, QuasarLoggingTestFixture) {
    // check that batcher with ClckBatch is releasing including last
    Y_UNIT_TEST(ordinaryBatching) {
        TestSrcControl ctrl(10);
        TestBatcher<ClckBatch> batcher;
        ctrl.sink = batcher.getSink();
        ctrl.readyForNext();
        ctrl.releasedIncludingLast.get_future().get();
    }

    // check that batcher with AppBatch is releasing excluding last
    Y_UNIT_TEST(overflowBatching) {
        TestSrcControl ctrl(11);
        TestBatcher<AppBatch> batcher;
        ctrl.sink = batcher.getSink();
        ctrl.readyForNext();
        ctrl.releasedBeforeLast.get_future().get();
    }

    Y_UNIT_TEST(flushByTimeout) {
        TestSrcControl ctrl(1);
        TestBatcher<ClckBatch> batcher;
        std::promise<void> sendPromise;
        batcher.onSend = [&sendPromise]() {
            sendPromise.set_value();
            return true;
        };
        batcher.maxCollectingTime_ = std::chrono::seconds(2);
        ctrl.sink = batcher.getSink();
        ctrl.readyForNext();
        sendPromise.get_future().get();
    }

    Y_UNIT_TEST(doNotFlushByTimeoutEmptyBatchAfterFirstEvent) {
        TestSrcControl ctrl(11);
        int eventNum = 0;
        ctrl.eventGenerator = [&eventNum]() {
            YandexIO::EventsDB::Event result;
            if (++eventNum == 11) {
                result.set_name("skip");
            }
            return result;
        };
        TestBatcher<ClckBatch> batcher;
        ctrl.sink = batcher.getSink();
        batcher.maxCollectingTime_ = std::chrono::seconds(2);
        bool firstSend = true;
        bool wasSecondSend = false;
        batcher.onSend = [&wasSecondSend, &firstSend]() {
            if (firstSend) {
                firstSend = false;
                return true;
            }
            wasSecondSend = true;
            return true;
        };
        ctrl.readyForNext();
        std::this_thread::sleep_for(std::chrono::seconds(4));
        UNIT_ASSERT(!wasSecondSend);
    }

    Y_UNIT_TEST(flushByTimeoutButBatchWasNonEmptyAfterReset) {
        TestSrcControl ctrl(11);
        TestBatcher<AppBatch> batcher;
        std::promise<void> sendPromise;
        bool first = true;
        batcher.maxCollectingTime_ = std::chrono::seconds(2);
        batcher.onSend = [&sendPromise, &first]() {
            if (first) {
                first = false;
            } else {
                sendPromise.set_value();
            }
            return true;
        };
        ctrl.sink = batcher.getSink();
        ctrl.readyForNext();
        sendPromise.get_future().get();
    }

    Y_UNIT_TEST(sendingRetrying) {
        TestSrcControl ctrl(10);
        TestBatcher<ClckBatch> batcher;
        std::promise<void> sendPromise;
        int attempt = 0;
        batcher.onSend = [&sendPromise, &attempt]() {
            if (++attempt == 10) {
                sendPromise.set_value();
                return true;
            }
            return false;
        };
        ctrl.sink = batcher.getSink();
        ctrl.readyForNext();
        sendPromise.get_future().get();
    }
}
