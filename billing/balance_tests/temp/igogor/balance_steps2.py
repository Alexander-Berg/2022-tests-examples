# coding: utf-8

import time
from datetime import timedelta

import balance.balance_api as api
import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.data.defaults as defaults
import btestlib.reporter as reporter
from temp.igogor.balance_objects import *


# В таком виде коллбэки создания переиспользуемы в разных модулях
# Здесь отделена логика преподготовки сущностей для создания заказа любого типа, каждый метод можно вызвать отдельно
# Класс нужен для хранения дефолтов, чтобы можно было их переопределить в каждом тесте один раз, а не в каждом вызове
class ObjectFactory(object):
    def __init__(self, consume_qty, completion_qty):
        self.consume_qty = consume_qty
        self.completion_qty = completion_qty

    @staticmethod
    def _optional(arg):
        return str(arg) if arg else u'Новый'

    def new_client(self, context, is_agency=False, agency=None, migrate_to_currency=None):
        # todo-igogor как тут понять что надо пресоздать агентство?
        with reporter.step(u'Создаем клиента в контексте: {}'.format(context)):
            client_template = Client.for_creation(context=context, agency=agency)
            if migrate_to_currency:
                client_template.migrate_to_currency = migrate_to_currency
            return create_or_update_client(client=client_template)

    def new_order(self, context, client=None):
        with reporter.step(u'Создаем заказ на клиента: {} в контексте: {}'.format(self._optional(client), context)):
            if not client:
                client = self.new_client(context)

            return create_or_update_orders(orders=[Order.for_creation(context=context, client=client)])[0]

    def order_with_consume(self, context, consume_qty=None, client=None):
        consume_qty = consume_qty or self.consume_qty

        with reporter.step(u'Создаем заказ с консьюмом qty={} на клиента: {} в контексте {}'
                                   .format(consume_qty, self._optional(client), context)):
            order = self.new_order(context, client)
            return put_consume_on_order(context=context, order=order, consume_qty=consume_qty)

    def order_with_several_consumes(self, context, consume_qty=None, client=None):
        consume_qty = consume_qty or self.consume_qty
        with reporter.step(u'Создаем заказ с несколькими консьюмами qty = {} на клиента: {} в контексте {}'
                                   .format(consume_qty, self._optional(client), context)):
            order = self.order_with_consume(context=context, consume_qty=consume_qty or self.consume_qty, client=client)
            put_consume_on_order(context=context, order=order, consume_qty=consume_qty)
            return order

    def order_with_consume_and_completion(self, context, consume_qty=None, completion_qty=None,
                                          client=None):
        consume_qty = consume_qty or self.consume_qty
        completion_qty = completion_qty or self.completion_qty
        with reporter.step(u'Создаем заказ с консьюмом qty = {} и откруткой qty = {} на клиента: {} в контексте {}'
                                   .format(consume_qty, completion_qty, self._optional(client), context)):
            order = self.order_with_consume(context=context, consume_qty=consume_qty, client=client)

            return put_completion_on_order(context=context, order=order, completion_qty=completion_qty)

    def order_with_several_consumes_and_completions(self, context, consume_qty=None, completion_qty=None, client=None):
        consume_qty = consume_qty or self.consume_qty
        completion_qty = completion_qty or self.completion_qty
        with reporter.step(u'Создаем заказ с несколькими консьюмами и открутками на клиента: {} в контексте {}'
                                   .format(self._optional(client), context)):
            order = self.order_with_consume_and_completion(context=context, consume_qty=consume_qty,
                                                           completion_qty=completion_qty, client=client)
            put_consume_on_order(context=context, order=order, consume_qty=consume_qty)
            put_completion_on_order(context=context, order=order, completion_qty=completion_qty)
            # todo-igogor надо аттачить возвращаемые значения?
            return order

    def converted(self, prepare_order, context, currency_context):
        with reporter.step(u'Создаем и конвертируем заказ'):
            order = prepare_order(context)
            convert_to_currency(currency_context, client=order.client, orders=[order])
            return order


