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

CURRENT_DT = nullify_time_of_date(datetime.today())
START_DT_PAST = CURRENT_DT.replace(day=1) + timedelta(days=-200)


class SpendableContractDefaults(object):
    MARKET_MARKETING = {
        'CONTRACT_PARAMS': {'services': [ServiceId.MARKETPLACE], 'currency': 'RUR', 'currency_code': 643,
                            'firm_id': FirmId.MARKET, 'nds': NDS_DEFAULT},
        'PERSON_TYPE': 'ur'}
    TAXI_COROBA = {
        'CONTRACT_PARAMS': {'services': [ServiceId.MARKETING_COROBA], 'currency': 'RUR', 'currency_code': 643,
                            'firm_id': FirmId.TAXI, 'nds': NDS_DEFAULT, 'payment_sum': 8000,
                            'pay_to': OebsPayReceiveType.TINKOFF},
        'PERSON_TYPE': 'ur'}
    TAXI_DONATE_RUSSIA = {
        'CONTRACT_PARAMS': {'services': [ServiceId.TAXI_PROMO], 'currency': 'RUR', 'currency_code': 643,
                            'firm_id': FirmId.TAXI, 'nds': NDS_DEFAULT,
                            'country': RegionId.RUSSIA, 'region': '02000001000'},
        'PERSON_TYPE': 'ur'}
    TAXI_DONATE_KZT = {'CONTRACT_PARAMS': {'services': [ServiceId.TAXI_PROMO], 'currency': 'USD', 'currency_code': 840,
                                           'firm_id': FirmId.TAXI_EU_BV, 'nds': NDS_NOT_RESIDENT,
                                           'country': RegionId.KAZAKHSTAN},
                       'PERSON_TYPE': 'eu_yt'}
    TAXI_DONATE_UA = {'CONTRACT_PARAMS': {'services': [ServiceId.TAXI_PROMO], 'currency': 'UAH', 'currency_code': 980,
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
                                       'personal_account': 1, 'service_min_cost': D('0'), 'type': 'ur'
                                       }
                   }
    YANDEX_TAXI_BV = {'CONTRACT_PARAMS': {'firm_id': FirmId.TAXI_EU_BV, 'country': RegionId.KAZAKHSTAN,
                                          'currency': 'USD', 'currency_code': 840, 'services': [111, 124, 128],
                                          'personal_account': 1, 'service_min_cost': D('0'),
                                          'partner_commission_pct2': D('0'), 'type': 'eu_yt'
                                          }
                      }
    YANDEX_TAXI_UKRAINE = {
        'CONTRACT_PARAMS': {'firm_id': FirmId.TAXI_UA, 'country': RegionId.UKRAINE,
                            'currency': 'UAH', 'currency_code': 980, 'services': [124, 128],
                            'personal_account': 1, 'service_min_cost': D('0'),
                            'partner_commission_pct2': D('0'), 'type': 'ua'}
    }
    TELEMEDICINE = {'CONTRACT_PARAMS': {'firm_id': FirmId.HEALTH_CLINIC,
                                        'currency': 'RUR', 'currency_code': 643,
                                        'services': [270, 170], 'personal_account': 1,
                                        'medicine_pay_commission': D('0'), 'medicine_pay_commission2': D('0'),
                                        'type': 'ur'
                                        }
                    }


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class ContractUtils(object):
    def offer_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      person_id=person.id,
                      manager_uid=manager.domain_passport_id,
                      signed=1)
        return params

    def new_offer(self, manager, additional_params=None):
        params = self.offer_params(manager)

        if additional_params is not None:
            params.update(additional_params)

        return self.xmlrpcserver.CreateOffer(self.session.oper_id, params)

    def new_contract(self, manager, additional_params=None):
        params = self.offer_params(manager)

        if additional_params is not None:
            params.update(additional_params)

        return self.xmlrpcserver.CreateCommonContract(self.session.oper_id, params)

    def expected_params(self, manager, params):
        if 'start_dt' not in params:
            start_dt = CURRENT_DT
        else:
            start_dt = params['start_dt']

        expected_data = {
            'MANAGER_CODE': manager.manager_code,
            'DT': start_dt,
            'SERVICES': params['services'],
            'active': True,
            'signed': True,
            'suspended': False,
            'person_id': params['person_id'],
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


class TestCreateOfferForSpendable(ContractUtils):
    # Тест расходных договоров для Яндекс.Маркет
    @pytest.mark.parametrize('additional_params',
                             [
                                 ({'nds': NDS_DEFAULT}),
                                 ({'nds': NDS_NOT_RESIDENT}),
                                 ({'start_dt': START_DT_PAST}),
                                 ({'external_id': 'Test_Оферта+' + str(datetime.now())}),
                                 ({'payment_type': 1}),
                                 ({'payment_type': 2})
                             ],
                             ids=[
                                 'CreateOffer market spendable check russian nds',
                                 'CreateOffer market spendable check nds = 0',
                                 'CreateOffer market spendable with start_dt',
                                 'CreateOffer market spendable with special external id',
                                 'CreateOffer market spendable payment_type = 1',
                                 'CreateOffer market spendable payment_type = 2',
                             ]
                             )
    def test_check_spendable_create_offer_market(self, some_manager, additional_params):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=SpendableContractDefaults.MARKET_MARKETING['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id},
                                 **SpendableContractDefaults.MARKET_MARKETING['CONTRACT_PARAMS'])

        res = self.new_offer(some_manager, additional_params=additional_params)

        self.check_create_contract_res_params(res, some_manager, additional_params)

    # Тест расходных договоров для Мотивации продавцов
    @pytest.mark.parametrize('additional_params',
                             [({'is_offer': 1})
                              ],
                             ids=[
                                 'CreateOffer adaptor 2 0 spendable offer = 1'
                             ]
                             )
    def test_check_spendable_create_offer_adaptor_2_0(self, some_manager, additional_params):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=SpendableContractDefaults.ADDAPPTER_2_0['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id},
                                 **SpendableContractDefaults.ADDAPPTER_2_0['CONTRACT_PARAMS'])

        res = self.new_offer(some_manager, additional_params=additional_params)

        additional_params.update({'pay_to': OebsPayReceiveType.YANDEX_MONEY})
        self.check_create_contract_res_params(res, some_manager, additional_params)

    # Тест расходных договоров для Такси (установка коробов)
    @pytest.mark.parametrize('additional_params',
                             [
                                 ({}),
                                 ({'currency': 'USD', 'currency_code': 840}),
                             ],
                             ids=[
                                 'CreateOffer taxi coroba RUB',
                                 'CreateOffer taxi coroba USD',
                             ]
                             )
    def test_check_spendable_create_offer_coroba(self, some_manager, additional_params):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=SpendableContractDefaults.TAXI_COROBA['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id},
                                 **SpendableContractDefaults.TAXI_COROBA['CONTRACT_PARAMS'])

        res = self.new_offer(some_manager, additional_params=additional_params)

        additional_params.update({'pay_to': OebsPayReceiveType.TINKOFF})
        self.check_create_contract_res_params(res, some_manager, additional_params)

    # Тест расходных договоров для Яндекс.Корпоративного такси
    @pytest.mark.parametrize('is_linked_contract_needed',
                             [
                                 (True),
                                 (False),
                             ],
                             ids=[
                                 'CreateOffer taxi corp with link contract id',
                                 'CreateOffer taxi corp wo link contract id',
                             ]
                             )
    def test_check_spendable_create_offer_corp_taxi(self, some_manager, is_linked_contract_needed):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=SpendableContractDefaults.TAXI_CORP['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params = {}
        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id},
                                 **SpendableContractDefaults.TAXI_CORP['CONTRACT_PARAMS'])

        if is_linked_contract_needed:
            obj_builder_2 = ob.PersonBuilder()
            obj_builder_2.prepare(client_id=person.client.id,
                                  **GeneralPartnerContractDefaults.YANDEX_TAXI['CONTRACT_PARAMS'])
            person_2 = obj_builder_2.build(self.session).obj

            additional_params_for_linked_contract = GeneralPartnerContractDefaults.YANDEX_TAXI['CONTRACT_PARAMS'].copy()
            additional_params_for_linked_contract.update(dict(client_id=person_2.client.id,
                                                              person_id=person_2.id,
                                                              payment_type='3', payment_term='90'))

            linked_contract = self.new_contract(some_manager, additional_params=additional_params_for_linked_contract)
            additional_params.update({'link_contract_id': linked_contract['ID']})

        res = self.new_offer(some_manager, additional_params=additional_params)

        self.check_create_contract_res_params(res, some_manager, additional_params)

    # Тест расходных договоров для Яндекс.Такси
    @pytest.mark.parametrize('spendable_data, general_data, is_tinkoff',
                             [
                                 (SpendableContractDefaults.TAXI_DONATE_RUSSIA,
                                  GeneralPartnerContractDefaults.YANDEX_TAXI, 0),
                                 (SpendableContractDefaults.TAXI_DONATE_KZT,
                                  GeneralPartnerContractDefaults.YANDEX_TAXI_BV, 0),
                                 (SpendableContractDefaults.TAXI_DONATE_UA,
                                  GeneralPartnerContractDefaults.YANDEX_TAXI_UKRAINE, 0),
                                 (SpendableContractDefaults.TAXI_DONATE_RUSSIA,
                                  GeneralPartnerContractDefaults.YANDEX_TAXI, 1)
                             ],
                             ids=[
                                 'CreateOffer taxi donate LLC',
                                 'CreateOffer taxi donate TAXI BV',
                                 'CreateOffer taxi donate TAXI UKR',
                                 'CreateOffer taxi donate LLC with pay_to = tinkoff',
                             ]
                             )
    def test_check_spendable_create_offer_taxi_donate(self, some_manager, spendable_data, general_data, is_tinkoff):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=spendable_data['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params = {}
        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id},
                                 **spendable_data['CONTRACT_PARAMS'])

        obj_builder_2 = ob.PersonBuilder()
        obj_builder_2.prepare(client_id=person.client.id, **general_data['CONTRACT_PARAMS'])
        person_2 = obj_builder_2.build(self.session).obj

        additional_params_for_general_contract = general_data['CONTRACT_PARAMS'].copy()
        additional_params_for_general_contract.update(dict(client_id=person_2.client.id,
                                                           person_id=person_2.id,
                                                           payment_type='3', payment_term='90'))

        general_contract = self.new_contract(some_manager, additional_params=additional_params_for_general_contract)
        additional_params.update({'link_contract_id': general_contract['ID']})

        res = self.new_offer(some_manager, additional_params=additional_params)

        additional_params.update({'pay_to': OebsPayReceiveType.TINKOFF if is_tinkoff else 1})
        self.check_create_contract_res_params(res, some_manager, additional_params)

    # Тест расходных договоров для Яндекс.Здоровье
    def test_check_spendable_create_offer_telemedicine_donate(self, some_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=SpendableContractDefaults.TELEMEDICINE_DONATE['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params = {}
        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id},
                                 **SpendableContractDefaults.TELEMEDICINE_DONATE['CONTRACT_PARAMS'])

        obj_builder_2 = ob.PersonBuilder()
        obj_builder_2.prepare(client_id=person.client.id,
                              **GeneralPartnerContractDefaults.TELEMEDICINE['CONTRACT_PARAMS'])
        person_2 = obj_builder_2.build(self.session).obj

        additional_params_for_general_contract = GeneralPartnerContractDefaults.TELEMEDICINE['CONTRACT_PARAMS'].copy()
        additional_params_for_general_contract.update(dict(client_id=person_2.client.id,
                                                           person_id=person_2.id,
                                                           payment_type='3', payment_term='90'))

        general_contract = self.new_contract(some_manager, additional_params=additional_params_for_general_contract)

        additional_params.update({'link_contract_id': general_contract['ID']})

        res = self.new_offer(some_manager, additional_params=additional_params)

        self.check_create_contract_res_params(res, some_manager, additional_params)

    # Тест расходных договоров, ошибочный плательщик
    def test_create_offer_invalid_person(self, some_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=SpendableContractDefaults.MARKET_MARKETING['PERSON_TYPE'], is_partner=0)
        person = obj_builder.build(self.session).obj

        additional_params = {}
        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id},
                                 **SpendableContractDefaults.MARKET_MARKETING['CONTRACT_PARAMS'])

        failure_msg = 'Invalid parameter for function: PERSON. Contract_type SPENDABLE and person.is_partner 0 are incompatible.'

        with pytest.raises(Exception) as exc:
            self.new_offer(some_manager, additional_params=additional_params)

        assert failure_msg in exc.value.faultString

    # Тест расходных договоров, не указан НДС
    def test_create_offer_wo_nds(self, some_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=SpendableContractDefaults.MARKET_MARKETING['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params = {}
        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id},
                                 **SpendableContractDefaults.MARKET_MARKETING['CONTRACT_PARAMS'])
        additional_params.pop('nds')

        failure_msg = 'Invalid parameter for function: NDS. Nds is mandatory for spendable and distribution contracts.'

        with pytest.raises(Exception) as exc:
            self.new_offer(some_manager, additional_params=additional_params)

        assert failure_msg in exc.value.faultString

    # На данный момент создавать договоры со 135 сервисом без 650/651 запрещено, тест не актуален.
    # Когда корп. такси мигрирует полностью на декаплинговые сервисы, можно будет вообще удалить.

    # Тест расходных договоров для Корпоративного такси без указания типа договора
    # def test_create_offer_wo_ctype_for_corp_taxi(self, some_manager):
    #     obj_builder = ob.PersonBuilder()
    #     obj_builder.prepare(type=SpendableContractDefaults.TAXI_CORP['PERSON_TYPE'], is_partner=1)
    #     person = obj_builder.build(self.session).obj
    #
    #     additional_params = {}
    #     additional_params.update({'person_id': person.id,
    #                               'client_id': person.client.id},
    #                              **SpendableContractDefaults.TAXI_CORP['CONTRACT_PARAMS'])
    #     additional_params.pop('ctype')
    #
    #     failure_msg = 'Invalid parameter for function: CTYPE. Required for 135 service'
    #
    #     with pytest.raises(Exception) as exc:
    #         self.new_offer(some_manager, additional_params=additional_params)
    #
    #     assert failure_msg in exc.value.faultString

    # Тест расходных договоров для Такси (установка коробов) с указанием типа договора
    def test_create_offer_with_ctype_for_coroba(self, some_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=SpendableContractDefaults.TAXI_COROBA['PERSON_TYPE'], is_partner=1)
        person = obj_builder.build(self.session).obj

        additional_params = {}
        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id,
                                  'ctype': 'SPENDABLE'},
                                 **SpendableContractDefaults.TAXI_COROBA['CONTRACT_PARAMS'])

        failure_msg = 'Invalid parameter for function: CTYPE. Allowed only for 135 service'

        with pytest.raises(Exception) as exc:
            self.new_offer(some_manager, additional_params=additional_params)

        assert failure_msg in exc.value.faultString
