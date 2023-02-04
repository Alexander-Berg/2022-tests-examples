import base64
import datetime
import json

from butils.application import getApplication
from mock import patch, Mock, PropertyMock

import pytest
from hamcrest import assert_that, equal_to, contains_string
from contextlib2 import nullcontext as does_not_raise
from uuid import UUID

from six import BytesIO

from tests import ws_responses
from tests.processes_testing_instances import (
    TestMaintenanceAction, TestingChecks, TestProcess,
    prepare_process_instance, run_process, FnsregTestResponse, TestSwitchingAction, prepare_info_for_rejection_code
)
from yb_darkspirit import scheme
from yb_darkspirit.api.errors import ServerError
from yb_darkspirit.core.cash_register import NotReadyException, CREATE_REREGISTRATION_APPLICATION_ACTION_NAME, \
    REGISTRATION_UUID_FIELD, REGISTRATION_REPORT_UUID_FIELD, CREATE_REGISTRATION_APPLICATION_ACTION_NAME, \
    CREATE_REGISTRATION_REPORT_ACTION_NAME
from yb_darkspirit.core.cash_register.maintenance_actions import (
    IssueAfterFSChangeAction, CheckOnlineAfterIssueAction, HideCashAction,
    ClearDeviceReregistrationAction, ConfigureAction, TicketCreationMixin,
    CreateApplicationIssueTicketForReregistrationAction, get_rereg_application_upload_issue_key,
    CreateReregistrationApplicationAction, UploadMixin,
    UploadToFnsregReregistrationApplicationAction, SwitchToActionMixin, RetryReregApplicationAction,
    UpdateGroupsAction, ReregistrationApplicationUploader, CreateIssueTicketForRegistrationReportAction,
    CreateApplicationIssueTicketForRegistrationAction, NewRegistrationAction, CreateRegistrationApplicationAction,
    CreateRegistrationReportAction, UploadToFnsregRegistrationApplicationAction, UploadToFnsregRegistrationReportAction,
    UploadToS3RegistrationCardAction, UploadToS3RegistrationReportCardAction,
)
from yb_darkspirit.core.cash_register.state_checks import Checks, FailedCheck, FailedCheckResult
from yb_darkspirit.core.errors import MissingProcessError
from yb_darkspirit.interactions import TrackerClient, FnsregClient, DocumentsClient
from yb_darkspirit.interface import CashRegister
from yb_darkspirit.process.schemas import RegistrationConfigSchema
from yb_darkspirit.process.stages import Stage


def test_get_checks_methods():
    assert_that(TestMaintenanceAction.get_skipping_checks(), equal_to({TestingChecks.TEST_CHECK_FALSE}))

    assert_that(TestMaintenanceAction.get_readiness_checks(), equal_to({TestingChecks.TEST_CHECK_TRUE}))

    assert_that(
        TestMaintenanceAction.get_checks(), equal_to({TestingChecks.TEST_CHECK_FALSE, TestingChecks.TEST_CHECK_TRUE})
    )


@patch.object(
    TestProcess,
    '_stages_list',
    [
        Stage(CheckOnlineAfterIssueAction),
        Stage(TestMaintenanceAction, name='finishing_stage'),
    ]
)
@pytest.mark.parametrize('wait_for,failed_checks,expected_exception,next_stage', [
    (datetime.timedelta(minutes=5), [Checks.ONLINE], does_not_raise(), 'check_online_after_issue'),
    (None, [Checks.ONLINE], pytest.raises(NotReadyException), 'check_online_after_issue'),
    (None, [], does_not_raise(), 'finishing_stage')
])
def test_checkonline_flow(
        session, cr_wrapper_fatal_error_after_change_fs, filter_failed_checks, need_wait_issue, wait_for, failed_checks,
        expected_exception, next_stage
):
    cr_wrapper = cr_wrapper_fatal_error_after_change_fs

    process = TestProcess()

    instance = prepare_process_instance(cr_wrapper, process)
    filter_failed_checks.return_value = [FailedCheck(check, FailedCheckResult('')) for check in failed_checks]
    need_wait_issue.return_value = wait_for

    with session.begin():
        session.add(instance)
        with expected_exception:
            run_process(session, instance, process)

    assert_that(instance.stage, equal_to(next_stage))


