import mock

from infra.dproxy.proto import dproxy_pb2
from infra.dproxy.src.api import migration_service


def make_request(section, environment_id):
    return mock.Mock(
        section=section,
        environment_id=environment_id
    )


def make_response(ready):
    return mock.Mock(
        migration_state=ready
    )


def test_migration_service():
    qloud_ready_list = ["ready_qloud_env"]
    platform_ready_list = ["ready_platform_env"]

    ready_qloud = make_request(
        section=dproxy_pb2.GetQloudEnvMigrationReadinessRequest.QloudSection.QLOUD,
        environment_id="ready_qloud_env"
    )
    expected_response = make_response(
        ready=dproxy_pb2.GetQloudEnvMigrationReadinessResponse.ReadinessState.READY
    )
    assert migration_service.is_ready_for_migrate(req=ready_qloud,
                                                  qloud_ready_list=qloud_ready_list,
                                                  platform_ready_list=platform_ready_list
                                                  ).migration_state == expected_response.migration_state

    not_ready_qloud = make_request(
        section=dproxy_pb2.GetQloudEnvMigrationReadinessRequest.QloudSection.QLOUD,
        environment_id="not_ready_qloud_env"
    )
    expected_response = make_response(
        ready=dproxy_pb2.GetQloudEnvMigrationReadinessResponse.ReadinessState.NOT_READY
    )
    assert migration_service.is_ready_for_migrate(req=not_ready_qloud,
                                                  qloud_ready_list=qloud_ready_list,
                                                  platform_ready_list=platform_ready_list
                                                  ).migration_state == expected_response.migration_state

    ready_platform = make_request(
        section=dproxy_pb2.GetQloudEnvMigrationReadinessRequest.QloudSection.PLATFORM,
        environment_id="ready_platform_env"
    )
    expected_response = make_response(
        ready=dproxy_pb2.GetQloudEnvMigrationReadinessResponse.ReadinessState.READY
    )
    assert migration_service.is_ready_for_migrate(req=ready_platform,
                                                  qloud_ready_list=qloud_ready_list,
                                                  platform_ready_list=platform_ready_list
                                                  ).migration_state == expected_response.migration_state

    not_ready_platform = make_request(
        section=dproxy_pb2.GetQloudEnvMigrationReadinessRequest.QloudSection.PLATFORM,
        environment_id="not_ready_platform_env"
    )
    expected_response = make_response(
        ready=dproxy_pb2.GetQloudEnvMigrationReadinessResponse.ReadinessState.NOT_READY
    )
    assert migration_service.is_ready_for_migrate(req=not_ready_platform,
                                                  qloud_ready_list=qloud_ready_list,
                                                  platform_ready_list=platform_ready_list
                                                  ).migration_state == expected_response.migration_state
