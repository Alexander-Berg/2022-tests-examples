import json

from butils.application import getApplication
from hamcrest import assert_that, equal_to
from marshmallow import fields
from mock import Mock, patch, PropertyMock

import pytest

from tests import ws_responses
from tests.processes_testing_instances import TestProcess, FnsregTestResponse, prepare_info_for_rejection_code
from yb_darkspirit import scheme
from yb_darkspirit.application import Environment
from yb_darkspirit.application.plugins.dbhelper import Session
from yb_darkspirit.core.cash_register import UPLOAD_TO_FNSREG_REREGISTRATION_APPLICATION_ACTION_NAME, \
    CREATE_REREGISTRATION_APPLICATION_ACTION_NAME, REREGISTRATION_UUID_FIELD, REGISTRATION_UUID_FIELD, \
    CREATE_REGISTRATION_APPLICATION_ACTION_NAME, CREATE_REGISTRATION_REPORT_ACTION_NAME, REGISTRATION_REPORT_UUID_FIELD
from yb_darkspirit.core.cash_register.state_checks import (
    CashRegisterBaseInfo, Checks, is_upload_done_in_process,
    is_expected_rereg_rejection_code,
    has_fr_partition, has_groups_from_process_config, has_valid_config, is_fnsreg_status_not_in_process,
    is_fnsreg_status_not_fnsapi_unavailable, is_fnsreg_need_retry
)
from yb_darkspirit.interactions import OfdPrivateClient, TrackerClient, FnsregClient, DocumentsClient
from yb_darkspirit.interface import CashRegister
from yb_darkspirit.process.schemas import StrictSchema


@pytest.mark.parametrize('ws_spaces_info,has_fr_expected', [
    (ws_responses.CASH_SPACE_INFO_WITH_FR_PATCH, True),
    (ws_responses.CASH_SPACE_INFO_WITHOUT_FR_PATCH, False),
])
def test_cash_has_fr_partition_check(session, cr_wrapper, ws_mocks, ws_spaces_info, has_fr_expected):
    ws_mocks.cashmachines_status(
        cr_wrapper.long_serial_number,
        body=json.dumps(ws_responses.CASHMACHINES_STATUS_CR_OVERDUE_OPEN_SHIFT_FS_FISCAL_GOOD)
    )
    ws_mocks.cashmachines_space_info(
        cr_wrapper.long_serial_number,
        use_password=True,
        body=json.dumps(ws_spaces_info),
        status=200,
    )
    ofd_private_client = Mock()
    tracker_client = Mock()
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, ofd_private_client, tracker_client, Mock())
    assert has_fr_partition(info).is_success() == has_fr_expected


@pytest.mark.parametrize('success', [True, False])
def test_check_rereg_fns_send_status_success(session, cr_wrapper_with_completed_registration, success):
    environment = Environment(getApplication().get_current_env_type())
    ofd_private_client = OfdPrivateClient(environment)
    with patch.object(ofd_private_client, 'check_document_status') as check_doc_mock:
        info = CashRegisterBaseInfo(cr_wrapper_with_completed_registration.serial_number, session, ofd_private_client,
                                    Mock(), Mock())
        check_doc_mock.return_value = [{'success': success}]

        assert_that(Checks.FNS_SEND_STATUS_SUCCESS(info).is_success(), equal_to(success))
        check_doc_mock.assert_called_once_with(
            cr_wrapper_with_completed_registration.current_registration.registration_number,
            cr_wrapper_with_completed_registration.fiscal_storage.serial_number,
            cr_wrapper_with_completed_registration.last_document_number
        )


