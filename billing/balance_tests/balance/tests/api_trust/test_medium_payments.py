# -*- coding: utf-8 -*-
import datetime
import pytest
import hamcrest
import uuid
import json

from balance import balance_steps as steps
from balance import balance_db as db
from btestlib.constants import Regions
from temp.igogor.balance_objects import (Contexts, Services, Products, PersonTypes, Currencies,
                                         ContractPaymentType, Firms, Paysyses, Passports)

import btestlib.utils as utils
from btestlib.data.defaults import Date
from btestlib.matchers import contains_dicts_with_entries

from simpleapi.steps import passport_steps as passport
from simpleapi.steps import payments_api_steps as payments_api
from simpleapi.steps import web_steps as web
from simpleapi.data.cards_pool import get_card
from balance.features import Features
import btestlib.reporter as reporter

pytestmark = [reporter.feature(Features.TRUST_API)]

CLOUD_143 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.CLOUD_143, product=Products.CLOUD, region=Regions.RU,
                                                 currency=Currencies.RUB, firm=Firms.CLOUD_123)
CLOUD_KZ = Contexts.DIRECT_FISH_KZ_CONTEXT.new(service=Services.CLOUD_143, product=Products.CLOUD_KZ, region=Regions.KZ,
                                               currency=Currencies.KZT, firm=Firms.CLOUD_KZ)

CLOUD_143_UR = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.CLOUD_143, product=Products.CLOUD,
                                                    region=Regions.RU,
                                                    currency=Currencies.RUB, firm=Firms.CLOUD_123,
                                                    person_type=PersonTypes.UR, paysys=Paysyses.CC_RUB_UR_TRUST_CLOUD,
                                                    additional_contract_params={})

AUTO_RU_UR = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.AUTORU, product=Products.AUTORU, region=Regions.RU,
                                                  currency=Currencies.RUB, firm=Firms.VERTICAL_12,
                                                  person_type=PersonTypes.UR, paysys=Paysyses.CC_RUB_UR_TRUST_AUTORU,
                                                  additional_contract_params={})

AUTO_RU_PH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.AUTORU, product=Products.AUTORU, region=Regions.RU,
                                                  currency=Currencies.RUB, firm=Firms.VERTICAL_12,
                                                  person_type=PersonTypes.PH, paysys=Paysyses.CC_RUB_UR_TRUST_AUTORU,
                                                  additional_contract_params={})

CLOUD_143_PH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.CLOUD_143, product=Products.CLOUD,
                                                    region=Regions.RU,
                                                    currency=Currencies.RUB, firm=Firms.CLOUD_123,
                                                    person_type=PersonTypes.PH, paysys=Paysyses.CC_RUB_PH_TRUST_CLOUD,
                                                    additional_contract_params={'InvoiceDesireType': 'charge_note'})

PRACTICUM_PH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.PRACTICUM, product=Products.PRACTICUM,
                                                    region=Regions.RU,
                                                    currency=Currencies.RUB, firm=Firms.SHAD_34,
                                                    person_type=PersonTypes.PH, paysys=Paysyses.CC_RUB_PH_TRUST_CLOUD,
                                                    additional_contract_params={'InvoiceDesireType': 'charge_note'})

PRACTICUM_UR = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.PRACTICUM, product=Products.PRACTICUM,
                                                    region=Regions.RU,
                                                    currency=Currencies.RUB, firm=Firms.SHAD_34,
                                                    person_type=PersonTypes.UR, paysys=Paysyses.CC_RUB_PH_TRUST_CLOUD,
                                                    additional_contract_params={'InvoiceDesireType': 'charge_note'})

NOW = datetime.datetime.now()
QTY = 100

PAYMENT_SYSTEMS = ['ApplePay', 'MIR', 'Maestro', 'MasterCard', 'VISA', 'VISA_ELECTRON']

NOTIFICATION_URL = 'https://yandex.ru'
BASE_PH_PAYMENT_METHOD = {'contract_id': None,
                          'currency': 'RUB',
                          'legal_entity': 0,
                          'payment_method_id': None,
                          'payment_method_info': {'payment_systems': PAYMENT_SYSTEMS},
                          'payment_method_type': 'card',
                          'person_id': None,
                          'person_name': None,
                          'region_id': 225,
                          'resident': 1}


def populate_with_billing_developer_payload(
    json_str,  # type: str
    firm_id,  # type: int
):  # type: (...) -> str
    parsed_json = json.loads(json_str)
    assert isinstance(parsed_json, dict)

    parsed_json['pass_params'] = {
        'terminal_route_data': {
            'firm_id': firm_id,
        },
    }

    return json.dumps(parsed_json)


def escape_json_str(json_str):
    return '"' + json_str.replace('"', r'\"') + '"'


