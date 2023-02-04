#include <balancer/kernel/custom_io/chunkio.h>
#include <balancer/kernel/http/parser/httpdecoder.h>
#include <balancer/kernel/http/parser/http.h>
#include <balancer/kernel/http/parser/request_builder.h>
#include <balancer/kernel/http/parser/tests/fuzzutil/http.h>
#include <balancer/kernel/memory/chunks.h>

#include <util/random/mersenne.h>
#include <util/string/builder.h>

#include <utility>

using namespace NSrvKernel;
using namespace NSrvKernel::NFuzzUtil;

static int DoParse(TChunkList inData) {
    TChunksInput in(std::move(inData));
    TRequest request = BuildRequest().Method(EMethod::GET)
                                     .Path("/")
                                     .Version11();
    TFromBackendDecoder decoder{&in, request};
    TResponse response;
    try {
        TryRethrowError(decoder.ReadResponse(response, TInstant::Max()));
    } catch (yexception& e) {
        return 0;
    }
    CheckResponse(response);
    TChunkList body;
    TryRethrowError(RecvAll(&decoder, body, TInstant::Max()));
    return 0;
}

static TChunkList GenerateData(TStringBuf data, TMersenne<unsigned>& rng) {
    TChunkList retval;

    const unsigned resultingLength = data.size();
    unsigned left = resultingLength;

    while (left) {
        const size_t chunkLen = Max(1U, Min(left, rng.Uniform(resultingLength) % (resultingLength + 1)));

        TChunkPtr chunk = NewChunkReserve(chunkLen);
        memcpy(chunk->Data(), data.data() + data.size() - left, chunkLen);
        chunk->Shrink(chunkLen);

        retval.Push(std::move(chunk));
        left -= chunkLen;
    }

    return retval;
}

static int TestContentLengthResponseParser(const uint8_t* data, size_t size) noexcept {
    TMersenne<unsigned> rng;
    TChunkList body(GenerateData(TStringBuf((const char*)data, size), rng));

    TString header = TStringBuilder()
        << "HTTP/1.1 200 OK\r\nContent-Length: "
        << ToString(body.size())
        << "\r\n\r\n";

    TChunkList inData(header);
    inData.Append(std::move(body));
    return DoParse(std::move(inData));
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
    return TestContentLengthResponseParser(data, size);
}
