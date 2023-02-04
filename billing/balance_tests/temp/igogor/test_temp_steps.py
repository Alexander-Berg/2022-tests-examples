# coding: utf-8
from datetime import datetime
from decimal import Decimal as D

from balance import balance_steps2 as steps
from balance.balance_objects import Client, Order, Request, RequestOrder, Invoice, Consume
from balance.balance_templates import Clients, Persons
from btestlib import constants as c

# context = Contexts.DIRECT_FISH_RUB_CONTEXT
create = steps.ObjectFactory(consume_qty=100, completion_qty=0)  # todo-igogor create.new_client() это норм?


def test_objects_info():
    # Что я хотел от объектов:
    # 1) К атрибутам вложенных объектов должно быть можно обратиться без проверки что вложенный объект не None
    client = Client().new()  # пустой объект
    # В пустом объекте все вложенные объекты тоже должны быть инициализированы пустыми объектами
    region_id = client.region.id  # ошибки не будет, хотя мы и не передавали объект для региона

    # 2) Объект должен мочь иметь в качестве вложенного объект своего же типа (Клиент содержит агентство)
    # и этот вложенный объект тоже должен инициализироваться пустым
    agency_id = client.agency.id
    # внутренний объект внутреннего объекта не должен и не может быть инициализирован - бесконечная рекурсия
    tmp = client.agency.agency.id  # будет ошибка NoneType object ...
    # но это ограничение действует только для вложенных объектов того же типа
    region_id = client.agency.region.id  # ошибки не будет
    # и именно из-за таких случаев надо создавать пустые объекты через new() метод, иначе объект может быть не полон
    agency_id = Client().agency.id  # будет ошибка NoneType object ...

    # 3) В объект должно быть можно положить кастомное значение помимо заданных в классе атрибутов
    client = Client().new(SOME_VERY_RANDOM_VALUE=1234123)
    # благодаря этому нет необходимости наперед задавать все атрибуты сущности в объекте (что зачастую невозможно)
    client_id = client.id  # только атрибуты что нужны часто (и есть во всех типах данной сущности)
    client.region = c.Regions.BY  # атрибуты для которых написан объект (вместо хранения region_id)
    # также может быть полезным задавать атрибут для значений которые нужно преобразовывать в запросе (напр даты)
    # но при этом кастомыне атрибуты хранятся и их можно получить
    svrv = client.params['SOME_VERY_RANDOM_VALUE']
    # можно сделать так client['SOME_VERY_RANDOM_VALUE'] или так client.SOME_VERY_RANDOM_VALUE но надо ли?

    # 4) Удобное мутирование существующего объекта
    # возвращается новый объект - содержит все атрибуты старого, которые не изменились, измененные и новые
    updated_client = client.new(id=111, OTHER_RANDOM_VALUE='adfasdf')
    # благодаря этому можно удобно использовать шаблоны
    client = Clients.DIRECT_CURRENCY_RUB.new(NAME='asdfasdf')

    # P.S. метод new является методом инстанса, а не классметодом, чтоб не дублировать логику для создания и изменения
    # для создания пустого или нового объекта можно было бы делать вместо
    empty = Client().new()
    # использовать классметод (что является классическим подходом)
    empty = Client.create()
    # но писать его лень - хотя работы и не много но сигнатуру придется дублировать
    # и на самом деле та же проблема была бы с __init__ - сигнатуру пришлось бы дублировать в new()


def test_steps_info():
    # 1) Параметры степа - объект и дополнительные параметры метода (не сущности)
    steps.create_or_update_client(Client().new(), passport_uid='12341234')
    # По-умолчанию словарь заменяется объектом
    # следовательно список словарей - список объектов, несколько параметров словарей - несколько параметров объектов

    # 2) Если параметр метода не определен как атрибут объекта - его можно передать через ключевой параметр
    steps.create_or_update_client(Client().new(CURRENCY_CONVERT_TYPE='COPY'))

    # 3) Чтобы не перечислять кучу лишних параметров - используются темплейты
    # в них меняется только то что важно для данного вызова
    client1 = steps.create_or_update_client(Clients.DEFAULT.new(region=c.Regions.RU,
                                                                currency=c.Currencies.RUB,
                                                                migrate_to_currency=datetime.now()))
    # это критически важно для сужностей с кучей атрибутов и их вариаций - договоры и плательщики

    # 4) Степы первого уровня ничего не предсоздают (для создания заказа клиент должен быть создан заранее)
    client = steps.create_or_update_client(Clients.DEFAULT.new())
    order = steps.create_or_update_orders([Order().new(service=context.service, client=client,
                                                       product=context.product, manager=context.manager)])
    # предсоздание будет решаться в степах более высокого уровня, как и работа с контекстами

    # 5) Степ должно быть можно дернуть практически с полным игнором структуры объекта - как метод апи
    client = steps.create_or_update_client(Client().new(IS_AGENCY=False, NAME=u'Гиви', REGION_ID=225,
                                                        SERVICE_ID=7, CURRENCY='RUB',
                                                        MIGRATE_TO_CURRENCY='2017-01-01'))
    # но надо учитывать, что в возвращенном объекте все переданные параметры в params, остальное пустое
    # если не заполнить корректно его атрибуты его нельзя в дальнейшем использовать в степах
    # но значения всех переданных параметров в нем доступны
    region_id = client.params['REGION_ID']

    # 6) Значение атрибута должно быть можно переопределить в вызове ключевым параметром
    person = steps.create_or_update_person(Persons.UR.new(client=client1, client_id=1111111))
    # в вызове будет использовано 1111111 а не client1.id
    # в возвращенном значении
    person.client == client1
    person.params['client_id'] == 1111111
    # в определенный момент мне казалось, что иметь эту фичу очень важно, но теперь я не знаю зачем

    # 7) Хороший однообразный отчет. В нем должны быть отражены те параметры, которые мы изменяли в темплейте

    # 8) Можно делать негативные проверки


def test_create_client():
    # создаем клиента из темплейта с дефолтами, переопределяя часто используемые параметры
    client1 = steps.create_or_update_client(Clients.DEFAULT.new(region=c.Regions.RU,
                                                                currency=c.Currencies.RUB,
                                                                migrate_to_currency=datetime.now()))

    # создаем клиента из темплейта с дефолтами, переопределяя параметр реквеста XmlRpc
    client2 = steps.create_or_update_client(Clients.DIRECT_CURRENCY_USD.new(CURRENCY_CONVERT_TYPE='COPY'))

    # если в апи добавился новый параметр и нам надо его использовать - его не надо поддерживать в степах и объектах
    client3 = steps.create_or_update_client(Clients.DIRECT_CURRENCY_USD.new(SOME_NEW_PARAMETER='Pff'))

    # обновляем клиента - в клиенте должен быть заполнен id
    updated_client = steps.create_or_update_client(client3.new(is_agency=True, NAME=u'Шмаровоз'))

    # Игорь - дебил, понаделал хуйни, а мы ебемся.
    # Можно дернуть как обычный метод xmlrpc, но на выходе будет не полнофункциональный объект - делать не буду
    # В ситуации когда надо просто что-то дернуть - должно быть полезно
    client4 = steps.create_or_update_client(Client().new(IS_AGENCY=False, NAME=u'Гиви', REGION_ID=225,
                                                         SERVICE_ID=7, CURRENCY='RUB',
                                                         MIGRATE_TO_CURRENCY='2017-01-01'))


def test_create_order():
    client = steps.create_or_update_client(Clients.DEFAULT.new())
    order = steps.create_or_update_orders([Order().new(service=context.service, client=client,
                                                       product=context.product, manager=context.manager)])


def test_create_person():
    client = steps.create_or_update_client(Clients.DIRECT_CURRENCY_RUB.new())

    person = steps.create_or_update_person(Persons.UR.new(client=client, longname=u'Доктор Зло'))


