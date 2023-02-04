# -*- coding: utf-8 -*-
import pytest
import datetime

from balance import mapper
from balance.paystep import PaystepNS
from billing.contract_iface import ContractTypeId
from balance.constants import (
    FirmId,
    ServiceId,
    SPECIAL_PROJECTS_COMMISSION_TYPE,
    DIRECT_MEDIA_COMMISSION_TYPE,
    DIRECT_PRODUCT_RUB_ID,
    SPECIAL_PROJECTS_MARKET_COMMISSION_TYPE,
    POSTPAY_PAYMENT_TYPE,
    PREPAY_PAYMENT_TYPE,
    NUM_CODE_RUR,
)

from tests import object_builder as ob
from tests.balance_tests.paystep.paystep_common import (
    create_passport,
    create_manager,
    create_request,
    create_client,
    create_firm,
    create_invoice,
    create_client,
    create_person,
    create_contract,
    create_product,
    create_order,
    create_service
)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]

NOW = datetime.datetime.now()


class TestContractlessPersons(object):

    def test_wo_persons(self, session):
        """существующие плательщики, которыз можно использовать при выставлении без договора"""
        request = create_request(session)
        ns = PaystepNS(request=request)
        assert ns.contractless_persons == set()

    def test_w_person(self, session):
        """существующие плательщики, которых можно использовать при выставлении без договора"""
        request = create_request(session)
        ns = PaystepNS(request=request)
        person = create_person(session, client=request.client)
        assert ns.contractless_persons == {person}

    @pytest.mark.parametrize(
        'payment_type, personal_account, personal_account_fictive, is_ok',
        [
            pytest.param(PREPAY_PAYMENT_TYPE, 0, 0, False, id='prepay'),
            pytest.param(POSTPAY_PAYMENT_TYPE, 0, 0, False, id='postpay_old'),
            pytest.param(POSTPAY_PAYMENT_TYPE, 1, 0, True, id='postpay_pa'),
            pytest.param(POSTPAY_PAYMENT_TYPE, 1, 1, True, id='postpay_fpa'),
        ]
    )
    def test_w_contract(self, session, client, payment_type, personal_account, personal_account_fictive, is_ok):
        request = create_request(session, client=client)
        ro, = request.rows

        person = create_person(session, client=client)
        create_contract(
            session,
            client=client,
            person=person,
            commission=ContractTypeId.NON_AGENCY,
            firm=FirmId.YANDEX_OOO,
            payment_type=payment_type,
            personal_account=personal_account,
            personal_account_fictive=personal_account_fictive,
            services={ro.order.service_id},
            is_signed=NOW,
            currency=NUM_CODE_RUR,
        )

        ns = PaystepNS(request)
        if is_ok:
            assert ns.contractless_persons == {person}
        else:
            assert ns.contractless_persons == set()

    def test_w_prepay_contract_other_person(self, session, client):
        request = create_request(session, client=client)
        ro, = request.rows

        person = create_person(session, client=client)
        create_contract(
            session,
            client=client,
            person=create_person(session, client=client),
            commission=ContractTypeId.NON_AGENCY,
            firm=FirmId.YANDEX_OOO,
            payment_type=PREPAY_PAYMENT_TYPE,
            services={ro.order.service_id},
            is_signed=NOW,
            currency=NUM_CODE_RUR,
        )

        ns = PaystepNS(request)
        assert ns.contractless_persons == {person}

    @pytest.mark.single_account
    def test_w_prepay_contract_single_account(self, session):
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = [ServiceId.DIRECT]
        client = create_client(session, single_account_number=ob.get_big_number())
        service = ob.Getter(mapper.Service, ServiceId.DIRECT).build(session).obj
        product = ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID).build(session).obj
        order = create_order(session, client, product, service)
        request = create_request(session, client=client, orders=[order])

        person = create_person(session, client=client)
        create_contract(
            session,
            client=client,
            person=person,
            commission=ContractTypeId.NON_AGENCY,
            firm=FirmId.YANDEX_OOO,
            payment_type=PREPAY_PAYMENT_TYPE,
            services={ServiceId.DIRECT},
            is_signed=NOW,
            currency=NUM_CODE_RUR,
        )

        ns = PaystepNS(request)
        assert ns.contractless_persons == {person}


