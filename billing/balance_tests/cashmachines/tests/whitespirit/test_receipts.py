# coding: utf-8
__author__ = 'a-vasin'

from datetime import date, datetime
from multiprocessing import Pool

import pytest
from hamcrest import equal_to, greater_than_or_equal_to, matches_regexp, anything, contains_string, has_item, \
    only_contains
from requests.exceptions import ReadTimeout

import cashmachines.whitespirit_steps as steps
from btestlib.matchers import matcher_for, matches_in_time
from cashmachines.data import defaults
from cashmachines.data.constants import *
from cashmachines.data.defaults import PRICE, QTY
from cashmachines.data.passwords import SUPER_SECRET_AUTORU_TVM_TIKEN
from simpleapi.matchers.deep_equals import deep_contains, deep_equals_to


def test_single_receipt_content():
    receipt = steps.CMSteps.make_single_receipt(PRICE, QTY)
    expected_receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]))
    utils.check_that(receipt, deep_contains(expected_receipt_content),
                     u'Проверяем, что чек содержит ожидаемые значения')


def test_receipt_static_params():
    receipt = steps.CMSteps.make_single_receipt(PRICE, QTY)
    expected_receipt_params = create_receipt_content_template(receipt)
    utils.check_that(receipt, deep_equals_to(expected_receipt_params),
                     u'Проверяем, что чек содержит ожидаемые значения')


def test_multiple_receipts_content():
    prices = [PRICE, 2 * PRICE]
    qtys = [QTY, 2 * QTY]

    receipt = steps.CMSteps.make_receipts(prices, qtys)
    expected_receipt_content = defaults.receipts(defaults.rows(prices, qtys), defaults.payments([5 * PRICE * QTY]))
    utils.check_that(receipt, deep_contains(expected_receipt_content),
                     u'Проверяем, что чек содержит ожидаемые значения')


@pytest.mark.parametrize('nds', CMNds.values(), ids=lambda nds: nds.name)
def test_single_receipt_calculated_content(nds):
    receipt = steps.CMSteps.make_single_receipt(PRICE, QTY, nds)
    expected_receipt_calculated_content = create_expected_receipt_calculated_content(receipt, [PRICE], [QTY], [nds])
    utils.check_that(receipt, deep_contains(expected_receipt_calculated_content),
                     u'Проверяем, что вычисленные данные чека корректны')


def test_multiple_receipt_calculated_content():
    ndses = CMNds.values() * 2
    prices = [i * PRICE for i in xrange(1, len(ndses) + 1)]
    qtys = [i * QTY for i in xrange(1, len(ndses) + 1)]

    receipt = steps.CMSteps.make_receipts(prices, qtys, ndses)
    expected_receipt_calculated_content = create_expected_receipt_calculated_content(receipt, prices, qtys, ndses)
    utils.check_that(receipt, deep_contains(expected_receipt_calculated_content),
                     u'Проверяем, что вычисленные данные чека корректны')


@pytest.mark.parametrize(
    'payment_amount',
    [QTY * PRICE / 2, QTY * PRICE * 2],
    ids=[
        'LESSER_THAN_ORDER',
        'GREATER_THAN_ORDER'
    ]
)
def test_payment_amount_differs_from_order_amount(payment_amount):
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([payment_amount]))

    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content)
    utils.check_that(response.get('value', None), "payments amount != rows amount",
                     u'Проверяем, что получили ожидаемую ошибку')


# a-vasin: точность кассы для цены - 2 знака, для количества - 3 знака
@pytest.mark.parametrize('price, qty, expected_error',
                         [
                             (10001, 0.0001,
                              u"{'receipt_content': DataError({'payments': DataError({0: DataError({'amount': DataError(precision not 0.01)})}), 'rows': DataError({0: DataError({'qty': DataError(value is less than 0.001)})})})}"),
                             (0.001, 1001,
                              u"{'receipt_content': DataError({'payments': DataError({0: DataError({'amount': DataError(precision not 0.01)})}), 'rows': DataError({0: DataError({'price': DataError(precision not 0.01)})})})}"),
                             (0.001, 0.0001,
                              u"{'receipt_content': DataError({'payments': DataError({0: DataError({'amount': DataError(precision not 0.01)})}), 'rows': DataError({0: DataError({'price': DataError(precision not 0.01), 'qty': DataError(value is less than 0.001)})})})}")
                         ],
                         ids=[
                             'QTY',
                             'PRICE',
                             'QTY_AND_PRICE'
                         ])
