#include <balancer/serval/contrib/cno/hpack.h>

#include <util/generic/scope.h>
#include <util/generic/vector.h>
#include <util/string/hex.h>
#include <util/system/unaligned_mem.h>

extern "C" int LLVMFuzzerTestOneInput(const ui8* data, size_t size) {
    size_t reqc = size > 0 ? (data[0] & 0x5) + 1 : 0;
    if (size < 2 + reqc)
        return 0;

    cno_hpack_t decoder;
    cno_hpack_t encoder;
    cno_hpack_t decoder2;
    cno_hpack_init(&decoder, (size_t)data[1] << 5);
    cno_hpack_init(&encoder, (size_t)data[1] << 5);
    cno_hpack_init(&decoder2, (size_t)data[1] << 5);
    Y_DEFER { cno_hpack_clear(&decoder); };
    Y_DEFER { cno_hpack_clear(&encoder); };
    Y_DEFER { cno_hpack_clear(&decoder2); };

    for (size_t pos = 2 + reqc; reqc--; ) {
        cno_buffer_t input = {(const char*)data + pos, Min<size_t>(size - pos, data[2 + reqc])};
        cno_buffer_dyn_t output = {};
        cno_header_t headers[CNO_MAX_HEADERS];
        cno_header_t headers2[CNO_MAX_HEADERS];
        size_t n = CNO_MAX_HEADERS;
        size_t m = CNO_MAX_HEADERS;

        if (cno_hpack_decode(&decoder, input, headers, &n) MUN_RETHROW)
            continue;
        Y_DEFER {
            for (size_t i = 0; i < n; i++)
                cno_hpack_free_header(&headers[i]);
        };
        mun_assert(n <= CNO_MAX_HEADERS, "buffer overflow: decoded %zu headers", n);
        Y_DEFER {
            cno_buffer_dyn_clear(&output);
        };
        mun_cant_fail(cno_hpack_encode(&encoder, &output, headers, n) MUN_RETHROW);
        mun_cant_fail(cno_hpack_decode(&decoder2, CNO_BUFFER_VIEW(output), headers2, &m) MUN_RETHROW);
        Y_DEFER {
            for (size_t i = 0; i < m; i++)
                cno_hpack_free_header(&headers2[i]);
        };
        mun_assert(m == n, "decoded %zu != %zu headers", m, n);
        for (size_t i = 0; i < m; i++) {
            mun_assert(cno_buffer_eq(headers[i].name, headers2[i].name), "at %zu: name %s != %s", i,
                HexEncode(headers[i].name.data, headers[i].name.size).c_str(),
                HexEncode(headers2[i].name.data, headers2[i].name.size).c_str());
            mun_assert(cno_buffer_eq(headers[i].value, headers2[i].value), "at %zu: value %s != %s", i,
                HexEncode(headers[i].value.data, headers[i].value.size).c_str(),
                HexEncode(headers2[i].value.data, headers2[i].value.size).c_str());
        }
        pos += input.size;
    }
    return 0;
}
