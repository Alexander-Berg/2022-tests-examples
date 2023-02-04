# coding=utf-8
import json
from datetime import datetime
from itertools import chain
from time import time
from typing import List, Iterable, Optional, Any

import pytest
import responses
from hamcrest import assert_that, contains_inanyorder, has_entry, has_property, is_, only_contains, equal_to, \
    contains_string
from mock import Mock, patch

from tests import ws_responses
from yb_darkspirit import scheme, interface
from yb_darkspirit.api.handlers.admin import MakeCorrectionReceipt, PrepareCorrectionForReceipt
from yb_darkspirit.application.plugins.dbhelper import Session

CR_INVENTARY_NUMBER = '123456789'
TICKET_NUM = 'ITDC-42'


class CreateTicketMocks(object):
    get_cr_inventary_number = None  # type: Mock
    get_newest_issue = None  # type: Mock

    def __init__(self, get_cr_inventary_number, get_newest_issue):
        self.get_newest_issue = get_newest_issue
        self.get_cr_inventary_number = get_cr_inventary_number


@pytest.fixture
def create_ticket_mocks():
    with \
            patch('yb_darkspirit.interface.CashRegister.cr_inventary_number') as mocked_get_cr_inventary_number,\
            patch('yb_darkspirit.core.fs_replacement._get_newest_issue') as mocked_get_newest_issue:
        mocked_get_cr_inventary_number.return_value = CR_INVENTARY_NUMBER
        mocked_get_newest_issue.return_value.key = None
        yield CreateTicketMocks(mocked_get_cr_inventary_number, mocked_get_newest_issue)


def test_create_ticket_for_replacing_single_fs_basic(
        cr_wrapper_postfiscal,
        test_client,
        rsps,
        ws_mocks,
        create_ticket_mocks
):
    ws_mocks.cashmachines_ident(
        cr_wrapper_postfiscal.long_serial_number,
        json={"status": "ok"},
        content_type="application/json",
    )
    rsps.add(
        responses.GET,
        url='https://test.bot.yandex-team.ru/api/v3/dc/request',
        json={'result': {'StNum': TICKET_NUM}},
        match_querystring=False,
    )

    resp = test_client.post(
        "/v1/admin/create-ticket-for-replacing-fs",
        json=dict(cr_serial=cr_wrapper_postfiscal.serial_number),
        content_type='application/json',
    )

    assert_that(resp.status_code, is_(200), resp.data)
    assert_that(json.loads(resp.data), has_entry('ticket', TICKET_NUM))


def test_create_ticket_for_replacing_single_fs_returns_existing_ticket(
        cr_wrapper_postfiscal,
        test_client,
        rsps,
        ws_mocks,
        create_ticket_mocks
):
    create_ticket_mocks.get_newest_issue.return_value.key = TICKET_NUM

    resp = test_client.post(
        "/v1/admin/create-ticket-for-replacing-fs",
        json=dict(cr_serial=cr_wrapper_postfiscal.serial_number),
        content_type='application/json',
    )

    assert_that(resp.status_code, is_(200), resp.data)
    assert_that(json.loads(resp.data), has_entry('ticket', TICKET_NUM))


def test_create_ticket_for_replacing_single_fs_nonconfigured_return_blockers(
        cr_wrapper_nonconfigured_fs_ready_fiscal,
        create_ticket_mocks,
        test_client
):
    cr_wrapper = cr_wrapper_nonconfigured_fs_ready_fiscal
    resp = test_client.post(
        "/v1/admin/create-ticket-for-replacing-fs",
        json=dict(cr_serial=cr_wrapper.serial_number),
        content_type='application/json',
    )
    assert_that(resp.status_code, is_(400), resp.data)
    assert_that(
        json.loads(resp.data),
        has_entry(
            'blockers',
            contains_inanyorder(contains_string("POSTFISCAL"), contains_string("FISCAL_STORAGE_ARCHIVE_READING"))
        )
    )


def test_create_ticket_for_replacing_single_fs_nonconfigured_with_gaps_returns_blocker(
        cr_wrapper_postfiscal_with_gaps,
        create_ticket_mocks,
        test_client
):
    cr_wrapper = cr_wrapper_postfiscal_with_gaps
    resp = test_client.post(
        "/v1/admin/create-ticket-for-replacing-fs",
        data=json.dumps(dict(cr_serial=cr_wrapper.serial_number)),
        content_type='application/json',
    )
    assert_that(resp.status_code, is_(400), resp.data)
    assert_that(
        json.loads(resp.data)['blockers'][0],
        contains_string('HAS_NO_GAPS')
    )


