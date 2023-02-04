# -*- coding: utf-8 -*-

import copy
import datetime

import hamcrest
import pytest

QTY = 2
from btestlib import utils as utils
from balance import balance_steps as steps
from btestlib.constants import Services
from temp.igogor.balance_objects import Contexts, Firms, ContractCommissionType, PersonTypes, Currencies

'''
allowed_agency_without_contract

- Если у агентства есть действующий договор на сервисы с признаком allowed_agency_without_contract = 1, то выставлять
счета на эти сервисы агентство может только по договору;
- Если у агентства есть приостановленный договор на сервисы с признаком allowed_agency_without_contract = 1,
то агентство не может выставлять счета на сервисы ни по договору, ни без договора.
- Если у агентства есть не действующий договор на сервисы с признаком allowed_agency_without_contract = 1
 (аннулированный, не подписанный, расторгнутый, с датой окончания в прошлом, с датой начала в будущем),
  то агентство может выставлять счета на эти сервисы без договора;

contract_needed

'''

NOW = datetime.datetime.now()
PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)
YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(NOW - datetime.timedelta(days=365))

MARKET = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111,
                                         contract_type=ContractCommissionType.PR_AGENCY)

DIRECT = MARKET.new(service=Services.DIRECT)
GEO = MARKET.new(service=Services.GEO)
BAYAN = MARKET.new(service=Services.BAYAN)

OFD = MARKET.new(service=Services.OFD, firm=Firms.OFD_18)
TOLOKA = MARKET.new(service=Services.TOLOKA,
                    contract_type=ContractCommissionType.SW_OPT_CLIENT,
                    firm=Firms.SERVICES_AG_16,
                    person_type=PersonTypes.SW_YT,
                    contract_currency=Currencies.USD)
DSP = MARKET.new(service=Services.DSP)
BANKI = MARKET.new(service=Services.BANKI, firm=Firms.YANDEX_1)
KUPIBILET = MARKET.new(service=Services.KUPIBILET, firm=Firms.YANDEX_1)
TRANSLATE = MARKET.new(service=Services.TRANSLATE)
DIRECT_TUNING = MARKET.new(service=Services.DIRECT_TUNING, firm=Firms.YANDEX_1)
MEDIANA = MARKET.new(service=Services.MEDIANA, firm=Firms.YANDEX_1)
VENDORS = MARKET.new(service=Services.VENDORS)
SCORING = MARKET.new(service=Services.SCORING, firm=Firms.YANDEX_1)
DOSTAVKA = MARKET.new(service=Services.DOSTAVKA)
PUBLIC = MARKET.new(service=Services.PUBLIC)
APIKEYS = MARKET.new(service=Services.APIKEYS)
CLOUD_143 = MARKET.new(service=Services.CLOUD_143)
SPAMDEF = MARKET.new(service=Services.SPAMDEF)
MEDIA_BANNERS = MARKET.new(service=Services.MEDIA_BANNERS)
MEDIA_BANNERS_167 = MARKET.new(service=Services.MEDIA_BANNERS_167)

SERVICES_WITH_ALLOWED_AGENCY_WITHOUT_CONTRACT_AND_CONTRACT_NEEDED = [DIRECT,
                                                                     MARKET,
                                                                     GEO]

SERVICES_WITH_ALLOWED_AGENCY_WITHOUT_CONTRACT_AND_CONTRACT_NEEDED_INCOMPATIBLE_OFFER = [BAYAN]
SERVICES_AGENCY_CONTRACT_NEEDED = [SCORING,
                                   OFD,
                                   TOLOKA,
                                   BANKI,
                                   KUPIBILET,
                                   # TRANSLATE,
                                   DIRECT_TUNING,
                                   MEDIANA,
                                   VENDORS,
                                   # CLOUD_143,
                                   MEDIA_BANNERS,
                                   # MEDIA_BANNERS_167
                                   ]

SERVICES_AGENCY_CONTRACT_NEEDED_TRUST_OR_PARTNER = [DOSTAVKA,
                                                    PUBLIC,
                                                    DSP,
                                                    SPAMDEF]

