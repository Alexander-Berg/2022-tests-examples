#include <balancer/server/server.h>
#include <balancer/server/ut/util/env.h>

#include <contrib/libs/benchmark/include/benchmark/benchmark.h>

#include <library/cpp/neh/rpc.h>

#include <util/system/thread.h>

using namespace NBalancerServer;
using namespace NBalancerServer::NTesting;
using namespace NSrvKernel;

namespace {

struct TNehEnv {
    TPortManager PortManager;
    ui32 Port = PortManager.GetPort();

    NNeh::IServicesRef Server = NNeh::CreateLoop();

    void Start(size_t threads) {
        Server->ForkLoop(threads);
    }

    void Stop() {
        Server->SyncStopFork();
    }

    ~TNehEnv() {
        Stop();
    }
};

template <typename Callback>
void StartServer(TEnv& env, size_t networkThreads, size_t serverThreads, size_t requestCount, Callback&& callback) {
    TOptions options;
    options.SetNetworkThreads(networkThreads);
    options.SetThreads(serverThreads);
    options.SetQueueSize(requestCount);

    env.Start(callback, options);
}

void StartServer(TNehEnv& env, size_t threads, NNeh::TServiceFunction callback) {
    env.Server->Add("http2://*:" + ToString(env.Port) + "/*", std::move(callback));
    env.Start(threads);
}

NNeh::TServiceFunction GetNehEchoCallback() {
    return NNeh::TServiceFunction([](const NNeh::IRequestRef& request) {
        NNeh::TDataSaver ds;
        ds << request->Data();
        request->SendReply(ds);
    });
}

}  // namespace

template <typename TEnv>
void Run(benchmark::State& state, TEnv& env, size_t networkThreads, size_t requestCount) {
    static constexpr size_t ExecutorStackSize = 30 * 1024;
    static constexpr size_t DataSize = 10 * 1024;

    const auto size = RandomNumber<size_t>(DataSize) + 1;
    TString data = NUnitTest::RandomString(size, size);

    auto request = [&](TCont* cont) {
        try {
            TString result = SendHttpRequest(cont, env.Port,
                "POST /yandsearch?xxx=1 HTTP/1.1",
                data,
                TString(),
                TString()
            ).Data;

            UNIT_ASSERT_VALUES_EQUAL(result, data);
        } catch (...) {
            UNIT_ASSERT_C(false, CurrentExceptionMessage());
        }
    };

    for (auto _ : state) {
        state.PauseTiming();

        TThreadPool shoot;
        shoot.Start(networkThreads, 0);

        TMutex lock;
        TCondVar cv;
        size_t readyCount = 0;

        for (size_t i = 0; i < networkThreads; ++i) {
            UNIT_ASSERT(
                shoot.AddFunc([&]() {
                    auto e = MakeHolder<TContExecutor>(ExecutorStackSize);
                    for (size_t i = 0; i < requestCount / networkThreads; i++) {
                        e->Create(request, "request");
                    }

                    with_lock (lock) {
                        ++readyCount;
                        if (readyCount == networkThreads) {
                            cv.BroadCast();
                            state.ResumeTiming();
                        } else {
                            cv.Wait(lock, [&] { return readyCount == networkThreads; });
                        }
                    }

                    e->Execute();
                })
            );
        }

        shoot.Stop();
    }

    env.Stop();
}

static void BMOneNetworkNoServerThreads(benchmark::State& state) {
    const size_t networkThreads = 1;
    const size_t serverThreads = 0;
    const size_t requestCount = 1000;

    TEnv env;
    StartServer(env, networkThreads, serverThreads, requestCount, GetEchoCallback());

    Run(state, env, networkThreads, requestCount);
}

static void BMOneNetworkOneServerThreads(benchmark::State& state) {
    const size_t networkThreads = 1;
    const size_t serverThreads = 1;
    const size_t requestCount = 1000;

    TEnv env;
    StartServer(env, networkThreads, serverThreads, requestCount, GetEchoCallback());

    Run(state, env, networkThreads, requestCount);
}

static void BMOneNetworkFourServerThreads(benchmark::State& state) {
    const size_t networkThreads = 1;
    const size_t serverThreads = 4;
    const size_t requestCount = 1000;

    TEnv env;
    StartServer(env, networkThreads, serverThreads, requestCount, GetEchoCallback());

    Run(state, env, networkThreads, requestCount);
}

static void BMFourNetworkNoServerThreads(benchmark::State& state) {
    const size_t networkThreads = 4;
    const size_t serverThreads = 0;
    const size_t requestCount = 1000;

    TEnv env;
    StartServer(env, networkThreads, serverThreads, requestCount, GetEchoCallback());

    Run(state, env, networkThreads, requestCount);
}

static void BMFourNetworkOneServerThreads(benchmark::State& state) {
    const size_t networkThreads = 4;
    const size_t serverThreads = 1;
    const size_t requestCount = 1000;

    TEnv env;
    StartServer(env, networkThreads, serverThreads, requestCount, GetEchoCallback());

    Run(state, env, networkThreads, requestCount);
}

static void BMFourNetworkTwoServerThreads(benchmark::State& state) {
    const size_t networkThreads = 4;
    const size_t serverThreads = 2;
    const size_t requestCount = 1000;

    TEnv env;
    StartServer(env, networkThreads, serverThreads, requestCount, GetEchoCallback());

    Run(state, env, networkThreads, requestCount);
}

static void BMFourNetworkThreeServerThreads(benchmark::State& state) {
    const size_t networkThreads = 4;
    const size_t serverThreads = 3;
    const size_t requestCount = 1000;

    TEnv env;
    StartServer(env, networkThreads, serverThreads, requestCount, GetEchoCallback());

    Run(state, env, networkThreads, requestCount);
}

static void BMFourNetworkFourServerThreads(benchmark::State& state) {
    const size_t networkThreads = 4;
    const size_t serverThreads = 4;
    const size_t requestCount = 1000;

    TEnv env;
    StartServer(env, networkThreads, serverThreads, requestCount, GetEchoCallback());

    Run(state, env, networkThreads, requestCount);
}

static void BMFourNetworkEightServerThreads(benchmark::State& state) {
    const size_t networkThreads = 4;
    const size_t serverThreads = 8;
    const size_t requestCount = 1000;

    TEnv env;
    StartServer(env, networkThreads, serverThreads, requestCount, GetEchoCallback());

    Run(state, env, networkThreads, requestCount);
}

static void BMNehFourThreads(benchmark::State& state) {
    const size_t threads = 4;
    const size_t requestCount = 1000;

    TNehEnv env;
    StartServer(env, threads, GetNehEchoCallback());

    Run(state, env, threads, requestCount);
}

BENCHMARK(BMOneNetworkNoServerThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
BENCHMARK(BMOneNetworkOneServerThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
BENCHMARK(BMOneNetworkFourServerThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
BENCHMARK(BMFourNetworkNoServerThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
BENCHMARK(BMFourNetworkOneServerThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
BENCHMARK(BMFourNetworkTwoServerThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
BENCHMARK(BMFourNetworkThreeServerThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
BENCHMARK(BMFourNetworkFourServerThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
BENCHMARK(BMFourNetworkEightServerThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
BENCHMARK(BMNehFourThreads)->Unit(benchmark::kMillisecond)->UseRealTime();