def test_too_small_values(price, qty, expected_error):
    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.CMSteps.make_single_receipt(price, qty)
    utils.check_that(response.get('value', None), equal_to(expected_error), u'Проверяем, что получили ожидаемую ошибку')


@pytest.mark.parametrize('price, qty, expected_error',
                         [
                             (-PRICE, QTY,
                              u"{'receipt_content': DataError({'payments': DataError({0: DataError({'amount': DataError(value is less than 0)})}), 'rows': DataError({0: DataError({'price': DataError(value is less than 0)})})})}"),
                             (PRICE, -QTY,
                              u"{'receipt_content': DataError({'payments': DataError({0: DataError({'amount': DataError(value is less than 0)})}), 'rows': DataError({0: DataError({'qty': DataError(value is less than 0.001)})})})}"),
                             (-PRICE, -QTY,
                              u"{'receipt_content': DataError({'rows': DataError({0: DataError({'price': DataError(value is less than 0), 'qty': DataError(value is less than 0.001)})})})}")
                         ],
                         ids=[
                             'PRICE',
                             'QTY',
                             'PRICE_QTY'
                         ])
def test_negative_values(price, qty, expected_error):
    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.CMSteps.make_single_receipt(price, qty)
    utils.check_that(response.get('value', None), equal_to(expected_error), u'Проверяем, что получили ожидаемую ошибку')


def test_incorrect_symbols():
    rows = defaults.rows([PRICE], [QTY])
    rows[0]['text'] += u'»_«'

    expected_receipt_content = defaults.receipts(rows, defaults.payments([PRICE * QTY]))
    receipt_content = steps.ReceiptSteps.receipts(expected_receipt_content)

    utils.check_that(receipt_content, deep_contains(expected_receipt_content),
                     u'Проверяем, что чек содержит ожидаемые значения')


def test_incorrect_amount():
    error_text = u"{'receipt_content': DataError({'rows': DataError({0: DataError({'price': DataError([<class 'decimal.ConversionSyntax'>])})})})}"

    rows = defaults.rows([PRICE], [QTY])
    rows[0]['price'] = u''
    receipt_content = defaults.receipts(rows, defaults.payments([PRICE * QTY]))

    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content)
    utils.check_that(response.get('value', None), equal_to(error_text), u'Проверяем, что получили ожидаемую ошибку')


def test_multiple_payments():
    rows = defaults.rows([PRICE], [QTY])
    payments = defaults.payments([PRICE * QTY / 2] * 2)
    receipt_content = defaults.receipts(rows, payments)

    receipt = steps.ReceiptSteps.receipts(receipt_content)

    utils.check_that(receipt, deep_contains(receipt_content), u'Проверяем, что чек содержит ожидаемые значения')


def test_text_length_overflow():
    size = 235

    rows = defaults.rows([PRICE], [QTY])
    payments = defaults.payments([PRICE * QTY])
    rows[0]['text'] = u'x' * size
    receipt_content = defaults.receipts(rows, payments)

    receipt = steps.ReceiptSteps.receipts(receipt_content)

    utils.check_that(receipt, deep_contains(receipt_content), u'Проверяем, что чек содержит ожидаемые значения')


