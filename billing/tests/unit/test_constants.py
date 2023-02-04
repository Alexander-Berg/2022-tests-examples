# -*- coding: utf-8 -*-

import unittest
from xml.etree import ElementTree as et

from billing.dcs.dcs.temporary.butils.dbhelper.dsn import DSN

from billing.dcs.dcs import constants


class DbBackendElementToDSNTestCase(unittest.TestCase):
    def perform_test(self, left, right):
        self.assertEqual(left.id, right.id)

        if left.dialect == right.dialect == 'yt':
            self.assertEqual(left.proxy, right.proxy)
            self.assertEqual(left.token, right.token)
        else:
            self.assertEqual(left.to_str(), right.to_str())

    @staticmethod
    def format_dsn(id_, type_, host=None, user=None, password=None,
                   schema=None, proxy=None, token=None, **kwargs):
        dsn = DSN(dialect=type_, host=host, user=user, password=password,
                  schema=schema, **kwargs)
        dsn.id = id_
        dsn.proxy = proxy
        dsn.token = token
        return dsn

    def test_oracle_backend(self):
        db_backend = et.Element('DbBackend', id='balance', type='oracle')
        et.SubElement(db_backend, 'Host').text = 'host'
        et.SubElement(db_backend, 'User').text = 'user'
        et.SubElement(db_backend, 'Pass').text = 'password'
        et.SubElement(db_backend, 'Scheme').text = 'schema'

        dsn = constants._db_backend_element_to_dsn(db_backend)
        expected_dsn = self.format_dsn(
            'balance', 'oracle', 'host', 'user', 'password', 'schema')

        # noinspection PyTypeChecker
        self.perform_test(dsn, expected_dsn)

    def test_yt_backend(self):
        db_backend = et.Element('DbBackend', id='hahn', type='yt')
        db_backend_proxy = et.SubElement(db_backend, 'Proxy')
        et.SubElement(db_backend_proxy, 'Url').text = 'https://yt'
        et.SubElement(db_backend, 'Token').text = 'token'

        dsn = constants._db_backend_element_to_dsn(db_backend)
        expected_dsn = self.format_dsn(
            'hahn', 'yt', proxy={'url': 'https://yt'}, token='token')

        # noinspection PyTypeChecker
        self.perform_test(dsn, expected_dsn)

    def test_backend_from_url(self):
        db_backend = et.Element('DbBackend', id='balance')
        et.SubElement(db_backend, 'URL').text = \
            'oracle://user:password@host?schema=schema'

        dsn = constants._db_backend_element_to_dsn(db_backend)
        expected_dsn = self.format_dsn(
            'balance', 'oracle', 'host', 'user', 'password', 'schema')

        # noinspection PyTypeChecker
        self.perform_test(dsn, expected_dsn)

# vim:ts=4:sts=4:sw=4:tw=79:et:
