# coding: utf-8
from datetime import datetime
from decimal import Decimal

import attr

import btestlib.reporter as reporter
from btestlib import constants as c
from btestlib import utils

NOT_SET = '__PaRaMeTeR.WaS_NoT.SeT__'


@attr.s(frozen=True)
class QtyMoney(object):
    qty = attr.ib(default=None)
    money = attr.ib(default=None)


@attr.s
class WithDictParams(object):
    # хранит атрибуты сущности, которые используются редко и не стоят отдельного атрибута, но надо хранить и передавать
    params = attr.ib(default=attr.Factory(dict), init=False)

    # хранит изменения внесенные в объект на последнем вызове new - для отчета и лога
    # дублирует часть значений из атрибутов и часть из params
    changes = attr.ib(default=attr.Factory(dict), init=False, cmp=False)
    # хранит дополнительные атрибуты переданные при создании объекта
    # нужно, чтобы при формировании запроса xmlrpc эти атрибуты накатывались последними
    # является подмножеством _changes - хранить проще чем фильтровать _changes
    params_changes = attr.ib(default=attr.Factory(dict), init=False, repr=False, cmp=False)

    def new(self, **kwargs):
        new_attributes = {name: value for name, value in kwargs.iteritems() if name != '_params' and value != NOT_SET}
        new_params = kwargs.get('_params', None) or {}
        new_inst = attr.evolve(self, **new_attributes)
        new_inst.params = utils.merge_dicts([self.params, new_params])
        new_inst.changes = utils.merge_dicts([new_attributes, new_params])
        new_inst.params_changes = new_params

        return new_inst

    # Не нравится то что тогда надо не возвращать None для отсутствующих, либо никак не валидируется доступ к аттриуту
    def __getitem__(self, item):
        return self.params.get(item, None)

    def __setitem__(self, key, value):
        self.params[key] = value

    def __getattr__(self, item):
        return self.params.get(item, None)


class EmptyObject(object):
    """
    Объект нужнен чтобы а) когда обращаешься к атрибуту вложенного объекта который не был задан ошибки не было
    б) было очевидно когда объект не был задан
    """

    def __getattr__(self, item):
        return None

    def __setattr__(self, key, value):
        raise utils.TestsError('Cannot set attribute of EmptyObject')

    def __nonzero__(self):
        return False

    def __eq__(self, other):
        return isinstance(other, EmptyObject)

    # метод нужен чтобы в контексте не проверять каждый раз что темплейт задан, но спорно
    def new(self, *args, **kwargs):
        return self


def is_empty(balance_object):
    return isinstance(balance_object, EmptyObject) or not any(attr.asdict(balance_object).values())


@attr.s
class Client(WithDictParams):
    id = attr.ib(default=None)
    is_agency = attr.ib(default=None)
    agency = attr.ib(default=attr.Factory(EmptyObject))

    region = attr.ib(default=attr.Factory(EmptyObject))
    currency = attr.ib(default=attr.Factory(EmptyObject))
    service = attr.ib(default=attr.Factory(EmptyObject))
    migrate_to_currency = attr.ib(default=None)

    def new(self, id=NOT_SET, is_agency=NOT_SET, agency=NOT_SET, region=NOT_SET, currency=NOT_SET, service=NOT_SET,
            migrate_to_currency=NOT_SET, **params):
        # type: (int, bool, Client, c.Regions.Region, c.Currencies.Currency, c.Services.Service, datetime) -> Client

        # params = locals()  # так можно сделать но не факт что короче и понятнее (и self убирать еще)
        return super(Client, self).new(id=id, is_agency=is_agency, agency=agency, region=region, currency=currency,
                                       service=service, migrate_to_currency=migrate_to_currency,
                                       _params=params)

    def __str__(self):
        return u'{}(id={}, is_agency={}{})'.format(type(self).__name__, self.id, self.is_agency,
                                                   u', agency={}'.format(self.agency.id) if self.agency.id else u'')


