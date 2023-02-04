#include <solomon/libs/cpp/dns/dns.h>

#include <library/cpp/threading/future/future.h>
#include <library/cpp/monlib/encode/json/json.h>
#include <library/cpp/monlib/metrics/metric_registry.h>

#include <util/generic/vector.h>
#include <util/system/event.h>
#include <util/datetime/cputimer.h>
#include <util/random/random.h>

#include <atomic>
#include <thread>

// Not a real unit-test, since testing DNS would be pretty flacky, but this way
// we can run the client under sanitizers and see if it actually works

using namespace NSolomon;

int main() {
    auto client = CreateDnsClient();
    std::atomic<int> responseCount{0};
    std::atomic<bool> start{false};

    TVector<std::thread> threads;
    NMonitoring::TMetricRegistry registry;
    auto* timings = registry.HistogramCounter(
        {{"sensor", "resolveTime"}},
        NMonitoring::ExplicitHistogram({5, 15, 50, 75, 100, 150, 500, 1000}));

    auto* failed = registry.Counter({{"sensor", "failed"}});

    constexpr auto THR_NUM = 6;
    constexpr auto TASK_PER_THR = 10;

    auto work = [&] {
        while (!start) {}
        Sleep(TDuration::MicroSeconds(RandomNumber(10000u)));

        TVector<NThreading::TFuture<void>> waitable;

        for (auto i = 0; i < TASK_PER_THR; ++i) {
            Sleep(TDuration::MicroSeconds(rand() % 23));
            TProfileTimer t;
            auto f = client->GetSrvRecords("zen-web-man.production.yandex-ichwill-zen-web.zen.stable.qloud-d.yandex.net")
                .Apply([&, t] (auto&& f) {
                    if (f.HasException()) {
                        failed->Inc();
                    }

                    responseCount++;
                    timings->Record(t.Get().MilliSeconds());
                });

            waitable.push_back(f);
        }

        WaitExceptionOrAll(waitable).GetValueSync();
    };

    for (auto i = 0; i < THR_NUM; ++i) {
        threads.push_back(std::thread(work));
    }

    start = true;

    TInstant s{TInstant::Now()};
    TDuration t{TDuration::Seconds(10)};

    Cerr << "waiting for " << (THR_NUM * TASK_PER_THR) << " responses" << Endl;
    while (responseCount.load() < THR_NUM * TASK_PER_THR) {
        Sleep(TDuration::Seconds(1));
        Cerr << "have " << responseCount.load() << " so far" << Endl;

        if (TInstant::Now() - s > t) {
            Cerr << "timed out" << Endl;
            auto encoder = NMonitoring::EncoderJson(&Cout, 2);
            registry.Accept(TInstant::Zero(), encoder.Get());
            return 1;
        }
    }

    for (auto&& t: threads) {
        t.join();
    }

    auto encoder = NMonitoring::EncoderJson(&Cout, 2);
    registry.Accept(TInstant::Zero(), encoder.Get());
}