def test_create_request():
    client = steps.create_or_update_client(Clients.DIRECT_CURRENCY_RUB.new())
    order_template = Order().new(service=context.service, client=client, product=context.product,
                                 manager=context.manager)

    # todo-igogor если так переиспользовать темплейт - теряются введенные параметры в отчете (что логично)
    order1, order2 = steps.create_or_update_orders([order_template.new(), order_template.new()])

    request = steps.create_request(Request().new(
        client=client,
        lines=[RequestOrder().new(order=order1, qty=D('100.0'), begin_dt=datetime.now()),
               RequestOrder().new(order=order2, qty=D('234.0'), Discount=5)],
        AdjustQty=1))


def test_create_invoice():
    client = steps.create_or_update_client(Clients.DIRECT_CURRENCY_RUB.new())

    order = steps.create_or_update_orders([Order().new(service=context.service, client=client,
                                                       product=context.product, manager=context.manager)])

    request = steps.create_request(Request().new(client=client,
                                                 lines=[RequestOrder().new(order=order, qty=D('100.0'))],
                                                 AdjustQty=1))
    person = steps.create_or_update_person(Persons.UR.new(client=client))

    invoice = steps.create_invoice(Invoice().new(request=request, person=person, paysys=c.Paysyses.BANK_UR_RUB,
                                                 price=context.price))


def create_invoice():
    client = steps.create_or_update_client(Clients.DIRECT_CURRENCY_RUB.new())
    order = steps.create_or_update_orders([Order().new(service=context.service, client=client,
                                                       product=context.product, manager=context.manager)])
    request = steps.create_request(Request().new(client=client,
                                                 lines=[RequestOrder().new(order=order, qty=D('100.0'))],
                                                 AdjustQty=1))
    person = steps.create_or_update_person(Persons.UR.new(client=client))
    invoice = steps.create_invoice(Invoice().new(request=request, person=person, paysys=c.Paysyses.BANK_UR_RUB,
                                                 price=context.price))

    return invoice


def test_pay_invoice():
    invoice = create_invoice()
    steps.pay(invoice=invoice)

    invoice2 = create_invoice()
    # todo-igogor эта ситуация работает, потому что консьюм реально создается один. Когда создается 2 - не будет.
    steps.pay(invoice=invoice2, payment_sum=invoice2.total.money * 0.5, payment_dt=datetime(2017, 6, 1))
    steps.pay(invoice=invoice2, payment_sum=invoice2.total.money - invoice2.total.money * 0.5,
              payment_dt=datetime(2017, 6, 2))

    invoice3 = create_invoice()

    steps.pay(invoice=invoice3, consumes_from_db=False)
    # хорошо, т.к. места таких вызовов легко найти
    invoice3.on_pay([Consume(order=invoice3.orders[0], qty=666, money=666999)])


def test_pay_invoice_several_orders():
    client = steps.create_or_update_client(Clients.DIRECT_CURRENCY_RUB.new())
    order1, order2 = steps.create_or_update_orders([Order().new(service=context.service, client=client,
                                                                product=context.product, manager=context.manager),
                                                    Order().new(service=context.service, client=client,
                                                                product=context.product, manager=context.manager)
                                                    ])
    request = steps.create_request(Request().new(client=client,
                                                 lines=[RequestOrder().new(order=order1, qty=D('100.0')),
                                                        RequestOrder().new(order=order2, qty=D('200.0'))],
                                                 AdjustQty=1))
    person = steps.create_or_update_person(Persons.UR.new(client=client))
    invoice = steps.create_invoice(Invoice().new(request=request, person=person, paysys=c.Paysyses.BANK_UR_RUB,
                                                 price=context.price))
    steps.pay(invoice)