@pytest.mark.parametrize('stage_name, mock_func_name, check', [
        (CREATE_REREGISTRATION_APPLICATION_ACTION_NAME.lower(), 'get_reregistration_application', Checks.REREGISTRATION_APPLICATION_XML_FOUND),
        (CREATE_REGISTRATION_APPLICATION_ACTION_NAME.lower(), 'get_registration_application', Checks.REGISTRATION_APPLICATION_XML_FOUND),
        (CREATE_REGISTRATION_REPORT_ACTION_NAME.lower(), 'get_registration_report', Checks.REGISTRATION_REPORT_XML_FOUND),
])
def test_check_application_xml_found(
    session, cr_wrapper_with_test_process_with_registration, stage_name, mock_func_name, check
):
    with patch.object(DocumentsClient, mock_func_name, Mock(return_value='attachment')):
        app = getApplication()
        info = CashRegisterBaseInfo(cr_wrapper_with_test_process_with_registration.serial_number, session, Mock(),
                                    TrackerClient.from_app(app), DocumentsClient.from_app(app))
        info.cr_wrapper.current_process.data.setdefault(
            stage_name, {}
        ).setdefault('upload_info', {})['date_stamp'] = '0'
        assert_that(check(info).is_success(), equal_to(True))
        info._documents_client.__getattribute__(mock_func_name).assert_called_with(
            cr_wrapper_with_test_process_with_registration.serial_number,
            cr_wrapper_with_test_process_with_registration.fiscal_storage.serial_number,
            '0'
        )


@pytest.mark.parametrize('mock_func_name, check', [
        ('get_reregistration_application', Checks.REREGISTRATION_APPLICATION_XML_FOUND),
        ('get_registration_application', Checks.REGISTRATION_APPLICATION_XML_FOUND),
        ('get_registration_report', Checks.REGISTRATION_REPORT_XML_FOUND),
        ('get_registration_card', Checks.REGISTRATION_CARD_FOUND),
])
def test_check_application_xml_not_found(session, cr_wrapper_with_completed_registration, mock_func_name, check):
    app = getApplication()
    with patch.object(DocumentsClient, mock_func_name, Mock(side_effect=DocumentsClient.from_app(app).s3_client.exceptions.NoSuchKey)):
        info = CashRegisterBaseInfo(cr_wrapper_with_completed_registration.serial_number, session, Mock(),
                                    TrackerClient.from_app(app), DocumentsClient.from_app(app))
        assert_that(check(info).is_success(), equal_to(False))


def test_cash_register_in_process_succeed_with_process(session, cr_wrapper_with_test_process):
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.CASH_REGISTER_IN_PROCESS(info).is_success(), equal_to(True))


def test_cash_register_in_process_fails_without_process(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.CASH_REGISTER_IN_PROCESS(info).is_success(), equal_to(False))


def test_upload_done_in_process_fails_without_process(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(is_upload_done_in_process(info, stage_name='').is_success(), equal_to(False))


@pytest.mark.parametrize(
    'result, data', [
        (False, {}), (False, {TestProcess.initial_stage().name: {}}),
        (True, {TestProcess.initial_stage().name: {'upload_info': 'some_info'}})
    ]
)
def test_upload_done_in_process_with_process(session, cr_wrapper_with_test_process, result, data):
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())
    with session.begin():
        cr_wrapper_with_test_process.current_process.data = data
    assert_that(
        is_upload_done_in_process(info, stage_name=TestProcess.initial_stage().name).is_success(),
        equal_to(result)
    )


def test_rereg_status_success_fails_without_process(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.FNSREG_REREGISTRATION_STATUS_SUCCESS(info).is_success(), equal_to(False))


