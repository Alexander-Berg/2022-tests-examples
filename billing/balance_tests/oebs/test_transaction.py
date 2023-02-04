# -*- coding: utf-8 -*-
from decimal import Decimal

import pytest
from mock import patch

from balance.constants import RegionId
from balance.mapper import Firm, FirmId
from balance.processors.oebs.dao.transaction import TransactionDao
from balance.processors.oebs.utils import FirmInfo, get_cursor_proxy_class
from tests import object_builder as ob

ALL_FIRMS = [FirmId.AUTORU,
             FirmId.YANDEX_INC,
             FirmId.YANDEX_OOO,
             FirmId.YANDEX_KZ,
             FirmId.YANDEX_ADS,
             FirmId.TAXI_AM,
             FirmId.UBER_KZ,
             FirmId.TAXI_KZ,
             FirmId.SERVICES_EU_AG,
             FirmId.AUTORU_EU_AG,
             FirmId.YANDEX_EU_AG,
             FirmId.HK_ECOMMERCE,
             FirmId.YANDEX_EU_NV,
             FirmId.TAXI_EU_BV,
             FirmId.UBER_ML_BV,
             FirmId.UBER_ML_BV_BYN,
             FirmId.YANDEX_TR,
             FirmId.MEDIASERVICES,
             FirmId.CLOUD,
             FirmId.GAS_STATIONS,
             FirmId.KINOPOISK,
             FirmId.TAXI,
             FirmId.VERTIKALI,
             FirmId.MARKET,
             FirmId.CLOUD_TECHNOLOGIES,
             FirmId.OFD,
             FirmId.BUS,
             FirmId.ZEN,
             FirmId.HEALTH_CLINIC,
             FirmId.DRIVE,
             FirmId.FOOD,
             FirmId.PROBKI
             ]


def get_tax_name(firm_id, resident, nds):
    if firm_id in [FirmId.YANDEX_INC]:
        return u'VAT FREE'

    if firm_id in [FirmId.YANDEX_KZ, FirmId.UBER_KZ, FirmId.TAXI_KZ, FirmId.KAZNET_MEDIA]:
        return u'ARKZ_НДС_20' if nds else u'ARKZ_БЕЗ_НДС'

    if firm_id in [FirmId.TAXI_AM]:
        return u'AM_VAT_FREE'

    if firm_id in [FirmId.SERVICES_EU_AG]:
        return u'CH20_VAT_20' if nds else u'CH_VAT_FREE'

    if firm_id in [FirmId.AUTORU_EU_AG, FirmId.YANDEX_EU_AG]:
        return u'CH_VAT_20' if nds else u'CH_VAT_FREE'

    if firm_id in [FirmId.HK_ECOMMERCE]:
        return u'HK90_VAT_FREE'

    if firm_id in [FirmId.YANDEX_EU_NV]:
        return u'NL10_VAT_{}'.format(nds) if nds else u'NL10_VAT_FREE'

    if firm_id in [FirmId.TAXI_EU_BV]:
        return u'NL31_VAT_FREE'

    if firm_id in [FirmId.UBER_ML_BV]:
        return u'NL35_VAT_{}'.format(nds) if nds else u'NL35_VAT_FREE'

    if firm_id in [FirmId.UBER_ML_BV_BYN]:
        return u'NL46_VAT_21' if nds else u'NL46_VAT_FREE'

    if firm_id in [FirmId.YANDEX_TR]:
        return u'TR_VAT_SALE_{}'.format(nds) if nds else u'TR_VAT_SALE_FREE'

    if nds:
        return u'НДС 20'
    else:
        return u'Без НДС (осв.)' if resident else u'Без НДС'


def get_invoice_type(firm_info, personal_account):
    if firm_info.region_id == RegionId.RUSSIA:
        return u'Счет на оплату'
    elif firm_info.region_id == RegionId.UKRAINE:
        return u'Счет на оплату Укр'
    else:
        if not personal_account and firm_info.id not in (FirmId.YANDEX_INC, FirmId.YANDEX_TR):
            return u'Account_bill'
        else:
            return u'Account'


def get_act_type(firm_info, currency, nds_pct, resident, barter):
    if firm_info.region_id == RegionId.RUSSIA:
        if barter == 1:
            return u'Акт бартерный'
        elif barter == 2:
            return u'Акт проч.бартер'
        elif currency != 'RUB':
            if currency in ('EUR', 'BYR', 'BYN', 'KZT'):
                return u'Акт валютный %s' % currency
            else:
                return u'Акт валютный'
        elif nds_pct:
            return u'Акт вып. работ'
        elif resident:
            return u'Акт без НДС (осв.)'
        else:
            return u'Акт без НДС'
    elif firm_info.region_id == RegionId.UKRAINE:
        return u'Акт вып. работ Укр'
    else:
        return u'Invoice'


