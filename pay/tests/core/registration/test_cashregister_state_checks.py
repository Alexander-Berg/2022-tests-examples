from mock import patch, Mock
from tests.conftest import doc_client_get_document

from tests.whitespirit_mocks import WhiteSpiritMocks
from tests.ws_responses import CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD, empty_registration_info
from tests.conftest import doc_client_get_document
import mock
from yb_darkspirit.core.cash_register.state_checks import (
    Checks,
    ChecksManager,
    prepare_failed_checks_dict, FailedCheck, FailedCheckResult,
)
from yb_darkspirit.core.cash_register.maintenance_actions import (
    MakeReadyToRegistrationAction
)
from yb_darkspirit.core.cash_register.state_manager import checks_for_ds_state
from hamcrest import assert_that, equal_to, empty, contains_inanyorder, has_entries
import datetime
from yb_darkspirit import scheme, interface
from yb_darkspirit.application.plugins.dbhelper import Session
from yb_darkspirit.interactions.ofd import OfdPrivateClient, OfdSyncStatus
from yb_darkspirit.interactions.tracker import TrackerClient
from butils.application import getApplication
import pytest
import responses
from typing import Dict, List, Union

from yb_darkspirit.process.prepare_unregistered_cash_to_registration import PrepareUnregisteredCashToRegistrationProcess

NOW = datetime.datetime.now()
BEFORE = NOW - datetime.timedelta(minutes=5)
LONG_BEFORE = NOW - datetime.timedelta(minutes=100)

DEFAULT_NUMBER = '12345'
EXPECTED_RNM = DEFAULT_NUMBER
DEFAULT_FISCAL_SERIAL = "9999" + DEFAULT_NUMBER
NOT_EXPECTED_RNM = '55555'
TICKET = 'ISSUE-1'


def _defaulted_cash_register(ds_state=scheme.DsCashState.UNREGISTERED.value,
                             whitespirit_url="https://whitespirit-dev1f.balance.os.yandex.net:8080",
                             oebs_address_code="SAS>SASTA", state="NONCONFIGURED", admin_password="123456",
                             current_groups="NEW", sw_version='3.5.30', update_dt=BEFORE, serial_number=DEFAULT_NUMBER,
                             hidden=False):
    return scheme.CashRegister(serial_number=serial_number, ds_state=ds_state, whitespirit_url=whitespirit_url,
                               oebs_address_code=oebs_address_code, state=state,
                               admin_password=admin_password, current_groups=current_groups,
                               sw_version=sw_version, update_dt=update_dt, hidden=hidden)


def _defaulted_fiscal_storage(serial_number=DEFAULT_FISCAL_SERIAL, state="READY_FISCAL", status="good", last_document_number=0):
    return scheme.FiscalStorage(serial_number=serial_number, state=state, status=status,
                                last_document_number=last_document_number, type_code="fn-1")


def _defaulted_document(document_number=0, document_type=scheme.DOCUMENT_TYPE_RECEIPT):
    return scheme.Document(fiscal_storage_number=document_number, document_type=document_type)


def _default_registration(state=None, registration_number=EXPECTED_RNM):
    return scheme.Registration(state=state, registration_number=registration_number) if state is not None else None


def _default_process_with_change_fs_ticket(ticket=TICKET):
    return scheme.CashRegisterProcess(data={'change_fs': {'ticket': ticket}}, process='process', stage='stage')


@pytest.fixture
def checks_manager(session, ofd_private_client, tracker_client, fnsreg_client, documents_client):
    return ChecksManager(session, ofd_private_client, tracker_client, fnsreg_client, documents_client)


