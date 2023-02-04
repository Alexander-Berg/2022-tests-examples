# -*- coding: utf-8 -*-
import datetime
import hamcrest
import pytest
import simplejson
from balance import mapper, constants as cst, muzzle_util as ut
from billing.contract_iface.contract_meta import collateral_types
from balance.processors.oebs.dao.contract import AttributeMapper, oebs_commission_type
from balance.processors.oebs.utils import oebs_currency_cc_by_code
from balance.processors.oebs_api.api import _json_default
from balance.processors.oebs_api.wrappers.contract import ContractWrapper
from decimal import Decimal as D
from tests import object_builder as ob

MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=30)
TWO_MONTH_BEFORE = (datetime.datetime.now() - datetime.timedelta(days=60)).replace(microsecond=0)
NOW = datetime.datetime.now()
MONTH_LATER = datetime.datetime.now() + datetime.timedelta(days=30)
TWO_MONTH_LATER = datetime.datetime.now() + datetime.timedelta(days=60)


@pytest.mark.parametrize('firm_id', [1, 2, 7, 111])
@pytest.mark.parametrize('doc_set', [1, 2, 3, 4])
@pytest.mark.parametrize('contract_type', [1, 5, 8])
def test_contract_type_partner(session, contract_type, doc_set, firm_id):
    contract = ob.ContractBuilder.construct(session, ctype='PARTNERS',
                                            contract_type=contract_type,
                                            doc_set=doc_set,
                                            firm=firm_id)
    session.flush()
    prev_type = oebs_commission_type(contract)
    new_type = ContractWrapper(contract).get_info().get('type', None)
    assert prev_type == new_type


@pytest.mark.parametrize('firm_id', [1, 7, 13, 16, 22, 29, 33, 111])
@pytest.mark.parametrize('contract_type', [1, 2, 3, 4, 5, 6, 7])
def test_contract_type_distribution(session, contract_type, firm_id):
    contract = ob.ContractBuilder.construct(session, ctype='DISTRIBUTION',
                                            contract_type=contract_type,
                                            firm=firm_id)
    session.flush()
    prev_type = oebs_commission_type(contract)
    new_type = ContractWrapper(contract).get_info().get('type', None)
    assert prev_type == new_type


@pytest.mark.parametrize('firm_id', [1, 7, 13, 16, 22, 29, 33, 111])
@pytest.mark.parametrize('contract_type', [1, 2, 3, 4, 5, 6, 7])
def test_contract_type_general(session, contract_type, firm_id):
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            contract_type=contract_type,
                                            firm=firm_id)
    session.flush()
    prev_type = oebs_commission_type(contract)
    new_type = ContractWrapper(contract).get_info().get('type', None)
    assert prev_type == new_type


@pytest.mark.parametrize('services', [
    {cst.ServiceId.TAXI_CORP, cst.ServiceId.TAXI_CORP_PARTNERS},
    # 2 разных сервиса
    {cst.ServiceId.MARKETING_COROBA, cst.ServiceId.MARKETPLACE},
    {cst.ServiceId.MARKETING_COROBA},
    {cst.ServiceId.MARKETPLACE},
    {cst.ServiceId.ZEN},
    {cst.ServiceId.TOLOKA},
    {cst.ServiceId.TELEMED_PROMO},
    {cst.ServiceId.BUSES_PROMO},
    {cst.ServiceId.DMP},
    {cst.ServiceId.BLUE_SUB},
    {cst.ServiceId.BLUE_REF},
    {cst.ServiceId.RED_SUB},
    {cst.ServiceId.BOYSCOUTS},
    {cst.ServiceId.ADDAPPTER_2},
    {cst.ServiceId.CLOUD_MARKETPLACE},
    {cst.ServiceId.ZAXI_SPENDABLE},
    {cst.ServiceId.DRIVE_REFUELLER_SPENDABLE},
    {cst.ServiceId.FOOD_COURIERS_SPENDABLE},
    {cst.ServiceId.LAVKA_COURIERS_SPENDABLE},
    {cst.ServiceId.BUG_BOUNTY}
])
def test_contract_type_spendable(session, services):
    contract = ob.ContractBuilder.construct(session, ctype='SPENDABLE',
                                            contract_type=87,
                                            services=services,
                                            firm=1,
                                            )
    session.flush()
    prev_type = oebs_commission_type(contract)
    new_type = ContractWrapper(contract).get_info()['type']
    assert prev_type == new_type


def test_empty_contract_and_collateral_partner(session):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=cst.FirmId.YANDEX_OOO)
    contract.append_collateral(dt=NOW,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               NDS=18)
    session.flush()
    for code in ['PAYMENT_TYPE', 'MANAGER_CODE']:
        (session.query(mapper.AttributeValue)
         .filter(mapper.AttributeValue.code == code,
                 mapper.AttributeValue.attribute_batch_id == contract.col0.attribute_batch_id
                 )).delete()
    session.refresh(contract)
    contract.col0.maybe_create_barcode()
    del contract.client_id
    del contract.col0.is_signed
    info = ContractWrapper(contract).get_info()
    hamcrest.assert_that(info, hamcrest.equal_to({'entity_id': str(contract.id),
                                                  'contract_num': contract.external_id,
                                                  'oe_code': u'YARU',
                                                  'other_revenue': 'N',
                                                  'partner_contract_id': str(contract.id),
                                                  'contragent_guid': 'P{}'.format(contract.person.id),
                                                  'start_date': contract.col0.dt,
                                                  'collaterals': [{'reference_id': contract.col0.id,
                                                                   'barcode': contract.collaterals[0].print_tpl_barcode,
                                                                   'coltype': 0,
                                                                   'start_date': contract.col0.dt,
                                                                   'live_signed': 'N',
                                                                   'cancel_flag': 'N',
                                                                   'fax_signed': 'N',
                                                                   'export_endocs': 'N',
                                                                   'terms': {'currency_code': 'RUB'}
                                                                   },

                                                                  {'reference_id': contract.collaterals[1].id,
                                                                   'barcode': contract.collaterals[1].print_tpl_barcode,
                                                                   'coltype': 2010,
                                                                   'colnum': '01',
                                                                   'start_date': contract.collaterals[1].dt,
                                                                   'live_signed': 'N',
                                                                   'cancel_flag': 'N',
                                                                   'fax_signed': 'N',
                                                                   'export_endocs': 'N',
                                                                   'terms': {'nds_percent': 20,
                                                                             'nds_flag': 'Y'}
                                                                   }
                                                                  ],
                                                  'is_oferta': 'N',
                                                  'entity_type': 'CONTRACT',
                                                  'type': 30}))


def test_empty_contract_and_collateral_spendable(session):
    service = cst.ServiceId.TAXI_CORP
    contract = ob.ContractBuilder.construct(session,
                                            ctype='SPENDABLE',
                                            contract_type=87,
                                            services={service},
                                            firm=1)
    contract.append_collateral(dt=NOW,
                               collateral_type=collateral_types['SPENDABLE'][7010],
                               nds=12)
    session.flush()
    for code in ['PAYMENT_TYPE', 'MANAGER_CODE']:
        (session.query(mapper.AttributeValue)
         .filter(mapper.AttributeValue.code == code,
                 mapper.AttributeValue.attribute_batch_id == contract.col0.attribute_batch_id
                 )).delete()
    session.refresh(contract)
    contract.col0.maybe_create_barcode()
    del contract.client_id
    del contract.col0.is_signed
    info = ContractWrapper(contract).get_info()
    hamcrest.assert_that(info, hamcrest.equal_to({'entity_id': str(contract.id),
                                                  'contract_num': contract.external_id,
                                                  'oe_code': u'YARU',
                                                  'other_revenue': 'N',
                                                  'partner_contract_id': str(contract.id),
                                                  'contragent_guid': 'P{}'.format(contract.person.id),
                                                  'start_date': contract.col0.dt,
                                                  'collaterals': [{'reference_id': contract.col0.id,
                                                                   'barcode': contract.collaterals[0].print_tpl_barcode,
                                                                   'coltype': 0,
                                                                   'start_date': contract.col0.dt,
                                                                   'live_signed': 'N',
                                                                   'cancel_flag': 'N',
                                                                   'fax_signed': 'N',
                                                                   'export_endocs': 'N',
                                                                   'terms': {'service': set([service]),
                                                                             'currency_code': 'RUB'}
                                                                   },

                                                                  {'reference_id': contract.collaterals[1].id,
                                                                   'barcode': contract.collaterals[1].print_tpl_barcode,
                                                                   'coltype': 7010,
                                                                   'colnum': '01',
                                                                   'start_date': contract.collaterals[1].dt,
                                                                   'live_signed': 'N',
                                                                   'cancel_flag': 'N',
                                                                   'fax_signed': 'N',
                                                                   'export_endocs': 'N',
                                                                   'terms': {'nds_flag': 'Y',
                                                                             'nds_percent': 12}
                                                                   }
                                                                  ],
                                                  'is_oferta': 'N',
                                                  'entity_type': 'CONTRACT',
                                                  'type': 81}))


@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
@pytest.mark.parametrize('currency_code_contract, currency_char_code_contract', [(None, 'RUB'),
                                                                                 (398, 'KZT'),
                                                                                 (810, 'RUB'),
                                                                                 (840, 'USD')
                                                                                 ])
