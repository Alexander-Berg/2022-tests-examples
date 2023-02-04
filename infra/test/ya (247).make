LIBRARY()

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/pod_agent/libs/behaviour/bt/nodes/base
    infra/pod_agent/libs/behaviour/bt/render
    infra/pod_agent/libs/behaviour/loaders
    infra/pod_agent/libs/behaviour/trees/base
    infra/pod_agent/libs/behaviour/trees/box
    infra/pod_agent/libs/behaviour/trees/layer
    infra/pod_agent/libs/behaviour/trees/static_resource
    infra/pod_agent/libs/behaviour/trees/volume
    infra/pod_agent/libs/behaviour/trees/workload/base
    infra/pod_agent/libs/behaviour/trees/workload/destroy
    infra/pod_agent/libs/behaviour/trees/workload/init
    infra/pod_agent/libs/behaviour/trees/workload/start
    infra/pod_agent/libs/behaviour/trees/workload/status
    infra/pod_agent/libs/behaviour/trees/workload/stop

    infra/pod_agent/libs/path_util
    infra/pod_agent/libs/pod_agent/update_holder/test_lib
    infra/pod_agent/libs/porto_client
    infra/pod_agent/libs/porto_client/porto_test_lib
)

SRCS(
    test_canon.cpp
    test_functions.cpp
)

END()
