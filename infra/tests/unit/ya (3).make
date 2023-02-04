PY2TEST()

OWNER(
    g:nanny
)

TEST_SRCS(
    conftest.py
    test_cms.py
    test_envutil.py
    test_util.py
    test_config.py
    test_confutil.py
    test_job.py
    test_filters.py
    test_specutil.py
    test_tcp_check.py
    test_porto_container_wrapper.py
    test_volumes_its.py
)

PEERDIR(
    contrib/python/mock
    infra/nanny/instancectl/src
)


NO_CHECK_IMPORTS()

END()
