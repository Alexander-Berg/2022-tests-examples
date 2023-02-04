#include <balancer/serval/contrib/cno/core.h>

#include <util/generic/scope.h>

extern "C" int LLVMFuzzerTestOneInput(const ui8* data, size_t size) {
    cno_connection_t conn;
    cno_init(&conn, CNO_SERVER);
    Y_DEFER { cno_fini(&conn); };
    conn.disallow_h2_prior_knowledge = 1;
    mun_cant_fail(cno_begin(&conn, CNO_HTTP1) MUN_RETHROW);
    cno_consume(&conn, (const char*)data, size);
    return 0;
}
