# coding=utf-8
from collections import namedtuple

from butils.application import getApplication
from hamcrest import assert_that, is_, has_entries, contains_exactly, equal_to, empty
from lxml import etree
from mock import patch, Mock

from yb_darkspirit import scheme

import json
import pytest

from yb_darkspirit.api.handlers.reregistrations import ReregisterFetch
from yb_darkspirit.api.utils import unpack_document_batches
from yb_darkspirit.core.registration.documents import make_registration_batch
from yb_darkspirit.interactions.tracker import TrackerClient, TicketAcquireResult
from yb_darkspirit.core import pdf_parsing
from yb_darkspirit.process.reregistration import ReRegistrationProcess

REGISTRATION_XSD_FILE = './tests/xsd/KO_ZVLREGKKT_1_800_11_05_04_01.xsd'


def test_parse_host():
    host = ReregisterFetch.parse_host("https://whitespirit1h.paysys.yandex.net:8080")
    assert_that(host, equal_to("1h"))


def test_fetch_registrations_fetching_ready_in_rereg(session, cr_wrapper_with_completed_registration):
    cr_wrapper_with_completed_registration.cash_register.change_state(scheme.DsCashState.IN_REREGISTRATION)
    with session.begin():
        register_list = ReregisterFetch.fetch_registrations(session)
    assert_that(register_list, contains_exactly(cr_wrapper_with_completed_registration.current_registration))


def test_fetch_registrations_not_fetching_ok(session, cr_wrapper_with_ready_registration):
    with session.begin():
        register_list = ReregisterFetch.fetch_registrations(session)
    assert_that(register_list, empty())


def _add_and_delete_process_with_fs_ticket(session, cr_wrapper_with_completed_registration, process, ticket):
    instance = process.create_cash_register_process(cr_wrapper_with_completed_registration.cash_register, process.initial_stage().name)
    instance.data = {'change_fs': {'ticket': ticket}}
    with session.begin():
        session.add(instance)
    with session.begin():
        session.delete(instance)


def test_fetch_last_process_data(session, cr_wrapper_with_completed_registration):
    process = ReRegistrationProcess()
    _add_and_delete_process_with_fs_ticket(session, cr_wrapper_with_completed_registration, process, 'some_info')
    _add_and_delete_process_with_fs_ticket(session, cr_wrapper_with_completed_registration, process, 'some_info_2')
    with session.begin():
        data = ReregisterFetch.fetch_last_process_data(session, [cr_wrapper_with_completed_registration.current_registration])

    assert_that(data, equal_to({cr_wrapper_with_completed_registration.cash_register_id: {'change_fs': {'ticket': 'some_info_2'}}}))


def test_reregister_fetch_with_ticket(session, cr_wrapper_with_completed_registration, test_client):
    cr_wrapper_with_completed_registration.cash_register.change_state(scheme.DsCashState.IN_REREGISTRATION)
    process = ReRegistrationProcess()
    _add_and_delete_process_with_fs_ticket(session, cr_wrapper_with_completed_registration, process, 'some_info')
    response = test_client.get(
        "/v1/reregistrations/reregister-fetch",
    )
    raw_expected_response_data = [
        {
            "host": ReregisterFetch.parse_host(cr_wrapper_with_completed_registration.cash_register.whitespirit_url),
            "inn": cr_wrapper_with_completed_registration.current_registration.firm_inn,
            "sn": cr_wrapper_with_completed_registration.cash_register.serial_number,
            "fn_sn_new": cr_wrapper_with_completed_registration.cash_register.fiscal_storage.serial_number,
            "hidden": cr_wrapper_with_completed_registration.cash_register.hidden,
            "status": cr_wrapper_with_completed_registration.cash_register.state.lower(),
            "startrek_ticket_for_replace_fn": "some_info",
        }
    ]

    assert_that(response.status_code, is_(200))
    assert_that(json.loads(response.data), equal_to(raw_expected_response_data))


