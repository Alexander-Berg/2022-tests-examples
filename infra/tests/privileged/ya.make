PY2TEST()

OWNER(g:netmon)

TAG(ya:manual ya:privileged ya:fat ya:external ya:force_sandbox)

SIZE(LARGE)

ALLOCATOR(J)

TEST_SRCS(test_controllers_socket_raw.py)

DEPENDS(infra/netmon/agent/agent)

PEERDIR(
    infra/netmon/agent/agent
)

REQUIREMENTS(
    container:260671263
    network:full
)



NO_CHECK_IMPORTS()

END()
