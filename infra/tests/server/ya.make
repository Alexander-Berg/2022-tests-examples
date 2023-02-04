PY2_LIBRARY()

OWNER(
    dmtrmonakhov
)

PY_SRCS(
    common.py
)

TEST_SRCS(
    conftest.py
    test_cli.py
    test_custom_labels.py
    test_dirutil.py
    test_disk.py
    test_dm_partless.py
    test_dm_syspart.py
    test_dm_syspart_lvm.py
    test_limits.py
    test_loglevel.py
    test_lvm.py
    test_mount.py
    test_root_reservation.py
    test_systemd.py
    test_salt_trees.py
)

PEERDIR(
    infra/diskmanager/lib
    infra/diskmanager/proto
    library/python/svn_version
    contrib/python/mock
)
END()
