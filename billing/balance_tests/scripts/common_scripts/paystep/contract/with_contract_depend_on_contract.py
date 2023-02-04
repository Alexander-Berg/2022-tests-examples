# coding: utf-8
import copy
import datetime

import hamcrest
import pytest

import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import AuditFeatures
from btestlib import reporter
from btestlib.constants import Firms, Services, Products, ContractCommissionType, ContractPaymentType
from temp.igogor.balance_objects import Contexts

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
MEDIA = DIRECT.new(service=Services.MEDIA_70, firm=Firms.MARKET_111)
SPEC_PROJECT_SCALE_3 = 3
SPEC_PROJECT_MARKET_14 = 14
BASE_2015_SCALE_1 = 1
MEDIA_PRODUCT_17 = Products.MEDIA_FOR_AUTORU
MEDIA_PRODUCT_33 = Products.MEDIA_507784
MEDIA_PRODUCT_1 = Products.MEDIA_508125

'''
какой договор предлагается на пейстепе:
'''

dt = datetime.datetime.now()
DT_1_DAY_AFTER = dt + datetime.timedelta(days=1)
DT_1_DAY_BEFORE = dt - datetime.timedelta(days=1)
DT_1_DAY_BEFORE_ISO = utils.Date.date_to_iso_format(dt - datetime.timedelta(days=1))
DT_1_DAY_AFTER_ISO = utils.Date.date_to_iso_format(dt + datetime.timedelta(days=1))
HALF_YEAR_AFTER_NOW_ISO = utils.Date.date_to_iso_format(dt + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(dt - datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW = dt - datetime.timedelta(days=180)
YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(dt - datetime.timedelta(days=365))

QTY = 100

BASE_CONTRACT_PARAMS = {
    'DT': YEAR_BEFORE_NOW_ISO,
    'PAYMENT_TYPE': 2,
}


def create_contract(client_id, person_id, context, additional_contract_param=None):
    is_cancelled_dt = None
    is_suspended_dt = None
    services_list = additional_contract_param['SERVICES'] if additional_contract_param.get('SERVICES',
                                                                                           False) else [
        context.service.id]
    print additional_contract_param
    contract_params = copy.deepcopy(BASE_CONTRACT_PARAMS)
    contract_params.update({'CLIENT_ID': client_id,
                            'PERSON_ID': person_id,
                            'SERVICES': services_list,
                            'FIRM': context.firm.id
                            })
    if additional_contract_param.get('IS_CANCELLED', False):
        is_cancelled_dt = additional_contract_param.pop('IS_CANCELLED')
    if additional_contract_param.get('IS_SUSPENDED', False):
        is_suspended_dt = additional_contract_param.pop('IS_SUSPENDED')
    contract_params.update(additional_contract_param)
    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    if is_cancelled_dt:
        contract_params.update({'IS_CANCELLED': is_cancelled_dt,
                                'ID': contract_id,
                                'EXTERNAL_ID': contract_eid})
    if is_suspended_dt:
        contract_params.update({'IS_SUSPENDED': is_suspended_dt,
                                'ID': contract_id,
                                'EXTERNAL_ID': contract_eid})
    steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    return contract_id


@pytest.mark.parametrize('params', [
    # подписанный договор без даты окончания доступен
    {'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO},
     'is_contract_available_on_paystep': True},

    # подписанный приостановленный договор недоступен
    pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_3)(
        {'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                                        'IS_SUSPENDED': YEAR_BEFORE_NOW_ISO},
         'is_contract_available_on_paystep': False}
    )),

    # неподписанный договор не доступен
    pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_3)(
        {'additional_contract_params': {'IS_SIGNED': None},
         'is_contract_available_on_paystep': False}
    )),

    # подписанный по факсу договор доступен
    {'additional_contract_params': {'IS_SIGNED': None,
                                    'IS_FAXED': YEAR_BEFORE_NOW_ISO},
     'is_contract_available_on_paystep': True},

    # подписанный отмененный договор не доступен
    pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_3)(
        {'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                                        'IS_CANCELLED': YEAR_BEFORE_NOW_ISO},
         'is_contract_available_on_paystep': False}
    )),

    # договор созданный позже, чем на текущее время, недоступен
    pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_3)(
        {'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                                        'DT': DT_1_DAY_AFTER_ISO},
         'is_contract_available_on_paystep': False}
    )),

    # договор с датой окончания позже, чем текущее время, доступен
    {'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                                    'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO},
     'is_contract_available_on_paystep': True},

    # договор с датой окончания ранее, чем текущее время, недоступен
    pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_3)(
        {'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                                        'FINISH_DT': DT_1_DAY_BEFORE_ISO},
         'is_contract_available_on_paystep': False}
    )),

    # постоплатный договор с датой окончания ранее, чем текущее время, недоступен
    pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11)(
        {'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                                        'FINISH_DT': DT_1_DAY_BEFORE_ISO,
                                        'PAYMENT_TYPE': ContractPaymentType.POSTPAY},
         'is_contract_available_on_paystep': False}
    )),

    # договор с сервисом не из заказа не доступен
    {'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                                    'SERVICES': [Services.BAYAN.id]},
     'is_contract_available_on_paystep': False},

    # договор с несколькими сервисами один из которых есть в заказе, доступен
    {'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                                    'SERVICES': [Services.DIRECT.id,
                                                 Services.BAYAN.id]},
     'is_contract_available_on_paystep': True},

    # если в реквесте указана дата счета, все проверки с датами производятся на эту дату
    {'InvoiceDesireDT': HALF_YEAR_BEFORE_NOW, 'additional_contract_params': {'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                                                                             'FINISH_DT': DT_1_DAY_BEFORE_ISO},
     'is_contract_available_on_paystep': True},

])
@pytest.mark.parametrize('context', [DIRECT.new(contract_type=ContractCommissionType.NO_AGENCY)])
def test_contract_is_available_depend_on_contract_dates(context, params):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_contract(client_id, person_id, context, additional_contract_param=params['additional_contract_params'])
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': params.get('InvoiceDesireDT', None)})
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = steps.RequestSteps.format_request_choices(request_choices)
    with_contract_request_choices = formatted_request_choices.get('with_contract', {})
    if params['is_contract_available_on_paystep']:
        utils.check_that(with_contract_request_choices, hamcrest.is_not(hamcrest.empty()))
    else:
        utils.check_that(with_contract_request_choices, hamcrest.empty())