@pytest.mark.parametrize('payment_type, expected_money_received_total', [
    (PaymentType.CARD, PRICE * QTY),
    (PaymentType.PREPAYMENT, 0),
    (PaymentType.EXTENSION, 0),
    (PaymentType.CREDIT, 0)
], ids=lambda pt, _: PaymentType.name(pt))
def test_payment_types(payment_type, expected_money_received_total):
    rows = defaults.rows([PRICE], [QTY])
    payments = defaults.payments([PRICE * QTY], [payment_type])
    receipt_content = defaults.receipts(rows, payments)

    receipt = steps.ReceiptSteps.receipts(receipt_content)

    expected_receipt_calculated_content = \
        create_expected_receipt_calculated_content(receipt, [PRICE], [QTY], payment_type=payment_type)
    receipt_content.update(expected_receipt_calculated_content)
    receipt_content['receipt_calculated_content']['money_received_total'] = unicode(expected_money_received_total)

    utils.check_that(receipt, deep_contains(receipt_content), u'Проверяем, что чек содержит ожидаемые значения')


@pytest.mark.parametrize('payment_type_type', PaymentTypeType.values())
def test_payment_type_types(payment_type_type):
    rows = defaults.rows([PRICE], [QTY], payment_type_types=[payment_type_type])
    payments = defaults.payments([PRICE * QTY])
    receipt_content = defaults.receipts(rows, payments)

    receipt = steps.ReceiptSteps.receipts(receipt_content)

    expected_receipt_calculated_content = \
        create_expected_receipt_calculated_content(receipt, [PRICE], [QTY], payment_type_types=[payment_type_type])
    receipt_content.update(expected_receipt_calculated_content)

    utils.check_that(receipt, deep_contains(receipt_content), u'Проверяем, что чек содержит ожидаемые значения')


def test_multiple_receipt_payment_type_types():
    payment_type_types = [
        PaymentTypeType.PREPAYMENT,
        PaymentTypeType.FULL_PREPAYMENT_WO_DELIVERY,
        PaymentTypeType.IP_PAYMENT,
        PaymentTypeType.PARTIAL_PREPAYMENT_WO_DELIVERY,
        PaymentTypeType.FULL_PAYMENT_W_DELIVERY,
        PaymentTypeType.PARTIAL_PAYMENT_W_DELIVERY,
        PaymentTypeType.CREDIT_W_DELIVERY
    ]
    prices = [i * PRICE for i in xrange(1, len(payment_type_types) + 1)]
    qtys = [i * QTY for i in xrange(1, len(payment_type_types) + 1)]

    rows = defaults.rows(prices, qtys, payment_type_types=payment_type_types)
    payments = defaults.payments([sum([price * qty for price, qty in zip(prices, qtys)])])
    receipt_content = defaults.receipts(rows, payments)

    receipt = steps.ReceiptSteps.receipts(receipt_content)

    expected_receipt_calculated_content = create_expected_receipt_calculated_content(receipt, prices, qtys,
                                                                                     payment_type_types=payment_type_types)
    receipt_content.update(expected_receipt_calculated_content)
    utils.check_that(receipt, deep_contains(receipt_content),
                     u'Проверяем, что вычесленные данные чека корректны')


def test_credit_after_delivery_payment_type_types():
    payment_type_types = [
        PaymentTypeType.CREDIT_W_DELIVERY,
        PaymentTypeType.CREDIT_AFTER_DELIVERY
    ]
    prices = [PRICE] * len(payment_type_types)
    qtys = [QTY] * len(payment_type_types)

    rows = defaults.rows(prices, qtys, payment_type_types=payment_type_types)
    payments = defaults.payments([sum([price * qty for price, qty in zip(prices, qtys)])])
    receipt_content = defaults.receipts(rows, payments)

    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content)
    utils.check_that(response.get('value', None), contains_string(u"Параметр команды содержит неверные данные"),
                     u'Проверяем, что получили ожидаемую ошибку')


@pytest.mark.parametrize('receipt_type', ReceiptType.values())
def test_receipt_type(receipt_type):
    rows = defaults.rows([PRICE], [QTY])
    payments = defaults.payments([PRICE * QTY])
    receipt_content = defaults.receipts(rows, payments, receipt_type=receipt_type)

    receipt = steps.ReceiptSteps.receipts(receipt_content)
    utils.check_that(receipt, deep_contains(receipt_content), u'Проверяем, что чек содержит ожидаемые значения')


