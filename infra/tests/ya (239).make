OWNER(torkve)

PY23_TEST()

PEERDIR(
    infra/skylib/porto

    infra/porto/api_py
    contrib/python/pytest
)

TEST_SRCS(
    conftest.py
    test_capabilities.py
    test_porto.py
)

TAG(ya:not_autocheck)

END()