def test_fnsreg_reregistration_status_response_from_stage_returns_none_without_process(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(info.fnsreg_reregistration_status, equal_to(None))


def test_reg_report_status_success_fails_without_process(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.FNSREG_REGISTRATION_REPORT_STATUS_SUCCESS(info).is_success(), equal_to(False))


def test_reg_report_status_response_from_stage_returns_none_without_process(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(info.fnsreg_registration_report_status, equal_to(None))


def test_reg_status_success_fails_without_process(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.FNSREG_REGISTRATION_STATUS_SUCCESS(info).is_success(), equal_to(False))


def test_reg_status_response_from_stage_returns_none_without_process(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(info.fnsreg_registration_status, equal_to(None))


@pytest.mark.parametrize(
    'data', [{}, {}, {TestProcess.initial_stage().name: {'upload_info': {}}}]
)
def test_fnsreg_reregistration_status_returns_none_on_no_rereg_uuid(
        session, cr_wrapper_with_test_process, data,
):
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())
    with session.begin():
        cr_wrapper_with_test_process.current_process.stage = UPLOAD_TO_FNSREG_REREGISTRATION_APPLICATION_ACTION_NAME.lower()
        cr_wrapper_with_test_process.current_process.data = data
    assert_that(info.fnsreg_reregistration_status, equal_to(None))


@patch.object(FnsregClient, 'get_reregistration_status', Mock())
def test_fnsreg_reregistration_status_gets_response(session, cr_wrapper_with_test_process):
    response = FnsregTestResponse(200)
    test_uuid = '123'
    FnsregClient.get_reregistration_status.return_value = response
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REREGISTRATION_APPLICATION_ACTION_NAME.lower(),
        REREGISTRATION_UUID_FIELD, test_uuid
    )

    assert_that(info.fnsreg_reregistration_status, equal_to(response))


@patch.object(FnsregClient, 'get_registration_status', Mock())
def test_fnsreg_registration_status_gets_response(session, cr_wrapper_with_test_process):
    response = FnsregTestResponse(200)
    test_uuid = '123'
    FnsregClient.get_registration_status.return_value = response
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REGISTRATION_APPLICATION_ACTION_NAME.lower(),
        REGISTRATION_UUID_FIELD, test_uuid
    )

    assert_that(info.fnsreg_registration_status, equal_to(response))


@patch.object(FnsregClient, 'get_registration_report_status', Mock())
def test_fnsreg_registration_report_status_gets_response(session, cr_wrapper_with_test_process):
    response = FnsregTestResponse(200)
    test_uuid = '123'
    FnsregClient.get_registration_report_status.return_value = response
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REGISTRATION_REPORT_ACTION_NAME.lower(),
        REGISTRATION_REPORT_UUID_FIELD, test_uuid
    )

    assert_that(info.fnsreg_registration_report_status, equal_to(response))


@patch.object(FnsregClient, 'get_reregistration_status', Mock())
def test_fnsreg_reregistration_status_gets_response(session, cr_wrapper_with_test_process):
    response = FnsregTestResponse(200)
    test_uuid = '123'
    FnsregClient.get_reregistration_status.return_value = response
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REREGISTRATION_APPLICATION_ACTION_NAME.lower(),
        REREGISTRATION_UUID_FIELD, test_uuid
    )

    assert_that(info.fnsreg_reregistration_status, equal_to(response))


@patch.object(FnsregClient, 'get_reregistration_status', Mock(return_value=FnsregTestResponse(500)))
def test_rereg_status_fails_with_bad_rereg_response(session, cr_wrapper_with_test_process):
    test_uuid = '123'
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REREGISTRATION_APPLICATION_ACTION_NAME.lower(),
        REREGISTRATION_UUID_FIELD, test_uuid
    )

    assert_that(Checks.FNSREG_REREGISTRATION_STATUS_SUCCESS(info).is_success(), equal_to(False))
    info._fnsreg_client.get_reregistration_status.assert_called_once_with(test_uuid)


@patch.object(FnsregClient, 'get_registration_status', Mock(return_value=FnsregTestResponse(500)))
def test_reg_status_fails_with_bad_response(session, cr_wrapper_with_test_process):
    test_uuid = '123'
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REGISTRATION_APPLICATION_ACTION_NAME.lower(),
        REGISTRATION_UUID_FIELD, test_uuid
    )

    assert_that(Checks.FNSREG_REGISTRATION_STATUS_SUCCESS(info).is_success(), equal_to(False))
    info._fnsreg_client.get_registration_status.assert_called_once_with(test_uuid)


