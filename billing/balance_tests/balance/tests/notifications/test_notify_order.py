# coding: utf-8

from datetime import timedelta
import time
import pytest
from hamcrest import equal_to, anything

import balance.balance_db as db
from balance.balance_steps.common_steps import CommonSteps
import btestlib.reporter as reporter
import temp.igogor.balance_steps2 as steps
from balance.features import Features
from btestlib import constants as const
from btestlib.matchers import matches_in_time, contains_dicts_equal_to
from temp.igogor.balance_objects import *
from temp.igogor.balance_objects import Contexts
from btestlib.environments import BalanceHosts as hosts

'''Не хватает кейсов:
Обработка ответа сервиса о нотификации
Для простых оплат?
'''

DIRECT_FISH_RUB_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_FISH_RUB')
DIRECT_FISH_USD_CONTEXT = Contexts.DIRECT_FISH_USD_CONTEXT.new(name='DIRECT_FISH_USD')
DIRECT_MONEY_RUB_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(name='DIRECT_MONEY_RUB')
NAVIGATOR_FISH = DIRECT_FISH_RUB_CONTEXT.new(name='NAVIGATOR_FISH',
                                             service=const.Services.NAVI, product=const.Products.NAVI,
                                             price=Decimal('1.0'))
DIRECT_MONEY_USD_CONTEXT = Contexts.DIRECT_MONEY_USD_CONTEXT.new(name='DIRECT_MONEY_USD')
MARKET_RUB_CONTEXT = Contexts.MARKET_RUB_CONTEXT.new(name='MARKET_FISH_RUB')
CATALOG1_CONTEXT = Contexts.CATALOG1_CONTEXT.new(name='CATALOG1')

JSON_REST_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='JSON_REST_CONTEXT',
                                                         service=const.Services.TEST_SERVICE,
                                                         product=const.Products.PRODUCT_TEST_SERVICE,
                                                         price=Decimal('30.0'))

CONSUME_QTY = Decimal('101.1111')
COMPLETION_QTY = Decimal('80.4448')
TRANSFER_QTY = CONSUME_QTY - COMPLETION_QTY
OF = steps.ObjectFactory(consume_qty=CONSUME_QTY, completion_qty=COMPLETION_QTY)

pytestmark = [pytest.mark.slow,
              reporter.feature(Features.NOTIFICATION),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/notification/')]


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY)
@pytest.mark.docs(u'--group', u'при включении \ оплате счёта')
@pytest.mark.ignore_hosts(hosts.PT, hosts.PTY, hosts.PTA)
@pytest.mark.parametrize(*utils.Pytest.combine(
    utils.Pytest.ParamsSet(names='context',
                           values=[
                               DIRECT_FISH_RUB_CONTEXT,
                               DIRECT_FISH_USD_CONTEXT,
                               DIRECT_MONEY_RUB_CONTEXT,
                               DIRECT_MONEY_USD_CONTEXT,
                               pytest.mark.smoke(MARKET_RUB_CONTEXT),
                               # Notificationf for 5 and 6 services use legacy code.
                               # Without logging to t_notification_log. Commented till redesign.
                               # CATALOG1_CONTEXT,
                               # CATALOG1_CONTEXT,
                               # CATALOG1_CONTEXT,
                               # CATALOG1_CONTEXT,
                               # CATALOG1_CONTEXT
                           ]),
    utils.Pytest.ParamsSet(names='prepare_order',
                           values=[
                               OF.new_order,
                               OF.order_with_consume,
                               OF.order_with_consume_and_completion,
                               OF.order_with_several_consumes,
                               OF.order_with_several_consumes_and_completions
                           ])),
                         ids=lambda context, prepare_order: '{}-{}'.format(context.name, prepare_order.__name__))
def test_notify_order_on_consume(context, prepare_order):
    order = prepare_order(context)

    trigger_notification(lambda: steps.put_consume_on_order(context=context, order=order, consume_qty=CONSUME_QTY))

    check_notification(order=order)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY, Features.CONVERSION)