@attr.s
class Person(WithDictParams):
    id = attr.ib(default=None)
    client = attr.ib(default=attr.Factory(EmptyObject))
    type = attr.ib(default=attr.Factory(EmptyObject))

    def new(self, id=NOT_SET, client=NOT_SET, type=NOT_SET, dict_params=None, **params):
        # type: (int, Client, c.PersonTypes) -> Person
        if any([param.islower() for param in params.keys()]):
            raise utils.TestsError('All optional params must be in UPPER_UNDERSCORE_CASE. '
                                   'If you need to insert raw param (in kebab-case) use dict_params argument')
        return super(Person, self).new(id=id, client=client, type=type,
                                       _params=utils.merge_dicts([dict_params, params]))

    def __str__(self):
        return u'Person(id={}, type={}, client={})'.format(self.id, self.type.code, self.client.id)


@attr.s
class Contract(WithDictParams):
    id = attr.ib(default=None)
    external_id = attr.ib(default=None)
    client = attr.ib(default=attr.Factory(EmptyObject))
    person = attr.ib(default=attr.Factory(EmptyObject))
    collaterals = attr.ib(default=attr.Factory(list))

    type = attr.ib(default=attr.Factory(EmptyObject))  # это параметр COMMISSION
    services = attr.ib(default=attr.Factory(list))
    firm = attr.ib(default=attr.Factory(EmptyObject))
    country = attr.ib(default=attr.Factory(EmptyObject))
    currency = attr.ib(default=attr.Factory(EmptyObject))
    payment_type = attr.ib(default=None)

    start_dt = attr.ib(default=None)
    finish_dt = attr.ib(default=None)
    signed_dt = attr.ib(default=None)
    faxed_dt = attr.ib(default=None)
    cancelled_dt = attr.ib(default=None)
    suspended_dt = attr.ib(default=None)
    sent_dt = attr.ib(default=None)
    booked_dt = attr.ib(default=None)

    def new(self, id=NOT_SET, external_id=NOT_SET, client=NOT_SET, person=NOT_SET, collaterals=NOT_SET,
            type=NOT_SET, services=NOT_SET, firm=NOT_SET, country=NOT_SET, currency=NOT_SET, payment_type=NOT_SET,
            start_dt=NOT_SET, finish_dt=NOT_SET, signed_dt=NOT_SET, faxed_dt=NOT_SET, cancelled_dt=NOT_SET,
            suspended_dt=NOT_SET, sent_dt=NOT_SET, booked_dt=NOT_SET, dict_params=None,
            **params):
        # type: () -> Contract
        if any([param.islower() for param in params.keys()]):
            raise utils.TestsError('All optional params must be in UPPER_UNDERSCORE case. '
                                   'If you need to insert raw param use dict_params argument')
        return super(Contract, self).new(id=id, external_id=external_id, client=client, person=person, type=type,
                                         services=services, firm=firm, country=country, currency=currency,
                                         payment_type=payment_type, start_dt=start_dt, finish_dt=finish_dt,
                                         signed_dt=signed_dt, faxed_dt=faxed_dt,
                                         cancelled_dt=cancelled_dt, suspended_dt=suspended_dt,
                                         sent_dt=sent_dt, booked_dt=booked_dt,
                                         collaterals=collaterals,
                                         _params=utils.merge_dicts([dict_params, params]))

    def on_collateral(self, collateral):
        # todo-igogor тут по-любому понадобится более сложная логика. Что печально мне.
        collateral_ids = [col.id for col in self.collaterals]
        if collateral.id in collateral_ids:
            self.collaterals[collateral_ids.index(collateral.id)] = collateral
        else:
            self.collaterals.append(collateral)

    def __str__(self):
        return u'Contract({}-{}, {}, client={}, person={})'.format(self.id, self.external_id, self.type.name,
                                                                   self.client.id, self.person.id)


