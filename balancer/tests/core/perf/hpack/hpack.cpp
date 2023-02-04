#include <library/cpp/testing/benchmark/bench.h>

#include <balancer/serval/contrib/cno/hpack.h>

#include <util/generic/scope.h>

static const struct cno_header_t Headers[] = {
    {CNO_BUFFER_STRING(":authority"), CNO_BUFFER_STRING("yandex.ru"), 0},
    {CNO_BUFFER_STRING(":method"), CNO_BUFFER_STRING("GET"), 0},
    {CNO_BUFFER_STRING(":path"), CNO_BUFFER_STRING("/yandsearch?text=%D0%B4%D0%B5%D0%BD%D1%8C+%D0%BD%D0%B5%D0%B7%D0%B0%D0%B2%D0%B8%D1%81%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D0%B8+%D1%84%D0%B8%D0%BB%D1%8C%D0%BC&lr=213"), 0},
    {CNO_BUFFER_STRING(":scheme"), CNO_BUFFER_STRING("unknown"), 0},
    {CNO_BUFFER_STRING("accept"), CNO_BUFFER_STRING("*/*"), 0},
    {CNO_BUFFER_STRING("accept-encoding"), CNO_BUFFER_STRING("gzip,deflate,sdch"), 0},
    {CNO_BUFFER_STRING("user-agent"), CNO_BUFFER_STRING("Wget/1.13.4 (linux-gnu)"), 0},
    {CNO_BUFFER_STRING("x-forwarded-for"), CNO_BUFFER_STRING("::1"), 0},
    {CNO_BUFFER_STRING("x-forwarded-for-y"), CNO_BUFFER_STRING("::1"), 0},
    {CNO_BUFFER_STRING("x-ip-properties"), CNO_BUFFER_STRING("EOEBGAAgACgB"), 0},
    {CNO_BUFFER_STRING("x-l7-exp"), CNO_BUFFER_STRING("true"), 0},
    {CNO_BUFFER_STRING("x-laas-answered"), CNO_BUFFER_STRING("1"), 0},
    {CNO_BUFFER_STRING("x-region-by-ip"), CNO_BUFFER_STRING("2:3"), 0},
    {CNO_BUFFER_STRING("x-region-city-id"), CNO_BUFFER_STRING("2:3"), 0},
    {CNO_BUFFER_STRING("x-region-id"), CNO_BUFFER_STRING("2:3"), 0},
    {CNO_BUFFER_STRING("x-region-is-user-choice"), CNO_BUFFER_STRING("0"), 0},
    {CNO_BUFFER_STRING("x-region-location"), CNO_BUFFER_STRING("55.753960, 37.620393, :5000, :528302650"), 0},
    {CNO_BUFFER_STRING("x-region-precision"), CNO_BUFFER_STRING("2"), 0},
    {CNO_BUFFER_STRING("x-region-should-update-cookie"), CNO_BUFFER_STRING("0"), 0},
    {CNO_BUFFER_STRING("x-region-suspected"), CNO_BUFFER_STRING("2:3"), 0},
    {CNO_BUFFER_STRING("x-region-suspected-city"), CNO_BUFFER_STRING("2:3"), 0},
    {CNO_BUFFER_STRING("x-region-suspected-location"), CNO_BUFFER_STRING("55.753960, 37.620393, :5000, :528302650"), 0},
    {CNO_BUFFER_STRING("x-region-suspected-precision"), CNO_BUFFER_STRING("2"), 0},
    {CNO_BUFFER_STRING("x-source-port"), CNO_BUFFER_STRING("61846"), 0},
    {CNO_BUFFER_STRING("x-start-time"), CNO_BUFFER_STRING("1544014654653528"), 0},
    {CNO_BUFFER_STRING("x-yandex-expconfigversion"), CNO_BUFFER_STRING("10566"), 0},
    {CNO_BUFFER_STRING("x-yandex-expsplitparams"), CNO_BUFFER_STRING("eyJyIjowLCJzIjoiIiwiZCI6IiIsIm0iOiIiLCJiIjoiIiwiaSI6ZmFsc2V9"), 0},
    {CNO_BUFFER_STRING("x-yandex-internal-request"), CNO_BUFFER_STRING("0"), 0},
    {CNO_BUFFER_STRING("x-yandex-logstatuid"), CNO_BUFFER_STRING("238205446:528302:36"), 0},
    {CNO_BUFFER_STRING("x-yandex-suspected-robot"), CNO_BUFFER_STRING("0"), 0},
};

Y_CPU_BENCHMARK(Encode, iface) {
    cno_buffer_dyn_t output = {};
    for (size_t i = 0; i < iface.Iterations(); ++i) {
        cno_hpack_t encoder;
        cno_hpack_init(&encoder, 4096);
        Y_DEFER { cno_hpack_clear(&encoder); };
        Y_DEFER { cno_buffer_dyn_clear(&output); };
        mun_cant_fail(cno_hpack_encode(&encoder, &output, Headers, sizeof(Headers) / sizeof(Headers[0])) MUN_RETHROW);
    }
}

Y_CPU_BENCHMARK(Decode, iface) {
    cno_buffer_dyn_t output = {};
    cno_hpack_t encoder;
    cno_hpack_init(&encoder, 4096);
    Y_DEFER { cno_hpack_clear(&encoder); };
    Y_DEFER { cno_buffer_dyn_clear(&output); };
    mun_cant_fail(cno_hpack_encode(&encoder, &output, Headers, sizeof(Headers) / sizeof(Headers[0])) MUN_RETHROW);
    cno_header_t headers[CNO_MAX_HEADERS];
    for (size_t i = 0; i < iface.Iterations(); ++i) {
        cno_hpack_t decoder;
        cno_hpack_init(&decoder, 4096);
        Y_DEFER { cno_hpack_clear(&decoder); };
        size_t n = CNO_MAX_HEADERS;
        mun_cant_fail(cno_hpack_decode(&decoder, CNO_BUFFER_VIEW(output), headers, &n) MUN_RETHROW);
        for (size_t i = 0; i < n; i++)
            cno_hpack_free_header(&headers[i]);
    }
}
