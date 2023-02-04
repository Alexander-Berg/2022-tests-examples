PY3TEST()

OWNER(g:yp-dns)

TEST_SRCS(test.py)

PEERDIR(
    infra/yp_dns/config
    infra/yp_dns/libs/config/protos
    contrib/python/dnspython
    contrib/python/protobuf
)

REQUIREMENTS(network:full)

END()
