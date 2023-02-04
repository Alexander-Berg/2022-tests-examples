from typing import Optional, List, Set, Any

import mock

from infra.deploy_notifications_controller.lib.models.event_meta import EventMeta
from infra.deploy_notifications_controller.lib.models.notification_policy import NotificationPolicy
from infra.deploy_notifications_controller.lib.models.stage_history_change import StageHistoryChange, ChangeKind, \
    StageHistoryUpdate
from infra.deploy_notifications_controller.lib.models.stage_progress import StageProgress
from infra.deploy_notifications_controller.lib.models.url_formatter import UrlFormatter
from yt.yson import YsonEntity, YsonBoolean

from infra.deploy_notifications_controller.lib.models.action import QnotifierMessage
from infra.deploy_notifications_controller.lib.models.stage import Meta, Spec, Status, Stage


url_formatter = UrlFormatter(stage_format='{controller} id={id}', user_format='{name}')


def update_dict(
    values: dict,
    **kwargs
):
    for key, value in kwargs.items():
        if value is None:
            values.pop(key)
        else:
            values[key] = value


def create_meta(
    **kwargs
) -> Meta:
    meta = {
        'effective_account_id': YsonEntity(),
        'inherit_acl': YsonBoolean(True),
        'type': 'stage',
        'project_id': 'maps-front-maps',
        'id': 'maps-front-maps_production',
        'creation_time': 1601567946612939,
        'account_id': 'abc:service:2348',
        'acl': [{'action': 'allow',
                 'permissions': ['read', 'write', 'create', 'ssh_access', 'root_ssh_access', 'read_secrets'],
                 'subjects': ['abc:service:2348', 'abc:service:2385', 'zomb-podrick', 'andreyvavilov']
                 }],
        'uuid': '2ad695-892f8180-8c625523-26ed2041',
    }

    update_dict(meta, **kwargs)

    return Meta(values=meta)


cluster_names = ['man', 'sas', 'vla']


def du_index_to_id(index: int) -> str:
    return 'app' + ('' if 0 == index else str(index))


def default_du_id() -> str:
    return du_index_to_id(0)


def create_spec_many_du(
    revision: int,
    du_revisions: List[int],
    dr_revision: Optional[int] = None,
    **kwargs
) -> Spec:
    clusters = [
        {'cluster': cluster_name,
         'spec': {'constraints': {'antiaffinity_constraints': [{'key': 'rack', 'max_pods': 1}]},
                  'replica_count': 60}}
        for cluster_name in cluster_names
    ]

    max_unavailable = 20 - max(du_revisions)

    deploy_units = dict()
    for index, du_revision in enumerate(du_revisions):
        deploy_units[du_index_to_id(index)] = {
            'multi_cluster_replica_set': {'replica_set': {
                'clusters': clusters,
                'deployment_strategy': {'max_unavailable': max_unavailable},
                'pod_template_spec': {'spec': {'pod_agent_payload': {'spec': {
                    'boxes': [{'id': 'app_box'}],
                    'mutable_workloads': [{'workload_ref': 'app_workload'}],
                    'workloads': [{
                        'box_ref': 'app_box',
                        'id': 'app_workload',
                        'transmit_logs': YsonBoolean(True)
                    }]
                }}}},
            }},
            'revision': du_revision,
        }

    spec = {
        'account_id': 'abc:service:2348',
        'deploy_units': deploy_units,
        'revision': revision,
        'revision_info': {'description': 'Update app: {repository=registry.yandex.net/maps/front-maps:v2.1093.0}'}
    }

    update_dict(spec, **kwargs)

    if dr_revision is not None:
        spec['dynamic_resources'] = {
            'test': {
                'dynamic_resource': {
                    'revision': dr_revision,
                },
                'deploy_unit_ref': 'app',
            }
        }

    return Spec(values=spec)


def create_spec(
    revision: int,
    du_revision: int,
    dr_revision: Optional[int] = None,
    **kwargs
) -> Spec:
    return create_spec_many_du(
        revision=revision,
        du_revisions=[du_revision],
        dr_revision=dr_revision,
        **kwargs
    )


bool_to_str = {
    False: 'false',
    True: 'true'
}


class ConditionState:
    READY = 'ready'
    IN_PROGRESS = 'in progress'
    FAILED = 'failed'