@pytest.mark.parametrize('context, params, ', [
    (CLOUD_143, {'person_for_contract': [{'type': PersonTypes.PH}]}),
    (CLOUD_KZ, {'person_for_contract': [{'type': PersonTypes.KZP}]}),
])
def test_get_request_payment_methods_contract_is_specified(context, params, get_free_user):
    user = get_free_user()

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    unbind_all_cards(user=user, service=context.service)
    generate_and_bind_card(context, user)
    person_type = params['person_for_contract'][0]['type']
    person_id = steps.PersonSteps.create(client_id, person_type.code)
    create_contract_func = create_contract if person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context)

    result = steps.TrustApiSteps.get_request_payment_methods(request_id=request_id,
                                                             contract_id=contract_id,
                                                             person_id=None,
                                                             passport_id=user.id_)
    bounded_methods = steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id,
                                                                    passport_id=user.id_)
    expected_result = get_payment_method_dict(person_id=person_id, bounded_methods=bounded_methods,
                                              firm_id=context.firm.id, contract_id=contract_id, region_id=context.region.id, currency=context.currency.iso_code)
    utils.check_that(result, contains_dicts_with_entries(expected_result, in_order=True))


def is_ur_only(persons):
    person_types = {person['type'].code for person in persons}
    return True if person_types == {'ur'} else False


@pytest.mark.parametrize('context, params, ', [
    (CLOUD_143_UR, {'request_id': None, 'with_exception': True}),
    (CLOUD_143_PH, {'request_id': None, 'with_exception': True}),
])
def test_get_request_payment_methods_mandatory_field(context, params, get_free_user):
    user = get_free_user()

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    unbind_all_cards(user=user, service=context.service)

    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    request_id = create_request(client_id, context)
    steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id, passport_id=user.id_)
    try:
        result = steps.TrustApiSteps.get_request_payment_methods(request_id=params.get('request_id', request_id),
                                                                 contract_id=None,
                                                                 person_id=None,
                                                                 passport_id=params.get('passport_id', user.id_))
        assert params.get('with_exception', False) is False
    except Exception as exc:
        assert params.get('with_exception', False) is True
        if params.get('request_id', not None) is None:
            assert steps.CommonSteps.get_exception_code(exc, 'msg') == 'Request with ID None not found in DB'


@pytest.mark.parametrize('context, params, ', [
    (CLOUD_143_PH, {'passport_id': 0}),
    (CLOUD_143_PH, {'passport_id': None}),
    (CLOUD_143_PH, {}),
])
def test_get_request_payment_methods_anon_payment_no_contract_no_card(context, params, get_free_user):
    user = get_free_user()

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    unbind_all_cards(user=user, service=context.service)

    request_id = create_request(client_id, context)
    steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id, passport_id=user.id_)
    result = steps.TrustApiSteps.get_request_payment_methods(request_id=params.get('request_id', request_id),
                                                             contract_id=None,
                                                             person_id=None,
                                                             passport_id=params.get('passport_id', user.id_))
    assert result == []


@pytest.mark.parametrize('context, params, ', [
    (CLOUD_143_PH, {'passport_id': 0}),
    (CLOUD_143_PH, {'passport_id': None}),
    (CLOUD_143_PH, {}),

])
def test_get_request_payment_methods_anon_payment_with_contract_no_card(context, params, get_free_user):
    user = get_free_user()

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    unbind_all_cards(user=user, service=context.service)

    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)

    request_id = create_request(client_id, context)
    steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id, passport_id=user.id_)
    result = steps.TrustApiSteps.get_request_payment_methods(request_id=params.get('request_id', request_id),
                                                             contract_id=None,
                                                             person_id=None,
                                                             passport_id=params.get('passport_id', user.id_))
    bounded_methods = steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id,
                                                                    passport_id=user.id_)
    expected_result = get_payment_method_dict(person_id=person_id, bounded_methods=bounded_methods,
                                              firm_id=context.firm.id, contract_id=contract_id, region_id=context.region.id, currency=context.currency.iso_code)
    payment_method_info = {} if params.get('passport_id', user.id_) in [None, -1, 0] else {'max_amount': '50000',
                                                                                           'payment_systems': [
                                                                                               'Maestro',
                                                                                               'MasterCard', 'VISA',
                                                                                               'VISA_ELECTRON']}
    utils.check_that(result, contains_dicts_with_entries([{'contract_id': contract_id,
                                                           'currency': 'RUB',
                                                           'legal_entity': 0,
                                                           'payment_method_id': None,
                                                           'payment_method_type': 'card',
                                                           'person_id': person_id,
                                                           'region_id': 225,
                                                           'resident': 1}],
                                                         in_order=True))


@pytest.mark.parametrize('context, params, ', [
    (CLOUD_143_PH, {'passport_id': 0}),
    (CLOUD_143_PH, {'passport_id': None})
])
def test_get_request_payment_methods_anon_payment_with_contract_with_card(context, params, get_free_user):
    user = get_free_user()

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    unbind_all_cards(user=user, service=context.service)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)

    request_id = create_request(client_id, context)
    steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id, passport_id=user.id_)
    result = steps.TrustApiSteps.get_request_payment_methods(request_id=params.get('request_id', request_id),
                                                             contract_id=None,
                                                             person_id=None,
                                                             passport_id=params.get('passport_id', user.id_))
    bounded_methods = steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id,
                                                                    passport_id=user.id_)
    expected_result = get_payment_method_dict(person_id=person_id, bounded_methods=bounded_methods,
                                              firm_id=context.firm.id, contract_id=contract_id, region_id=context.region.id, currency=context.currency.iso_code)
    payment_method_info = {} if params.get('passport_id', user.id_) in [None, -1, 0] else {'max_amount': '50000',
                                                                                           'payment_systems': [
                                                                                               'Maestro',
                                                                                               'MasterCard', 'VISA',
                                                                                               'VISA_ELECTRON']}
    utils.check_that(result, contains_dicts_with_entries([{'contract_id': contract_id,
                                                           'currency': 'RUB',
                                                           'legal_entity': 0,
                                                           'payment_method_id': None,
                                                           'payment_method_info': payment_method_info,
                                                           'payment_method_type': 'card',
                                                           'person_id': person_id,
                                                           # 'person_name': u'Юр. лицо или ПБОЮЛqdZd АО «Дроздова»',
                                                           'region_id': 225,
                                                           'resident': 1}], in_order=True))


