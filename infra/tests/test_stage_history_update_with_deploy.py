import pytest

from infra.deploy_notifications_controller.lib.models.action import InfraChange, Notification
from infra.deploy_notifications_controller.lib.models.stage_history_change import ChangeKind
from test_stage_history_change_utils import create_stage, create_update, default_old_spec_revision, \
    default_old_du_revision, create_spec, create_input_data, default_update_spec_revision, \
    create_status, default_update_du_revision, \
    default_infra_parameters, default_du_id, create_notification_policy, \
    DEFAULT_NOTIFICATION_ACTIONS, default_update_dr_revision, default_old_dr_revision, StageInfraParameters, \
    default_update_du_revision_delta
from test_stage_history_update_with_deploy_utils import one_du_deployed_updated_progress, one_du_deployed_old_progress, \
    TestStageDeployParameters, create_expected_message_generator, create_expected_infras_generator, \
    update_stage_deploy_scenario, dummy_qnotifier_message_generator, expected_empty_infras_generator, \
    two_du_deployed_old_progress, two_du_deployed_updated_progress, one_du_deploying_progress, \
    two_du_deploying_progress, create_all_du_deploying_after_progress, create_all_du_deployed_after_progress


two_du_one_deploying_progress = two_du_deploying_progress.copy_with(
    du_revisions=[default_update_du_revision, default_old_du_revision],
)

two_du_one_deployed_updated_progress = create_all_du_deployed_after_progress(
    old_progress=two_du_one_deploying_progress,
)

one_du_deploying_after_deploying_progress = create_all_du_deploying_after_progress(
    old_progress=one_du_deploying_progress,
)

two_du_deploying_after_deploying_progress = create_all_du_deploying_after_progress(
    old_progress=two_du_deploying_progress,
)

one_du_deploying_after_deployed_progress = create_all_du_deploying_after_progress(
    old_progress=one_du_deployed_updated_progress,
)

two_du_deploying_after_deployed_progress = create_all_du_deploying_after_progress(
    old_progress=two_du_deployed_updated_progress,
)


