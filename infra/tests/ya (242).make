PY3TEST()

OWNER(g:yp-dns)

INCLUDE(${ARCADIA_ROOT}/yp/python/ya_programs.make.inc)

TEST_SRCS(
    conftest.py
    helpers.py
    test_additional_in_query.py
    test_dns64.py
    test_dynamic_zones.py
    test_list_zones.py
    test_reload.py
    test_resolve_txt.py
    test_soa.py
    test_start.py
    test_statistics.py
    test_wildcards.py
)

PEERDIR(
    infra/unbound/python/helpers
    infra/unbound/python/unbound
    infra/unbound/python/unbound-control
    yp/tests/helpers
    contrib/python/dnspython
    contrib/python/timeout-decorator
)

DEPENDS(
    contrib/tools/unbound/unbound
    contrib/tools/unbound/unbound-control
)

SIZE(MEDIUM)

FORK_TEST_FILES()

FORK_TESTS()

SPLIT_FACTOR(32)

REQUIREMENTS(
    network:restricted
    ram:16
)

END()