@patch.object(
    TestProcess,
    '_stages_list',
    [
        Stage(IssueAfterFSChangeAction),
        Stage(TestMaintenanceAction, name='finishing_stage'),
    ]
)
@pytest.mark.parametrize('wait_for,failed_checks,next_stage,ticket_set', [
    (datetime.timedelta(minutes=5), [], 'finishing_stage', False),
    (None, [], 'finishing_stage', False),
    (None, [Checks.ONLINE], 'finishing_stage', True),
    (datetime.timedelta(minutes=5), [Checks.ONLINE], 'issue_after_fs_change', False),
])
def test_issue_after_fs_flow(
        session, cr_wrapper_fatal_error_after_change_fs, filter_failed_checks, need_wait_fs,
        get_or_create_ticket_issue_after_change_fs, wait_for, failed_checks, next_stage, ticket_set
):
    cr_wrapper = cr_wrapper_fatal_error_after_change_fs
    process = TestProcess()

    get_or_create_ticket_issue_after_change_fs.return_value = 'ticket_data'
    ticket_data = 'ticket_data' if ticket_set else None
    instance = prepare_process_instance(cr_wrapper, process)
    filter_failed_checks.return_value = [FailedCheck(check, FailedCheckResult('')) for check in failed_checks]
    need_wait_fs.return_value = wait_for

    with session.begin():
        session.add(instance)
        run_process(session, instance, process)

    assert_that(instance.stage, equal_to(next_stage))
    assert_that(instance.get_data_field(stage_name='issue_after_fs_change', field_name='ticket'), equal_to(ticket_data))


class TestTicketCreationMixinAction(TicketCreationMixin, TestMaintenanceAction):
    name_ = 'TEST_TICKET_CREATION_MIXIN'

    @classmethod
    def get_or_create_ticket(cls, cr_wrapper, reason):
        return 'test_ticket'


@patch.object(
    TestProcess,
    '_stages_list',
    [
        Stage(TestTicketCreationMixinAction),
    ]
)
def test_ticket_mixin(session, cr_wrapper):
    process = TestProcess()

    instance = prepare_process_instance(cr_wrapper, process)
    with session.begin():
        session.add(instance)
        TestTicketCreationMixinAction.apply(session, instance)

    assert_that(
        instance.get_data_field(stage_name=TestTicketCreationMixinAction.get_lower_name(), field_name='ticket'),
        equal_to('test_ticket')
    )


@patch.object(
    TestProcess,
    '_stages_list',
    [
        Stage(HideCashAction),
    ]
)
def test_cash_hides_after_hide_cash_stage(session, cr_wrapper_postfiscal):
    cr_wrapper = cr_wrapper_postfiscal

    process = TestProcess()

    instance = prepare_process_instance(cr_wrapper, process)
    with session.begin():
        session.add(instance)
        HideCashAction.apply(session, instance)

    assert_that(cr_wrapper.cash_register.hidden, equal_to(True))


@patch.object(
    TestProcess,
    '_stages_list',
    [
        Stage(ClearDeviceReregistrationAction),
    ]
)
def test_clear_device_reregistration_action_calls_clear_device(
        session, cr_wrapper_fatal_error_after_change_fs, ws_mocks
):
    process = TestProcess()

    response = ws_mocks.cashmachines_clear_device_data(
        cr_long_sn=cr_wrapper_fatal_error_after_change_fs.whitespirit_key,
        json={"status": "ok"},
        content_type="application/json"
    )

    instance = prepare_process_instance(cr_wrapper_fatal_error_after_change_fs, process)
    with session.begin():
        session.add(instance)
        ClearDeviceReregistrationAction.apply(session, instance)

    assert_that(response.call_count, equal_to(1))


@patch.object(
    TestProcess,
    '_stages_list',
    [
        Stage(ConfigureAction),
    ]
)
def test_configure_action_calls_configure_and_reboot(
        session, cr_wrapper_with_ready_registration, ws_mocks
):
    process = TestProcess()

    response_configure = ws_mocks.cashmachines_configure(
        cr_long_sn=cr_wrapper_with_ready_registration.long_serial_number
    )

    response_reboot = ws_mocks.cashmachines_reboot(
        cr_long_sn=cr_wrapper_with_ready_registration.long_serial_number
    )

    instance = prepare_process_instance(cr_wrapper_with_ready_registration, process)
    with session.begin():
        session.add(instance)
        ConfigureAction.apply(session, instance)

    assert_that(response_configure.call_count, equal_to(1))
    assert_that(response_reboot.call_count, equal_to(1))


@patch.object(
    TestProcess,
    '_stages_list',
    [
        Stage(CreateApplicationIssueTicketForReregistrationAction),
    ]
)
@patch.object(TrackerClient, 'find_last_rereg_application_upload_issue', Mock(return_value=PropertyMock(key='ticket')))
def test_create_application_action_writes_ticket_on_skip(session, cr_wrapper_with_completed_registration):
    process = TestProcess()

    instance = prepare_process_instance(cr_wrapper_with_completed_registration, process)
    with session.begin():
        session.add(instance)
        CreateApplicationIssueTicketForReregistrationAction.apply_if_skipped(session, instance)

    assert_that(
         instance.get_data_field(
             stage_name=CreateApplicationIssueTicketForReregistrationAction.get_lower_name(),
             field_name=CreateApplicationIssueTicketForReregistrationAction.ticket_name
         ),
         equal_to('ticket')
    )


