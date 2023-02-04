PY3TEST()
OWNER(g:kernel)
TAG(ya:manual)

DATA(
    arcadia/infra/ebpf-agent/progs/obj/dummy_egress.o
)

# Generic test config
INCLUDE(${ARCADIA_ROOT}/infra/kernel/test/misc/config.inc)
END()
