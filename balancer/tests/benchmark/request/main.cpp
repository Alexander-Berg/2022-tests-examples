#include <balancer/kernel/fs/file_manager.h>

#include <balancer/kernel/http/parser/http.h>
#include <balancer/kernel/http/parser/tests/util/custom_chunks_emitter_io.h>

#include <library/cpp/testing/benchmark/bench.h>

#include <library/cpp/testing/unittest/tests_data.h>

#include <util/generic/string.h>
#include <util/generic/xrange.h>

using namespace NSrvKernel;

namespace {

    class TFixtureData {
    public:
        TFixtureData(TStringBuf dataPath) {
            auto inputStream = TFileManager::Instance().GetInput(TString{dataPath});
            HttpRequestSamples_ = MakeHolder<TCustomChunksEmitterIo>(
                inputStream->ReadAll(), TCustomChunksEmitterIo::EChunksEmittingPolicy::Tcp
            );
        }

        THolder<TCustomChunksEmitterIo>& HttpRequestSamples() {
            return HttpRequestSamples_;
        }

    private:
        THolder<TCustomChunksEmitterIo> HttpRequestSamples_;
    };

    const TString DataPath = GetWorkPath() + "/requests.in";

    TFixtureData FixtureData(DataPath);

    void RunRequestSamplesParsing(const NBench::NCpu::TParams& iface) {
        auto& requestSamples = FixtureData.HttpRequestSamples();
        for (size_t i = 0; i < iface.Iterations(); i++) {
            Y_VERIFY(!requestSamples->Empty());
            size_t parsedRequestCount = 0;
            while (!requestSamples->Empty()) {
                TRequest request;
                TChunkList unparsed;
                TError error = request.Read(requestSamples.Get(), unparsed, TInstant::Max());
                Y_VERIFY(!error);
                ++parsedRequestCount;
                requestSamples->Prepend(std::move(unparsed));
            }
            // The number of requests is fixed unless requests.in is changed.
            Y_VERIFY(parsedRequestCount == 10406);
            requestSamples->Reset();
        }
    }

}

Y_CPU_BENCHMARK(RequestPico, iface) {
    RunRequestSamplesParsing(iface);
}

