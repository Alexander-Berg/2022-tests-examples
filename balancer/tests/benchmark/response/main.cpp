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
            HttpResponseSamples_ = MakeHolder<TCustomChunksEmitterIo>(
                inputStream->ReadAll(), TCustomChunksEmitterIo::EChunksEmittingPolicy::Tcp
            );
        }

        THolder<TCustomChunksEmitterIo>& HttpResponseSamples() {
            return HttpResponseSamples_;
        }

    private:
        THolder<TCustomChunksEmitterIo> HttpResponseSamples_;
    };

    const TString DataPath = GetWorkPath() + "/responses.in";

    TFixtureData FixtureData(DataPath);

    void RunResponseSamplesParsing(const NBench::NCpu::TParams& iface) {
        auto& responseSamples = FixtureData.HttpResponseSamples();
        for (size_t i = 0; i < iface.Iterations(); i++) {
            Y_VERIFY(!responseSamples->Empty());
            size_t parsedResponseCount = 0;
            while (!responseSamples->Empty()) {
                TResponse response;
                TChunkList unparsed;
                TError error = response.Read(responseSamples.Get(), unparsed, TInstant::Max());
                Y_VERIFY(!error);
                if (response.Props().ContentLength) {
                    // Skip body.
                    size_t length = *response.Props().ContentLength;
                    while (unparsed.size() < length) {
                        TChunkList lst;
                        auto error = responseSamples.Get()->Recv(lst, TInstant::Max());
                        Y_VERIFY(!error);
                        unparsed.Append(std::move(lst));
                    }
                    CutPrefix(length, unparsed);
                }
                ++parsedResponseCount;
                responseSamples->Prepend(std::move(unparsed));
            }
            // The number of responses is fixed unless responses.in is changed.
            Y_VERIFY(parsedResponseCount == 5496);
            responseSamples->Reset();
        }
    }

}

Y_CPU_BENCHMARK(ResponsePico, iface) {
    RunResponseSamplesParsing(iface);
}