SERVICES_AGENCY_CONTRACT_NEEDED_SPECIAL = [APIKEYS]

BASE_CONTRACT_PARAMS = {
    'DT': YEAR_BEFORE_NOW_ISO,
    'PAYMENT_TYPE': 2,
    'IS_SIGNED': YEAR_BEFORE_NOW_ISO}


@pytest.mark.parametrize('additional_contract_params',
                         [
                             {},
                             {'IS_SUSPENDED': YEAR_BEFORE_NOW_ISO},
                             {'IS_CANCELLED': YEAR_BEFORE_NOW_ISO}
                         ])
@pytest.mark.parametrize('context',
                         SERVICES_WITH_ALLOWED_AGENCY_WITHOUT_CONTRACT_AND_CONTRACT_NEEDED +
                         SERVICES_WITH_ALLOWED_AGENCY_WITHOUT_CONTRACT_AND_CONTRACT_NEEDED_INCOMPATIBLE_OFFER
                         # [BAYAN]
                         )
def test_contract_is_available_depend_on_allowed_agency_without_contract(context, additional_contract_params):
    client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    contract_params = copy.deepcopy(BASE_CONTRACT_PARAMS)
    contract_params.update({'CLIENT_ID': agency_id,
                            'PERSON_ID': person_id,
                            'SERVICES': [context.service.id],
                            'FIRM': context.firm.id,
                            'CURRENCY': context.contract_currency.num_code if hasattr(context,
                                                                                      'contract_currency')
                            else Currencies.RUB.num_code
                            })

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    if additional_contract_params:
        contract_params.update(additional_contract_params)
        contract_params.update({'ID': contract_id,
                                'EXTERNAL_ID': contract_eid})
        steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list.append(
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW})
    request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = steps.RequestSteps.format_request_choices(request_choices)
    without_contract_request_choices = formatted_request_choices.get('without_contract', {})
    with_contract_request_choices = formatted_request_choices.get('with_contract', {})

    allowed_agency_without_contract = steps.CommonSteps.get_extprops('Service', context.service.id,
                                                                     'allowed_agency_without_contract'
                                                                     )[0]['value_num']
    utils.check_that(allowed_agency_without_contract, hamcrest.is_(1))
    if additional_contract_params.get('IS_SUSPENDED', None):
        utils.check_that(with_contract_request_choices, hamcrest.empty())
        utils.check_that(without_contract_request_choices, hamcrest.empty())
    elif additional_contract_params.get('IS_CANCELLED', None):
        utils.check_that(with_contract_request_choices, hamcrest.empty())
        if context in SERVICES_WITH_ALLOWED_AGENCY_WITHOUT_CONTRACT_AND_CONTRACT_NEEDED_INCOMPATIBLE_OFFER:
            utils.check_that(without_contract_request_choices, hamcrest.empty())
        else:
            utils.check_that(without_contract_request_choices, hamcrest.is_not(hamcrest.empty()))
    else:
        utils.check_that(with_contract_request_choices, hamcrest.is_not(hamcrest.empty()))
        utils.check_that(without_contract_request_choices, hamcrest.empty())


@pytest.mark.parametrize('context',
                         SERVICES_AGENCY_CONTRACT_NEEDED, ids=lambda ctx: 'service={}'.format(ctx.service.id))
def test_contract_only_is_available_contract_needed(context):
    client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    contract_params = copy.deepcopy(BASE_CONTRACT_PARAMS)
    contract_params.update({'CLIENT_ID': agency_id,
                            'PERSON_ID': person_id,
                            'SERVICES': [context.service.id],
                            'FIRM': context.firm.id,
                            'CURRENCY': context.contract_currency.num_code if hasattr(context,
                                                                                      'contract_currency')
                            else Currencies.RUB.num_code
                            })

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list.append(
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW})
    request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = steps.RequestSteps.format_request_choices(request_choices)
    without_contract_request_choices = formatted_request_choices.get('without_contract', {})
    with_contract_request_choices = formatted_request_choices.get('with_contract', {})
    utils.check_that(with_contract_request_choices, hamcrest.is_not(hamcrest.empty()))
    utils.check_that(without_contract_request_choices, hamcrest.empty())