@pytest.mark.docs(u'--group', u'Конвертация клиента')
@pytest.mark.parametrize(*utils.Pytest.combine(
    ('context, currency_context', [
        (DIRECT_FISH_RUB_CONTEXT,
         DIRECT_MONEY_RUB_CONTEXT
                ),
    ]),
    ('prepare_order', [
        OF.new_order,
        OF.order_with_consume,
        OF.order_with_consume_and_completion,
        OF.order_with_several_consumes_and_completions  # igogor убрал т.к. шумит
    ])),
                         ids=lambda context, currency_context, prepare_order: '{}-{}'.format(context.name,
                                                                                             prepare_order.__name__))
def test_notify_on_conversion(context, currency_context, prepare_order):
    order = prepare_order(context)

    trigger_notification(lambda: steps.convert_to_currency(context=currency_context,
                                                           client=order.client,
                                                           orders=[order]))
    check_notification(order=order)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY,
                  Features.CONVERSION, Features.TRANSFER)
@pytest.mark.docs(u'--group', u'при переносе с\на заказ')
@pytest.mark.parametrize(*utils.Pytest.combine(
    ('context', [
        DIRECT_FISH_RUB_CONTEXT,
        DIRECT_FISH_USD_CONTEXT,
        DIRECT_MONEY_RUB_CONTEXT,
        DIRECT_MONEY_USD_CONTEXT,
        MARKET_RUB_CONTEXT
    ]),
    ('prepare_from_order, prepare_to_order', [
        (OF.order_with_consume, OF.new_order),
        (OF.order_with_consume_and_completion, OF.order_with_consume_and_completion),
    ])),
                         ids=lambda context, prepare_from_order, prepare_to_order: '{}:{}-{}'.format(
                             context.name, prepare_from_order.__name__, prepare_to_order.__name__))
def test_notify_order_on_transfer(context, prepare_from_order, prepare_to_order):
    from_order = prepare_from_order(context)
    to_order = prepare_to_order(context, client=from_order.client)

    trigger_notification(lambda: steps.transfer(context, from_order, to_order, qty=TRANSFER_QTY))

    check_notification(order=from_order)

    check_notification(order=to_order)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY, Features.TRANSFER)
@pytest.mark.docs(u'--group', u'при переносе с\на заказ')
def test_notify_order_on_transfer_converted_to_currency():
    context = DIRECT_FISH_RUB_CONTEXT
    currency_context = DIRECT_MONEY_RUB_CONTEXT

    from_order = OF.converted(OF.order_with_consume, context=context, currency_context=currency_context)
    to_order = OF.new_order(currency_context, client=from_order.client)

    trigger_notification(lambda: steps.transfer(currency_context, from_order, to_order, qty=TRANSFER_QTY,
                                                conversion_context=context))

    check_notification(order=from_order)

    check_notification(order=to_order)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY, Features.TRANSFER)
@pytest.mark.docs(u'--group', u'при переносе с\на заказ')
def test_notify_order_on_transfer_converted_to_converted():
    context = DIRECT_FISH_RUB_CONTEXT
    currency_context = DIRECT_MONEY_RUB_CONTEXT

    from_order = OF.order_with_consume(context)
    to_order = OF.order_with_consume(context, client=from_order.client)
    steps.convert_to_currency(currency_context, client=from_order.client, orders=[from_order, to_order])

    trigger_notification(lambda: steps.transfer(currency_context, from_order, to_order, qty=TRANSFER_QTY,
                                                conversion_context=context))

    check_notification(order=from_order)

    check_notification(order=to_order)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY, Features.TRANSFER)
