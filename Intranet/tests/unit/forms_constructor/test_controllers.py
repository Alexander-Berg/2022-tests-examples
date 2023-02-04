import uuid

import pytest
from unittest.mock import patch, Mock

from intranet.femida.src.core.switches import TemporarySwitch
from intranet.femida.src.candidates.choices import (
    VERIFICATION_STATUSES,
    VERIFICATION_RESOLUTIONS,
    VERIFICATION_TYPES,
)
from intranet.femida.src.candidates.models import Candidate
from intranet.femida.src.forms_constructor.controllers import (
    handle_verification_form_answer,
    VerificationResult,
)
from intranet.femida.src.utils.datetime import shifted_now
from intranet.femida.src.candidates.startrek.issues import create_ess_issue
from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import freeze


@patch('django.utils.timezone.now', freeze)
@patch('intranet.femida.src.forms_constructor.controllers.send_verification_data_to_vendor')
@patch('intranet.femida.src.notifications.candidates.VerificationSubmittedNotification.send')
@patch('intranet.femida.src.notifications.candidates.VerificationWarningNotification.send')
@pytest.mark.parametrize('type', (
    VERIFICATION_TYPES.default,
    VERIFICATION_TYPES.international_by_grade,
))
def test_handle_correct_data(mocked_error, mocked_submit, mocked_vendor_submit, type):
    verification = f.VerificationFactory(
        link_expiration_date=shifted_now(days=1),
        type=type,
    )
    data = {
        'params': {
            'uuid': verification.uuid.hex,
            'verification_data': {'some': 'data'},
            'verification_questions': 'questions:\nanswers\n\n',
        },
        'jsonrpc': '2.0',
        'method': 'POST',
    }
    handle_verification_form_answer(data)
    verification.refresh_from_db()
    assert verification.raw_data == data
    assert verification.sent_on_check == freeze()
    assert not mocked_error.called
    assert mocked_submit.called

    if verification.type == VERIFICATION_TYPES.default:
        mocked_vendor_submit.assert_called_once_with(verification)
        assert verification.status == VERIFICATION_STATUSES.on_check
    elif verification.type == VERIFICATION_TYPES.international_by_grade:
        mocked_vendor_submit.assert_not_called()
        assert verification.status == VERIFICATION_STATUSES.on_ess_check


@patch('intranet.femida.src.notifications.candidates.VerificationSubmittedNotification.send')
@patch('intranet.femida.src.notifications.candidates.VerificationWarningNotification.send')
@pytest.mark.parametrize('get_uuid', (
    lambda: uuid.uuid4().hex,
    lambda: '1234',
))
def test_handle_undefined_verification(mocked_error, mocked_submit, get_uuid):
    data = {
        'params': {
            'uuid': get_uuid(),
            'verification_data': {'some': 'data'},
            'verification_questions': 'questions/answers',
        },
        'jsonrpc': '2.0',
        'method': 'POST',
    }
    handle_verification_form_answer(data)
    assert not mocked_error.called
    assert not mocked_submit.called


@pytest.mark.parametrize('link_expiration_date, status', (
    (shifted_now(days=-1), VERIFICATION_STATUSES.new),  # просроченная ссылка
    (shifted_now(days=1), VERIFICATION_STATUSES.on_check),  # повторная отправка verification
))
@patch('intranet.femida.src.notifications.candidates.VerificationSubmittedNotification.send')
@patch('intranet.femida.src.notifications.candidates.VerificationWarningNotification.send')
def test_handle_incorrect_data(mocked_error, mocked_submit, link_expiration_date, status):
    verification = f.VerificationFactory(
        link_expiration_date=link_expiration_date,
        status=status,
    )
    data = {
        'params': {
            'uuid': verification.uuid.hex,
            'verification_data': {'some': 'data'},
            'verification_questions': 'questions/answers',
        },
        'jsonrpc': '2.0',
        'method': 'POST',
    }
    handle_verification_form_answer(data)
    assert mocked_error.called
    assert not mocked_submit.called