@patch.object(FnsregClient, 'get_registration_report_status', Mock(return_value=FnsregTestResponse(500)))
def test_reg_report_status_fails_with_bad_response(session, cr_wrapper_with_test_process):
    test_uuid = '123'
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REGISTRATION_REPORT_ACTION_NAME.lower(),
        REGISTRATION_REPORT_UUID_FIELD, test_uuid
    )

    assert_that(Checks.FNSREG_REGISTRATION_REPORT_STATUS_SUCCESS(info).is_success(), equal_to(False))
    info._fnsreg_client.get_registration_report_status.assert_called_once_with(test_uuid)


@patch.object(
    FnsregClient, 'get_reregistration_status',
    Mock(return_value=FnsregTestResponse(200, content=json.dumps({'status': 'success'})))
)
def test_rereg_status_success_succeed(session, cr_wrapper_with_test_process):
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REREGISTRATION_APPLICATION_ACTION_NAME.lower(),
        REREGISTRATION_UUID_FIELD, '123'
    )
    assert_that(Checks.FNSREG_REREGISTRATION_STATUS_SUCCESS(info).is_success(), equal_to(True))


@patch.object(
    FnsregClient, 'get_registration_status',
    Mock(return_value=FnsregTestResponse(200, content=json.dumps({'status': 'success'})))
)
def test_reg_status_success_succeed(session, cr_wrapper_with_test_process):
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REGISTRATION_APPLICATION_ACTION_NAME.lower(),
        REGISTRATION_UUID_FIELD, '123'
    )
    assert_that(Checks.FNSREG_REGISTRATION_STATUS_SUCCESS(info).is_success(), equal_to(True))


@patch.object(
    FnsregClient, 'get_registration_report_status',
    Mock(return_value=FnsregTestResponse(200, content=json.dumps({'status': 'success'})))
)
def test_reg_report_status_success_succeed(session, cr_wrapper_with_test_process):
    info = _get_info_for_fnsreg_status(
        session, cr_wrapper_with_test_process, CREATE_REGISTRATION_REPORT_ACTION_NAME.lower(),
        REGISTRATION_REPORT_UUID_FIELD, '123'
    )
    assert_that(Checks.FNSREG_REGISTRATION_REPORT_STATUS_SUCCESS(info).is_success(), equal_to(True))


def _prepare_info_for_status_tests(status_mock, session, cr_wrapper, error, status, stage_name, uuid_field, test_uuid):
    process = cr_wrapper.current_process
    process.data.setdefault(stage_name, {})[uuid_field] = test_uuid
    response = FnsregTestResponse(500 if error else 200, content=json.dumps({'status': status, 'error': error}))
    status_mock.return_value = response
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock(),
                                FnsregClient.from_app(getApplication()))
    return info


@pytest.mark.parametrize(
    'status_method', ('fnsreg_registration_status', 'fnsreg_registration_report_status', 'fnsreg_reregistration_status')
)
def test_fnsreg_status_none_without_uuid(session, cr_wrapper_with_test_process, fnsreg_get_status_mock, status_method):
    info = _prepare_info_for_status_tests(
        fnsreg_get_status_mock, session, cr_wrapper_with_test_process, None, 'blabla', '', '', None
    )

    assert_that(info.__getattribute__(status_method), equal_to(None))


def test_rereg_status_returns_status(session, cr_wrapper_with_test_process, fnsreg_get_status_mock):
    status = 'yes'
    test_uuid = 'uuid'
    info = _prepare_info_for_status_tests(
        fnsreg_get_status_mock, session, cr_wrapper_with_test_process, None, status,
        CREATE_REREGISTRATION_APPLICATION_ACTION_NAME.lower(), REREGISTRATION_UUID_FIELD, test_uuid
    )

    assert_that(info.fnsreg_reregistration_status.json()['status'], equal_to(status))
    fnsreg_get_status_mock.assert_called_once_with(test_uuid, FnsregClient.REREGISTRATION)