@pytest.mark.parametrize('taxation_type', [
    TaxationType.USN_I,
    TaxationType.USN_IMC,
    TaxationType.ESN_CI,
    TaxationType.ESN_A,
    TaxationType.PATENT
])
def test_forbidden_taxation_type(taxation_type):
    rows = defaults.rows([PRICE], [QTY])
    payments = defaults.payments([PRICE * QTY])
    receipt_content = defaults.receipts(rows, payments, taxation_type=taxation_type)

    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content)
    # a-vasin: TODO это недопилено, потому что не пофикшено и никакая ошибка не возвращается
    utils.check_that(response.get('value', None), equal_to(u"Some error"), u'Проверяем, что получили ожидаемую ошибку')


@pytest.mark.parametrize('agent_type', [
    AgentType.PAYMENT_BANK_AGENT,
    AgentType.PAYMENT_BANK_SUBAGENT,
    AgentType.PAYMENT_AGENT,
    AgentType.PAYMENT_SUBAGENT,
    AgentType.CONFIDANT_AGENT,
    AgentType.COMMISSION_AGENT
])
def test_forbidden_agent_type(agent_type):
    rows = defaults.rows([PRICE], [QTY])
    payments = defaults.payments([PRICE * QTY])
    receipt_content = defaults.receipts(rows, payments, agent_type=agent_type)

    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content)
    utils.check_that(response.get('value', None), equal_to(u"Some error"), u'Проверяем, что получили ожидаемую ошибку')


@pytest.mark.parametrize("group", Group.values())
def test_groups(group):
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]))
    steps.ReceiptSteps.receipts(receipt_content, group)


@pytest.mark.parametrize("group, error", [
    ('not_existing_group', u"NoFreeKKT"),
    ('!!!', u'BadDataInput')
], ids=['MISSING', 'INCORRECT'])
def test_wrong_group(group, error):
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]))
    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content, group)
    utils.check_that(response.get('error', None), equal_to(error), u'Проверяем, что получили ожидаемую ошибку')


def test_wrong_tvm_token():
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]))
    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content, tvm_token='Wrong token')
    utils.check_that(response.get('value', None), contains_string(u'Malformed ticket'),
                     u'Проверяем, что получили ожидаемую ошибку')


def test_expired_token():
    expired_token = "3:serv:CKcVEPm1k9QFIggI54t6EOaLeg:BDp4Av5O1YX19HbdMGda71AfJnshTSX_wyJarmaatZ0pybSFCilosGE8gjDz8MJ3b102qM43hyJWO6kGr2OuEF5KmGKqrPbg_kidBX8yhuDaRqMqtcV6tCSlUiCn0RgtXguj-mEbhHIPYlRLeAWlYc3O3hJ7-1Q5aydKXa6ZXP4"

    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]))
    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content, tvm_token=expired_token)
    utils.check_that(response.get('value', None), contains_string(u'Expired ticket'),
                     u'Проверяем, что получили ожидаемую ошибку')


# a-vasin: просто смотрим, что не упало
def test_autoru_tvm_token():
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]))
    steps.ReceiptSteps.receipts(receipt_content, tvm_token=SUPER_SECRET_AUTORU_TVM_TIKEN)


def test_specified_sn():
    serial_number = steps.CMSteps.get_random_sn()
    conditional = defaults.conditional(accept_device=defaults.sn_condition(serial_number))
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]),
                                        conditional=conditional)
    receipt = steps.ReceiptSteps.receipts(receipt_content)
    utils.check_that(receipt['kkt']['sn'], equal_to(serial_number), u'Проверяем, что выбрана заданная касса')


@pytest.mark.skip("Too slow. Add some lost receipts clean-up")
def test_lost_receipt():
    receipt_id = unicode(utils.random_string(256))
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]))
    receipt_content['receipt_content']['additional_user_requisite']['value'] = receipt_id

    with pytest.raises(ReadTimeout):
        steps.ReceiptSteps.receipts(receipt_content, timeout=0.1)

    item_matcher = matcher_for(lambda item: item['receipt_content']['additional_user_requisite']['value'] == receipt_id)
    utils.check_that(lambda: steps.DebugSteps.lost_receipts()['lost_receipts'],
                     matches_in_time(has_item(item_matcher)),
                     u"Проверяем, что потерянный чек будет залогирован")


