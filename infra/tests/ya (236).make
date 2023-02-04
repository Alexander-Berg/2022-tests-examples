PY2TEST()

OWNER(
    olegsenin
)

PY_SRCS(
    test_lib.py
)

SET(QEMU_ROOTFS_DIR infra/environments/rtc-xenial/release/vm-image-ng)
INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)

DEPENDS(${ARCADIA_ROOT}/infra/shawshank/bin)
DATA(arcadia/infra/shawshank/tests/env_data)

TEST_SRCS(
    test_env.py
    test_decap.py
    test_master.py
    test_tun64.py
    test_util.py
    test_netlink.py
    test_vip.py
)

PEERDIR(
    infra/awacs/proto
    infra/shawshank/lib
    infra/porto/api_py
    contrib/python/mock
    contrib/python/pyroute2
    contrib/python/ipaddr
)

END()