@pytest.mark.parametrize('currency_code_collateral, currency_char_code_collateral', [
    (None, None),
    (978, 'EUR')
])
def test_contract_currency_code(session, currency_code_contract, currency_char_code_contract, w_signed_contract,
                                w_signed_collateral, currency_code_collateral, currency_char_code_collateral):
    """ОЕБС должен собирать по подписанным последний"""
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=cst.FirmId.MARKET,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                            currency=currency_code_contract)
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               memo='memo',
                               collateral_type=collateral_types['PARTNERS'][2010],
                               currency=currency_code_collateral)
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    ccurrent = contract.current_signed() if contract.signed else contract.current
    json_info = simplejson.dumps(ContractWrapper(contract).get_info(), default=_json_default,
                                 ensure_ascii=False).encode('utf8')
    currency_bill = ccurrent.currency
    currency_oebs = oebs_currency_cc_by_code(session, ccurrent.currency)
    assert collaterals_info[0]['terms']['currency_code'] == currency_char_code_contract
    if currency_char_code_collateral is None:
        assert 'currency_code' not in collaterals_info[1]['terms']
    else:
        assert collaterals_info[1]['terms']['currency_code'] == currency_char_code_collateral


def test_contract_entity_id(session):
    contract = ob.ContractBuilder.construct(session, ctype='PARTNERS', contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=cst.FirmId.YANDEX_OOO)
    assert ContractWrapper(contract).get_info()['entity_id'] == str(contract.id)


def test_contract_contract_num(session):
    contract = ob.ContractBuilder.construct(session, ctype='PARTNERS', contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=cst.FirmId.YANDEX_OOO)
    assert ContractWrapper(contract).get_info()['contract_num'] == contract.external_id


@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
def test_contract_start_date_partner(session, w_signed_contract, w_signed_collateral):
    """намеренно не установлен признак PRINT_FORM_DT, потому что такого нет в проде для партнерских договоров"""
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None)
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2010])
    session.flush()
    ccurrent = contract.current_signed() if contract.signed else contract.current
    assert ContractWrapper(contract).get_info()['start_date'] == TWO_MONTH_BEFORE
    assert ContractWrapper(contract).get_info()['start_date'] == ccurrent.dt


def test_contract_partner_contract_id(session):
    contract = ob.ContractBuilder.construct(session, ctype='PARTNERS', contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=cst.FirmId.YANDEX_OOO)
    assert ContractWrapper(contract).get_info()['partner_contract_id'] == str(contract.id)


@pytest.mark.parametrize('firm, oe_code', [(cst.FirmId.YANDEX_EU_AG, 'YECH'),
                                           (cst.FirmId.YANDEX_OOO, 'YARU')])
def test_contract_oe_code(session, firm, oe_code):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=firm)
    assert ContractWrapper(contract).get_info()['oe_code'] == oe_code


@pytest.mark.parametrize('w_manager, w_manager_login', [(False, False),
                                                        (True, False),
                                                        (True, False)])
def test_contract_manager(session, w_manager, w_manager_login):
    """Менеджеры всегда только в договоре (нулевом допнике)"""
    manager = ob.SingleManagerBuilder.construct(session,
                                                domain_login=ob.generate_character_string(2) if w_manager_login else '')
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=cst.FirmId.YANDEX_OOO)

    contract.col0.manager_code = manager.manager_code if w_manager else None
    session.flush()
    ccurrent = contract.current_signed() if contract.signed else contract.current
    info = ContractWrapper(contract).get_info()
    if w_manager:
        if w_manager_login:
            assert info['manager'] == {'code': manager.manager_code,
                                       'login': manager.domain_login}
            assert info['manager']['code'] == ccurrent.manager_code
        else:
            assert info['manager'] == {'code': manager.manager_code}
            assert info['manager']['code'] == ccurrent.manager_code
    else:
        assert 'manager' not in info


@pytest.mark.parametrize('w_bo_manager, w_manager_login', [(False, False),
                                                           (True, False),
                                                           (True, False)])
def test_contract_bo_manager(session, w_bo_manager, w_manager_login):
    """Менеджеры бекофиса в партнерском договоре не выгружаются"""
    manager = ob.SingleManagerBuilder.construct(session,
                                                domain_login=ob.generate_character_string(2) if w_manager_login else '')
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=cst.FirmId.YANDEX_OOO)
    contract.col0.manager_bo_code = manager.manager_code if w_bo_manager else None
    session.flush()
    info = ContractWrapper(contract).get_info()
    assert 'bo_manager' not in info


@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
def test_contract_is_oferta_partner(session, w_signed_contract, w_signed_collateral):
    """для партнерского договора всегда N"""
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_offer=1,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None)
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_offer=1,
                               is_signed=TWO_MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2010])
    session.flush()
    mp = AttributeMapper(contract.current_signed() if contract.signed else contract.current, None)
    assert mp.is_offer[0]['term_value'] == '0'
    assert ContractWrapper(contract).get_info()['is_oferta'] == 'N'


def test_contract_client_guid(session):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=NOW,
                                            firm=cst.FirmId.YANDEX_OOO
                                            )

    session.flush()
    assert ContractWrapper(contract).get_info()['client_guid'] == 'C{}'.format(contract.client_id)


@pytest.mark.parametrize('w_signed_collateral', [True, False])
def test_collateral_start_date_partner(session, w_signed_collateral):
    """намеренно не установлен признак PRINT_FORM_DT, потому что такого нет в проде для партнерских договоров"""
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=NOW if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['start_date'] == TWO_MONTH_BEFORE
    assert collaterals_info[1]['start_date'] == MONTH_BEFORE


@pytest.mark.parametrize('w_fax_signed_contract, w_fax_signed_collateral', [(None, None),
                                                                            (TWO_MONTH_BEFORE, None),
                                                                            (TWO_MONTH_BEFORE, MONTH_BEFORE)])
