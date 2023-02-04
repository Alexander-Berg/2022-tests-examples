PY2TEST()

OWNER(okats)

SIZE(LARGE)
TAG(ya:fat)
FORK_SUBTESTS()

DEPENDS(
    infra/callisto/deploy/deployer/python
    infra/callisto/deploy/deployer/testing/workload
)

PEERDIR(
    infra/callisto/protos/deploy
    infra/callisto/deploy/deployer/config
)

TEST_SRCS(
    extended_status.py
    helper.py
    startup.py
    static.py
)

ENV(CHAOS_FAILURE_PROBABILITY=0.5)  # Deadly unstable environment

END()

RECURSE(
    workload
)
