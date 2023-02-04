# -*- coding: utf-8 -*-

from datetime import datetime, timedelta
from collections import Iterable
from decimal import Decimal as D

import pytest
# igogor: нет в аркадии BALANCE-34434
from pytest_lazyfixture import lazy_fixture

from balance import mapper
from balance.constants import *
from billing.contract_iface.contract_meta import ContractTypes

from tests import object_builder as ob


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


class GeneralPartnerContractDefaults(object):
    YANDEX_TAXI = {'CONTRACT_PARAMS': {'firm_id': FirmId.TAXI, 'country': RegionId.RUSSIA, 'region': '77000000000',
                                       'currency': 'RUR', 'currency_code': 643,
                                       'services': [111, 124, 128, 125, 605, 626],
                                       'nds_for_receipt': NDS_DEFAULT,
                                       'personal_account': 1, 'service_min_cost': D('0')
                                       }, 'PERSON_TYPE': 'ur'
                   }
    YANDEX_TAXI_BV = {'CONTRACT_PARAMS': {'firm_id': FirmId.TAXI_EU_BV, 'country': RegionId.KAZAKHSTAN,
                                          'currency': 'USD',  'currency_code': 840, 'services': [111, 124, 128],
                                          'personal_account': 1, 'service_min_cost': D('0'),
                                          'partner_commission_pct2': D('0')
                                          }, 'PERSON_TYPE': 'eu_yt'
                      }
    YANDEX_TAXI_UKRAINE = {
        'CONTRACT_PARAMS': {'firm_id': FirmId.TAXI_UA, 'country': RegionId.UKRAINE,
                            'currency': 'UAH',  'currency_code': 980, 'services': [124, 128],
                            'personal_account': 1, 'service_min_cost': D('0'),
                            'partner_commission_pct2': D('0')}, 'PERSON_TYPE': 'ua'
                           }
    TELEMEDICINE = {'CONTRACT_PARAMS': {'firm_id': FirmId.HEALTH_CLINIC,
                                        'currency': 'RUR', 'currency_code': 643,
                                        'services': [270, 170], 'personal_account': 1,
                                        'medicine_pay_commission': D('9.99'), 'medicine_pay_commission2': D('90'),
                                        }, 'PERSON_TYPE': 'ur'
                    }
    BUSES2_0 = {'CONTRACT_PARAMS': {'firm_id': FirmId.BUS,
                                    'currency': 'RUR',  'currency_code': 643,
                                    'services': [205], 'personal_account': 1,
                                    }, 'PERSON_TYPE': 'ur'
                }


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class ContractUtils(object):
    def contract_params(self):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      person_id=person.id,
                      currency='RUR',
                      currency_code=634,
                      payment_term=10,
                      payment_type=3,
                      services=[205]
                      )
        return params

    def new_contract(self, additional_params=None):
        params = self.contract_params()

        if additional_params is not None:
            params.update(additional_params)

        return self.xmlrpcserver.CreateCommonContract(self.session.oper_id, params)

    @staticmethod
    def expected_params(params):
        expected_data = dict(currency=810,
                             payment_term=10,
                             payment_type=3,
                             services=[205]
                             )

        if 'start_dt' not in params:
            start_dt = CURRENT_DT
        else:
            start_dt = params['start_dt']

        expected_data.update({
            'dt': start_dt,
            'person_id': params['person_id'],
            'client_id': params['client_id'],
            'active': True,
            'signed': True,
            'suspended': False,
            'is_cancelled': None,
            'is_faxed': None
        })

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

    def check_create_contract_res_params_manager_code(self, res, params, expected_manager_code):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert contract_obj.manager.manager_code == expected_manager_code
        assert self.contract_contains(contract_obj, self.expected_params(params))


class TestCreateCommonContractManager(ContractUtils):
    # проверяем возможность передавать manager_code и/или manager_uid, manager_code приоритетнее
    # если в t_manager.passport_id пусто, то ищем по t_manager.domain_passport_id
    @pytest.mark.tickets('BALANCE-28643')
    @pytest.mark.parametrize('manager, use_code, use_uid',
                             [
                                 (lazy_fixture('manager_wo_passport'), True, False),
                                 (lazy_fixture('manager_wo_passport'), False, True),
                                 (lazy_fixture('manager_wo_passport'), True, True),
                                 (lazy_fixture('some_manager'), True, False),
                                 (lazy_fixture('some_manager'), False, True),
                             ],
                             ids=[
                                 'CreateCommonContract with manager_code only and manager wo passport_id',
                                 'CreateCommonContract with manager_uid only and manager wo passport_id',
                                 'CreateCommonContract with manager_code and manager_uid',
                                 'CreateCommonContract with manager_code only and manager with passport_id',
                                 'CreateCommonContract with manager_uid only and manager with passport_id',
                             ]
                             )
    def test_check_manager_params(self, manager, use_code, use_uid):
        person = ob.PersonBuilder.construct(self.session, type='ur')

        additional_params = {'person_id': person.id,
                             'client_id': person.client.id,
                             'signed': 1,
                             'is_offer': 0
                             }
        additional_params.update(**GeneralPartnerContractDefaults.BUSES2_0['CONTRACT_PARAMS'])

        if use_code:
            additional_params.update({'manager_code': manager.manager_code})

        if use_uid:
            additional_params.update({'manager_uid': manager.passport_id or manager.domain_passport_id})

        res = self.new_contract(additional_params=additional_params)

        self.check_create_contract_res_params_manager_code(res, additional_params, manager.manager_code)
