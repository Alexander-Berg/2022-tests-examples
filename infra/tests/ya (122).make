PY3TEST()

OWNER(nekto0n)


TEST_SRCS(
    test_metrics.py
    test_sched.py
    test_selector.py
    test_sockutil.py
    test_validation.py
    test_limited_semaphore.py
)

PEERDIR(
    infra/orly/lib
)

END()