@attr.s
class Collateral(WithDictParams):
    id = attr.ib(default=None)
    contract = attr.ib(default=attr.Factory(EmptyObject))

    type_id = attr.ib(default=None)  # todo-igogor хотелось бы объект тут конечно, но пожалуй не обосновано

    start_dt = attr.ib(default=None)
    finish_dt = attr.ib(default=None)
    signed_dt = attr.ib(default=None)
    faxed_dt = attr.ib(default=None)
    booked_dt = attr.ib(default=None)
    cancelled_dt = attr.ib(default=None)

    # todo-igogor надо сделать получение дикт параметра по краткому имени без говна типа col-new-group02-grp-80-...

    def new(self, id=NOT_SET, contract=NOT_SET, type_id=NOT_SET,
            start_dt=NOT_SET, finish_dt=NOT_SET, signed_dt=NOT_SET, faxed_dt=NOT_SET, booked_dt=NOT_SET,
            cancelled_dt=NOT_SET, dict_params=None, **params):
        # type: () -> Collateral
        if any([param.islower() for param in params.keys()]):
            raise utils.TestsError('All optional params must be in UPPER_UNDERSCORE case. '
                                   'If you need to insert raw param use dict_params argument')
        return super(Collateral, self).new(id=id, contract=contract, type_id=type_id,
                                           start_dt=start_dt, finish_dt=finish_dt,
                                           signed_dt=signed_dt, faxed_dt=faxed_dt,
                                           booked_dt=booked_dt, cancelled_dt=cancelled_dt,
                                           _params=utils.merge_dicts([dict_params, params]))

    def __str__(self):
        return u'Collateral({}, contract={}-{})'.format(self.id, self.contract.id, self.contract.external_id)


@attr.s
class Order(WithDictParams):
    id = attr.ib(default=None)
    client = attr.ib(default=attr.Factory(EmptyObject))
    service = attr.ib(default=attr.Factory(EmptyObject))
    service_order_id = attr.ib(default=None)
    product = attr.ib(default=attr.Factory(EmptyObject))
    # todo-igogor зачем хранить валюту в заказе? Брать из клиента?
    # currency = attr.ib(default=attr.Factory(Currencies.empty))
    manager = attr.ib(default=attr.Factory(EmptyObject))
    agency = attr.ib(default=attr.Factory(EmptyObject))
    consumes = attr.ib(default=attr.Factory(list))  # todo-igogor использовать Consume a не QtyMoney?
    # В пользу этого говорит, что возможно нам надо будет хранить дополнительные атрибуты консьюма
    completions = attr.ib(default=attr.Factory(list))
    group_order = attr.ib(default=attr.Factory(EmptyObject))
    group_members = attr.ib(default=attr.Factory(list))
    is_converted = attr.ib(default=False)  # todo-igogor Почему здесь сразу прописан фолс - убрать

    # todo-igogor выпилить этот метод!!!
    @classmethod
    def for_creation(cls, context, client, agency=None, service_order_id=None):
        return cls(id=None,
                   client=client,
                   service=context.service,
                   service_order_id=service_order_id,
                   product=context.product,
                   currency=context.currency,
                   manager=context.manager,
                   agency=agency)

    def new(self, id=NOT_SET, service=NOT_SET, service_order_id=NOT_SET, client=NOT_SET, product=NOT_SET,
            manager=NOT_SET, agency=NOT_SET,
            group_order=NOT_SET, group_members=NOT_SET, **params):
        # type: () -> Order
        return super(Order, self).new(id=id, service=service, service_order_id=service_order_id, client=client,
                                      product=product, manager=manager, agency=agency, group_order=group_order,
                                      group_members=group_members, _params=params)

    @property
    def consume_qty(self):
        return sum([consume.qty for consume in self.consumes])

    @property
    def consume_money(self):
        return sum([consume.money for consume in self.consumes])

    @property
    def consume_money_rounded(self):
        # #balance_logic округление
        return sum([round(consume.money, 2) for consume in self.consumes])

    @property
    def completion_qty(self):
        return sum([completion.qty for completion in self.completions])

    @property
    def completion_money(self):
        return sum([completion.money for completion in self.completions])

    @property
    def group(self):
        return [self.group_order] + self.group_members

    @property
    def group_consume_qty(self):
        return sum([member.consume_qty for member in self.group])

    @property
    def group_consume_money(self):
        return sum([member.consume_money for member in self.group])

    @property
    def is_multicurrency(self):
        return self.product.type == c.ProductTypes.MONEY

    @property
    def is_main_order(self):
        return self.group_order is not None and self.group_order.id == self.id

    @property
    def notifications_count(self):
        # отправляется нотификация на каждый консьюм, на главный заказ при объединении, на начало и конец конвертации
        return len(self.consumes) + int(self.is_main_order) + (2 if self.is_converted else 0)

    def on_consume(self, qty, money):
        self.consumes.append(QtyMoney(qty=qty, money=money))  # todo-igogor здесь надо хранить консьюм всеже наверное

    def on_completion(self, qty, money):
        self.completions.append(QtyMoney(qty=qty, money=money))

    def on_transfer(self, to_order, transfer_consume, to_order_consume=None):
        transfer_consume = QtyMoney(qty=-abs(transfer_consume.qty), money=-abs(transfer_consume.money))
        if not to_order_consume:
            to_order_consume = QtyMoney(qty=abs(transfer_consume.qty), money=abs(transfer_consume.money))
        # todo-igogor не нравится мне здесь использование astuple не нужно оно
        self.on_consume(*attr.astuple(transfer_consume))
        to_order.on_consume(*attr.astuple(to_order_consume))

    def on_group_transfer(self):
        for member in self.group_members:
            if member.consume_qty:
                member.on_transfer(to_order=self,
                                   transfer_consume=QtyMoney(qty=member.consume_qty - member.completion_qty,
                                                             money=member.consume_money - member.completion_money))

    def on_conversion(self):
        self.is_converted = True

    def on_grouping(self, parent, members):
        self.group_order = parent
        self.group_members = members

    def __str__(self):
        return 'Order(id={}, soid={}-{})'.format(self.id, self.service.id, self.service_order_id)


