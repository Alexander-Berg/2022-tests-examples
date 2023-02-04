PY3_LIBRARY()

OWNER(
    dima-zakarov
    g:yp-dns
)

PY_SRCS(
    NAMESPACE infra.yp_dns_api.tests.helpers
    replicator.pyx
)

PEERDIR(
    infra/yp_dns_api/tests/helpers
)

END()