def test_reregister_fetch_with_no_ticket(session, cr_wrapper_with_completed_registration, test_client):
    cr_wrapper_with_completed_registration.cash_register.change_state(scheme.DsCashState.IN_REREGISTRATION)
    response = test_client.get(
        "/v1/reregistrations/reregister-fetch",
    )
    raw_expected_response_data = [
        {
            "host": ReregisterFetch.parse_host(cr_wrapper_with_completed_registration.cash_register.whitespirit_url),
            "inn": cr_wrapper_with_completed_registration.current_registration.firm_inn,
            "sn": cr_wrapper_with_completed_registration.cash_register.serial_number,
            "fn_sn_new": cr_wrapper_with_completed_registration.cash_register.fiscal_storage.serial_number,
            "hidden": cr_wrapper_with_completed_registration.cash_register.hidden,
            "status": cr_wrapper_with_completed_registration.cash_register.state.lower(),
            "startrek_ticket_for_replace_fn": None,
        }
    ]

    assert_that(response.status_code, is_(200))
    assert_that(json.loads(response.data), equal_to(raw_expected_response_data))


@patch.object(TrackerClient, 'get_last_attachment', Mock(return_value=('content', 'an url')))
@patch.object(pdf_parsing, '_parse_pdf',
              Mock(return_value={'sn': '3820049015521', 'rnm': '12345', 'fn': '9282440300668633'}))
@patch.object(TrackerClient, 'get_status', Mock(return_value='closed'))
def test_parse_pdf_card(test_client):
    resp = test_client.post(
        '/v1/reregistrations/parse_reregistration_card',
        json={
            'ticket': 'SPIRITSUP-00000',
            'cr_serial_number': '3820049015521',
            'fs_serial_number': '9282440300668633',
        }
    )
    assert_that(resp.status_code, is_(200))
    assert_that(
        json.loads(resp.data),
        contains_exactly(u'ticket_status', u'file_url', u'last_attachment')
    )


def _make_reregister_request(test_client, serial_numbers, sn_to_ofd=None):
    data = {"serial_numbers": serial_numbers}
    if sn_to_ofd is not None:
        data["sn_to_ofd"] = sn_to_ofd

    return test_client.post(
        "/v1/reregistrations/reregister-batch",
        data=json.dumps(data),
        content_type="application/json",
    )


def test_reregister_batch(reregister_entry):
    # TODO: change FS
    registration = reregister_entry
    # TODO: check all fields
    assert registration.is_reregistration
    assert registration.state == scheme.REGISTRATION_COMPLETE


def test_reregister_document_conforms_to_xsd_schema(reregister_entry):
    registration = reregister_entry
    batch = next(make_registration_batch([registration]))
    document = next(unpack_document_batches(batch.data))
    doc = etree.fromstring(document.data)

    xmlschema_doc = etree.parse(REGISTRATION_XSD_FILE)
    xmlschema = etree.XMLSchema(xmlschema_doc)
    xmlschema.assertValid(doc)


IssueClass = namedtuple('Issue', ('key',))
ResponseClass = namedtuple('Response', ('status_code', 'content',))


class ReregistrationStatusResponse(object):
    def __init__(self, status_code, body):
        self.status_code = status_code
        self.body = body

    def json(self):
        return self.body


def test_create_application(
        response_maker,
        cr_wrapper_with_expired_registration,
        test_client,
        tracker_client,
        documents_client,
):
    response_maker.reregister_batch_response(cr_wrapper_with_expired_registration)
    s3_url = 'https://blah.foo:42/bar'
    documents_client.upload_reregistration_application.return_value = {'url': s3_url}

    issue_key = 'SPIRITSUP-42'
    tracker_client.find_last_rereg_application_upload_issue.return_value = []
    tracker_client.create_rereg_application_upload_issue.return_value = IssueClass(issue_key)

    response = test_client.post(
        '/v1/reregistrations/create-application',
        data=json.dumps({'cr_serial_number': cr_wrapper_with_expired_registration.serial_number}),
        content_type='application/json',
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'issue': issue_key,
            's3_url': s3_url,
        })
    )


