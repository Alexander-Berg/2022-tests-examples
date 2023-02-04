#include <balancer/modules/click/tests/fuzz/common.h>

#include <balancer/kernel/custom_io/chunkio.h>
#include <balancer/kernel/http/parser/httpdecoder.h>
#include <balancer/kernel/http/parser/http.h>
#include <balancer/kernel/http/parser/request_builder.h>
#include <balancer/kernel/http/parser/tests/fuzzutil/crash.h>
#include <balancer/kernel/http/parser/tests/fuzzutil/http.h>
#include <balancer/kernel/io/iobase.h>
#include <balancer/kernel/memory/chunks.h>

#include <yweb/weblibs/signurl/clickhandle.h>

#include <util/generic/strbuf.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/generic/yexception.h>
#include <util/stream/null.h>
#include <util/stream/str.h>


using namespace NSrvKernel;
using namespace NSrvKernel::NFuzzUtil;

ClickHandle::TOptions Options() {
    ClickHandle::TOptions retval;
    retval.EnableProxy = false;
    retval.ReportToCerr = false;
    return retval;
}

class TNullLogger : public ClickLogger {
public:
    TNullLogger()
        : ClickLogger("", nullptr, nullptr)
    {}
};


using TClickHeaders = TVector<std::pair<TString, TString>>;

template<typename THeadersSetup>
static void TestClick(TString sourceData, THeadersSetup&& headersSetup) noexcept {
    const auto options = Options();
    TNullLogger logger;

    TClickHeaders headers;
    headersSetup(sourceData, headers);
    TString clickOutput;

    // keys are not const for some reason, so re-creating them for each test.
    NSignUrl::TSignKeys signKeys = NSignUrl::TSignKeys::CreateFromLines(SIGN_KEYS_STRING);

    try {
        TStringOutput output(clickOutput);
        ClickHandle handle(output, &signKeys, sourceData,  headers, logger, options);
        if (!handle.Process(sourceData.c_str(), nullptr)) {
            return;
        }
    } catch (yexception &) {
        return;
    }

    TChunkList inputChunks(clickOutput);
    TChunksInput chunksIn(std::move(inputChunks));
    TRequest request = BuildRequest().Method(EMethod::GET)
                                     .Path("/")
                                     .Version11();
    TFromBackendDecoder decoder{&chunksIn, request};
    TResponse response;
    try {
        TryRethrowError(decoder.ReadResponse(response, TInstant::Max()));
    } catch (yexception& e) {
        Crash(); // failed to parse http response
    }

    CheckResponse(response);
    TChunkList body;
    try {
        TryRethrowError(RecvAll(&decoder, body, TInstant::Max()));
    } catch (yexception& e) {
        Crash(); // failed to parse *complete* http response (either not enough content-len or chunks)
    }
}

static int TestHandle(const uint8_t *data, size_t size) noexcept {
    TString sourceData(TStringBuf((const char*) data, size));
    sourceData.push_back('\0');

    TestClick(sourceData, [](const TString& url, TClickHeaders& headers) {
        Y_UNUSED(url);
        Y_UNUSED(headers);
    });
    TestClick(sourceData, [](const TString& url, TClickHeaders& headers) {
        Y_UNUSED(url);
        headers.push_back(std::make_pair("Host", "yandex.ru"));
    });
    TestClick(sourceData, [](const TString& url, TClickHeaders& headers) {
        headers.push_back(std::make_pair("Host", "yandex.ru"));
        headers.push_back(std::make_pair("Header", url));
    });
    TestClick(sourceData, [](const TString& url, TClickHeaders& headers) {
        headers.push_back(std::make_pair("Host", "yandex.ru"));
        headers.push_back(std::make_pair(url, "Value"));
    });
    TestClick(sourceData, [](const TString& url, TClickHeaders& headers) {
        headers.push_back(std::make_pair("Host", "yandex.ru"));
        headers.push_back(std::make_pair(url, url));
    });

    return 0;
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
    return TestHandle(data, size);
}