def test_reg_status_returns_status(session, cr_wrapper_with_test_process, fnsreg_get_status_mock):
    status = 'yes'
    test_uuid = 'uuid'
    info = _prepare_info_for_status_tests(
        fnsreg_get_status_mock, session, cr_wrapper_with_test_process, None, status,
        CREATE_REGISTRATION_APPLICATION_ACTION_NAME.lower(), REGISTRATION_UUID_FIELD, test_uuid
    )

    assert_that(info.fnsreg_registration_status.json()['status'], equal_to(status))
    fnsreg_get_status_mock.assert_called_once_with(test_uuid, FnsregClient.REGISTRATION)


def test_reg_report_status_returns_status(session, cr_wrapper_with_test_process, fnsreg_get_status_mock):
    status = 'yes'
    test_uuid = 'uuid'
    info = _prepare_info_for_status_tests(
        fnsreg_get_status_mock, session, cr_wrapper_with_test_process, None, status,
        CREATE_REGISTRATION_REPORT_ACTION_NAME.lower(), REGISTRATION_REPORT_UUID_FIELD, test_uuid
    )

    assert_that(info.fnsreg_registration_report_status.json()['status'], equal_to(status))
    fnsreg_get_status_mock.assert_called_once_with(test_uuid, FnsregClient.REGISTRATION_REPORT)


def test_fnsreg_status_not_in_process_succeeds(session, cr_wrapper_with_test_process):
    response = FnsregTestResponse(200, content=json.dumps({'status': 'not_in_process'}))
    assert_that(is_fnsreg_status_not_in_process(response).is_success(), equal_to(True))


def test_fnsreg_status_not_in_process_fails(session, cr_wrapper_with_test_process):
    response = FnsregTestResponse(200, content=json.dumps({'status': 'in_process'}))
    assert_that(is_fnsreg_status_not_in_process(response).is_success(), equal_to(False))


@pytest.mark.parametrize('checker', (is_fnsreg_status_not_in_process, is_fnsreg_status_not_fnsapi_unavailable))
def test_fnsreg_status_checkers_fail_on_none(session, cr_wrapper_with_test_process, checker):
    assert_that(checker(None).is_success(), equal_to(False))


def test_fnsreg_status_not_fnsapi_unavailable_fails(session, cr_wrapper_with_test_process):
    response = FnsregTestResponse(500, content=json.dumps({'error': 'FNS_API_UNAVAILABLE'}))
    assert_that(is_fnsreg_status_not_fnsapi_unavailable(response).is_success(), equal_to(False))


@pytest.mark.parametrize('error, status', [(None, 'rejected'), ('RETRYABLE_FNS_API_ERROR', None)])
@pytest.mark.parametrize('checker', (is_fnsreg_status_not_in_process, is_fnsreg_status_not_fnsapi_unavailable))
def test_fnsreg_status_checker_succeed(
        session, cr_wrapper_with_test_process, fnsreg_get_status_mock, error, status, checker
):
    response = FnsregTestResponse(500 if error else 200, content=json.dumps({'status': status, 'error': error}))

    assert_that(checker(response).is_success(), equal_to(True))


@pytest.mark.parametrize(
    'result, inn_fns_api_not_available_list', [
        (False, ['7704340310']), (True, ['000000']), (True, []), (True, None),
    ]
)
def test_reregistration_with_fnsapi_available_fails_if_inn_in_config(
        session, cr_wrapper_with_test_process_with_registration, result, inn_fns_api_not_available_list,
):
    with session.begin():
        cr_wrapper_with_test_process_with_registration.current_process.data['config']\
            ['inn_fns_api_not_available_list'] = inn_fns_api_not_available_list

    info = CashRegisterBaseInfo(cr_wrapper_with_test_process_with_registration.serial_number, session, Mock(), Mock(), Mock())

    assert_that(Checks.NOT_IN_FNS_API_AVAILABLE_LIST(info).is_success(), equal_to(result))


