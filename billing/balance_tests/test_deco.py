# -*- coding: utf-8 -*-
import random
import contextlib
import functools

from balance import deco, exc
from balance.constants import *
from butils import rpcutil, execution_context
from butils.application import HOST
from tests.base import BalanceTest

import pytest
# from tests.object_builder import *


ACTION_BEGIN = 'begin'  # type: str
ACTION_COMMIT = 'commit'
ACTION_ROLLBACK = 'rollback'
ACTION_USE = 'use'


class ExpectedError(exc.EXCEPTION):
    def __init__(self, *args):
        self.args = args


class ExpectedErrorWoRollback(ExpectedError):
    wo_rollback = True


class MockSession(object):
    def __init__(self, args, kwargs):
        self.args = args
        self.kwargs = kwargs
        self.actions = []
        self.context = execution_context.get_context()

    def begin(self):
        self.actions.append(ACTION_BEGIN)
        return self.get_transaction()

    @contextlib.contextmanager
    def get_transaction(self):
        try:
            yield self
        except:
            self.rollback()
            raise
        else:
            self.commit()

    def commit(self):
        self.actions.append(ACTION_COMMIT)

    def use(self):
        self.actions.append(ACTION_USE)

    def rollback(self):
        self.actions.append(ACTION_ROLLBACK)


def do_nothing_deco(func):
    @functools.wraps(func)
    def new_func(*a, **kw):
        return func(*a, **kw)

    return new_func


def composite_deco_with_passport_session_transaction(func):
    result = deco.with_passport_session_transaction(func)
    # print result
    result2 = do_nothing_deco(result)
    # print result2
    return result2