@pytest.mark.parametrize("field_name, expected_error", [
    ("firm_reply_email", {u'receipt_request': {u'receipt_content': [u"'firm_reply_email' is a required property"]}}),
    ("firm_url", {u'receipt_request': {u'receipt_content': [u"'firm_url' is a required property"]}})
], ids=lambda fn: fn.upper())
def test_required_fields(field_name, expected_error):
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]))
    del receipt_content['receipt_content'][field_name]

    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content)

    utils.check_that(response.get('errors', None), equal_to(expected_error),
                     u"Проверяем, что без обязтельного поля чек не бьется")


@pytest.mark.parametrize("phone_number, expected_error", [
    (u"+", "{'receipt_content': DataError({'supplier_phone': DataError({0: DataError(String is shorter than 3 characters), 1: DataError(value should be None)})})}"),
    # (u"666", "something"),
    # (u"phone_number", "something"),
    (u"+1111111111111111111", "{'receipt_content': DataError({'supplier_phone': DataError({0: DataError(String is longer than 19 characters), 1: DataError(value should be None)})})}"),

    (u"+111111111111111111", None),
    (u"+55", None),
    (u"+79275689855", None)
], ids=lambda fn: fn.upper())
def test_phone_number_format(phone_number, expected_error):
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]))
    receipt_content['receipt_content']['supplier_phone'] = phone_number

    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.receipts(receipt_content)

    utils.check_that(response.get('value', None), equal_to(expected_error),
                     u"Проверяем, что телефон валидируется")


# a-vasin: многопоточность!!!!111111одинодинодин
def test_wait_for_free():
    receipts_number = 3
    wait4free = 3
    wait_time = 1

    serial_number = steps.CMSteps.get_random_sn()
    conditional = defaults.conditional(after_select_device=wait_time,
                                       accept_device=defaults.sn_condition(serial_number))
    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]),
                                        conditional=conditional)

    pool = Pool(processes=receipts_number)
    results = [pool.apply_async(get_receipt_async, (receipt_content, wait4free)) for _ in range(receipts_number)]
    receipts = [res.get() for res in results]

    utils.check_that(receipts, only_contains(steps.FAILURE_MATCHER), u"Проверяем, что все чеки пробились")


# a-vasin: просто смотрим, что не падает, т.к. ничего кроме логов не затрагивает
def test_trace():
    trace = {
        "service_id": "test_service_id",
        "id": 234L,
        123: (321, 145),
        0.5: [u"17.8"],
        u"321": 0.001,
        "413": {"213": "123"}
    }

    receipt_content = defaults.receipts(defaults.rows([PRICE], [QTY]), defaults.payments([PRICE * QTY]), trace=trace)
    steps.ReceiptSteps.receipts(receipt_content)


# -------------------------------------
# Utils

# a-vasin: нужно объявить на топ левеле, а то не заработает =(
def get_receipt_async(receipt_content, wait4free):
    with utils.check_mode(utils.CheckMode.IGNORED):
        return steps.ReceiptSteps.receipts(receipt_content, wait4free=wait4free)


def create_expected_receipt_calculated_content(receipt, prices, qtys, ndses=None, payment_type=PaymentType.CARD,
                                               payment_type_types=None, receipt_type=ReceiptType.INCOME):
    if ndses is None:
        ndses = [CMNds.NDS_20] * len(prices)

    total_sum = sum([price * qty for price, qty in zip(prices, qtys)])
    rows = create_calculated_rows(prices, qtys, ndses, payment_type_types)
    tax_totals = create_tax_totals(rows)

    receipt_data = defaults.receipts(defaults.rows([0], [0]), defaults.payments([0]))
    qr_dt = datetime.strptime(receipt['dt'], "%Y-%m-%d %H:%M:%S").strftime("%Y%m%dT%H%M")

    return {u'receipt_calculated_content': {
        u'money_received_total': unicode(total_sum),
        u'rows': rows,
        u'tax_totals': tax_totals,
        u'total': unicode(total_sum),
        u'totals': [{u'amount': unicode(total_sum), u'payment_type': payment_type}],
        u'firm_reply_email': receipt_data['receipt_content']['firm_reply_email'],
        u'firm_url': receipt_data['receipt_content']['firm_url'],
        u'qr': u't={dt}&s={sum:.2f}&fn={fn}&i={id}&fp={fp}&n={receipt_type}'
            .format(dt=qr_dt, sum=total_sum, id=receipt['id'], fn=receipt['fn']['sn'], fp=receipt['fp'],
                    receipt_type=receipt_type.id)
    }}