TAXES_NAMES = [u'НДС 20', u'Без НДС (осв.)', u'Без НДС', u'VAT FREE',
               u'ARKZ_НДС_20', u'ARKZ_БЕЗ_НДС', u'AM_VAT_FREE',
               u'CH20_VAT_20', u'CH_VAT_FREE', u'CH_VAT_20',
               u'NL10_VAT_20', u'NL10_VAT_FREE', u'NL31_VAT_FREE',
               u'NL35_VAT_FREE', u'NL35_VAT_20', u'HK90_VAT_FREE',
               u'NL46_VAT_21', u'NL46_VAT_FREE',
               u'TR_VAT_SALE_20', u'TR_VAT_SALE_FREE']

INVOICE_TYPES = [u'Счет на оплату', u'Account',
                 u'Account_bill', u'Счет на оплату Укр',
                 u'Акт валютный', u'Акт бартерный',
                 u'Акт проч.бартер', u'Акт вып. работ',
                 u'Акт без НДС (осв.)', u'Акт без НДС',
                 u'Акт валютный EUR', u'Акт валютный BYR',
                 u'Акт валютный BYN', u'Акт валютный KZT',
                 u'Invoice']

CursorProxy = get_cursor_proxy_class()


class MockCursor(CursorProxy):
    def __init__(self, *args, **kwargs):
        super(MockCursor, self).__init__(*args, **kwargs)
        self.query = None
        self.query_params = None
        self.taxes = None
        self.invoice_types = None
        self.ls_type_id = None

    def var(self, type_):
        class MockVar(object):
            def __init__(self, value):
                self.value = value

            def getvalue(self):
                return self.value

            def setvalue(self, value_1, value_2):
                return None

        return MockVar(13)

    def execute(self, query, *args, **kwargs):
        self.query = ' '.join(query.split())
        self.query_params = args

    def set_oebs_answers(self, taxes=None, invoice_types=0, ls_type_id=None):
        self.taxes = taxes
        self.invoice_types = invoice_types if invoice_types else 0
        self.ls_type_id = ls_type_id

    def fetchall(self):
        if self.query == 'select party_id from hz_parties where orig_system_reference = :1':
            return [['']]
        if self.query == 'select tax_code, vat_tax_id from ar_vat_tax_vl':
            return [(tax_name, index) for index, tax_name in enumerate(TAXES_NAMES)]
        if self.query == 'select * from ar_trx_errors_gt':
            return []
        if self.query == 'select attribute13, inventory_item_id, primary_uom_code from mtl_system_items_fvl where attribute_category = \'AR.-\' AND organization_id = 101 AND attribute13 in (\'1475\')':
            return [(1475, 5, 10)]
        if self.query == 'select cust_trx_type_id from ra_cust_trx_types where name = :1':
            invoice_types = {tax_name: index for index, tax_name in enumerate(self.invoice_types)}
            type_ = self.query_params[0][0]
            return [[invoice_types[type_.decode('utf8') if type(type_) is str else type_]]]
        if self.query == 'select cust_trx_type_id from ra_cust_trx_types where type=:1 and attribute1=:2':
            return [[self.ls_type_id]]
        if self.query == 'begin :sequence_num := XXCMN_INTAPI_BILLS_PKG.GET_TRX_NUMBER(:dt); end;':
            return 0
        return [[0]]


def create_transaction(transaction, firm_info, nds_pct, resident, personal_account=None, currency=None,
                       ref_eid=None, dt=None, barter=None):
    transaction.create_transaction(bank_code=None, barter=barter,
                                   client_id=None, contact_id=None,
                                   contract_id=None, currency=currency,
                                   dt=dt, eid=None, is_agency=None,
                                   manager_code=None, person_id='1',
                                   rate=None, ref_eid=ref_eid, personal_account=personal_account,
                                   remainder=None, resident=resident, rows=[{'product_id': 1475,
                                                                             'price': Decimal('1'),
                                                                             'discount_pct': Decimal('23'),
                                                                             'quantity': 1,
                                                                             'sum': 1,
                                                                             'nds_sum': 1,
                                                                             'nsp_sum': 0,
                                                                             'service_id': 7,
                                                                             'client_id': 1,
                                                                             'nds_pct': 20}],
                                   tax=(nds_pct, 0),
                                   # firm_id=firm_id)
                                   firm_info=firm_info)


@pytest.mark.parametrize('nds_pct, resident', [(20, 1), (0, 1), (0, 0)])
@pytest.mark.parametrize('firm_id', ALL_FIRMS)
def test_vat_tax_id(session, firm_id, nds_pct, resident):
    cursor = MockCursor(session.connection().connection.cursor())
    transaction = TransactionDao(cursor, '')
    transaction.cursor.set_oebs_answers(taxes=TAXES_NAMES,
                                        invoice_types=INVOICE_TYPES)
    firm = ob.Getter(Firm, firm_id).build(session).obj
    with patch.object(TransactionDao, 'callproc', return_value=1) as mock_method:
        create_transaction(transaction, firm_info=FirmInfo(firm), nds_pct=nds_pct, resident=resident)
    lines = mock_method.mock_calls[0][1][6]
    tax_name = get_tax_name(firm_id, resident, nds_pct)
    assert lines.data[1].data['vat_tax_id'] == TAXES_NAMES.index(tax_name)