# def test_create_act():
#     client = steps.create_or_update_client(Clients.DIRECT_CURRENCY_RUB.new())
#     order1, order2 = steps.create_or_update_orders([Order().new(service=context.service, client=client,
#                                                                 product=context.product, manager=context.manager),
#                                                     Order().new(service=context.service, client=client,
#                                                                 product=context.product, manager=context.manager)
#                                                     ])
#     request = steps.create_request(Request().new(client=client,
#                                                  request_orders=[RequestOrder().new(order=order1, qty=D('100.0')),
#                                                                  RequestOrder().new(order=order2, qty=D('200.0'))],
#                                                  AdjustQty=1))
#     person = steps.create_or_update_person(Persons.UR.new(client=client))
#     invoice = steps.create_invoice(Invoice().new(request=request, person=person, paysys=c.Paysyses.BANK_UR_RUB,
#                                                  price=context.price))
#     steps.pay(invoice)
#     invoice.on_payment(consumes=[Consume().new()])
#     # utils.check_that(invoice.consumes, equal_to([Consume().new()]))
#
#     steps.pay(Consume().new(invoice=invoice, consume=QtyMoney(qty=D('100.0'), money=D('3000.0')), dt=...))
#     steps.do_completions(order1, bucks=D('100.0'))
#     steps.do_completions(Consume().new(order=order1, completion=QtyMoney(qty=D('100.0', money=D('3000.0')))))
#     steps.do_completions(order2, bucks=D('200.0'))
#
#     act = steps.create_act(Act().new(client=client))


# def test_create_function():
#     act = Act().new(
#         invoice=Invoice().new(request=Request().new(request_orders=[RequestOrder().new(order=Order.new())])))


# def design_from_above():
#     context = Context().new(name='Pff',
#                             client_template=Clients.SOME.new(SOME=X),
#                             person_template=Persons.SOME,
#                             contract_template=Contracts.SOME,
#                             paysys=c.Paysyses.SOME,
#                             service=c.Services.SOME,
#                             product_template=c.Products.SOME,
#                             manager=c.Managers.SOME_MANAGER)
#
#     client = steps.create(context.client.new(...))
#     client = steps.create_or_update_client(context.client.new(...))
#
#     person = steps.create(context.person.new(client=client))
#     # понимает, что надо создать по client.id is None но надо ли
#     person = steps.create_or_update_person(context.person.new(client=context.client.new(...)))
#
#     contract = steps.create(context.contract.new(client=client, person=person))
#
#     order = steps.create(context.order(client=client))
#     order = steps.create_or_update_orders(context.order.new(client=client))
#     order1, order2 = steps.create([context.order.new(client=client),
#                                    context.order.new(client=client)])
#     order1, order2 = steps.create_or_update_orders([context.order.new(client=client),
#                                                     context.order.new(client=client)])
#
#     request = steps.create(Request().new(client=client,
#                                          request_orders=[RequestOrder().new(order=order1, qty=D('100.0')),
#                                                          RequestOrder().new(order=order2, qty=D('200.0'))]))
#     request = steps.create_request(Request().new(client=client,
#                                                  request_orders=[RequestOrder().new(order=order1, qty=D('100.0')),
#                                                                  RequestOrder().new(order=order2, qty=D('200.0'))]))
#
#     invoice = steps.create(context.invoice.new(request=request, person=person))
#     invoice = steps.create_invoice(Invoice().new(request=request, person=person, paysys=c.Paysyses.BANK_UR_RUB,
#                                                  price=context.price))
#
#     steps.pay(invoice)
#     steps.do_completions(order1, bucks=D('100'))
#
#     act = steps.create(context.act.new(invoice=invoice, dt=...))
#     act = steps.create_act(Act().new(invoice=invoice, dt=...))
#
#     # sweet dreams are made of this
#     act = create(Act().new(total=QtyMoney(qty=D('100.0'), money=D('100.0'))), context=context)
#     act = create(Act().new(lines=[Consume().new(completion=QtyMoney(qty=D('100.0'), money=D('3000.0'))),
#                                   Consume().new(completion=QtyMoney(qty=D('100.0'), money=D('3000.0')))]))
#
#     invoice = steps.get_from_db(invoice_id=1341234123)
#     act = create(Act().new(invoice=invoice, total=QtyMoney(qty=D('100.0'), money=D('3000.0'))),
#                  context=context)
#
#     act = create(Act().new(client=client),
#                  context=context)
#
#     context = Context(contract_template=Contracts.Y_TEMPLATE)
#     act = create(Act().new(invoice=Invoice().new(credit=1)), context=context)