class DummyServant(object):
    name = 'test'

    def __init__(self, test_class):
        self.test_class = test_class
        self._cached_passport_id = None

    def _new_session(self, *args, **kwargs):
        return MockSession(args, kwargs)

    @staticmethod
    def _get_operator_uid(args, kwargs):
        # так делается в muzzle
        return args[0]

    @staticmethod
    def _replace_args(args, kwargs, session):
        # так делается в muzzle
        args = (session,) + args[1:]
        return args, kwargs

    def _cache_passport_id(self, passport_id):
        self._cached_passport_id = passport_id

    @deco.with_passport_session
    def auto_session_passport_session(self, session, a, b):
        session.use()
        return session, [ACTION_USE]

    @rpcutil.call_description(
        rpcutil.arg_int,
        rpcutil.arg_service_id,
        rpcutil.arg_variant(rpcutil.arg_int, rpcutil.arg_str),
    )
    @deco.with_passport_rosession
    def auto_session_passport_rosession(self, session, service_id, b):
        session.use()
        return session, [ACTION_BEGIN, ACTION_USE, ACTION_ROLLBACK]

    @rpcutil.call_description(
        rpcutil.arg_long,
        rpcutil.arg_service_id,
        rpcutil.arg_variant(rpcutil.arg_int, rpcutil.arg_str),
    )
    @deco.with_passport_rosession(BALANCE_STANDBY_DATABASE_ID)
    def auto_session_passport_standbyrosession(self, session, service_id, b):
        session.use()
        return session, [ACTION_BEGIN, ACTION_USE, ACTION_ROLLBACK]

    @deco.rosession_simple(BALANCE_META_DATABASE_ID)
    def auto_session_passport_meta_session(self, session, passport_id, service_id, b):
        session.use()
        return session, [ACTION_BEGIN, ACTION_USE, ACTION_ROLLBACK]

    @rpcutil.call_description(
        rpcutil.arg_operator_uid,
        rpcutil.arg_service_id,
        rpcutil.arg_variant(rpcutil.arg_int, rpcutil.arg_str),
    )
    @deco.with_passport_session_transaction
    def auto_session_passport_session_transaction(self, session, service_id, b):
        session.use()
        return session, [ACTION_BEGIN, ACTION_USE, ACTION_COMMIT]

    @rpcutil.call_description(
        rpcutil.arg_operator_uid,
        rpcutil.arg_service_id,
        rpcutil.arg_variant(rpcutil.arg_int, rpcutil.arg_str),
    )
    @deco.with_passport_session_transaction
    @do_nothing_deco
    def auto_session_passport_session_transaction_deco_before(self, session, service_id, b):
        session.use()
        return session, [ACTION_BEGIN, ACTION_USE, ACTION_COMMIT]

    @rpcutil.call_description(
        rpcutil.arg_operator_uid,
        rpcutil.arg_service_id,
        rpcutil.arg_variant(rpcutil.arg_int, rpcutil.arg_str),
    )
    @do_nothing_deco
    @deco.with_passport_session_transaction
    def auto_session_passport_session_transaction_deco_after(self, session, service_id, b):
        session.use()
        return session, [ACTION_BEGIN, ACTION_USE, ACTION_COMMIT]

    @deco.with_session
    def auto_session_with_session(self, session, passport_id, service_id, b):
        session.use()
        return session, [ACTION_USE]

    @deco.with_rosession
    def auto_session_with_rosession(self, session, passport_id, service_id, b):
        session.use()
        return session, [ACTION_BEGIN, ACTION_USE, ACTION_ROLLBACK]

    @rpcutil.call_description(
        rpcutil.arg_operator_uid,
        rpcutil.arg_service_id,
        rpcutil.arg_variant(rpcutil.arg_int, rpcutil.arg_str),
    )
    @composite_deco_with_passport_session_transaction
    def auto_session_composite_decorated(self, session, service_id, b):
        session.use()
        return session, [ACTION_BEGIN, ACTION_USE, ACTION_COMMIT]

    @rpcutil.call_description(
        rpcutil.arg_operator_uid,
        rpcutil.arg_service_id,
        rpcutil.arg_variant(rpcutil.arg_int, rpcutil.arg_str),
    )
    @composite_deco_with_passport_session_transaction(BALANCE_META_DATABASE_ID)
    def auto_session_composite_decorated_meta(self, session, service_id, b):
        session.use()
        return session, [ACTION_BEGIN, ACTION_USE, ACTION_COMMIT]

    @rpcutil.call_description(
        rpcutil.arg_operator_uid,
        rpcutil.arg_service_id,
        rpcutil.arg_variant(rpcutil.arg_int, rpcutil.arg_str),
    )
    @deco.with_passport_session_transaction
    def auto_session_passport_session_transaction_exception(self, session, service_id, b):
        session.use()
        raise ExpectedError(session, [ACTION_BEGIN, ACTION_USE, ACTION_ROLLBACK])

    @rpcutil.call_description(
        rpcutil.arg_operator_uid,
        rpcutil.arg_service_id,
        rpcutil.arg_variant(rpcutil.arg_int, rpcutil.arg_str),
    )
    @deco.with_passport_session_transaction
    def auto_session_passport_session_transaction_exception_wo_rollback(self, session, service_id, b):
        session.use()
        raise ExpectedErrorWoRollback(session, [ACTION_BEGIN, ACTION_USE, ACTION_COMMIT])

    @deco.with_rosession
    def get_without_args(self, session):
        return session

    @deco.with_rosession(BALANCE_STANDBY_DATABASE_ID)
    @deco.with_session_transaction
    def several_session(self, rw_session, standby_session):
        return rw_session, standby_session

    @deco.with_session_transaction
    @deco.with_rosession(BALANCE_STANDBY_DATABASE_ID)
    def several_session_2(self, standby_session, rw_session):
        return standby_session, rw_session

    @property
    def all_auto_session_methods(self):
        for method_name in self.__class__.__dict__:
            if method_name.startswith('auto_session_'):
                method = getattr(self, method_name)
                if callable(method):
                    yield method