@pytest.mark.docs(u'--group', u'при переносе с\на заказ')
def test_notify_order_on_transfer_currency_to_converted():
    context = DIRECT_FISH_RUB_CONTEXT
    currency_context = DIRECT_MONEY_RUB_CONTEXT

    to_order = OF.converted(OF.order_with_consume_and_completion, context=context, currency_context=currency_context)
    from_order = OF.order_with_consume(currency_context, client=to_order.client)

    trigger_notification(lambda: steps.transfer(currency_context, from_order, to_order, qty=TRANSFER_QTY,
                                                conversion_context=context))

    check_notification(order=from_order)

    check_notification(order=to_order)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY, Features.UNIFIED_ACCOUNT)
@pytest.mark.docs(u'--group', u'Нотификации и единый счет')
@pytest.mark.tickets('BALANCE-22232, BALANCE-22629')
@pytest.mark.parametrize(*utils.Pytest.combine(
    ('context', [
        DIRECT_FISH_RUB_CONTEXT,
        DIRECT_MONEY_RUB_CONTEXT,
        # pytest.mark.skip(MARKET_RUB_CONTEXT)
    ]),
    ('group_name, prepare_parent, prepare_members', [
        ('empty_parent_and_member', OF.new_order, [OF.new_order]),
        ('member_with_consume', OF.new_order, [OF.order_with_consume]),
        ('both_with_completions', OF.order_with_consume_and_completion, [OF.order_with_consume_and_completion]),
        ('several_members_with_consumes', OF.order_with_consume, [OF.order_with_consume,
                                                                  OF.order_with_consume_and_completion]),
    ])),
                         ids=lambda context, group_name, prepare_parent, prepare_members: '{}:{}'.format(
                             context.name, group_name))
def test_notify_group_on_grouping(prepare_parent, prepare_members, context, group_name):
    parent = prepare_parent(context)
    members = [prepare_member(context, client=parent.client) for prepare_member in prepare_members]

    trigger_notification(lambda: steps.group_orders(parent=parent, members=members))

    check_notification(order=parent)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY,
                  Features.UNIFIED_ACCOUNT, Features.TRANSFER)
@pytest.mark.docs(u'--group', u'Нотификации и единый счет')
@pytest.mark.parametrize(*utils.Pytest.combine(
    ('context', [
        # DIRECT_FISH_RUB_CONTEXT,
        # DIRECT_MONEY_RUB_CONTEXT,
        pytest.mark.skip(
            MARKET_RUB_CONTEXT
        )
    ]),
    ('group_name, prepare_parent, prepare_members', [
        ('member_with_consume', OF.new_order, [OF.order_with_consume]),
        ('both_with_completions', OF.order_with_consume_and_completion, [OF.order_with_consume_and_completion]),
        pytest.mark.smoke(
            ('several_members_with_consumes', OF.order_with_consume, [OF.order_with_consume,
                                                                      OF.order_with_consume_and_completion])),
    ])),
                         ids=lambda context, group_name, prepare_parent, prepare_members: '{}:{}'.format(
                             context.name, group_name))
def test_notify_group_on_group_transfer(prepare_parent, prepare_members, context, group_name):
    parent = prepare_parent(context)
    members = [prepare_member(context, client=parent.client) for prepare_member in prepare_members]
    steps.group_orders(parent=parent, members=members)

    trigger_notification(lambda: steps.group_transfer(parent=parent))

    check_notification(order=parent)

    for member in parent.group_members:
        check_notification(order=member)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY, Features.UNIFIED_ACCOUNT)
@pytest.mark.docs(u'--group', u'Нотификации и единый счет')
@pytest.mark.parametrize('context',
                         [
                             DIRECT_FISH_RUB_CONTEXT,
                             DIRECT_MONEY_RUB_CONTEXT,
                             # pytest.mark.skip(MARKET_RUB_CONTEXT)
                         ],
                         ids=lambda context: context.name)
