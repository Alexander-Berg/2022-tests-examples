# Tests are actually declared in peerdir library
PEERDIR(infra/kernel/test/misc)

DEPENDS(
    arc/local/bin

    infra/kernel/tools/bpftool/release
    infra/kernel/tools/fio/release
    infra/kernel/tools/fsstress
    infra/kernel/tools/perf/release
    infra/kernel/tools/stress-ng/release
    infra/kernel/test/schbench

    contrib/libs/liburing/test/file-update
    contrib/libs/liburing/test/232c93d07b74-test
    contrib/libs/liburing/test/35fa71a030ca-test
    contrib/libs/liburing/test/500f9fbadef8-test
    contrib/libs/liburing/test/7ad0e4b2f83c-test
    contrib/libs/liburing/test/8a9973408177-test
    contrib/libs/liburing/test/917257daa0fe-test
    contrib/libs/liburing/test/a0908ae19763-test
    contrib/libs/liburing/test/a4c0b3decb33-test
    contrib/libs/liburing/test/accept
    contrib/libs/liburing/test/accept-link
    contrib/libs/liburing/test/accept-reuse
    contrib/libs/liburing/test/accept-test
    contrib/libs/liburing/test/across-fork
    contrib/libs/liburing/test/b19062a56726-test
    contrib/libs/liburing/test/b5837bd5311d-test
    contrib/libs/liburing/test/ce593a6c480a-test
    contrib/libs/liburing/test/close-opath
    contrib/libs/liburing/test/connect
    contrib/libs/liburing/test/cq-full
    contrib/libs/liburing/test/cq-overflow
    contrib/libs/liburing/test/cq-peek-batch
    contrib/libs/liburing/test/cq-ready
    contrib/libs/liburing/test/cq-size
    contrib/libs/liburing/test/d4ae271dfaae-test
    contrib/libs/liburing/test/d77a67ed5f27-test
    contrib/libs/liburing/test/defer
    contrib/libs/liburing/test/eeed8b54e0df-test
    contrib/libs/liburing/test/eventfd
    contrib/libs/liburing/test/eventfd-disable
    contrib/libs/liburing/test/eventfd-ring
    contrib/libs/liburing/test/fadvise
    contrib/libs/liburing/test/fallocate
    contrib/libs/liburing/test/fc2a85cb02ef-test
    contrib/libs/liburing/test/file-register
    contrib/libs/liburing/test/file-update
    contrib/libs/liburing/test/fixed-link
    contrib/libs/liburing/test/fsync
    contrib/libs/liburing/test/io-cancel
    contrib/libs/liburing/test/io_uring_enter
    contrib/libs/liburing/test/io_uring_register
    contrib/libs/liburing/test/io_uring_setup
    contrib/libs/liburing/test/iopoll
    contrib/libs/liburing/test/lfs-openat
    contrib/libs/liburing/test/lfs-openat-write
    contrib/libs/liburing/test/link
    contrib/libs/liburing/test/link-timeout
    contrib/libs/liburing/test/link_drain
    contrib/libs/liburing/test/madvise
    contrib/libs/liburing/test/nop
    contrib/libs/liburing/test/open-close
    contrib/libs/liburing/test/openat2
    contrib/libs/liburing/test/personality
    contrib/libs/liburing/test/poll
    contrib/libs/liburing/test/poll-cancel
    contrib/libs/liburing/test/poll-cancel-ton
    contrib/libs/liburing/test/poll-link
    contrib/libs/liburing/test/poll-many
    contrib/libs/liburing/test/poll-v-poll
    contrib/libs/liburing/test/probe
    contrib/libs/liburing/test/read-write
    contrib/libs/liburing/test/ring-leak
    contrib/libs/liburing/test/send_recv
    contrib/libs/liburing/test/send_recvmsg
    contrib/libs/liburing/test/shared-wq
    contrib/libs/liburing/test/short-read
    contrib/libs/liburing/test/socket-rw
    contrib/libs/liburing/test/splice
    contrib/libs/liburing/test/sq-full
    contrib/libs/liburing/test/sq-full-cpp
    contrib/libs/liburing/test/sq-poll-kthread
    contrib/libs/liburing/test/sq-space_left
    contrib/libs/liburing/test/stdout
    contrib/libs/liburing/test/submit-reuse
    contrib/libs/liburing/test/teardowns
    contrib/libs/liburing/test/timeout
    contrib/libs/liburing/test/timeout-overflow
)

# To fit in host with 32cpu and 64gb of ram
SPLIT_FACTOR(8)
REQUIREMENTS(
    cpu:4
    ram:8
# Network required for test_nvidia_module.py
    network:full
)

DATA(
    arcadia/arc/ci/tests/store
    arcadia/infra/ebpf-agent/progs/obj
)

SIZE(MEDIUM)
TAG(
    ya:external
    ya:norestart
)
FORK_TESTS(MODULO)