def test_rereg_rejected_code_fails_on_non_rejected(session, cr_wrapper_with_test_process):
    test_code = "23"
    info = prepare_info_for_rejection_code(session, cr_wrapper_with_test_process, 'success', test_code)

    assert_that(is_expected_rereg_rejection_code(info, expected=test_code).is_success(), equal_to(False))


def test_rereg_rejected_code_fails_on_wrong_code(session, cr_wrapper_with_test_process):
    test_code = "23"
    info = prepare_info_for_rejection_code(session, cr_wrapper_with_test_process, 'rejected', test_code + "1")

    assert_that(
        is_expected_rereg_rejection_code(info, expected=test_code).is_success(), equal_to(False)
    )


def test_rereg_rejected_code_succeeds(session, cr_wrapper_with_test_process):
    test_code = "23"
    info = prepare_info_for_rejection_code(session, cr_wrapper_with_test_process, 'rejected', test_code)

    assert_that(
        is_expected_rereg_rejection_code(info, expected=test_code).is_success(), equal_to(True)
    )


def test_fnsreg_need_retry_fails_on_non_retry_error_message(
        session, cr_wrapper_with_test_process,
):
    response = FnsregTestResponse(500, content=json.dumps({}))
    assert_that(is_fnsreg_need_retry(response).is_success(), equal_to(False))


def test_fnsreg_need_retry_succeeds(
        session, cr_wrapper_with_test_process,
):
    response = FnsregTestResponse(500, content=json.dumps({'error': 'RETRYABLE_FNS_API_ERROR'}))

    assert_that(is_fnsreg_need_retry(response).is_success(), equal_to(True))


def test_has_groups_from_process_config_with_rewrite_fails(session, cr_wrapper_with_test_process):
    groups = ['SOME_OTHER_GROUP']

    process = cr_wrapper_with_test_process.current_process
    _set_config_default(process, cr_wrapper_with_test_process)
    process.config_field('new_groups', groups)
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())

    assert_that(has_groups_from_process_config(info).is_success(), equal_to(False))


def test_has_groups_from_process_config_with_rewrite_passess(session, cr_wrapper_with_test_process):
    groups = cr_wrapper_with_test_process.cash_register.target_groups.split(',')

    process = cr_wrapper_with_test_process.current_process
    _set_config_default(process, cr_wrapper_with_test_process)
    process.config_field('new_groups', groups)
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())

    assert_that(bool(has_groups_from_process_config(info).is_success()), equal_to(True))


def test_has_groups_from_process_config_with_no_rewrite_passes(session, cr_wrapper_with_test_process):
    groups = [cr_wrapper_with_test_process.cash_register.target_groups.split(',')[0]]
    cr_wrapper_with_test_process.cash_register.target_groups += ',SOME_OTHER_GROUP'

    process = cr_wrapper_with_test_process.current_process
    _set_config_default(process, cr_wrapper_with_test_process)
    process.config_field('new_groups', groups)
    process.config_field('rewrite_groups', False)
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())

    assert_that(has_groups_from_process_config(info).is_success(), equal_to(True))


def test_has_groups_from_process_config_with_no_rewrite_fails(session, cr_wrapper_with_test_process):
    groups = ['SOME_OTHER_GROUP']

    process = cr_wrapper_with_test_process.current_process
    _set_config_default(process, cr_wrapper_with_test_process)
    process.config_field('new_groups', groups)
    process.config_field('rewrite_groups', False)
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())

    assert_that(has_groups_from_process_config(info).is_success(), equal_to(False))


