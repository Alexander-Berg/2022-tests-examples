#include "test_request.h"

#include <balancer/kernel/custom_io/chunkio.h>
#include <balancer/kernel/http/parser/httpdecoder.h>
#include <balancer/kernel/http/parser/tests/util/custom_chunks_emitter_io.h>
#include <balancer/kernel/memory/chunks.h>
#include <balancer/kernel/http/parser/tests/fuzzutil/http.h>

#include <util/generic/strbuf.h>
#include <util/generic/yexception.h>

#include <utility>

namespace NSrvKernel {
namespace NFuzzUtil {

    int TestRequest(const uint8_t *data, size_t size, THttpParseOptions options) noexcept {
        TChunkList input = TChunkList(TBlob::NoCopy(reinterpret_cast<const char *>(data), size));
        TCustomChunksEmitterIo stringEmitter(ToString(std::move(input)));

        TFromClientDecoder decoder(&stringEmitter, options);
        TChunkList body;
        TRequest request;
        try {
            TryRethrowError(decoder.ReadRequest(request, TInstant::Max()));
        } catch (yexception &e) {
            return 0;
        }
        NFuzzUtil::CheckRequest(request);
        try {
            TryRethrowError(RecvAll(&decoder, body, TInstant::Max()));
        } catch (yexception &e) {
            return 0;
        }

        return 0;
    }

}
}