def test_notify_group_on_member_consume(context):
    parent = OF.new_order(context)
    member = OF.new_order(context, client=parent.client)
    steps.group_orders(parent=parent, members=[member])

    trigger_notification(lambda: steps.put_consume_on_order(context=context, order=member, consume_qty=CONSUME_QTY))
    # при консьюме на дочерний заказ зачисляется на родительский
    parent.on_group_transfer()

    check_notification(order=parent)
    check_notification(order=member)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY, Features.UNIFIED_ACCOUNT)
@pytest.mark.docs(u'--group', u'Нотификации и единый счет')
@pytest.mark.parametrize(*utils.Pytest.combine(
    ('context', [
        DIRECT_FISH_RUB_CONTEXT,
        DIRECT_MONEY_RUB_CONTEXT,
        NAVIGATOR_FISH
        # (MARKET_RUB_CONTEXT, NotificationType.NON_CURRENCY) # todo-igogor чет не работает
    ]),
    ('group_name, prepare_parent, prepare_members', [
        ('empty_parent_and_member', OF.new_order, [OF.new_order]),
        ('member_with_consume', OF.new_order, [OF.order_with_consume]),
    ])),
                         ids=lambda context, group_name, prepare_parent, prepare_members: '{}:{}'.format(
                             context.name, group_name))
def test_notify_group_on_parent_consume(prepare_parent, prepare_members, context, group_name):
    parent = prepare_parent(context)
    members = [prepare_member(context, client=parent.client) for prepare_member in prepare_members]
    steps.group_orders(parent=parent, members=members)

    trigger_notification(lambda: steps.put_consume_on_order(context=context, order=parent, consume_qty=CONSUME_QTY))

    check_notification(order=parent)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY,
                  Features.UNIFIED_ACCOUNT, Features.CONVERSION)
@pytest.mark.docs(u'--group', u'Нотификации и единый счет')
@pytest.mark.parametrize(*utils.Pytest.combine(
    utils.Pytest.ParamsSet(names='context, currency_context',
                           values=[
                               (DIRECT_FISH_RUB_CONTEXT, DIRECT_MONEY_RUB_CONTEXT)
                           ]),
    utils.Pytest.ParamsSet(names='group_name, prepare_parent, prepare_members',
                           values=[
                               ('empty_parent_and_member', OF.new_order, [OF.new_order]),
                               ('member_with_several_completions', OF.new_order,
                                [OF.order_with_several_consumes_and_completions]),
                               ('parent_and_member_with_completions', OF.order_with_consume_and_completion,
                                [OF.order_with_consume_and_completion])
                           ])),
                         ids=lambda context, currency_context, group_name, prepare_parent,
                                    prepare_members: '{}:{}'.format(context.name, group_name))
def test_notify_group_on_conversion_end(context, currency_context, prepare_parent, prepare_members, group_name):
    parent = prepare_parent(context)
    members = [prepare_member(context, client=parent.client) for prepare_member in prepare_members]
    steps.group_orders(parent=parent, members=members)

    trigger_notification(lambda: steps.convert_to_currency(currency_context, parent.client, orders=parent.group))

    with reporter.step(u'Проверяем нотификации по родительскому заказу'):
        # при конвертации группового заказа происходит неявный перенос
        parent.on_group_transfer()
        check_notification(order=parent, descr=u'при окончании конвертации')
    with reporter.step(u'Проверяем нотификации по дочерним заказам'):
        for member in parent.group_members:
            check_notification(order=member, descr=u'при окончании конвертации')


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY,
                  Features.UNIFIED_ACCOUNT, Features.CONVERSION)
@pytest.mark.docs(u'--group', u'Нотификации и единый счет')
@pytest.mark.parametrize(*utils.Pytest.combine(
    utils.Pytest.ParamsSet(names='context, currency_context',
                           values=[
                               (DIRECT_FISH_RUB_CONTEXT, DIRECT_MONEY_RUB_CONTEXT)
                           ]),
    utils.Pytest.ParamsSet(names='group_name, prepare_parent, prepare_members',
                           values=[
                               ('empty_parent_and_member', OF.new_order, [OF.new_order]),
                               ('member_with_several_completions', OF.new_order,
                                [OF.order_with_several_consumes_and_completions]),
                               ('parent_and_member_with_completions', OF.order_with_consume_and_completion,
                                [OF.order_with_consume_and_completion])
                           ])),
                         ids=lambda context, currency_context, group_name, prepare_parent,
                                    prepare_members: '{}:{}'.format(context.name, group_name))