@pytest.mark.parametrize('context, params, ', [
    (PRACTICUM_PH, {'passport_id': 0}),
    (PRACTICUM_PH, {'passport_id': None}),
])
def test_get_request_payment_methods_anon_payment_with_contract_with_card_practicum(context, params, get_free_user):
    user = get_free_user()

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    unbind_all_cards(user=user, service=context.service)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    request_id = create_request(client_id, context, params={})
    steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id, passport_id=user.id_)
    result = steps.TrustApiSteps.get_request_payment_methods(request_id=params.get('request_id', request_id),
                                                             contract_id=None,
                                                             person_id=None,
                                                             passport_id=params.get('passport_id', user.id_))
    bounded_methods = steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id,
                                                                    passport_id=user.id_)
    expected_result = get_payment_method_dict(person_id=person_id, bounded_methods=bounded_methods,
                                              firm_id=context.firm.id, contract_id=None, region_id=context.region.id,
                                              currency=context.currency.iso_code)
    payment_method_info = {} if params.get('passport_id', user.id_) in [None, -1, 0] else {'max_amount': '50000',
                                                                                           'payment_systems': [
                                                                                               'Maestro',
                                                                                               'MasterCard', 'VISA',
                                                                                               'VISA_ELECTRON']}
    utils.check_that(result, contains_dicts_with_entries([{'contract_id': None,
                                                           'currency': 'RUB',
                                                           'legal_entity': 0,
                                                           'payment_method_id': None,
                                                           'payment_method_info': payment_method_info,
                                                           'payment_method_type': 'card',
                                                           'person_id': person_id,
                                                           # 'person_name': u'Юр. лицо или ПБОЮЛqdZd АО «Дроздова»',
                                                           'region_id': 225,
                                                           'resident': 1}], in_order=True))