def test_all_checks_are_good(session, checks_manager, ws_mocks):
    # type: (Session, ChecksManager, WhiteSpiritMocks) -> None
    cash_register = _defaulted_cash_register()
    _store_to_db(session, cash_register, _defaulted_fiscal_storage(),
                 _defaulted_document(), _default_registration(), _default_process_with_change_fs_ticket(ticket=None))
    # Initialize mock of new `cash_register`
    ws_mocks.cashmachines_ssh_ping(
        cr_long_sn=cash_register.long_serial_number,
        use_password=False,
        json={'result': 'pong'},
    )
    ws_mocks.cashmachines_ssh_ping(
        cr_long_sn=cash_register.long_serial_number,
        use_password=True,
        json={'result': 'pong'},
    )
    status_response = CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD.copy()
    status_response.update({
        "sn": cash_register.long_serial_number,
        "fn_sn": cash_register.fiscal_storage.serial_number,
        "registration_info": empty_registration_info(
            cr_sn=cash_register.long_serial_number,
            fn_sn=cash_register.fiscal_storage.serial_number
        )
    })
    status_response['lowlevel'].update({
        'last_device_dt': BEFORE.strftime('%Y-%m-%d %H:%M:%S')
    })
    ws_mocks.cashmachines_status(
        cr_long_sn=cash_register.long_serial_number,
        json=status_response
    )

    assert_that(
        checks_manager.filter_failed_checks(
            cash_register.serial_number,
            MakeReadyToRegistrationAction.get_readiness_checks()
        ),
        empty()
    )


@pytest.mark.parametrize("check,cash_register", [
    (Checks.UNREGISTERED, _defaulted_cash_register(ds_state=scheme.DsCashState.OK.value)),
    (Checks.HAS_WHITESPIRIT_URL, _defaulted_cash_register(whitespirit_url=None)),
    (Checks.NONCONFIGURED, _defaulted_cash_register(state="FISCAL")),
    (Checks.HAS_OEBS_ADDRESS_CODE, _defaulted_cash_register(oebs_address_code=None)),
    (Checks.HAS_ADMIN_PASSWORD, _defaulted_cash_register(admin_password=None)),
    (Checks.NOT_BUG, _defaulted_cash_register(current_groups="BUG")),
    (Checks.FRESH_DATA_IN_DB, _defaulted_cash_register(update_dt=LONG_BEFORE)),
    (Checks.FRESH_SOFTWARE, _defaulted_cash_register(sw_version='3.4.50')),
    (Checks.FRESH_SOFTWARE, _defaulted_cash_register(sw_version='3.5.29')),
    (Checks.CLOSED_SHIFT, _defaulted_cash_register(state="OVERDUE_OPEN_SHIFT")),
    (Checks.CLOSED_FS, _defaulted_cash_register(state="OVERDUE_OPEN_SHIFT")),
    (Checks.CLOSED_FS, _defaulted_cash_register(state="CLOSE_SHIFT")),
    (Checks.CASH_CARDS_OK, _defaulted_cash_register(serial_number="not_a_number")),
    (Checks.IS_HIDDEN, _defaulted_cash_register(hidden=False)),
])
def test_cash_register_checks(session, checks_manager, check, cash_register):
    # type: (Session, ChecksManager, Checks, scheme.CashRegister) -> None
    _store_to_db(session, cash_register, _defaulted_fiscal_storage(), _defaulted_document(),
                 _default_registration(), _default_process_with_change_fs_ticket())
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            [c.check for c in checks_manager.filter_failed_checks(cash_register.serial_number, {check})],
            equal_to([check])
        )