@attr.s
class Request(WithDictParams):
    id = attr.ib(default=None)
    client = attr.ib(default=attr.Factory(EmptyObject))
    lines = attr.ib(default=attr.Factory(list))

    @property
    def qty(self):
        return sum([line.qty for line in self.lines])

    def new(self, id=NOT_SET, client=NOT_SET, lines=NOT_SET, **params):
        # type: (int, Client, list) -> Request
        return super(Request, self).new(id=id, client=client, lines=lines, _params=params)

    def __str__(self):
        return u'Request(id={}, client={}, orders=[{}])'.format(
            self.id, self.client.id, u', '.join([u'{}: {}'.format(line.order.id, line.qty)
                                                 for line in self.lines]))


@attr.s
class RequestOrder(WithDictParams):
    order = attr.ib(default=attr.Factory(EmptyObject))
    qty = attr.ib(default=None)  # не QtyMoney т.к. на этом этапе деньги еще не определены?
    begin_dt = attr.ib(default=None)

    def new(self, order=NOT_SET, qty=NOT_SET, begin_dt=NOT_SET, **params):
        # type: (Order, Decimal, datetime) -> RequestOrder
        return super(RequestOrder, self).new(order=order, qty=qty, begin_dt=begin_dt, _params=params)

    def __str__(self):
        return u'RequestOrder(order={}, qty={})'.format(self.order.id, self.qty)


@attr.s
class Line(WithDictParams):
    order = attr.ib(default=attr.Factory(EmptyObject))
    qty = attr.ib(default=None)  # не QtyMoney т.к. на этом этапе деньги еще не определены?
    sum = attr.ib(default=None)
    begin_dt = attr.ib(default=None)

    def new(self, order=NOT_SET, qty=NOT_SET, sum=NOT_SET, begin_dt=NOT_SET, **params):
        # type: (Order, Decimal, Decimal, datetime) -> RequestOrder
        return super(Line, self).new(order=order, qty=qty, begin_dt=begin_dt, _params=params)

    def __str__(self):
        return u'Line(order={}, qty={})'.format(self.order.id, self.qty)


