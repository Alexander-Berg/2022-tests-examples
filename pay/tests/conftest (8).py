# -*- encoding: utf-8 -*-
import csv
from os.path import (
    abspath,
    dirname,
    join,
)
import threading
import json
import StringIO

from contextlib2 import nullcontext
from mock import PropertyMock

import responses

from yb_darkspirit.core.cash_register.maintenance_actions import ChangeFSAction, MaintenanceAction, \
    IssueAfterFSChangeAction, CheckOnlineAfterIssueAction, SetDatetimeAction, WaitForIssueAfterFSTicketClosedAction, \
    WaitForFSTicketClosedAction, ClearDeviceReregistrationAction, ReregisterAction, ConfigureAction, \
    NewReregistrationAction
from yb_darkspirit.interactions import DocumentsClient, FnsregClient
from yb_darkspirit.interactions.solomon import BackgroundTaskMetricCollector
from .interaction_mocks import *

from butils.application import Application as ApplicationBase
from hamcrest import assert_that, is_
from sqlalchemy.engine import Connection
from typing import Iterable
from mock import Mock

from yb_darkspirit import interface, scheme, tracing
from yb_darkspirit.application.plugins.dbhelper import Session
from yb_darkspirit.interface import CashRegister
from yb_darkspirit.scheme import WhiteSpirit
from yb_darkspirit.servant import app
from yb_darkspirit.interactions.ofd import OfdPrivateClient
from yb_darkspirit.interactions.tvm import TvmManagerHolder
from yb_darkspirit.application import Environment
from yb_darkspirit.core.cash_register.state_checks import ChecksManager, Checks, CheckResult
from .processes_testing_instances import TestMaintenanceAction, TestProcess

from .whitespirit_mocks import WhiteSpiritMocks, add_document_to_cr, make_document, set_cr_state, mock_cr_configure
from . import ws_responses

try:
    from yb_darkspirit.version import __version__ as VERSION
except ImportError:
    VERSION = '0.0.0-noversion'


class TestApplication(ApplicationBase):
    database_id = "darkspirit"
    __version__ = VERSION
    static_plugins = [
        ("yb_darkspirit.application.plugins.dbhelper", None),
        ("butils.application.plugins.secrets_cfg", None),
    ]

    def new_session(self, *args, **kwargs):
        # сессию туда добавляет фикстура session с autouse=True
        return threading.current_thread().test_session


def pytest_addoption(parser):
    parser.addoption(
        "--application-config-file", action="store", default="application-test.cfg.xml"
    )


@pytest.fixture(scope="session")
def app_config_file(request):
    return request.config.getoption("--application-config-file")


@pytest.fixture(scope="session", autouse=True)
def tvm_manager_mock():
    tvm_manager = Mock()
    tvm_manager.get_ticket.return_value = 'fake_ticket'
    return tvm_manager


@pytest.fixture(scope="session", autouse=True)
def application(app_config_file, tvm_manager_mock):
    # type: (str, Mock) -> TestApplication
    current_dir = dirname(abspath(__file__))
    cfg_path = join(current_dir, app_config_file)
    application = TestApplication(cfg_path)
    TvmManagerHolder.tvm_manager = tvm_manager_mock
    return application


@pytest.fixture(scope="session")
def cr_sn():
    return "381002827554"


@pytest.fixture(scope="session")
def yandex_ofd_inn():
    return "7704358518"


@pytest.fixture(scope="session")
def yandex_taxi_firm_inn():
    return "7704340310"


@pytest.fixture
def ws_url():
    return "https://whitespirit-dev1f.balance.os.yandex.net:8080"


@pytest.fixture(autouse=True)
def ws(session, ws_url):
    whitespirit = WhiteSpirit(url=ws_url, version='1.0.304')  # use random string as version
    session.add(whitespirit)
    session.flush()