@pytest.mark.parametrize('context, params, ', [
    (CLOUD_143_PH, {})
])
def test_get_request_payment_methods_no_person_with_card(context, params, get_free_user):
    user = get_free_user()

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    unbind_all_cards(user=user, service=context.service)
    request_id = create_request(client_id, context)
    generate_and_bind_card(context, user)
    steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id, passport_id=user.id_)
    result = steps.TrustApiSteps.get_request_payment_methods(request_id=request_id,
                                                             contract_id=None,
                                                             person_id=None,
                                                             passport_id=user.id_)
    assert result == []


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {}),
])
def test_get_card_binding_url(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(service=context.service, user=user)
    result = \
        steps.TrustApiSteps.get_card_binding_url(service_id=context.service.id,
                                                 currency=context.currency.iso_code,
                                                 passport_id=user.id_)
    binding_url = result['binding_url']
    token = result['purchase_token']
    assert binding_url is not None
    assert token is not None


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {}),
])
def test_get_card_binding_url_extra_params(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(service=context.service, user=user)
    result = \
        steps.TrustApiSteps.get_card_binding_url(service_id=context.service.id,
                                                 currency=context.currency.iso_code,
                                                 passport_id=user.id_,
                                                 payload=json.dumps({}),
                                                 return_path='rfef',
                                                 notification_url=NOTIFICATION_URL)
    binding_url = result['binding_url']
    token = result['purchase_token']
    assert binding_url is not None
    assert token is not None
    card = get_card()
    bind_card(user, card, binding_url, context, token)


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {}),
])
def test_check_binding(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(service=context.service, user=user)
    binding_result, token = generate_and_bind_card(context=context, user=user)
    utils.check_that([binding_result],
                     contains_dicts_with_entries(
                         [{'status': 'success',
                           'payment_resp_desc': 'paid ok',
                           'timeout': '1200',
                           }]))
    assert binding_result['payment_method_id'] is not None
    assert binding_result['purchase_token'] == token


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {})
])
def test_check_binding_mandatory_fields(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(service=context.service, user=user)
    result = \
        steps.TrustApiSteps.get_card_binding_url(service_id=context.service.id,
                                                 currency=context.currency.iso_code,
                                                 passport_id=user.id_)
    token = params.get('purchase_token', result['purchase_token'])
    steps.TrustApiSteps.check_binding(passport_id=user.id_, service_id=context.service.id, token=token)


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {'card_count': 0}),
    (CLOUD_143_PH, {'card_count': 1}),
])
def test_get_bound_payment_methods(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(service=context.service, user=user)
    cards = []
    for x in range(params['card_count']):
        result = \
            steps.TrustApiSteps.get_card_binding_url(service_id=context.service.id,
                                                     currency=context.currency.iso_code,
                                                     passport_id=user.id_)
        binding_url = result['binding_url']
        token = result['purchase_token']
        card = get_card()
        bind_card(user, card, binding_url, context, token)
        cards.append(card)
    payment_methods = steps.TrustApiSteps.get_bound_payment_methods(service_id=context.service.id,
                                                                    passport_id=user.id_)
    expected_methods = []
    for card in cards:
        expected_methods.extend(get_bounded_payment_method_dict(card=card, passport_id=user.id_, firms=[16, 123]))
        expected_methods.extend(get_bounded_payment_method_dict(card=card, passport_id=user.id_, firms=[1020],
                                                                currency='KZT'))
    utils.check_that(payment_methods,
                     contains_dicts_with_entries(expected_methods))


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {'person_for_method': None, 'with_exception': False}),
    (CLOUD_143_PH, {'currency_for_method': None, 'with_exception': True}),
    (CLOUD_143_PH, {'payment_method_id_for_method': None, 'with_exception': True}),
    (CLOUD_143_PH, {'request_for_method': None, 'with_exception': True}),
    (CLOUD_143_PH, {'contract_for_method': None, 'with_exception': True}),
    (CLOUD_143_PH, {}),
])
def test_pay_request_mandatory_field(context, params, get_free_user):
    # user = USER_LIST[11]
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context, params=context.additional_contract_params)
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    try:
        payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                           payment_method_id=params.get(
                                                               'payment_method_id_for_method',
                                                               payment_method[
                                                                   'payment_method_id']),
                                                           currency_code=params.get('currency_for_method',
                                                                                    context.currency.iso_code),
                                                           # currency_code='USD',
                                                           request_id=params.get('request_for_method', request_id),
                                                           person_id=params.get('person_for_method', person_id),
                                                           contract_id=params.get('contract_for_method',
                                                                                  contract_id))
        assert params.get('with_exception', False) is False
    except Exception as exc:
        print exc
        assert params.get('with_exception', False)
        if params.get('contract_for_method', not None) is None:
            assert steps.CommonSteps.get_exception_code(exc,
                                                        'msg') == 'Invalid parameter for function: No payment options available'
        elif params.get('request_for_method', not None) is None:
            assert steps.CommonSteps.get_exception_code(exc, 'msg') == 'Invalid parameter for function: Must specify RequestID'
        elif params.get('payment_method_id_for_method', not None) is None:
            assert steps.CommonSteps.get_exception_code(exc,
                                                        'msg') == 'Invalid parameter for function: Must specify PaymentMethodID'
        elif params.get('currency_for_method', not None) is None:
            assert steps.CommonSteps.get_exception_code(exc,
                                                        'msg') == 'Invalid parameter for function: Must specify Currency'


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {}),
])
def test_pay_request_check_payment_and_invoice(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context, params=context.additional_contract_params)
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                       payment_method_id=params.get('payment_method_id_for_method',
                                                                                    payment_method[
                                                                                        'payment_method_id']),
                                                       currency_code=params.get('currency_for_method',
                                                                                context.currency.iso_code),
                                                       request_id=params.get('request_for_method', request_id),
                                                       person_id=params.get('person_for_method', person_id),
                                                       contract_id=params.get('contract_for_method', contract_id),
                                                       payload=json.dumps({}),
                                                       notification_url=None,

                                                       receipt_email='test_balance_notify@yandex-team.ru')

    transaction_id = payment_response['transaction_id']
    check_invoice_and_payment(invoice_id=payment_response['invoice_id'], contract_id=contract_id,
                              passport_id=user.id_, request_id=request_id, client_id=client_id,
                              context=context, transaction_id=transaction_id, notification_url=None)

    utils.wait_until(
        lambda: steps.TrustApiSteps.check_request_payment(passport_id=user.id_, request_id=request_id,
                                                          service_id=context.service.id,
                                                          transaction_id=None)[
            'resp_desc'],
        success_condition=hamcrest.equal_to('success'))
    payment_response = steps.TrustApiSteps.check_request_payment(passport_id=user.id_, request_id=request_id,
                                                                 service_id=context.service.id,
                                                                 transaction_id=None)