def create_calculated_rows(prices, qtys, ndses, payment_type_types=None):
    if payment_type_types is None:
        payment_type_types = [PaymentTypeType.PREPAYMENT] * len(prices)

    return [{
                u'amount': u"{:.2f}".format(round(price * qty, 2)),
                u'payment_type_type': payment_type_type,
                u'price': unicode(price),
                u'qty': unicode(qty),
                u'tax_amount': u'{:.2f}'.format(price * qty * nds.pct / (100 + nds.pct)),
                u'tax_pct': u'{:.2f}'.format(nds.pct),
                u'tax_type': nds.name,
                u'text': u'Test row'}
            for price, qty, nds, payment_type_type in zip(prices, qtys, ndses, payment_type_types)]


def create_tax_totals(rows):
    tax_totals = []
    for nds in CMNds.values():
        taxes = [float(row['tax_amount']) for row in rows if row['tax_type'] == nds.name]
        if taxes:
            tax_totals.append({
                u'tax_amount': u'{:.2f}'.format(sum(taxes)),
                u'tax_pct': u'{:.2f}'.format(nds.pct),
                u'tax_type': nds.name
            })

    return tax_totals


def create_receipt_content_template(receipt, origin=Origin.ONLINE):
    config_data = defaults.config(defaults.STABLE_SN)
    receipt_data = defaults.receipts(defaults.rows([0], [0]), defaults.payments([0]))

    return {
        u'check_url': u'https://greed-ts.paysys.yandex.net:8019/?n={}&fn={}&fpd={}'
            .format(receipt['id'], receipt['fn']['sn'], receipt['fp']),
        u'document_index': greater_than_or_equal_to(0),  # 46
        # u'2017-07-17 16:17:00'
        u'dt': matcher_for(lambda dt: datetime.strptime(dt, "%Y-%m-%d %H:%M:%S").date() == date.today() and
                                      datetime.strptime(dt, "%Y-%m-%d %H:%M:%S").second == 0),
        u'firm': {
            u'inn': config_data['reg_info']['user_inn'],
            u'name': config_data['reg_info']['user_name'],
            u'reply_email': receipt_data['receipt_content']['firm_reply_email']
        },
        u'fn': {
            u'model': u'ФН-1',
            u'sn': matches_regexp(r"\d{16}")  # u'9999078900002485'
        },
        u'fp': greater_than_or_equal_to(0),  # 4271366921L; 372413759
        u'id': greater_than_or_equal_to(0),
        u'kkt': {
            u'automatic_machine_number': config_data['reg_info']['terminal_number'],
            u'rn': matches_regexp(r"\d{16}"),  # u'8782424972063255',
            u'sn': matches_regexp(r"\d{20}"),  # u'00000000381007528533',
            u"version": matches_regexp(r"\d+.\d+.\d+")
        },
        u'localzone': u'Europe/Moscow',
        u'location': {
            u'address': config_data['reg_info']['account_address'],
            u'description': receipt_data['receipt_content']['firm_url']
        },
        u'ofd': {
            u'check_url': config_data['ofd_info']['check_url'],
            u'inn': config_data['reg_info']['ofd_inn'],
            u'name': config_data['reg_info']['ofd_name']
        },
        u'ofd_ticket_received': greater_than_or_equal_to(0),  # 0
        u'shift_number': greater_than_or_equal_to(0),  # 95

        u'receipt_calculated_content': anything(),
        u'receipt_content': anything(),
        u'origin': origin
    }
