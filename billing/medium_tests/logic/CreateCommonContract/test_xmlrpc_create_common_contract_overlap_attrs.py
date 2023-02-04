# -*- coding: utf-8 -*-

import pytest
from datetime import datetime, timedelta
from decimal import Decimal as D

from tests import object_builder as ob

from balance import mapper
from balance.constants import *


def nullify_time_of_date(date):
    return date.replace(hour=0, minute=0, second=0, microsecond=0) if date else None


NDS_DEFAULT = 18
NDS_UKRAINE = 20
NDS_NOT_RESIDENT = 0
WO_NDS_RECEIPT = -1
NDS_ZERO = 0

CURRENT_DT = nullify_time_of_date(datetime.today())
START_DT_PAST = CURRENT_DT.replace(day=1) + timedelta(days=-200)

CONTRACT_START_DT_1 = START_DT_PAST
CONTRACT_START_DT_2 = CURRENT_DT + timedelta(days=2)
CONTRACT_END_DT_1 = CURRENT_DT + timedelta(days=1)

COMMON_PARAMS_GENERAL = {
    'firm_id': FirmId.GAS_STATIONS,
    'personal_account': 1,
    'services': [ServiceId.GAS_STATION],
    'signed': 1
}
COMMON_PARAMS_SPENDABLE = {
    'firm_id': FirmId.ZEN,
    'nds': NDS_DEFAULT,
    'services': [ServiceId.ZEN],
    'signed': 1
}
COMMON_PARAMS_PARTNER = {
    'country': RegionId.RUSSIA,
    'firm_id': FirmId.YANDEX_OOO,
    'nds': NDS_DEFAULT,
    'services': [ServiceId.RNY],
    'ctype': 'PARTNERS',
    'signed': 1,
    'partners_contract_type': PartnersContractType.OFFER
}
TAXI_CORP = {'services': [ServiceId.TAXI_CORP, ServiceId.TAXI_CORP_PARTNERS],
             'firm_id': FirmId.TAXI,
             'nds': NDS_DEFAULT,
             'country': RegionId.RUSSIA,
             'region': '02000001000',
             'ctype': 'SPENDABLE',
             'type': 'ur'}
YANDEX_TAXI = {'firm_id': FirmId.TAXI,
               'country': RegionId.RUSSIA,
               'region': '77000000000',
               'services': [111, 124, 128, 125, 605, 626],
               'nds_for_receipt': NDS_DEFAULT,
               'personal_account': 1,
               'service_min_cost': D('0'),
               'type': 'ur'
               }

COMMON_PARAMS_CLOUD = COMMON_PARAMS_GENERAL.copy()
COMMON_PARAMS_CLOUD.update(firm_id=123, services=[143])

ERROR_MESSAGE_PATTERN_1 = u'уже существуют договоры с такими типом, фирмой или сервисом:'
ERROR_MESSAGE_PATTERN_2 = u'Договор конфликтует по датам с договорами ID:'


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class ContractUtils(object):
    def contract_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      person_id=person.id,
                      currency='RUR',
                      payment_type=2,
                      manager_uid=manager.domain_passport_id,
                      )
        return params

    def new_contract(self, manager, additional_params=None):
        params = self.contract_params(manager)

        if additional_params is not None:
            params.update(additional_params)

        return self.xmlrpcserver.CreateCommonContract(self.session.oper_id, params)

    def new_collateral(self, contract_id, collateral_type, params):
        return self.xmlrpcserver.CreateCollateral(self.session.oper_id, contract_id, collateral_type, params)

    def check_create_contract_res(self, res):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        assert contract_obj.external_id == res['EXTERNAL_ID']

    def create_cloud_project_uuid(self):
        query = "SELECT S_CLOUD_PROJECT_TEST.nextval id FROM dual"
        project_id = self.session.execute(query).first()['id']
        project_id_str = str(project_id).zfill(32)
        return '{}-{}-{}-{}-{}'.format(project_id_str[:8], project_id_str[8:12], project_id_str[12:16],
                                       project_id_str[16:20], project_id_str[20:])


