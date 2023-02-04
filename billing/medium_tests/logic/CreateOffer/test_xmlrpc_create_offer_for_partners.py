# -*- coding: utf-8 -*-

import pytest
from datetime import datetime, timedelta
from collections import Iterable

from tests import object_builder as ob

from balance import mapper
from billing.contract_iface.contract_meta import ContractTypes
from balance.constants import OebsPayReceiveType, FirmId, PartnersContractType


def nullify_time_of_date(date):
    return date.replace(hour=0, minute=0, second=0, microsecond=0) if date else None


NDS_DEFAULT = 20
NDS_NOT_RESIDENT = 0
WO_NDS_RECEIPT = -1
NDS_ZERO = 0
NDS_RU_DEFAULT = 18

CURRENT_DT = nullify_time_of_date(datetime.today())
START_DT_PAST = CURRENT_DT.replace(day=1) + timedelta(days=-200)


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
@pytest.mark.usefixtures("some_partner_manager")
class ContractUtils(object):
    def partners_offer_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        params = dict(client_id=client.id,
                      person_id=person.id,
                      manager_uid=manager.domain_passport_id,
                      ctype='PARTNERS',
                      firm_id=FirmId.YANDEX_OOO,
                      currency='RUB')
        return params

    def new_partners_offer(self, manager, additional_params=None):
        params = self.partners_offer_params(manager)

        if additional_params is not None:
            params.update(additional_params)

        return self.xmlrpcserver.CreateOffer(self.session.oper_id, params)

    def get_expected_params(self, manager, params):
        currency_num_code = (self.session.query(mapper.Currency).filter_by(iso_code=params.get('currency', 'RUB')).one()
                             .iso_num_code)
        expected_data = {
            'MANAGER_CODE': manager.manager_code,
            'CURRENCY': currency_num_code,
            'DT': params.get('start_dt', CURRENT_DT),
            'CONTRACT_TYPE': PartnersContractType.OFFER,
            'FIRM': params.get('firm_id', FirmId.YANDEX_OOO),
            'PAYMENT_TYPE': 1,
            'PAY_TO': OebsPayReceiveType.BANK,
            'MARKET_API_PCT': 50,
            'TEST_MODE': params.get('test_mode', 0),
            'PARTNER_PCT': 43,
            'REWARD_TYPE': 1,
            'OPEN_DATE': 0,
            'SERVICE_START_DT': params.get('service_start_dt', CURRENT_DT),
            'CTYPE': ContractTypes('PARTNERS'),
            'SUSPENDED': False,
            'PERSON_ID': params['person_id'],
            'CLIENT_ID': params['client_id'],
            'TYPE': 'PARTNERS',
            'IS_CANCELLED': None,
            'IS_FAXED': None,
            'IS_SIGNED': None
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

    def check_contract_contains(self, obj, expected):
        for attr, expect_value in expected.items():
            res_value = self.get_contract_attr(obj, attr)
            if isinstance(res_value, Iterable):
                res_value = set(res_value)
                expect_value = set(expect_value)
            elif isinstance(res_value, ContractTypes):
                res_value = res_value.type
                expect_value = expect_value.type

            assert expect_value == res_value, (attr, expected)


NON_RES_OFFER_RSYA_PARAMS = {
    'firm_id': FirmId.YANDEX_EU_AG,
    'nds': NDS_ZERO,
}

BYU_OFFER_RSYA_PARAMS = {
    'firm_id': FirmId.YANDEX_ADS,
    'nds': NDS_ZERO,
    'currency': 'BYN',
}


NON_RES_OFFER_RSYA_PARAMS_TEST_MODE = {
    'firm_id': FirmId.YANDEX_EU_AG,
    'nds': NDS_ZERO,
    'test_mode': 1,
}


class TestCreateOfferForRSYA(ContractUtils):
    # проверка создания партнерских оферт
    @pytest.mark.parametrize('person_type, additional_params',
                             [
                                 ('ur', {}),
                                 ('sw_yt', NON_RES_OFFER_RSYA_PARAMS),
                                 ('sw_ytph', NON_RES_OFFER_RSYA_PARAMS),
                                 ('sw_ytph', NON_RES_OFFER_RSYA_PARAMS_TEST_MODE),
                                 ('byu', BYU_OFFER_RSYA_PARAMS),
                             ],
                             ids=[
                                 'CreateOffer with mandatory params only with ur',
                                 'RSYA Services AG CreateOffer with sw_ytph',
                                 'RSYA Services AG CreateOffer with sw_yt',
                                 'RSYA Services AG CreateOffer with sw_ytph and test mode',
                                 'RSYA Services AG CreateOffer with byu',
                             ]
    )
    def test_check_create_offer_partners(self, some_partner_manager, person_type, additional_params):
        if 'nds' not in additional_params:
            if person_type == 'ph':
                additional_params.update(dict(nds=NDS_ZERO))
            elif person_type == 'ur':
                additional_params.update(dict(nds=NDS_RU_DEFAULT))
            else:
                additional_params.update(dict(nds=NDS_DEFAULT))

        obj_builder = ob.PersonBuilder()
        obj_builder.prepare(type=person_type, is_partner=1)
        person = obj_builder.build(self.session).obj

        params = dict(person_id=person.id, client_id=person.client.id)
        additional_params.update(params)

        res = self.new_partners_offer(some_partner_manager, additional_params=additional_params)

        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        current_state = contract_obj.current_state()
        assert contract_obj.external_id == res['EXTERNAL_ID']
        check_external_id(contract_obj, current_state, additional_params)
        self.check_contract_contains(contract_obj, self.get_expected_params(some_partner_manager, additional_params))


def check_external_id(contract, current_state, additional_params):
    assert current_state.contract_type == PartnersContractType.OFFER  # проверяется только оферта в самом тесте
    prefix = u'РС'
    firm_id = additional_params.get('firm_id', FirmId.YANDEX_OOO)
    if firm_id != FirmId.YANDEX_OOO:
        prefix = 'YAN' if firm_id != FirmId.YANDEX_ADS else 'BYAN'
    assert contract.external_id.startswith(prefix)