@pytest.mark.parametrize('context, params', [
    (AUTO_RU_UR, {}),
])
def pay_request_check_payment_and_invoice_auto(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context, params=context.additional_contract_params)
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                       payment_method_id=params.get('payment_method_id_for_method',
                                                                                    payment_method[
                                                                                        'payment_method_id']),
                                                       currency_code=params.get('currency_for_method',
                                                                                context.currency.iso_code),
                                                       request_id=params.get('request_for_method', request_id),
                                                       person_id=params.get('person_for_method', person_id),
                                                       contract_id=params.get('contract_for_method', contract_id),
                                                       payload=json.dumps({}), notification_url=None,
                                                       receipt_email='aikawa@yandex-team.ru')

    transaction_id = payment_response['transaction_id']

    utils.wait_until(
        lambda: steps.TrustApiSteps.check_request_payment(passport_id=user.id_, request_id=request_id,
                                                          service_id=context.service.id,
                                                          transaction_id=None)[
            'resp_desc'],
        success_condition=hamcrest.equal_to('success'))
    invoice_id = steps.TrustApiSteps.check_request_payment(passport_id=user.id_, request_id=request_id,
                                                           service_id=context.service.id,
                                                           transaction_id=None)['invoice_id']
    payment_id = db.get_payments_by_invoice(invoice_id)[0]['id']
    steps.CommonSteps.export(queue_='CASH_REGISTER', classname='Payment', object_id=payment_id)


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {'passport_id': 0}),
])
def test_pay_request_anon_payment_error(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context, params=context.additional_contract_params)
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    with pytest.raises(Exception) as exception:
        payment_response = steps.TrustApiSteps.pay_request(passport_id=params['passport_id'],
                                                           payment_method_id=params.get('payment_method_id_for_method',
                                                                                        payment_method[
                                                                                            'payment_method_id']),
                                                           currency_code=params.get('currency_for_method',
                                                                                    context.currency.iso_code),
                                                           request_id=params.get('request_for_method', request_id),
                                                           person_id=params.get('person_for_method', person_id),
                                                           contract_id=params.get('contract_for_method', contract_id),
                                                           payload=json.dumps({}),
                                                           notification_url=NOTIFICATION_URL)
    assert steps.CommonSteps.get_exception_code(exception.value,
                                                'msg') == 'Invalid parameter for function: Can\'t do payment with bound card without passport_id'


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {'passport_id': 0}),
])
def test_pay_request_anon_payment_success(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context, params=context.additional_contract_params)
    payment_response = steps.TrustApiSteps.pay_request(passport_id=params['passport_id'],
                                                       payment_method_id='trust_web_page',
                                                       currency_code=params.get('currency_for_method',
                                                                                context.currency.iso_code),
                                                       request_id=params.get('request_for_method', request_id),
                                                       person_id=params.get('person_for_method', person_id),
                                                       contract_id=params.get('contract_for_method', contract_id),
                                                       payload=json.dumps({}),
                                                       notification_url=NOTIFICATION_URL,
                                                       # redirect_url=REDIRECT_URL
                                                       )
    transaction_id = payment_response['transaction_id']
    check_invoice_and_payment(invoice_id=payment_response['invoice_id'], contract_id=contract_id,
                              passport_id=None, request_id=request_id, client_id=client_id,
                              context=context, transaction_id=transaction_id, notification_url=NOTIFICATION_URL)


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {'passport_id': 0}),
])
def test_pay_request_web_payment_success_with_passport(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context, params=context.additional_contract_params)
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                       payment_method_id='trust_web_page',
                                                       currency_code=params.get('currency_for_method',
                                                                                context.currency.iso_code),
                                                       request_id=params.get('request_for_method', request_id),
                                                       person_id=params.get('person_for_method', person_id),
                                                       contract_id=params.get('contract_for_method', contract_id),
                                                       payload=json.dumps({}),
                                                       notification_url=NOTIFICATION_URL
                                                       )
    transaction_id = payment_response['transaction_id']
    check_invoice_and_payment(invoice_id=payment_response['invoice_id'], contract_id=contract_id,
                              passport_id=user.id_, request_id=request_id, client_id=client_id,
                              context=context, transaction_id=transaction_id, notification_url=NOTIFICATION_URL)
    assert payment_response[
               'payment_url'] == 'https://trust-test.yandex.ru/web/payment?purchase_token={transaction_id}'.format(
        transaction_id=transaction_id)


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {'person_for_contract': [{'type': PersonTypes.PH}]}),
])
def test_pay_request_person_and_contract_mismatch(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_type = params['person_for_contract'][0]['type']
    person_id_not_from_contract = steps.PersonSteps.create(client_id, person_type.code)
    person_id = steps.PersonSteps.create(client_id, person_type.code)
    create_contract_func = create_contract if person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context,
                                params={
                                    'InvoiceDesireType': 'charge_note'} if person_type == PersonTypes.PH else {})
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    with pytest.raises(Exception) as exception:
        payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                           payment_method_id=payment_method['payment_method_id'],
                                                           currency_code=context.currency.iso_code,
                                                           request_id=request_id,
                                                           person_id=person_id_not_from_contract,
                                                           contract_id=contract_id)
    assert steps.CommonSteps.get_exception_code(exception.value,
                                                'msg') == 'Invalid parameter for function: No payment options available'


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {}),
])
def test_pay_request_wrong_payment_method_id(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type.code == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context)
    payment_method_id = 'random_fgbfbfgb'
    with pytest.raises(Exception) as exception:
        steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                        payment_method_id=payment_method_id,
                                        currency_code=context.currency.iso_code,
                                        request_id=request_id,
                                        person_id=person_id,
                                        contract_id=contract_id)
    assert steps.CommonSteps.get_exception_code(exception.value,
                                                'msg') == 'Invalid parameter for function: No payment options available'


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {})
])
def test_pay_request_no_person_no_triplet(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type.code == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context)
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    with pytest.raises(Exception) as exception:
        steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                        payment_method_id=payment_method['payment_method_id'],
                                        currency_code=context.currency.iso_code,
                                        request_id=request_id,
                                        contract_id=None)
    assert steps.CommonSteps.get_exception_code(exception.value,
                                                'msg') == 'Invalid parameter for function: Must specify either person_id, contract_id or legal_entity, resident and region_id'


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {'person_for_contract': [{'type': PersonTypes.PH}]}),
])
def test_pay_request_person_by_triplet(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_type = params['person_for_contract'][0]['type']
    person_id = steps.PersonSteps.create(client_id, person_type.code)
    create_contract_func = create_contract if person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context,
                                params={
                                    'InvoiceDesireType': 'charge_note'} if person_type == PersonTypes.PH else {})
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                       payment_method_id=payment_method['payment_method_id'],
                                                       currency_code=context.currency.iso_code,
                                                       request_id=request_id,
                                                       contract_id=contract_id,
                                                       legal_entity=payment_method['legal_entity'],
                                                       resident=payment_method['resident'],
                                                       region_id=payment_method['region_id'])


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {})
])
@pytest.mark.parametrize('wait_for_success', [
    True, False
])
def test_pay_request_twice(context, params, wait_for_success, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context)
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                       payment_method_id=payment_method['payment_method_id'],
                                                       currency_code=context.currency.iso_code,
                                                       request_id=request_id,
                                                       person_id=person_id, contract_id=contract_id)
    if wait_for_success:
        utils.wait_until(
            lambda: steps.TrustApiSteps.check_request_payment(passport_id=user.id_, request_id=request_id,
                                                              service_id=context.service.id,
                                                              transaction_id=None)[
                'resp_desc'],
            success_condition=hamcrest.equal_to('success'))
        payment_response = steps.TrustApiSteps.check_request_payment(passport_id=user.id_, request_id=request_id,
                                                                     service_id=context.service.id,
                                                                     transaction_id=None)
    payment_response_second = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                              payment_method_id=payment_method['payment_method_id'],
                                                              currency_code=context.currency.iso_code,
                                                              request_id=request_id,
                                                              person_id=person_id, contract_id=contract_id)
    assert payment_response == payment_response_second


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {})
])
def test_pay_request_twice_wrong_paysys_code(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context)
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                       payment_method_id=payment_method['payment_method_id'],
                                                       currency_code=context.currency.iso_code,
                                                       request_id=request_id,
                                                       person_id=person_id, contract_id=contract_id)
    db.balance().execute('''UPDATE T_PAYMENT SET PAYSYS_CODE = 'TRUST' WHERE INVOICE_ID = :invoice_id''',
                         {'invoice_id': payment_response['invoice_id']})
    payments = db.get_payments_by_invoice(payment_response['invoice_id'])
    assert len(payments) == 1
    with pytest.raises(Exception) as exception:
        steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                        payment_method_id=payment_method['payment_method_id'],
                                        currency_code=context.currency.iso_code,
                                        request_id=request_id,
                                        person_id=person_id, contract_id=contract_id)
    utils.check_that(steps.CommonSteps.get_exception_code(exception.value), hamcrest.equal_to('BALANCE_PAYMENTS_EXCEPTION'))


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {})
])
def test_pay_request_two_trust_payment(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context)
    payment_method = get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0]
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                                       payment_method_id=payment_method['payment_method_id'],
                                                       currency_code=context.currency.iso_code,
                                                       request_id=request_id,
                                                       person_id=person_id, contract_id=contract_id)
    first_payment_dt = db.get_payments_by_invoice(payment_response['invoice_id'])[0]['dt']
    db.balance().execute(
        '''INSERT INTO T_PAYMENT (ID, dt,  INVOICE_ID, PAYSYS_CODE, AMOUNT) VALUES (S_PAYMENT_ID.nextval, :dt, :invoice_id, 'TRUST_API', 100)''',
        {'invoice_id': payment_response['invoice_id'], 'dt': datetime.datetime.now() - datetime.timedelta(hours=1)})
    payments = db.get_payments_by_invoice(payment_response['invoice_id'])
    assert len(payments) == 2
    try:
        steps.TrustApiSteps.pay_request(passport_id=user.id_,
                                        payment_method_id=payment_method['payment_method_id'],
                                        currency_code=context.currency.iso_code,
                                        request_id=request_id,
                                        person_id=person_id, contract_id=contract_id)

    except Exception as exc:
        assert first_payment_dt > datetime.datetime.now() - datetime.timedelta(hours=1)


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {}),
])
def test_pay_request_delete_payment_then_pay(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context)
    payment_method_id = \
        get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0][
            'payment_method_id']
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_, payment_method_id=payment_method_id,
                                                       currency_code=context.currency.iso_code,
                                                       request_id=request_id,
                                                       contract_id=contract_id)
    invoice_id = payment_response['invoice_id']
    utils.wait_until(
        lambda: steps.TrustApiSteps.check_request_payment(passport_id=user.id_, request_id=request_id,
                                                          service_id=context.service.id,
                                                          transaction_id=None)[
            'resp_desc'],
        success_condition=hamcrest.equal_to('success'))

    db.balance().execute('''DELETE FROM t_payment WHERE invoice_id = :invoice_id''', {'invoice_id': invoice_id})
    payments = db.get_payments_by_invoice(invoice_id)
    assert len(payments) == 0
    with pytest.raises(Exception) as exception:
        steps.TrustApiSteps.pay_request(passport_id=user.id_, payment_method_id=payment_method_id,
                                        currency_code=context.currency.iso_code,
                                        request_id=request_id,
                                        person_id=person_id, contract_id=contract_id)
    utils.check_that(steps.CommonSteps.get_exception_code(exception.value), hamcrest.equal_to('BALANCE_PAYMENTS_EXCEPTION'))
    assert len(db.get_invoices_by_client_id(client_id)) == 2
    invoices = db.get_invoices_by_client_id(client_id)
    assert len(db.get_payments_by_invoice(invoices[0]['id'])) == 0


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {}),
])
def test_pay_request_extra_field(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context)
    payment_method_id = \
        get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0][
            'payment_method_id']
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_, payment_method_id=payment_method_id,
                                                       currency_code=context.currency.iso_code,
                                                       request_id=request_id,
                                                       contract_id=contract_id, payload=json.dumps({}),
                                                       notification_url=NOTIFICATION_URL)
    utils.check_that([payment_response],
                     contains_dicts_with_entries(
                         [{'amount': '100',
                           'cancel_dt': None,
                           'notify_url': NOTIFICATION_URL,
                           'currency': 'RUR',
                           'developer_payload': populate_with_billing_developer_payload('{}', context.firm.id),
                           'payment_dt': None,
                           'postauth_dt': None,
                           'request_id': request_id,
                           'resp_code': None,
                           'resp_desc': None}
                          ]))


