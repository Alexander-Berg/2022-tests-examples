# -*- coding: utf-8 -*-

import pytest
from datetime import datetime, timedelta
from collections import Iterable

from tests import object_builder as ob

from balance import mapper
from balance.constants import *
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

COMMON_PARAMS_GENERAL = {
    'personal_account': 1,
    'services': [ServiceId.GAS_STATION],
    'payment_term': 10,
    'payment_type': 3,
    'signed': 1,
    'start_dt': START_DT_PAST,
}


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class ContractUtils(object):
    def contract_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      person_id=person.id,
                      currency='RUR',
                      manager_uid=manager.domain_passport_id,
                      )
        return params

    def new_contract(self, manager, additional_params=None):
        params = self.contract_params(manager)

        if additional_params is not None:
            params.update(additional_params)

        return self.xmlrpcserver.CreateCommonContract(self.session.oper_id, params)

    def expected_params(self, manager):
        expected_data = {
            'TYPE': 'GENERAL',
            'ACTIVE': True,
            'IS_CANCELLED': None,
            'IS_FAXED': None,
            'SIGNED': 1,
            'SUSPENDED': False,
            'MANAGER_CODE': manager.manager_code,
            'CURRENCY': 810,
            'DT': START_DT_PAST,
            'PAYMENT_TYPE': 3,
            'SERVICES': [ServiceId.GAS_STATION],
            'IS_DEACTIVATED': None
        }
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

    def check_create_contract_res_params(self, res, manager):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert self.contract_contains(contract_obj, self.expected_params(manager))


class TestCreateCommonContractGasStations(ContractUtils):
    # Тест возможности создания договора Яндекс.Заправки
    def test_create_gas_stations_contract_with_gas_stations_firm(self, some_manager):
        additional_params = COMMON_PARAMS_GENERAL
        additional_params.update({'firm_id': FirmId.GAS_STATIONS})

        res = self.new_contract(some_manager, additional_params=additional_params)
        self.check_create_contract_res_params(res, some_manager)

    # Тест невозможности создания договора Яндекс.Заправки от лица другой фирмы
    def test_create_gas_stations_contract_with_wrong_firm(self, some_manager):
        additional_params = COMMON_PARAMS_GENERAL
        additional_params.update({'firm_id': FirmId.YANDEX_OOO})

        failure_msg = u'Договоры с сервисом Яндекс.Заправки могут быть созданы только с фирмой "ООО Яндекс.Заправки"'

        with pytest.raises(Exception) as exc:
            self.new_contract(some_manager, additional_params=additional_params)

        assert failure_msg in exc.value.faultString
