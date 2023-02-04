# -*- coding: utf-8 -*-

# Turn sqlalchemy warnings into exceptions by default.
# Here we hasn't yet imported modules, where we apply out filters,
# so it is safe to insert this rule to the beginning of the filters list.
import warnings
warnings.filterwarnings('error', module='sqlalchemy.*')

try:
    import balance.usercustomize
except:
    pass


import datetime as dt
from contextlib import contextmanager

import pytest
import mock
import httpretty

from butils import logger

import base
from balance.constants import TVM2_SERVICE_TICKET_HEADER
from balance.constants import BALANCE_META_DATABASE_ID
import balance.muzzle_util as ut


log = logger.get_logger()

# Temporary skip stager tests: BALANCE-29895
collect_ignore = ['stager_tests']


@pytest.fixture(scope='session', autouse=True)
def app():
    app = base.init_app()
    base.init_sa_logging()
    base.init_sderr_logging()

    # mocks
    from balance import mncloselib
    mncloselib.is_monthly_acts_done = lambda x: True

    from balance import mapper
    mapper.Job.next_dt = property(lambda s: dt.datetime.now() + dt.timedelta(1))

    return app


@contextmanager
def create_session(request, app, modular=False):
    base.clear_threading_local(not_clear_list=['test_meta_session'])
    session = base.init_session(app)
    assert session

    if not modular and request.cls is not None:
        request.cls.session = session

    yield session

    if not modular and request.cls is not None:
        del request.cls.session

    log.debug('clear session: %s' % session)
    session.rollback()
    session.close()
    base.clear_threading_local()


@contextmanager
def create_meta_session(request, app, modular=False):
    base.clear_threading_local(not_clear_list=['test_session'])
    session = base.init_session(app, database_id=BALANCE_META_DATABASE_ID)
    assert session

    if not modular and request.cls is not None:
        request.cls.meta_session = session

    yield session

    if not modular and request.cls is not None:
        del request.cls.meta_session

    log.debug('clear session: %s' % session)
    session.rollback()
    session.close()
    base.clear_threading_local()


# Сессии, время жизни - 1 тест.
@pytest.fixture
def session(request, app):
    """
    Такой странный код приходится писать,
    потому что в pytest.fixture функцию можно обернуть только один раз,
    т.к. pytest сохраняет в объект функции переданные в вызов fixture параметры.
    То есть, если обернуть функцию create_session два раза,
    то у всех фикстур будут приеняться параметры последней фикстуры, пример:
    session = pytest.fixture(create_session)
    modular_session = pytest.fixture(scope="module")(create_session)
    В этом случае у session scope будет = "module".
    """
    with create_session(request, app) as s:
        yield s


@pytest.fixture
def meta_session(request, app):
    with create_meta_session(request, app) as s:
        yield s


# Сессия, время жизни - модуль.
@pytest.fixture(scope="module")
def modular_session(request, app):
    """ смотрите docstring session """
    with create_session(request, app, modular=True) as s:
        yield s


@pytest.fixture()
def medium_instance(app, session):
    with base.prepared_dispatcher(
            app=app,
            factory_callback=base.create_medium_dispatcher,
            session=session,
    ) as (port, dispatcher):
        yield port, dispatcher


@pytest.fixture()
def takeout_instance(app, session):
    with base.prepared_dispatcher(
            app=app,
            factory_callback=base.create_takeout_dispatcher,
            session=session,
    ) as (port, dispatcher):
        yield port, dispatcher


@pytest.fixture()
def testxmlrpc_instance(app, session):
    with base.prepared_dispatcher(
            app=app,
            factory_callback=base.create_text_xmlrpc_dispatcher,
            session=session,
    ) as (port, dispatcher):
        yield port, dispatcher


@pytest.fixture
def xmlrpcserver(medium_xmlrpc):
    yield medium_xmlrpc


@pytest.fixture()
def medium_http(medium_instance, httpretty_enabled_fixture):
    port, dispatcher = medium_instance
    yield base.ServantMocker(port=port, dispatcher=dispatcher)


# igogor: просто потому-что меня разражает старое название
@pytest.fixture()
def medium_xmlrpc(request, medium_instance, httpretty_enabled_fixture):
    port, dispatcher = medium_instance
    servant_mocker = base.ServantMocker(port=port, dispatcher=dispatcher)
    with servant_mocker.mocked_uri(path="/xmlrpc", http_method=httpretty.POST) as uri:
        with base.prepare_server_proxy(uri=uri, namespace="Balance", request=request) as medium_service_proxy:
            yield medium_service_proxy


@pytest.fixture()
def medium_xmlrpc_tvm_server(request, medium_instance, httpretty_enabled_fixture):
    port, dispatcher = medium_instance
    servant_mocker = base.ServantMocker(port=port, dispatcher=dispatcher)
    with servant_mocker.mocked_uri(path="/xmlrpctvm", http_method=httpretty.POST) as uri:
        with base.prepare_server_proxy(uri=uri, namespace="Balance", request=request) as medium_service_proxy:
            yield medium_service_proxy