@pytest.mark.parametrize('context, params', [
    (CLOUD_143_PH, {}),
])
def test_check_request_payment(context, params, get_free_user):
    user = get_free_user()

    unbind_all_cards(user=user, service=context.service)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    generate_and_bind_card(context, user)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract_func = create_contract if context.person_type == PersonTypes.UR else create_offer
    contract_id, _ = create_contract_func(context, client_id, person_id)
    request_id = create_request(client_id, context)
    payment_method_id = \
        get_payment_method_info(request_id, passport_id=user.id_, contract_id=None, person_id=None)[0][
            'payment_method_id']
    payment_response = steps.TrustApiSteps.pay_request(passport_id=user.id_, payment_method_id=payment_method_id,
                                                       currency_code=context.currency.iso_code,
                                                       request_id=request_id,
                                                       contract_id=contract_id, payload=json.dumps({}),
                                                       notification_url='https://www.yandex.ru')
    with pytest.raises(Exception) as exception:
        steps.TrustApiSteps.check_request_payment(passport_id=user.id_, service_id=context.service.id)
    assert steps.CommonSteps.get_exception_code(exception.value,
                                                'msg') == 'Invalid parameter for function: Must specify TransactionID or RequestID'


def get_payment_method_dict(contract_id=None, person_id=None, bounded_methods=[], firm_id=None, region_id=None,
                            currency=None):
    person_db = db.get_person_by_id(person_id)[0]
    result = [{'contract_id': contract_id,
               'currency': currency,
               'legal_entity': get_person_legal_entity(person_db['id']),
               'payment_method_id': None,
               'payment_method_type': 'card',
               'person_id': person_db['id'],
               'person_name': person_db['name'],
               'region_id': region_id,
               'resident': 1}]
    bounded_methods_for_firm = [method for method in bounded_methods if method['firm_id'] == firm_id]
    if bounded_methods_for_firm:
        deleted_keys = ['region_id', 'payment_method', 'currency', 'firm_id', 'id']
        for method in bounded_methods_for_firm:
            result.append({'contract_id': contract_id,
                           'currency': currency,
                           'legal_entity': get_person_legal_entity(person_db['id']),
                           'payment_method_id': method['id'],
                           'payment_method_info': {k: v for k, v in method.iteritems() if k not in deleted_keys},
                           'payment_method_type': 'card',
                           'person_id': person_db['id'],
                           'person_name': person_db['name'],
                           'region_id': region_id,
                           'resident': 1})
    return result