condition_to_progress = {
    ConditionState.READY: StageProgress.State.DEPLOYED,
    ConditionState.IN_PROGRESS: StageProgress.State.DEPLOYING
}

state_to_reason = {
    ConditionState.READY: '',
    ConditionState.IN_PROGRESS: 'RESOLVING_SANDBOX_RESOURCES',
    ConditionState.FAILED: 'FAILED'
}

DEFAULT_NOTIFICATION_ACTIONS = {
    'start_actions': [
        {
            'jns_message': {
                'project': 'test',
                'channel': 'test',
            }
        }
    ],
    'finish_actions': [
        {
            'jns_message': {
                'project': 'test',
                'channel': 'test',
            }
        }
    ],
}


def create_condition(status_state: StageProgress.State, condition_state: str) -> dict:
    expected_status_state = condition_to_progress.get(condition_state, None)
    status = (expected_status_state == status_state)

    condition = {'last_transition_time': {'nanos': 150614000, 'seconds': 1606133791}, 'status': bool_to_str[status]}

    reason = state_to_reason.get(status_state, '')
    if len(reason) > 0:
        condition['reason'] = reason

    return condition


def create_du_status(
    du_revision: int,
    du_ldr: Optional[int],
    **kwargs,
) -> dict[str, Any]:
    status_state = (
        StageProgress.State.DEPLOYED if du_revision == du_ldr else StageProgress.State.DEPLOYING)

    pods_total = 180
    pods_ready = pods_total - (1 if status_state == StageProgress.State.DEPLOYING else 0)

    failed_condition = create_condition(status_state, ConditionState.FAILED)
    in_progress_condition = create_condition(status_state, ConditionState.IN_PROGRESS)
    ready_condition = create_condition(status_state, ConditionState.READY)

    cluster_statuses = {
        cluster_name: {'endpoint_set_id': 'maps-front-maps_production.app'}
        for cluster_name in cluster_names
    }

    du_status = {
        'failed': failed_condition,
        'in_progress': in_progress_condition,
        'ready': ready_condition,
        'multi_cluster_replica_set': {
            'cluster_statuses': cluster_statuses,
            'replica_set_id': 'maps-front-maps_production.app'
        },
        'progress': {'pods_ready': pods_ready, 'pods_total': pods_total},
        'target_revision': du_revision,
        'target_spec_timestamp': 1724313653261369554,
        'yasm_itype': 'deploy',
    }

    update_dict(du_status, **kwargs)

    if du_ldr is not None:
        du_status['latest_deployed_revision'] = du_ldr

    return du_status


def create_status_many_du(
    revision: int,
    du_revisions: List[int],
    all_du_ldr: Optional[int] = None,
    du_ldrs: Optional[List[int]] = None,
    dr_revision: Optional[int] = None,
    dr_ready_status: Optional[bool] = None,
    **kwargs
) -> Status:
    if du_ldrs is None:
        du_ldrs = [all_du_ldr] * len(du_revisions)

    du_revisions_ldrs = zip(du_revisions, du_ldrs)

    deploy_units = dict()
    for index, du_revision_ldr in enumerate(du_revisions_ldrs):
        du_revision, du_ldr = du_revision_ldr
        deploy_units[du_index_to_id(index)] = create_du_status(
            du_revision,
            du_ldr,
            **kwargs
        )

    status = {
        'deploy_units': deploy_units,
        'revision': revision,
        'spec_timestamp': 1724313653261369554,
        'validated': {'last_transition_time': {'nanos': 478303000, 'seconds': 1601567970}, 'status': 'true'},
    }

    if dr_revision is not None:
        dr_status = {
            'revision': dr_revision,
            'ready': {
                'condition': {
                    'status': dr_ready_status
                }
            }
        }
        status['dynamic_resources'] = {'test': dr_status}

    return Status(values=status)


def create_status(
    revision: int,
    du_revision: int,
    du_ldr: Optional[int],
    dr_revision: Optional[int] = None,
    dr_ready_status: Optional[bool] = None,
    **kwargs
) -> Status:
    return create_status_many_du(
        revision=revision,
        du_revisions=[du_revision],
        all_du_ldr=du_ldr,
        dr_revision=dr_revision,
        dr_ready_status=dr_ready_status,
        **kwargs
    )

default_event = EventMeta(author='test', timestamp=10 ** 12)


