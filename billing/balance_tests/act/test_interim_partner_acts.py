import datetime

import pytest

import tests.object_builder as ob
from balance.constants import ServiceId
from balance.reverse_partners import InterimPartnerActGenerator
from butils import exc

NOW = datetime.datetime.now()
TWO_MONTHS_AGO = NOW - datetime.timedelta(days=60)
APIKEYS_SERVICES = [129]


@pytest.fixture()
def contract(session):
    person_b = ob.PersonBuilder(type='ur')
    return ob.ContractBuilder.construct(session, services=APIKEYS_SERVICES, person=person_b, currency=810)


@pytest.fixture()
def act_generator(session):
    return InterimPartnerActGenerator(session)


class TestGeneratePartnerAct:
    def test_error_if_contract_not_found(self, contract, act_generator):
        with pytest.raises(exc.INVALID_PARAM, match='Contract not found'):
            INVALID_CONTRACT_ID = -1
            act_generator.generate_acts(contract_id=INVALID_CONTRACT_ID, service_id=ServiceId.APIKEYS, act_dt=NOW)

    def test_error_if_order_not_found(self, session, contract, act_generator):
        with pytest.raises(exc.INVALID_PARAM, match='Order not found'):
            INVALID_ORDER_ID = -1
            act_generator.generate_acts(contract_id=contract.id, service_id=ServiceId.APIKEYS, act_dt=NOW,
                                        service_orders_ids=[INVALID_ORDER_ID])

    def test_contract_without_orders_return_no_acts(self, session, contract, act_generator):
        result = act_generator.generate_acts(contract_id=contract.id, service_id=ServiceId.APIKEYS, act_dt=NOW,
                                             service_orders_ids=None)
        assert result == []
