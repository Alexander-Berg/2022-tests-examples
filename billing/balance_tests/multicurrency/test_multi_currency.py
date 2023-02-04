# -*- coding: utf-8 -*-
import StringIO
import csv
import logging

from tests.base import MediumTest
from tests.object_builder import ClientBuilder, PersonBuilder, InvoiceBuilder

log = logging.getLogger('test_multi_currency')


class TestMultiCurrency(MediumTest):
    def test_GetDirectDiscount(self):
        res = self.xmlrpcserver.GetDirectDiscount({
            'Mod': '1000000000',
            'Rem': '327380'
        })
        csvfile = StringIO.StringIO(res)
        reader = csv.reader(csvfile, delimiter='\t')
        rows = [row for row in reader]
        self.assert_(len(rows) > 0)
        self.assertEqual('CLIENT_ID', rows[0][0])

    def test_GetClientNDS(self):
        res = self.xmlrpcserver.GetClientNDS({
            'Mod': '1000000000',
            'Rem': '11267'
        })
        csvfile = StringIO.StringIO(res)
        reader = csv.reader(csvfile, delimiter='\t')
        rows = [row for row in reader]
        self.assert_(len(rows) > 0)
        self.assertEqual('CLIENT_ID', rows[0][0])

    def test_GetFirmCountryCurrency_default(self):
        res = self.xmlrpcserver.GetFirmCountryCurrency({})
        self.assertEqual(3, len(res))
        self.assertEqual(0, res[0])

    def test_GetFirmCountryCurrency_empty_client(self):
        client = ClientBuilder()
        client.build(self.session)
        self.session.flush()
        res = self.xmlrpcserver.GetFirmCountryCurrency(dict(client_id=client.obj.id))
        self.assertEqual(3, len(res))
        self.assertEqual(0, res[0])
        self.assert_(len(res[2]) > 100)

    def test_GetFirmCountryCurrency_client_by_region(self):
        client = ClientBuilder()
        client.build(self.session)
        self.session.flush()
        res = self.xmlrpcserver.GetFirmCountryCurrency(
            dict(client_id=client.obj.id, region_id=225))
        self.assertEqual(3, len(res))
        self.assertEqual(0, res[0])
        self.assertEqual(2, len(res[2]))
        self.assertListEqual(
            [
                {
                    'agency': 0,
                    'currency': 'RUB',
                    'firm_id': 1,
                    'region_id': 225,
                    'region_name': u'Россия',
                    'region_name_en': 'Russia',
                    'resident': 1,
                },
                {
                    'agency': 1,
                    'currency': 'RUB',
                    'firm_id': 1,
                    'region_id': 225,
                    'region_name': u'Россия',
                    'region_name_en': 'Russia',
                    'resident': 1,
                },
            ],
            sorted(res[2])
        )

    def test_GetFirmCountryCurrency_client_w_region(self):
        client = ClientBuilder(region_id=225)
        client.build(self.session)
        self.session.flush()
        res = self.xmlrpcserver.GetFirmCountryCurrency(dict(client_id=client.obj.id))
        self.assertEqual(3, len(res))
        self.assertEqual(0, res[0])
        self.assertListEqual([{
            'agency': 0,
            'currency': 'RUB',
            'firm_id': 1,
            'region_id': 225,
            'region_name': u'Россия',
            'region_name_en': 'Russia',
            'resident': 1
        }], res[2])

    def test_GetFirmCountryCurrency_client_w_region_modify(self):
        client = ClientBuilder(region_id=225)
        person = PersonBuilder(client=client)
        InvoiceBuilder(person=person).build(self.session)
        client.build(self.session)
        self.session.flush()
        res = self.xmlrpcserver.GetFirmCountryCurrency(dict(client_id=client.obj.id))
        self.assertEqual(3, len(res))
        self.assertEqual(0, res[0])
        self.assertListEqual([{
            'agency': 0,
            'currency': 'RUB',
            'firm_id': 1,
            'region_id': 225,
            'region_name': u'Россия',
            'region_name_en': 'Russia',
            'resident': 1,
            'convert_type_modify': 1
        }], res[2])

    def test_GetFirmCountryCurrency_client_w_region_sw(self):
        client = ClientBuilder(region_id=126)
        person = PersonBuilder(client=client, type='sw_ur')
        InvoiceBuilder(person=person).build(self.session)
        client.build(self.session)
        self.session.flush()
        res = self.xmlrpcserver.GetFirmCountryCurrency(dict(client_id=client.obj.id))
        self.assertEqual(3, len(res))
        self.assertEqual(0, res[0])
        self.assertListEqual([
            {
                'agency': 0,
                'currency': 'CHF',
                'firm_id': 7,
                'region_id': 126,
                'region_name': u'Швейцария',
                'region_name_en': 'Switzerland',
                'resident': 1,
            }, {
                'agency': 0,
                'currency': 'EUR',
                'firm_id': 7,
                'region_id': 126,
                'region_name': u'Швейцария',
                'region_name_en': 'Switzerland',
                'resident': 1,
            }, {
                'agency': 0,
                'currency': 'USD',
                'firm_id': 7,
                'region_id': 126,
                'region_name': u'Швейцария',
                'region_name_en': 'Switzerland',
                'resident': 1,
            },
        ], sorted(res[2]))