class StageInfraParameters:
    __slots__ = [
        'infra_service',
        'infra_environment',
    ]

    infra_service: Optional[int]
    infra_environment: Optional[int]

    def __init__(
        self,
        infra_service: Optional[int],
        infra_environment: Optional[int],
    ):
        self.infra_service = infra_service
        self.infra_environment = infra_environment


default_infra_parameters = StageInfraParameters(
    infra_service=1234,
    infra_environment=5678
)

default_old_spec_revision = 11
default_old_du_revision = 1
default_old_dr_revision = 1

default_update_spec_revision_delta = 2
default_update_spec_revision = default_old_spec_revision + default_update_spec_revision_delta

default_update_du_revision_delta = 3
default_update_du_revision = default_old_du_revision + default_update_du_revision_delta

default_update_dr_revision_delta = 4
default_update_dr_revision = default_old_dr_revision + default_update_dr_revision_delta


def create_stage_many_du(
    revision: int,
    du_revisions: List[int],
    all_du_ldr: Optional[int] = None,
    du_ldrs: Optional[List[int]] = None,
    last_timestamp: Optional[int] = None,
    infra_parameters: Optional[StageInfraParameters] = default_infra_parameters,
    dr_revision: Optional[int] = None,
    dr_ready_status: Optional[bool] = None,
) -> Stage:
    meta = create_meta()

    spec = create_spec_many_du(
        revision,
        du_revisions,
        dr_revision=dr_revision,
    )

    status = create_status_many_du(
        revision,
        du_revisions,
        all_du_ldr=all_du_ldr,
        du_ldrs=du_ldrs,
        dr_revision=dr_revision,
        dr_ready_status=dr_ready_status,
    )

    if not last_timestamp:
        last_timestamp = default_event.timestamp - 1

    return Stage(
        meta=meta,
        spec=spec,
        status=status,
        infra_service=infra_parameters.infra_service,
        infra_environment=infra_parameters.infra_environment,
        last_timestamp=last_timestamp,
    )


def create_stage(
    revision: int,
    du_revision: int,
    du_ldr: Optional[int] = None,
    last_timestamp: Optional[int] = None,
    infra_parameters: Optional[StageInfraParameters] = default_infra_parameters,
    dr_revision: Optional[int] = None,
    dr_ready_status: Optional[bool] = None,
) -> Stage:
    return create_stage_many_du(
        revision=revision,
        du_revisions=[du_revision],
        all_du_ldr=du_ldr,
        last_timestamp=last_timestamp,
        infra_parameters=infra_parameters,
        dr_revision=dr_revision,
        dr_ready_status=dr_ready_status,
    )


def create_notification_policy(stage: Stage, spec: dict) -> NotificationPolicy:
    return NotificationPolicy(stage_id=stage.meta.id, spec=spec)


def create_update(
    meta: Optional[Meta] = None,
    spec: Optional[Spec] = None,
    status: Optional[Status] = None,
) -> StageHistoryUpdate:
    return StageHistoryUpdate(
        event=default_event,
        meta=meta,
        spec=spec,
        status=status,
    )


def create_input_data(
    stage: Stage,
    notification_policy: Optional[NotificationPolicy] = None,
) -> StageHistoryUpdate.InputData:
    return StageHistoryChange.InputData(
        stage,
        log=mock.Mock(),
        url_formatter=url_formatter,
        notification_policy=notification_policy,
    )


def create_expected_message(
    stage: Stage,
    change: StageHistoryChange,
    custom_tags: Optional[List[str]] = None,
    custom_change_kinds: Optional[Set[ChangeKind]] = None,
    revisions: Optional[Set[int]] = None,
) -> QnotifierMessage:
    output_data = StageHistoryChange.OutputData()

    if custom_tags:
        output_data.tags = custom_tags

    tags = change.create_tags(create_input_data(stage), output_data)

    change_kinds = {change.change_kind.value}
    if custom_change_kinds:
        for change_kind in custom_change_kinds:
            change_kinds.add(change_kind.value)

    return QnotifierMessage(
        stage_id=stage.id,
        timestamp=default_event.timestamp,
        title='',
        plain_text='',
        html='',
        tags=tags,
        attempts=0,
        authors={default_event.author.name},
        project_id=stage.project_id,
        change_kinds=change_kinds,
        revisions=revisions,
    )


def get_progress(stage: Stage):
    return StageHistoryUpdate.get_progress(stage.spec, stage.status)
