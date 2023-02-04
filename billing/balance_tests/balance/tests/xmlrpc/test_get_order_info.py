# coding: utf-8

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils
from btestlib.matchers import contains_dicts_with_entries

SERVICE_ID = 7
PRODUCT_ID = 1475
CONSUME_QTY = 100
CONSUME_MONEY = 3000
COMPLETION_QTY = 80
COMPLETION_MONEY = 2400
BASE_DT = datetime.datetime.now()
PAYSYS_ID = 1003


def request_order_info(service_id, service_order_id):
    return {'ServiceID': service_id, 'ServiceOrderID': service_order_id}


def response_order_info(custom_values):
    default_order_info = {'CompletionFixedMoneyQty': '0',
                          'CompletionMoneyQty': '0',
                          'ConsumeMoneyQty': '0',
                          'GroupServiceOrderID': None,
                          'ServiceID': None,
                          'completion_amount': None,
                          'completion_qty': '0',
                          'consume_amount': None,
                          'consume_qty': '0',
                          'invoice_currency': None,
                          'product_id': None}
    for value in custom_values:
        if value not in default_order_info:
            raise utils.TestsError('Given invalid parameter for GetOrdersInfo response: ' + value)

    default_order_info.update(custom_values)
    return default_order_info


class CaseInfo(object):
    @classmethod
    def combine(cls, cases):
        case_info = CaseInfo(client_ids=[], input_list=[], expected=[])
        # todo-igogor как в одну строчку получить объединение списков?
        for case in cases:
            case_info.client_ids += case.client_ids
            case_info.input_list += case.input_list
            case_info.expected += case.expected
        return case_info

    def __init__(self, client_ids, input_list, expected):
        self.client_ids = client_ids
        self.input_list = input_list
        self.expected = expected


def order_without_consumes(client_id=None):
    if not client_id:
        client_id = steps.ClientSteps.create()
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID)

    return CaseInfo(client_ids=[client_id],
                    input_list=[request_order_info(service_id=SERVICE_ID, service_order_id=service_order_id)],
                    expected=[response_order_info({'ServiceID': SERVICE_ID,
                                                   'product_id': PRODUCT_ID})])


'''ВАЖНО
Для упрощения методами создающими/модифицирующими заказ пользуемся только для кейса содержащего один заказ!
Для ситуаций когда надо рассмотреть кейс с несколькими заказами - создаем/модифицируем кейс для каждого
заказа отдельно - затем объединяем в один кейс CaseInfo.combine().
При таком подходе не надо усложнять каждый модифицирующий метод для работы с несколькими заказами - упрощение.
'''
def order_with_consumes(case_info=None):
    if not case_info:
        case_info = order_without_consumes()
    elif len(case_info.input) > 1:
        raise utils.TestsError('Here you should only use CaseInfo for one order! '
                               'For cases with several orders use CaseInfo.combine')
    person_id = steps.PersonSteps.create(case_info.client_ids[0], 'ur')
    service_order_id = case_info.input_list[0]['ServiceOrderID']
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id,
                    'Qty': CONSUME_QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(case_info.client_ids[0], orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, overdraft=0)
    steps.InvoiceSteps.pay(invoice_id)

    case_info.expected[0].update({'ConsumeMoneyQty': str(CONSUME_MONEY),
                                  'completion_amount': '0',  # ебаный биллинг
                                  'consume_amount': str(CONSUME_MONEY),
                                  'consume_qty': str(CONSUME_QTY),
                                  'invoice_currency': 'RUB'
                                  })
    return case_info


def order_with_consumes_and_completions(case_info=None):
    if not case_info:
        case_info = order_with_consumes()
    elif len(case_info.input) > 1:
        raise utils.TestsError('Here you should only use CaseInfo for one order! '
                               'For cases with several orders use CaseInfo.combine')
    service_order_id = case_info.input_list[0]['ServiceOrderID']
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': COMPLETION_QTY, 'Money': 0}, 0, BASE_DT)
    case_info.expected[0].update({'CompletionMoneyQty': str(COMPLETION_MONEY),
                                  'completion_amount': str(COMPLETION_MONEY),
                                  'completion_qty': str(COMPLETION_QTY)})
    return case_info


def empty_orders_list():
    return CaseInfo(client_ids=None, input_list=[], expected=[])


def two_orders_same_client():
    first_order_case = order_without_consumes()
    second_order_case = order_without_consumes(client_id=first_order_case.client_ids[0])
    return CaseInfo.combine([first_order_case, second_order_case])


def two_orders_different_clients():
    first_order_case = order_without_consumes()
    second_order_case = order_without_consumes()
    return CaseInfo.combine([first_order_case, second_order_case])


@pytest.mark.priority('low')
@reporter.feature(Features.XMLRPC, Features.TO_UNIT)
@pytest.mark.parametrize('case_info',
                         # [[{'ServiceID': 7, 'ServiceOrderID': 15414553}]]
                         [
                             # igogor тема параметризации коллбеками на первый взгляд мне даже очень нравится.
                             order_without_consumes,
                             order_with_consumes,
                             pytest.mark.smoke(order_with_consumes_and_completions),
                             empty_orders_list,
                             two_orders_same_client,
                             two_orders_different_clients,
                         ],
                         ids=lambda case: case.__name__)
def test_get_orders_info(case_info):
    case = case_info()
    actual = api.medium().GetOrdersInfo(case.input_list)
    utils.check_that(actual, contains_dicts_with_entries(case.expected))