def test_notify_group_on_conversion_start(prepare_parent, prepare_members, context, currency_context, group_name):
    parent = prepare_parent(context)
    members = [prepare_member(context, client=parent.client) for prepare_member in prepare_members]
    steps.group_orders(parent=parent, members=members)

    trigger_notification(lambda: steps.convert_to_currency(currency_context, parent.client, orders=parent.group,
                                                           migrate_to_currency=datetime.now() + timedelta(minutes=5)))

    with reporter.step(u'Проверяем нотификации по родительскому заказу'):
        # при конвертации группового заказа происходит неявный перенос
        parent.on_group_transfer()
        check_notification(order=parent,
                           corrections={'CompletionFixedMoneyQty': '0',
                                        'CompletionFixedQty': '0',
                                        'TotalConsumeQty': _number_str(parent.group_consume_qty)},
                           descr=u' при постановке в очередь на ковертацию')
    with reporter.step(u'Проверяем нотификации по дочерним заказам'):
        for member in parent.group_members:
            check_notification_on_conversion_start(order=member)


@pytest.mark.smoke
@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY,
                  Features.UNIFIED_ACCOUNT, Features.CONVERSION)
@pytest.mark.docs(u'--group', u'Нотификации и единый счет')
def test_notify_group_on_consume_with_converted_member():
    context = DIRECT_FISH_RUB_CONTEXT
    currency_context = DIRECT_MONEY_RUB_CONTEXT

    converted_member = OF.converted(OF.order_with_consume_and_completion, context=context,
                                    currency_context=currency_context)
    currency_parent = OF.new_order(currency_context, client=converted_member.client)
    steps.group_orders(parent=currency_parent, members=[converted_member])

    trigger_notification(lambda: steps.put_consume_on_order(context=currency_context, order=currency_parent,
                                                            consume_qty=CONSUME_QTY))

    check_notification(order=currency_parent)


@reporter.feature(Features.NOTIFICATION, Features.MULTICURRENCY,
                  Features.UNIFIED_ACCOUNT, Features.CONVERSION)
@pytest.mark.docs(u'--group', u'Нотификации и единый счет')
def test_notify_group_on_consume_with_converted_parent():
    context = DIRECT_FISH_RUB_CONTEXT
    currency_context = DIRECT_MONEY_RUB_CONTEXT

    converted_parent = OF.converted(OF.order_with_consume_and_completion, context=context,
                                    currency_context=currency_context)
    currency_member = OF.order_with_consume(currency_context, client=converted_parent.client)
    steps.group_orders(parent=converted_parent, members=[currency_member])

    trigger_notification(lambda: steps.put_consume_on_order(context=context,
                                                            order=converted_parent,
                                                            consume_qty=CONSUME_QTY))

    check_notification(order=converted_parent)


@reporter.feature(Features.NOTIFICATION)
@pytest.mark.docs(u'--group', u'Нотификации и единый счет')
@pytest.mark.tickets('BALANCE-22427')
def test_notify_order_json():

    context = DIRECT_FISH_RUB_CONTEXT

    validate_notification_protocol(service=context.service, is_json_rest=True)

    order = OF.order_with_consume_and_completion(context)

    trigger_notification(lambda: steps.put_consume_on_order(context=context, order=order, consume_qty=CONSUME_QTY))

    check_notification(order=order)


def trigger_notification(trigger):
    with reporter.step(u'Инициируем нотификацию:'):
        return trigger()