@attr.s
class Invoice(WithDictParams):
    id = attr.ib(default=None)
    external_id = attr.ib(default=None)
    request = attr.ib(default=attr.Factory(EmptyObject))
    person = attr.ib(default=attr.Factory(EmptyObject))
    paysys = attr.ib(default=attr.Factory(EmptyObject))
    contract = attr.ib(default=attr.Factory(EmptyObject))
    is_overdraft = attr.ib(default=None)  # todo-igogor имхо эти поля потом не будут нужны
    is_credit = attr.ib(default=None)
    total = attr.ib(default=attr.Factory(EmptyObject))
    consumes = attr.ib(default=attr.Factory(list))  # todo-igogor вообще консьюмы можно брать из заказов реквеста

    def new(self, id=NOT_SET, external_id=NOT_SET, request=NOT_SET, person=NOT_SET, paysys=NOT_SET, contract=NOT_SET,
            is_credit=NOT_SET,
            is_overdraft=NOT_SET, total=NOT_SET, consumes=NOT_SET, **params):
        # type: (int, str, Request, Person, c.Paysys, Contract, bool, bool, QtyMoney, list) -> Invoice
        return super(Invoice, self).new(id=id, external_id=external_id, request=request, person=person, paysys=paysys,
                                        contract=contract, is_credit=is_credit, is_overdraft=is_overdraft, total=total,
                                        _params=params)

    @property
    def orders(self):
        return [line.order for line in self.request.lines]

    def get_order(self, order_id):
        return filter(lambda order: order.id == order_id, self.orders)[0]

    def on_payment(self, consumes, clear_consumes=False):
        # todo-igogor это некорректно, т.к. в заказе могут быть консьюмы из другого счета - сложновато
        if clear_consumes:
            for order in self.orders:
                order.consumes = []  # todo-igogor сделать метод в order?

        for consume in consumes:
            self.get_order(order_id=consume.order.id).on_consume(qty=consume.qty, money=consume.money)

    def __str__(self):
        return u'Invoice(id={}, external_id={}, client={}, sum={})'.format(
            self.id, self.external_id, self.request.client.id, self.total.money)


# todo-igogor как же мне не хотелось добавлять этот класс, а обойтись только QtyMoney
# todo-igogor избавиться от QtyMoney и оставить только этот?
@attr.s
class Consume(WithDictParams):  # todo-igogor надо ObjectWithParams?
    order = attr.ib(default=attr.Factory(EmptyObject))

    consume = attr.ib(default=attr.Factory(EmptyObject))
    completion = attr.ib(default=attr.Factory(EmptyObject))

    def new(self, order=NOT_SET, consume=NOT_SET, completion=NOT_SET, **params):
        # type: () -> Consume
        return super(self, Consume).new(order=order, consume=consume, completion=completion, _params=params)

    def __str__(self):
        return u'Consume(order_id={}, qty={}, money={})'.format(self.order.id, self.qty, self.money)


@attr.s
class Act(WithDictParams):
    id = attr.ib(default=None)
    external_id = attr.ib(default=None)
    invoice = attr.ib(default=attr.Factory(EmptyObject))
    dt = attr.ib(default=None)

    def new(self, id=NOT_SET, external_id=NOT_SET, invoice=NOT_SET, dt=NOT_SET, **params):
        # type: () -> Act
        return super(Act, self).new(id=id, external_id=external_id, invoice=invoice, dt=dt, _params=params)


