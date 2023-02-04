# -*- coding: utf-8 -*-
from copy import copy
from datetime import datetime, timedelta
from decimal import Decimal

import pytest
from butils import logger
import hamcrest as hm
from dateutil.relativedelta import relativedelta
from xmlrpclib import Fault

from balance import mapper
from balance import muzzle_util as ut
from billing.contract_iface.constants import ContractTypeId
from balance.constants import *
from balance.constants import OebsPayReceiveType
from billing.contract_iface.contract_meta import contract_attributes
from balance.mapper import get_unified_corp_contract_services
from tests import object_builder as ob
from tests.base import MediumTest

log = logger.get_logger()
MEMO = u"Договор создан автоматически\n"


class ContractUtils(MediumTest):
    maxDiff = None

    PARTNERS_MANAGER_UID = 29786964
    MANAGER_UID = 244916211
    DISTRIBUTION_MANAGER_UID = 322921809

    def setUp(self):
        super(ContractUtils, self).setUp()
        self.unified_corp_contract_services = get_unified_corp_contract_services(self.session)

    def partners_offer_params(self):
        person = ob.PersonBuilder(is_partner=1).build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      contract_type=9,
                      ctype='PARTNERS',
                      currency='RUB',
                      firm_id=1,
                      nds=18,
                      manager_uid=self.PARTNERS_MANAGER_UID,
                      partner_pct=43,
                      payment_type=1,
                      person_id=person.id,
                      signed=1,
                      start_dt=datetime(2018, 5, 1),
                      service_start_dt=datetime(2018, 4, 1)
                      )
        return params

    def new_partners_offer(self, func_name='CreateOffer'):
        params = self.partners_offer_params()
        func = getattr(self.xmlrpcserver, func_name)
        return func(self.session.oper_id, params)

    def new_testmode_partners_offer(self):
        params = self.partners_offer_params()
        person_id = params.pop('person_id')
        params.update(test_mode=1, signed=0)
        res = self.xmlrpcserver.CreateOffer(self.session.oper_id, params)
        res.update(person_id=person_id)
        return res

    def zen_params(self):
        person = ob.PersonBuilder(is_partner=1).build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      currency='RUB',
                      firm_id=28,
                      manager_uid=self.MANAGER_UID,
                      nds=18,
                      person_id=person.id,
                      start_dt=datetime(2018, 5, 1),
                      services=[134],
                      )
        return params

    def new_zen_contract(self):
        params = self.zen_params()
        return self.xmlrpcserver.CreateOffer(self.session.oper_id, params)

    def taxi_contract_params(self, add_corp_taxi):
        person = ob.PersonBuilder(type='ur').build(self.session).obj
        client = person.client
        services = [ServiceId.TAXI_CASH, ServiceId.TAXI_CARD, ServiceId.TAXI_PAYMENT,
                    ServiceId.UBER_PAYMENT, ServiceId.UBER_PAYMENT_ROAMING]

        result = {
            'client_id': client.id,
            'person_id': person.id,
            'manager_uid': self.MANAGER_UID,

            'country': RegionId.RUSSIA,
            'region': 213,  # Москва
            'firm_id': FirmId.TAXI,
            'services': services,

            'currency': 'RUB',
            'nds_for_receipt': 18,
            'offer_confirmation_type': 'no',
            'payment_type': PREPAY_PAYMENT_TYPE,
            'personal_account': 1,
            'start_dt': ut.month_first_day(self.session.now()),
        }

        if add_corp_taxi:
            result['ctype'] = 'GENERAL'
            services.append(ServiceId.TAXI_CORP)

        return result

    def new_taxi_contract(self, add_corp_taxi=False, commission=ContractTypeId.NON_AGENCY):
        contract_params = self.taxi_contract_params(add_corp_taxi)
        contract_params['commission'] = commission
        return self.xmlrpcserver.CreateCommonContract(self.session.oper_id, contract_params)

    def new_taxi_offer(self, add_corp_taxi):
        return self.xmlrpcserver.CreateOffer(self.session.oper_id, self.taxi_contract_params(add_corp_taxi))

    def distribution_group_offer_params(self):
        person = ob.PersonBuilder.construct(self.session, is_partner=1)
        client = person.client
        start_dt = ut.month_first_day(self.session.now() - relativedelta(months=2))
        params = dict(
            person_id=person.id,
            client_id=client.id,
            ctype='DISTRIBUTION',
            distribution_contract_type=DistributionContractType.GROUP_OFFER,
            firm_id=1,
            currency='RUB',
            nds=18,
            manager_uid=self.DISTRIBUTION_MANAGER_UID,
            service_start_dt=start_dt,
            start_dt=start_dt
        )
        return params

    def new_distribution_group_offer(self):
        return self.xmlrpcserver.CreateOffer(
            self.session.oper_id,
            self.distribution_group_offer_params()
        )

    def distribution_child_offer_params(self, group_offer_id):
        client_id, person_id = self.session.execute(
            'select client_id, person_id from bo.t_contract2 where id={}'.format(group_offer_id)
        ).next()
        start_dt = ut.month_first_day(self.session.now() - relativedelta(months=2))
        tag = ob.DistributionTagBuilder.construct(self.session, client_id=client_id)
        return dict(
            parent_contract_id=group_offer_id,
            person_id=person_id,
            client_id=client_id,
            ctype='DISTRIBUTION',
            distribution_contract_type=DistributionContractType.CHILD_OFFER,
            firm_id=1,
            currency='RUB',
            nds=18,
            manager_uid=self.DISTRIBUTION_MANAGER_UID,
            service_start_dt=start_dt,
            start_dt=start_dt,
            distribution_tag=tag.id,
            supplements={1},
            products_revshare={'13002': '1'},
            signed=1,
        )

    def new_distribution_child_offer(self, group_offer_id=None, params=None):
        if params is None:
            if group_offer_id is None:
                group_offer_id = self.new_distribution_group_offer()['ID']
            params = self.distribution_child_offer_params(group_offer_id)
        else:
            group_offer_id = params['parent_contract_id']

        contract = self.session.query(mapper.Contract).get(group_offer_id)
        if not contract.signed:
            self.sign_contract(contract)

        return self.xmlrpcserver.CreateOffer(
            self.session.oper_id,
            params
        )

    def unified_corp_contract_params(self, client_id=None):
        if client_id:
            person = ob.PersonBuilder(type='ur', client_id=client_id).build(self.session).obj
        else:
            person = ob.PersonBuilder(type='ur').build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      currency='RUR',
                      firm_id=FirmId.TAXI,
                      manager_uid=self.MANAGER_UID,
                      payment_term=10,
                      payment_type=POSTPAY_PAYMENT_TYPE,
                      person_id=person.id,
                      personal_account=1,
                      services=self.unified_corp_contract_services,
                      signed=1,
                      start_dt=ut.month_first_day(self.session.now()) - relativedelta(months=1),
                      )
        return params

    def new_unified_corp_contract(self, services=None, start_dt=None, finish_dt=None, client_id=None):
        contract_params = self.unified_corp_contract_params(client_id=client_id)
        if services:
            contract_params['services'] = services
        if start_dt:
            contract_params['start_dt'] = start_dt
        if finish_dt:
            contract_params['finish_dt'] = finish_dt
        return self.xmlrpcserver.CreateCommonContract(self.session.oper_id, contract_params)

    def check_create_contract_res(self, res, with_attributes=False):
        self.assertIsInstance(res, dict)

        expected_keys = {'ID', 'EXTERNAL_ID'}
        if with_attributes:
            expected_keys.add('PAGE_ATTRIBUTES')
        self.assertItemsEqual(res.keys(), expected_keys)

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        self.assertEqual(contract_obj.external_id, res['EXTERNAL_ID'])

    def check_create_collateral_res(self, res):
        self.assertIsInstance(res, dict)
        self.assertItemsEqual(res.keys(), {'CONTRACT_ID', 'CONTRACT_EXTERNAL_ID', 'COLLATERAL_NUM'})

        contract_obj = self.session.query(mapper.Contract).get(res['CONTRACT_ID'])
        self.assertEqual(contract_obj.external_id, res['CONTRACT_EXTERNAL_ID'])

    def compare_contracts_params(self, actual_params, expected_params):

        special_attrs = {
            'print_tpl_barcode': lambda x: isinstance(x, (int, long)) and x > 0,
            'is_signed': lambda x: x > (datetime.now() - timedelta(days=1))
        }

        diffs = u''

        wrongs = u''
        for attr in actual_params.keys():
            if attr in special_attrs:
                val = actual_params.pop(attr)
                check = special_attrs[attr]
                if not check(val):
                    wrongs += u'\nWrong value {} for {}'.format(val, attr)

        diffs += wrongs

        not_in_expected = set(actual_params.keys()) - set(expected_params.keys())
        not_in_actual = set(expected_params.keys()) - set(actual_params.keys())

        if not_in_expected:
            diffs += u'\nIn actual, but not in expected: {}'.format(str(not_in_expected))
        if not_in_actual:
            diffs += u'\nIn expected, but not in actual: {}'.format(str(not_in_actual))

        not_equals = u''
        for attr in set(actual_params).intersection(set(expected_params)):
            v1, v2 = actual_params[attr], expected_params[attr]
            if v1 != v2:
                not_equals += u'\n%s in actual is %s but in expected is %s' % (attr, v1, v2)

        if not_equals:
            diffs += u'\nDifferent values for same keys:'
            diffs += not_equals

        self.assertFalse(diffs, diffs)

    def check_contract(self, contract_id, expected_attributes):

        contract_obj = self.session.query(mapper.Contract).get(contract_id)

        cstate = contract_obj.current_state()
        ctype = contract_obj.type
        possible_attributes = [attr.lower() for attr in contract_attributes[ctype].keys()]

        actual_state = {attr: cstate.get(attr)
                        for attr in possible_attributes}

        expected_state = {attr: value
                          for (attr, value) in expected_attributes.items()}

        normalize = lambda dct: {k: v for (k, v) in dct.items()
                                 if (v is not None and
                                     v not in (set(), {}, []))
                                 }

        actual_state = normalize(actual_state)
        expected_state = normalize(expected_state)

        self.compare_contracts_params(actual_state, expected_state)

    def get_expected_partner_offer_page_attributes(self, contract_id):

        def normalize_vals(expected_states):
            for state_attr, (state_val, attrs_to_change) in expected_states.items():
                if not isinstance(state_val, (list, tuple)):
                    state_val_list = [state_val] * len(attrs_to_change)
                    expected_states[state_attr] = (state_val_list, attrs_to_change)
            return expected_states

        def update_state(expected_attributes, attrs_to_change, state_vals, state_attr):
            attr_to_val = dict(zip(attrs_to_change, state_vals))
            for attr, state in expected_attributes.items():
                if attr in attr_to_val:
                    expected_attributes[attr][state_attr] = attr_to_val[attr]

        contract = self.session.query(mapper.Contract).get(contract_id)

        # ожидаемые значения
        expected_values = {
            'trace': None,
            'external_id': contract.external_id,
            'contract_type': 9,
            'firm': 1,
            'client_id': contract.client.id,
            'person_id': contract.person.id,
            'pay_to': OebsPayReceiveType.BANK,
            'currency': 643,
            'manager_code': 20160,
            'dt': datetime(2018, 5, 1),
            'service_start_dt': datetime(2018, 4, 1),
            'end_dt': None,
            'payment_type': 1,
            'memo': MEMO,
            'atypical_conditions': None,
            'reward_type': 1,
            'partner_pct': 43,
            'market_api_pct': 50,
            'nds': 0,
            'unilateral_acts': 1,
            'mkb_price': {},
            'search_forms': 0,
            'open_date': 0,
            'other_revenue': 0,
            'is_booked': None,
            'is_booked_dt': None,
            'test_mode': 0,
            'is_faxed': None,
            'is_signed': contract.col0.is_signed,
            'is_archived': None,
            'is_archived_dt': None,
            'sent_dt': None,
            'is_cancelled': None,
            'button_submit': None,
            'type': 'PARTNERS',
            'invisible': None,
            'individual_docs': None,
            'id': contract_id,
            'selfemployed': None
        }

        # ожидаемые состояния
        default_state = {'origenabled': True, 'changed': True, 'enabled': True, 'value': None, 'visible': True,
                         'oldvalue': None}
        expected_attributes = {attr: copy(default_state) for attr in expected_values}

        not_changed_attrs = {attr for attr, val in expected_values.items() if val in (None, {}) or attr == 'type'}

        # state_attr: (state_val or state_val_list, attrs_to_change)
        expected_states = normalize_vals({
            'changed': (False, not_changed_attrs),
            'enabled': (False, {'pay_to', 'nds', 'is_archived', 'is_archived_dt'}),
            'oldvalue': ([{}, 'PARTNERS'], ['mkb_price', 'type'])
        })

        update_state(expected_attributes, expected_values.keys(), expected_values.values(), 'value')
        for state_attr, (state_val_list, attrs_to_change) in expected_states.items():
            update_state(expected_attributes, attrs_to_change, state_val_list, state_attr)
        return expected_attributes

    def check_page_attributes(self, page_attributes, expected_attributes):

        def cast_types(expected_dict, actual_dict):
            def cast_values_types(expected_value, actual_value):
                # приводим только типы, которые не приводит сам pytest
                if type(expected_value) != type(actual_value) and \
                        not (isinstance(expected_value, basestring) and isinstance(actual_value, basestring)):
                    if isinstance(expected_value, basestring):
                        actual_value = type(expected_value)(actual_value)
                    elif isinstance(actual_value, basestring):
                        expected_value = type(actual_value)(expected_value)
                return expected_value, actual_value

            if not (isinstance(expected_dict, dict) and isinstance(actual_dict, dict)):
                raise TypeError("Expected two dicts but got {} and {}".format(type(expected_dict), type(actual_dict)))

            for k in expected_dict:
                if k in actual_dict:
                    if isinstance(expected_dict[k], dict) and isinstance(actual_dict[k], dict):
                        expected_dict[k], actual_dict[k] = cast_types(expected_dict[k], actual_dict[k])
                    else:
                        expected_dict[k], actual_dict[k] = cast_values_types(expected_dict[k], actual_dict[k])
            return expected_dict, actual_dict

        # убираем групповые атрибуты и атрибуты ДС
        page_attributes = {k: v for k, v in page_attributes.items() if k not in
                           ('form', 'collateral_form', 'group01', 'group02', 'group03')
                           and not k.startswith('col_new_')}
        # оставляем только видимые атрибуты
        page_attributes = {k: v for k, v in page_attributes.items() if v['visible']}

        cast_types(expected_attributes, page_attributes)
        hm.assert_that(expected_attributes, hm.has_entries(page_attributes))

    def sign_contract(self, contract, dt=None):
        contract.col0.is_signed = dt or self.session.now()