# todo-igogor как красиво переключать создание клиента/агентства с агентством/без...
# может посоздавать более осмысленные фактори методы в Client?
def create_or_update_client(client, params=None):
    # type: (Client, dict) -> Client
    with reporter.step(u'{} {}:'.format(u'Создаем' if client.id else u'Редактируем',
                                        u'клиента' if client.is_agency else u'агентство')):
        request = utils.remove_empty({'CLIENT_ID': client.id,
                                      'NAME': client.name,
                                      'EMAIL': client.email,
                                      'PHONE': client.phone,
                                      'FAX': client.fax,
                                      'URL': client.url,
                                      'CITY': client.city,
                                      'IS_AGENCY': client.is_agency,
                                      'AGENCY_ID': client.agency.id if client.agency else None,
                                      'REGION_ID': client.region.id if client.region else None,
                                      'SERVICE_ID': client.service.id if client.service else None,
                                      'CURRENCY': client.currency.iso_code if client.currency else None,
                                      'MIGRATE_TO_CURRENCY': client.migrate_to_currency
                                      # todo-igogor такие закомментированные опциональные поля можно добавить во все степы
                                      # чтобы были перед глазами
                                      # 'CURRENCY_CONVERT_TYPE': None,  # 'COPY' или 'MODIFY'
                                      # 'ONLY_MANUAL_NAME_UPDATE': None  # bool
                                      })

        request.update(params or {})

        code, status, client_id = api.medium().CreateClient(defaults.PASSPORT_UID, request)

        # todo-igogor если в params передали параметры, которые хранятся в клиента - они не попадут в объект
        created_client = attr.assoc(client, id=client_id)
        print created_client
        reporter.logger().info(unicode(repr(created_client)))

        return created_client


def get_free_service_order_id(service):
    with reporter.step(u"Получаем service_order_id для сервиса {0}".format(service)):
        seq_name = api.test_balance().server.GetTestSequenceNameForService(service.id)
        query = 'select {0}.nextval from dual'.format(seq_name)

        service_order_id = db.balance().execute(query="select {0}.nextval from dual".format(seq_name),
                                                single_row=True)['nextval']
        reporter.attach(u"service_order_id", utils.Presenter.pretty(service_order_id))
        return service_order_id


def create_or_update_orders(orders, params_list=None, passport_uid=defaults.PASSPORT_UID):
    def xmlrpc_request_block(order):
        mandatory = {'ClientID': order.client.id,
                     'ProductID': order.product.id,
                     'ServiceID': order.service.id,
                     'ServiceOrderID': order.service_order_id,
                     'TEXT': 'Py_Test order'
                     }
        optionals = {'ManagerUID': order.manager.uid if order.manager else None,
                     'AgencyID': order.agency.id if order.agency else None}
        mandatory.update(utils.remove_empty(optionals))
        return mandatory

    with reporter.step(u'Создаем/обновляем группу заказов:'):
        if not params_list:
            params_list = [{} for _ in orders]
        reporter.attach(u'Параметры заказов',
                        # todo-igogor здесь бы лучше использовать большой repr заказа
                        utils.Presenter.pretty({str(order): order_params
                                                for order, order_params in zip(orders, params_list)}))
        for order in orders:
            if not order.service_order_id:
                order.service_order_id = get_free_service_order_id(order.service)

        request_params = [utils.merge_dicts([xmlrpc_request_block(order), order_params])
                          for order, order_params in zip(orders, params_list)]

        response_list = api.medium().CreateOrUpdateOrdersBatch(passport_uid, request_params)
        errors = {str(order): order_response[1] for order, order_response in zip(orders, response_list)
                  if order_response[0] == -1}
        if errors:
            reporter.attach(u'Ошибки создания/обновления заказов', utils.Presenter.pretty(errors))
            raise utils.TestsError('CreateOrUpdateOrdersBatch returned errors: {}'.format(errors))

        for order in orders:
            order.id = db.balance().execute(query="SELECT id FROM T_ORDER "
                                                  "WHERE SERVICE_ID = {} AND SERVICE_ORDER_ID = {}"
                                            .format(order.service.id, order.service_order_id),
                                            single_row=True, fail_empty=True)['id']
        return orders


def put_consume_on_order(context, order, consume_qty):
    with reporter.step(u'Добавляем на заказ {} консьюм с qty = {}'.format(order, consume_qty)):
        person_id = steps.PersonSteps.create(order.client.id, context.person_type.code)
        orders_list = [{'ServiceID': order.service.id,
                        'ServiceOrderID': order.service_order_id,
                        'Qty': float(consume_qty),
                        'BeginDT': datetime.now()}]
        request_id = steps.RequestSteps.create(order.client.id, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0, overdraft=0)
        steps.InvoiceSteps.pay(invoice_id)

        qty, money = consume_qty, context.money(consume_qty)
        if order.is_converted:
            qty, money = context.qty(consume_qty), consume_qty
        order.on_consume(qty=qty, money=money)
        return order


def put_completion_on_order(context, order, completion_qty):
    with reporter.step(u'Откручиваем заказ {} на qty = {}'.format(order, completion_qty)):
        total_completion_qty = order.completion_qty + completion_qty
        steps.CampaignsSteps.do_campaigns(service_id=order.service.id,
                                          service_order_id=order.service_order_id,
                                          campaigns_params={order.product.type.code: float(total_completion_qty)},
                                          campaigns_dt=datetime.now())
        order.on_completion(qty=completion_qty, money=context.money(completion_qty))
        return order