@patch.object(TrackerClient, 'find_last_rereg_application_upload_issue', Mock())
def test_get_rereg_application_upload_issue_key_with_ticket_in_process(session, cr_wrapper_with_test_process):
    cr_wrapper_with_test_process.current_process.data = {
        CreateApplicationIssueTicketForReregistrationAction.get_lower_name(): {'ticket': 'ticket_key'}
    }
    tracker_client = TrackerClient.from_app(getApplication())
    assert_that(
        get_rereg_application_upload_issue_key(cr_wrapper_with_test_process, tracker_client),
        equal_to('ticket_key')
    )
    tracker_client.find_last_rereg_application_upload_issue.assert_not_called()


@patch.object(
    TrackerClient, 'find_last_rereg_application_upload_issue',
    Mock(return_value=PropertyMock(key='ticket_key'))
)
def test_get_rereg_application_upload_issue_key_with_no_ticket_in_process(
        session, cr_wrapper_with_test_process_with_registration
):
    cr_wrapper_with_test_process_with_registration.current_process.data = {}
    tracker_client = TrackerClient.from_app(getApplication())
    assert_that(
        get_rereg_application_upload_issue_key(
            cr_wrapper_with_test_process_with_registration, tracker_client
        ),
        equal_to('ticket_key')
    )
    tracker_client.find_last_rereg_application_upload_issue.assert_called_once_with(
        cr_wrapper_with_test_process_with_registration.current_registration, fn_changed=True
    )


def test_get_rereg_application_upload_issue_key_with_no_process(session, cr_wrapper_with_completed_registration):
    tracker_client = TrackerClient.from_app(getApplication())
    with pytest.raises(MissingProcessError):
        get_rereg_application_upload_issue_key(cr_wrapper_with_completed_registration, tracker_client)


@pytest.mark.parametrize('address_changed', (True, False))
def test_get_info_for_application(
        session, cr_wrapper_with_test_process, get_rereg_application_upload_issue_key_fixture, address_changed,
):
    app_version = '228_337'
    cr_wrapper_with_test_process.current_process.data = {
        'config': {'fns_app_version': app_version, 'address_changed': address_changed}
    }
    ticket_key = 'ticket_key'
    get_rereg_application_upload_issue_key_fixture.return_value = ticket_key
    uploader = ReregistrationApplicationUploader(cr_wrapper_with_test_process.cash_register)
    assert_that(
        uploader.info_for_application,
        equal_to((ticket_key, app_version, address_changed, True))
    )
    get_rereg_application_upload_issue_key_fixture.assert_called_once_with(uploader.cr_wrapper, uploader.tracker_client,
                                                                           fn_changed=True)


def test_get_info_for_application_for_address_changed(
        session, cr_wrapper_with_test_process_with_registration, get_rereg_application_upload_issue_key_fixture
):
    app_version = '228_337'
    cr_wrapper_with_test_process_with_registration.current_process.data = {
        'config': {'fns_app_version': app_version, 'address_changed': False}
    }
    cr_wrapper_with_test_process_with_registration.current_registration.address.update_dt \
        = datetime.datetime.now() + datetime.timedelta(days=42)
    cr_wrapper_with_test_process_with_registration.current_registration.parent = \
        cr_wrapper_with_test_process_with_registration.current_registration
    ticket_key = 'ticket_key'
    get_rereg_application_upload_issue_key_fixture.return_value = ticket_key
    uploader = ReregistrationApplicationUploader(cr_wrapper_with_test_process_with_registration.cash_register)
    assert_that(
        uploader.info_for_application,
        equal_to((ticket_key, app_version, True, True))
    )
    get_rereg_application_upload_issue_key_fixture.assert_called_once_with(uploader.cr_wrapper, uploader.tracker_client,
                                                                           fn_changed=True)


@patch.object(ReregistrationApplicationUploader, 'info_for_application', Mock())
@patch.object(FnsregClient, 'generate_reregistration_application', Mock(return_value=FnsregTestResponse(200, 'content')))
def test_generate_reregistration_application_with_fnsreg_succeed_with_content(
        session, cr_wrapper_with_completed_registration
):
    app_version = '123'
    ReregistrationApplicationUploader.info_for_application = None, app_version, False, True
    fnsreg_client = FnsregClient.from_app(getApplication())
    content = ReregistrationApplicationUploader(
        cr_wrapper_with_completed_registration.cash_register
    ).create_application()

    assert_that(content, equal_to('content'))
    fnsreg_client.generate_reregistration_application.assert_called_once_with(
        cr_wrapper_with_completed_registration.current_registration, False, app_version, fn_changed=True
    )