def test_collateral_fax_signed(session, w_fax_signed_contract, w_fax_signed_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_faxed=w_fax_signed_contract,
                                            firm=cst.FirmId.YANDEX_OOO
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_faxed=w_fax_signed_collateral,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['fax_signed'] == ('Y' if w_fax_signed_contract else 'N')
    assert collaterals_info[1]['fax_signed'] == ('Y' if w_fax_signed_collateral else 'N')


@pytest.mark.parametrize('is_cancelled_contract, is_cancelled_collateral', [(None, None),
                                                                            (TWO_MONTH_BEFORE, None),
                                                                            (TWO_MONTH_BEFORE, MONTH_BEFORE)])
def test_collateral_cancel_flag(session, is_cancelled_contract, is_cancelled_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_cancelled=is_cancelled_contract,
                                            firm=cst.FirmId.YANDEX_OOO
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_cancelled=is_cancelled_collateral,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['cancel_flag'] == ('Y' if is_cancelled_contract else 'N')
    assert collaterals_info[1]['cancel_flag'] == ('Y' if is_cancelled_collateral else 'N')


@pytest.mark.parametrize('sent_dt_contract, sent_dt_collateral', [(None, None),
                                                                  (TWO_MONTH_BEFORE, None),
                                                                  (TWO_MONTH_BEFORE, MONTH_BEFORE)])
def test_collateral_sent_date(session, sent_dt_contract, sent_dt_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            sent_dt=sent_dt_contract,
                                            firm=cst.FirmId.YANDEX_OOO
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               sent_dt=sent_dt_collateral,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if sent_dt_contract:
        assert collaterals_info[0]['sent_date'] == sent_dt_contract
    else:
        assert 'sent_date' not in collaterals_info[0]
    if sent_dt_collateral:
        assert collaterals_info[1]['sent_date'] == sent_dt_collateral
    else:
        assert 'sent_date' not in collaterals_info[1]


@pytest.mark.parametrize('collateral_type', [2010,  # ДС
                                             2090  # Уведомление
                                             ])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(None, None),
                                                                    (TWO_MONTH_BEFORE, None),
                                                                    (TWO_MONTH_BEFORE, MONTH_BEFORE)])
def test_collateral_live_signed(session, w_signed_contract, w_signed_collateral, collateral_type):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            is_signed=w_signed_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=w_signed_collateral,
                               collateral_type=collateral_types['PARTNERS'][collateral_type],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['live_signed'] == ('Y' if w_signed_contract else 'N')
    if collateral_type == 2090:
        assert collaterals_info[1]['live_signed'] == 'Y'
    else:
        assert collaterals_info[1]['live_signed'] == ('Y' if w_signed_collateral else 'N')


@pytest.mark.parametrize('w_signed_collateral', [True, False])
def test_collateral_reference_id(session, w_signed_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=NOW,
                                            firm=cst.FirmId.YANDEX_OOO
                                            )
    contract.append_collateral(dt=NOW,
                               is_signed=NOW if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['reference_id'] == contract.collaterals[0].id
    assert collaterals_info[1]['reference_id'] == contract.collaterals[1].id


@pytest.mark.parametrize('w_signed_collateral', [True, False])
def test_collateral_search_forms(session, w_signed_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            search_forms=1,
                                            dt=TWO_MONTH_BEFORE,
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=NOW if w_signed_collateral else None,
                               search_forms=0,
                               collateral_type=collateral_types['PARTNERS'][2010])
    session.flush()
    mp_0 = AttributeMapper(contract.collaterals[0], None)
    assert mp_0.search_forms[0]['term_value'] == '1'
    mp_1 = AttributeMapper(contract.collaterals[1], None)
    assert mp_1.search_forms[0]['term_value'] == '-1'
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['search_forms'] == 'Y'
    assert collaterals_info[1]['terms']['search_forms'] == 'N'


@pytest.mark.parametrize('collateral_type', [2010,  # ДС
                                             2090  # Уведомление
                                             ])
def test_collateral_colnum(session, collateral_type):
    # TOdo
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['PARTNERS'][collateral_type],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert 'colnum' not in collaterals_info[0]
    if collateral_type != 2090:
        assert collaterals_info[1]['colnum'] == '01'
    else:
        assert 'colnum' not in collaterals_info[0]


def test_collateral_coltype(session):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['coltype'] == 0
    assert collaterals_info[1]['coltype'] == 2010


@pytest.mark.parametrize('print_template_collateral', ['133123', None])
@pytest.mark.parametrize('print_template_contract', ['1123', None])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
def test_collateral_export_endocs(session, w_signed_contract, w_signed_collateral, print_template_collateral,
                                  print_template_contract):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            firm=1,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None)
    contract.col0.print_template = print_template_contract
    contract.append_collateral(dt=MONTH_BEFORE,
                               print_template=print_template_collateral,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']

    assert collaterals_info[0]['export_endocs'] == ('Y' if print_template_contract else 'N')
    assert collaterals_info[0]['export_endocs'] == ('Y' if getattr(contract.collaterals[0], 'print_template',
                                                                   None) else 'N')

    assert collaterals_info[1]['export_endocs'] == ('Y' if print_template_collateral else 'N')
    assert collaterals_info[1]['export_endocs'] == ('Y' if getattr(contract.collaterals[1], 'print_template',
                                                                   None) else 'N')


@pytest.mark.parametrize('contract_nds', [18, None])
@pytest.mark.parametrize('collateral_nds', [18, None])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
def test_collateral_nds(session, w_signed_contract, w_signed_collateral, contract_nds, collateral_nds):
    """Историю надо собирать только по подписанным допникам, если подписан договор"""
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            nds=contract_nds,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               nds=collateral_nds,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']

    # старый эскпорт история
    ndses = contract.get_attribute_history('nds', check_signed=contract.signed)
    ndses_orig = contract.get_attribute_history('nds', check_signed=contract.signed)
    if len(ndses) != 2:
        if contract_nds is None:
            ndses.insert(0, (None, None))
        if collateral_nds is None or (w_signed_contract and not w_signed_collateral):
            ndses.append((None, None))
    # старый эскпорт история

    # старый эскпорт term_code
    nds_0 = AttributeMapper(contract.collaterals[0], None).nds[0]['term_value']
    nds_1 = AttributeMapper(contract.collaterals[1], None).nds[0]['term_value']
    # старый эскпорт term_code

    json_info = simplejson.dumps(ContractWrapper(contract).get_info(), default=_json_default,
                                 ensure_ascii=False).encode('utf8')
    if contract_nds is not None:
        assert collaterals_info[0]['terms']['nds_flag'] == ('Y' if contract_nds else 'N')
        assert collaterals_info[0]['terms']['nds_percent'] == (20 if contract_nds else None)
        assert collaterals_info[0]['terms']['nds_percent'] == (20 if ndses[0][1] else None)
    else:
        assert 'nds_flag' not in collaterals_info[0]['terms']
        assert 'nds_percent' not in collaterals_info[0]['terms']

    if collateral_nds is not None:
        assert collaterals_info[1]['terms']['nds_flag'] == ('Y' if collateral_nds else 'N')
        assert collaterals_info[1]['terms']['nds_percent'] == (20 if collateral_nds else None)
    else:
        assert 'nds_flag' not in collaterals_info[1]['terms']
        assert 'nds_percent' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('discard_nds_contract', [1])
@pytest.mark.parametrize('discard_nds_collateral', [0])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
@pytest.mark.parametrize('is_cancelled_contract, is_cancelled_collateral', [(None, None),
                                                                            (TWO_MONTH_LATER, None),
                                                                            (MONTH_LATER, TWO_MONTH_LATER)])
def test_collateral_nds_flag_general(session, w_signed_contract, w_signed_collateral, discard_nds_contract,
                                     discard_nds_collateral, is_cancelled_contract, is_cancelled_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            dt=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            is_cancelled=is_cancelled_contract,
                                            discard_nds=discard_nds_contract,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               discard_nds=discard_nds_collateral,
                               is_cancelled=is_cancelled_contract,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['GENERAL'][1001])
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']

    # старый эскпорт история
    ndses = contract.get_attribute_history('discard_nds', check_signed=contract.signed)
    ndses_orig = contract.get_attribute_history('discard_nds', check_signed=contract.signed)
    if len(ndses) != 2:
        if discard_nds_contract is None:
            ndses.insert(0, (None, None))
        if discard_nds_collateral is None or (w_signed_contract and not w_signed_collateral):
            ndses.append((None, None))

    # старый эскпорт term_code в ДС
    nds_0 = AttributeMapper(contract.collaterals[0], None).discard_nds[0]['term_value']
    nds_1 = AttributeMapper(contract.collaterals[1], None).discard_nds[0]['term_value']
    # старый эскпорт term_code в договоре
    ccurrent = contract.current_signed() if contract.signed else contract.current
    nds_header = {'term_value2': 'N/A', 'term_code': 'XXOKE_NDS', 'attr2': None, 'attr3': None, 'attr1': None,
                  'header_id': 123, 'term_value': ccurrent.discard_nds}

    json_info = simplejson.dumps(ContractWrapper(contract).get_info(), default=_json_default,
                                 ensure_ascii=False).encode('utf8')
    if discard_nds_contract is not None:
        assert collaterals_info[0]['terms']['nds_flag'] == ('N' if discard_nds_contract else 'Y')
        assert 'nds_percent' not in collaterals_info[0]['terms']
    else:
        assert 'nds_flag' not in collaterals_info[0]['terms']
        assert 'nds_percent' not in collaterals_info[0]['terms']

    if discard_nds_collateral is not None:
        assert collaterals_info[1]['terms']['nds_flag'] == ('N' if discard_nds_collateral else 'Y')
        assert 'nds_percent' not in collaterals_info[1]['terms']
    else:
        assert 'nds_flag' not in collaterals_info[1]['terms']
        assert 'nds_percent' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('payment_type_contract', [1, None])
@pytest.mark.parametrize('payment_type_collateral', [2, None])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
def test_collateral_acts_period(session, w_signed_contract, w_signed_collateral, payment_type_contract,
                                payment_type_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            dt=TWO_MONTH_BEFORE,
                                            payment_type=payment_type_contract,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None)
    contract.append_collateral(dt=MONTH_BEFORE,
                               payment_type=payment_type_collateral,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               memo='memo'
                               )
    session.flush()
    ccurrent = contract.current_signed() if contract.signed else contract.current
    # старый экспорт
    payment_type_prev = ccurrent.payment_type
    payment_type_trash = AttributeMapper(contract.collaterals[0], None).payment_type_trash[0]['term_value']
    payment_type_trash1 = AttributeMapper(contract.collaterals[1], None).payment_type_trash[0]['term_value']
    # старый экспорт

    contract_info = ContractWrapper(contract).get_info()
    assert contract_info['collaterals'][0]['terms']['acts_period'] == payment_type_contract
    acts_period_1 = int(payment_type_trash) if payment_type_trash != 'None' else None
    assert contract_info['collaterals'][0]['terms']['acts_period'] == acts_period_1

    assert contract_info['collaterals'][1]['terms']['acts_period'] == payment_type_collateral
    acts_period2 = int(payment_type_trash1) if payment_type_trash1 != 'None' else None
    assert contract_info['collaterals'][1]['terms']['acts_period'] == acts_period2


@pytest.mark.parametrize('end_date_contract', [MONTH_LATER, None])
@pytest.mark.parametrize('end_date_collateral', [TWO_MONTH_LATER, None])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
@pytest.mark.parametrize('is_cancelled_contract', [MONTH_LATER, None])
@pytest.mark.parametrize('is_cancelled_collateral', [MONTH_LATER, None])
def test_collateral_end_dt(session, w_signed_contract, w_signed_collateral, end_date_contract, end_date_collateral,
                           is_cancelled_contract, is_cancelled_collateral):
    """общий тест про дату окончания в шапке и ДС в зависимости от подписанности/удалености"""
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=NOW,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            end_dt=end_date_contract,
                                            is_cancelled=is_cancelled_contract,
                                            is_signed=NOW if w_signed_contract else None)
    contract.append_collateral(dt=NOW,
                               is_signed=NOW if w_signed_collateral else None,
                               end_dt=end_date_collateral,
                               is_cancelled=is_cancelled_collateral,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    contract_info = ContractWrapper(contract).get_info()
    ccurrent = contract.current_signed() if contract.signed else contract.current
    dt_list = []
    # блок про собирание в шапку
    dt_list.append(end_date_contract)
    if not is_cancelled_collateral:
        if w_signed_contract and w_signed_collateral:
            dt_list.append(end_date_collateral)
        elif not w_signed_contract:
            dt_list.append(end_date_collateral)

    actual_end_dt = dt_list and dt_list[-1] or None
    prev_end_dt = ccurrent.end_dt
    assert actual_end_dt == prev_end_dt

    # блок про значения в ДС
    prev_end_dt_0 = AttributeMapper(contract.collaterals[0], None).end_dt
    if end_date_contract:
        assert prev_end_dt_0[0]['term_value_date'] == end_date_contract
    else:
        assert prev_end_dt_0 is None
    prev_end_dt_1 = AttributeMapper(contract.collaterals[1], None).end_dt
    if end_date_collateral:
        assert prev_end_dt_1[0]['term_value_date'] == end_date_collateral
    else:
        assert prev_end_dt_1 is None

    assert contract_info['collaterals'][0]['terms']['end_date'] == end_date_contract
    assert contract_info['collaterals'][1]['terms']['end_date'] == end_date_collateral


@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
@pytest.mark.parametrize('w_parent', [True, False])
def test_collateral_end_dt_distribution(session, w_signed_contract, w_signed_collateral, w_parent):
    """дата окончания из дочерних допников не грузится с родительским договором"""

    parent_contract = ob.ContractBuilder.construct(session,
                                                   ctype='DISTRIBUTION',
                                                   contract_type=cst.DistributionContractType.OFFER,
                                                   dt=NOW,
                                                   firm=cst.FirmId.YANDEX_OOO,
                                                   end_dt=None,
                                                   is_signed=NOW if w_signed_contract else None)

    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=NOW,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            end_dt=NOW,
                                            is_signed=NOW if w_signed_contract else None,
                                            parent_contract_id=parent_contract.id if w_parent else None)
    contract.append_collateral(dt=NOW,
                               is_signed=NOW if w_signed_collateral else None,
                               end_dt=TWO_MONTH_LATER,
                               collateral_type=collateral_types['DISTRIBUTION'][3030],
                               memo='memo')
    session.flush()
    if w_parent:
        parent_contract_info = ContractWrapper(parent_contract).get_info()
        assert len(parent_contract_info['collaterals']) == 2
        assert 'end_date' not in parent_contract_info['collaterals'][1]['terms']
    else:
        contract_info = ContractWrapper(contract).get_info()
        assert len(contract_info['collaterals']) == 2
        assert contract_info['collaterals'][1]['terms']['end_date'] == TWO_MONTH_LATER


@pytest.mark.parametrize('tail_time_contract', [12, None])
@pytest.mark.parametrize('tail_time_collateral', [0, 3])
def test_collateral_end_dt_tail_time(session, tail_time_contract, tail_time_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            end_dt=NOW,
                                            tail_time=tail_time_contract,
                                            is_signed=None)
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE,
                               end_dt=TWO_MONTH_LATER,
                               tail_time=tail_time_collateral,
                               collateral_type=collateral_types['DISTRIBUTION'][3030])

    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE,
                               end_dt=TWO_MONTH_LATER,
                               collateral_type=collateral_types['DISTRIBUTION'][3030])
    session.flush()
    finish_dt = AttributeMapper(contract.collaterals[0], None).end_dt
    finish_dt_1 = AttributeMapper(contract.collaterals[1], None).end_dt
    finish_dt_2 = AttributeMapper(contract.collaterals[2], None).end_dt
    contract_info = ContractWrapper(contract).get_info()
    assert contract_info['collaterals'][0]['terms']['end_date'] == ut.add_months_to_date(NOW,
                                                                                         int(tail_time_contract or 0))
    assert contract_info['collaterals'][1]['terms']['end_date'] == ut.add_months_to_date(TWO_MONTH_LATER,
                                                                                         int(tail_time_collateral or 0))

    assert contract_info['collaterals'][2]['terms']['end_date'] == TWO_MONTH_LATER