@pytest.fixture
def test_xmlrpc_srv(request, testxmlrpc_instance, session, httpretty_enabled_fixture):
    port, dispatcher = testxmlrpc_instance
    servant_mocker = base.ServantMocker(port=port, dispatcher=dispatcher)
    with servant_mocker.mocked_uri(path="/xmlrpc", http_method=httpretty.POST) as uri:
        with base.prepare_server_proxy(uri=uri, namespace="Balance", request=request) as medium_service_proxy:
            yield medium_service_proxy


@pytest.fixture()
def takeout_http(takeout_instance, httpretty_enabled_fixture):
    port, dispatcher = takeout_instance
    yield base.ServantMocker(port=port, dispatcher=dispatcher)


@pytest.fixture(scope='session')
def muzzle_logic():
    from muzzle.muzzle_logic import MuzzleLogic
    return MuzzleLogic()


@pytest.fixture()
def httpretty_enabled_fixture():
    """ reduce indentation when using httpretty. """
    with httpretty.enabled(allow_net_connect=True):
        yield


@pytest.fixture(autouse=True, scope='session')
def patch_secrets_loading():
    with mock.patch('butils.application.secret.load_secret_from_file') as load_secret_from_file_mock:
        load_secret_from_file_mock.return_value = 'Kf82ssQw1ui'
        yield


@pytest.fixture(autouse=True, scope='session')
def patch_bfop_get_env():
    with mock.patch('balance.publisher.bfop.util.get_env') as get_env_mock:
        get_env_mock.return_value = 'development'
        yield


@pytest.fixture()
def create_tvm_client_mock():
    from balance import tvm
    tvm._TVM_CLIENTS_CACHE.clear()
    with mock.patch('balance.tvm.create_tvm_client') as create_tvm_client:
        yield create_tvm_client


@pytest.fixture()
def tvm_client_mock(create_tvm_client_mock):
    tvm_client = mock.Mock()
    create_tvm_client_mock.return_value = tvm_client
    return tvm_client


@pytest.fixture()
def tvm_check_service_ticket_mock(tvm_client_mock):
    return tvm_client_mock.check_service_ticket


@pytest.fixture()
def tvm_check_permissions_mock(tvm_client_mock):
    from balance.mapper import TVMACLPermission
    with mock.patch.object(TVMACLPermission, TVMACLPermission.is_allowed.__name__,
                           autospec=True) as check_permissions_mock:
        yield check_permissions_mock


@pytest.fixture
def tvm_ticket(medium_xmlrpc_tvm_server):
    ticket = "tvm_mocked_ticket"
    service_ticket = base.ServiceTicket(src=11111)
    medium_xmlrpc_tvm_server.yb_extra_headers[TVM2_SERVICE_TICKET_HEADER] = ticket
    yield ticket, service_ticket, {TVM2_SERVICE_TICKET_HEADER: ticket}


@pytest.fixture
def tvm_valid_ticket_mock(
        tvm_ticket,
        tvm_check_service_ticket_mock,
        tvm_check_permissions_mock,
):

    ticket, service_ticket, headers = tvm_ticket
    tvm_check_service_ticket_mock.return_value = service_ticket
    tvm_check_permissions_mock.return_value = True
    yield tvm_ticket


@pytest.fixture()
def tvm_invalid_ticket_mock(
        tvm_ticket,
        tvm_check_service_ticket_mock,

):
    from tvmauth.exceptions import TicketParsingException

    tvm_check_service_ticket_mock.side_effect = TicketParsingException(
        message='Bad ticket',
        status=1,
        debug_info='debug info'
    )
    yield tvm_ticket


@pytest.fixture()
def tvm_no_permission_mock(
        tvm_ticket,
        tvm_valid_ticket_mock,
        tvm_check_permissions_mock,
):
    tvm_check_permissions_mock.return_value = False
    yield tvm_ticket


def clear_caches_func(session):
    if hasattr(session, 'clear_cache'):
        session.clear_cache()


@pytest.fixture(autouse=True)
def clear_caches(session):
    clear_caches_func(session)
    yield
    clear_caches_func(session)


# --- --- ---
# Использование флагов

# ToDo[natabers]: BALANCE-36923
@pytest.fixture(params=[
    pytest.param(False, id='old_paystep'),
    pytest.param(True, id='new_paystep'),
])
def switch_new_paystep_flag(request, session):
    session.config.__dict__['USE_NEW_PAYSTEP'] = request.param
    return request.param


@pytest.fixture(params=[
    pytest.param(True, id='old_update_amount'),
    pytest.param(False, id='new_update_amount'),
])
def swith_paid_amount_flag(request, session):
    session.config.__dict__['USE_OLD_UPDATE_PAID_AMOUNT'] = request.param
    return request.param


@pytest.fixture
def service_ticket_mock():
    with mock.patch('balance.tvm.get_or_create_tvm_client') as m:
        m.return_value.get_service_ticket_for.return_value = 'ticket'
        yield m
