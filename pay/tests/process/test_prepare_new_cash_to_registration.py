from hamcrest import equal_to, assert_that
from mock import patch, Mock

from tests.processes_testing_instances import run_process, prepare_process_instance
from yb_darkspirit.process.base import ProcessRunStatus
from yb_darkspirit.process.prepare_new_cash_to_ready_to_registration import \
    PrepareNewCashToReadyToRegistrationProcess
from yb_darkspirit.process.stages import WaitingStage
from yb_darkspirit.scheme import DsCashState
from yb_darkspirit.core.cash_register.maintenance_actions import (
    UploadChpassoldToWhitespiritAction,
    UploadActualFirmwareToWhitespiritAction,
    UploadResizePatchToWhitespiritAction, ChangeSSHPasswordAction, SetupSshConnectionAction, LoadAdminPasswordAction,
    ResizeDiskPartitionsAction, ApplyActualFirmwareAction, SetDatetimeAction, MakeReadyToRegistrationAction,
)

from tests import ws_responses


def test_pipeline(session, cr_wrapper_unregistered, ws_mocks,
                  ofd_private_client, tracker_client,
                  check_if_ready_for_actions, filter_failed_checks):
    def assert_stage_is(stage_name, is_final=False):
        stage = PrepareNewCashToReadyToRegistrationProcess.stages()[stage_name].stage
        assert_that(instance.stage, equal_to(stage.name))
        if isinstance(stage, WaitingStage):
            check_if_ready_for_actions.return_value = {}
        else:
            check_if_ready_for_actions.return_value = {stage.maintenance_action.get_name(): []}
        res = run_process(session, instance, process)
        assert_that(res.status, equal_to(ProcessRunStatus.COMPLETE if is_final else ProcessRunStatus.IN_PROGRESS))

    cr_wrapper = cr_wrapper_unregistered
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD
    )

    process = PrepareNewCashToReadyToRegistrationProcess()

    instance = prepare_process_instance(cr_wrapper, process, process.initial_stage())
    with session.begin():
        session.add(instance)
        with patch.object(UploadChpassoldToWhitespiritAction, 'apply', Mock()):
            assert_stage_is(UploadChpassoldToWhitespiritAction.get_lower_name())
        assert_stage_is('wait_pass_patch')

        ws_mocks.cashmachines_upgrade(cr_long_sn=cr_wrapper.whitespirit_key)
        assert_stage_is(ChangeSSHPasswordAction.get_lower_name())
        assert_stage_is('wait_change_pass')

        ws_mocks.cashmachines_setup_ssh_connection(cr_wrapper.long_serial_number)
        assert_stage_is(SetupSshConnectionAction.get_lower_name())
        assert_stage_is('wait_ssh_connection')

        ws_mocks.cashmachines_get_password(
            cr_wrapper.long_serial_number,
            use_password=False,
            body='{"Password": 666666}'
        )
        assert_stage_is(LoadAdminPasswordAction.get_lower_name())
        assert_stage_is('wait_load_password')

        with patch.object(UploadResizePatchToWhitespiritAction, 'apply', Mock()):
            assert_stage_is(UploadResizePatchToWhitespiritAction.get_lower_name())
        assert_stage_is('wait_upload_resize')

        ws_mocks.cashmachines_upgrade(cr_long_sn=cr_wrapper.whitespirit_key)
        assert_stage_is(ResizeDiskPartitionsAction.get_lower_name())
        assert_stage_is('wait_apply_resize')

        with patch.object(UploadActualFirmwareToWhitespiritAction, 'apply', Mock()):
            assert_stage_is(UploadActualFirmwareToWhitespiritAction.get_lower_name())
        assert_stage_is('wait_firmware')

        ws_mocks.cashmachines_upgrade(cr_long_sn=cr_wrapper.whitespirit_key)
        assert_stage_is(ApplyActualFirmwareAction.get_lower_name())
        assert_stage_is('wait_apply_firmware')

        ws_mocks.cashmachines_set_datetime_json(cr_long_sn=cr_wrapper.long_serial_number)
        assert_stage_is(SetDatetimeAction.get_lower_name())

        assert_stage_is(MakeReadyToRegistrationAction.get_lower_name(), is_final=True)

        assert_that(instance.cash_register.ds_state, equal_to(DsCashState.READY_TO_REGISTRATION.value))
