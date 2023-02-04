PEERDIR(infra/ebpf-agent/tests)

DATA(
    arcadia/infra/ebpf-agent/progs/include/utils.h
    arcadia/infra/ebpf-agent/lib/nets.cpp
)

DEPENDS(
    infra/ebpf-agent
    infra/ebpf-agent/utils/net-stat
    infra/kernel/tools/bpftool/release
    infra/kernel/tools/bpftrace/release
)

SIZE(MEDIUM)

REQUIREMENTS(
    cpu:4
    ram:8
    network:full
)