@pytest.fixture(scope="session")
def connection(application):
    # type: (TestApplication) -> Connection
    engine = application.get_dbhelper(
        database_id=application.database_id
    ).engines[0]
    connection = engine.connect()
    yield connection
    connection.close()


@pytest.fixture(scope="function")
def session(connection, application):
    # type: (Connection, TestApplication) -> Session
    sessionmaker = application.get_dbhelper(
        database_id=application.database_id
    ).sessionmakers[0]

    transaction = connection.begin()
    session = sessionmaker(bind=connection)
    session.execute("ALTER SESSION SET TIME_ZONE = 'Europe/Moscow'")

    threading.current_thread().test_session = session
    session.clone = lambda: session

    yield session

    transaction.rollback()
    session.close()


@pytest.fixture(scope="function")
def another_session(connection, application):
    # type: (Connection, TestApplication) -> Session
    sessionmaker = application.get_dbhelper(
        database_id=application.database_id
    ).sessionmakers[0]

    transaction = connection.begin()
    session = sessionmaker(bind=connection)

    session.clone = lambda: session

    yield session

    transaction.rollback()
    session.close()


@pytest.fixture(scope="session")
def another_connection(application):
    # type: (TestApplication) -> Connection
    engine = application.get_dbhelper(
        database_id=application.database_id
    ).engines[0]
    connection = engine.connect()
    yield connection
    connection.close()


@pytest.fixture(scope="function")
def commitable_session(another_connection, application):
    # type: (Connection, TestApplication) -> Session
    sessionmaker = application.get_dbhelper(
        database_id=application.database_id
    ).sessionmakers[0]

    session = sessionmaker(bind=another_connection)
    session.clone = lambda: session

    yield session
    session.close()


@pytest.fixture(scope="function")
def committed_objects(commitable_session):
    def clean_up(commitable_session, serial_numbers):
        cash_register_conflicts = (
            commitable_session.query(scheme.CashRegister)
                .filter(scheme.CashRegister.serial_number.in_(serial_numbers))
                .all()
        )  # type: Iterable[scheme.CashRegister]
        for cash_register_conflict in cash_register_conflicts:
            commitable_session.delete(cash_register_conflict.current_process)
            commitable_session.delete(cash_register_conflict)
        commitable_session.flush()

    serial_numbers = ['12345', '12346']
    with commitable_session.begin():
        clean_up(commitable_session, serial_numbers)

        process_ids = []
        for serial_number in serial_numbers:
            cash_register = scheme.CashRegister(serial_number=serial_number)
            commitable_session.add(cash_register)
            commitable_session.flush()

            cash_register_process = scheme.CashRegisterProcess(
                cash_register_id=cash_register.id,
                process='test_process',
                stage='42'
            )
            commitable_session.add(cash_register_process)
            commitable_session.flush()

            process_ids.append(cash_register_process.id)

    yield process_ids

    with commitable_session.begin():
        clean_up(commitable_session, serial_numbers)


@pytest.fixture
def rsps():
    # type: () -> responses.RequestsMock
    """
    Не проверяем запуск всех замоканных запросов, так как для консистентности ответов WS между собой
    можем замокать лишнее.
    """
    with responses.RequestsMock(
            assert_all_requests_are_fired=False,
            passthru_prefixes=('https://st-api.test.yandex-team.ru',),
    ) as rsps_:
        yield rsps_


@pytest.fixture
def ws_mocks(ws_url, rsps, session):
    # type: (str, responses.RequestsMock, Session) -> WhiteSpiritMocks
    return WhiteSpiritMocks(ws_url, rsps, session, cashmashines_response_kwargs=dict(
        json=ws_responses.CASHMACHINES,
        content_type="application/json",
    ))


@pytest.fixture
def cr_wrapper_maker(session, ws_mocks, test_client, response_maker):
    return CrWrapperMaker(session, ws_mocks, test_client, response_maker)


@pytest.fixture
def response_maker(ws_mocks, test_client, yandex_taxi_firm_inn, yandex_ofd_inn):
    return ResponseMaker(ws_mocks, test_client, yandex_taxi_firm_inn, yandex_ofd_inn)


