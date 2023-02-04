# -*- coding: utf-8 -*-
from collections import namedtuple
from xml.etree import ElementTree as et
from xml.parsers.expat import ExpatError
import os
import functools
import random
from urlparse import urlsplit, urlunparse

import requests
import sys
import threading
import unittest
import xmlrpclib
import datetime as dt
import contextlib

import sqlalchemy as sa
import httpretty
from werkzeug.test import Client, EnvironBuilder

from butils import logger

from balance import application
from balance import exc
from balance import muzzle_util as ut
from balance.constants import BALANCE_DATABASE_ID, BALANCE_META_DATABASE_ID
from tests import test_application

# typing imports
from typing import Tuple, Callable, Text, Optional, Dict, Any
from butils.application import Application
from butils.wsgi_util import WsgiDispatcher
from sqlalchemy.orm import sessionmaker


# Fork-compatible random generator
RANDOM = random.SystemRandom()

log = logger.get_logger()


os.environ['NLS_LANG'] = 'AMERICAN_CIS.UTF8'
os.environ['NLS_NUMERIC_CHARACTERS'] = '. '


def clear_threading_local(not_clear_list=None):
    if not_clear_list:
        not_clear_list.append('additional_info')
    else:
        not_clear_list = ['additional_info', ]
    for key in list(threading.current_thread().__dict__):
        if not key.startswith('_') and key not in not_clear_list:
            delattr(threading.current_thread(), key)


def init_app():
    try:
        app = application.getApplication()
    except RuntimeError:
        app = test_application.ApplicationForTests()
    if not isinstance(app, test_application.ApplicationForTests):
        raise RuntimeError("Application must be instance of TestApplication")

    return app


def init_sa_logging():
    # patch sqlalchemy loggers
    for mod in [y for x, y in sys.modules.iteritems() if x[:10] == 'sqlalchemy' and y]:
        for c in mod.__dict__.values():
            if hasattr(c, '_should_log_debug'):
                c._should_log_debug = lambda x: True
            if hasattr(c, '_should_log_info'):
                c._should_log_info = lambda x: True


def init_sderr_logging():
    # remove stderr handlers from log
    import logging
    root_logger = logging.getLogger('')
    logs = [h for h in root_logger.handlers if
            isinstance(h, logging.StreamHandler) and getattr(h, 'stream') == sys.stderr]
    if logs:
        root_logger.removeHandler(logs[0])


def init_session(app, database_id=BALANCE_DATABASE_ID):
    session = app.new_session(database_id=database_id)
    session.autoflush = False
    session.oper_id = RANDOM.randrange(-666666666, -2)
    session.begin()

    log.debug('init session: %s' % session)
    if database_id == BALANCE_META_DATABASE_ID:
        return session
    session.execute('begin dbms_output.enable(100000); end;')

    # Create test user if not exists
    from balance.mapper import permissions
    test_user = session.query(permissions.Passport).filter_by(passport_id=session.oper_id).first()
    AdminRole = session.query(permissions.Role).get(0)
    if not test_user:
        test_user = permissions.Passport(
            passport_id=session.oper_id,
            gecos=str('gecos for passport_id=' + str(session.oper_id))
        )
        session.add(test_user)
        session.flush()
        # There is a trigger on t_passport which creates a role on insert. Also, there is a
        # UNIQ constraint on t_role_user.passport_id.
        # All this bullshit is temporary (hopefully).
        # dbhelper.Session.commit(session)
        # dbhelper.Session.begin(session)

        if hasattr(session, 'roles'):
            del session.roles[0]
            session.roles.append(AdminRole)

    if not test_user.roles:  # never will happen while there is a trigger
        session.add(permissions.RealRolePassport(session, passport=test_user, role=AdminRole))
        session.flush()

    if test_user.roles[0] != AdminRole:
        session.query(permissions.RealRolePassport).filter_by(passport_id=test_user.passport_id).delete()
        session.add(permissions.RealRolePassport(session, passport=test_user, role=AdminRole))
        session.flush()

    return session


def patch_session(dispatcher, session):
    # type: (WsgiDispatcher, sessionmaker) -> WsgiDispatcher

    # Patch dispatcher to use our session, eliminating the need for commit.
    # Wrap all calls in a nested transaction,
    # so rollbacks don't fuck up everything.
    original_handle_request = dispatcher.handle_request

    @functools.wraps(original_handle_request)
    def call_wrapper(*args, **kwargs):
        # Make app.new_session always return this session
        threading.current_thread().test_session = session

        session.begin_nested()
        try:
            res = original_handle_request(*args, **kwargs)
        except exc.EXCEPTION as e:
            if e.wo_rollback:
                session.commit()
            else:
                session.rollback()
            raise
        except:
            session.rollback()
            raise
        else:
            if session.is_active:
                session.commit()
            else:
                session.rollback()
            return res

    assert hasattr(dispatcher, "_original_handle_request") is False, \
        "invoker already has _original_handle_request attribute"
    dispatcher._original_handle_request = original_handle_request
    dispatcher.handle_request = call_wrapper
    return dispatcher