@patch.object(ReregistrationApplicationUploader, 'info_for_application', Mock())
@patch.object(FnsregClient, 'generate_reregistration_application', Mock(return_value=FnsregTestResponse(500, '{}')))
def test_generate_reregistration_application_with_fnsreg_fails_on_bad_response_code(
        session, cr_wrapper_with_completed_registration
):
    ReregistrationApplicationUploader.info_for_application = 'ticket_key', '123', False, True
    with pytest.raises(ServerError):
        ReregistrationApplicationUploader(cr_wrapper_with_completed_registration.cash_register).create_application()


@patch.object(
    FnsregClient,
    'generate_reregistration_application', Mock(return_value=FnsregTestResponse(200, 'content'))
)
@patch.object(
    ReregistrationApplicationUploader,
    'info_for_application', Mock(return_value=('ticket_key', '123', False, True))
)
def test_get_attachment(
        session, cr_wrapper_with_test_process_with_registration
):
    ReregistrationApplicationUploader.info_for_application = 'ticket_key', '123', False, True
    with session.begin():
        attach = ReregistrationApplicationUploader(
            cr_wrapper_with_test_process_with_registration.cash_register
        ).create_application()
    assert_that(attach, equal_to('content'))


@patch.object(UploadMixin, 'apply_on_cash_register', Mock(return_value={'upload_info': 'some_info'}))
def test_upload_mixin_writes_upload_info(session, cr_wrapper_with_test_process):
    with session.begin():
        UploadMixin.apply(session, cr_wrapper_with_test_process.current_process)

    assert_that(
        cr_wrapper_with_test_process.current_process.get_data_field(TestProcess.initial_stage().name, 'upload_info'),
        equal_to('some_info')
    )


@patch.object(ReregistrationApplicationUploader, 'create_application', Mock())
@patch.object(ReregistrationApplicationUploader, 'upload_application_to_tracker', Mock())
@patch.object(DocumentsClient, 'upload_reregistration_application', Mock(return_value={'some_info': 'info'}))
def test_upload_to_s3_reregistration_application_action(
        session, cr_wrapper_with_test_process_with_registration
):
    content = 'content'
    ReregistrationApplicationUploader.create_application.return_value = content

    upload_info = CreateReregistrationApplicationAction.apply_on_cash_register(
        session, cr_wrapper_with_test_process_with_registration.cash_register,
        CreateReregistrationApplicationAction.reason_from_process_instance(
            cr_wrapper_with_test_process_with_registration.current_process
        )
    )['upload_info']

    assert_that(upload_info, equal_to({'some_info': 'info'}))

    doc = DocumentsClient.upload_reregistration_application.call_args.kwargs['doc']
    cr_sn = DocumentsClient.upload_reregistration_application.call_args.kwargs['cr_sn']
    fn_sn = DocumentsClient.upload_reregistration_application.call_args.kwargs['fn_sn']

    assert_that(doc.buf, equal_to(content))
    assert_that(cr_sn, equal_to(cr_wrapper_with_test_process_with_registration.serial_number))
    assert_that(fn_sn, equal_to(cr_wrapper_with_test_process_with_registration.fiscal_storage.serial_number))


@patch.object(ReregistrationApplicationUploader, 'create_application', Mock())
@patch.object(ReregistrationApplicationUploader, 'upload_application_to_tracker', Mock())
@patch.object(DocumentsClient, 'upload_reregistration_application', Mock(return_value={'some_info': 'info'}))
@patch('uuid.uuid4', Mock(return_value=UUID('11111111111111111111111111111111')))
def test_create_application_saves_uuid(
        session, cr_wrapper_with_test_process_with_registration
):
    test_uuid = str(UUID('11111111111111111111111111111111'))
    CreateReregistrationApplicationAction.apply_on_cash_register(
        session, cr_wrapper_with_test_process_with_registration.cash_register,
        CreateReregistrationApplicationAction.reason_from_process_instance(
            cr_wrapper_with_test_process_with_registration.current_process
        )
    )

    process_instance = cr_wrapper_with_test_process_with_registration.current_process
    assert_that(
        process_instance.get_data_field(process_instance.stage, 'reregistration_uuid'),
        equal_to(test_uuid)
    )