@pytest.fixture(autouse=True)
def oebs_address_mock():
    with patch.object(CashRegister, "oebs_cash_register_address",
                      PropertyMock(return_value=ws_responses.CR_ADDRESS_CODE)):
        yield


class ResponseMaker(object):
    def __init__(self, ws_mocks, test_client, yandex_taxi_firm_inn, yandex_ofd_inn):
        self.ws_mocks = ws_mocks
        self.test_client = test_client
        self.yandex_taxi_firm_inn = yandex_taxi_firm_inn
        self.yandex_ofd_inn = yandex_ofd_inn

    def create_applications_response(self, cr_wrapper, is_bso_kkt=False):
        data = {
            "firm_inn": self.yandex_taxi_firm_inn,
            "ofd_inn": self.yandex_ofd_inn,
            "serial_numbers": [cr_wrapper.serial_number],
            "is_bso_kkt": is_bso_kkt,
        }

        response = self.test_client.post(
            "/v1/registrations/create-applications",
            data=json.dumps(data),
            content_type="application/json",
        )
        assert_that(response.status_code, is_(200), response.data)
        return response

    @staticmethod
    def _csv_memory_file(cr_wrapper):
        csv_memory_file = StringIO.StringIO()
        csv_writer = csv.writer(csv_memory_file, delimiter=";", )
        csv_writer.writerow(
            map(lambda s: s.encode("cp1251"), [
                u"Код НО", u"Дата регистрации ККТ в НО", u"Адрес места установки",
                u"Регистрационный номер", u"Модель", u"Срок окончания действия ФН",
                u"Состояние", u"Заводской номер ККТ", u"Наименование ОФД",
                u"Заводской номер ФН"
            ])
        )
        csv_writer.writerow(
            map(lambda s: s.encode("cp1251"), [
                u"9965", u"", u"141004,50,,Мытищи,,Силикатная,19,,",
                u"1709626002105",
                u"РП Система 1ФС", u"", u"Присвоен регистрационный номер ККТ",
                cr_wrapper.serial_number,
                u"Общество с ограниченной ответственностью «Яндекс.ОФД»",
                cr_wrapper.fiscal_storage.serial_number,
            ])
        )
        csv_memory_file.seek(0)
        return csv_memory_file

    def configure_response(self, cr_wrapper, is_bso_kkt=False):
        response = self.test_client.post(
            "/v1/registrations/configure",
            data={"file": (self._csv_memory_file(cr_wrapper), "test.csv")}
        )
        assert_that(response.status_code, is_(200), response.data)
        return response

    def register_response(self, cr_wrapper, is_bso_kkt=False):
        self.configure_response(cr_wrapper, is_bso_kkt)
        mock_cr_configure(self.ws_mocks, cr_wrapper.long_serial_number)
        registration_report = (ws_responses.BSO_REGISTRATION_REPORT
                               if is_bso_kkt
                               else ws_responses.DEFAULT_REGISTRATION_REPORT)
        registration_document = make_document(registration_report, cr_wrapper.last_document_number)
        add_document_to_cr(registration_document, self.ws_mocks, cr_wrapper.long_serial_number)
        self.ws_mocks.cashmachines_register(
            cr_long_sn=cr_wrapper.long_serial_number,
            json=registration_document,
            content_type="application/json",
        )

        data = {
            "serial_numbers": [cr_wrapper.serial_number],
        }
        response = self.test_client.post(
            "/v1/registrations/register",
            data=json.dumps(data),
            content_type="application/json",
        )
        assert_that(response.status_code, is_(200), response.data)
        return response

    def create_fiscalization_xml_response(self, cr_wrapper):
        data = {
            "serial_numbers": [cr_wrapper.serial_number],
        }

        response = self.test_client.post(
            "/v1/registrations/create-fiscalization-xml",
            data=json.dumps(data),
            content_type="application/json"
        )
        assert_that(response.status_code, is_(200), response.data)
        return response

    def reregister_batch_response(self, cr_wrapper, is_bso_kkt=False):
        # TODO: check whether tests commit something in DB
        reregistration_report = (ws_responses.BSO_REREGISTRATION_REPORT
                                 if is_bso_kkt
                                 else ws_responses.DEFAULT_REREGISTRATION_REPORT)
        reregistration_document = make_document(reregistration_report, cr_wrapper.last_document_number)
        add_document_to_cr(reregistration_document, self.ws_mocks, cr_wrapper.long_serial_number)
        self.ws_mocks.cashmachines_register(
            cr_long_sn=cr_wrapper.long_serial_number,
            reset=True,
            json=reregistration_document,
            content_type="application/json",
        )
        data = {
            "serial_numbers": [cr_wrapper.serial_number],
        }
        response = self.test_client.post(
            "/v1/reregistrations/reregister-batch",
            data=json.dumps(data),
            content_type="application/json",
        )
        assert_that(response.status_code, is_(200), response.data)
        return response


