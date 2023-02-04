# -*- coding: utf-8 -*-

import pytest
from datetime import datetime, timedelta
from collections import Iterable
from decimal import Decimal as D

from tests import object_builder as ob

from balance import mapper
from balance.constants import *
from balance.constants import OebsPayReceiveType
from billing.contract_iface.contract_meta import ContractTypes


def nullify_time_of_date(date):
    return date.replace(hour=0, minute=0, second=0, microsecond=0) if date else None


NDS_DEFAULT = 18
NDS_UKRAINE = 20
NDS_NOT_RESIDENT = 0
WO_NDS_RECEIPT = -1
NDS_ZERO = 0

BANK_ID = 21

CURRENT_DT = nullify_time_of_date(datetime.today())
START_DT_PAST = CURRENT_DT.replace(day=1) + timedelta(days=-200)


class SpendableContractDefaults(object):
    MARKET_MARKETING = {'CONTRACT_PARAMS': {'services': [ServiceId.MARKETPLACE], 'currency': 'RUR', 'currency_code': 643,
                                            'firm_id': FirmId.MARKET, 'nds': NDS_DEFAULT},
                        'PERSON_TYPE': 'ur'}
    TAXI_COROBA = {'CONTRACT_PARAMS': {'services': [ServiceId.MARKETING_COROBA], 'currency': 'RUR', 'currency_code': 643,
                                       'firm_id': FirmId.TAXI, 'nds': NDS_DEFAULT, 'payment_sum': 8000,
                                       'pay_to': OebsPayReceiveType.TINKOFF},
                   'PERSON_TYPE': 'ur'}
    TAXI_DONATE_RUSSIA = {'CONTRACT_PARAMS': {'services': [ServiceId.TAXI_PROMO], 'currency': 'RUR', 'currency_code': 643,
                                              'firm_id': FirmId.TAXI, 'nds': NDS_DEFAULT,
                                              'country': RegionId.RUSSIA, 'region': '02000001000'},
                          'PERSON_TYPE': 'ur'}
    TAXI_DONATE_KZT = {'CONTRACT_PARAMS': {'services': [ServiceId.TAXI_PROMO], 'currency': 'USD',  'currency_code': 840,
                                           'firm_id': FirmId.TAXI_EU_BV, 'nds': NDS_NOT_RESIDENT,
                                           'country': RegionId.KAZAKHSTAN},
                       'PERSON_TYPE': 'eu_yt'}
    TAXI_DONATE_UA = {'CONTRACT_PARAMS': {'services': [ServiceId.TAXI_PROMO], 'currency': 'UAH',  'currency_code': 980,
                                          'firm_id': FirmId.TAXI_UA, 'nds': NDS_UKRAINE,
                                          'country': RegionId.UKRAINE},
                      'PERSON_TYPE': 'ua'}
    TAXI_CORP = {'CONTRACT_PARAMS': {'services': [ServiceId.TAXI_CORP, ServiceId.TAXI_CORP_PARTNERS], 'currency': 'RUR',
                                     'currency_code': 643, 'firm_id': FirmId.TAXI, 'nds': NDS_DEFAULT,
                                     'country': RegionId.RUSSIA, 'region': '02000001000', 'ctype': 'SPENDABLE'},
                 'PERSON_TYPE': 'ur'}
    TELEMEDICINE_DONATE = {'CONTRACT_PARAMS': {'services': [ServiceId.TELEMED_PROMO],
                                               'currency': 'RUR', 'currency_code': 643, 'firm_id': FirmId.HEALTH_CLINIC,
                                               'nds': NDS_DEFAULT},
                           'PERSON_TYPE': 'ur'}
    ADDAPPTER_2_0 = {'CONTRACT_PARAMS': {'services': [ServiceId.ADDAPPTER_2],
                                         'currency': 'RUR',
                                         'currency_code': 643,
                                         'firm_id': FirmId.YANDEX_OOO,
                                         'pay_to': OebsPayReceiveType.YANDEX_MONEY,
                                         'payment_type': 1,  # Период актов (выплат)
                                         'nds': NDS_ZERO},
                     'PERSON_TYPE': 'ph'}