@pytest.mark.parametrize('collateral_type', [1006, 90])
@pytest.mark.parametrize('finish_dt_contract', [
    MONTH_LATER,
    None])
@pytest.mark.parametrize('finish_dt_collateral', [TWO_MONTH_LATER, None])
def test_collateral_end_dt_general(session, finish_dt_contract, finish_dt_collateral, collateral_type):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            finish_dt=finish_dt_contract,
                                            is_signed=None)
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE,
                               finish_dt=finish_dt_collateral,
                               collateral_type=collateral_types['GENERAL'][collateral_type],
                               memo='memo')
    session.flush()
    finish_dt = AttributeMapper(contract.collaterals[0], None).finish_dt
    finish_dt_1 = AttributeMapper(contract.collaterals[1], None).finish_dt
    contract_info = ContractWrapper(contract).get_info()
    if finish_dt_contract:
        finish_dt_contract = finish_dt_contract - datetime.timedelta(days=1)
        assert contract_info['collaterals'][0]['terms']['end_date'] == finish_dt_contract
    else:
        assert 'end_date' not in contract_info['collaterals'][0]['terms']

    if finish_dt_collateral:
        finish_dt_collateral = finish_dt_collateral - datetime.timedelta(days=1)
        assert contract_info['collaterals'][1]['terms']['end_date'] == finish_dt_collateral
    else:
        assert 'end_date' not in contract_info['collaterals'][1]['terms']


@pytest.mark.parametrize('contract_agregator_pct', [D('45.4'), None])
@pytest.mark.parametrize('collateral_agregator_pct', [66.4, None])
@pytest.mark.parametrize('contract_type', [
    2,
    6
])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
def test_collateral_partner_percent(session, w_signed_contract,
                                    w_signed_collateral,
                                    contract_agregator_pct,
                                    collateral_agregator_pct,
                                    contract_type,
                                    ):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            doc_set=contract_type,
                                            contract_type=contract_type,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                            partner_pct=10,
                                            firm=1,
                                            agregator_pct=contract_agregator_pct
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               partner_pct=11,
                               agregator_pct=collateral_agregator_pct,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               memo='memo')
    session.flush()
    contract_info = ContractWrapper(contract).get_info()
    # старый экспорт
    if contract.ctype.type != 'GEOCONTEXT':
        commissions = contract.get_attribute_history('partner_pct')
        if contract.ctype.type == 'PARTNERS' and contract.current.contract_type == 2:
            commissions = contract.get_attribute_history('agregator_pct')
    orig_commissions = commissions[:]
    if len(commissions) != 2:
        if contract_agregator_pct is None:
            commissions.insert(0, (None, None))
        if collateral_agregator_pct is None:
            commissions.append((None, None))

    percent = AttributeMapper(contract.collaterals[0], None).percent
    percent1 = AttributeMapper(contract.collaterals[1], None).percent
    # старый экспорт
    json_info = simplejson.dumps(ContractWrapper(contract).get_info(), default=_json_default,
                                 ensure_ascii=False).encode('utf8')
    if contract_type == 2:
        if contract_agregator_pct:
            assert contract_info['collaterals'][0]['terms']['partner_percent'] == contract_agregator_pct
            assert contract_info['collaterals'][0]['terms']['partner_percent'] == commissions[0][1]
            assert contract_info['collaterals'][0]['terms']['partner_percent'] == percent[0]['term_value_number']
        else:
            assert 'partner_percent' not in contract_info['collaterals'][0]['terms']
            assert percent[0]['term_value_number'] is None

        if collateral_agregator_pct:
            assert contract_info['collaterals'][1]['terms']['partner_percent'] == collateral_agregator_pct
            assert contract_info['collaterals'][1]['terms']['partner_percent'] == commissions[1][1]
            assert contract_info['collaterals'][1]['terms']['partner_percent'] == percent1[0]['term_value_number']
        else:
            assert percent1[0]['term_value_number'] is None
            assert 'partner_percent' not in contract_info['collaterals'][1]['terms']

    else:
        assert contract_info['collaterals'][0]['terms']['partner_percent'] == 10
        assert contract_info['collaterals'][0]['terms']['partner_percent'] == commissions[0][1]
        assert contract_info['collaterals'][0]['terms']['partner_percent'] == percent[0]['term_value_number']

        assert contract_info['collaterals'][1]['terms']['partner_percent'] == 11
        assert contract_info['collaterals'][1]['terms']['partner_percent'] == commissions[1][1]
        assert contract_info['collaterals'][1]['terms']['partner_percent'] == percent1[0]['term_value_number']


@pytest.mark.parametrize('memo_contract', [
    u'Договор создан автоматически',
    None,
    ''
])
@pytest.mark.parametrize('memo_collateral', [u'ДС создано автоматически', None])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
def test_collateral_memo(session, w_signed_contract, w_signed_collateral, memo_contract, memo_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            memo=memo_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               nds=18,
                               memo=memo_collateral
                               )
    session.flush()
    memo_0 = AttributeMapper(contract.collaterals[0], None).memo[0]
    # if memo_contract:
    #     assert memo_0['term_value_string'] == memo_contract
    # else:
    #     assert 'term_value_string' not in memo_0
    memo_1 = AttributeMapper(contract.collaterals[1], None).memo[0]
    # if memo_collateral:
    #     assert memo_1['term_value_string'] == memo_collateral
    # else:
    #     assert 'term_value_string' not in memo_1
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['memo'] == memo_contract
    assert collaterals_info[1]['terms']['memo'] == memo_collateral


def create_place(session, client, place_id=None):
    if not place_id:
        place_id = session.execute('select bo.s_place_id.nextval from dual').scalar()
    place = mapper.Place()
    place.id = place_id
    place.client = client
    place.type = 2
    place.internal_type = 0
    place.url = 'url_{}'.format(ob.generate_character_string(3))
    session.add(place)
    return place.url


@pytest.mark.parametrize('mkb_price_contract', [
    {396275: 3, 2: 345, 23: None},
    None
])
@pytest.mark.parametrize('mkb_price_collateral', [
    {11: 44, 28: None},
    {396275: 3, 2: 345, 23: None},
    None
])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [
    (False, False),
    (True, False),
    (True, True)
])
def test_collateral_mkb_price(session, w_signed_contract, w_signed_collateral, mkb_price_contract,
                              mkb_price_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            mkb_price=mkb_price_contract
                                            )
    places_info = {}
    if mkb_price_contract:
        for place_id in mkb_price_contract:
            place_url = create_place(session, contract.client, place_id)
            places_info[place_id] = place_url

    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2030],
                               mkb_price=mkb_price_collateral,
                               memo='memo')

    contract.collaterals[1].mkb_price = mkb_price_collateral
    if mkb_price_collateral and mkb_price_collateral != mkb_price_contract:
        for place_id in mkb_price_collateral:
            place_url = create_place(session, contract.client, place_id)
            places_info[place_id] = place_url

    session.flush()
    mkb_price = AttributeMapper(contract.collaterals[0], None).mkb_price
    mkb_price1 = AttributeMapper(contract.collaterals[1], None).mkb_price
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if mkb_price_contract:
        assert collaterals_info[0]['terms']['mkb_price'] == [{'price': 345, 'url': places_info[2]},
                                                             {'price': 3, 'url': places_info[396275]}]
    else:
        assert 'mkb_price' not in collaterals_info[0]['terms']

    if mkb_price_collateral and mkb_price_collateral != mkb_price_contract:
        assert collaterals_info[1]['terms']['mkb_price'] == [{'price': v, 'url': places_info[k]}

                                                             for k, v in mkb_price_collateral.iteritems() if
                                                             v is not None]
    else:
        assert 'mkb_price' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('domains_contract', [u'chinapro.ru', None])