@patch.object(DocumentsClient, 'get_reregistration_application', Mock(return_value='content'))
@patch.object(
    FnsregClient, 'upload_reregistration_application',
    Mock(return_value=FnsregTestResponse(422, content=json.dumps({'error': 'REG_REQUEST_ALREADY_EXIST'})))
)
@patch('uuid.uuid4', Mock(return_value=UUID('11111111111111111111111111111111')))
def test_upload_to_fnsreg_reregistration_application_with_previously_loaded_uuid(
        session, cr_wrapper_with_test_process_with_registration, get_rereg_application_upload_issue_key_fixture
):
    test_uuid = str(UUID('12345678123456781234567812345678'))
    cr_wrapper_with_test_process_with_registration.current_process.data.setdefault(
        CreateReregistrationApplicationAction.get_lower_name(), {}
    )['reregistration_uuid'] = test_uuid
    get_rereg_application_upload_issue_key_fixture.return_value = 'ticket_key'
    upload_info = UploadToFnsregReregistrationApplicationAction.apply_on_cash_register(
        session, cr_wrapper_with_test_process_with_registration.cash_register,
        UploadToFnsregReregistrationApplicationAction.reason_from_process_instance(
            cr_wrapper_with_test_process_with_registration.current_process
        )
    )['upload_info']
    assert_that(upload_info['uuid'], equal_to(test_uuid))


@patch.object(DocumentsClient, 'get_reregistration_application', Mock(return_value='content'))
@patch.object(FnsregClient, 'upload_reregistration_application', Mock(return_value=FnsregTestResponse(200)))
@patch('uuid.uuid4', Mock(return_value=UUID('12345678123456781234567812345678')))
def test_upload_to_fnsreg_reregistration_application_action(session, cr_wrapper_with_test_process_with_registration):
    test_uuid = str(UUID('12345678123456781234567812345678'))
    process_instance = cr_wrapper_with_test_process_with_registration.current_process
    process_instance.data.setdefault(
        CREATE_REREGISTRATION_APPLICATION_ACTION_NAME.lower(), {}
    ).setdefault('upload_info', {})['date_stamp'] = '0'

    cr_wrapper_with_test_process_with_registration.current_process.data.setdefault(
        CreateReregistrationApplicationAction.get_lower_name(), {}
    )['reregistration_uuid'] = test_uuid

    upload_info = UploadToFnsregReregistrationApplicationAction.apply_on_cash_register(
        session, cr_wrapper_with_test_process_with_registration.cash_register,
        UploadToFnsregReregistrationApplicationAction.reason_from_process_instance(process_instance)
    )['upload_info']

    assert_that(upload_info, equal_to({'uuid': test_uuid}))
    FnsregClient.upload_reregistration_application.assert_called_once_with('content', registration_uuid=test_uuid)
    DocumentsClient.get_reregistration_application.assert_called_once_with(
        cr_wrapper_with_test_process_with_registration.serial_number,
        cr_wrapper_with_test_process_with_registration.fiscal_storage.serial_number,
        '0'
    )


@pytest.mark.parametrize(
    'action_apply, switched',
    [(TestSwitchingAction.apply_if_skipped, False), (TestSwitchingAction.apply, True)]
)
def test_switch_to_action_mixin_writes_switched_flag_to_process_data(
        session, cr_wrapper_with_test_process, action_apply, switched
):
    with session.begin():
        action_apply(session, cr_wrapper_with_test_process.current_process)

    assert_that(
        cr_wrapper_with_test_process.current_process.get_data_field(TestProcess.initial_stage().name, 'switched'),
        equal_to(switched)
    )


def test_reset_upload_info(session, cr_wrapper_with_test_process):
    upload_info = {'key': 'value'}
    instance = cr_wrapper_with_test_process.current_process
    stage_name = TestProcess.initial_stage().name
    with session.begin():
        instance.set_data_field_for_stage('upload_info', upload_info)

    SwitchToActionMixin.reset_upload_info(
        instance, stage_name,
    )

    assert_that(instance.get_data_field(stage_name, 'upload_info'), equal_to(None))


def test_retry_fnsreg_application_with_address_changed(session, cr_wrapper_with_test_process):
    with patch(
        'yb_darkspirit.core.cash_register.maintenance_actions._get_cr_base_info',
        return_value=prepare_info_for_rejection_code(
            session, cr_wrapper_with_test_process, 'rejected', "32"
        )
    ):
        instance = cr_wrapper_with_test_process.current_process
        stage_names = [
            CreateReregistrationApplicationAction.get_lower_name(),
            UploadToFnsregReregistrationApplicationAction.get_lower_name()
        ]
        retry_fnsreg_with_stages(session, instance, stage_names)

        assert_that(
            instance.get_data_field(
                'config', 'address_changed'
            ),
            equal_to(True)
        )


def retry_fnsreg_with_stages(session, instance, stage_names):
    upload_info = {'key': 'value'}
    with session.begin():
        for stage_name in stage_names:
            instance.data.setdefault(stage_name, {})['upload_info'] = upload_info

    with session.begin():
        RetryReregApplicationAction.apply(session, instance)

    for stage_name in stage_names:
        assert_that(instance.get_data_field(stage_name, 'upload_info'), equal_to(None))

    assert_that(instance.get_data_field(TestProcess.initial_stage().name, 'switched'), equal_to(True))


