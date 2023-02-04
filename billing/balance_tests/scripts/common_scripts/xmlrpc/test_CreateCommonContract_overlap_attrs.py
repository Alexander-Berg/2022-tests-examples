# -*- coding: utf-8 -*-
from __future__ import unicode_literals

__author__ = 'el-yurchito'

from copy import copy
from datetime import datetime

import hamcrest
import pytest

from balance import balance_steps as steps
from btestlib import reporter, utils
from btestlib.constants import ContractPaymentType, Currencies, Firms, Managers, Nds, PersonTypes, Services
from btestlib.data import defaults
from balance.features import Features


CONTRACT_START_DT_1 = datetime(2018, 8, 1)
CONTRACT_START_DT_2 = datetime(2018, 9, 1)
CONTRACT_END_DT_1 = datetime(2018, 8, 31)

MANAGER = Managers.SOME_MANAGER
PASSPORT_ID = defaults.PASSPORT_UID

COMMON_PARAMS_GENERAL = {
    'currency': Currencies.RUB.char_code,
    'firm_id': Firms.GAS_STATIONS_124.id,
    'manager_uid': MANAGER.uid,
    'personal_account': 1,
    'payment_type': ContractPaymentType.PREPAY,
    'services': [Services.GAS_STATIONS.id],
}
COMMON_PARAMS_SPENDABLE = {
    'currency': Currencies.RUB.char_code,
    'firm_id': Firms.ZEN_28.id,
    'manager_uid': MANAGER.uid,
    'nds': str(Nds.DEFAULT),
    'payment_type': ContractPaymentType.PREPAY,
    'services': [Services.ZEN.id],
}

COMMON_PARAMS_CLOUD = copy(COMMON_PARAMS_GENERAL)
COMMON_PARAMS_CLOUD.update(firm_id=Firms.CLOUD_123.id,
                           # projects=['99999999-9999-9999-9999-999999999999'],
                           services=[Services.CLOUD_143.id])

ERROR_MESSAGE_PATTERN_1 = r'Для клиента с id=(\d+) уже существуют договоры с такими типом, фирмой или сервисом: (\d+)'
ERROR_MESSAGE_PATTERN_2 = r'Договор конфликтует по датам с договорами ID: (\d+)'


@reporter.feature(Features.XMLRPC)
def test_overlapping_contracts():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

    contract_params_common = copy(COMMON_PARAMS_GENERAL)
    contract_params_common['client_id'] = client_id
    contract_params_common['person_id'] = person_id

    # договор №1 начинается 2018-08-01, дата окончания открыта
    contract_params_1 = copy(contract_params_common)
    contract_params_1['start_dt'] = CONTRACT_START_DT_1

    # договор №2 начинается 2018-09-01, дата окончания открыта
    # т.е. договоры 1 и 2 пересекаются по сроку действия
    contract_params_2 = copy(contract_params_common)
    contract_params_2['start_dt'] = CONTRACT_START_DT_2

    # создание 1го договора должно быть успешно
    steps.ContractSteps.create_common_contract(contract_params_1)

    # создание второго договора должно вызвать ошибку
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_common_contract(contract_params_2)
        utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                         hamcrest.matches_regexp(ERROR_MESSAGE_PATTERN_1))

    # однако если мы изменим сервис, оставив ту же фирму и тип договора, то создание будет успешным
    contract_params_2['services'] = [Services.DISK.id]
    steps.ContractSteps.create_common_contract(contract_params_2)


@reporter.feature(Features.XMLRPC)
def test_non_overlapping_contracts():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

    contract_params_common = copy(COMMON_PARAMS_GENERAL)
    contract_params_common['client_id'] = client_id
    contract_params_common['person_id'] = person_id

    # договор №1 начинается 2018-08-01, дата окончания 2018-08-31
    contract_params_1 = copy(contract_params_common)
    contract_params_1['start_dt'] = CONTRACT_START_DT_1
    contract_params_1['finish_dt'] = CONTRACT_END_DT_1

    # договор №2 начинается 2018-09-01, дата окончания открыта
    # т.е. договоры 1 и 2 не пересекаются по сроку действия
    contract_params_2 = copy(contract_params_common)
    contract_params_2['start_dt'] = CONTRACT_START_DT_2

    # оба договора должны быть успешно созданы
    for params in (contract_params_1, contract_params_2):
        steps.ContractSteps.create_common_contract(params)