@pytest.mark.parametrize('domains_collateral', [u'medoviy.ru', None])
def test_collateral_domains(session, domains_contract, domains_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            domains=domains_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               domains=domains_collateral,
                               memo='memo'
                               )
    session.flush()
    domains = AttributeMapper(contract.collaterals[0], None).domains
    domains1 = AttributeMapper(contract.collaterals[1], None).domains
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if domains_contract:
        assert collaterals_info[0]['terms']['domains'] == domains_contract
    else:
        assert 'domains' not in collaterals_info[0]['terms']

    if domains_collateral:
        assert collaterals_info[1]['terms']['domains'] == domains_collateral
    else:
        assert 'domains' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('search_forms_contract', [1, 0])
@pytest.mark.parametrize('search_forms_collateral', [1, 0])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
def test_collateral_search_forms(session, w_signed_contract, w_signed_collateral, search_forms_contract,
                                 search_forms_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            search_forms=search_forms_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               search_forms=search_forms_collateral)
    session.flush()
    search_forms = AttributeMapper(contract.collaterals[0], None).search_forms
    search_forms1 = AttributeMapper(contract.collaterals[1], None).search_forms
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if search_forms_contract is not None:
        search_forms_contract = 'Y' if search_forms_contract else 'N'
        assert collaterals_info[0]['terms']['search_forms'] == search_forms_contract
    else:
        assert 'search_forms' not in collaterals_info[0]['terms']

    if search_forms_collateral is not None:
        search_forms_collateral = 'Y' if search_forms_collateral else 'N'
        assert collaterals_info[1]['terms']['search_forms'] == search_forms_collateral
    else:
        assert 'search_forms' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('open_date_contract', [1, 0, None])
@pytest.mark.parametrize('open_date_collateral', [1, 0, None])
@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [(False, False),
                                                                    (True, False),
                                                                    (True, True)])
def test_collateral_open_date(session, w_signed_contract, w_signed_collateral, open_date_contract,
                              open_date_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            open_date=open_date_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               open_date=open_date_collateral)
    session.flush()
    open_date = AttributeMapper(contract.collaterals[0], None).open_date
    open_date1 = AttributeMapper(contract.collaterals[1], None).open_date
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if open_date_contract:
        assert collaterals_info[0]['terms']['open_date'] == 'Y'
    else:
        assert collaterals_info[0]['terms']['open_date'] == 'N'

    if open_date_collateral:
        assert collaterals_info[1]['terms']['open_date'] == 'Y'
    else:
        assert collaterals_info[1]['terms']['open_date'] == 'N'


def test_collateral_end_reason(session):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            end_reason=1
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               end_reason=3)
    session.flush()
    end_reason = AttributeMapper(contract.collaterals[0], None).end_reason
    end_reason1 = AttributeMapper(contract.collaterals[1], None).end_reason
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['end_reason'] == 1
    assert collaterals_info[1]['terms']['end_reason'] == 3


def test_collateral_pay_to(session):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            pay_to=1
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               pay_to=3)
    session.flush()
    pay_to = AttributeMapper(contract.collaterals[0], None).pay_to
    pay_to1 = AttributeMapper(contract.collaterals[1], None).pay_to
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['pay_to'] == 1
    assert collaterals_info[1]['terms']['pay_to'] == 3


def test_collateral_unilateral(session):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            unilateral_acts=1
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               unilateral_acts=0)
    session.flush()
    unilateral_acts = AttributeMapper(contract.collaterals[0], None).unilateral_acts
    unilateral_acts_1 = AttributeMapper(contract.collaterals[1], None).unilateral_acts
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['unilateral'] == 'Y'
    assert collaterals_info[1]['terms']['unilateral'] == 'N'


@pytest.mark.parametrize('unilateral_contract', [1, 0, None])
def test_collateral_unilateral_general(session, unilateral_contract):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            unilateral=unilateral_contract
                                            )
    session.flush()
    unilateral = AttributeMapper(contract.collaterals[0], None).unilateral
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if unilateral_contract == 1:
        assert collaterals_info[0]['terms']['unilateral'] == 'Y'
    else:
        assert collaterals_info[0]['terms']['unilateral'] == 'N'


def test_collateral_individual_docs(session):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            individual_docs=1
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['PARTNERS'][2010],
                               individual_docs=0)
    session.flush()
    individual_docs = AttributeMapper(contract.collaterals[0], None).individual_docs
    individual_docs_1 = AttributeMapper(contract.collaterals[1], None).individual_docs
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['individual_docs'] == 'Y'
    assert collaterals_info[1]['terms']['individual_docs'] == 'N'


def test_collateral_selfemployed(session):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            selfemployed=1
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['PARTNERS'][2160],
                               selfemployed=0)
    session.flush()
    # В старый экспорт атрибут не добавлялся
    # _ = AttributeMapper(contract.collaterals[0], None).selfemployed
    # _ = AttributeMapper(contract.collaterals[1], None).selfemployed
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['selfemployed'] == 'Y'
    assert collaterals_info[1]['terms']['selfemployed'] == 'N'


def test_atypical_conditions(session):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='PARTNERS',
                                            contract_type=cst.PartnersContractType.RSYA_2014,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            atypical_conditions=1
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['PARTNERS'][2160],
                               atypical_conditions=0)

    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['PARTNERS'][2160],
                               selfemployed=0)
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['atypical_conditions'] == 'Y'
    assert collaterals_info[1]['terms']['atypical_conditions'] == 'N'
    assert 'atypical_conditions' not in collaterals_info[2]['terms']


@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [
    (False, False),
    (True, False),
    (True, True)
])
@pytest.mark.parametrize('linked_contract_services', [
    {cst.ServiceId.DIRECT},
    {cst.ServiceId.MARKET},
    {cst.ServiceId.DIRECT, cst.ServiceId.GEOCON},
    {cst.ServiceId.MEDIA_SELLING},
    {cst.ServiceId.MKB},
    {cst.ServiceId.GEOCON},
    {cst.ServiceId.TAXI_PAYMENT},
    {cst.ServiceId.MEDIA_BANNERS}
])
def test_linked_contracts_spendable(session, linked_contract_services, w_signed_contract, w_signed_collateral):
    # TODO Закси не проверяем в расходных, потому что не встречается (закси надо брать из curentt)
    linked_contract = ob.ContractBuilder.construct(session,
                                                   ctype='GENERAL',
                                                   contract_type=87,
                                                   services=linked_contract_services,
                                                   dt=TWO_MONTH_BEFORE if w_signed_contract else None,
                                                   is_signed=TWO_MONTH_BEFORE,
                                                   firm=1,
                                                   )

    linked_contract.append_collateral(dt=MONTH_BEFORE,
                                      is_signed=MONTH_BEFORE if w_signed_collateral else None,
                                      collateral_type=collateral_types['GENERAL'][1001],
                                      services={cst.ServiceId.GEOCON},
                                      )

    contract = ob.ContractBuilder.construct(session,
                                            ctype='SPENDABLE',
                                            contract_type=87,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=1,
                                            link_contract_id=linked_contract.id
                                            )
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    prev_link_contract_1 = AttributeMapper(contract.collaterals[0], None).link_contract_id
    prev_groups_1 = {int(row['term_value']) for row in prev_link_contract_1}
    if linked_contract_services == {cst.ServiceId.MARKET} and not (w_signed_contract and w_signed_collateral):
        assert 'linked_contracts' not in collaterals_info[0]['terms']
    else:
        groups_1 = set()
        for row in collaterals_info[0]['terms']['linked_contracts']:
            assert row['contract_id'] == linked_contract.id
            groups_1.add(row['group_id'])

        assert prev_groups_1 == groups_1


@pytest.mark.parametrize('linked_contracts', [None, set()])
def test_linked_contracts_general_empty(session, linked_contracts):
    linked_contract = ob.ContractBuilder.construct(session,
                                                   ctype='GENERAL',
                                                   services={7},
                                                   dt=TWO_MONTH_BEFORE,
                                                   is_signed=TWO_MONTH_BEFORE,
                                                   firm=1,
                                                   )

    linked_contract.append_collateral(dt=MONTH_BEFORE,
                                      is_signed=MONTH_BEFORE,
                                      collateral_type=collateral_types['GENERAL'][1001],
                                      services={cst.ServiceId.GEOCON},
                                      )

    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=1,
                                            link_contract_id=None,
                                            linked_contracts=linked_contracts
                                            )
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert 'linked_contracts' not in collaterals_info[0]['terms']


@pytest.mark.parametrize('w_signed_contract, w_signed_collateral', [
    (False, False),
    (True, False),
    (True, True)
])
@pytest.mark.parametrize('linked_contract_services', [
    {cst.ServiceId.DIRECT},
    {cst.ServiceId.MARKET},
    {cst.ServiceId.DIRECT, cst.ServiceId.GEOCON}])
