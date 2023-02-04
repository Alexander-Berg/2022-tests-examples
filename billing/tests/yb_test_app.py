# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from builtins import super
from future import standard_library

standard_library.install_aliases()

import logging

import pytest
import allure

from brest import utils as bru
from brest.core.application import BrestApp

LOGGER = logging.getLogger()


class MakoRendererMock(object):
    def __init__(self):
        self.tmpl_name = None
        self.kwargs = {}

    def render(self, tmpl_name, **kwargs):
        self.tmpl_name = tmpl_name
        self.kwargs = kwargs

        return u'Rendered text'


class TestApp(BrestApp):
    _use_test_session = True

    def __init__(self, **kwargs):
        super(TestApp, self).__init__(**kwargs)
        self.mako_renderer = MakoRendererMock()

    def new_session(self, *args, **kwargs):
        """
        Creates thread-global rollback session with "oper_id" selected randomly at the initialization and
        caches it in the thread local storage in case of "_use_test_session" attribute is true.
        Uses real "new_session" method otherwise.
        """
        if not self._use_test_session:
            return super(TestApp, self).new_session(*args, **kwargs)

        tls = self.thread_local_storage

        if hasattr(tls, 'test_session'):
            LOGGER.debug('Session has been found in the thread local storage: %s', tls.test_session)
            _patch_session(tls.test_session, *args, **kwargs)
        else:
            tls.test_session = _create_real_session()
            _init_session(tls.test_session)

        from butils.dbhelper.session import Session

        def bound_method(new_method, instance):
            from types import MethodType

            return MethodType(new_method, instance, instance.__class__)

        # Restore "flush" method for read only sessions
        tls.test_session.flush = bound_method(Session.flush, tls.test_session)

        return tls.test_session

    def cleanup_session(self):
        LOGGER.debug('Cleaning up active session (if exists)')

        tls = self.thread_local_storage
        session = getattr(tls, 'test_session', None)

        if not session:
            return

        session.rollback()
        session.close()

        delattr(tls, 'test_session')

    @classmethod
    def _get_config(cls, cfg_default_path=None, use_if_exists=True, worker_mode=False):
        """
        'Cause default implementation does not support "xi:fallback".
        """
        from lxml import etree

        cfg_path = cls._get_config_path(cfg_default_path, use_if_exists, worker_mode)

        cfg = etree.parse(cfg_path)
        cfg.xinclude()

        return cfg_path, cfg

    def _configure_logs(self):
        pass


def create_app():
    """
    Big fucking hack!
    """
    TestApp._worker_configure_logs()

    bru.fix_env()
    app = TestApp()
    bru.fix_env()

    return app


def _init_oper_id(session):
    from tests import object_builder as ob
    passport = ob.PassportBuilder().build(session).obj
    return passport.passport_id


@allure.step('create new session')
def _create_real_session():
    """
    Creates session

    :rtype: butils.dbhelper.session.Session
    """
    LOGGER.debug('Creating session')
    from brest.core.application import get_application

    try:
        session = get_application()._new_session()
        return session
    except RuntimeError as e:
        LOGGER.warn('Missing "_new_session" implementation: %s', e)
        return None


def _init_session(
    session,
    oper_id=None,
):  # type: (butils.dbhelper.session.Session, int) -> None
    """
    Prepare session for operation in tests
    """
    LOGGER.debug('Initializing session with: oper_id=%s', oper_id)

    session.autoflush = False
    session.clone = lambda: session

    session.begin()

    session.execute('alter session set current_schema=bo')
    session.execute('begin dbms_output.enable(100000); end;')

    if oper_id is None:
        oper_id = _init_oper_id(session)
        LOGGER.debug('Initialized oper_id=%s', oper_id)
    session.oper_id = oper_id


def _patch_session(session, *args, **kwargs):
    try:
        from balance import mapper
    except ImportError:
        LOGGER.error('Can\'t import balance!')
        return

    oper_id = args[0] if args else None

    if oper_id is not None:
        session.oper_id = oper_id
        try:
            del session.oper_perms
        except AttributeError:
            pass
        session._passport = session.query(mapper.Passport).getone(passport_id=oper_id)
