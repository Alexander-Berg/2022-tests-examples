PY3TEST()

OWNER(g:tentacles)

PEERDIR(
    contrib/python/requests-mock
    infra/rtc_sla_tentacles/backend/lib/tests
    infra/rtc_sla_tentacles/backend/lib/harvesters
    infra/rtc_sla_tentacles/backend/lib/harvesters_snapshots/tests_snapshots_data
)

TEST_SRCS(
    conftest.py
    test_clickhouse_dumper.py
    test_harvesters_manager.py
    test_hq_harvester.py
    test_harvesters_config_files.py
    test_juggler_harvester.py
    test_nanny_state_dumper.py
    test_resource_maker.py
    test_yp_lite.py
    test_ticker.py
)

REQUIREMENTS(network:full)
TAG(ya:external)

# https://wiki.yandex-team.ru/yatool/test/
# This macro makes directories inside available in tests environment.
# Used in `test_config_files.py` to test actual config files in this
# directory.
DATA(
    arcadia/infra/rtc_sla_tentacles/backend/conf
)

END()