def unpatch_session(dispatcher):  # type: (WsgiDispatcher) -> WsgiDispatcher
    dispatcher.handle_request = dispatcher._original_handle_request
    del dispatcher._original_handle_request


def get_dbms_output(session):
    sql = sa.text(
        "begin dbms_output.get_line(:line, :status); end;",
        bindparams=[
            sa.outparam('status', sa.types.Integer),
            sa.outparam('line', sa.types.String),
        ]
    )
    lines = []
    while True:
        res = session.execute(sql).out_parameters
        if res['status'] != 0:
            break
        log.debug(res['line'])
        lines.append(res['line'])
    return lines


class TestBase(unittest.TestCase):
    """
    Base class for tests
    """
    __loginited = False

    @classmethod
    def setUpClass(cls):
        if not hasattr(cls, 'app'):
            cls.app = init_app()
            cls.cfg = cls.app.cfg
            cls.dbhelper = cls.app.dbhelper

        init_sa_logging()

        if not TestBase.__loginited:
            init_sderr_logging()
        log.debug('log inited')
        TestBase.__loginited = True

    @classmethod
    def tearDownClass(cls):
        pass

    def setUp(self):
        clear_threading_local()
        self.session = init_session(application.getApplication())

        # mocking stuff
        from balance import mncloselib
        mncloselib.is_monthly_acts_done = lambda x: True

        from balance import mapper
        mapper.Job.next_dt = property(lambda s: dt.datetime.now() + dt.timedelta(1))

        self.sql_dbms = []
        self._create_tables()

    def logDbmsOutput(self, session=None):
        self.sql_dbms.extend(get_dbms_output(session or self.session))

    def tearDown(self):
        log.debug('clear session: %s' % self.session)
        self.session.rollback()
        self.session.close()
        del self.session
        clear_threading_local()

    def assertApproximates(self, first, second, tolerance, msg=None):
        """asserts that C{first} - C{second} > C{tolerance}
        @param msg: if msg is None, then the failure message will be
                '%r ~== %r' % (first, second)
        """
        self.assert_(abs(first - second) <= tolerance,
                     msg or "%s ~== %s" % (first, second))

    def assertEqual(self, first, second, msg=None):
        try:
            msg = msg or (
                'Assert failed: %s == %s' % (first, second))
        except:
            pass
        assert first == second, msg

    def assertNotEqual(self, first, second, msg=None):
        try:
            msg = msg or (
                'Assert failed: %s != %s' % (first, second))
        except:
            pass
        assert first != second, msg

    def assert_(self, expr, msg=None):
        assert expr, msg

    def fail(self, msg):
        if isinstance(msg, Exception):
            import traceback
            raise AssertionError(traceback.format_exc())
        else:
            raise AssertionError(msg)

    def assertFault(self, code_or_codes, func, *args):
        "Fail unless func raises Fault with specified code"
        a_codes = frozenset(
            code_or_codes if isinstance(code_or_codes, (tuple, list)) else (code_or_codes, ))
        try:
            func(*args)
        except xmlrpclib.Fault as exc:
            log.debug('assert exception: %s' % exc.faultString)
            try:
                doc = et.XML(exc.faultString)
            except ExpatError:
                raise exc  # re-raise old exception
            codes = list(elem.text for elem in doc.findall('code'))
            if bool(a_codes.intersection(codes)):
                return
            raise AssertionError("expected codes %s, got %s" % (a_codes, exc.faultString))
        except ut.EXCEPTION as exc:
            # Also accept regular exception with the same name - in case when xmlrpcserver has been
            # replaced with xmlrpclogic for debugging purposes
            if exc.__class__.__name__ not in a_codes:
                raise exc
            return
        raise AssertionError("Function didn't raise Fault! (Codes %s was expected)" % (a_codes, ))

    @property
    def coreobj(self):
        from balance import core
        return core.Core(self.session)

    @classmethod
    def _call_test(cls, test_name):
        t = cls(test_name)
        t.setUpClass()
        t.setUp()
        try:
            getattr(t, test_name)()
        except:
            raise
            t.tearDown()
            t.tearDownClass()
        else:
            t.tearDown()
            t.tearDownClass()

    _test_tables = []

    def _create_tables(self):
        """ Create sqlalchemy tables listed in self._test_tables
        if tables are not exists"""

        engine = application.getApplication().get_dbhelper().engines[0]
        for t in self._test_tables:
            exists = engine.dialect.has_table(engine.connect(), t.__table__.name)
            if not exists:
                log.info('Creating table %s', t.__table__)
                t.__table__.metadata.create_all(bind=engine, tables=[t.__table__])