class CrWrapperMaker(object):
    def __init__(self, session, ws_mocks, test_client, response_maker):
        self.session = session
        self.ws_mocks = ws_mocks
        self.test_client = test_client
        self.response_maker = response_maker

    def default(self):
        # type: () -> CashRegister
        cr_wrapper = self.ws_mocks.cr_wrappers[ws_responses.CR_LONG_SN]
        cr_wrapper.admin_password = "666666"
        cr_wrapper.cash_register.ds_state = scheme.DsCashState.OK.value
        interface.global_metadata = None
        return cr_wrapper

    def with_test_process(self):
        cr_wrapper = self.default()
        cr_wrapper.cash_register.target_groups = cr_wrapper.cash_register.current_groups[:]
        with self.session.begin():
            self.session.add(TestProcess.create_cash_register_process(cr_wrapper.cash_register))
        return cr_wrapper

    def with_test_process_with_registration(self):
        cr_wrapper = self.with_completed_registration()
        with self.session.begin():
            self.session.add(TestProcess.create_cash_register_process(cr_wrapper.cash_register))
        return cr_wrapper

    def offline(self):
        cr_wrapper = self.default()
        self.ws_mocks.cashmachines_status(
            cr_long_sn=cr_wrapper.long_serial_number,
            status=404,
            json={
                "error": "NotFoundSN",
                "value": "",
            },
            do_sync=False,
            content_type="application/json"
        )
        return cr_wrapper

    def hidden(self):
        cr_wrapper = self.default()
        self.ws_mocks.cashmachines_status(
            cr_long_sn=cr_wrapper.whitespirit_key,
            json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
        )
        mock_cr_configure(self.ws_mocks, cr_wrapper.long_serial_number)
        response = self.test_client.patch(
            '/v1/cash-registers/{0}'.format(cr_wrapper.cash_register_id),
            data=json.dumps({
                'hidden': True,
            }),
            content_type="application/json"
        )
        assert_that(response.status_code, is_(200), response.data)
        return cr_wrapper

    def nonconfigured_fs_ready_fiscal(self, is_bso_kkt=False):
        cr_wrapper = self.default()
        self.ws_mocks.cashmachines_status(
            cr_long_sn=cr_wrapper.long_serial_number,
            json=(ws_responses.CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_BSO_GOOD
                  if is_bso_kkt
                  else ws_responses.CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD),
            content_type="application/json",
        )
        self.ws_mocks.cashmachines_set_datetime(cr_long_sn=cr_wrapper.long_serial_number)
        return cr_wrapper

    def ready_to_registration(self, is_bso_kkt=False):
        cr_wrapper = self.nonconfigured_fs_ready_fiscal(is_bso_kkt)
        self.ws_mocks.cashmachines_status(
            cr_long_sn=cr_wrapper.long_serial_number,
            json=(ws_responses.CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_BSO_GOOD
                  if is_bso_kkt
                  else ws_responses.CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD),
            content_type="application/json",
        )
        self.ws_mocks.cashmachines_set_datetime(cr_long_sn=cr_wrapper.long_serial_number)
        self.ws_mocks.cashmachines_ssh_ping(cr_long_sn=cr_wrapper.whitespirit_key, use_password=True)
        self.ws_mocks.cashmachines_ssh_ping(cr_long_sn=cr_wrapper.whitespirit_key, use_password=False)
        response = self.test_client.post(
            '/v1/cash-registers/{0}/change_state'.format(cr_wrapper.cash_register_id),
            data=json.dumps({
                'target_state': scheme.DsCashState.READY_TO_REGISTRATION.value,
                'skip_push_to_cash_register': True,
                'reason': 'test',
                'skip_checks': False
            }),
            content_type="application/json"
        )
        assert_that(response.status_code, is_(200), response.data)
        return cr_wrapper

    def _with_state(self, cr_wrapper, state, ws_response, skip_checks=False):
        self.ws_mocks.cashmachines_status(
            cr_long_sn=cr_wrapper.whitespirit_key,
            json=ws_response,
            do_sync=True,
        )
        cr_wrapper.session.flush()
        mock_cr_configure(self.ws_mocks, cr_wrapper.long_serial_number)
        self.ws_mocks.cashmachines_ssh_ping(cr_long_sn=cr_wrapper.whitespirit_key, use_password=True)
        self.ws_mocks.cashmachines_ssh_ping(cr_long_sn=cr_wrapper.whitespirit_key, use_password=False)
        response = self.test_client.post(
            '/v1/cash-registers/{0}/change_state'.format(cr_wrapper.cash_register_id),
            data=json.dumps({
                'target_state': state,
                'skip_push_to_cash_register': True,
                'reason': 'test',
                'skip_checks': skip_checks
            }),
            content_type="application/json"
        )
        assert_that(response.status_code, is_(200), response.data)
        return response

    def unregistered(self):
        cr_wrapper = self.default()
        response = self._with_state(cr_wrapper,
                                    scheme.DsCashState.UNREGISTERED.value,
                                    ws_responses.CASHMACHINES_STATUS_CR_POSTFISCAL_FS_ARCHIVE_READING)
        assert_that(response.status_code, is_(200), response.data)
        return cr_wrapper

    def ok(self):
        cr_wrapper = self.with_completed_registration()
        with patch.object(Checks, '__call__', _checks_call_patch):
            response = self._with_state(cr_wrapper,
                                        scheme.DsCashState.OK.value,
                                        ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD)
        assert_that(response.status_code, is_(200), response.data)
        return cr_wrapper

    def with_new_registration(self, is_bso_kkt=False):
        cr_wrapper = self.nonconfigured_fs_ready_fiscal(is_bso_kkt)
        self.response_maker.create_applications_response(cr_wrapper, is_bso_kkt)
        return cr_wrapper

    def with_ready_registration(self, is_bso_kkt=False):
        cr_wrapper = self.with_new_registration(is_bso_kkt)
        self.response_maker.configure_response(cr_wrapper, is_bso_kkt)
        return cr_wrapper

    def with_completed_registration(self, is_bso_kkt=False):
        cr_wrapper = self.with_ready_registration(is_bso_kkt)
        self.response_maker.register_response(cr_wrapper, is_bso_kkt)
        return cr_wrapper

    def with_created_fiscalization_xml_response(self, is_bso_kkt=False):
        cr_wrapper = self.with_completed_registration(is_bso_kkt)
        self.response_maker.create_fiscalization_xml_response(cr_wrapper)
        return cr_wrapper

    def with_expired_registration(self, is_bso_kkt=False):
        cr_wrapper = self.with_created_fiscalization_xml_response(is_bso_kkt)
        # TODO: replace this with close_shift
        close_fn_report_document = make_document(ws_responses.CLOSE_FN_REPORT, cr_wrapper.last_document_number)
        add_document_to_cr(close_fn_report_document, self.ws_mocks, cr_wrapper.long_serial_number)
        cr_wrapper.pull_document(close_fn_report_document['id'])
        return cr_wrapper

    def postfiscal(self, is_bso_kkt=False):
        cr_wrapper = self.with_expired_registration(is_bso_kkt)
        set_cr_state(
            self.ws_mocks,
            cr_wrapper.long_serial_number,
            scheme.CASH_REGISTER_STATE_POSTFISCAL,
            scheme.FISCAL_STORAGE_ARCHIVE_READING,
        )
        return cr_wrapper

    def postfiscal_with_gaps(self, is_bso_kkt=False):
        cr_wrapper = self.with_created_fiscalization_xml_response(is_bso_kkt)
        # TODO: replace this with close_shift
        document_number = cr_wrapper.last_document_number + 1  # Mind the gap
        close_fn_report_document = make_document(ws_responses.CLOSE_FN_REPORT, document_number)
        add_document_to_cr(close_fn_report_document, self.ws_mocks, cr_wrapper.long_serial_number)
        cr_wrapper.pull_document(close_fn_report_document['id'])

        set_cr_state(
            self.ws_mocks,
            cr_wrapper.long_serial_number,
            scheme.CASH_REGISTER_STATE_POSTFISCAL,
            scheme.FISCAL_STORAGE_ARCHIVE_READING,
        )
        return cr_wrapper

    def ready_for_reregister(self, is_bso_kkt=False):
        cr_wrapper = self.with_expired_registration(is_bso_kkt)
        self.response_maker.reregister_batch_response(cr_wrapper, is_bso_kkt)
        return cr_wrapper

    def fatal_error_after_change_fs(self, is_bso_kkt=False):
        cr_wrapper = self.with_expired_registration(is_bso_kkt)
        response = self._with_state(cr_wrapper,
                                    scheme.DsCashState.READY_TO_REREGISTRATION.value,
                                    ws_responses.CASHMACHINES_STATUS_CR_FATAL_ERROR_SECOND_FS_FISCAL_GOOD,
                                    skip_checks=True
                                    )
        assert_that(response.status_code, is_(200), response.data)
        return cr_wrapper

    def ready_to_reregister_nonconfigured_fs_ready(self, is_bso_kkt=False):
        cr_wrapper = self.with_expired_registration(is_bso_kkt)
        self.ws_mocks.cashmachines_status(
            cr_long_sn=cr_wrapper.long_serial_number,
            json=(ws_responses.CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_BSO_GOOD
                  if is_bso_kkt
                  else ws_responses.CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD),
            content_type="application/json",
        )
        self.ws_mocks.cashmachines_set_datetime(cr_long_sn=cr_wrapper.long_serial_number)
        return cr_wrapper