def get_person_legal_entity(person_id):
    return db.balance().execute('''SELECT pc.ur AS ur
    FROM t_Person p INNER JOIN T_PERSON_CATEGORY pc ON pc.CATEGORY = p.type
    WHERE id = :person_id''', {'person_id': person_id})[0]['ur']


def get_bounded_payment_method_dict(card, passport_id, firms=[], **kwargs):
    res = []
    for firm_id in firms:
        base = {'account': card['card_number'][:6] + '****' + card['card_number'][-4:],
                 'currency': 'RUB' if firm_id != 16 else 'USD',
                 'expiration_month': '01',
                 'expiration_year': '2025',
                 'expired': False,
                 'firm_id': firm_id,
                 'orig_uid': passport_id,
                 'payment_method': 'card',
                 'region_id': 225}
        base.update(kwargs)
        res.append(base)
    return res


def unbind_all_cards(user, service):
    payment_methods = steps.TrustApiSteps.get_bound_payment_methods(service_id=service.id, passport_id=user.id_)
    cards = [method['id'] for method in payment_methods if method.get('id', False)]
    if cards:
        for card_id in cards:
            passport.auth_via_page(user=user, passport=Passports.PROD)
            session_id = passport.get_current_session_id()
            payments_api.PaymentMethods.unbind_card(service, user, card_id, session_id)
    assert steps.TrustApiSteps.get_bound_payment_methods(service_id=service.id, passport_id=user.id_) == []