def test_linked_contracts_general(session, linked_contract_services, w_signed_contract, w_signed_collateral):
    # TODO commission = 60 только в GENERAL
    linked_contract = ob.ContractBuilder.construct(session,
                                                   ctype='GENERAL',
                                                   services=linked_contract_services,
                                                   dt=TWO_MONTH_BEFORE,
                                                   is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                                   firm=cst.FirmId.YANDEX_OOO,
                                                   )

    linked_contract.append_collateral(dt=MONTH_BEFORE,
                                      is_signed=MONTH_BEFORE if w_signed_collateral else None,
                                      collateral_type=collateral_types['GENERAL'][1001],
                                      services={cst.ServiceId.GEOCON},
                                      )

    linked_contract_1 = ob.ContractBuilder.construct(session,
                                                     ctype='GENERAL',
                                                     services=linked_contract_services,
                                                     dt=TWO_MONTH_BEFORE,
                                                     is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                                     firm=cst.FirmId.YANDEX_OOO,
                                                     )

    linked_contract_2 = ob.ContractBuilder.construct(session,
                                                     ctype='GENERAL',
                                                     services=linked_contract_services,
                                                     dt=TWO_MONTH_BEFORE,
                                                     is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                                     firm=cst.FirmId.YANDEX_OOO,
                                                     )

    linked_contract_3 = ob.ContractBuilder.construct(session,
                                                     ctype='GENERAL',
                                                     services=linked_contract_services,
                                                     dt=TWO_MONTH_BEFORE,
                                                     is_signed=TWO_MONTH_BEFORE if w_signed_contract else None,
                                                     firm=cst.FirmId.YANDEX_OOO,
                                                     )

    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            link_contract_id=linked_contract.id,
                                            linked_contracts={linked_contract_1.id,
                                                              linked_contract_2.id}
                                            )

    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=MONTH_BEFORE if w_signed_collateral else None,
                               collateral_type=collateral_types['GENERAL'][1021],
                               services={cst.ServiceId.GEOCON},
                               linked_contracts={linked_contract_3.id,
                                                 linked_contract_2.id}
                               )
    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    prev_link_contract_1 = AttributeMapper(contract.collaterals[0], None).link_contract_id
    prev_linked_contracts = AttributeMapper(contract.collaterals[0], None).linked_contracts
    prev_linked_contracts1 = AttributeMapper(contract.collaterals[1], None).linked_contracts
    prev_groups_1 = {int(row['term_value']) for row in prev_link_contract_1}
    if linked_contract_services == {cst.ServiceId.DIRECT}:
        assert sorted(collaterals_info[0]['terms']['linked_contracts']) == sorted(
            [{'group_id': 7 if (w_signed_contract and not w_signed_collateral) else 12,
              'contract_id': linked_contract.id},
             {'group_id': 7, 'contract_id': linked_contract_1.id},
             {'group_id': 7, 'contract_id': linked_contract_2.id}])

    elif linked_contract_services == {cst.ServiceId.MARKET}:
        if w_signed_contract and not w_signed_collateral:
            assert 'linked_contracts' not in collaterals_info[0]['terms']
        else:
            assert sorted(collaterals_info[0]['terms']['linked_contracts']) == sorted(
                [{'group_id': 7 if (w_signed_contract and not w_signed_collateral) else 12,
                  'contract_id': linked_contract.id}])
    else:
        if w_signed_contract and not w_signed_collateral:
            assert sorted(collaterals_info[0]['terms']['linked_contracts']) == sorted(
                [{'group_id': 7, 'contract_id': linked_contract_1.id},
                 {'group_id': 12, 'contract_id': linked_contract_1.id},
                 {'group_id': 12, 'contract_id': linked_contract.id},
                 {'group_id': 7, 'contract_id': linked_contract.id},
                 {'group_id': 7, 'contract_id': linked_contract_2.id},
                 {'group_id': 12, 'contract_id': linked_contract_2.id},
                 ])
        else:

            assert sorted(collaterals_info[0]['terms']['linked_contracts']) == sorted(
                [{'group_id': 12, 'contract_id': linked_contract.id},
                 {'group_id': 7, 'contract_id': linked_contract_1.id},
                 {'group_id': 7, 'contract_id': linked_contract_2.id},
                 {'group_id': 12, 'contract_id': linked_contract_1.id},
                 {'group_id': 12, 'contract_id': linked_contract_2.id}
                 ])


@pytest.mark.parametrize('region_contract', [7700000000000, None])
@pytest.mark.parametrize('region_collateral', [6600000100000, None])
def test_collateral_region(session, region_contract, region_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='SPENDABLE',
                                            contract_type=87,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=1,
                                            region=region_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['SPENDABLE'][7030],
                               region=region_collateral)
    session.flush()
    region = AttributeMapper(contract.collaterals[0], None).region
    region_1 = AttributeMapper(contract.collaterals[1], None).region
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['region'] == region_contract
    assert collaterals_info[1]['terms']['region'] == region_collateral


@pytest.mark.parametrize('country_contract, expected_country_contract', [(225, 643),
                                                                         (None, None)])
@pytest.mark.parametrize('country_collateral, expected_country_collateral', [(187, 804),
                                                                             (None, None)])
def test_collateral_country(session, country_contract, country_collateral, expected_country_contract,
                            expected_country_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='SPENDABLE',
                                            contract_type=87,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=1,
                                            country=country_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['SPENDABLE'][7030],
                               country=country_collateral,
                               memo='memo')
    session.flush()
    country = AttributeMapper(contract.collaterals[0], None).country
    country_1 = AttributeMapper(contract.collaterals[1], None).country
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if country_contract:
        assert collaterals_info[0]['terms']['country'] == expected_country_contract
    else:
        assert 'country' not in collaterals_info[0]['terms']
    if country_collateral:
        assert collaterals_info[1]['terms']['country'] == expected_country_collateral
    else:
        assert 'country' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('distribution_places_contract', [{1, 2},
                                                          None
                                                          ])
@pytest.mark.parametrize('distribution_places_collateral', [
    {2, 4},
    {1, 2},
    None
])
def test_collateral_distribution_places_collateral(session, distribution_places_contract,
                                                   distribution_places_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            distribution_places=distribution_places_contract,
                                            product_search='altlinux + FF'
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['DISTRIBUTION'][3030],
                               distribution_places=distribution_places_collateral,
                               product_search=None,
                               memo='memo'
                               )
    session.flush()
    distr_types = AttributeMapper(contract.collaterals[0], None).distr_types
    distr_types_1 = AttributeMapper(contract.collaterals[1], None).distr_types
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if distribution_places_contract:
        assert collaterals_info[0]['terms']['distribution_types'] == [{'description': 'altlinux + FF', 'type': 1},
                                                                      {'description': '', 'type': 2}]
    else:
        assert 'distribution_types' not in collaterals_info[0]['terms']
    if distribution_places_collateral and distribution_places_collateral != distribution_places_contract:
        if not distribution_places_contract:
            assert collaterals_info[1]['terms']['distribution_types'] == [{'description': '', 'type': type}
                                                                          for type in distribution_places_collateral]
        else:
            assert collaterals_info[1]['terms']['distribution_types'] == [{'description': '', 'type': 1},
                                                                          {'description': '', 'type': 2},
                                                                          {'description': '', 'type': 4}]
    else:
        assert 'distribution_types' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('region_contract', [7700000000000, None])
@pytest.mark.parametrize('region_collateral', [6600000100000, None])
def test_collateral_region(session, region_contract, region_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='SPENDABLE',
                                            contract_type=87,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=1,
                                            region=region_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['SPENDABLE'][7030],
                               region=region_collateral,
                               memo='memo')
    session.flush()
    region = AttributeMapper(contract.collaterals[0], None).region
    region_1 = AttributeMapper(contract.collaterals[1], None).region
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['region'] == region_contract
    assert collaterals_info[1]['terms']['region'] == region_collateral


@pytest.mark.parametrize('install_price_contract', [10, None])
@pytest.mark.parametrize('install_price_collateral', [0, 3])
def test_collateral_install_price(session, install_price_contract, install_price_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            install_price=install_price_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['DISTRIBUTION'][3030],
                               install_price=install_price_collateral,
                               memo='memo')
    session.flush()
    install_price = AttributeMapper(contract.collaterals[0], None).install_price
    install_price_1 = AttributeMapper(contract.collaterals[1], None).install_price
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if install_price_contract is not None:
        assert collaterals_info[0]['terms']['install_price'] == install_price_contract
    else:
        assert 'install_price' not in collaterals_info[0]['terms']
    if install_price_collateral:
        assert collaterals_info[1]['terms']['install_price'] == install_price_collateral
    else:
        assert 'install_price' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('tail_time_contract', [10, None])
@pytest.mark.parametrize('tail_time_collateral', [0, 3])
def test_collateral_tail_time(session, tail_time_contract, tail_time_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            tail_time=tail_time_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['DISTRIBUTION'][3030],
                               tail_time=tail_time_collateral,
                               memo='memo')
    session.flush()
    tail_time = AttributeMapper(contract.collaterals[0], None).tail_time
    tail_time_1 = AttributeMapper(contract.collaterals[1], None).tail_time
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if tail_time_contract is not None:
        assert collaterals_info[0]['terms']['tail_time'] == tail_time_contract
    else:
        assert 'tail_time' not in collaterals_info[0]['terms']
    if tail_time_collateral:
        assert collaterals_info[1]['terms']['tail_time'] == tail_time_collateral
    else:
        assert 'tail_time' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('install_soft_contract', [10, None])
@pytest.mark.parametrize('install_soft_collateral', [0, 3])
def test_collateral_install_soft(session, install_soft_contract, install_soft_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            install_soft=install_soft_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['DISTRIBUTION'][3030],
                               install_soft=install_soft_collateral,
                               memo='memo')
    session.flush()
    install_soft = AttributeMapper(contract.collaterals[0], None).install_soft
    install_soft_1 = AttributeMapper(contract.collaterals[1], None).install_soft
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if install_soft_contract is not None:
        assert collaterals_info[0]['terms']['install_soft'] == install_soft_contract
    else:
        assert 'install_soft' not in collaterals_info[0]['terms']
    if install_soft_collateral:
        assert collaterals_info[1]['terms']['install_soft'] == install_soft_collateral
    else:
        assert 'install_soft' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('supplements_contract', [{3, 6}, None])