def test_form_final_group_list_forms_new_groups(session, cr_wrapper_with_test_process):
    groups = ['SOME_GROUP']
    run_form_final_group_list_with_assertion(cr_wrapper_with_test_process, True, groups, groups)


def test_form_final_group_list_with_empty_input_forms_nogroup(session, cr_wrapper_with_test_process):
    groups = []
    run_form_final_group_list_with_assertion(cr_wrapper_with_test_process, True, groups, [scheme.Groups.NO_GROUP.value])


def test_form_final_group_list_with_no_rewrite_appends_to_current_groups(session, cr_wrapper_with_test_process):
    old_groups = cr_wrapper_with_test_process.cash_register.current_groups.split(',')
    groups = ['SOME_GROUP']
    run_form_final_group_list_with_assertion(cr_wrapper_with_test_process, False, groups, old_groups + groups)


def test_form_final_group_list_no_rewrite_with_nogroup_removes_nogroup(session, cr_wrapper_with_test_process):
    cr_wrapper_with_test_process.cash_register.current_groups = scheme.Groups.NO_GROUP.value
    groups = ['SOME_GROUP']
    run_form_final_group_list_with_assertion(cr_wrapper_with_test_process, False, groups, groups)


@patch.object(CashRegister, 'configure_group', Mock())
def test_write_groups_write_groups(session, cr_wrapper, ws_mocks):
    groups = ['SOME_GROUP', 'ANOTHER_GROUP']
    UpdateGroupsAction.write_groups(session, cr_wrapper, groups)
    cr_wrapper.configure_group.assert_called_once_with(groups)
    assert_that(cr_wrapper.cash_register.target_groups, equal_to('SOME_GROUP,ANOTHER_GROUP'))


@pytest.mark.parametrize('action', [
    CreateIssueTicketForRegistrationReportAction, CreateApplicationIssueTicketForRegistrationAction
])
@patch.object(TrackerClient, '_create_application_upload_issue', Mock(return_value=PropertyMock(key='ticket_key')))
@patch.object(RegistrationConfigSchema, 'load_from_process', Mock(
    return_value=PropertyMock(inn_fns_api_not_available_list=['7704340310'])
))
@patch.object(TrackerClient, 'acquire_application_upload_ticket', Mock())
def test_create_reg_issue_with_fns_unavailable(session, cr_wrapper_with_completed_registration, action):
    key = action.get_or_create_ticket(cr_wrapper_with_completed_registration, '')

    assert_that(key, equal_to('ticket_key'))
    TrackerClient.acquire_application_upload_ticket.assert_called_with(key=key, fns_automation=False)
    TrackerClient._create_application_upload_issue.assert_called()


@pytest.mark.parametrize('action', [
    CreateIssueTicketForRegistrationReportAction, CreateApplicationIssueTicketForRegistrationAction
])
@patch.object(TrackerClient, '_create_application_upload_issue', Mock(return_value=PropertyMock(key='ticket_key')))
@patch.object(RegistrationConfigSchema, 'load_from_process', Mock(
    return_value=PropertyMock(inn_fns_api_not_available_list=[])
))
@patch.object(TrackerClient, 'acquire_application_upload_ticket', Mock())
def test_create_reg_issue_with_fns_available(session, cr_wrapper_with_completed_registration, action):
    key = action.get_or_create_ticket(cr_wrapper_with_completed_registration, '')

    assert_that(key, equal_to('ticket_key'))
    TrackerClient.acquire_application_upload_ticket.assert_called_with(key=key, fns_automation=True)
    TrackerClient._create_application_upload_issue.assert_called()


@pytest.mark.parametrize('action', [
    CreateIssueTicketForRegistrationReportAction, CreateApplicationIssueTicketForRegistrationAction
])
@patch.object(TrackerClient, '_find_issue_by_tags', Mock(return_value=PropertyMock(key='ticket_key')))
def test_create_reg_skipped(session, cr_wrapper_with_test_process_with_registration, action):
    process = cr_wrapper_with_test_process_with_registration.current_process
    action.apply_if_skipped(session, process)
    assert_that(
        process.get_data_field(process.stage, action.ticket_name),
        equal_to('ticket_key')
    )


@patch.object(RegistrationConfigSchema, 'load_from_process', Mock(
    return_value=PropertyMock(firm_inn=u'7704340310', ofd_inn=scheme.YANDEX_OFD_INN, is_bso_kkt=False)
))
def test_new_registration(session, cr_wrapper_with_test_process, ws_mocks):
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper_with_test_process.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD
    )
    with session.begin():
        NewRegistrationAction.apply_on_cash_register(session, cr_wrapper_with_test_process.cash_register, '')
    assert_that(cr_wrapper_with_test_process.current_registration.state, equal_to(scheme.REGISTRATION_NEW))
    assert_that(cr_wrapper_with_test_process.current_registration.firm_inn, equal_to('7704340310'))
    assert_that(cr_wrapper_with_test_process.current_registration.ofd_inn, equal_to(scheme.YANDEX_OFD_INN))
    assert_that(cr_wrapper_with_test_process.current_registration.is_bso_kkt, equal_to(False))