@pytest.mark.parametrize("cr_sn_parse_pdf, fs_sn_parse_pdf, failed", [
    (DEFAULT_NUMBER, "123", []),
    ("not a number", "not a number", [Checks.CASH_CARDS_OK]),
])
@mock.patch("yb_darkspirit.core.pdf_parsing._parse_pdf")
def test_cash_cards_ok_check(
        _parse_pdf_patch, session, checks_manager, doc_client_get_document,
        cr_sn_parse_pdf, fs_sn_parse_pdf, failed
):
    cash_register = _defaulted_cash_register()
    fiscal_storage = _defaulted_fiscal_storage(
        serial_number=fs_sn_parse_pdf if all((character.isdigit() for character in fs_sn_parse_pdf)) else DEFAULT_NUMBER
    )
    _store_to_db(session, cash_register, fiscal_storage, _defaulted_document(),
                 _default_registration(), _default_process_with_change_fs_ticket())

    class ReadDummy(object):
        def read(self):
            pass

    doc_client_get_document.return_value = {'Body': ReadDummy()}
    _parse_pdf_patch.return_value = {"sn": cr_sn_parse_pdf, "fn": fs_sn_parse_pdf}
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            [c.check for c in checks_manager.filter_failed_checks(cash_register.serial_number, {Checks.CASH_CARDS_OK})],
            equal_to(failed)
        )


@pytest.mark.parametrize("check,fiscal_storage", [
    (Checks.FISCAL_STORAGE_STATUS_IS_GOOD, _defaulted_fiscal_storage(status='ARCHIVE_READING')),
    (Checks.FISCAL_STORAGE_UNREGISTERED, _defaulted_fiscal_storage(state='overflow')),
    (Checks.EMPTY_FISCAL_STORAGE, _defaulted_fiscal_storage(last_document_number=5)),
])
def test_fiscal_storage_checks(session, checks_manager, check, fiscal_storage):
    # type: (Session, ChecksManager, Checks, scheme.FiscalStorage) -> None
    _store_to_db(session, _defaulted_cash_register(), fiscal_storage, _defaulted_document(),
                 _default_registration(), _default_process_with_change_fs_ticket())
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            [c.check for c in checks_manager.filter_failed_checks(DEFAULT_NUMBER, {check})],
            equal_to([check])
        )


@pytest.mark.parametrize("check,last_document_number,last_document_type", [
    (Checks.LAST_DOCUMENT_IS_CLOSE_FN, 1, 'Receipt'),
    (Checks.LAST_DOCUMENT_IS_CLOSE_FN, 1, 'ReRegistrationReport'),
    (Checks.ALL_DOCUMENTS_SYNCED, 14, 'CloseFNReport'),
])
def test_documents_checks_failed(session, checks_manager, check, last_document_number, last_document_type):
    # type: (Session, ChecksManager, Checks, int, str) -> None
    fiscal_storage = _defaulted_fiscal_storage(last_document_number=last_document_number)
    document = _defaulted_document(document_number=last_document_number, document_type=last_document_type)
    _store_to_db(session, _defaulted_cash_register(), fiscal_storage, document,
                 _default_registration(state=scheme.REGISTRATION_EXPIRED), _default_process_with_change_fs_ticket())
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            [c.check for c in checks_manager.filter_failed_checks(DEFAULT_NUMBER, {check})],
            equal_to([check])
        )


def test_documents_checks_ok(session, checks_manager):
    # type: (Session, ChecksManager) -> None
    checks = {Checks.LAST_DOCUMENT_IS_CLOSE_FN, Checks.ALL_DOCUMENTS_SYNCED}
    fiscal_storage = _defaulted_fiscal_storage(last_document_number=1)
    document = _defaulted_document(document_number=1, document_type="CloseFNReport")
    _store_to_db(session, _defaulted_cash_register(), fiscal_storage, document,
                 _default_registration(), _default_process_with_change_fs_ticket())
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            checks_manager.filter_failed_checks(DEFAULT_NUMBER, checks),
            empty()
        )



def test_registration_checks_ok(session, checks_manager):
    # type: (Session, ChecksManager) -> None
    registration = _default_registration(state="EXPIRED")
    _store_to_db(session, _defaulted_cash_register(), _defaulted_fiscal_storage(), _defaulted_document(),
                 registration, _default_process_with_change_fs_ticket())
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            checks_manager.filter_failed_checks(DEFAULT_NUMBER, {Checks.NO_ACTIVE_REGISTRATIONS}),
            empty()
        )