@pytest.mark.parametrize('supplements_collateral', [{3, 6}, {3, 5}, {}])
def test_collateral_supplements(session, supplements_contract, supplements_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            supplements=supplements_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['DISTRIBUTION'][3030],
                               supplements=supplements_collateral,
                               memo='memo')
    supplements = AttributeMapper(contract.collaterals[0], None).supplements
    supplements_1 = AttributeMapper(contract.collaterals[1], None).supplements
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if supplements_contract is not None:
        assert collaterals_info[0]['terms']['supplements'] == supplements_contract
    else:
        assert 'supplements' not in collaterals_info[0]['terms']
    if supplements_collateral and supplements_collateral != supplements_contract:
        if supplements_contract:
            supplements_collateral = supplements_collateral.union(supplements_contract)
        assert collaterals_info[1]['terms']['supplements'] == supplements_collateral
    else:
        assert 'supplements' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('download_domains_contract', ['softmen.ru', None])
@pytest.mark.parametrize('download_domains_collateral', ['', 'www.ru'])
def test_collateral_download_domains(session, download_domains_contract, download_domains_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            download_domains=download_domains_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['DISTRIBUTION'][3030],
                               download_domains=download_domains_collateral,
                               memo='memo')
    session.flush()
    download_domains = AttributeMapper(contract.collaterals[0], None).download_domains
    download_domains_1 = AttributeMapper(contract.collaterals[1], None).download_domains
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if download_domains_contract is not None:
        assert collaterals_info[0]['terms']['download_domains'] == download_domains_contract
    else:
        assert 'download_domains' not in collaterals_info[0]['terms']
    if download_domains_collateral:
        assert collaterals_info[1]['terms']['download_domains'] == download_domains_collateral
    else:
        assert 'download_domains' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('service_start_dt_contract', [NOW, None])
@pytest.mark.parametrize('service_start_dt_collateral', [TWO_MONTH_LATER, None])
def test_collateral_service_start_dt(session, service_start_dt_contract, service_start_dt_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            service_start_dt=service_start_dt_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['DISTRIBUTION'][3030],
                               service_start_dt=service_start_dt_collateral,
                               memo='memo')
    session.flush()
    service_start_dt = AttributeMapper(contract.collaterals[0], None).service_start_dt
    service_start_dt_1 = AttributeMapper(contract.collaterals[1], None).service_start_dt
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if service_start_dt_contract is not None:
        assert collaterals_info[0]['terms']['service_start_dt'] == service_start_dt_contract
    else:
        assert 'service_start_dt' not in collaterals_info[0]['terms']
    if service_start_dt_collateral:
        assert collaterals_info[1]['terms']['service_start_dt'] == service_start_dt_collateral
    else:
        assert 'service_start_dt' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('search_price_contract', [4.5, 0])
@pytest.mark.parametrize('search_price_collateral', [4, None])
def test_collateral_search_price(session, search_price_contract, search_price_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='DISTRIBUTION',
                                            contract_type=cst.DistributionContractType.OFFER,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            search_price=search_price_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['DISTRIBUTION'][3030],
                               search_price=search_price_collateral,
                               memo='memo')
    session.flush()
    search_price = AttributeMapper(contract.collaterals[0], None).search_price
    search_price_1 = AttributeMapper(contract.collaterals[1], None).search_price
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if search_price_contract:
        assert collaterals_info[0]['terms']['search_price'] == search_price_contract
    else:
        assert 'search_price' not in collaterals_info[0]['terms']
    if search_price_collateral:
        assert collaterals_info[1]['terms']['search_price'] == search_price_collateral
    else:
        assert 'search_price' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('services_contract', [{2, 4}, set(), ])
@pytest.mark.parametrize('services_collateral', [
    {2, 4},
    {2, 3},
    None])
def test_collateral_services(session, services_contract, services_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            services=services_contract
                                            )
    contract.collaterals[0].firm_id = 1
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1001],
                               services=services_collateral,
                               memo='memo')
    session.flush()
    services = AttributeMapper(contract.collaterals[0], None).services
    services_1 = AttributeMapper(contract.collaterals[1], None).services
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if services_contract:
        assert collaterals_info[0]['terms']['service'] == services_contract
    else:
        assert 'service' not in collaterals_info[0]['terms']
    if services_collateral and services_contract != services_collateral:
        assert collaterals_info[1]['terms']['service'] == services_collateral
    else:
        assert 'service' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('payment_type_contract', [None, 2, 3])
@pytest.mark.parametrize('payment_type_collateral', [None, 3])
def test_collateral_credit_depend_on_payment_type(session, payment_type_contract, payment_type_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            payment_type=payment_type_contract,
                                            credit_type=2,
                                            services={7}
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1001],
                               payment_type=payment_type_collateral,
                               credit_type=1,
                               memo='f',
                               services={7})
    session.flush()
    # credit_col = AttributeMapper(contract.collaterals[0], None).credit_col
    # credit_col_1 = AttributeMapper(contract.collaterals[1], None).credit_col
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if payment_type_contract != 3:
        assert 'credit' not in collaterals_info[0]['terms']
    else:
        assert collaterals_info[0]['terms']['credit'] == {'limit': 0,
                                                          'type': 2}
    if payment_type_collateral != 3:
        assert 'credit' not in collaterals_info[1]['terms']
    else:
        assert collaterals_info[1]['terms']['credit'] == {'type': 1}


@pytest.mark.parametrize('credit_limit_single_contract', [
    None,
    45
])
@pytest.mark.parametrize('w_credit_limit, credit_limit_single_collateral', [
    (True, 0),
    # (False, -1),
    (True, 2322)
])
@pytest.mark.parametrize('credit_limit_contract', [
    None,

    {26: 43}
])
@pytest.mark.parametrize('credit_limit_collateral', [
    {26: 43},
    {},
    {17: 0},
    {23: None},
    {23: 655, 43: 199}
])
def test_collateral_credit_depend_on_limit(session, credit_limit_single_contract, credit_limit_single_collateral,
                                           credit_limit_contract, credit_limit_collateral, w_credit_limit):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            payment_type=3,
                                            credit_type=2,
                                            credit_limit=credit_limit_contract,
                                            credit_limit_single=credit_limit_single_contract,
                                            services={7}
                                            )
    extra_params = {}
    if w_credit_limit:
        extra_params['credit_limit_single'] = credit_limit_single_collateral
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1021],
                               payment_type=3,
                               credit_type=2,
                               credit_limit=credit_limit_collateral,
                               services={7},
                               **extra_params)
    session.flush()
    # credit_col = AttributeMapper(contract.collaterals[0], None).credit_col
    # credit_col_1 = AttributeMapper(contract.collaterals[1], None).credit_col
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    limit_1 = credit_limit_single_contract or 0
    if credit_limit_contract and not credit_limit_single_contract:
        limit_1 = sum(v for v in credit_limit_contract.values() if v)

    assert collaterals_info[0]['terms']['credit'] == {'limit': limit_1, 'type': 2}
    # assert collaterals_info[0]['terms']['credit']['limit'] == credit_col[1]['term_value_number']
    limit_2 = credit_limit_single_collateral or 0

    if not w_credit_limit:
        limit_2 = sum(v for v in credit_limit_collateral.values() if v)

    assert collaterals_info[1]['terms']['credit'] == {'limit': limit_2, 'type': 2}

    # prev_limit = credit_col_1[1]['term_value_number']
    if w_credit_limit and credit_limit_single_collateral is not None:
        prev_limit = credit_limit_single_collateral
    if not w_credit_limit:
        prev_limit = sum(v for v in credit_limit_collateral.values() if v)
    # assert collaterals_info[1]['terms']['credit']['limit'] == prev_limit


@pytest.mark.parametrize('commission_type_contract', [23, None])
@pytest.mark.parametrize('commission_type_collateral', [None, 1])
@pytest.mark.parametrize('commission_declared_sum_contract', [22666666, 0])
@pytest.mark.parametrize('commission_declared_sum_collateral', [None, 1])
def test_collateral_commission(session, commission_type_contract, commission_type_collateral,
                               commission_declared_sum_contract, commission_declared_sum_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            firm=cst.FirmId.YANDEX_OOO,
                                            services={7},
                                            currency=810,
                                            commission_type=commission_type_contract,
                                            commission_declared_sum=commission_declared_sum_contract
                                            )
    contract.collaterals[0].firm_id = 1
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1000],
                               services={7},
                               currency=810,
                               commission_type=commission_type_collateral,
                               commission_declared_sum=commission_declared_sum_collateral
                               )
    session.flush()
    commission_type = AttributeMapper(contract.collaterals[0], None).commission_type
    commission_declared_sum = AttributeMapper(contract.collaterals[0], None).commission_declared_sum
    commission_type_1 = AttributeMapper(contract.collaterals[1], None).commission_type
    commission_declared_sum_1 = AttributeMapper(contract.collaterals[1], None).commission_declared_sum
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if commission_type_contract:
        if commission_type_contract == 23:
            if commission_declared_sum_contract is not None:
                assert collaterals_info[0]['terms']['commission'] == {
                    'percent': 25 if commission_declared_sum_contract else 0,
                    'turnover': commission_declared_sum_contract,
                    'type': 23}
            else:
                assert collaterals_info[0]['terms']['commission'] == {'type': 23}
        else:
            assert collaterals_info[0]['terms']['commission'] == {'percent': 15,
                                                                  'type': 3}
    else:
        # if commission_declared_sum_contract is not None:
        #     assert collaterals_info[0]['terms']['commission'] == {'turnover': commission_declared_sum_contract}
        # else:
        assert 'commission' not in collaterals_info[0]['terms']

    if commission_type_collateral:
        if commission_declared_sum_collateral is not None:
            assert collaterals_info[1]['terms']['commission'] == {'percent': 10,
                                                                  'turnover': commission_declared_sum_collateral,
                                                                  'type': 1}

        else:
            assert collaterals_info[1]['terms']['commission'] == {'percent': 10,
                                                                  'turnover': None,
                                                                  'type': commission_type_collateral}
    else:
        # if commission_declared_sum_collateral is not None:
        #     assert collaterals_info[1]['terms']['commission'] == {'turnover': commission_declared_sum_collateral}
        # else:
        assert 'commission' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('payment_type_contract', [2, None])
