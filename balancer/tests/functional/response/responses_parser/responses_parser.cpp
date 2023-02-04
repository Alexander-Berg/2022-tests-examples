#include <balancer/kernel/http/parser/http.h>
#include <balancer/kernel/http/parser/tests/util/custom_chunks_emitter_io.h>

using namespace NSrvKernel;

int main() {
    THolder<TCustomChunksEmitterIo> responsesStream = MakeHolder<TCustomChunksEmitterIo>(
        Cin.ReadAll(), TCustomChunksEmitterIo::EChunksEmittingPolicy::Tcp);
    while (!responsesStream->Empty()) {
        TResponse parsedResponse;
        TChunkList unparsed;
        TError error = parsedResponse.Read(responsesStream.Get(), unparsed, TInstant::Max());
        Y_VERIFY(!error);

        TChunkList body;
        if (parsedResponse.Props().ContentLength) {
            size_t length = *parsedResponse.Props().ContentLength;
            while (unparsed.size() < length) {
                TChunkList lst;
                auto error = responsesStream.Get()->Recv(lst, TInstant::Max());
                Y_VERIFY(!error);
                unparsed.Append(std::move(lst));
            }
            body = CutPrefix(length, unparsed);
        }

        responsesStream->Prepend(std::move(unparsed));

        parsedResponse.BuildTo(Cout);
        parsedResponse.Props().BuildTo(Cout);
        Cout << body;
    }
    return 0;
}
