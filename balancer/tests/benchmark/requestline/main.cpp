#include <library/cpp/testing/benchmark/bench.h>

#include <util/generic/string.h>
#include <util/generic/xrange.h>
#include <balancer/kernel/http/parser/http.h>

using namespace NSrvKernel;

Y_CPU_BENCHMARK(RequestLine, iface) {
    TRequest request;
    TString req = "GET /search?request=test&q=v HTTP/1.1\r\n\r\n";
    for (const auto i : xrange(iface.Iterations())) {
        Y_UNUSED(i);
        TError error = request.Parse(req);
        Y_VERIFY(!error);
    }
}

Y_CPU_BENCHMARK(BadRequestLine, iface) {
    TRequest request;
    TString req = "GET /search\t?request=test&q=v HTTP/1.1\r\n\r\n";

    for (const auto i : xrange(iface.Iterations())) {
        Y_UNUSED(i);
        TError error = request.Parse(req);
        Y_VERIFY(error);
    }
}