@pytest.mark.parametrize('personal_account', [0, 1])
@pytest.mark.parametrize('firm_id', ALL_FIRMS)
def test_cust_trx_type_name_invoice(session, firm_id, personal_account):
    cursor = MockCursor(session.connection().connection.cursor())
    transaction = TransactionDao(cursor, '')
    ls_type_id = ob.get_big_number()
    transaction.cursor.set_oebs_answers(taxes=TAXES_NAMES,
                                        invoice_types=INVOICE_TYPES,
                                        ls_type_id=ls_type_id)
    firm_info = FirmInfo(ob.Getter(Firm, firm_id).build(session).obj)
    with patch.object(TransactionDao, 'callproc', return_value=1) as mock_method:
        create_transaction(transaction, firm_info=firm_info, nds_pct=20, resident=1,
                           personal_account=personal_account)
    lines = mock_method.mock_calls[0][1][5]

    if personal_account:
        assert lines.data[0].data['cust_trx_type_id'] == ls_type_id
    else:
        invoice_type = get_invoice_type(firm_info, personal_account)
        assert lines.data[0].data['cust_trx_type_id'] == INVOICE_TYPES.index(invoice_type)


@pytest.mark.parametrize('nds_pct, resident', [(20, 1), (0, 1), (0, 0)])
@pytest.mark.parametrize('currency', ['RUB', 'EUR', 'BYR', 'BYN', 'KZT', 'USD'])
@pytest.mark.parametrize('firm_id', [FirmId.YANDEX_OOO])
def test_cust_trx_type_name_act_ru(session, firm_id, nds_pct, resident, currency):
    cursor = MockCursor(session.connection().connection.cursor())
    transaction = TransactionDao(cursor, '')
    transaction.cursor.set_oebs_answers(taxes=TAXES_NAMES,
                                        invoice_types=INVOICE_TYPES)
    firm_info = FirmInfo(ob.Getter(Firm, firm_id).build(session).obj)
    with patch.object(TransactionDao, 'callproc', return_value=1) as mock_method:
        create_transaction(transaction, firm_info=firm_info, nds_pct=nds_pct, resident=resident,
                           currency=currency, ref_eid=1, dt=session.now())
    lines = mock_method.mock_calls[0][1][5]
    act_type = get_act_type(firm_info, currency, nds_pct, resident, None)
    assert lines.data[0].data['cust_trx_type_id'] == INVOICE_TYPES.index(act_type)


@pytest.mark.parametrize('nds_pct, resident', [(20, 1), (0, 0)])
@pytest.mark.parametrize('currency', ['RUB', 'EUR', 'BYR', 'BYN', 'KZT', 'USD'])
@pytest.mark.parametrize('firm_id', [FirmId.YANDEX_INC,
                                     FirmId.SERVICES_EU_AG])
def test_cust_trx_type_name_act_non_ru(session, firm_id, nds_pct, resident, currency):
    cursor = MockCursor(session.connection().connection.cursor())
    transaction = TransactionDao(cursor, '')
    transaction.cursor.set_oebs_answers(taxes=TAXES_NAMES,
                                        invoice_types=INVOICE_TYPES)
    firm_info = FirmInfo(ob.Getter(Firm, firm_id).build(session).obj)
    with patch.object(TransactionDao, 'callproc', return_value=1) as mock_method:
        create_transaction(transaction, firm_info=firm_info, nds_pct=nds_pct, resident=resident,
                           currency=currency, ref_eid=1, dt=session.now())
    lines = mock_method.mock_calls[0][1][5]
    assert lines.data[0].data['cust_trx_type_id'] == INVOICE_TYPES.index(u'Invoice')


@pytest.mark.parametrize('barter', [1, 2, 3])
@pytest.mark.parametrize('nds_pct, resident', [(20, 1), (0, 1), (0, 0)])
@pytest.mark.parametrize('firm_id', [FirmId.YANDEX_OOO])
def test_cust_trx_type_name_act_barter(session, firm_id, nds_pct, barter, resident):
    cursor = MockCursor(session.connection().connection.cursor())
    transaction = TransactionDao(cursor, '')
    transaction.cursor.set_oebs_answers(taxes=TAXES_NAMES,
                                        invoice_types=INVOICE_TYPES)
    firm_info = FirmInfo(ob.Getter(Firm, firm_id).build(session).obj)
    with patch.object(TransactionDao, 'callproc', return_value=1) as mock_method:
        create_transaction(transaction, firm_info=firm_info, nds_pct=nds_pct, resident=resident,
                           currency='RUB', ref_eid=1, dt=session.now(), barter=barter)
    lines = mock_method.mock_calls[0][1][5]
    act_type = get_act_type(firm_info, 'RUB', nds_pct, resident, barter)
    assert lines.data[0].data['cust_trx_type_id'] == INVOICE_TYPES.index(act_type)