def bind_card(user, card, binding_url, context, token):
    web.Cloud.bind_card(card, binding_url)
    utils.wait_until(
        lambda: steps.TrustApiSteps.check_binding(passport_id=user.id_, service_id=context.service.id, token=token)[
            'payment_resp_desc'],
        success_condition=hamcrest.equal_to('paid ok'))
    return steps.TrustApiSteps.check_binding(passport_id=user.id_, service_id=context.service.id, token=token)


def generate_and_bind_card(context, user):
    result = \
        steps.TrustApiSteps.get_card_binding_url(service_id=context.service.id,
                                                 currency=context.currency.iso_code,
                                                 passport_id=user.id_)
    binding_url = result['binding_url']
    token = result['purchase_token']
    card = get_card()
    return bind_card(user, card, binding_url, context, token), token


def create_request(client_id, context, params=None):
    if params is None:
        params = {'InvoiceDesireType': 'charge_note'} if context.person_type == PersonTypes.PH else {}
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': NOW}
                   ]
    return steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                     additional_params=params)


def create_offer(context, client_id, person_id):
    return steps.ContractSteps.create_offer({'client_id': client_id,
                                             'currency': 'EUR',
                                             'firm_id': context.firm.id,
                                             'manager_uid': '244916211',
                                             'payment_term': 15,
                                             'payment_type': 3,
                                             'person_id': person_id,
                                             'projects': [str(uuid.uuid4())],
                                             'services': [context.service.id],
                                             'start_dt': Date.TODAY},
                                            327225081)
    # USER.id_)


def create_contract(context, client_id, person_id):
    return steps.ContractSteps.create_contract('no_agency',
                                               params={'CLIENT_ID': client_id,
                                                       'PERSON_ID': person_id,
                                                       'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                                       'SERVICES': [context.service.id],
                                                       'FIRM': str(context.firm.id),
                                                       'DT': Date.TODAY_ISO,
                                                       'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
                                                       'IS_SIGNED': Date.TODAY_ISO,
                                                       'CURRENCY': context.currency.num_code,
                                                       })


def get_payment_method_info(request_id, passport_id, contract_id=None, person_id=None):
    payment_methods = steps.TrustApiSteps.get_request_payment_methods(request_id=request_id,
                                                                      contract_id=contract_id,
                                                                      person_id=person_id,
                                                                      passport_id=passport_id,
                                                                      )
    return [{'payment_method_id': method['payment_method_id'], 'legal_entity': method['legal_entity'],
             'region_id': method['region_id'], 'resident': method['resident']} for method in payment_methods if
            method['payment_method_id'] is not None]


def check_invoice_and_payment(invoice_id, contract_id, passport_id, request_id, client_id, invoice_count=1,
                              context=None, payment_count=1, transaction_id='', notification_url=None,
                              ):
    invoices = db.get_invoice_by_id(invoice_id)
    assert len(invoices) == invoice_count
    utils.check_that(invoices,
                     contains_dicts_with_entries(
                         [{'client_id': client_id,
                           'consume_sum': 0,
                           'currency': 'RUR',
                           'effective_sum': 100,
                           'extern': 0,
                           'firm_id': 123,
                           'id': invoice_id,
                           'credit': 0,
                           'iso_currency': 'RUB',
                           'nds_pct': 20,
                           'overdraft': 0,
                           'payment_term_dt': None,
                           'payment_term_id': None,
                           'paysys_id': context.paysys.id,
                           'postpay': 0,
                           'promo_code_id': None,
                           'receipt_sum': 0,
                           'receipt_sum_1c': 0,
                           'request_id': request_id,
                           'total_act_sum': 0,
                           'total_sum': 100,
                           'transfer_acted': None,
                           'turn_on_dt': None,
                           'contract_id': contract_id,
                           'type': 'charge_note'}]))
    if payment_count:
        payments = db.get_payments_by_invoice(invoice_id)
        assert len(payments) == payment_count
        utils.check_that(payments,
                         contains_dicts_with_entries(
                             [{'amount': 100,
                               'card_holder': None,
                               'creator_uid': passport_id,
                               'currency': 'RUR',
                               'invoice_id': invoice_id,
                               'paysys_code': 'TRUST_API',
                               'postauth_dt': None,
                               'purchase_token': None,
                               'transaction_id': transaction_id}]))
        for payment_number in range(payment_count):
            developer_payload_db = \
                steps.CommonSteps.get_extprops(classname='Payment', object_id=payments[payment_number]['id'],
                                               attrname='developer_payload')[0]
            assert developer_payload_db['value_clob'] == escape_json_str(
                populate_with_billing_developer_payload("{}", firm_id=context.firm.id)
            )
            if notification_url:
                notify_url_db = \
                    steps.CommonSteps.get_extprops(classname='Payment', object_id=payments[payment_number]['id'],
                                                   attrname='notify_url')[0]
                assert notify_url_db['value_str'] == notification_url