@patch.object(DocumentsClient, 'upload_registration_application', Mock(return_value={'url': 'url'}))
@patch.object(TrackerClient, 'attach_file', Mock())
@patch.object(FnsregClient, 'generate_registration_application', Mock(return_value=FnsregTestResponse(200, 'attach')))
@patch.object(TrackerClient, 'find_last_reg_application_upload_issue', Mock(return_value=PropertyMock(key='issue')))
@patch.object(RegistrationConfigSchema, 'load_from_process', Mock(return_value=PropertyMock(fns_app_version='V505')))
@patch('uuid.uuid4', Mock(return_value=UUID('11111111111111111111111111111111')))
def test_create_reg_application(session, cr_wrapper_with_test_process):
    CreateRegistrationApplicationAction.apply_on_cash_register(session, cr_wrapper_with_test_process.cash_register, '')

    test_uuid = str(UUID('11111111111111111111111111111111'))
    assert_that(FnsregClient.generate_registration_application.call_args.args[1], equal_to('V505'))
    _check_application_clients_calls(
        cr_wrapper_with_test_process,
        TrackerClient.attach_file.call_args.kwargs, DocumentsClient.upload_registration_application.call_args.kwargs
    )

    process = cr_wrapper_with_test_process.current_process
    assert_that(process.get_data_field(process.stage, REGISTRATION_UUID_FIELD), equal_to(test_uuid))


@patch.object(DocumentsClient, 'upload_registration_report_application', Mock(return_value={'url': 'url'}))
@patch.object(TrackerClient, 'attach_file', Mock())
@patch.object(FnsregClient, 'generate_registration_report_application', Mock(return_value=FnsregTestResponse(200, 'attach')))
@patch.object(TrackerClient, 'find_last_reg_report_upload_issue', Mock(return_value=PropertyMock(key='issue')))
@patch('uuid.uuid4', Mock(return_value=UUID('11111111111111111111111111111111')))
def test_create_reg_report_application(session, cr_wrapper_with_test_process):
    CreateRegistrationReportAction.apply_on_cash_register(session, cr_wrapper_with_test_process.cash_register, '')

    test_uuid = str(UUID('11111111111111111111111111111111'))
    _check_application_clients_calls(
        cr_wrapper_with_test_process, TrackerClient.attach_file.call_args.kwargs,
        DocumentsClient.upload_registration_report_application.call_args.kwargs
    )

    process = cr_wrapper_with_test_process.current_process
    assert_that(process.get_data_field(process.stage, REGISTRATION_REPORT_UUID_FIELD), equal_to(test_uuid))


@patch.object(FnsregClient, 'upload_registration_application', Mock(return_value=FnsregTestResponse(200)))
@patch.object(DocumentsClient, 'get_registration_application', Mock(return_value='attach'))
def test_upload_fnsreg_reg_application(session, cr_wrapper_with_test_process):
    test_uuid = str(UUID('11111111111111111111111111111111'))
    test_stamp = '2022-07-23'
    process = cr_wrapper_with_test_process.current_process
    process.data.setdefault(CREATE_REGISTRATION_APPLICATION_ACTION_NAME.lower(), {})[REGISTRATION_UUID_FIELD] = test_uuid
    process.data[CREATE_REGISTRATION_APPLICATION_ACTION_NAME.lower()].setdefault('upload_info', {})['date_stamp'] = test_stamp

    result = UploadToFnsregRegistrationApplicationAction.apply_on_cash_register(
        session, cr_wrapper_with_test_process.cash_register, ''
    )
    assert_that(result['upload_info']['uuid'], equal_to(test_uuid))
    DocumentsClient.get_registration_application.assert_called_with(
        cr_wrapper_with_test_process.serial_number, cr_wrapper_with_test_process.fiscal_storage.serial_number, test_stamp
    )
    FnsregClient.upload_registration_application.assert_called_with('attach', registration_uuid=test_uuid)