class TestDeco(BalanceTest):
    passport_id = 3
    service_id = 666

    def setUp(self):
        super(TestDeco, self).setUp()
        self.servant = DummyServant(self)

    # def _get_callback(self):
    #     params = {}
    #
    #     def callback(*a, **kw):
    #         params['kwargs'] = kw
    #         params['args'] = a
    #
    #     return callback, params

    # @pytest.mark.parametrize(['database_id', 'method_name'], [
    #                                 ('balance_meta', 'passport_meta_session'),
    #                                 ('balance_ro',   'passport_standbyrosession'),
    #                                 (None,           'passport_rosession')
    #                                 ], indirect=False)
    # def test_database_id(self, database_id, method_name):
    #     method = getattr(self.servant, method_name)
    #     session, actions = method(self.passport_id, 1, 2)
    #     assert session.kwargs['database_id'] == database_id

    def test_database_id(self):
        for database_id, method in [
            (BALANCE_META_DATABASE_ID, self.servant.auto_session_passport_meta_session),
            (BALANCE_STANDBY_DATABASE_ID, self.servant.auto_session_passport_standbyrosession),
            (None, self.servant.auto_session_passport_rosession)
        ]:
            self._check_database_id(method, database_id)

    def _check_database_id(self, method, database_id):
        session, actions = method(self.passport_id, self.service_id, 2)
        assert session.kwargs['database_id'] == database_id

    def test_passport_rosession(self):
        session, actions = self.servant.auto_session_passport_session(self.passport_id, 1, 2)
        assert session.kwargs['oper_id'] == self.passport_id
        assert session.kwargs['database_id'] is None

    def test_get_without_args(self):
        session = self.servant.get_without_args()

    def test_passport_cache(self):
        assert self.servant._cached_passport_id is None
        self.servant.auto_session_passport_session_transaction(self.passport_id, self.service_id, 2)
        assert self.servant._cached_passport_id == self.passport_id

    def test_session_actions(self):
        # session_callback, session_creation_params = self._get_callback()

        for method in self.servant.all_auto_session_methods:
            self._check_session_actions(method)


    def test_several_sessions(self):
        rw_session, standby_session = self.servant.several_session()
        assert standby_session.kwargs['database_id'] == BALANCE_STANDBY_DATABASE_ID
        assert rw_session.kwargs['database_id'] is None
        standby_session, rw_session = self.servant.several_session_2()
        assert rw_session.actions == [ACTION_BEGIN, ACTION_COMMIT]
        assert standby_session.actions == [ACTION_BEGIN, ACTION_ROLLBACK]

    def _check_session_actions(self, method):
        method_name = method.__name__
        try:
            session, actions = method(self.passport_id, self.service_id, method_name)
        except ExpectedError as e:
            session, actions = e.args
        assert actions == session.actions
        assert 'test.' + method_name == session.kwargs['identity']
        assert '%s-%s' % (method_name, self.service_id) == '%s-%s' % (method_name, session.context['service_id'])
        assert 'passport' not in method_name or self.passport_id == session.context['passport_id']

    def test_service_id(self):
        session, actions = self.servant.auto_session_passport_rosession(self.passport_id, self.service_id, 2)
        assert self.service_id == session.context['service_id']
        assert self.passport_id == session.context['passport_id']

    def test_composite_decorated(self):
        for method, database_id in ((self.servant.auto_session_composite_decorated, None), \
                                    (self.servant.auto_session_composite_decorated_meta, BALANCE_META_DATABASE_ID)):
            session, actions = method(self.passport_id, self.service_id, 2)
            assert database_id == session.kwargs['database_id']


class TestConnection(BalanceTest):
    def _get_current_session_params(self, ctx_id):
        self.session.clear_cache()
        return self.session.execute(
            '''
            select machine, program, module, action, client_identifier, client_info 
            from v$session 
            where upper(client_identifier) like :host and action like :context
            ''', {'host': '%%%s%%' % HOST.upper(), 'context': '%%%s%%' % ctx_id}
        ).fetchall()

    def test_connection_params(self):
        ctx_id = execution_context.gen_ctx_id()
        service_id = random.randint(0, 1000000)
        passport_id = random.randint(0, 1000000)
        method = 'test_method'
        priority = '##'
        ctx = {'method': method,
               'priority': len(priority),
               'service_id': service_id,
               'passport_id': passport_id,
               'ctx_id': ctx_id,
               }
        with execution_context.context(ctx=ctx):
            res = self._get_current_session_params(ctx_id)
            assert res
            for row in res:
                r = dict(row)
                assert str(ctx_id) in r['action']
                assert priority in r['module']
                assert method in r['module']
                assert str(service_id) in r['client_info']
                assert str(passport_id) in r['client_info']