def test_registration_checks_fail(session, checks_manager):
    # type: (Session, ChecksManager) -> None
    check = Checks.NO_ACTIVE_REGISTRATIONS
    registration = _default_registration(state="REGISTERED")
    _store_to_db(session, _defaulted_cash_register(), _defaulted_fiscal_storage(), _defaulted_document(),
                 registration, _default_process_with_change_fs_ticket())
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            [c.check for c in checks_manager.filter_failed_checks(DEFAULT_NUMBER, {check})],
            equal_to([check])
        )


@pytest.mark.parametrize("documents_count,passed,registration_number", [
    (1, True, EXPECTED_RNM),
    (2, False, EXPECTED_RNM),
    (3, False, EXPECTED_RNM),
    (1, False, NOT_EXPECTED_RNM),  # OFD does not know about the registration
])
def test_ofd_check_ok(session, ofd_private_client, checks_manager, documents_count, passed, registration_number):
    # type: (Session, OfdPrivateClient, ChecksManager, int, bool, str) -> None
    ofd_private_client.check_cash_register_sync_status.side_effect = _mocked_check_cash_register_sync_status
    check = Checks.SYNCED_WITH_OFD
    registration = _default_registration(state="EXPIRED", registration_number=registration_number)
    fiscal_storage = _defaulted_fiscal_storage(last_document_number=documents_count)
    docs = [_defaulted_document(i) for i in range(documents_count)]
    _store_to_db(session, _defaulted_cash_register(), fiscal_storage, docs,
                 registration, _default_process_with_change_fs_ticket())
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            [c.check for c in checks_manager.filter_failed_checks(DEFAULT_NUMBER, {check})],
            empty() if passed else equal_to([check])
        )


def test_ofd_check_empty_fs(session, ofd_private_client, checks_manager):
    # type: (Session, OfdPrivateClient, ChecksManager) -> None
    ofd_private_client.check_cash_register_sync_status.side_effect = _mocked_check_cash_register_sync_status
    fiscal_storage = _defaulted_fiscal_storage(last_document_number=0)
    cash_register = _defaulted_cash_register()
    _store_to_db(session, cash_register, fiscal_storage, None, _default_registration(), _default_process_with_change_fs_ticket())

    old_fiscal_storage = scheme.FiscalStorage(serial_number='23456', type_code="fn-1")
    session.add(old_fiscal_storage)
    session.flush()
    old_registration = scheme.Registration(state='EXPIRED', fiscal_storage_id=old_fiscal_storage.id,
                                           cash_register_id=cash_register.id, registration_number=EXPECTED_RNM)
    session.add(old_registration)
    session.flush()

    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            checks_manager.filter_failed_checks(DEFAULT_NUMBER, {Checks.SYNCED_WITH_OFD}),
            empty()
        )


def _mocked_check_cash_register_sync_status(registration_number, fs_serial_number):
    if registration_number == EXPECTED_RNM and fs_serial_number == DEFAULT_FISCAL_SERIAL:
        return OfdSyncStatus(success_count=1)
    return OfdSyncStatus()