@reporter.feature(Features.XMLRPC)
def test_spendable_flow():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code, {'is-partner': '1'})

    contract_params_common = copy(COMMON_PARAMS_SPENDABLE)
    contract_params_common['client_id'] = client_id
    contract_params_common['person_id'] = person_id

    # договор №1 начинается 2018-08-01, дата окончания открыта
    contract_params_1 = copy(contract_params_common)
    contract_params_1['start_dt'] = CONTRACT_START_DT_1

    # договор №2 начинается 2018-09-01, дата окончания открыта
    # т.е. договоры 1 и 2 пересекаются по сроку действия
    contract_params_2 = copy(contract_params_common)
    contract_params_2['start_dt'] = CONTRACT_START_DT_2

    # создание 1го договора должно быть успешно
    contract1_id = steps.ContractSteps.create_common_contract(contract_params_1)[0]

    # создание второго договора должно вызвать ошибку
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_common_contract(contract_params_2)
        utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                         hamcrest.matches_regexp(ERROR_MESSAGE_PATTERN_1))

    # создаём ДС на расторжение 1го договора (7050 - расторжение SPENDABLE)
    steps.ContractSteps.create_collateral(7050, {'CONTRACT2_ID': contract1_id,
                                                 'DT': CONTRACT_END_DT_1,
                                                 'END_DT': CONTRACT_END_DT_1,
                                                 'IS_SIGNED': CONTRACT_END_DT_1.isoformat(),
                                                 'IS_BOOKED': CONTRACT_END_DT_1.isoformat()})

    # теперь второй договор должен успешно создаться, т.к. более не пересекается по датам с первым
    steps.ContractSteps.create_common_contract(contract_params_2)


@reporter.feature(Features.XMLRPC)
def test_partners_flow():
    client_id, person_id = steps.PartnerSteps.create_partner_client_person()
    steps.ClientSteps.set_client_partner_type(client_id)

    contract_params_common = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'NDS': str(Nds.DEFAULT),
    }

    # договор №1 начинается 2018-08-01, дата окончания открыта
    contract_params_1 = copy(contract_params_common)
    contract_params_1['DT'] = CONTRACT_START_DT_1
    contract1_id = steps.ContractSteps.create_contract('rsya_aggregator', contract_params_1)[0]

    # договор №2 начинается 2018-09-01, дата окончания открыта
    # т.е. договоры 1 и 2 пересекаются по сроку действия
    contract_params_2 = copy(contract_params_common)
    contract_params_2['DT'] = CONTRACT_START_DT_2

    # создание второго договора должно вызвать ошибку
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_contract('rsya_aggregator', contract_params_2)
        utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                         hamcrest.matches_regexp(ERROR_MESSAGE_PATTERN_2))

    # создаём ДС на расторжение 1го договора (2090 - расторжение PARTNERS)
    steps.ContractSteps.create_collateral(2090, {'CONTRACT2_ID': contract1_id,
                                                 'DT': CONTRACT_END_DT_1,
                                                 'END_DT': CONTRACT_END_DT_1,
                                                 'END_REASON': '1',  # по соглашению сторон
                                                 'IS_SIGNED': CONTRACT_END_DT_1.isoformat(),
                                                 'IS_BOOKED': CONTRACT_END_DT_1.isoformat()})

    # теперь второй договор должен успешно создаться, т.к. более не пересекается по датам с первым
    steps.ContractSteps.create_contract('rsya_aggregator', contract_params_2)


@reporter.feature(Features.XMLRPC)
def test_signed_not_signed():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

    contract_params_common = copy(COMMON_PARAMS_GENERAL)
    contract_params_common['client_id'] = client_id
    contract_params_common['person_id'] = person_id
    contract_params_common['start_dt'] = CONTRACT_START_DT_1

    # создаём 1й договор, он не подписан
    steps.ContractSteps.create_common_contract(contract_params_common)

    # создаём 2й договор (оферта), он будет подписан
    # он не будем пересекаться с первым по атрибутам, т.к. у него другой тип
    steps.ContractSteps.create_offer(contract_params_common)


@reporter.feature(Features.XMLRPC)
def test_cloud_may():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

    contract_params_common = copy(COMMON_PARAMS_CLOUD)
    contract_params_common['client_id'] = client_id
    contract_params_common['person_id'] = person_id
    contract_params_common['start_dt'] = CONTRACT_START_DT_1

    # клауду можно создавать повторяющиеся договоры (с пересекающимися параметрами)
    for i in range(2):
        contract_params = copy(contract_params_common)
        contract_params['projects'] = [steps.PartnerSteps.create_cloud_project_uuid()]
        steps.ContractSteps.create_common_contract(contract_params)
