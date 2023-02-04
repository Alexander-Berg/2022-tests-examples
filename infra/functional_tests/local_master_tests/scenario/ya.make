PY3_LIBRARY()

OWNER(
    g:yp-sd
)

PEERDIR(
    infra/yp_service_discovery/functional_tests/common

    yp/cpp/yp
    infra/libs/logger

    library/cpp/json
    library/cpp/testing/unittest

    library/cpp/yson/node
    library/cpp/dwarf_backtrace/registry
)

PY_SRCS(
    scenario.pyx
)

SRCS(
    scenario.cpp
)

END()