@pytest.mark.parametrize('payment_type_collateral', [3, None])
def test_collateral_payment_type(session, payment_type_contract, payment_type_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            payment_type=payment_type_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1019],
                               payment_type=payment_type_collateral,
                               memo='memo')
    session.flush()
    payment_type = AttributeMapper(contract.collaterals[0], None).payment_type
    payment_type_1 = AttributeMapper(contract.collaterals[1], None).payment_type
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if payment_type_contract:
        assert collaterals_info[0]['terms']['payment_type'] == payment_type_contract
    else:
        assert 'payment_type' not in collaterals_info[0]['terms']
    if payment_type_collateral:
        assert collaterals_info[1]['terms']['payment_type'] == payment_type_collateral
    else:
        assert 'payment_type' not in collaterals_info[1]['terms']


@pytest.mark.parametrize('discount_policy_type_contract',
                         [
                             3, 4,
                             None
                         ])
@pytest.mark.parametrize('budget_discount_pct_contract', [8,
                                                          0, None
                                                          ])
@pytest.mark.parametrize('fixed_discount_pct_contract', [12,
                                                         None
                                                         ])
@pytest.mark.parametrize('discount_pct_contract', [
    0, 5,
    None
])
def test_collateral_discount(session, discount_policy_type_contract, budget_discount_pct_contract,
                             fixed_discount_pct_contract, discount_pct_contract):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            discount_policy_type=discount_policy_type_contract,
                                            budget_discount_pct=budget_discount_pct_contract,
                                            fixed_discount_pct=fixed_discount_pct_contract,
                                            discount_pct=discount_pct_contract
                                            )
    # contract.append_collateral(dt=MONTH_BEFORE,
    #                            is_signed=None,
    #                            collateral_type=collateral_types['GENERAL'][1019],
    #                            supercommission=supercommission_collateral,
    #                            memo='memo')
    session.flush()
    discount_policy = AttributeMapper(contract.collaterals[0], None).discount_policy
    fixed_discount_percent_prev = [r['term_value_number'] for r in discount_policy if
                                   r['term_code'] == 'XXOKE_BASE_DISCOUNT' and r['term_value'] != '-1']
    budget_discount_percent_prev = [r['term_value_number'] for r in discount_policy if
                                    r['term_code'] == 'XXOKE_EXTRA_DISCOUNT' and r['term_value'] != '-1']

    type_prev = [r['term_value'] for r in discount_policy if
                 r['term_code'] == 'XXOKE_DISCOUNT_POLICY']

    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    discount_info = collaterals_info[0]['terms'].get('discount')
    if discount_policy_type_contract in (2, 3, 11):
        if budget_discount_pct_contract is not None:
            if fixed_discount_pct_contract:
                assert discount_info == {'budget_discount_percent': budget_discount_pct_contract,
                                         'fixed_discount_percent': fixed_discount_pct_contract,
                                         'type': discount_policy_type_contract}
            else:
                assert discount_info == {'budget_discount_percent': budget_discount_pct_contract,
                                         'type': discount_policy_type_contract}
        else:
            if fixed_discount_pct_contract:
                assert discount_info == {'fixed_discount_percent': fixed_discount_pct_contract,
                                         'type': discount_policy_type_contract}
            else:
                assert discount_info == {'type': discount_policy_type_contract}

    elif discount_pct_contract is not None:
        assert discount_info == {'fixed_discount_percent': discount_pct_contract,
                                 'type': 1}
    else:
        assert discount_info == {'type': 1}


@pytest.mark.parametrize('commission_charge_type_contract', [1, 0, None])
def test_commission_charge_type(session, commission_charge_type_contract):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            commission_charge_type=commission_charge_type_contract
                                            )

    session.flush()
    commission_charge_type = AttributeMapper(contract.collaterals[0], None).commission_charge_type
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    # if commission_charge_type_contract is None:
    #     commission_charge_type_contract = 1
    assert collaterals_info[0]['terms']['commission_charge_type'] == commission_charge_type_contract


@pytest.mark.parametrize('commission_payback_type_contract', [1, 2, None])
def test_commission_payback_type(session, commission_payback_type_contract):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            commission_payback_type=commission_payback_type_contract
                                            )

    session.flush()
    commission_payback_type = AttributeMapper(contract.collaterals[0], None).commission_payback_type
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['commission_payback_type'] == commission_payback_type_contract


@pytest.mark.parametrize('edo_type', ['2BE', None])
def test_edo_type(session, edo_type):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            edo_type=edo_type
                                            )

    session.flush()
    edo_type_prev = AttributeMapper(contract.collaterals[0], None).edo_type
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['edo_type'] == edo_type


@pytest.mark.parametrize('tickets, collateral_tickets, expected',
                         [('BALANCE-666 https://st.yandex-team.ru/PAYSUP-593275', None,
                           ['BALANCE-666,PAYSUP-593275', None]),
                        (None, 'BALANCE-666', [None, 'BALANCE-666'])])
def test_tickets(session, tickets, collateral_tickets, expected):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            tickets=tickets
                                            )

    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1000],
                               services={7},
                               currency=810,
                               tickets=collateral_tickets,
                               )

    session.flush()
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    for coll_id, result in enumerate(expected):
        if result:
            assert collaterals_info[coll_id]['terms']['tickets'] == result
        else:
            assert 'tickets' not in collaterals_info[coll_id]['terms']


@pytest.mark.parametrize('calc_termination', [TWO_MONTH_BEFORE, None])
def test_calc_termination(session, calc_termination):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            calc_termination=calc_termination
                                            )

    session.flush()
    calc_termination_prev = AttributeMapper(contract.collaterals[0], None).calc_termination
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['calc_termination'] == calc_termination


@pytest.mark.parametrize('loyal_clients_contract', [{"todate": "2013-02-28", "turnover": "4065509"}, None])
def test_collateral_loyal_clients(session, loyal_clients_contract):
    client = ob.ClientBuilder.construct(session)
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            loyal_clients={
                                                client.id: loyal_clients_contract} if loyal_clients_contract else None
                                            )
    session.flush()
    loyal_clients = AttributeMapper(contract.collaterals[0], None).loyal_clients
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    if loyal_clients_contract:
        assert collaterals_info[0]['terms']['loyal_clients'] == [{'turnover_date': datetime.datetime(2013, 2, 28, 0, 0),
                                                                  'client_id': client.id,
                                                                  'turnover': 4065509}]
    else:
        assert 'loyal_clients' not in collaterals_info[0]['terms']


def test_new_commissioner_report():
    # todo
    return


@pytest.mark.parametrize('netting_pct_contract', [D('2'), None])
@pytest.mark.parametrize('netting_pct_collateral', [D('3'), D('0')])
@pytest.mark.parametrize('netting_contract', [1, 0])
@pytest.mark.parametrize('netting_collateral', [1, 0])
def test_collateral_netting(session, netting_contract, netting_collateral, netting_pct_contract,
                            netting_pct_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            netting=netting_contract,
                                            netting_pct=netting_pct_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1019],
                               netting=netting_collateral,
                               netting_pct=netting_pct_collateral,
                               memo='memo')

    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1019],
                               netting_pct=D('4'),
                               memo='memo')

    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1019],
                               netting=1 - netting_collateral,
                               memo='memo')
    session.flush()
    # netting = AttributeMapper(contract.collaterals[0], None).netting_pct
    # netting_1 = AttributeMapper(contract.collaterals[1], None).netting_pct
    # AttributeMapper(contract.collaterals[2], None).netting_pct
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    #
    if netting_contract == 1:
        if netting_pct_contract:
            assert collaterals_info[0]['terms']['netting'] == {'netting_flag': 'Y',
                                                               'percent': D('2')}
        else:
            assert collaterals_info[0]['terms']['netting'] == {'netting_flag': 'Y'}
    else:
        if netting_pct_contract:
            assert collaterals_info[0]['terms']['netting'] == {'netting_flag': 'N',
                                                               'percent': D('2')}
        else:
            assert collaterals_info[0]['terms']['netting'] == {'netting_flag': 'N'}

    if netting_collateral == 1:
        assert collaterals_info[1]['terms']['netting'] == {'netting_flag': 'Y',
                                                           'percent': netting_pct_collateral}

    else:
        assert collaterals_info[1]['terms']['netting'] == {'netting_flag': 'N',
                                                           'percent': netting_pct_collateral}

    assert collaterals_info[2]['terms']['netting'] == {'percent': D('4')}

    if 1 - netting_collateral:
        assert collaterals_info[3]['terms']['netting'] == {'netting_flag': 'Y'}
    else:
        assert collaterals_info[3]['terms']['netting'] == {'netting_flag': 'N'}


@pytest.mark.parametrize('supercommission_contract', [7, 0])
@pytest.mark.parametrize('supercommission_collateral', [4, None])
def test_collateral_supercommission(session, supercommission_contract, supercommission_collateral):
    contract = ob.ContractBuilder.construct(session,
                                            ctype='GENERAL',
                                            contract_type=6,
                                            dt=TWO_MONTH_BEFORE,
                                            is_signed=TWO_MONTH_BEFORE,
                                            services={cst.ServiceId.TAXI_CORP},
                                            firm=cst.FirmId.YANDEX_OOO,
                                            supercommission=supercommission_contract
                                            )
    contract.append_collateral(dt=MONTH_BEFORE,
                               is_signed=None,
                               collateral_type=collateral_types['GENERAL'][1019],
                               supercommission=supercommission_collateral,
                               memo='memo')
    session.flush()
    supercommission = AttributeMapper(contract.collaterals[0], None).supercommission
    supercommission_1 = AttributeMapper(contract.collaterals[1], None).supercommission
    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    assert collaterals_info[0]['terms']['supercommission'] == supercommission_contract
    assert collaterals_info[1]['terms']['supercommission'] == supercommission_collateral