def test_repeated_create_application_returns_last_issue(
        response_maker,
        cr_wrapper_with_expired_registration,
        test_client,
        tracker_client,
        documents_client,
):
    response_maker.reregister_batch_response(cr_wrapper_with_expired_registration)
    s3_url = 'https://blah.foo:42/bar'
    documents_client.upload_reregistration_application.return_value = {'url': s3_url}

    prev_issue_key = 'SPIRITSUP-100500'
    tracker_client.find_last_rereg_application_upload_issue.return_value = IssueClass(prev_issue_key)

    response = test_client.post(
        '/v1/reregistrations/create-application',
        data=json.dumps({'cr_serial_number': cr_wrapper_with_expired_registration.serial_number}),
        content_type='application/json',
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'issue': prev_issue_key,
            's3_url': s3_url,
        })
    )


@pytest.mark.parametrize("ofd_inn", ["7704358518", "7709364346"])
def test_set_ofd_reregistration(
        ofd_inn,
        test_client,
        cr_wrapper_with_expired_registration,
        session
):
    _make_reregister_request(
        test_client,
        serial_numbers=[cr_wrapper_with_expired_registration.serial_number],
        sn_to_ofd={cr_wrapper_with_expired_registration.serial_number: ofd_inn}
    )

    registration = cr_wrapper_with_expired_registration.current_registration
    assert registration
    assert registration.ofd_inn == ofd_inn


def test_preserve_ofd_reregistration(
        test_client,
        cr_wrapper_with_expired_registration,
        session
):
    old_registration = cr_wrapper_with_expired_registration.current_registration
    assert old_registration

    response = _make_reregister_request(
        test_client,
        serial_numbers=[cr_wrapper_with_expired_registration.serial_number]
    )
    assert response.status_code == 200

    registration = cr_wrapper_with_expired_registration.current_registration
    assert registration
    assert registration.parent_id == old_registration.id
    assert registration.ofd_inn == old_registration.ofd_inn


@pytest.mark.parametrize('is_bso_kkt', [False, True])
def test_reregistration_preserves_bso_attribute(
        is_bso_kkt,
        test_client,
        cr_wrapper_maker,
        session,
):
    cr_wrapper = cr_wrapper_maker.with_expired_registration(is_bso_kkt)
    old_registration = cr_wrapper.current_registration
    assert old_registration

    response = _make_reregister_request(
        test_client,
        serial_numbers=[cr_wrapper.serial_number]
    )
    assert response.status_code == 200

    registration = cr_wrapper.current_registration
    assert registration
    assert registration.is_bso_kkt == old_registration.is_bso_kkt


def test_success_acquire_application_upload_ticket(
        test_client,
        tracker_client
):
    tracker_client.acquire_application_upload_ticket.return_value = TicketAcquireResult.SUCCESS

    data = {'ticket': 'ISSUE-42'}
    response = test_client.post(
        '/v1/reregistrations/acquire-application-upload-ticket',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'issue': 'ISSUE-42',
        })
    )

    tracker_client.acquire_application_upload_ticket.assert_called_with(key='ISSUE-42')


@pytest.mark.parametrize('acquire_result,expected_code', [
    (TicketAcquireResult.BAD_STATE, 400),
    (TicketAcquireResult.ALREADY_ACQUIRED, 410)
])
def test_acquire_application_upload_ticket_failure(
        test_client,
        tracker_client,
        acquire_result,
        expected_code
):
    tracker_client.acquire_application_upload_ticket.return_value = acquire_result

    data = {'ticket': 'ISSUE-42'}
    response = test_client.post(
        '/v1/reregistrations/acquire-application-upload-ticket',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(expected_code), response.data)
    tracker_client.acquire_application_upload_ticket.assert_called_with(key='ISSUE-42')


def test_create_application_auto(
        cr_wrapper_ready_for_reregistration,
        test_client,
        tracker_client,
        fnsreg_client
):
    cr_wrapper = cr_wrapper_ready_for_reregistration
    issue = 'ISSUE-42'

    cr_sn = cr_wrapper.serial_number
    fn_sn = '9999078900003131'
    tracker_client.find_rereg_issue_serial_numbers.return_value = (cr_sn, fn_sn)
    tracker_client.attach_file.return_value = None

    fnsreg_client.generate_reregistration_application.return_value = ResponseClass(
        status_code=200,
        content='It looks like a valid application XML',
    )

    data = {
        'ticket': issue,
        'app_version': 'V503'
    }
    response = test_client.post(
        '/v1/reregistrations/create-application-auto',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'issue': issue,
            'cr_serial_number': cr_sn,
            'new_fn_serial_number': fn_sn,
        })
    )

    _, kwargs = tracker_client.attach_file.call_args
    assert kwargs['issue_key'] == issue


