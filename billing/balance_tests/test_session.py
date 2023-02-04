# -*- coding: utf-8 -*-

from datetime import datetime, timedelta

import pytest

from balance import mapper
from balance import exc
from balance import deco

from tests.base_routine import BalanceRoutineTest
from tests import object_builder as ob


class TestSession(BalanceRoutineTest):
    def setUp(self):
        super(TestSession, self).setUp()

        self.key = 'TEST_CONFIG_PARAM_666'
        from butils.dbhelper.dbconfig import PARTIAL_PARAMS
        PARTIAL_PARAMS.add(self.key)

        self.session.add(mapper.Config(item=self.key))
        self.session.flush()
        self.session.config._PartialConfigurators = dict()

    def test_using_partial_config(self):
        params = {
            'Client_ids': [self.order.client.id]
        }

        self.session.config.set(self.key, params, column_name='value_json')
        self.session.config.load_partions_config(self.key, self.order.client)
        self.assertEqual(self.session.config.is_use(self.key), True)

    def test_working_partial_config(self):
        val = str(self.order.id % 2)

        params = {
            'Client_ids': [self.order.client.id]
        }

        self.session.config.set(self.key, params, column_name='value_json')
        self.session.config.load_partions_config(self.key, self.order.client)

        self.assertIn(self.key, self.session.config._cache)

    def test_not_using_partial_config(self):
        val = str(self.order.id % 2)

        params = {
            'Client_ids': [self.order.client.id - 5]
        }

        self.session.config.set(self.key, params, column_name='value_json')
        self.assertEqual(self.session.config.is_use(self.key), False)

        self.order = self._create_order()
        self.session.config.load_partions_config(self.key, self.order.client)
        self.assertEqual(self.session.config.is_use(self.key), False)


class TestOracleLocks(object):
    def _real_session(self, app):
        s = app.real_new_session()
        s.execute("ALTER session SET current_schema=BO")
        s.execute('begin dbms_output.enable(100000); end;')
        return s

    @pytest.fixture()
    def order(self, session):
        return ob.OrderBuilder.construct(session)

    @pytest.mark.parametrize('lock_mode', ['shared', 'exclusive'])
    @pytest.mark.parametrize('timeout', ['0', '10'])
    def test_lock_new(self, app, order, lock_mode, timeout):
        session = self._real_session(app)
        with session.begin():
            session.oracle_lock(order, lock_mode, timeout)

    @pytest.mark.parametrize(
        'first_lock_mode, second_lock_mode, is_allowed',
        [
            ('shared', 'shared', True),
            ('exclusive', 'shared', False),
            ('shared', 'exclusive', False),
            ('exclusive', 'exclusive', False),
        ]
    )
    def test_locked(self, app, order, first_lock_mode, second_lock_mode, is_allowed):
        session1 = self._real_session(app)
        session2 = self._real_session(app)

        with session1.begin(), session2.begin():
            session1.oracle_lock(order, first_lock_mode)

            if is_allowed:
                session2.oracle_lock(order, second_lock_mode)
            else:
                with pytest.raises(exc.DEFER_LOCKED_OBJECT):
                    session2.oracle_lock(order, second_lock_mode)

    @pytest.mark.parametrize('first_lock_mode', ['shared', 'exclusive'])
    @pytest.mark.parametrize('second_lock_mode', ['shared', 'exclusive'])
    def test_multiple_locks(self, app, order, first_lock_mode, second_lock_mode):
        session = self._real_session(app)
        with session.begin():
            session.oracle_lock(order, first_lock_mode)
            session.oracle_lock(order, second_lock_mode)

    @pytest.mark.parametrize(
        ['namespace_param_name', 'function_name'],
        [
            pytest.param('namespace', 'sf_oracle_lock_handler', id='sf_oracle_lock_handler'),
            pytest.param('tablename', 'sf_oracle_lock_handler_new', id='sf_oracle_lock_handler_new'),
        ],
    )
    @pytest.mark.parametrize(
        ['expiration_secs', 'pass_'],
        [
            pytest.param(None, False, id='not passed'),
            pytest.param(None, True, id='None'),
            pytest.param(60 * 10, True, id='10 minutes'),
        ],
    )
    def test_sf_oracle_lock_handler_expiration_secs(self, namespace_param_name, function_name,
                                                    expiration_secs, pass_, session):
        call_query_template = """
            declare
              handle VARCHAR2(512 char);
            begin
              handle := bo.{function_name}(
                {namespace_param_name} => :namespace,
                object_id => :object_id
                {expiration_secs_param}
              );
            end;
        """
        namespace = ob.generate_character_string(10)
        object_id = ob.generate_int(5)
        call_query_params = {
            'namespace': namespace,
            'object_id': object_id,
        }
        template_params = {
            'namespace_param_name': namespace_param_name,
            'function_name': function_name,
        }
        if pass_:
            template_params['expiration_secs_param'] = ', expiration_secs => :expiration_secs'
            call_query_params['expiration_secs'] = expiration_secs
        else:
            template_params['expiration_secs_param'] = ''
        call_query = call_query_template.format(**template_params)
        session.execute(call_query, call_query_params)
        expiration_secs = expiration_secs or 60 * 60 * 24
        expected_expiration = datetime.now() + timedelta(seconds=expiration_secs)
        lock_name = '{}_{}'.format(namespace, object_id)
        actual_expiration = session.execute(
            'select expiration from sys.dbms_lock_allocated where name = :lock_name',
            dict(lock_name=lock_name)
        ).scalar()
        difference_secs = abs((actual_expiration - expected_expiration).total_seconds())
        assert difference_secs < 60, (expected_expiration, actual_expiration)


def test_transaction_cache_is_cleared_on_flush(session):
    @deco.transaction_cached()
    def my_func(client_):
        return True

    # We have to generate some changes.
    # Empty flush does not fire any events,
    # which are necessary for session cache functionality.
    client = ob.ClientBuilder.construct(session)
    client.name = 'xxx'

    my_func(client)
    # Check that cache is not empty
    assert getattr(session, session.transaction_cache_attr, None)
    session.flush()
    # Cache must become empty
    assert not getattr(session, session.transaction_cache_attr, None)


def test_sql_id_by_statement():
    from butils.dbhelper.helper import sql_id_by_statement

    statement = "SELECT id, external_id FROM bo.t_invoice"
    assert sql_id_by_statement(statement) == '006zcvdqxzp4s'

    statement = u"""SELECT id AS "ID", external_id AS "Внешний ID" FROM bo.t_invoice"""
    assert sql_id_by_statement(statement) == '1x5usa72h2mb9'
