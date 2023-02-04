PY3TEST()

OWNER(
    g:deploy
)

TEST_SRCS(
    conftest.py
    test_add_sidecar_logrotate.py
    test_coredump_stage.py
    test_create_stage.py
    test_dynamic_resource_stage.py
    test_quota_capacity.py
    test_full_cycle_stage.py
    test_reallocation.py
    test_redeploy_restart.py
    test_replica_set_controller.py
    test_sbr_resolve.py
    test_sequence_allocation.py
    test_forward_compatibility.py
    test_sidecar_updater.py
    test_sequence_allocation_with_approve.py
    utils.py
)

SIZE(LARGE)

PEERDIR(
    contrib/python/psutil
    infra/awacs/proto
    infra/dctl/src
    library/python/resource
    yp/yp_proto/yp/client/api/proto
    yp/python/local
    yt/yt/python/yt_yson_bindings
)

RESOURCE(
    infra/stage_controller/daemon/it/bin/stage.yml /stage.yml
    infra/stage_controller/daemon/it/bin/stage-coredump.yml /stage-coredump.yml
    infra/stage_controller/daemon/it/bin/stage-dr.yml /stage-dr.yml
    infra/stage_controller/daemon/it/bin/stage-multi.yml /stage-multi.yml
    infra/stage_controller/daemon/it/bin/stage-multi-new.yml /stage-multi-new.yml
    infra/stage_controller/daemon/it/bin/stage-sbr.yml /stage-sbr.yml
    infra/stage_controller/daemon/it/bin/stage-sidecars.yml /stage-sidecars.yml
    infra/stage_controller/daemon/it/bin/stage-reallocation.yml /stage-reallocation.yml
    infra/stage_controller/daemon/it/bin/project.yml /project.yml
)

REQUIREMENTS(
    network:full
)

TAG(
    ya:external
    ya:fat
    ya:force_sandbox
    ya:huge_logs
    ya:not_autocheck
    ya:norestart
    ya:noretries
)

DEFAULT(STAGECTL_IT_TEST_SPLIT_FACTOR 10)
DEFAULT(STAGECTL_IT_TEST_TIMEOUT 3600)

# spread all tests between 10 chunks
# beware: this split factor is used for deploy quota size assumption.
# if you really-really need more concurrency pleace re-visit deploy qouta requests.
FORK_SUBTESTS()
SPLIT_FACTOR($STAGECTL_IT_TEST_SPLIT_FACTOR)

TIMEOUT($STAGECTL_IT_TEST_TIMEOUT)

END()