@pytest.mark.parametrize('context, params',
                         [
                             # если в реквесте все продукты с 17 commission_type, договор со шкалой 3 доступен
                             (MEDIA.new(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                        scale=SPEC_PROJECT_SCALE_3,
                                        product_list=[MEDIA_PRODUCT_17]),
                              {'is_contract_available_on_paystep': True}),

                             # если в реквесте не все продукты с 17 commission_type, договор со шкалой 3 недоступен
                             (MEDIA.new(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                        scale=SPEC_PROJECT_SCALE_3,
                                        product_list=[MEDIA_PRODUCT_17,
                                                      MEDIA_PRODUCT_33]),
                              {'is_contract_available_on_paystep': False}),

                             # если в реквесте все продукты с 33 commission_type, договор со шкалой 14 доступен
                             (MEDIA.new(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                        scale=SPEC_PROJECT_MARKET_14,
                                        product_list=[MEDIA_PRODUCT_33]),
                              {'is_contract_available_on_paystep': True}),

                             # если в реквесте не все продукты с 33 commission_type, договор со шкалой 14 недоступен
                             (MEDIA.new(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                        scale=SPEC_PROJECT_MARKET_14,
                                        product_list=[MEDIA_PRODUCT_17,
                                                      MEDIA_PRODUCT_33]),
                              {'is_contract_available_on_paystep': False}),

                             # если в договоре указана шкала не 3, 14, то договор доступен,
                             # если commission_type не 17, 33 у всех продуктов

                             (MEDIA.new(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                        scale=BASE_2015_SCALE_1,
                                        product_list=[MEDIA_PRODUCT_1]
                                        ),
                              {'is_contract_available_on_paystep': True}),

                             #
                             (MEDIA.new(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                        scale=BASE_2015_SCALE_1,
                                        product_list=[MEDIA_PRODUCT_1,
                                                      MEDIA_PRODUCT_17]
                                        ),
                              {'is_contract_available_on_paystep': False}),

                             (MEDIA.new(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                        scale=BASE_2015_SCALE_1,
                                        product_list=[MEDIA_PRODUCT_1,
                                                      MEDIA_PRODUCT_33]
                                        ),
                              {'is_contract_available_on_paystep': False}),

                             # договор другого типа доступен, если все продукты в нем с commission_type 17
                             (MEDIA.new(contract_type=ContractCommissionType.PR_AGENCY,
                                        product_list=[MEDIA_PRODUCT_17]
                                        ),
                              {'is_contract_available_on_paystep': True}),

                             (MEDIA.new(contract_type=ContractCommissionType.PR_AGENCY,
                                        product_list=[MEDIA_PRODUCT_17,
                                                      MEDIA_PRODUCT_1]
                                        ),
                              {'is_contract_available_on_paystep': False}),

                             (MEDIA.new(contract_type=ContractCommissionType.PR_AGENCY,
                                        product_list=[MEDIA_PRODUCT_33,
                                                      MEDIA_PRODUCT_1]
                                        ),
                              {'is_contract_available_on_paystep': False}),

                             (MEDIA.new(contract_type=ContractCommissionType.PR_AGENCY,
                                        product_list=[MEDIA_PRODUCT_1]
                                        ),
                              {'is_contract_available_on_paystep': True}),

                         ])
def test_contract_is_available_depend_on_product_commission_type(context, params):
    client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    create_contract(agency_id, person_id, context,
                    additional_contract_param={
                        'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': context.scale if hasattr(context,
                                                                                              'scale') else None,
                        'IS_SIGNED': YEAR_BEFORE_NOW_ISO})
    orders_list = []
    for product in context.product_list:
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
        steps.OrderSteps.create(client_id=client_id, product_id=product.id, service_id=context.service.id,
                                service_order_id=service_order_id, params={'AgencyID': agency_id})
        orders_list.append(
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt})
    request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = steps.RequestSteps.format_request_choices(request_choices)
    with_contract_request_choices = formatted_request_choices.get('with_contract', {})
    if params['is_contract_available_on_paystep']:
        utils.check_that(with_contract_request_choices, hamcrest.is_not(hamcrest.empty()))
    else:
        utils.check_that(with_contract_request_choices, hamcrest.empty())
