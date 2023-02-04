#include <balancer/kernel/custom_io/chunkio.h>
#include <balancer/kernel/http/parser/http.h>
#include <balancer/kernel/http/parser/tests/fuzzutil/http.h>
#include <balancer/kernel/memory/chunks.h>

#include <util/generic/strbuf.h>

#include <utility>

namespace NSrvKernel {
namespace NFuzzUtil {

    int TestResponse(const uint8_t *data, size_t size, THttpParseOptions options) noexcept {
        TChunkList inData(TBlob::NoCopy(reinterpret_cast<const char *>(data), size));
        TChunksInput in(std::move(inData));
        TChunkList unparsed;
        TResponse response;
        try {
            TryRethrowError(response.Read(&in, unparsed, options, TInstant::Max()));
        } catch (yexception &e) {
            return 0;
        }

        NFuzzUtil::CheckResponse(response);
        return 0;
    }

}
}