class GeneralPartnerContractDefaults(object):
    YANDEX_TAXI = {'CONTRACT_PARAMS': {'firm_id': FirmId.TAXI, 'country': RegionId.RUSSIA, 'region': '77000000000',
                                       'currency': 'RUR', 'currency_code': 643,
                                       'services': [111, 124, 128, 125, 605, 626],
                                       'nds_for_receipt': NDS_DEFAULT,
                                       'personal_account': 1, 'service_min_cost': D('0'),
                                       },
                    'PERSON_TYPE': 'ur'
                   }
    YANDEX_TAXI_BV = {'CONTRACT_PARAMS': {'firm_id': FirmId.TAXI_EU_BV, 'country': RegionId.KAZAKHSTAN,
                                          'currency': 'USD',  'currency_code': 840, 'services': [111, 124, 128],
                                          'personal_account': 1, 'service_min_cost': D('0'),
                                          'partner_commission_pct2': D('0'),
                                          },
                      'PERSON_TYPE': 'eu_yt'
                      }
    YANDEX_TAXI_UKRAINE = {
        'CONTRACT_PARAMS': {'firm_id': FirmId.TAXI_UA, 'country': RegionId.UKRAINE,
                            'currency': 'UAH',  'currency_code': 980, 'services': [124, 128],
                            'personal_account': 1, 'service_min_cost': D('0'),
                            'partner_commission_pct2': D('0')},
        'PERSON_TYPE': 'ua'
    }
    TELEMEDICINE = {'CONTRACT_PARAMS': {'firm_id': FirmId.HEALTH_CLINIC,
                                        'currency': 'RUR', 'currency_code': 643,
                                        'services': [270, 170], 'personal_account': 1,
                                        'medicine_pay_commission': D('9.99'), 'medicine_pay_commission2': D('90'),
                                        },
                    'PERSON_TYPE': 'ur'
    }


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class ContractUtils(object):
    def contract_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      person_id=person.id,
                      manager_uid=manager.domain_passport_id,
                      firm_id=1,
                      currency='RUR',
                      currency_code=634,
                      payment_term=10,
                      payment_type=2,
                      services=[202]
                      )
        return params

    def new_contract(self, manager, additional_params=None):
        params = self.contract_params(manager)

        if additional_params is not None:
            params.update(additional_params)

        return self.xmlrpcserver.CreateCommonContract(self.session.oper_id, params)

    def expected_params(self, manager, params):
        if 'start_dt' not in params:
            start_dt = CURRENT_DT
        else:
            start_dt = params['start_dt']

        expected_data = {
            'manager_code': manager.manager_code,
            'dt': start_dt,
            'services': params['services'],
            'signed': False,
            'suspended': False,
            'person_id': params['person_id'],
            'is_offer': 1 if params['is_offer'] else 0,
            'type': 'SPENDABLE',
            'is_cancelled': None,
            'is_faxed': None
        }

        if 'country' in params:
            expected_data.update({'COUNTRY': params['country']})

        if 'region' in params:
            expected_data.update({'REGION': params['region']})

        if 'currency_code' in params:
            expected_data.update({'CURRENCY': params['currency_code']})

        if 'payment_type' in params:
            expected_data.update({'PAYMENT_TYPE': params['payment_type']})

        if 'nds' in params:
            expected_data.update({'NDS': params['nds']})

        return expected_data

    @staticmethod
    def get_contract_attr(obj, attr):
        for o in (obj.current_state(), obj, obj.col0):
            try:
                return getattr(o, attr.lower())
            except AttributeError:
                continue
        return None

    def contract_contains(self, contract, params=None):
        if params is None:
            return True
        for param_key in params:
            try:
                val = self.get_contract_attr(contract, param_key)
            except AttributeError:
                return False
            if val != params[param_key]:
                if isinstance(val, Iterable) and set(val) == set(params[param_key]):
                    continue
                if isinstance(val, ContractTypes) and val.type == params[param_key].type:
                    continue
                return False
        return True

    def check_create_contract_res_params(self, res, manager, params):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert self.contract_contains(contract_obj, self.expected_params(manager, params))