class TestContractXMLRPC(ContractUtils):
    PARTNERS_OFFER_ATTRIBUTES = {
        'firm': 1,
        'memo': MEMO,
        'manager_code': 20160,
        'currency': 643,
        'payment_type': 1,
        'contract_type': 9,
        'dt': datetime(2018, 5, 1),
        'service_start_dt': datetime(2018, 4, 1),
        'reward_type': 1,
        'test_mode': 0,
        'nds': 0,
        'partner_pct': 43,
        'market_api_pct': 50,
        'pay_to': OebsPayReceiveType.BANK,
        'unilateral_acts': 1,
        'open_date': 0,
        'other_revenue': 0,
        'search_forms': 0,
        'services': {ServiceId.RNY},
    }

    def test_create_partners_offer(self):
        res = self.new_partners_offer()
        self.check_create_contract_res(res)
        self.check_contract(res['ID'], self.PARTNERS_OFFER_ATTRIBUTES)

    def test_create_partners_offer2(self):
        res = self.new_partners_offer(func_name='CreateOffer2')
        self.check_create_contract_res(res, with_attributes=True)
        self.check_contract(res['ID'], self.PARTNERS_OFFER_ATTRIBUTES)
        self.check_page_attributes(res['PAGE_ATTRIBUTES'], self.get_expected_partner_offer_page_attributes(res['ID']))

    def test_create_partners_collateral(self):
        new_contract_id = self.new_partners_offer()['ID']
        end_dt = datetime.now() + timedelta(days=1)
        res = self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                                 new_contract_id,
                                                 2090,
                                                 {'end_dt': end_dt, 'end_reason': 2})
        expected_attributes = self.PARTNERS_OFFER_ATTRIBUTES.copy()
        expected_attributes.update(end_dt=end_dt.replace(microsecond=0),
                                   end_reason=2)
        self.check_create_collateral_res(res)
        self.check_contract(res['CONTRACT_ID'], expected_attributes)

    def test_update_partners_contract(self):
        new_contract_res = self.new_testmode_partners_offer()
        contract_id = new_contract_res['ID']
        person_id = new_contract_res['person_id']
        res = self.xmlrpcserver.UpdateContract(self.session.oper_id,
                                               contract_id,
                                               {'person_id': person_id, 'signed': 1})
        expected_attributes = self.PARTNERS_OFFER_ATTRIBUTES.copy()
        expected_attributes.update(memo=MEMO + MEMO)
        self.check_create_contract_res(res)
        self.check_contract(res['ID'], expected_attributes)

    def test_selfemployed_collateral(self):
        new_contract_id = self.new_partners_offer()['ID']
        cc_1 = self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                                 new_contract_id,
                                                 2040,
                                                 {'memo': 'memo1'})
        cc_2 = self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                                  new_contract_id,
                                                  2040,
                                                  {'memo': 'memo2'})
        res = self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                                  new_contract_id,
                                                  2160,
                                                  {'selfemployed': 1})
        cc_3 = self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                                  new_contract_id,
                                                  2040,
                                                  {'memo': 'memo3'})
        res2 = self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                                 new_contract_id,
                                                 2160,
                                                 {'selfemployed': 0, 'DT': ut.trunc_date(datetime.now() + timedelta(1))})
        expected_attributes = self.PARTNERS_OFFER_ATTRIBUTES.copy()
        expected_attributes.update({'selfemployed': 0})
        assert res['COLLATERAL_NUM'] == u'Ф-01'
        assert res2['COLLATERAL_NUM'] == u'Ф-02'
        self.check_contract(res['CONTRACT_ID'], expected_attributes)


    ZEN_OFFER_ATTRIBUTES = {
        'firm': 28,
        'is_offer': 1,
        'memo': MEMO,
        'manager_code': 21902,
        'other_revenue': 0,
        'pay_to': OebsPayReceiveType.BANK,
        'currency': 643,
        'payment_type': 1,
        'services': {134},
        'dt': datetime(2018, 5, 1),
        'nds': 18,
    }

    def test_create_zen_collateral(self):
        new_contract_id = self.new_zen_contract()['ID']
        end_dt = datetime.now() + timedelta(days=1)
        res = self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                                 new_contract_id,
                                                 7050,
                                                 {'end_dt': end_dt})
        expected_attributes = self.ZEN_OFFER_ATTRIBUTES.copy()
        expected_attributes.update(end_dt=end_dt.replace(microsecond=0))
        self.check_create_collateral_res(res)
        contract = self.session.query(mapper.Contract).get(new_contract_id)
        self.assertTrue(len(contract.collaterals) == 2)  # col0 and new collateral
        self.assertTrue(contract.collaterals[-1].signed == True)
        self.check_contract(res['CONTRACT_ID'], expected_attributes)

    def test_link_contract_id(self):
        # создание связанного договора: доходная версия
        def linked_contract_general_params(main_contract):
            return {
                'client_id': main_contract.client.id,
                'person_id': main_contract.person.id,
                'manager_uid': self.MANAGER_UID,

                'firm_id': FirmId.GAS_STATIONS,
                'services': [ServiceId.ZAXI],

                'currency': 'RUB',
                'payment_type': POSTPAY_PAYMENT_TYPE,
                'payment_term': 10,
                'personal_account': 1,
                'start_dt': ut.month_first_day(self.session.now()),

                'link_contract_id': main_contract.id,
            }

        # создание связанного договора: расходная версия
        def linked_contract_spendable_params(main_contract):
            person_partner = ob \
                .PersonBuilder(client=main_contract.client,
                               is_partner=True,
                               type=main_contract.person.type) \
                .build(self.session) \
                .obj
            return {
                'client_id': main_contract.client.id,
                'person_id': person_partner.id,
                'manager_uid': self.MANAGER_UID,

                'country': RegionId.RUSSIA,
                'region': 213,  # Москва
                'firm_id': FirmId.TAXI,
                'services': [ServiceId.TAXI_PROMO],
                'currency': 'RUB',
                'start_dt': ut.month_first_day(self.session.now()),

                'link_contract_id': main_contract.id,
                'nds': 0,
                'signed': 1,
            }

        for step, linked_contract_params_method, create_contract_method in (
                ('linked_general', linked_contract_general_params, self.xmlrpcserver.CreateOffer),
                ('linked_spendable', linked_contract_spendable_params, self.xmlrpcserver.CreateCommonContract),
        ):
            log.info('Step #%s' % step)

            # создаём неподписанный доходный договор Такси
            contract_general = self.session.query(mapper.Contract).getone(self.new_taxi_contract()['ID'])

            # параметры связанного договора
            linked_contract_params = linked_contract_params_method(contract_general)

            # пытаемся создать связанный договор - не должно получиться
            with pytest.raises(Exception) as exc_info:
                create_contract_method(self.session.oper_id, linked_contract_params)
            expected_error_msg = u'Нельзя подписывать договор, связанный с неподписанным. ' \
                                 u'ID связанного неподписанного договора = %d' % contract_general.id
            assert expected_error_msg in exc_info.value.faultString

            # подписываем доходный договор и повторяем попытку - должно получиться
            self.sign_contract(contract_general)
            create_contract_method(self.session.oper_id, linked_contract_params)

    def test_CreateCollateral_is_signed(self):
        """ Создание допника с подписью """

        CANCELLATION_COLLATERAL_TYPE = 90

        contract_id = self.new_taxi_contract()['ID']
        contract = self.session.query(mapper.Contract).get(contract_id)
        contract_dt = contract.col0.dt

        params = {}
        params['SIGN'] = 1
        params['DT'] = contract_dt
        params['SIGN_DT'] = contract_dt
        params['FINISH_DT'] = contract_dt + relativedelta(months=1)

        self.sign_contract(contract)
        self.xmlrpcserver.CreateCollateral(
            self.session.oper_id,
            contract_id,
            CANCELLATION_COLLATERAL_TYPE,
            params)
        self.session.refresh(contract)

        # Для не оферты
        assert contract.is_offer == False
        # Создался допник в котором проставилась подпись с правильной
        # датой и время обрезалось
        col = contract.collaterals[1]
        assert col.is_signed == ut.trunc_date(params['SIGN_DT'])
        # и условия допника действуют
        cs = contract.current_signed()
        assert cs.finish_dt == params['FINISH_DT']

    def test_CreateCollateral_test_default_sign_dt(self):
        """ Создание допника без SIGN_DT """

        CANCELLATION_COLLATERAL_TYPE = 90
        params = {
            'FINISH_DT': datetime.now() + timedelta(1),
            'SIGN': 1,
        }

        contract_id = self.new_taxi_contract()['ID']
        contract = self.session.query(mapper.Contract).get(contract_id)
        self.sign_contract(contract)

        self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                           contract_id,
                                           CANCELLATION_COLLATERAL_TYPE,
                                           params)
        self.session.refresh(contract)
        col = contract.collaterals[1]

        # Когда не передаем SIGN_DT туда пишется дата col.dt без времени
        assert col.is_signed == ut.trunc_date(col.dt)

    def test_CreateCollateral_sign_dt_in_past(self):
        """ Создание допника с DT и SIGN_DT в прошлом """

        CANCELLATION_COLLATERAL_TYPE = 90

        contract_id = self.new_taxi_contract()['ID']
        contract = self.session.query(mapper.Contract).get(contract_id)
        self.sign_contract(contract)
        contract_dt = contract.col0.dt

        params = {}
        params['SIGN'] = 1
        params['DT'] = contract_dt
        params['SIGN_DT'] = contract_dt.replace(
            year=contract_dt.year - 1)
        params['FINISH_DT'] = contract_dt + relativedelta(months=1)

        self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                           contract_id,
                                           CANCELLATION_COLLATERAL_TYPE,
                                           params)
        self.session.refresh(contract)
        col = contract.collaterals[1]

        # Дата допника проставилась и обрезалась
        assert col.dt == ut.trunc_date(params['DT'])

        # Дата подписи проставилась и обрезалась
        assert col.is_signed == ut.trunc_date(params['SIGN_DT'])

    def test_CreateCollateral_distribution_child_offer_revshare(self):
        """ Создание допника на изменение процента партнёров """
        REVSHARE_COLLATERAL_TYPE = 3020

        group_offer_id = self.new_distribution_group_offer()['ID']
        contract_params = self.distribution_child_offer_params(group_offer_id)
        contract_id = self.new_distribution_child_offer(params=contract_params)['ID']

        def dates_generator():
            contract = self.session.query(mapper.Contract).get(contract_id)
            start, i = contract.col0.dt, 1
            while True:
                yield start + timedelta(days=i)
                i += 1
        dt = dates_generator()

        def checker(params, expected_revshare):
            res = self.xmlrpcserver.CreateCollateral(
                self.session.oper_id,
                contract_id,
                REVSHARE_COLLATERAL_TYPE,
                params
            )
            self.check_create_collateral_res(res)

            contract = self.session.query(mapper.Contract).get(res['CONTRACT_ID'])
            cstate = contract.current_state()
            revshare = cstate.get('products_revshare')
            assert revshare == expected_revshare

        expected_revshare = {
            13002: {'value_str': None, 'value_num': Decimal(1)}
        }

        # Дополнение
        params = dict(
            DT=next(dt),
            products_revshare={'10000': '10'},
            products_revshare_scales={'10002': 'NET-749_retail_fixed_vimpelcom'}
        )
        expected_revshare.update({
            10000: {'value_num': Decimal(10), 'value_str': None},
            10002: {'value_num': None, 'value_str': 'NET-749_retail_fixed_vimpelcom'}
        })
        checker(params, expected_revshare)

        # Смена шкалы на процент и наоборот
        params = dict(
            DT=next(dt),
            products_revshare_scales={'10000': 'NET-772_retail_fixed_nasvyazi_ESC'},
            products_revshare={'10002': '30'}
        )
        expected_revshare.update({
            10000: {'value_num': None, 'value_str': 'NET-772_retail_fixed_nasvyazi_ESC'},
            10002: {'value_num': Decimal(30), 'value_str': None}
        })
        checker(params, expected_revshare)


