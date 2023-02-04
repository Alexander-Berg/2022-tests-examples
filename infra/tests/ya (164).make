PY3TEST()

OWNER(g:hostman)

TEST_SRCS(
    test_certctl.py
    test_certificate.py
    test_fileutil.py
    test_jugglerutil.py
)

PEERDIR(
    infra/rtc/certman
)

END()