def test_create_ticket_for_replacing_single_fs_absent_cash_register_returns_error(
        create_ticket_mocks,
        test_client
):
    resp = test_client.post(
        "/v1/admin/create-ticket-for-replacing-fs",
        data=json.dumps(dict(cr_serial='0123456789')),
        content_type='application/json',
    )
    assert_that(resp.status_code, is_(400), resp.data)
    assert_that(
        json.loads(resp.data),
        has_entry('message', 'No cash register found')
    )


def test_create_ticket_for_replacing_single_fs_offline_cash_register_returns_error(
        cr_wrapper_offline,
        create_ticket_mocks,
        test_client
):
    resp = test_client.post(
        "/v1/admin/create-ticket-for-replacing-fs",
        data=json.dumps(dict(cr_serial=cr_wrapper_offline.serial_number)),
        content_type='application/json',
    )
    assert_that(resp.status_code, is_(400), resp.data)
    assert_that(
        json.loads(resp.data),
        has_entry('message', 'Cash register is unreachable in whitespirit')
    )


def _make_fiscal_storage(session, serial_number=None):
    # type: (Session, Optional[str]) -> scheme.FiscalStorage
    fs = scheme.FiscalStorage(
        serial_number=serial_number or "9282440300604311",
        state="FISCAL",
        state_dt=datetime.utcfromtimestamp(time()),
        update_dt=datetime.utcfromtimestamp(time()),
        last_document_number=240087,
        hidden=False,
        type_code='fn-1',
        status="good",
    )
    with session.begin():
        session.add(fs)
    return fs


def _make_document(session, fs, is_confirmed=True):
    # type: (Session, scheme.FiscalStorage, bool) -> scheme.Document
    doc = scheme.Document(
        document_type="Receipt",
        fiscal_storage_number=228508,
        fiscal_storage_sign=699537704,
        dt=datetime.utcfromtimestamp(time()),
        raw_document='',
        archive=0,
        fiscal_storage_id=fs.id,
        is_confirmed=is_confirmed,
        user_requisite_name="trust_purchase_token",
        user_requisite_value="e83376a6b2a44c37228d9e108ff85e8a"
    )
    session.add(doc)
    session.flush()
    return doc


def _make_cash_register(session, fs, serial_number, group, hidden=False):
    # type: (Session, scheme.FiscalStorage, str, str, bool) -> scheme.CashRegister
    cr = scheme.CashRegister(
        serial_number=serial_number,
        state=scheme.CASH_REGISTER_STATE_OPEN_SHIFT,
        fiscal_storage_id=fs.id,
        current_groups=group,
        target_groups=group,
        ds_state=scheme.DsCashState.OK.value,
        hidden=hidden,
    )
    with session.begin():
        session.add(cr)
    return cr


def _make_batch_cash_registers(session, group, count, sn_init, hidden=False):
    # type: (Session, str, int, int, bool) -> List[scheme.CashRegister]
    crs = [_make_cash_register(session,
                               fs=_make_fiscal_storage(session, str(sn_init * 100 + i)),
                               serial_number=str(sn_init + i),
                               group=group,
                               hidden=hidden)
           for i in xrange(count)]
    with session.begin():
        session.add_all(crs)
    return crs


def _make_registration(session, inn, fs, cash_register):
    # type: (Session, str, scheme.FiscalStorage, scheme.CashRegister) -> scheme.Registration
    reg = scheme.Registration(firm_inn=inn, fiscal_storage=fs, cash_register=cash_register,
                              state=scheme.REGISTRATION_COMPLETE)
    with session.begin():
        session.add(reg)
    return reg


def _make_batch_registrations(session, inn, cash_registers):
    # type: (Session, str, Iterable[scheme.CashRegister]) -> List[scheme.Registration]
    registrations = [_make_registration(session, inn=inn, fs=cr.fiscal_storage, cash_register=cr)
                     for cr in cash_registers]
    with session.begin():
        session.add_all(registrations)
    return registrations


def test_make_correction_receipt__get_fs_sn_and_doc_number(session):
    fs1 = _make_fiscal_storage(session, serial_number="9282440300604311")
    fs2 = _make_fiscal_storage(session, serial_number="9282440300604322")
    doc1 = _make_document(session, fs1)
    doc2 = _make_document(session, fs2)

    fs_sn, doc_num, doc_type = PrepareCorrectionForReceipt._get_document_info(session, doc1.id)
    assert_that(fs_sn, is_(fs1.serial_number))
    assert_that(doc_num, is_(doc1.fiscal_storage_number))
    assert_that(doc_type, is_(doc1.document_type))


@pytest.mark.parametrize('use_hidden_cash_registers', [True, False], ids=['hidden cash registers',
                                                                          'not hidden cash registers'])
