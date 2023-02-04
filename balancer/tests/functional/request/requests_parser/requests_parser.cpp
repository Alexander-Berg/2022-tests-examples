#include <balancer/kernel/http/parser/http.h>
#include <balancer/kernel/http/parser/tests/util/custom_chunks_emitter_io.h>

using namespace NSrvKernel;

int main() {
    THolder<TCustomChunksEmitterIo> requestsStream = MakeHolder<TCustomChunksEmitterIo>(
        Cin.ReadAll(), TCustomChunksEmitterIo::EChunksEmittingPolicy::Tcp);
    while (!requestsStream->Empty()) {
        TRequest parsedRequest;
        TChunkList unparsed;
        TError error = parsedRequest.Read(requestsStream.Get(), unparsed, TInstant::Max());
        Y_VERIFY(!error);

        requestsStream->Prepend(std::move(unparsed));

        parsedRequest.BuildTo(Cout);
        parsedRequest.Props().BuildTo(Cout);
    }
    return 0;
}
