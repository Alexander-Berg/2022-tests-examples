import pytest

from walle import restrictions
from walle.hosts import HostOperationState
from walle.maintenance_plot.scenarios_settings.noc_hard import NocHardMaintenancePlotSettings
from walle.scenario.mixins import ParentStageHandler
from walle.scenario.stage.host_group_scheduler_stage import HostGroupSchedulerStage
from walle.scenario.stage.host_group_wait_before_requesting_cms_stage import HostGroupWaitBeforeRequestingCmsStage
from walle.scenario.stage.noc_hard_host_group_approve_stage import (
    NocHardHostGroupApproveStage,
    ManualConfirmationHostGroupApproveStage,
)
from walle.scenario.stages import (
    CheckAndReportAboutDeadlinesStage,
    CheckAndReportAboutHostRestrictionsStage,
    HostRootStage,
    ReleaseHostStage,
    StageDesc,
    SwitchToMaintenanceHostStage,
)


@pytest.mark.parametrize("enable_manual_approval_after_hosts_power_off", (True, False))
@pytest.mark.parametrize("ignore_cms_on_host_operations", (True, False))
@pytest.mark.parametrize("get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time", (True, False))
@pytest.mark.parametrize("enable_redeploy_after_change_of_mac_address", (True, False))
def test_stages_are_generated_conditionally(
    enable_redeploy_after_change_of_mac_address,
    get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time,
    ignore_cms_on_host_operations,
    enable_manual_approval_after_hosts_power_off,
):
    scenario_settings = NocHardMaintenancePlotSettings(
        enable_redeploy_after_change_of_mac_address=enable_redeploy_after_change_of_mac_address,
        get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time=get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time,
        enable_manual_approval_after_hosts_power_off=enable_manual_approval_after_hosts_power_off,
    ).to_dict()

    stage_map = [
        # Unconditionally presents in every case
        StageDesc(stage_class=NocHardHostGroupApproveStage),
        # Conditioned on top level
        StageDesc(
            stage_class=CheckAndReportAboutHostRestrictionsStage,
            params={
                'restrictions_to_check': [
                    restrictions.AUTOMATED_REBOOT,
                    restrictions.AUTOMATED_PROFILE,
                    restrictions.AUTOMATED_REDEPLOY,
                    restrictions.AUTOMATED_DNS,
                    restrictions.AUTOMATION,
                ],
            },
            conditions={'if_any': [{'enable_redeploy_after_change_of_mac_address': True}]},
        ),
        # Always presents with default setting value match
        StageDesc(
            stage_class=HostGroupWaitBeforeRequestingCmsStage,
            params={
                'time_field_name': 'request_cms_x_seconds_before_maintenance_start_time',
            },
            conditions={'if_any': [{'ignore_cms_on_host_operations': False}]},
        ),
        StageDesc(
            stage_class=CheckAndReportAboutDeadlinesStage,
            params={
                'children': [
                    StageDesc(
                        stage_class=HostGroupSchedulerStage,
                        params={
                            'children': [
                                StageDesc(
                                    stage_class=HostRootStage,
                                    params={
                                        'children': [
                                            # Conditioned on nested level
                                            StageDesc(
                                                stage_class=SwitchToMaintenanceHostStage,
                                                params={
                                                    'ignore_cms': False,
                                                    'power_off': False,
                                                    'workdays_only': False,
                                                    'operation_state': HostOperationState.DECOMMISSIONED,
                                                },
                                                conditions={
                                                    'if_any': [
                                                        {
                                                            'get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time': False
                                                        }
                                                    ]
                                                },
                                            ),
                                            StageDesc(stage_class=ReleaseHostStage),
                                        ]
                                    },
                                ),
                            ]
                        },
                    )
                ],
            },
            conditions={'if_any': [{'enable_manual_approval_after_hosts_power_off': False}]},
        ),
        StageDesc(
            stage_class=ManualConfirmationHostGroupApproveStage,
            params={
                'option_name': 'enable_manual_approval_after_hosts_power_off',
            },
            conditions={'if_any': [{'enable_manual_approval_after_hosts_power_off': True}]},
        ),
    ]
    stage = ParentStageHandler([], stage_map=stage_map)

    expected_stages = [NocHardHostGroupApproveStage()]
    if enable_redeploy_after_change_of_mac_address is True:
        expected_stages.append(
            CheckAndReportAboutHostRestrictionsStage(
                restrictions_to_check=[
                    restrictions.AUTOMATED_REBOOT,
                    restrictions.AUTOMATED_PROFILE,
                    restrictions.AUTOMATED_REDEPLOY,
                    restrictions.AUTOMATED_DNS,
                    restrictions.AUTOMATION,
                ]
            )
        )
    expected_stages.append(
        HostGroupWaitBeforeRequestingCmsStage(time_field_name="request_cms_x_seconds_before_maintenance_start_time")
    )
    if enable_manual_approval_after_hosts_power_off is True:
        expected_stages.append(
            ManualConfirmationHostGroupApproveStage(option_name="enable_manual_approval_after_hosts_power_off")
        )
    else:
        if get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time is True:
            expected_stages.append(
                CheckAndReportAboutDeadlinesStage(
                    [
                        HostGroupSchedulerStage(
                            [
                                HostRootStage([ReleaseHostStage()]),
                            ]
                        ),
                    ]
                )
            )
        else:
            expected_stages.append(
                CheckAndReportAboutDeadlinesStage(
                    [
                        HostGroupSchedulerStage(
                            [
                                HostRootStage(
                                    [
                                        SwitchToMaintenanceHostStage(
                                            ignore_cms=False,
                                            power_off=False,
                                            workdays_only=False,
                                            operation_state=HostOperationState.DECOMMISSIONED,
                                        ),
                                        ReleaseHostStage(),
                                    ]
                                ),
                            ]
                        ),
                    ]
                )
            )
    assert stage.generate_stages(stage.params['stage_map'], scenario_settings) == expected_stages