@patch.object(FnsregClient, 'upload_registration_report_application', Mock(return_value=FnsregTestResponse(200)))
@patch.object(DocumentsClient, 'get_registration_report', Mock(return_value='attach'))
@patch.object(RegistrationConfigSchema, 'load_from_process', Mock(return_value=PropertyMock(enable_kkt_validation=False)))
def test_upload_fnsreg_reg_report_application(session, cr_wrapper_with_test_process):
    test_uuid = str(UUID('11111111111111111111111111111111'))
    test_stamp = '2022-07-23'
    process = cr_wrapper_with_test_process.current_process
    process.data.setdefault(CREATE_REGISTRATION_REPORT_ACTION_NAME.lower(), {})[REGISTRATION_REPORT_UUID_FIELD] = test_uuid
    process.data[CREATE_REGISTRATION_REPORT_ACTION_NAME.lower()].setdefault('upload_info', {})['date_stamp'] = test_stamp

    result = UploadToFnsregRegistrationReportAction.apply_on_cash_register(
        session, cr_wrapper_with_test_process.cash_register, ''
    )
    assert_that(result['upload_info']['uuid'], equal_to(test_uuid))
    DocumentsClient.get_registration_report.assert_called_with(
        cr_wrapper_with_test_process.serial_number, cr_wrapper_with_test_process.fiscal_storage.serial_number, test_stamp
    )
    FnsregClient.upload_registration_report_application.assert_called_with(
        'attach', test_uuid, cr_wrapper_with_test_process.serial_number,
        cr_wrapper_with_test_process.cash_register.type.description, False
    )


@patch.object(FnsregClient, 'get_registration_status', Mock(
    return_value=FnsregTestResponse(200, content='{{"documentBase64":"{}"}}'.format(base64.b64encode('content')))
))
@patch.object(DocumentsClient, 'upload_registration_card', Mock(return_value='upload_info'))
def test_upload_to_s3_reg_card(session, cr_wrapper_with_test_process):
    test_uuid = str(UUID('11111111111111111111111111111111'))
    process = cr_wrapper_with_test_process.current_process
    process.data.setdefault(CREATE_REGISTRATION_APPLICATION_ACTION_NAME.lower(), {})[
        REGISTRATION_UUID_FIELD] = test_uuid
    result = UploadToS3RegistrationCardAction.apply_on_cash_register(
        session, cr_wrapper_with_test_process.cash_register, ''
    )
    _check_upload_to_s3_card_client_calls(
        cr_wrapper_with_test_process, test_uuid, DocumentsClient.upload_registration_card.call_args.args,
        FnsregClient.get_registration_status.call_args.args
    )
    assert_that(result['upload_info'], equal_to('upload_info'))


@patch.object(FnsregClient, 'get_registration_report_status', Mock(
    return_value=FnsregTestResponse(200, content='{{"documentBase64":"{}"}}'.format(base64.b64encode('content')))
))
@patch.object(DocumentsClient, 'upload_registration_report_card', Mock(return_value='upload_info'))
def test_upload_to_s3_reg_report_card(session, cr_wrapper_with_test_process):
    test_uuid = str(UUID('11111111111111111111111111111111'))
    process = cr_wrapper_with_test_process.current_process
    process.data.setdefault(CREATE_REGISTRATION_REPORT_ACTION_NAME.lower(), {})[
        REGISTRATION_REPORT_UUID_FIELD] = test_uuid
    result = UploadToS3RegistrationReportCardAction.apply_on_cash_register(
        session, cr_wrapper_with_test_process.cash_register, ''
    )
    _check_upload_to_s3_card_client_calls(
        cr_wrapper_with_test_process, test_uuid, DocumentsClient.upload_registration_report_card.call_args.args,
        FnsregClient.get_registration_report_status.call_args.args
    )
    assert_that(result['upload_info'], equal_to('upload_info'))


def _check_upload_to_s3_card_client_calls(cr_wrapper, test_uuid, s3client_args, fnsregclient_args):
    # type: (CashRegister, str, list, list) -> None
    assert_that(s3client_args[0], equal_to(cr_wrapper.serial_number))
    assert_that(s3client_args[1], equal_to(cr_wrapper.fiscal_storage.serial_number))
    assert_that(s3client_args[2].getvalue(), equal_to('content'))
    assert_that(fnsregclient_args[0], equal_to(test_uuid))


def _check_application_clients_calls(cr_wrapper, tracker_attach_kwargs, s3_upload_kwargs):
    # type: (CashRegister, dict, dict) -> None
    assert_that(tracker_attach_kwargs['issue_key'], equal_to('issue'))
    assert_that(tracker_attach_kwargs['attachment'].buf, equal_to('attach'))
    assert_that(tracker_attach_kwargs['comment'], contains_string('url'))
    assert_that(s3_upload_kwargs['cr_sn'], equal_to(cr_wrapper.serial_number))
    assert_that(s3_upload_kwargs['fn_sn'], equal_to(cr_wrapper.fiscal_storage.serial_number))


def run_form_final_group_list_with_assertion(cr_wrapper, rewrite_groups, groups, expected):
    final_groups = UpdateGroupsAction.form_final_group_list(cr_wrapper, groups, rewrite_groups)

    assert_that(set(final_groups), equal_to(set(expected)))