@pytest.mark.parametrize('non_resident_currency_payment, is_client_subclient_non_resident',
                         [('RUR', True), (None, False)])
def test_ns_subclient_non_resident(session, non_resident_currency_payment, is_client_subclient_non_resident):
    request = create_request(session)
    request.client.fullname = 'client_fullname'
    request.client.non_resident_currency_payment = non_resident_currency_payment
    ns = PaystepNS(request=request)
    person = create_person(session, client=request.client)
    assert ns.subclient_non_resident is is_client_subclient_non_resident


def test_ns_need_contract_force_contract(session):
    """если договор передан явно, выставляться можно только по нему"""
    request = create_request(session)
    contract = create_invoice(session, client=request.client)
    ns = PaystepNS(request=request, contract=contract)
    assert ns.need_contract is True


def test_ns_need_contract_subclient_non_resident(session):
    """если в реквесте указан субклиент нерезидент, выставляться можно только по договору"""
    request = create_request(session)
    request.client.fullname = 'client_fullname'
    request.client.non_resident_currency_payment = 'RUR'
    ns = PaystepNS(request=request, contract=None)
    assert ns.need_contract is True


@pytest.mark.parametrize('wholesale_agent_premium_awards_scale_type, product_commission_types, is_contract_match',
                         [(3,
                           (SPECIAL_PROJECTS_COMMISSION_TYPE,
                            SPECIAL_PROJECTS_COMMISSION_TYPE),
                           True),

                          (3,
                           (SPECIAL_PROJECTS_COMMISSION_TYPE,
                            DIRECT_MEDIA_COMMISSION_TYPE),
                           False),

                          (14,
                           (SPECIAL_PROJECTS_MARKET_COMMISSION_TYPE,
                            SPECIAL_PROJECTS_MARKET_COMMISSION_TYPE),
                           True),

                          (14,
                           (SPECIAL_PROJECTS_MARKET_COMMISSION_TYPE,
                            DIRECT_MEDIA_COMMISSION_TYPE),
                           False),

                          (15,
                           (SPECIAL_PROJECTS_MARKET_COMMISSION_TYPE,
                            DIRECT_MEDIA_COMMISSION_TYPE),
                           False),

                          (15,
                           (SPECIAL_PROJECTS_COMMISSION_TYPE,
                            DIRECT_MEDIA_COMMISSION_TYPE),
                           False),

                          (15,
                           (DIRECT_MEDIA_COMMISSION_TYPE,
                            DIRECT_MEDIA_COMMISSION_TYPE),
                           True),
                          ])
@pytest.mark.parametrize('contract_commission',
                         [ContractTypeId.WHOLESALE_AGENCY_AWARD,
                          ContractTypeId.YANDEX_ADS_WHOLESALE_AGENCY_AWARD])
def test_products_commissions_w_contract(session, contract_commission, client, service, product_commission_types,
                                         is_contract_match, wholesale_agent_premium_awards_scale_type):
    orders = []
    for commission_type in product_commission_types:
        product = create_product(session, commission_type=commission_type)
        orders.append(create_order(session, client, product, service))
    request = create_request(session, client=client, orders=orders)
    person = create_person(session, client=request.client)
    contract = create_contract(session, is_signed=NOW, dt=NOW, commission=contract_commission, person=person,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype='GENERAL',
                               wholesale_agent_premium_awards_scale_type=wholesale_agent_premium_awards_scale_type)
    ns = PaystepNS(request=request, contract=contract)
    if is_contract_match:
        assert ns.matching_contracts == {contract}
    else:
        assert ns.matching_contracts == set()