class BalanceTest(TestBase):
    """
    Base class for tests for 'tools/balance' package
    """
    pass


class MuzzleTest(TestBase):
    """
    Base class for tests for tools/muzzle package
    """
    pass


class YbTransport(xmlrpclib.Transport):
    def __init__(self, use_datetime=0):
        xmlrpclib.Transport.__init__(self, use_datetime)
        self.yb_extra_headers = {}

    def send_content(self, connection, request_body):
        if self.yb_extra_headers:
            for key, value in self.yb_extra_headers.items():
                connection.putheader(key, value)
        xmlrpclib.Transport.send_content(self, connection, request_body)


# хак для того что бы pytest не звал __repr__ и
# всё такое от объекта старого стиля xmlrpclib._Method
class ProxyWrap(object):
    def __init__(self, proxy, namespace, yb_extra_headers):
        self.__proxy = proxy
        self.__namespace = namespace
        self.yb_extra_headers = yb_extra_headers

    def __call__(self, attr):
        # см. исходники xmlrpclib
        return self.__proxy(attr)

    def __getattr__(self, item):
        namespace = getattr(self.__proxy, self.__namespace)
        return getattr(namespace, item)


def create_server_proxy(url, namespace='Balance'):
    transport = YbTransport()
    server_proxy = xmlrpclib.ServerProxy(
        url, allow_none=True, transport=transport
    )
    return ProxyWrap(server_proxy, namespace, transport.yb_extra_headers)


def httpretty_setup(allow_net_connect=False):
    # type: (bool) -> Tuple[bool, bool, Optional[bool]]
    was_disabled = not httpretty.is_enabled()
    was_reset = not bool(httpretty.HTTPretty._entries)
    if was_disabled:
        if was_reset:
            # igogor: на всякий случай, т.к. для проверки опираемся только на одну часть которая резетится
            httpretty.reset()
        httpretty.enable(allow_net_connect)
        prev_connect = None
    else:
        prev_connect = httpretty.HTTPretty.allow_net_connect
        httpretty.HTTPretty.allow_net_connect = allow_net_connect
    return was_disabled, was_reset, prev_connect


def httpretty_teardown(was_disabled, was_reset, prev_connect):
    # type: (bool, bool, Optional[bool]) -> None
    if was_disabled:
        httpretty.disable()
        if was_reset:
            httpretty.reset()
    elif prev_connect is not None:
        httpretty.HTTPretty.allow_net_connect = prev_connect
    else:
        raise RuntimeError("If httpretty was enabled before setup, prev_connect must be defined")


@contextlib.contextmanager
def httpretty_ensure_enabled(allow_net_connect=False):
    # type: (bool) -> None
    was_disabled, was_reset, prev_connect = httpretty_setup(allow_net_connect)
    yield
    httpretty_teardown(was_disabled=was_disabled, was_reset=was_reset, prev_connect=prev_connect)


def create_medium_dispatcher(app):  # type: (Application) -> Tuple[Text, WsgiDispatcher]
    from medium.medium_servant import create_dispatcher
    from medium.uwsgi_run import MediumRunner

    # todo-igogor разобраться почему в юнит-тестах не видит YANDEX_XML_CONFIG
    medium_port = MediumRunner(config_path=app.cfg_path).medium_port()
    dispatcher = create_dispatcher(app)
    return medium_port, dispatcher


def create_takeout_dispatcher(app):  # type: (Application) -> Tuple[Text, WsgiDispatcher]
    from medium.uwsgi_run import MediumRunner

    _, dispatcher = create_medium_dispatcher(app)
    takeout_port = MediumRunner(config_path=app.cfg_path).takeout_port()
    return takeout_port, dispatcher


def create_text_xmlrpc_dispatcher(app):  # type: (Application) -> Tuple[Text, WsgiDispatcher]
    from test_xmlrpc.test_xmlrpc_servant import create_dispatcher
    from test_xmlrpc.uwsgi_run import TestXmlRpcRunner
    test_xmlrpc_port = TestXmlRpcRunner(config_path=app.cfg_path).test_xmlrpc_port()
    dispatcher = create_dispatcher(app)
    return test_xmlrpc_port, dispatcher


@contextlib.contextmanager
def prepared_dispatcher(
        app,  # type: Application
        factory_callback,  # type: Callable[[Application], Tuple[Text, WsgiDispatcher]]
        session,  # type: sessionmaker
):  # type: (...) -> Tuple[Text, WsgiDispatcher]
    port, dispatcher = factory_callback(app)
    patch_session(dispatcher, session)

    yield port, dispatcher

    unpatch_session(dispatcher)