@pytest.mark.parametrize("failed_checks,expected_dict", [
    ([], {}),
    ([Checks.NONCONFIGURED, Checks.SYNCED_WITH_OFD, Checks.READY_TO_REGISTER], {
        'CLEAR_DEVICE': [],
        'MAKE_READY_TO_REGISTRATION': ['NONCONFIGURED'],
    }),
    ([Checks.FRESH_SOFTWARE, Checks.READY_TO_REGISTER], {
        'MAKE_READY_TO_REGISTRATION': ['FRESH_SOFTWARE'],
    }),
    ([Checks.EMPTY_FISCAL_STORAGE, Checks.FRESH_SOFTWARE, Checks.FISCAL_STORAGE_UNREGISTERED, Checks.READY_TO_REGISTER],
     {
         'CHANGE_FS': ['FRESH_SOFTWARE'],
         'MAKE_READY_TO_REGISTRATION': ['EMPTY_FISCAL_STORAGE', 'FRESH_SOFTWARE', 'FISCAL_STORAGE_UNREGISTERED'],
     }),
    (list(Checks), {
        "MAKE_READY_TO_REGISTRATION": [
            'HAS_WHITESPIRIT_URL', 'FRESH_SOFTWARE', 'HAS_ADMIN_PASSWORD',
            'HAS_OEBS_ADDRESS_CODE', 'NO_ACTIVE_REGISTRATIONS', 'NOT_BUG', 'FRESH_DATA_IN_DB',
            'FISCAL_STORAGE_UNREGISTERED', 'FISCAL_STORAGE_STATUS_IS_GOOD',
            'EMPTY_FISCAL_STORAGE', 'NONCONFIGURED', 'HAS_SSH_KEY_ACCESS', 'FNSREG_NO_ACTIVE_REGISTRATION',
            'ONLINE', 'ACTUAL_DATETIME', 'SUPPORTED_FISCAL_STORAGE_MODEL'
        ],
        "SYNC_DOCUMENTS": [
            'HAS_ADMIN_PASSWORD', 'NOT_BUG', 'FRESH_SOFTWARE',
            'HAS_OEBS_ADDRESS_CODE', 'CLOSED_FS', 'FRESH_DATA_IN_DB', 'HAS_WHITESPIRIT_URL',
            'NO_ACTIVE_REGISTRATIONS', 'FNSREG_NO_ACTIVE_REGISTRATION', 'ONLINE',
        ],
        "CHANGE_FS": [
            'HAS_ADMIN_PASSWORD', 'NOT_BUG', 'LAST_DOCUMENT_IS_CLOSE_FN',
            'FRESH_SOFTWARE', 'ALL_DOCUMENTS_SYNCED', 'SYNCED_WITH_OFD', 'HAS_OEBS_ADDRESS_CODE', 'CLOSED_FS',
            'FRESH_DATA_IN_DB', 'HAS_WHITESPIRIT_URL', 'NO_ACTIVE_REGISTRATIONS', 'FNSREG_NO_ACTIVE_REGISTRATION',
            'ONLINE',
        ],
        "SET_DATETIME": [
            'HAS_ADMIN_PASSWORD', 'NOT_BUG', 'HAS_WHITESPIRIT_URL', 'FRESH_SOFTWARE', 'ONLINE', 'HAS_OEBS_ADDRESS_CODE',
            'FRESH_DATA_IN_DB', 'FNSREG_NO_ACTIVE_REGISTRATION', 'NO_ACTIVE_REGISTRATIONS'
        ],
        "CLEAR_DEVICE": [
            'EMPTY_FISCAL_STORAGE', 'HAS_ADMIN_PASSWORD', 'NOT_BUG',
            'FISCAL_STORAGE_UNREGISTERED', 'FISCAL_STORAGE_STATUS_IS_GOOD', 'FRESH_SOFTWARE', 'HAS_OEBS_ADDRESS_CODE',
            'CLOSED_FS', 'FRESH_DATA_IN_DB', 'HAS_WHITESPIRIT_URL', 'NO_ACTIVE_REGISTRATIONS', 'FS_TICKET_CLOSED',
            'FNSREG_NO_ACTIVE_REGISTRATION', 'ONLINE', 'SUPPORTED_FISCAL_STORAGE_MODEL'
        ],
        "TURN_OFF_LED": [
            'HAS_WHITESPIRIT_URL', 'FRESH_SOFTWARE', 'HAS_ADMIN_PASSWORD',
            'HAS_OEBS_ADDRESS_CODE', 'NO_ACTIVE_REGISTRATIONS', 'NOT_BUG', 'FRESH_DATA_IN_DB',
            'FISCAL_STORAGE_UNREGISTERED', 'FISCAL_STORAGE_STATUS_IS_GOOD',
            'EMPTY_FISCAL_STORAGE', 'CLOSED_FS', 'FS_TICKET_CLOSED', 'FNSREG_NO_ACTIVE_REGISTRATION',
            'ONLINE', 'SUPPORTED_FISCAL_STORAGE_MODEL'
        ],
    }),
])
def test_failed_checks_dict(session, failed_checks, expected_dict):
    # type: (Session, List[Checks], Dict[str, List[str]]) -> None
    real_dict_checks = prepare_failed_checks_dict(
        [FailedCheck(check, FailedCheckResult('')) for check in failed_checks],
        PrepareUnregisteredCashToRegistrationProcess.actions(),
        checks_for_ds_state(
            PrepareUnregisteredCashToRegistrationProcess.expected_ds_state()
        )
    )
    real_dict = {
        action: [c.check.name for c in checks]
        for action, checks in real_dict_checks.items()
    }
    assert_that(real_dict, has_entries({
        k: contains_inanyorder(*v) for k, v in expected_dict.items()
    }))
    assert_that(expected_dict, has_entries({
        k: contains_inanyorder(*v) for k, v in real_dict.items()
    }))


