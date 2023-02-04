PY3TEST()

OWNER(g:yp-dns)

INCLUDE(${ARCADIA_ROOT}/yp/python/ya_programs.make.inc)

TEST_SRCS(
    conftest.py
    helpers.py
    test_banning_clusters.py
    test_clusters_balancing.py
    test_list_zone_record_sets.py
    test_update_records_one_cluster.py
    test_updates_replication.py
)

PEERDIR(
    infra/yp_dns_api/tests/helpers/py
    infra/yp_dns_api/bridge/api
    infra/yp_dns_api/bridge/python/daemon
    infra/yp_dns_api/config
    infra/yp_dns_api/python/client
    infra/libs/local_yp
    infra/libs/yp_dns/changelist/proto
    yp/python/local
    yp/tests/helpers
    library/cpp/dwarf_backtrace/registry
    library/python/resource
)

TIMEOUT(600)

SIZE(MEDIUM)

ENV(YP_TOKEN=secret)

ENABLE(NO_STRIP)

TAG(ya:norestart)

FORK_TEST_FILES()

FORK_TESTS()

REQUIREMENTS(ram:20)

END()

RECURSE(
    helpers
)