class ServantMocker(object):
    def __init__(self, port, dispatcher, scheme="http"):  # type: (Text, WsgiDispatcher) -> None
        self.port = port
        self.dispatcher = dispatcher
        self.scheme=scheme

    def url(self, path):
        return urlunparse((self.scheme, "localhost:%s" % self.port, path, '', '', ''))

    def mock_path(self, path, http_method, buffered=True):
        # type: (Text, Text, bool) -> None
        mock_socket(uri=self.url(path),
                    http_method=http_method,
                    dispatcher=self.dispatcher,
                    buffered=buffered)

    @contextlib.contextmanager
    def mocked_uri(self, path, http_method):  # type: (Text, Text) -> Text
        with httpretty_ensure_enabled(allow_net_connect=False):
            self.mock_path(path=path, http_method=http_method)
            yield self.url(path)

    def request(self, path, http_method=httpretty.GET, **kwargs):
        # type: (Text, Text, **Any) -> requests.Response
        with self.mocked_uri(path=path, http_method=http_method) as uri:
            return requests.request(http_method.lower(), uri, **kwargs)


def mock_socket(uri, http_method, dispatcher, buffered=True):
    # type: (Text, Text, WsgiDispatcher, bool) -> None

    def request_callback(request, uri, response_headers):
        # type: (httpretty.HTTPrettyRequest, Text, Dict[Text, Any]) -> Tuple[int, Dict[Text, Any], Text]
        parsed_uri = urlsplit(uri)
        base_url = urlunparse((parsed_uri.scheme, parsed_uri.netloc, parsed_uri.path, "", "", ""))
        builder = EnvironBuilder(
            path=parsed_uri.path,
            base_url=base_url,
            query_string=parsed_uri.query,
            method=request.method,
            input_stream=request.rfile,
            headers=request.headers.dict,
            errors_stream=sys.stdout, # todo-igogor сюда ли лить?
            multithread=False,
            multiprocess=True,
            run_once=False,
        )
        environ = builder.get_environ()
        # igogor: этот параметр uwsgi не добавляет
        if "HTTP_CONTENT_TYPE" in environ:
            del environ["HTTP_CONTENT_TYPE"]
        # igogor: а этот добавляет
        environ["REQUEST_URI"] = builder.path
        body, status, headers = Client(dispatcher).open(environ, buffered=buffered)
        status_code = int(status.split(None, 1)[0])
        return (status_code, dict(headers), "".join(body))

    httpretty.register_uri(
        method=http_method.upper(),
        uri=uri,
        body=request_callback,
    )


@contextlib.contextmanager
def prepare_server_proxy(uri, namespace, request):
    # type: (Text, Text, Request) -> xmlrpclib.ServerProxy
    server_proxy = create_server_proxy(uri, namespace=namespace)

    if request.cls is not None:
        request.cls.xmlrpcserver = server_proxy

    yield server_proxy

    if request.cls is not None:
        del request.cls.xmlrpcserver

    # ServerProxy (а точнее Transport) кэширует соединения к серверу.
    # Т.к. для каждого теста создается новый ServerProxy,
    # соединения, закэшированные в предыдущих инстансах закрываются
    # (автоматически) не сразу. Если соединения здесь не закрывать явно,
    # то у сервера быстро заканчиваются воркеры.
    # Есть еще (возможно, более правильный) способ -
    # сделать инстанс ServerProxy фикстурой со scope = session.
    # Но тогда придется написать еще N фикстур на каждый сервант/endpoint/namespace.

    # todo-igogor кмк это больше не нужно?
    # По поводу кода, см. исходники xmlrpclib
    close_method = server_proxy('close')
    close_method()


class MediumTest(TestBase):
    """
    Base class for tests for tools/medium package
    """
    @classmethod
    def setUpClass(cls):
        super(MediumTest, cls).setUpClass()

    def setUp(self):
        super(MediumTest, self).setUp()

        port, dispatcher = create_medium_dispatcher(self.app)
        self.servant_mocker = ServantMocker(port=port, dispatcher=dispatcher)
        patch_session(dispatcher=self.servant_mocker.dispatcher, session=self.session)

        self.httpretty_setup_res = httpretty_setup(allow_net_connect=True)
        self.servant_mocker.mock_path(path="/xmlrpc", http_method=httpretty.POST, buffered=True)

        type(self).server = create_server_proxy(self.servant_mocker.url("/xmlrpc"), namespace="Balance")
        type(self).xmlrpcserver = self.server

    def tearDown(self):
        super(MediumTest, self).tearDown()
        unpatch_session(self.servant_mocker.dispatcher)
        httpretty_teardown(*self.httpretty_setup_res)

    @classmethod
    def tearDownClass(cls):
        super(MediumTest, cls).tearDownClass()


ServiceTicket = namedtuple('ServiceTicket', ['src'])