@pytest.mark.parametrize('ticket, result', [('ticket', True), (None, False)])
def test_is_reg_application_found(session, cr_wrapper_with_ready_registration, ticket, result):
    with patch.object(TrackerClient, 'find_last_reg_application_upload_issue') as mock:
        mock.return_value = ticket
        info = CashRegisterBaseInfo(
            cr_wrapper_with_ready_registration.serial_number, session, Mock(), TrackerClient.from_app(getApplication()),
            Mock(), Mock()
        )
        assert_that(Checks.REGISTRATION_APPLICATION_ISSUE_FOUND(info).is_success(), equal_to(result))


class TestConfigSchema(StrictSchema):
    main = fields.Str(required=True)


def test_has_valid_config(session, cr_wrapper_with_test_process):
    process = cr_wrapper_with_test_process.current_process
    process.config_field('main', 'nihau')
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())
    assert_that(has_valid_config(info, TestConfigSchema()).is_success(), equal_to(True))


def test_has_valid_config_fails_with_no_process(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(has_valid_config(info, TestConfigSchema()).is_success(), equal_to(False))


def test_has_valid_config_fails_with_no_required_field(session, cr_wrapper_with_test_process):
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())
    assert_that(has_valid_config(info, TestConfigSchema()).is_success(), equal_to(False))


def test_active_registration(session, cr_wrapper_with_completed_registration):
    info = CashRegisterBaseInfo(cr_wrapper_with_completed_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.ACTIVE_REGISTRATION(info).is_success(), equal_to(True))
    assert_that(Checks.NO_ACTIVE_REGISTRATIONS(info).is_success(), equal_to(False))


def test_active_registration_fails_on_no_registration(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.ACTIVE_REGISTRATION(info).is_success(), equal_to(False))
    assert_that(Checks.NO_ACTIVE_REGISTRATIONS(info).is_success(), equal_to(True))


def test_active_registration_fails_on_expired_registration(session, cr_wrapper_with_expired_registration):
    info = CashRegisterBaseInfo(cr_wrapper_with_expired_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.ACTIVE_REGISTRATION(info).is_success(), equal_to(False))
    assert_that(Checks.NO_ACTIVE_REGISTRATIONS(info).is_success(), equal_to(True))


def test_active_registration_fails_on_incomplete_registration(session, cr_wrapper_with_ready_registration):
    cr_wrapper_with_ready_registration.current_registration.state = scheme.REGISTRATION_NEW
    info = CashRegisterBaseInfo(cr_wrapper_with_ready_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.ACTIVE_REGISTRATION(info).is_success(), equal_to(False))
    assert_that(Checks.NO_ACTIVE_REGISTRATIONS(info).is_success(), equal_to(True))


def test_last_registration_doesnt_exist(session, cr_wrapper):
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.LAST_REGISTRATION_DOESNT_EXIST_OR_EXPIRED(info).is_success(), equal_to(True))


def test_last_registration_expired(session, cr_wrapper_with_expired_registration):
    info = CashRegisterBaseInfo(cr_wrapper_with_expired_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.LAST_REGISTRATION_DOESNT_EXIST_OR_EXPIRED(info).is_success(), equal_to(True))


def test_last_registration_doesnt_exist_or_expired_fails(session, cr_wrapper_with_completed_registration):
    info = CashRegisterBaseInfo(cr_wrapper_with_completed_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.LAST_REGISTRATION_DOESNT_EXIST_OR_EXPIRED(info).is_success(), equal_to(False))


def test_new_registration_has_config_info(session, cr_wrapper_with_test_process_with_registration):
    cr_wrapper = cr_wrapper_with_test_process_with_registration
    process, reg = cr_wrapper.current_process, cr_wrapper.current_registration
    _set_config_manual(process, 'V505', reg.firm_inn, reg.is_bso_kkt, reg.ofd_inn)
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process_with_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.NEW_REGISTRATION_HAS_CONFIG_INFO(info).is_success(), equal_to(True))