def test_make_correction_receipt__get_suitable_cash_registers(app_config_file, session, yandex_taxi_firm_inn,
                                                              use_hidden_cash_registers):
    some_other_firm_inn = '7777711111'
    with session.begin():
        session.add(scheme.Firm(inn=some_other_firm_inn, title='Some other firm', kpp='_', ogrn='_', agent=False,
                                sono_initial=0, sono_destination=0))
    cash_registers = list(chain(
        _make_batch_cash_registers(session, group='NEW', count=5, sn_init=100),
        _make_batch_cash_registers(session, group='_NOGROUP', count=10, sn_init=200),
        _make_batch_cash_registers(session, group='NEW', count=15, sn_init=300, hidden=use_hidden_cash_registers),
    ))
    registrations = list(chain(
        _make_batch_registrations(session, yandex_taxi_firm_inn, cash_registers[:7]),
        _make_batch_registrations(session, some_other_firm_inn, cash_registers[7:17]),
        _make_batch_registrations(session, yandex_taxi_firm_inn, cash_registers[17:]),
    ))
    doc = _make_document(session, next(cr for cr in cash_registers if cr.serial_number == '303').fiscal_storage)
    inn = PrepareCorrectionForReceipt._get_inn_from_document(session, doc.id)
    suitable_cash_registers = MakeCorrectionReceipt._get_top_suitable_cash_registers(
        session, inn, limit=10, use_hidden_cash_registers=use_hidden_cash_registers)
    suitable_registrations = [r for r in registrations if r.cash_register in suitable_cash_registers]

    assert_that(suitable_registrations, only_contains(has_property('firm_inn', is_(yandex_taxi_firm_inn))))
    assert_that(suitable_cash_registers, only_contains(has_property('hidden', is_(use_hidden_cash_registers))))


def test_update_groups_for_close_shift_cr_is_ok(cr_wrapper, test_client, ws_mocks):
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
    )
    _assert_groups(cr_wrapper, '_NOGROUP', None)
    _configure_kkt(test_client, ws_mocks, cr_wrapper, ['NEW'])
    _assert_groups(cr_wrapper, '_NOGROUP', 'NEW')


def test_update_groups_for_offline_cr_is_not_found(cr_wrapper_offline, test_client, ws_mocks):
    cr_wrapper = cr_wrapper_offline
    _assert_groups(cr_wrapper, 'GROUP0', None)
    with patch.object(interface.CashRegister, 'sync'):
        _configure_kkt(test_client, ws_mocks, cr_wrapper, ['NEW'], status=404)


@pytest.mark.parametrize(['is_registration_expired', 'is_bso_kkt', 'current_groups', 'new_groups', 'status_code', 'target_groups'], [
    (False, True, 'BSO', ['NEW'], 400, None),
    (False, True, 'BSO', ['BSO'], 200, 'BSO'),
    (False, False, '_NOGROUP', ['BSO'], 400, None),
    (False, False, '_NOGROUP', ['NEW'], 200, 'NEW'),
    (True, False, '_NOGROUP', ['BSO'], 200, 'BSO'),
])
def test_update_groups_bso_cr(is_registration_expired, cr_wrapper_maker, test_client, ws_mocks,
                              is_bso_kkt, current_groups, new_groups, status_code, target_groups):
    if is_registration_expired:
        cr_wrapper = cr_wrapper_maker.with_expired_registration(is_bso_kkt)
    else:
        cr_wrapper = cr_wrapper_maker.with_new_registration(is_bso_kkt)
    ws_mocks.cashmachines_set_datetime(cr_wrapper.whitespirit_key)
    ws_mocks.cashmachines_configure(cr_wrapper.whitespirit_key)
    _assert_groups(cr_wrapper, current_groups, None)
    assert_that(_update_groups(test_client, cr_wrapper, new_groups).status_code, is_(status_code))
    _assert_groups(cr_wrapper, current_groups, target_groups)


def _assert_groups(wrapper, current_groups, target_groups):
    # type: (interface.CashRegister, str, Any[str, None]) -> None
    assert_that(wrapper.cash_register.current_groups, equal_to(current_groups))
    assert_that(wrapper.cash_register.target_groups, equal_to(target_groups))


def _update_groups(test_client, cr_wrapper, groups, rewrite_groups=True, update_now=False):
    return test_client.post('/v1/admin/update_groups', json={
        "rewrite_groups": rewrite_groups,
        "update_now": update_now,
        "groups": groups,
        "serial_number": cr_wrapper.serial_number
    })


def _configure_kkt(test_client, ws_mocks, cr_wrapper, groups, rewrite_groups=True, status=200):
    ws_mocks.cashmachines_set_datetime(cr_wrapper.whitespirit_key)
    ws_mocks.cashmachines_configure(cr_wrapper.whitespirit_key)
    res = _update_groups(test_client, cr_wrapper, groups, rewrite_groups)
    assert_that(res.status_code, is_(status))
