# coding: utf-8

import urlparse

from balance import balance_steps as steps
from btestlib import passport_steps, utils, environments as env
from btestlib.constants import PersonTypes, Paysyses, Services, Products, Users

'''
Примеры вызовов POST и GET запросов

Данными запросами удобно пользоваться, если для какого-то действия не предусмотрено xmlrpc метода,
и ради одного этого действия не хочется поднимать браузер

Тип запроса, параметры, необходимые для запроса, и их формат можно увидеть в dev-косноли браузера на вкладке Network,
после осуществления действия вызывающего нужный запрос

Например,

Параметры, передаваемые при выставлении счета:
https://jing.yandex-team.ru/files/blubimov/2017-02-06_15-56-10.png

Параметры, передаваемые при отметке актов как плохой долг по кнопке 'Плохой долг' на странице счета:
https://jing.yandex-team.ru/files/blubimov/2017-02-01_15-03-13.png
'''


# Пример вызова POST-запроса для выставления счета
# Вызов запроса равносилен нажатию на кнопку 'Выставить счет' на странице paypreview
# Параметры запроса: https://jing.yandex-team.ru/files/blubimov/2017-02-06_15-56-10.png
def test_post_request():
    # Создаем сессию с авторизацией в паспорте
    user = Users.YB_ADM
    session = passport_steps.auth_session(user)

    # Формируем параметры для передачи в запрос
    issue_invoice_url = urlparse.urljoin(env.balance_env().balance_ci, '/issue-invoice.xml')
    client_id = steps.ClientSteps.create()
    request_id = create_request(client_id)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    paysys_id = Paysyses.BANK_UR_RUB.id
    sk = utils.get_secret_key(user.uid)

    params = dict(request_id=request_id, paysys_id=paysys_id, person_id=person_id, endbuyer_id='',
                  contract_id='', sk=sk)

    # Если необходимо указываем кастомные заголовки для передачи в запрос
    # Эти заголовки будут переданы вместе с автоматически сформированными
    # заголовками из session, содержащими данные авторазации
    headers = {'X-My-Custom-Header': 'MyCustomHeaderValue'}

    # Осуществляем запрос (по-умолчанию тип запроса - POST)
    resp = utils.call_http(session, issue_invoice_url, params, headers)

    # Получаем нужные данные из ответа
    invoice_id = utils.get_url_parameter(resp.url, param='invoice_id')[0]


# Пример вызова GET-запроса для отметки актов как плохой долг
# Вызов запроса равносилен нажатию по кнопку 'Плохой долг' на странице счета
# Параметры запроса: https://jing.yandex-team.ru/files/blubimov/2017-02-01_15-03-13.png
def test_get_request():
    # Создаем сессию с авторизацией в паспорте
    session = passport_steps.auth_session()

    # Формируем параметры для передачи в запрос
    invoice_id = create_invoice_with_act()

    make_bad_debt_url = urlparse.urljoin(env.balance_env().balance_ai, '/set-bad-debt.xml')

    params = {'invoice_id': invoice_id, 'commentary': 'test', 'our-fault': 1}

    # Если необходимо указываем кастомные заголовки для передачи в запрос
    # Эти заголовки будут переданы вместе с автоматически сформированными
    # заголовками из session, содержащими данные авторазации
    headers = {'X-Requested-With': 'XMLHttpRequest'}

    # Осуществляем запрос, тип запроса передаем в параметр method
    utils.call_http(session, make_bad_debt_url, params, headers, method='GET')


def create_request(client_id):
    SERVICE_ID = Services.DIRECT.id
    PRODUCT_ID = Products.DIRECT_FISH.id
    QTY = 200

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                            service_order_id=service_order_id)
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    return request_id


def create_invoice_with_act():
    PERSON_TYPE = PersonTypes.UR.code
    PAYSYS_ID = Paysyses.BANK_UR_RUB.id
    SERVICE_ID = Services.DIRECT.id
    PRODUCT_ID = Products.DIRECT_FISH.id
    QTY = 200

    client_id = steps.ClientSteps.create()
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                            service_order_id=service_order_id)
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID)
    steps.InvoiceSteps.turn_on(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': QTY})
    steps.ActsSteps.generate(client_id, force=0)
    return invoice_id