def test_new_registration_has_config_info_fails_on_wrong_ofd(session, cr_wrapper_with_test_process_with_registration):
    cr_wrapper = cr_wrapper_with_test_process_with_registration
    process, reg = cr_wrapper.current_process, cr_wrapper.current_registration
    _set_config_manual(process, 'V505', reg.firm_inn, reg.is_bso_kkt, '777')
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process_with_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.NEW_REGISTRATION_HAS_CONFIG_INFO(info).is_success(), equal_to(False))


def test_new_registration_has_config_info_fails_on_wrong_firm(session, cr_wrapper_with_test_process_with_registration):
    cr_wrapper = cr_wrapper_with_test_process_with_registration
    process, reg = cr_wrapper.current_process, cr_wrapper.current_registration
    _set_config_manual(process, 'V505', '777', reg.is_bso_kkt, reg.ofd_inn)
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process_with_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.NEW_REGISTRATION_HAS_CONFIG_INFO(info).is_success(), equal_to(False))


def test_new_registration_has_config_info_fails_on_wrong_bso(session, cr_wrapper_with_test_process_with_registration):
    cr_wrapper = cr_wrapper_with_test_process_with_registration
    process, reg = cr_wrapper.current_process, cr_wrapper.current_registration
    _set_config_manual(process, 'V505', reg.firm_inn, reg.is_bso_kkt ^ True, reg.ofd_inn)
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process_with_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.NEW_REGISTRATION_HAS_CONFIG_INFO(info).is_success(), equal_to(False))


def test_new_registration_has_config_info_fails_on_wrong_app_ver(session, cr_wrapper_with_test_process_with_registration):
    cr_wrapper = cr_wrapper_with_test_process_with_registration
    process, reg = cr_wrapper.current_process, cr_wrapper.current_registration
    _set_config_manual(process, None, reg.firm_inn, reg.is_bso_kkt, reg.ofd_inn)
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process_with_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.NEW_REGISTRATION_HAS_CONFIG_INFO(info).is_success(), equal_to(False))


def test_new_registration_fails_on_no_process(session, cr_wrapper_with_completed_registration):
    info = CashRegisterBaseInfo(cr_wrapper_with_completed_registration.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.NEW_REGISTRATION_HAS_CONFIG_INFO(info).is_success(), equal_to(False))


def test_new_registration_fails_on_no_reg(session, cr_wrapper_with_test_process):
    info = CashRegisterBaseInfo(cr_wrapper_with_test_process.serial_number, session, Mock(), Mock(), Mock())
    assert_that(Checks.NEW_REGISTRATION_HAS_CONFIG_INFO(info).is_success(), equal_to(False))


def _set_config_default(process, cr_wrapper):
    # type: (scheme.CashRegisterProcess, CashRegister) -> None
    _set_config_manual(
        process, 'V505',
        cr_wrapper.firm_inn, False
    )


def _set_config_manual(process, fns_app, firm_inn, is_bso_kkt, ofd_inn=scheme.YANDEX_OFD_INN):
    # type: (scheme.CashRegisterProcess, str, str, bool, str) -> None
    process.config_field('fns_app_version', fns_app)
    process.config_field('firm_inn', firm_inn)
    process.config_field('is_bso_kkt', is_bso_kkt)
    process.config_field('ofd_inn', ofd_inn)


def _prepare_info_for_request_check(session, cr_wrapper, response_code, body):
    response = FnsregTestResponse(response_code, content=json.dumps(body))
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock())
    info.fnsreg_reregistration_status = response
    return info


def _get_info_for_fnsreg_status(session, cr_wrapper, stage_name, uuid_name, uuid_value):
    # type: (Session, CashRegister, str, str, str) -> CashRegisterBaseInfo
    fnsreg_client = FnsregClient.from_app(getApplication())
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock(), fnsreg_client)
    with session.begin():
        cr_wrapper.current_process.stage = stage_name
        cr_wrapper.current_process.set_data_field_for_stage(uuid_name, uuid_value)
    return info