# todo добавить в Context возможность проверять наличие атрибута
# через 'if context.qty is not None' вместо if hasattr(context, 'qty')
# todo сделать currency пропертей (если не задано получать значение из paysys)
@attr.s()
class Context(WithDictParams):
    __CONTEXTS = {}
    # Минимальный набор атрибутов контекста, которые необходимо инициализировать в каждом контексте
    # Расширять контексты можно через метод new,
    # через него в контексте можно переопределять значения существующих атрибутов
    # и задавать любой другой атрибут с произвольным именем
    name = attr.ib(default=None)
    service = attr.ib(default=attr.Factory(EmptyObject))
    paysys = attr.ib(default=attr.Factory(EmptyObject))
    product = attr.ib(default=attr.Factory(EmptyObject))
    price = attr.ib(default=None)
    manager = attr.ib(default=attr.Factory(EmptyObject))

    # todo-igogor можно положить в ProductType default_qty и брать оттуда
    lines_qty = attr.ib(default=attr.Factory(lambda: [Decimal('1000.1')]))

    agency_template = attr.ib(default=attr.Factory(EmptyObject))
    client_template = attr.ib(default=attr.Factory(EmptyObject))
    person_template = attr.ib(default=attr.Factory(EmptyObject))
    contract_template = attr.ib(default=attr.Factory(EmptyObject))
    order_template = attr.ib(default=attr.Factory(EmptyObject))
    lines_templates = attr.ib(default=attr.Factory(list))
    request_template = attr.ib(default=attr.Factory(EmptyObject))
    invoice_template = attr.ib(default=attr.Factory(EmptyObject))
    act_template = attr.ib(default=attr.Factory(EmptyObject))

    def __attrs_post_init__(self):
        if self.name is None:
            return
        if self.name in Context.__CONTEXTS:
            logger = reporter.logger()
            logger.warn('WARNING! Context name=%s already registered' % (self.name, ))
        Context.__CONTEXTS[self.name] = self

    @classmethod
    def get_context_by_name(cls, name):
        return cls.__CONTEXTS[name]

    def money(self, qty):
        return qty * self.price

    def qty(self, money):
        # #balance_logic округление
        return utils.dround(money / self.price, 6)

    # @property
    # def currency(self):
    #     type: () -> c.Currencies
    # return self.currency if ('currency' in attr.asdict(self).keys() + self.params.keys()) else self.paysys.currency

    # todo-igogor перечислить здесь все стандартные параметры
    def new(self, name=NOT_SET, service=NOT_SET, paysys=NOT_SET, product=NOT_SET, price=NOT_SET, manager=NOT_SET,
            lines_qty=NOT_SET,
            agency_template=NOT_SET, client_template=NOT_SET, person_template=NOT_SET, contract_template=NOT_SET,
            order_template=NOT_SET, lines_templates=NOT_SET, request_template=NOT_SET, invoice_template=NOT_SET,
            act_template=NOT_SET,
            **params):
        # type: () -> Context

        # attrs_from_self_instance = attr.asdict(self, recurse=False)
        # attrs_all = utils.merge_dicts([attrs_from_self_instance, kwargs])
        # attrs_names_from_context_class = [f.name for f in attr.fields(Context)]
        # attrs_names_to_extend_context = list(set(attrs_all.keys()) - set(attrs_names_from_context_class))
        # ContextNew = attr.make_class('ContextNew', attrs_names_to_extend_context, bases=(Context,))
        # return ContextNew(**attrs_all)
        return super(Context, self).new(name=name, service=service, paysys=paysys, product=product, price=price,
                                        manager=manager, lines_qty=lines_qty,
                                        agency_template=agency_template, client_template=client_template,
                                        person_template=person_template, contract_template=contract_template,
                                        order_template=order_template, lines_templates=lines_templates,
                                        request_template=request_template, invoice_template=invoice_template,
                                        act_template=act_template, _params=params)

    @property
    def agency(self):
        # type: () -> Client
        return self.agency_template.new(is_agency=True)

    @property
    def client(self):
        # type: () -> Client
        if not self.client_template:
            raise utils.TestsError('Must specify client_template in context. Look into balance_templates.Clients')
        return self.client_template.new(agency=self.agency, is_agency=False)

    @property
    def person(self):
        # type: () -> Person
        if not self.person_template:
            raise utils.TestsError('Must specify person_template in context. Look into balance_template.Persons')
        return self.person_template.new(client=self.agency or self.client)

    @property
    def contract(self):
        # type: () -> Contract
        return self.contract_template.new(client=self.agency or self.client,
                                          person=self.person.new(client=EmptyObject()))

    @property
    def order(self):
        # type: () -> Order
        order_template = self.order_template or Order().new()
        return order_template.new(client=self.client.new(agency=EmptyObject()), agency=self.agency,
                                  service=self.service, product=self.product, manager=self.manager)

    @property
    def lines(self):
        # копируем т.к. будем модифицировать
        if self.lines_templates:
            lines = [line.new(order=line.order or self.order.new(client=EmptyObject(), agency=EmptyObject()))
                     for line in self.lines_templates]
        else:
            lines = [Line().new(order=self.order.new(client=EmptyObject(), agency=EmptyObject()), qty=qty)
                     for qty in self.lines_qty]

        if self.agency and not lines[0].order.client:
            lines[0].order.client = self.client.new(agency=EmptyObject())

        return lines

    @property
    def request(self):
        # type: () -> Request
        request_template = self.request_template or Request().new()
        return request_template.new(client=self.agency or self.client, lines=self.lines)

    @property
    def invoice(self):
        # type: () -> Invoice
        invoice_template = self.invoice_template or Invoice().new()
        return invoice_template.new(request=self.request, person=self.person.new(client=EmptyObject()),
                                    contract=self.contract.new(client=EmptyObject(), person=EmptyObject()),
                                    paysys=self.paysys)

    @property
    def act(self):
        # type: () -> Act
        act_template = self.act_template or Act().new()
        return act_template.new(invoice=self.invoice)

    def __str__(self):
        return self.name