@pytest.mark.parametrize('is_vendor_ok, exists_duplicate', (
    (True, True),
    (False, False),
))
@patch('intranet.femida.src.notifications.candidates.VerificationMaybeDuplicateNotification.send')
@patch('intranet.femida.src.notifications.candidates.VerificationSuccessNotification.send')
@patch('intranet.femida.src.forms_constructor.controllers.IssueTransitionOperation.delay')
def test_verification_check_result_handle(
    mocked_transition,
    mocked_notification,
    mocked_duplicate_notification,
    is_vendor_ok,
    exists_duplicate
):
    inn = '123456789012'
    if exists_duplicate:
        Candidate.unsafe.create(inn=inn)
    verification = f.VerificationFactory(
        status=VERIFICATION_STATUSES.on_check,
        link_expiration_date=shifted_now(days=1),
        raw_data={
            'params': {
                'inn': inn
            }
        },
    )
    data = {
        'params': {
            'uuid': verification.uuid.hex,
            'result': 'verification check result',
        },
    }
    VerificationResult(data, ok=is_vendor_ok).update_verification()
    verification.refresh_from_db()
    assert verification.candidate.inn == verification.inn
    if is_vendor_ok:
        assert verification.status == VERIFICATION_STATUSES.closed
        assert verification.resolution == VERIFICATION_RESOLUTIONS.hire
        assert mocked_notification.called
    else:
        assert verification.status == VERIFICATION_STATUSES.on_ess_check
    assert mocked_transition.called
    assert mocked_duplicate_notification.called == exists_duplicate


@patch('intranet.femida.src.forms_constructor.controllers.alarm')
@pytest.mark.parametrize('ok', (True, False))
def test_verification_check_unknown_uuid(mocked_alarm, ok):
    data = {
        'params': {
            'uuid': uuid.uuid4().hex,
            'result': 'verification check result',
        },
    }
    VerificationResult(data, ok=ok).update_verification()
    assert mocked_alarm.called


@pytest.mark.parametrize('link_expiration_date, status', (
    (shifted_now(days=-1), VERIFICATION_STATUSES.on_check),  # просроченная ссылка
    (shifted_now(days=1), VERIFICATION_STATUSES.closed),  # повторная отправка
))
@pytest.mark.parametrize('ok', (True, False))
@patch('intranet.femida.src.forms_constructor.controllers.IssueTransitionOperation.delay')
@patch('intranet.femida.src.notifications.candidates.VerificationWarningNotification.send')
def test_verification_result_form_problems(mocked_error, mocked_close,
                                           link_expiration_date, status, ok):
    verification = f.VerificationFactory(
        link_expiration_date=link_expiration_date,
        status=status,
    )
    data = {
        'params': {
            'uuid': verification.uuid.hex,
            'result': 'verification check result',
        },
    }
    VerificationResult(data, ok=ok).update_verification()
    assert mocked_error.called
    assert not mocked_close.called


# TODO: порефакторить после релиза FEMIDA-7226
@pytest.mark.parametrize('is_vendor_ok, is_switch_active', [
    (is_vendor_ok, is_switch_active)
    for is_vendor_ok in [True, False]
    for is_switch_active in [True, False]
])
@patch('intranet.femida.src.notifications.candidates.VerificationSuccessNotification.send')
@patch('intranet.femida.src.forms_constructor.controllers.IssueTransitionOperation.delay')
@patch('intranet.femida.src.forms_constructor.controllers.IssueUpdateOperation.delay')
def test_verification_result_with_waffle_switch_enabled(mocked_update, mocked_transition,
                                                        mocked_notification, is_vendor_ok,
                                                        is_switch_active):
    f.create_waffle_switch(TemporarySwitch.NEW_VERIFICATION_FLOW, is_switch_active)
    verification = f.VerificationFactory(
        status=VERIFICATION_STATUSES.on_check,
        link_expiration_date=shifted_now(days=1),
        raw_data={
            'params': {
                'inn': '123456789012'
            }
        },
    )
    data = {
        'params': {
            'uuid': verification.uuid.hex,
            'result': 'verification check result',
        },
    }
    if is_switch_active:
        ok_status = VERIFICATION_STATUSES.on_ess_check
        ok_resolution = ''
        notification_called = False
    else:
        ok_status = VERIFICATION_STATUSES.closed
        ok_resolution = VERIFICATION_RESOLUTIONS.hire
        notification_called = True
    VerificationResult(data, ok=is_vendor_ok).update_verification()
    verification.refresh_from_db()
    assert verification.candidate.inn == verification.inn
    if is_vendor_ok:
        assert verification.status == ok_status
        assert verification.resolution == ok_resolution
        assert mocked_notification.called == notification_called
    else:
        assert verification.status == VERIFICATION_STATUSES.on_ess_check
    if is_switch_active:
        assert mocked_update.called
    else:
        assert mocked_transition.called


@pytest.mark.parametrize('is_switch_active, tags', [
    (False, []),
    (True, ['awaiting-checks']),
])
@patch('intranet.femida.src.candidates.startrek.issues.create_issue',
       return_value=Mock(key='ESS-1'))
def test_verification_new_flow_accept_form(mocked_create_issue_task, su_client, is_switch_active, tags):
    f.create_waffle_switch(TemporarySwitch.NEW_VERIFICATION_FLOW, is_switch_active)
    verification = f.VerificationFactory()
    create_ess_issue(verification)

    assert mocked_create_issue_task.call_args.kwargs['tags'] == tags
