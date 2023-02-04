PY23_TEST()

TEST_SRCS(
    conftest.py
    test_core_config.py
    test_flask_h.py
    test_fs_util.py
    test_metrics_average.py
    test_metrics_counter.py
    test_metrics_histogram.py
    test_metrics_inventory.py
    test_metrics_meter.py
    test_mongodb.py
    test_mongoengine_patches.py
    test_monotonic.py
    test_prof.py
    test_util_fs.py
    test_util_log_formatters.py
    test_util_misc.py
    test_util_net_mail.py
    test_util_retry.py
    test_yandex_blackbox.py
    test_yandex_oauth.py
    test_yandex_passport.py
    utils.py
    utils_test.py
)

PEERDIR(
    contrib/python/mock
    infra/walle/server/contrib/sepelib/core
    infra/walle/server/contrib/sepelib/flask
    infra/walle/server/contrib/sepelib/flask-test
    infra/walle/server/contrib/sepelib/gevent
    infra/walle/server/contrib/sepelib/http
    infra/walle/server/contrib/sepelib/metrics
    infra/walle/server/contrib/sepelib/mongo
    infra/walle/server/contrib/sepelib/mongo-test
    infra/walle/server/contrib/sepelib/subprocess
    infra/walle/server/contrib/sepelib/util
    infra/walle/server/contrib/sepelib/yandex
)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

SIZE(MEDIUM)

END()
