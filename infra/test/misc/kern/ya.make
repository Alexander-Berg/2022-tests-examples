PY3_LIBRARY()

OWNER(g:kernel)

PY_SRCS(
    __init__.py
    cgroup.py
    disk.py
    fio.py
    kernel.py
    misc.py
    namespace.py
    pagemap.py
    proc.py
    quota.py
    syscall.py
    task.py
)

PEERDIR(
    library/python/testing/yatest_common
)
END()