def _lists_to_sets(dict_of_lists):
    return {k: set(v) for k, v in dict_of_lists.items()}


def _store_to_db(session, cash_register, fiscal_storage, document, registration, process):
    # type: (Session, scheme.CashRegister, scheme.FiscalStorage, Union[scheme.Document, List[scheme.Document], None], scheme.Registration, scheme.CashRegisterProcess) -> None
    real_update_dt = cash_register.update_dt

    session.add(fiscal_storage)
    session.flush()

    cash_register.fiscal_storage = fiscal_storage
    session.add(cash_register)
    session.flush()

    if registration is not None:
        registration.cash_register = cash_register
        registration.fiscal_storage = fiscal_storage
        session.add(registration)
        session.flush()

    if isinstance(document, list):
        docs = document
    else:
        docs = [document]
    for doc in docs:
        if doc is None:
            continue
        doc.fiscal_storage = fiscal_storage
        session.add(doc)
        session.flush()

    process.cash_register = cash_register
    session.add(process)
    session.flush()

    session.query(scheme.CashRegister) \
        .filter(scheme.CashRegister.id == cash_register.id) \
        .update({scheme.CashRegister.update_dt: real_update_dt})


@pytest.mark.parametrize('ticket,ticket_status,failed_checks', [
    (None, 'closed', set()),
    (TICKET, 'closed', {Checks.HAS_NO_FS_TICKET}),
    (TICKET, 'opened', {Checks.HAS_NO_FS_TICKET, Checks.FS_TICKET_CLOSED}),
])
def test_tracker(session, rsps, ticket, ticket_status, failed_checks):
    # type: (Session, responses.RequestsMock, str, str, list) -> None
    ticket_checks = {Checks.FS_TICKET_CLOSED, Checks.HAS_NO_FS_TICKET}
    _set_issue_status_mock(rsps, ticket_status)
    _store_to_db(session, _defaulted_cash_register(), _defaulted_fiscal_storage(),
                 _defaulted_document(), _default_registration(), _default_process_with_change_fs_ticket(ticket=ticket))
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            set(map(lambda c: c.check,
                ChecksManager(
                    session, Mock(), TrackerClient.from_app(getApplication()), Mock(), Mock()
                ).filter_failed_checks(DEFAULT_NUMBER, ticket_checks)
            )),
            equal_to(failed_checks)
        )


def _set_issue_status_mock(rsps, status):
    rsps.remove(responses.GET, 'https://st-api.test.yandex-team.ru/v2/issues/{ticket}'.format(ticket=TICKET))
    rsps.add(
        responses.GET,
        url='https://st-api.test.yandex-team.ru/v2/issues/{ticket}'.format(ticket=TICKET),
        json={"self": "", "status": {"self": "", "key": status}},
    )
