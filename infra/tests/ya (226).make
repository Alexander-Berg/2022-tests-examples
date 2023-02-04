PY3TEST()

OWNER(g:tentacles)

PEERDIR(
    contrib/python/PyYAML

    infra/rtc_sla_tentacles/backend/lib
    infra/rtc_sla_tentacles/backend/lib/juggler_checks_manager
    infra/rtc_sla_tentacles/backend/lib/config
    infra/rtc_sla_tentacles/backend/lib/tests
)

TEST_SRCS(
    conftest.py
    test_config_files.py
    test_clickhouse_config.py
    test_config_interface.py
    test_harvesters_config.py
    test_misc_config.py
    test_mongo_config.py
    test_secrets_config.py
)

# https://wiki.yandex-team.ru/yatool/test/
# This macro makes directories inside available in tests environment.
# Used in `test_config_files.py` to test actual config files in this
# directory.
DATA(
    arcadia/infra/rtc_sla_tentacles/backend/conf
)

END()