@pytest.mark.parametrize(
    'parameters',
    [
        TestStageDeployParameters(
            old_stage_progress=one_du_deployed_old_progress,
            updated_stage_progress=one_du_deploying_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=one_du_deployed_old_progress,
            updated_stage_progress=one_du_deploying_progress.copy_with(
                spec_revision=one_du_deployed_old_progress.spec_revision,
            )
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_deployed_old_progress,
            updated_stage_progress=two_du_deploying_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_deployed_old_progress,
            updated_stage_progress=two_du_one_deploying_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=one_du_deploying_progress,
            updated_stage_progress=one_du_deploying_after_deploying_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_deploying_progress,
            updated_stage_progress=two_du_deploying_after_deploying_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_deploying_progress,
            updated_stage_progress=two_du_deploying_after_deploying_progress.copy_with(
                du_revisions=[
                    default_update_du_revision + default_update_du_revision_delta,
                    default_update_du_revision,
                ],
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_deploying_progress,
            updated_stage_progress=two_du_deploying_after_deploying_progress.copy_with(
                du_revisions=[
                    default_update_du_revision + default_update_du_revision_delta,
                    default_update_du_revision,
                ],
                du_ldrs=[
                    default_old_du_revision,
                    default_update_du_revision,
                ],
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=one_du_deploying_progress,
            updated_stage_progress=one_du_deploying_after_deployed_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_deploying_progress,
            updated_stage_progress=two_du_deploying_after_deployed_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_deploying_progress,
            updated_stage_progress=two_du_deploying_after_deployed_progress.copy_with(
                du_revisions=[
                    default_update_du_revision + default_update_du_revision_delta,
                    default_update_du_revision,
                ],
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_one_deploying_progress,
            updated_stage_progress=two_du_one_deploying_progress.copy_with(
                du_revisions=[default_update_du_revision, default_update_du_revision],
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_one_deploying_progress,
            updated_stage_progress=two_du_one_deployed_updated_progress.copy_with(
                du_revisions=[default_update_du_revision, default_update_du_revision],
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_one_deploying_progress,
            updated_stage_progress=create_all_du_deploying_after_progress(
                old_progress=two_du_one_deploying_progress,
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_one_deploying_progress,
            updated_stage_progress=create_all_du_deploying_after_progress(
                old_progress=two_du_one_deployed_updated_progress,
            ),
        ),
    ],
    ids=[
        "one du was deployed and started",
        "one du was deployed and started, but spec revision not changed",
        "two du were deployed and both started",
        "two du were deployed and only one of them started",
        "one du was deploying and started again",
        "two du were deploying, but both started again",
        "two du were deploying, but one started again",
        "two du were deploying, but one started again and other finished",
        "one du was deploying and finished, but started again after",
        "two du were deploying and both finished, but both started again after",
        "two du were deploying and both finished, but one started again after",
        "two du: one was deploying, but other started",
        "two du: one was deploying and finished, but other started",
        "two du: one was deploying, but both started",
        "two du: one was deploying and finished, but both started after",
    ]
)
def test_update_stage_deploy_started(
    parameters: TestStageDeployParameters,
):
    expected_message_generator = create_expected_message_generator(
        custom_change_kinds={
            ChangeKind.DEPLOYING,
            ChangeKind.DEPLOYED,
        },
        revisions={
            parameters.updated_stage_progress.spec_revision - 1,
            parameters.updated_stage_progress.spec_revision,
        },
    )

    expected_infras_generator = create_expected_infras_generator(
        [
            (parameters.updated_stage_progress.spec_revision - 1, InfraChange.EventKind.CANCELLED),
            (parameters.updated_stage_progress.spec_revision, InfraChange.EventKind.STARTED),
        ],
    )

    update_stage_deploy_scenario(
        parameters=parameters,
        expected_message_generator=expected_message_generator,
        expected_infras_generator=expected_infras_generator,
    )


@pytest.mark.parametrize(
    'parameters',
    [
        TestStageDeployParameters(
            old_stage_progress=one_du_deploying_progress,
            updated_stage_progress=one_du_deployed_updated_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_deploying_progress,
            updated_stage_progress=two_du_deployed_updated_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_one_deploying_progress,
            updated_stage_progress=two_du_one_deployed_updated_progress,
        ),
    ],
    ids=[
        "one du was deploying and finished",
        "two du were deploying and both finished",
        "two du: one du was deploying and finished, other was deployed all time",
    ]
)
def test_update_stage_deploy_finished(
    parameters: TestStageDeployParameters,
):
    expected_infras_generator = create_expected_infras_generator(
        [
            (parameters.updated_stage_progress.spec_revision, InfraChange.EventKind.FINISHED),
        ],
    )

    update_stage_deploy_scenario(
        parameters=parameters,
        expected_message_generator=dummy_qnotifier_message_generator,  # TODO see https://st.yandex-team.ru/DEPLOY-4157
        expected_infras_generator=expected_infras_generator,
    )


@pytest.mark.parametrize(
    'parameters',
    [
        TestStageDeployParameters(
            old_stage_progress=one_du_deploying_progress,
            updated_stage_progress=one_du_deployed_updated_progress.copy_with(
                spec_revision=one_du_deployed_updated_progress.spec_revision + 1,
            ),
        ),
    ],
    ids=[
        "one du was deploying and finished, but there was zero diff",
    ]
)
def test_update_stage_deploy_finished_with_zero_diff(
    parameters: TestStageDeployParameters,
):
    expected_message_generator = create_expected_message_generator(
        custom_change_kinds={
            ChangeKind.DEPLOYED,
        },
        revisions={
            parameters.updated_stage_progress.spec_revision,
        },
    )

    expected_infras_generator = create_expected_infras_generator(
        [
            (parameters.updated_stage_progress.spec_revision, InfraChange.EventKind.FINISHED),
        ],
    )

    update_stage_deploy_scenario(
        parameters=parameters,
        expected_message_generator=expected_message_generator,
        expected_infras_generator=expected_infras_generator,
    )


@pytest.mark.parametrize(
    'parameters',
    [
        TestStageDeployParameters(
            old_stage_progress=one_du_deployed_old_progress,
            updated_stage_progress=one_du_deployed_old_progress.copy_with(
                du_ldrs=[None],
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=two_du_deploying_progress,
            updated_stage_progress=two_du_deploying_progress.copy_with(
                du_ldrs=[default_update_du_revision, default_old_du_revision],
            ),
        ),
    ],
    ids=[
        "one du was deployed, but ldr went to None (backward compatibility test)",
        "two du were deploying, but only one finished",
    ]
)
def test_update_stage_deploy_not_changed(
    parameters: TestStageDeployParameters,
):
    update_stage_deploy_scenario(
        parameters=parameters,
        expected_message_generator=dummy_qnotifier_message_generator,
        expected_infras_generator=expected_empty_infras_generator,
    )


@pytest.mark.parametrize(
    'parameters',
    [
        TestStageDeployParameters(
            old_stage_progress=one_du_deployed_old_progress,
            updated_stage_progress=one_du_deployed_updated_progress,
        ),
        TestStageDeployParameters(
            old_stage_progress=one_du_deploying_progress,
            updated_stage_progress=create_all_du_deployed_after_progress(
                old_progress=create_all_du_deploying_after_progress(
                    old_progress=one_du_deployed_updated_progress,
                ),
            ),
        )
    ],
    ids=[
        "one du was deployed, (deploying) and deployed again",
        "one du was deploying, (deployed and deploying) and deployed again",
    ]
)
def test_update_stage_deployed_with_revisions_changed(
    parameters: TestStageDeployParameters,
):
    expected_message_generator = create_expected_message_generator(
        custom_change_kinds={
            ChangeKind.DEPLOYED,
        },
        revisions={
            parameters.updated_stage_progress.spec_revision - 1,
            parameters.updated_stage_progress.spec_revision,
        },
    )

    expected_infras_generator = create_expected_infras_generator(
        [
            # TODO see https://st.yandex-team.ru/DEPLOY-4595
            # (parameters.updated_stage_progress.spec_revision, InfraChange.EventKind.STARTED),
            (parameters.updated_stage_progress.spec_revision - 1, InfraChange.EventKind.CANCELLED),
            (parameters.updated_stage_progress.spec_revision, InfraChange.EventKind.FINISHED),
        ],
    )

    update_stage_deploy_scenario(
        parameters=parameters,
        expected_message_generator=expected_message_generator,
        expected_infras_generator=expected_infras_generator,
    )


@pytest.mark.parametrize(
    'parameters',
    [
        TestStageDeployParameters(
            old_stage_progress=one_du_deployed_old_progress,
            updated_stage_progress=one_du_deploying_progress,
            infra_parameters=StageInfraParameters(
                infra_service=None,
                infra_environment=default_infra_parameters.infra_environment,
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=one_du_deployed_old_progress,
            updated_stage_progress=one_du_deploying_progress,
            infra_parameters=StageInfraParameters(
                infra_service=default_infra_parameters.infra_service,
                infra_environment=None,
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=one_du_deployed_old_progress,
            updated_stage_progress=one_du_deployed_old_progress.copy_with(
                spec_revision=default_old_spec_revision + 1,
            ),
        ),
        TestStageDeployParameters(
            old_stage_progress=one_du_deploying_progress,
            updated_stage_progress=one_du_deploying_progress.copy_with(
                spec_revision=default_update_spec_revision + 1,
            ),
        ),
    ],
    ids=[
        "one du was deployed and started, but infra service is None",
        "one du was deployed and started, but infra environment is None",
        "one du was deployed, but there was zero diff",
        "one du was deploying, but there was zero diff",
    ]
)
def test_update_stage_deploy_no_infras(
    parameters: TestStageDeployParameters,
):
    expected_message_generator = create_expected_message_generator(
        custom_change_kinds=set(),
        revisions={
            parameters.updated_stage_progress.spec_revision,
        },
    )

    update_stage_deploy_scenario(
        parameters=parameters,
        expected_message_generator=expected_message_generator,
        expected_infras_generator=expected_empty_infras_generator,
    )


@pytest.mark.parametrize(
    'stage_du_ldr,update_spec_revision,update_du_revision,update_du_ldr,first_event_kind,second_event_kind',
    [
        (default_old_du_revision, default_update_spec_revision, default_update_du_revision, default_old_du_revision,
         Notification.EventKind.STAGE_DEPLOY_STARTED, Notification.EventKind.DEPLOY_UNIT_STARTED),
        (default_old_du_revision - 1, default_old_spec_revision, default_old_du_revision, default_old_du_revision,
         Notification.EventKind.STAGE_DEPLOYED, Notification.EventKind.DEPLOY_UNIT_FINISHED)
    ],
    ids=["deploy_started", "deploy_finished"]
)
def test_deploy_unit_deploy_w_notification_policy(
    stage_du_ldr: int,
    update_spec_revision: int,
    update_du_revision: int,
    update_du_ldr: int,
    first_event_kind: Notification.EventKind,
    second_event_kind: Notification.EventKind,
):
    du_id = default_du_id()
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=stage_du_ldr)
    notification_policy = create_notification_policy(
        stage,
        {
            'stage_actions': DEFAULT_NOTIFICATION_ACTIONS,
            'deploy_unit_actions': {
                du_id: DEFAULT_NOTIFICATION_ACTIONS,
            },
        }
    )

    update = create_update(
        spec=create_spec(update_spec_revision, update_du_revision),
        status=create_status(update_spec_revision, update_du_revision, du_ldr=update_du_ldr)
    )
    input_data = create_input_data(stage, notification_policy=notification_policy)
    _, output_data = update.process_changes(input_data)
    assert len(output_data.notifications) == 2
    assert output_data.notifications[0].event_kind == first_event_kind
    assert output_data.notifications[1].event_kind == second_event_kind
    assert output_data.notifications[1].deploy_unit_id == du_id


@pytest.mark.parametrize(
    'stage_dr_ready_status,update_spec_revision,update_dr_revision,update_dr_ready_status,result_event_kind',
    [
        (True, default_update_spec_revision, default_update_dr_revision, False, Notification.EventKind.DYNAMIC_RESOURCE_STARTED),
        (False, default_update_spec_revision, default_old_dr_revision, True, Notification.EventKind.DYNAMIC_RESOURCE_FINISHED)
    ],
    ids=["deploy_started", "deploy_finished"]
)
def test_dynamic_resource_deploy_w_notification_policy(
    stage_dr_ready_status: bool,
    update_spec_revision: int,
    update_dr_revision: int,
    update_dr_ready_status: bool,
    result_event_kind: Notification.EventKind,
):
    stage = create_stage(
        default_old_spec_revision,
        default_old_du_revision,
        du_ldr=default_old_du_revision,
        dr_revision=default_old_dr_revision,
        dr_ready_status=stage_dr_ready_status,
    )
    dr_id = 'test'
    notification_policy = create_notification_policy(
        stage,
        {
            'dynamic_resource_actions': {
                dr_id: DEFAULT_NOTIFICATION_ACTIONS,
            },
        }
    )

    update = create_update(
        spec=create_spec(update_spec_revision, default_old_du_revision, dr_revision=update_dr_revision),
        status=create_status(
            update_spec_revision,
            default_old_du_revision,
            du_ldr=default_old_du_revision,
            dr_revision=update_dr_revision,
            dr_ready_status=update_dr_ready_status,
        )
    )
    input_data = create_input_data(stage, notification_policy=notification_policy)
    _, output_data = update.process_changes(input_data)
    assert len(output_data.notifications) == 1
    assert output_data.notifications[0].event_kind == result_event_kind
    assert output_data.notifications[0].dynamic_resource_id == dr_id
