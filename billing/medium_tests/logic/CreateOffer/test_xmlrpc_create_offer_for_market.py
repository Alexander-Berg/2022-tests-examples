# -*- coding: utf-8 -*-

import pytest
from datetime import datetime, timedelta

from tests import object_builder as ob

from balance import mapper
from balance.constants import *


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class ContractUtils(object):
    CURRENT_DT = datetime.today()
    START_DT_FUTURE = CURRENT_DT + timedelta(days=5)
    START_DT_PAST = CURRENT_DT + timedelta(days=-5)

    def offer_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      currency='RUB',
                      firm_id=FirmId.MARKET,
                      manager_uid=manager.domain_passport_id,
                      person_id=person.id,
                      start_dt=self.START_DT_FUTURE,
                      services={ServiceId.MARKETPLACE_NEW},
                      commission_pct=3
                      )
        return params

    def new_offer(self, manager, additional_params=None):
        params = self.offer_params(manager)
        if additional_params is not None:
            params.update(additional_params)
        return self.xmlrpcserver.CreateOffer(self.session.oper_id, params)

    def check_create_contract_res_ls(self, res, is_ls_expected):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        is_ls_actual = contract_obj.current_state().personal_account
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert is_ls_actual == is_ls_expected

    def check_create_contract_res_client(self, res, person):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).\
                                    filter(mapper.Contract.client_id == person.client.id).first()
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert contract_obj.person == person
        assert contract_obj.current_state().payment_type == PREPAY_PAYMENT_TYPE

    def check_create_contract_res_no_is_offer(self, res):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert not hasattr(contract_obj.col0, 'is_offer')


PAYMENT_TERM = 10


class TestCreateOfferForMarket(ContractUtils):
    # проверка установки признака лицевого счета
    @pytest.mark.parametrize("additional_params, is_ls",
                             [({'payment_type': PREPAY_PAYMENT_TYPE}, None),
                              ({'payment_term': PAYMENT_TERM, 'payment_type': POSTPAY_PAYMENT_TYPE}, None),
                              ({'payment_term': PAYMENT_TERM, 'payment_type': POSTPAY_PAYMENT_TYPE,
                                'personal_account': 0}, 0),
                              ({'payment_term': PAYMENT_TERM, 'payment_type': POSTPAY_PAYMENT_TYPE,
                                'personal_account': 1}, 1)],
                             ids=['prepayment', 'postpayment_without_ls', 'postpayment_ls_0', 'postpayment_ls_1'])
    def test_marketplace_offer(self, some_manager, additional_params, is_ls):
        res = self.new_offer(some_manager, additional_params=additional_params)
        self.check_create_contract_res_ls(res, is_ls)

    # проверка корректности создания клиентского договора
    def test_check_create_offer_market(self, some_manager):
        obj_builder = ob.PersonBuilder()
        person = obj_builder.build(self.session).obj

        additional_params = ({'payment_type': PREPAY_PAYMENT_TYPE,
                              'person_id': person.id,
                              'client_id': person.client.id})

        res = self.new_offer(some_manager, additional_params=additional_params)

        self.check_create_contract_res_client(res, person)

    # проверка корректности работы CreateOffer с базой (исключение лишних полей)
    def test_check_is_offer(self, some_manager):
        additional_params = {'is_offer': 1, 'payment_type': PREPAY_PAYMENT_TYPE}
        res = self.new_offer(some_manager, additional_params=additional_params)
        self.check_create_contract_res_no_is_offer(res)