def _checks_call_patch(self, info):
    if self == Checks.CASH_CARDS_OK:
        return CheckResult()
    else:
        return self.value(info)


@pytest.fixture(scope="function")
def cr_wrapper(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    yield cr_wrapper_maker.default()


@pytest.fixture(scope="function")
def cr_wrapper_with_test_process(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    yield cr_wrapper_maker.with_test_process()


@pytest.fixture(scope="function")
def cr_wrapper_with_test_process_with_registration(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    yield cr_wrapper_maker.with_test_process_with_registration()


@pytest.fixture
def cr_wrapper_offline(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.offline()


@pytest.fixture
def cr_wrapper_hidden(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.hidden()


@pytest.fixture
def cr_wrapper_ok(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.ok()


@pytest.fixture
def cr_wrapper_nonconfigured_fs_ready_fiscal(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.nonconfigured_fs_ready_fiscal()


@pytest.fixture
def cr_wrapper_ready_to_registration(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.ready_to_registration()


@pytest.fixture
def test_client():
    with app.test_client() as test_client:
        yield test_client


@pytest.fixture
def cr_wrapper_unregistered(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.unregistered()


@pytest.fixture
def cr_wrapper_with_ready_registration(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.with_ready_registration()


@pytest.fixture
def cr_wrapper_with_completed_registration(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.with_completed_registration()


@pytest.fixture
def cr_wrapper_postfiscal(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.postfiscal()


@pytest.fixture
def cr_wrapper_with_expired_registration(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.with_expired_registration()


@pytest.fixture
def cr_wrapper_with_created_fiscalization_xml_response(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.with_created_fiscalization_xml_response()


@pytest.fixture
def cr_wrapper_postfiscal_with_gaps(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.postfiscal_with_gaps()


@pytest.fixture
def cr_wrapper_postfiscal(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.postfiscal()


@pytest.fixture
def cr_wrapper_ready_for_reregistration(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.ready_for_reregister()


@pytest.fixture
def cr_wrapper_fatal_error_after_change_fs(cr_wrapper_maker):
    # type: (CrWrapperMaker) -> CashRegister
    return cr_wrapper_maker.fatal_error_after_change_fs()


@pytest.fixture
def reregister_entry(
        response_maker,
        cr_wrapper_with_expired_registration,
        session,
):
    """
    :rtype: scheme.Registration
    """
    response_maker.reregister_batch_response(cr_wrapper_with_expired_registration)
    return (
        session.query(scheme.Registration)
            .filter_by(cash_register_id=cr_wrapper_with_expired_registration.cash_register_id,
                       state=scheme.REGISTRATION_COMPLETE)
            .one()
    )


@pytest.fixture
def dev_ofd_private_client():
    return OfdPrivateClient(Environment.DEV)


@pytest.fixture
def check_if_ready_for_actions():
    with patch.object(ChecksManager, 'check_if_ready_for_actions') as mock:
        yield mock


@pytest.fixture
def filter_failed_checks():
    with patch.object(ChecksManager, 'filter_failed_checks') as mock:
        yield mock


@pytest.fixture
def doc_client_get_document():
    with patch.object(DocumentsClient, 'get_document') as mock:
        yield mock


@pytest.fixture
def get_or_create_ticket_issue_after_change_fs():
    with patch.object(IssueAfterFSChangeAction, 'get_or_create_ticket') as mock:
        yield mock


@pytest.fixture
def need_wait_fs():
    with patch.object(IssueAfterFSChangeAction, 'wait_for_') as mock:
        yield mock


@pytest.fixture
def need_wait_issue():
    with patch.object(CheckOnlineAfterIssueAction, 'wait_for_') as mock:
        yield mock


@pytest.fixture
def maintenance_test_action_apply_on_cash_register():
    with patch.object(TestMaintenanceAction, 'apply_on_cash_register') as mock:
        yield mock


@pytest.fixture
def maintenance_test_action_need_wait():
    with patch.object(TestMaintenanceAction, 'wait_for_') as mock:
        yield mock


@pytest.fixture
def maintenance_test_action_apply_if_skipped():
    with patch.object(TestMaintenanceAction, 'apply_if_skipped') as mock:
        yield mock


@pytest.fixture
def get_rereg_application_upload_issue_key_fixture():
    with patch('yb_darkspirit.core.cash_register.maintenance_actions.get_rereg_application_upload_issue_key') as mock:
        yield mock


@pytest.fixture
def pull_document():
    with patch.object(CashRegister, 'pull_document') as mock:
        yield mock


@pytest.fixture
def get_missing_document_numbers():
    with patch.object(CashRegister, 'get_missing_document_numbers') as mock:
        yield mock


@pytest.fixture
def disabled_metrics_subprocess_collect():
    with patch.object(BackgroundTaskMetricCollector, 'collect_subprocess') as mock:
        mock.return_value = nullcontext()
        yield mock


@pytest.fixture
def fnsreg_get_status_mock():
    with patch.object(FnsregClient, '_get_status') as mock:
        yield mock