class TestCreateCommonContractForSpendable(ContractUtils):
    # Тест CreateCommonContract для расходных договоров
    @pytest.mark.parametrize('spendable_data, general_data, is_offer',
                             [
                                 (SpendableContractDefaults.TAXI_CORP, GeneralPartnerContractDefaults.YANDEX_TAXI, 0),
                                 (SpendableContractDefaults.TAXI_COROBA, None, 0),
                                 (SpendableContractDefaults.MARKET_MARKETING, None, 1),
                                 (SpendableContractDefaults.ADDAPPTER_2_0, None, 1)
                             ],
                             ids=[
                                 'CreateCommonContract taxi corp',
                                 'CreateCommonContract taxi coroba',
                                 'CreateCommonContract market',
                                 'CreateCommonContract addappter 2 0'
                             ]
                             )
    def test_check_spendable_create_common_contract(self, some_manager, spendable_data, general_data, is_offer):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=spendable_data['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params = {}
        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id,
                                  'is_offer': is_offer},
                                 **spendable_data['CONTRACT_PARAMS'])

        if general_data:
            person_2 = ob.PersonBuilder.construct(
                self.session,
                client_id=person.client.id,
                type=general_data['PERSON_TYPE'],
                **general_data['CONTRACT_PARAMS']
            )

            additional_params_for_general_contract = general_data['CONTRACT_PARAMS'].copy()
            additional_params_for_general_contract.update(dict(client_id=person_2.client.id,
                                                               person_id=person_2.id,
                                                               payment_type='3', payment_term='90'))

            general_contract = self.new_contract(some_manager, additional_params=additional_params_for_general_contract)
            additional_params.update({'link_contract_id': general_contract['ID']})

        res = self.new_contract(some_manager, additional_params=additional_params)

        self.check_create_contract_res_params(res, some_manager, additional_params)

    @pytest.mark.parametrize('spendable_data, general_data, is_offer',
                             [
                                 (SpendableContractDefaults.TAXI_COROBA, None, 0),
                                 (SpendableContractDefaults.MARKET_MARKETING, None, 1),
                                 (SpendableContractDefaults.ADDAPPTER_2_0, None, 1)
                             ],
                             ids=[
                                 'CreateCommonContract taxi coroba',
                                 'CreateCommonContract market',
                                 'CreateCommonContract addappter 2 0'
                             ]
                             )
    def test_payment_type_value_not_in_source(self, some_manager, spendable_data, general_data, is_offer):
        spendable_data = spendable_data.copy()
        bad_payment_type = 66666666
        spendable_data['CONTRACT_PARAMS']['payment_type'] = bad_payment_type
        failure_msg = u"Rule violation: 'Attribute payment_type=%s not found in source'" % (bad_payment_type, )
        with pytest.raises(Exception) as exc:
            self.test_check_spendable_create_common_contract(some_manager, spendable_data, general_data, is_offer)
        assert failure_msg in exc.value.faultString

    # Тест CreateCommonContract для расходных договоров Яндекс.Такси, договор не подписан
    def test_create_common_contract_taxi_donate(self, some_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=SpendableContractDefaults.TAXI_DONATE_RUSSIA['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params = {}
        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id},
                                 **SpendableContractDefaults.TAXI_DONATE_RUSSIA['CONTRACT_PARAMS'])

        obj_builder_2 = ob.PersonBuilder()
        obj_builder_2.prepare(client_id=person.client.id,
                              type=GeneralPartnerContractDefaults.YANDEX_TAXI['PERSON_TYPE'],
                              **GeneralPartnerContractDefaults.YANDEX_TAXI['CONTRACT_PARAMS'])
        person_2 = obj_builder_2.build(self.session).obj

        additional_params_for_general_contract = GeneralPartnerContractDefaults.YANDEX_TAXI['CONTRACT_PARAMS'].copy()
        additional_params_for_general_contract.update(dict(client_id=person_2.client.id,
                                                           person_id=person_2.id,
                                                           payment_type='3', payment_term='90'))

        general_contract = self.new_contract(some_manager, additional_params=additional_params_for_general_contract)
        additional_params.update({'link_contract_id': general_contract['ID']})

        failure_msg = u"Rule violation: 'Договор должен быть подписан'"

        with pytest.raises(Exception) as exc:
            self.new_contract(some_manager, additional_params=additional_params)

        assert failure_msg in exc.value.faultString