def test_upload_application_auto(
        test_client,
        tracker_client,
        fnsreg_client,
        documents_client
):
    issue = 'ISSUE-42'
    application_xml = 'It looks like a valid application XML'
    reregistration_uuid = '4242-test'

    tracker_client.get_last_attachment.return_value = (application_xml, 'application_auto_ISSUE-42.xml')
    fnsreg_client.upload_reregistration_application.return_value = ResponseClass(status_code=200, content=None)
    documents_client.upload_reregistration_application.return_value = {'url': 's3://url'}

    data = {
        'ticket': issue,
        'reregistration_uuid': reregistration_uuid,
        'cr_serial_number': '001',
        'fn_serial_number': '002',
    }
    response = test_client.post(
        '/v1/reregistrations/upload-application-auto',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            's3_url': 's3://url'
        })
    )

    fnsreg_client.upload_reregistration_application.assert_called_with(application_xml,
                                                                       registration_uuid=reregistration_uuid)

    _, kwargs = documents_client.upload_reregistration_application.call_args
    assert kwargs['cr_sn'] == '001'
    assert kwargs['fn_sn'] == '002'
    assert kwargs['doc'].read() == application_xml


def test_check_status_on_closed_ticket(
        test_client,
        tracker_client
):
    issue = 'ISSUE-42'
    reregistration_uuid = '4242-test'

    tracker_client.get_status.return_value = 'closed'

    data = {
        'ticket': issue,
        'reregistration_uuid': reregistration_uuid
    }
    response = test_client.post(
        '/v1/reregistrations/check-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'success'
        })
    )


def test_check_status_rejected_registration(
        test_client,
        tracker_client,
        fnsreg_client
):
    issue = 'ISSUE-42'
    reregistration_uuid = '4242-test'

    tracker_client.get_status.return_value = 'open'
    fnsreg_client.get_reregistration_status.return_value = ReregistrationStatusResponse(
        status_code=200,
        body={'status': 'rejected'}
    )

    data = {
        'ticket': issue,
        'reregistration_uuid': reregistration_uuid
    }
    response = test_client.post(
        '/v1/reregistrations/check-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'rejected'
        })
    )


def test_check_status_in_process_registration(
        test_client,
        tracker_client,
        fnsreg_client
):
    issue = 'ISSUE-42'
    reregistration_uuid = '4242-test'

    tracker_client.get_status.return_value = 'open'
    fnsreg_client.get_reregistration_status.return_value = ReregistrationStatusResponse(
        status_code=200,
        body={'status': 'in_process'}
    )

    data = {
        'ticket': issue,
        'reregistration_uuid': reregistration_uuid
    }
    response = test_client.post(
        '/v1/reregistrations/check-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'in_process'
        })
    )


def test_check_status_success_registration(
        test_client,
        tracker_client,
        fnsreg_client
):
    issue = 'ISSUE-42'
    reregistration_uuid = '4242-test'

    tracker_client.get_status.return_value = 'open'
    tracker_client.attach_file.return_value = None
    tracker_client.close_ticket.return_value = None
    fnsreg_client.get_reregistration_status.return_value = ReregistrationStatusResponse(
        status_code=200,
        body={
            'status': 'success',
            'documentBase64': 'ZG9j',  # doc
        }
    )

    data = {
        'ticket': issue,
        'reregistration_uuid': reregistration_uuid
    }
    response = test_client.post(
        '/v1/reregistrations/check-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'success'
        })
    )

    _, kwargs = tracker_client.attach_file.call_args
    assert kwargs['issue_key'] == issue

    tracker_client.close_ticket.assert_called_with(key=issue)