class TestUnifiedCorpContract(ContractUtils):
    def test_remove_service_first_col(self):
        start_dt = ut.month_first_day(self.session.now()) - relativedelta(months=1)
        contract_id = self.new_unified_corp_contract(
            services=self.unified_corp_contract_services[:],
            start_dt=start_dt,
        )['ID']

        error_message = u''
        try:
            params = {
                'SERVICES': self.unified_corp_contract_services[:-2],
                'DT': start_dt + relativedelta(days=1),
                'SIGN': 1,
            }
            self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                               contract_id,
                                               CollateralType.CHANGE_SERVICES,
                                               params)
        except Exception as e:
            assert isinstance(e, Fault)
            error_message = e.faultString
        assert u'Нельзя удалять сервисы единого договора' in error_message

    def test_remove_service_second_col(self):
        start_dt = ut.month_first_day(self.session.now()) - relativedelta(months=1)
        contract_id = self.new_unified_corp_contract(
            services=self.unified_corp_contract_services[:-2],
            start_dt=start_dt,
        )['ID']

        params = {
            'SERVICES': self.unified_corp_contract_services[:],
            'DT': start_dt + relativedelta(days=1),
            'SIGN': 1,
        }
        self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                           contract_id,
                                           CollateralType.CHANGE_SERVICES,
                                           params)
        error_message = u''
        try:
            params = {
                'SERVICES': self.unified_corp_contract_services[:-2],
                'DT': start_dt + relativedelta(days=2),
                'SIGN': 1,
            }
            self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                               contract_id,
                                               CollateralType.CHANGE_SERVICES,
                                               params)
        except Exception as e:
            assert isinstance(e, Fault)
            error_message = e.faultString
        assert u'Нельзя удалять сервисы единого договора' in error_message

    def test_wrong_service_index_postion_order(self):
        start_dt = ut.month_first_day(self.session.now()) - relativedelta(months=1)
        contract_id = self.new_unified_corp_contract(
            services=[ServiceId.DRIVE_B2B],
            start_dt=start_dt,
        )['ID']

        error_message = u''
        try:
            params = {
                'SERVICES': [ServiceId.TAXI_CORP_CLIENTS, ServiceId.TAXI_CORP, ServiceId.DRIVE_B2B],
                'DT': start_dt + relativedelta(days=1),
                'SIGN': 1,
            }
            self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                               contract_id,
                                               CollateralType.CHANGE_SERVICES,
                                               params)
        except Exception as e:
            assert isinstance(e, Fault)
            error_message = e.faultString
        assert u'Нарушение порядка добавления сервисов в единый договор' in error_message

    def test_intersection_with_another_contract_future_period(self):
        start_dt_1 = ut.month_first_day(self.session.now()) - relativedelta(months=1)
        finish_dt_1 = start_dt_1 + relativedelta(days=5)
        contract_id_1 = self.new_unified_corp_contract(
            services=self.unified_corp_contract_services[:-2],
            start_dt=start_dt_1,
            finish_dt=finish_dt_1
        )['ID']

        contract_1 = self.session.query(mapper.Contract).getone(contract_id_1)
        client_id = contract_1.client_id

        start_dt_2 = finish_dt_1
        finish_dt_2 = start_dt_2 + relativedelta(days=5)
        contract_id_2 = self.new_unified_corp_contract(
            services=self.unified_corp_contract_services[:],
            start_dt=start_dt_2,
            finish_dt=finish_dt_2,
            client_id=client_id
        )['ID']


        params = {
            'SERVICES': self.unified_corp_contract_services[:],
            'DT': start_dt_1 + relativedelta(days=1),
            'SIGN': 1,
        }
        self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                           contract_id_1,
                                           CollateralType.CHANGE_SERVICES,
                                           params)
        hist = contract_1.get_attribute_history('SERVICES')
        expected = [
            (start_dt_1, {sid: 1 for sid in self.unified_corp_contract_services[:-2]}),
            (start_dt_1 + relativedelta(days=1), {sid: 1 for sid in self.unified_corp_contract_services[:]}),
        ]
        assert len(hist) == len(expected)
        for i, (dt, services) in enumerate(expected):
            assert hist[i][0] == dt
            assert hist[i][1] == services


    def test_intersection_with_another_contract_same_periods(self):
        start_dt_1 = ut.month_first_day(self.session.now()) - relativedelta(months=1)
        finish_dt_1 = start_dt_1 + relativedelta(days=5)
        contract_id_1 = self.new_unified_corp_contract(
            services=self.unified_corp_contract_services[:-2],
            start_dt=start_dt_1,
            finish_dt=finish_dt_1
        )['ID']

        contract_1 = self.session.query(mapper.Contract).getone(contract_id_1)
        client_id = contract_1.client_id

        start_dt_2 = ut.month_first_day(self.session.now()) - relativedelta(months=1)
        finish_dt_2 = start_dt_2 + relativedelta(days=5)
        contract_id_2 = self.new_unified_corp_contract(
            services=self.unified_corp_contract_services[-2:],
            start_dt=start_dt_2,
            finish_dt=finish_dt_2,
            client_id=client_id
        )['ID']

        error_message = u''
        try:
            params = {
                'SERVICES': self.unified_corp_contract_services[:],
                'DT': start_dt_1 + relativedelta(days=1),
                'SIGN': 1,
            }
            self.xmlrpcserver.CreateCollateral(self.session.oper_id,
                                               contract_id_1,
                                               CollateralType.CHANGE_SERVICES,
                                               params)
        except Exception as e:
            assert isinstance(e, Fault)
            error_message = e.faultString
        assert u'Пересечение сервисов единого договора по датам с договором' in error_message