class TestCreateCommonContractOverlappingAttrs(ContractUtils):
    # Проверка создания договоров с перекрывающимися временными интервалами
    def test_overlapping_contracts(self, some_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type='ph')
        person = obj_builder.build(self.session).obj

        contract_params_common = COMMON_PARAMS_GENERAL.copy()
        contract_params_common['client_id'] = person.client.id
        contract_params_common['person_id'] = person.id

        # договор №1 начинается 2018-08-01, дата окончания открыта
        contract_params_1 = contract_params_common.copy()
        contract_params_1['start_dt'] = CONTRACT_START_DT_1

        # договор №2 начинается 2018-09-01, дата окончания открыта
        # т.е. договоры 1 и 2 пересекаются по сроку действия
        contract_params_2 = contract_params_common.copy()
        contract_params_2['start_dt'] = CONTRACT_START_DT_2

        # создание 1го договора должно быть успешно
        self.new_contract(some_manager, additional_params=contract_params_1)

        # создание второго договора должно вызвать ошибку
        failure_msg = ERROR_MESSAGE_PATTERN_1

        with pytest.raises(Exception) as exc:
            self.new_contract(some_manager, additional_params=contract_params_2)

        assert failure_msg in exc.value.faultString

        # однако если мы изменим сервис, оставив ту же фирму и тип договора, то создание будет успешным
        contract_params_2['services'] = [116]
        res = self.new_contract(some_manager, additional_params=contract_params_2)
        self.check_create_contract_res(res)

    # Проверка создания договоров с неперекрывающимися временными интервалами
    def test_non_overlapping_contracts(self, some_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type='ph')
        person = obj_builder.build(self.session).obj

        contract_params_common = COMMON_PARAMS_GENERAL.copy()
        contract_params_common['client_id'] = person.client.id
        contract_params_common['person_id'] = person.id

        # договор №1 начинается 2018-08-01, дата окончания 2018-08-31
        contract_params_1 = contract_params_common.copy()
        contract_params_1['start_dt'] = CONTRACT_START_DT_1
        contract_params_1['finish_dt'] = CONTRACT_END_DT_1

        # договор №2 начинается 2018-09-01, дата окончания открыта
        # т.е. договоры 1 и 2 пересекаются по сроку действия
        contract_params_2 = contract_params_common.copy()
        contract_params_2['start_dt'] = CONTRACT_START_DT_2

        # оба договора должны быть успешно созданы
        for params in (contract_params_1, contract_params_2):
            res = self.new_contract(some_manager, additional_params=params)
            self.check_create_contract_res(res)

    # Проверка создания расходных договоров с перекрывающимися временными интервалами
    def test_spendable_flow(self, some_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type='ur', is_partner=1)
        person = obj_builder.build(self.session).obj

        contract_params_common = {}
        contract_params_common.update({'person_id': person.id,
                                       'client_id': person.client.id,
                                       'signed': 1},
                                      **TAXI_CORP)

        obj_builder_2 = ob.PersonBuilder()
        obj_builder_2.prepare(client_id=person.client.id, **YANDEX_TAXI)
        person_2 = obj_builder_2.build(self.session).obj

        params_for_general_contract = YANDEX_TAXI.copy()
        params_for_general_contract.update(dict(client_id=person_2.client.id,
                                                person_id=person_2.id,
                                                payment_type='3', payment_term='90',
                                                signed=1))

        general_contract = self.new_contract(some_manager, additional_params=params_for_general_contract)
        contract_params_common.update({'link_contract_id': general_contract['ID']})

        contract_params_1 = contract_params_common.copy()
        contract_params_1['start_dt'] = CONTRACT_START_DT_1

        # т.е. договоры 1 и 2 пересекаются по сроку действия
        contract_params_2 = contract_params_common.copy()
        contract_params_2['start_dt'] = CONTRACT_START_DT_2

        # создание 1го договора должно быть успешно
        res_1 = self.new_contract(some_manager, additional_params=contract_params_1)

        # создание второго договора должно вызвать ошибку
        failure_msg = ERROR_MESSAGE_PATTERN_1

        with pytest.raises(Exception) as exc:
            self.new_contract(some_manager, additional_params=contract_params_2)

        assert failure_msg in exc.value.faultString

        # создаём ДС на расторжение 1го договора (7050 - расторжение SPENDABLE)
        collateral_params = {'end_dt': CONTRACT_END_DT_1,
                             'is_booked': 1
                             }

        self.new_collateral(res_1['ID'], 7050, collateral_params)

        contract_obj = self.session.query(mapper.Contract).get(res_1['ID'])
        contract_obj.collaterals[1].is_signed = CONTRACT_END_DT_1
        self.session.flush()

        res_2 = self.new_contract(some_manager, additional_params=contract_params_2)
        self.check_create_contract_res(res_2)

    # Проверка создания партнерских договоров с перекрывающимися временными интервалами
    def test_partners_flow(self, some_partner_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type='ur', is_partner=1, partner_type=2)
        person = obj_builder.build(self.session).obj

        contract_params_common = COMMON_PARAMS_PARTNER.copy()
        contract_params_common['client_id'] = person.client.id
        contract_params_common['person_id'] = person.id

        contract_params_1 = contract_params_common.copy()
        contract_params_1['start_dt'] = CONTRACT_START_DT_1

        # т.е. договоры 1 и 2 пересекаются по сроку действия
        contract_params_2 = contract_params_common.copy()
        contract_params_2['start_dt'] = CONTRACT_START_DT_2

        # создание 1го договора должно быть успешно
        res_1 = self.new_contract(some_partner_manager, additional_params=contract_params_1)

        # создание второго договора должно вызвать ошибку
        failure_msg = ERROR_MESSAGE_PATTERN_2

        with pytest.raises(Exception) as exc:
            self.new_contract(some_partner_manager, additional_params=contract_params_2)

        assert failure_msg in exc.value.faultString

        collateral_params = {'end_dt': CONTRACT_END_DT_1,
                             'is_booked': 1
                             }

        self.new_collateral(res_1['ID'], 2090, collateral_params)

        contract_obj = self.session.query(mapper.Contract).get(res_1['ID'])
        contract_obj.collaterals[1].is_signed = CONTRACT_END_DT_1
        self.session.flush()

        res_2 = self.new_contract(some_partner_manager, additional_params=contract_params_2)

        self.check_create_contract_res(res_2)

    # Тест создания двух договоров для двух проектов в CLOUD
    def test_cloud_may(self, some_manager):
        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type='ph')
        person = obj_builder.build(self.session).obj

        contract_params_common = COMMON_PARAMS_CLOUD.copy()
        contract_params_common['client_id'] = person.client.id
        contract_params_common['person_id'] = person.id
        contract_params_common['start_dt'] = CONTRACT_START_DT_1

        for i in range(2):
            contract_params = contract_params_common.copy()
            contract_params['projects'] = [self.create_cloud_project_uuid()]
            self.new_contract(some_manager, additional_params=contract_params)