def convert_to_currency(context, client, migrate_to_currency=None, orders=None):
    # Можно передавать только migrate_to_currency в будущем, иначе баланс не даст изменить клиента.
    # Но в этом случае конвертации сейчас не произойдет.
    # Если migrate_to_currency == None, то гарантировано будет выполнена конвертация.
    assert migrate_to_currency is None or migrate_to_currency > datetime.now() + timedelta(seconds=60)

    with reporter.step(u'Конвертируем клиента {} в контексте {}'.format(client, context)):
        updated_client = attr.evolve(client,
                                     # Должна быть в будущем, иначе баланс поругается
                                     migrate_to_currency=migrate_to_currency or (datetime.now() + timedelta(seconds=300)),
                                     currency=context.client_template.currency,
                                     region=context.client_template.region)

        create_or_update_client(updated_client, {'CURRENCY_CONVERT_TYPE': 'MODIFY'})

        if migrate_to_currency is None:
            # Выставляем дату конвертации в прошлое, чтобы она гарантировано произошла.
            rowcount = db.balance().execute(
                '''UPDATE T_CLIENT_SERVICE_DATA SET migrate_to_currency = :dt
                   WHERE class_id = :client_id AND iso_currency = :iso_currency AND service_id = 7 ''',
                {'client_id': updated_client.id, 'iso_currency': context.client_template.currency.iso_code,
                 'dt': datetime.now() - timedelta(seconds=10)})
            assert rowcount == 1

        steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', updated_client.id)

        if orders:
            for order in orders:
                order.on_conversion()

        return updated_client


# todo-igogor а ведь тут может быть случай для нескольких заказов
# todo-igogor нет возможности вызвать с катомными невалидными параметрами для получения ошибки
def transfer(context, from_order, to_order, qty, conversion_context=None, from_consume=None, to_consume=None):
    with reporter.step(u'Перенос с заказа {} на заказ {} , qty = {}'.format(from_order, to_order, qty)):
        # #balance_logic
        consume_old_qty = from_order.consume_money if from_order.is_converted else from_order.consume_qty

        api.medium().CreateTransferMultiple(defaults.PASSPORT_UID,
                                            [{'ServiceID': from_order.service.id,
                                              'ServiceOrderID': from_order.service_order_id,
                                              'QtyOld': float(consume_old_qty),
                                              'QtyNew': float(consume_old_qty - qty)}],
                                            [{'ServiceID': to_order.service.id,
                                              'ServiceOrderID': to_order.service_order_id,
                                              'QtyDelta': 1}])

        # #balance_logic
        # todo-igogor я сожалею о всем далее написаном, наверное не стоило и ненавижу мультивалютность.
        if not from_consume:
            if from_order.is_converted:
                # для конвертированного заказа переносимый qty считается деньгами, но фишечный qty надо обновить
                from_consume = QtyMoney(qty=conversion_context.qty(qty), money=qty)
            else:
                # дефолтный консьюм переноса для однообразных заказов
                from_consume = QtyMoney(qty=qty, money=context.money(qty))
        # используется в качестве приходящего значения, в переносах между денежным и конвертированным заказами
        rounded_qty = utils.dround(qty, 2)
        if not to_consume:
            if to_order.is_converted:
                # для конвертированного заказа переносимый qty считается деньгами, но фишечный qty надо обновить
                to_consume = QtyMoney(qty=conversion_context.qty(qty), money=qty)
            elif from_order.is_converted and to_order.is_multicurrency:
                to_consume = QtyMoney(qty=qty, money=rounded_qty)

        from_order.on_transfer(to_order, transfer_consume=from_consume, to_order_consume=to_consume)


def group_transfer(parent):
    with reporter.step(u'Перенос по общему счету для клиента {}'.format(parent.client)):
        steps.CommonSteps.export('UA_TRANSFER', 'Client', parent.client.id)
        parent.on_group_transfer()


# todo-igogor переделать на новый степ
def group_orders(parent, members, without_transfer=True):
    # type: (OrderInfo, List[OrderInfo]) -> OrderInfo
    with reporter.step(u'Объединяем в заказы в группу. Главный: {}, дочерние: {}'
                               .format(parent, [str(member) for member in members])):
        steps.OrderSteps.merge(parent_order=parent.id, sub_orders_ids=[member.id for member in members],
                               group_without_transfer=1 if without_transfer else 0)
        parent.on_grouping(parent, members)
        for member in members:
            member.on_grouping(parent, members)
        return parent


def trigger_notification(trigger):
    with reporter.step(u'Инициируем нотификацию:'):
        return trigger()


def get_order_notifications(order):
    with reporter.step(u'Получаем нотификации для заказа {}'.format(str(order))):
        order_opcode = 1
        notifications = api.test_balance().GetNotification(order_opcode, order.id)
        notifications = [notification['args'][0] for notification in notifications]
        reporter.attach(u'Нотификации', utils.Presenter.pretty(notifications))
        return notifications
