# -*- coding: utf-8 -*-

import pytest
from datetime import datetime, timedelta

from tests import object_builder as ob

from balance import mapper
from balance.constants import *


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class ContractUtils(object):

    MANAGER_UID = 342884841

    CURRENT_DT = datetime.today()
    START_DT_FUTURE = CURRENT_DT + timedelta(days=5)
    START_DT_PAST = CURRENT_DT + timedelta(days=-5)

    def offer_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      ctype='GENERAL',
                      currency='RUB',
                      firm_id=FirmId.TAXI,
                      manager_uid=manager.domain_passport_id,
                      payment_type=None,
                      payment_term=None,
                      person_id=person.id,
                      personal_account=1,
                      start_dt=self.START_DT_FUTURE,
                      services={ServiceId.TAXI_CORP, ServiceId.TAXI_CORP_CLIENTS},
                      offer_confirmation_type='no'
                      )
        return params

    def new_offer(self, manager, additional_params=None):
        params = self.offer_params(manager)
        if additional_params is not None:
            params.update(additional_params)
        return self.xmlrpcserver.CreateOffer(self.session.oper_id, params)

    def check_create_contract_res_person_payment(self, res, person_expected, payment_expected):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        person_actual = contract_obj.person.id
        payment_actual = contract_obj.current_state().payment_type
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert person_actual == person_expected
        assert payment_actual == payment_expected

    def check_create_contract_res_person_payment_deactivated(self, res, person_expected, payment_expected):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        person_actual = contract_obj.person.id
        payment_actual = contract_obj.current_state().payment_type
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert person_actual == person_expected
        assert payment_actual == payment_expected
        assert contract_obj.current_state().is_deactivated == 1


class TestCreateOfferForCorptaxi(ContractUtils):

    # проверка различных типов оплаты
    @pytest.mark.parametrize("additional_params",
                             [
                                 ({'payment_term': None, 'payment_type': PREPAY_PAYMENT_TYPE}),
                                 ({'payment_term': 10, 'payment_type': POSTPAY_PAYMENT_TYPE})
                             ],
                             ids=
                             [
                                 'Prepay contract',
                                 'Postpay contract'
                             ]
                             )
    def test_check_corptaxi_general_create_offer(self, some_manager, additional_params):
        person = ob.PersonBuilder.construct(self.session, type='ur')

        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id})

        res = self.new_offer(some_manager, additional_params=additional_params)

        self.check_create_contract_res_person_payment(res, person.id, additional_params['payment_type'])

    # проверка различных типов оплаты для неактивного договора
    @pytest.mark.parametrize("additional_params",
                             [
                                 ({'payment_term': None, 'payment_type': PREPAY_PAYMENT_TYPE}),
                                 ({'payment_term': 10, 'payment_type': POSTPAY_PAYMENT_TYPE})
                             ],
                             ids=
                             [
                                 'Prepay contract',
                                 'Postpay contract'
                             ]
                             )
    def test_create_deactivated_offer_for_corptaxi(self, some_manager, additional_params):
        person = ob.PersonBuilder.construct(self.session, type='ur')

        additional_params.update({'person_id': person.id,
                                  'client_id': person.client.id,
                                  'is_deactivated': 1})

        res = self.new_offer(some_manager, additional_params=additional_params)

        self.check_create_contract_res_person_payment_deactivated(res, person.id, additional_params['payment_type'])