# todo-igogor выпилить. Не использовать!!!
class Product(object):
    def __init__(self, service_id, product_id, shipment_type, second_shipment_type=None):
        self.id = product_id
        self.service_id = service_id
        self.shipment_type = shipment_type
        self.second_shipment_type = second_shipment_type

    def __repr__(self):
        return 'Product: {service_id}-{id} ({shipment_type} \ {second_shipment_type})' \
            .format(**self.__dict__)

@attr.s()
class TrustPaymentData(WithDictParams):
    name = attr.ib(default=None)
    payment_row_update_dt = attr.ib(default=None)
    start_dt_utc = attr.ib(default=None)
    payment_row_dt = attr.ib(default=None)
    dt = attr.ib(default=None)
    payment_dt = attr.ib(default=None)
    postauth_dt = attr.ib(default=None)
    currency = attr.ib(default=attr.Factory(EmptyObject))
    service = attr.ib(default=attr.Factory(EmptyObject))
    region_id = attr.ib(default=attr.Factory(EmptyObject))
    dt_offset = attr.ib(default=None)
    commission_category = attr.ib(default=None)
    amount = attr.ib(default=None)
    price = attr.ib(default=None)
    payment_method = attr.ib(default=None)
    cancel_dt = attr.ib(default=None)

    def new(self, name=NOT_SET, payment_row_update_dt=NOT_SET, start_dt_utc=NOT_SET, payment_row_dt=NOT_SET,
            dt=NOT_SET, payment_dt=NOT_SET, postauth_dt=NOT_SET, currency=NOT_SET, service=NOT_SET,
            region_id=NOT_SET, dt_offset=NOT_SET, commission_category=NOT_SET, amount=NOT_SET,
            price=NOT_SET, payment_method=NOT_SET, cancel_dt=NOT_SET, **params):
        return super(TrustPaymentData, self).new(name=name, payment_row_update_dt=payment_row_update_dt,
                                                 start_dt_utc=start_dt_utc, payment_row_dt=payment_row_dt,
                                                 dt=dt, payment_dt=payment_dt, postauth_dt=postauth_dt,
                                                 currency=currency, service=service, region_id=region_id,
                                                 dt_offset=dt_offset, commission_category=commission_category,
                                                 amount=amount, price=price, payment_method=payment_method,
                                                 cancel_dt=cancel_dt, _params=params)

if __name__ == "__main__":
    reporter.log(Product(7, 1475, 'Bucks', 'Money'))
