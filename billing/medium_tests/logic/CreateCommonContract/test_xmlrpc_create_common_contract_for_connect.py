# -*- coding: utf-8 -*-

import pytest
from datetime import datetime, timedelta
from collections import Iterable

from tests import object_builder as ob

from balance import mapper
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
                      payment_type=3,
                      services=[202]
                      )
        return params

    def new_contract(self, manager, additional_params=None):
        params = self.contract_params(manager)

        if additional_params is not None:
            params.update(additional_params)

        return self.xmlrpcserver.CreateCommonContract(self.session.oper_id, params)

    def expected_params(self, manager, params):
        expected_data = dict(currency=810,
                             payment_term=10,
                             payment_type=3,
                             services=[202]
                             )

        if 'start_dt' not in params:
            start_dt = CURRENT_DT
        else:
            start_dt = params['start_dt']

        expected_data.update({
            'manager_code': manager.manager_code,
            'dt': start_dt,
            'person_id': params['person_id'],
            'client_id': params['client_id'],
            'active': False,
            'signed': False,
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

    def check_create_contract_res_params(self, res, manager, params):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert self.contract_contains(contract_obj, self.expected_params(manager, params))


class TestCreateCommonContractForConnect(ContractUtils):
    # Тест расходных договоров для Яндекс.Маркет
    @pytest.mark.parametrize('additional_params',
                             [
                                 ({}),
                                 ({'bank_details_id': BANK_ID}),
                             ],
                             ids=[
                                 'CreateCommonContract with Connect',
                                 'CreateCommonContract with Connect and bank_details_id',
                             ]
                             )
    def test_check_connect_create_offer(self, some_manager, additional_params):
        person = ob.PersonBuilder.construct(self.session, type='ur')

        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id,
                                  'is_offer': 1
                                  })

        res = self.new_contract(some_manager, additional_params=additional_params)

        self.check_create_contract_res_params(res, some_manager, additional_params)
