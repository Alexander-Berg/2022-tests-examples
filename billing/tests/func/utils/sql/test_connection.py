# -*- coding: utf-8 -*-

import functools

import mock

from billing.dcs.dcs.utils.sql.connection import COMPONENTS_DB_PREFIX, \
    _construct_dsn_from_component, _dsn_to_str

from billing.dcs.tests.utils import BaseTestCase, create_patcher


CHECK_RUNS_PATCH_PATH = 'billing.dcs.dcs.utils.sql.connection'
patch = create_patcher(CHECK_RUNS_PATCH_PATH)


def without_application(func):
    @functools.wraps(func)
    def inner(*args, **kwargs):
        with mock.patch('billing.dcs.dcs.temporary.butils.application.application', None):
            return func(*args, **kwargs)
    return inner


class ConstructDsnFromComponentTestCase(BaseTestCase):
    def perform_unsuccessful_test(self, db_id, expected_call_params,
                                  log_method='exception'):
        with patch('log') as log_:
            dsn_url = _construct_dsn_from_component(db_id)

        self.assertEqual(dsn_url, '')
        getattr(log_, log_method).assert_called_once_with(*expected_call_params)

    def perform_successful_test(self, db_id):
        dsn_uri = _dsn_to_str(_construct_dsn_from_component(db_id))
        expected_dsn_uri = 'oracle+dcs://user:password@host'
        self.assertEqual(dsn_uri, expected_dsn_uri)

    @without_application
    def test_without_application(self):
        expected_exception = 'Cannot construct dsn without application:'
        self.perform_unsuccessful_test('anything', (expected_exception, ))

    def test_secret_not_found(self):
        expected_exception = 'Secret not found:'
        self.perform_unsuccessful_test('fail_secret', (expected_exception, ))

    def test_secret_cannot_be_loaded(self):
        expected_exception = 'Secret could not be loaded:'
        self.perform_unsuccessful_test(
            'fail_to_load_secret', (expected_exception, ))

    def test_component_not_found(self):
        db_id = 'anything'
        component_db_id = COMPONENTS_DB_PREFIX + db_id
        expected_args = ('Component with id = "%s" not found', component_db_id)
        self.perform_unsuccessful_test(
            db_id, expected_args, log_method='warning')

    def test_successful(self):
        self.perform_successful_test('dummy')
        self.perform_successful_test(COMPONENTS_DB_PREFIX + 'dummy')

    def test_successful_from_url(self):
        self.perform_successful_test('from_url')

    def test_successful_with_secret(self):
        self.perform_successful_test('from_url_with_secret')

# vim:ts=4:sts=4:sw=4:tw=79:et:
