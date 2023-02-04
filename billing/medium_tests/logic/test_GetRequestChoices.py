# -*- coding: utf-8 -*-

import xmlrpclib
import datetime

import hamcrest
import pytest

from balance import exc, mapper
from billing.contract_iface import ContractTypeId
from balance.constants import (PaymentMethodIDs, FirmId, RegionId,
                               PREPAY_PAYMENT_TYPE, POSTPAY_PAYMENT_TYPE,
                               DIRECT_SERVICE_ID, PersonCategoryCodes,
                               ServiceId, DIRECT_PRODUCT_RUB_ID)

import tests.tutils as tut
import tests.object_builder as ob
from tests.balance_tests.paystep.paystep_common import (create_product, create_client,
                                                        create_person, create_contract,
                                                        create_request, create_firm,
                                                        create_pay_policy, create_paysys,
                                                        create_order, create_currency,
                                                        create_price_tax_rate, create_person_category,
                                                        not_existing_contract_id)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]

NOW = datetime.datetime.now()
TOMORROW = NOW + datetime.timedelta(days=1)
YESTERDAY = NOW - datetime.timedelta(days=1)

ID_TYPE_TO_PARAM = {'ContractID': 'id',
                    'ContractExternalID': 'external_id'}


def test_base(session, xmlrpcserver):
    client = create_client(session)
    product = create_product(session, price=100, create_price=True, create_taxes=True)
    service = ob.Getter(mapper.Service, 7).build(session).obj
    order = create_order(session, client=client, product=product, service=service)

    request = create_request(session, orders=[order])

    res = xmlrpcserver.GetRequestChoices(
        {'OperatorUid': session.oper_id, 'RequestID': request.id})
    assert len(res['pcp_list']) > 0


def test_w_contract(session, client, currency, xmlrpcserver):
    firm = create_firm(session, default_currency=currency.char_code)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    person = create_person(session, type=person_category.category, client=client)
    request = create_request(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, is_signed=NOW, dt=NOW,
                               services={request.request_orders[0].order.service.id},
                               client=client, person=person,
                               firm=firm.id,
                               currency=currency.num_code,
                               payment_type=POSTPAY_PAYMENT_TYPE, personal_account=1, credit_limit_single=100)
    create_pay_policy(session, firm_id=contract.firm.id, legal_entity=0,
                      paymethods_params=[(currency.char_code, PaymentMethodIDs.bank)],
                      region_id=contract.firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)
    create_price_tax_rate(session, request.request_orders[0].order.product, contract.firm.country, currency)
    create_paysys(session, firm=contract.firm, iso_currency=currency.iso_code,
                  category=person.person_category.category, currency=currency.char_code,
                  group_id=0, payment_method_id=PaymentMethodIDs.bank, extern=1)

    res = xmlrpcserver.GetRequestChoices({'OperatorUid': session.oper_id,
                                          'RequestID': request.id,
                                          'ContractID': contract.id})
    assert 'credits' in res


@pytest.mark.parametrize('fixed_currency', [0, 1])
@pytest.mark.parametrize('is_agency_allowed', [0, 1])
def test_w_overdraft(session, currency, client, xmlrpcserver, fixed_currency, is_agency_allowed):
    client.is_agency = is_agency_allowed
    service = ob.ServiceBuilder.construct(session, id=ob.generate_int(4))
    request = create_request(session, client=client, service=service)
    firm = create_firm(session)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    person = create_person(session, type=person_category.category, client=client)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0,
                      paymethods_params=[(currency.char_code, PaymentMethodIDs.bank)],
                      region_id=firm.country.region_id, service_id=service.id,
                      is_agency=is_agency_allowed)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                  category=person.person_category.category, currency=currency.char_code,
                  group_id=0, payment_method_id=PaymentMethodIDs.bank, extern=1)

    try:
        params = session.query(mapper.ServiceFirmOverdraftParams).getone(service_id=service.id, firm_id=firm.id)
        params.start_dt = params.end_dt = None
        params.fixed_currency = fixed_currency
        params.is_agency_allowed = is_agency_allowed
    except exc.NOT_FOUND:
        params = mapper.ServiceFirmOverdraftParams(service_id=service.id, firm_id=firm.id, payment_term_id=15, use_working_cal=0,
                                                   fixed_currency=fixed_currency, thresholds={currency.iso_code: 1000}, turnover_firms=[],
                                                   is_agency_allowed=is_agency_allowed, only_external=0)
        session.add(params)

    session.flush()

    client.set_overdraft_limit(service.id, firm.id, 1000000, currency.iso_code if fixed_currency else None)

    session.flush()

    res = xmlrpcserver.GetRequestChoices(
        {'OperatorUid': session.oper_id,
         'RequestID': request.id,
         'PersonID': person.id})
    assert 'overdrafts' in res and 'is_available' in res['overdrafts']


def test_common_attributes(session, xmlrpcserver):
    client = create_client(session)
    order = create_order(session, client=client, service=ob.Getter(mapper.Service, 7),
                         product=ob.Getter(mapper.Product, 503162))
    request = create_request(session, client=client, orders=[order])
    res = xmlrpcserver.GetRequestChoices({
        'OperatorUid': session.oper_id,
        'RequestID': request.id
    })

    attrs = sorted(['paysys_list', 'pcp_list', 'persons_parent', 'request'])
    paysys_attrs = sorted(('cc', 'id', 'name'))
    pcp_attrs = sorted(('contract', 'paysyses', 'person'))
    pcp_person_attr = sorted(('id', 'name', 'type', 'legal_entity', 'region_id', 'resident'))
    pcp_paysyses_attrs = sorted(['cc', 'id', 'name', 'payment_method', 'region_id', 'resident', 'currency',
                                 'payment_method_code', 'legal_entity', 'disabled_reasons',
                                 'payment_limit', 'paysys_group'])
    persons_attrs = sorted(('agency_id', 'class_id', 'id', 'is_agency', 'name'))
    request_attrs = sorted(('client_id', 'id'))

    assert attrs == sorted(res.keys())
    assert paysys_attrs == sorted(res['paysys_list'][0].keys())
    assert pcp_person_attr == sorted(res['pcp_list'][0]['person'].keys())
    assert pcp_paysyses_attrs == sorted(res['pcp_list'][0]['paysyses'][0].keys())
    assert pcp_attrs == sorted(res['pcp_list'][0].keys())
    assert persons_attrs == sorted(res['persons_parent'].keys())
    assert request_attrs == sorted(res['request'].keys())


@pytest.mark.parametrize('skip_trust', [None, False, True])
def test_SkipTrust(session, xmlrpcserver, skip_trust):
    client = create_client(session)
    product = create_product(session, price=100, create_price=True, create_taxes=True)
    order = create_order(session, client=client, service=ob.Getter(mapper.Service, 7),
                         product=product)
    request = create_request(session, client=client, orders=[order])
    session.flush()

    args = {
        'OperatorUid': session.oper_id,
        'RequestID': request.id,
    }
    if skip_trust is not None:
        args['SkipTrust'] = skip_trust
    res = xmlrpcserver.GetRequestChoices(args)

    paysys_groups = {ps['paysys_group'] for pcp in res['pcp_list'] for ps in pcp['paysyses']}
    if skip_trust:
        assert paysys_groups == {'default'}
    else:
        assert paysys_groups == {'default', 'trust'}


@pytest.mark.parametrize('id_type', ['ContractID', 'ContractExternalID'])
@pytest.mark.parametrize('contract_params', [{},
                                             {'is_suspended': tut.shift_date(days=-3), 'is_deactivated': 1}],
                         ids=lambda param: str(param.keys()))
def test_active_contract(session, xmlrpcserver, id_type, contract_params):
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=FirmId.YANDEX_OOO,
                               client=create_client(session), payment_type=PREPAY_PAYMENT_TYPE,
                               services={DIRECT_SERVICE_ID}, is_signed=NOW, person=create_person(session),
                               **contract_params)
    contract.external_id = contract.create_new_eid()
    order = create_order(session, client=contract.client, service=ob.Getter(mapper.Service, 7),
                         product=create_product(session, price=100, create_price=True, create_taxes=True))
    request = create_request(session, contract.client, orders=[order])
    result = xmlrpcserver.GetRequestChoices({'OperatorUid': session.oper_id,
                                             'RequestID': request.id,
                                             id_type: getattr(contract, ID_TYPE_TO_PARAM[id_type]),
                                             })

    assert 'pcp_list' in result
    assert 1 == len(result['pcp_list'])
    assert 'contract' in result['pcp_list'][0]
    assert {'id': contract.id, 'external_id': contract.external_id, 'client_id': contract.client.id,
            'person_id': contract.person.id} == result['pcp_list'][0]['contract']


@pytest.mark.parametrize('extra_contract_params', [{'dt': TOMORROW},
                                                   {'finish_dt': YESTERDAY},
                                                   {'is_signed': None},
                                                   {'is_cancelled': YESTERDAY},
                                                   {'is_suspended': YESTERDAY}],
                         ids=lambda param: str(param.keys()))
def test_not_active_contract(session, xmlrpcserver, extra_contract_params):
    base_params = dict(commission=ContractTypeId.NON_AGENCY, firm=FirmId.YANDEX_OOO,
                       payment_type=PREPAY_PAYMENT_TYPE, services={DIRECT_SERVICE_ID},
                       is_signed=NOW, client=create_client(session), person=create_person(session))
    contract_params = base_params.copy()
    contract_params.update(extra_contract_params)
    contract = create_contract(session, **contract_params)
    contract.external_id = contract.create_new_eid()
    order = create_order(session, client=contract.client, service=ob.Getter(mapper.Service, 7),
                         product=create_product(session, price=100, create_price=True, create_taxes=True))
    request = create_request(session, contract.client, orders=[order])
    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.GetRequestChoices({'OperatorUid': session.oper_id,
                                        'RequestID': request.id,
                                        'ContractID': contract.id})
    expected_msg = "Request {} does not match contract {}".format(request.id, contract.external_id)
    assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')


@pytest.mark.parametrize('id_type', ['ContractID', 'ContractExternalID'])
def test_not_found_contract(session, xmlrpcserver, id_type):
    request = create_request(session)
    contract_id = not_existing_contract_id(session)
    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.GetRequestChoices({'OperatorUid': session.oper_id,
                                        'RequestID': request.id,
                                        id_type: contract_id})
    expected_msg = "Contract {}={} not found".format(ID_TYPE_TO_PARAM[id_type], contract_id)
    assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')


@pytest.mark.parametrize('id_type', ['ContractID', 'ContractExternalID'])
@pytest.mark.parametrize('extra_contract_params', [{'dt': TOMORROW},
                                                   {'finish_dt': YESTERDAY},
                                                   {'is_signed': None},
                                                   {'is_cancelled': YESTERDAY},
                                                   {'is_suspended': YESTERDAY}],
                         ids=lambda param: str(param.keys()))
def test_not_unique_external_id_one_active(session, xmlrpcserver, id_type, extra_contract_params):
    base_params = dict(commission=ContractTypeId.NON_AGENCY, firm=FirmId.YANDEX_OOO,
                       client=create_client(session), payment_type=PREPAY_PAYMENT_TYPE,
                       services={DIRECT_SERVICE_ID}, is_signed=NOW, person=create_person(session),
                       external_id='test %s' % ob.get_big_number())
    contract1 = create_contract(session, **base_params)
    contract_params = base_params.copy()
    contract_params.update(extra_contract_params)
    contract2 = create_contract(session, **contract_params)
    contract2.external_id = contract1.external_id
    order = create_order(session, client=contract1.client, service=ob.Getter(mapper.Service, 7),
                         product=create_product(session, price=100, create_price=True, create_taxes=True))
    request = create_request(session, contract1.client, orders=[order])
    result = xmlrpcserver.GetRequestChoices({'OperatorUid': session.oper_id,
                                             'RequestID': request.id,
                                             'ContractExternalID': contract1.external_id})

    assert 'pcp_list' in result
    assert 1 == len(result['pcp_list'])
    assert 'contract' in result['pcp_list'][0]
    assert {'id': contract1.id, 'external_id': contract1.external_id, 'client_id': contract1.client.id,
            'person_id': contract1.person.id} == result['pcp_list'][0]['contract']


def test_not_unique_external_id_two_active(session, xmlrpcserver):
    contracts = [create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=FirmId.YANDEX_OOO,
                                 client=create_client(session), payment_type=PREPAY_PAYMENT_TYPE,
                                 services={DIRECT_SERVICE_ID}, is_signed=NOW, person=create_person(session)) for _ in
                 range(2)]
    contracts[1].external_id = contracts[0].external_id
    order = create_order(session, client=contracts[0].client, service=ob.Getter(mapper.Service, 7),
                         product=create_product(session, price=100, create_price=True, create_taxes=True))
    request = create_request(session, contracts[0].client, orders=[order])

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.GetRequestChoices({'OperatorUid': session.oper_id,
                                        'RequestID': request.id,
                                        'ContractExternalID': contracts[0].external_id})

    expected_msg = "Object is not unique: Contract: filter: {{'external_id': u'{}'}}".format(contracts[0].external_id)
    assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')


@pytest.mark.parametrize('id_type', ['ContractID', 'ContractExternalID'])
def test_not_unique_external_id_two_not_active(session, xmlrpcserver, id_type):
    contracts = [create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=FirmId.YANDEX_OOO,
                                 client=create_client(session), payment_type=PREPAY_PAYMENT_TYPE,
                                 services={DIRECT_SERVICE_ID}, is_signed=None, person=create_person(session)) for _ in
                 range(2)]
    contracts[1].external_id = contracts[0].external_id
    order = create_order(session, client=contracts[0].client, service=ob.Getter(mapper.Service, 7),
                         product=create_product(session, price=100, create_price=True, create_taxes=True))
    request = create_request(session, contracts[0].client, orders=[order])
    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.GetRequestChoices({'OperatorUid': session.oper_id,
                                        'RequestID': request.id,
                                        id_type: getattr(contracts[0], ID_TYPE_TO_PARAM[id_type]),
                                        })
    if id_type == 'ContractExternalID':
        expected_msg = "Active contract with external_id={} not found".format(contracts[0].external_id)
    else:
        expected_msg = "Request {} does not match contract {}".format(request.id, contracts[0].external_id)

    assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')


def test_preview_case(session, xmlrpcserver):
    # если передать сразу параметры для оплаты, то зайдем в preview,
    # чтобы сразу посчитать параметры для кредита и овердрафта
    client = create_client(session)
    order = create_order(
        session,
        client=client,
        service=ob.Getter(mapper.Service, 7),
        product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID),
    )
    request = create_request(session, client, orders=[order])
    person = create_person(session, client=client, type='ph')
    res = xmlrpcserver.GetRequestChoices(
        {
            'OperatorUid': session.oper_id,
            'RequestID': request.id,
            'PersonID': person.id,
            'PaysysID': 1002,  # физик, резидент, картой
        },
    )
    hamcrest.assert_that(
        res,
        hamcrest.has_entries({'credits': hamcrest.has_entries({
            'available_sum': '0',
            'is_available': False,
            'is_present': False,
            'overdraft_sum': '0',
        }),
            'overdrafts': hamcrest.has_entries({
                'available_sum': '0',
                'available_sum_ue': '0',
                'is_available': True,
                'is_present': False,
                'iso_currency': 'RUB',
            }),
            'paysys_list': hamcrest.contains(
                hamcrest.has_entries({
                    'cc': 'as',
                    'id': 1002,
                    'name': u'Кредитной картой',
                }),
            ),
            'pcp_list': hamcrest.contains(
                hamcrest.has_entries({'contract': None,
                                      'paysyses': hamcrest.contains(
                                          hamcrest.has_entries({
                                              'cc': 'as',
                                              'currency': 'RUB',
                                              'disabled_reasons': hamcrest.empty(),
                                              'id': 1002,
                                              'legal_entity': 0,
                                              'name': u'Кредитной картой',
                                              'payment_limit': '49999.99',
                                              'payment_method': hamcrest.has_entries({
                                                  'cc': 'card',
                                                  'id': 1101,
                                                  'name': 'Credit Card',
                                              }),
                                              'payment_method_code': 'card',
                                              'paysys_group': 'default',
                                              'region_id': 225,
                                              'resident': 1}),
                                      ),
                                      'person': hamcrest.has_entries({
                                          'id': person.id,
                                          'legal_entity': 0,
                                          'name': None,
                                          'region_id': 225,
                                          'resident': 1,
                                          'type': 'ph',
                                      }),
                                      }),
            ),
            'persons_parent': hamcrest.has_entries({
                'agency_id': None,
                'class_id': client.class_id,
                'id': client.id,
                'is_agency': 0,
                'name': 'test',
            }),
            'request': hamcrest.has_entries({
                'client_id': client.id,
                'id': str(request.id),
            }),
        }),
    )


@pytest.mark.single_account
class TestWithSingleAccount(object):
    # noinspection PyMethodMayBeStatic
    def get_creatable_person_categories(self, client, session, xmlrpcserver):
        order = ob.OrderBuilder(service_id=ServiceId.DIRECT, client=client,
                                product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID))

        basket = ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(order=order, quantity=1)]
        )

        request = ob.RequestBuilder(basket=basket).build(session).obj

        answer = xmlrpcserver.GetRequestChoices({'OperatorUid': session.oper_id, 'RequestID': request.id})
        return {
            x['person']['type']
            for x in answer['pcp_list']
            if x['person']['id'] is None
        }

    def test_denied(self, session, xmlrpcserver):
        client = create_client(session, with_single_account=True, region_id=RegionId.RUSSIA)
        ob.PersonBuilder(type=PersonCategoryCodes.russia_resident_individual, client=client).build(session)
        creatable_person_categories = self.get_creatable_person_categories(client, session, xmlrpcserver)
        assert PersonCategoryCodes.russia_resident_individual not in creatable_person_categories
        assert PersonCategoryCodes.russia_resident_legal_entity in creatable_person_categories

    def test_allowed_without_single_account(self, session, xmlrpcserver, client):
        ob.PersonBuilder(type=PersonCategoryCodes.russia_resident_individual, client=client).build(session)
        creatable_person_categories = self.get_creatable_person_categories(client, session, xmlrpcserver)
        assert PersonCategoryCodes.russia_resident_individual in creatable_person_categories
        assert PersonCategoryCodes.russia_resident_legal_entity in creatable_person_categories

    def test_allowed_without_existing_person(self, session, xmlrpcserver):
        client = create_client(session, with_single_account=True, region_id=RegionId.RUSSIA)
        creatable_person_categories = self.get_creatable_person_categories(client, session, xmlrpcserver)
        assert PersonCategoryCodes.russia_resident_individual in creatable_person_categories
        assert PersonCategoryCodes.russia_resident_legal_entity in creatable_person_categories