def validate_notification_protocol(service, is_json_rest):
    protocol = 'json-rest' if is_json_rest else 'xml-rpc'
    with reporter.step(u'Валидируем, что для сервиса {} протокол нотификаций: {}'.format(service, protocol)):
        result = db.balance().execute(query="SELECT protocol "
                                            "FROM T_SERVICE_NOTIFY_PARAMS WHERE SERVICE_ID = {}".format(service.id),
                                      single_row=True)
        utils.check_condition(result['protocol'], equal_to(protocol))


def check_notification(order, corrections=None, descr=u''):
    with reporter.step(u'Проверяем нотификацию для заказа {} - {}'.format(order, descr)):
        expected_notification = build_notification(order)
        if corrections:
            expected_notification.update(corrections)
        reporter.log('expected_notification = {}'.format(utils.Presenter.pretty(expected_notification)))
        utils.check_that(CommonSteps.build_notification(1, order.id)['args'],
                         contains_dicts_equal_to([expected_notification], same_length=False),
                         error=u'Проверяем, что среди отправленных есть нотификация с нужными значениями')


def check_notification_on_conversion_start(order):
    check_notification(order=order,
                       # при конвертации отправляется 2 нотификации, а не 1, но проверять надо обе
                       # добавлять в подсчет нотификаций эту логику слишком сложно, поэтому подправляем вручную
                       corrections={'CompletionFixedMoneyQty': '0',
                                    'CompletionFixedQty': '0'},
                       descr=u'при постановке на конвертацию')


def _number_str(value):
    if int(value) == value:
        # целые значения отображаются без .0
        return str(int(value))
    else:
        # к float приводится т.к. у Decimal местами странное представление - '4826.68800'
        return str(float(value))


def _number_str_precision(value, precision=4):
    if int(value) == value:
        # целые значения отображаются без .0
        return str(int(value))
    else:
        # к float приводится т.к. у Decimal местами странное представление - '4826.68800'
        return "{:10.4f}".format((float(value))).strip()


def build_notification(order):
    # Подобный подход когда в одном месте в зависимости от условий формируется эталон
    # Он нагляден - видна логика.
    # Но он содержит логику!!!, т.е. надо дублировать логику биллинга, что грех.
    # Но я не могу придумать как удобно этого не делать, т.к. без этого делать параметризацию - адище.
    base_notification = {'CompletionQty': _number_str(order.completion_qty),
                         'ConsumeQty': _number_str(order.consume_qty),
                         'ConsumeSum': _number_str(order.consume_money_rounded),
                         'ServiceID': order.service.id,
                         'ServiceOrderID': order.service_order_id,
                         'Signal': 1,
                         'SignalDescription': 'Order balance have been changed',
                         'Tid': anything()
                         }

    if order.consumes:
        base_notification.update({'ConsumeCurrency': order.currency.iso_code,
                                  'ConsumeAmount': _number_str(order.consume_money_rounded)})

    if order.service == const.Services.DIRECT:
        base_notification.update({'ProductCurrency': ''})
        base_notification.update({'CashbackCurrentQty': '0'})

    if order.is_multicurrency:
        base_notification.update({'ProductCurrency': order.currency.iso_code})

    if order.is_converted:
        base_notification.update({'CompletionFixedMoneyQty': _number_str_precision(order.completion_money),
                                  'CompletionFixedQty': _number_str(order.completion_qty),
                                  'ConsumeMoneyQty': _number_str_precision(order.consume_money)})

    if order.is_main_order:
        total_consume_qty = order.group_consume_money if (order.is_multicurrency or order.is_converted) \
            else order.group_consume_qty

        base_notification.update({'TotalConsumeQty': _number_str(total_consume_qty)})
        if order.service == const.Services.DIRECT:
            base_notification.update({'TotalCashbackCurrentQty': '0'})

    return base_notification


if __name__ == "__main__":
    pytest.main("-v test_notify_order.py -n 8")
