# coding=utf-8
import datetime

import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils

# pytestmark = pytest.mark.xfail

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.OVERDRAFT)
              ]

SERVICE_ID = 99
# TODO choose correspond product
PRODUCT_ID = 504601
PRODUCT_PRICE = 80
PAYSYS_ID = 1091
PERSON_TYPE_UR = 'ur_autoru'
PERSON_TYPE_PH = 'ph_autoru'
OVERDRAFT_LIMIT = 1500.0 / PRODUCT_PRICE
QUANT = 0.03 / PRODUCT_PRICE

BASE_DT = utils.add_months_to_date(datetime.datetime.now(), -4)
to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta


# TODO ТЗ: <TBD>

@pytest.fixture
# TODO какую структуру имеет contract_info?
def data_generator(dt_deltas=[], qty_modificators=[], addition=0, is_agency=0, contract_info=None, previous_limit=None):
    # Сумма и дата счёта, в рамках которого генерируются все акты
    qty = 2000

    # Овердрафт в Авто.Ру по валютному продукту, оборот накапливается по продуктам в днях(6).
    # Все клиенты АвтоРу - валютные. Фиксированные параметры мультивалютности.
    client_params = {'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime(2000, 1, 1),
                     'SERVICE_ID': SERVICE_ID, 'IS_AGENCY': is_agency}
    # создаем клиента
    client_id = steps.ClientSteps.create(client_params)
    # создаем плательщиков TODO почему 2?
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE_UR)
    # название person_id2 не является хорошей практикой
    # Второй плательщик (для договора), так как при наличии договора с плат. из него нельзя выставляться без договора
    person_id2 = steps.PersonSteps.create(client_id, PERSON_TYPE_PH)

    # Наличие договора влияет на выдачу овердрафта.
    if contract_info is not None:
        steps.ContractSteps.create_contract(contract_info[0], {'CLIENT_ID': client_id,
                                                               'PERSON_ID': person_id2,
                                                               'DT': to_iso(BASE_DT - dt_delta(days=180)),
                                                               # TODO что за смысл в условной логике
                                                               # TODO что будем делать когда даты надо будет сместить (хардкод - зло)
                                                               'FINISH_DT': to_iso(BASE_DT + dt_delta(days=180)) if
                                                               contract_info[1] == 1
                                                               else to_iso(BASE_DT - dt_delta(days=180)),
                                                               'IS_SIGNED': to_iso(BASE_DT - dt_delta(days=180)),
                                                               }
                                            )

    # TODO как я вижу все эти действия абсолютно одинаковы для всех кейсов
    # Все акты генерируются по заявкам по одному заказу в рамках одном счёта
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=BASE_DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # В зависимости от переданных параметров генерируем акты:
    #  - в месяце со смещением dt_deltas[index]
    #  - сумма вычисляется как часть qty_modificators[index] от лимита OVERDRAFT_LIMIT +\- добавка addition
    qty = 0  # TODO плохо использовать одну переменную для разных целей
    for index in range(len(dt_deltas)):
        # TODO утилитные методы надо группировать по области
        dt = utils.add_months_to_date(datetime.datetime.now(), -dt_deltas[index])
        qty += OVERDRAFT_LIMIT / qty_modificators[
            index] + addition  # TODO что? TODO кажется что qty_modificator должен быть дробным и умножаться
        # TODO зачем задавать нули в бакс и мани?
        # TODO зачем именованные параметры передаются позиционно?
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0, 'Days': qty, 'Money': 0}, 0, dt)
        # TODO опять именованный параметр позиционно.
        # TODO в таких случаях можно более говоряще называть параметры
        steps.ActsSteps.generate(client_id, 1, dt)

    # По мотивам BALANCE-XXXXX: обнулялся лимит в случае НЕнулевого предыдущего значения лимита. Вставляем предыдущее.
    if previous_limit is not None:
        steps.OverdraftSteps.set_force_overdraft(client_id, service_id=SERVICE_ID, limit=previous_limit, firm_id=10,
                                                 start_dt=datetime.datetime.now())

    # TODO сделать степ
    api.test_balance().CalculateOverdraft([client_id])
    return client_id


@pytest.fixture(scope="module")
def client_list():
    clients = dict()

    # Формат параметризации:
    #  qty_deltas - list смещений месяца актов # TODO смещение даты акта в месяцах?
    # TODO модификатор это слишком размытое понятие.
    #  qty_modificators - list модификаторов для сумм актов (от OVERDRAFT_LIMIT) (в фишках)
    #  addition - добавка к сумме акта (в фишках)
    #  is_agency - признак "агентство", запрещает овердрафт
    # TODO это явно словарь, а не тупл
    #  contract_info - tuple (тип договора, признак "договор действует" (0/1))
    # TODO не понятно что такое
    #  previous_limit - предыдущий лимит

    # 1
    # _Name: test_limit_by_same_acts_in_two_months
    # _Info: P2; overdraft, autoru
    # _Summ: Два акта по половине лимита в двух предыдущих месяцах.
    test_description = 'test_limit_by_same_acts_in_two_months'  # TODO название не отражает того, что акты заполняют лимит
    # TODO дата генератор должен возвращать словарь
    client_id = data_generator(dt_deltas=[2, 1],
                               qty_modificators=[2, 2],
                               addition=0,
                               is_agency=0,
                               contract_info=None
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    # TODO входное значение вычисляется хитро из qty_modificators + addition,
    # не понятно зачем если мы потом явно вводим эталон
    clients[test_description]['expected'] = 300  # Макс(750,750)/3 округл. до 100

    # 2
    # _Name: test_limit_by_diff_acts_in_two_months
    # _Info: P2; overdraft, autoru
    # _Summ: Два акта в соотношении 1:2 в двух предыдущих месяцах
    test_description = 'test_limit_by_diff_acts_in_two_months'
    client_id = data_generator(dt_deltas=[2, 1],
                               qty_modificators=[3, 1.5],
                               addition=0,
                               is_agency=0,
                               contract_info=None
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = 400  # Макс(1000,500)/3 кругл до 100

    # 3
    # _Name: test_limit_by_act_in_prev_month
    # _Info: P2; overdraft, autoru
    # _Summ: Акт в пред. месяце на сумму лимита
    test_description = 'test_limit_by_act_in_prev_month'
    client_id = data_generator(dt_deltas=[1],
                               qty_modificators=[1],
                               addition=0,
                               is_agency=0,
                               contract_info=None
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = 500  # Макс(1500)/3 кругл до 100

    # 4
    # _Name: test_gt_limit_by_act_in_prevprev_month
    # _Info: P2; overdraft, autoru
    # _Summ: Акт в пред.пред месяце на сумму лимита+0.03 копейки
    test_description = 'test_gt_limit_by_act_in_prevprev_month'
    client_id = data_generator(dt_deltas=[2],
                               qty_modificators=[1],
                               addition=QUANT,  # TODO почему QUANT?
                               is_agency=0,
                               contract_info=None
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = 600  # Макс(1500.03)/3 кругл до 100

    # 5
    # _Name: test_gt_limit_with_inactive_postpay_contract
    # _Info: P2; overdraft, autoru
    # _Summ: Акт в пред.пред месяце на сумму лимита+0.03; есть НЕдействующий постоплатный договор "Авто.Ру: не агентский"
    test_description = 'test_gt_limit_with_inactive_postpay_contract'
    client_id = data_generator(dt_deltas=[2],
                               qty_modificators=[1],
                               addition=QUANT,
                               is_agency=0,
                               # TODO откуда берется это строковое значение?
                               contract_info=('auto_ru_non_agency_post', 0)
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = 600  # Макс(1500.03)/3 кругл до 100

    # 6
    # _Name: test_gt_limit_with_prepay_contract
    # _Info: P2; overdraft, autoru
    # _Summ: Акт в пред.пред месяце на сумму лимита+0.03; есть действующий предоплатный договор "Авто.Ру: не агентский"
    test_description = 'test_gt_limit_with_prepay_contract'
    client_id = data_generator(dt_deltas=[2],
                               qty_modificators=[1],
                               addition=QUANT,
                               is_agency=0,
                               contract_info=('auto_ru_non_agency', 1)
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = 600  # Макс(1500.03)/3 кругл до 100

    # 7
    # _Name: test_lt_limit_by_act_in_one_month
    # _Info: P2; overdraft, autoru
    # _Summ: Акт в пред.пред месяце на сумму лимита-0.03; Овердрафта нет.
    test_description = 'test_lt_limit_by_act_in_one_month'
    client_id = data_generator(dt_deltas=[2],
                               qty_modificators=[1],
                               addition=-QUANT,
                               is_agency=0,
                               contract_info=None
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = None  # TODO почему None, а не 0?

    # 8
    # _Name: test_lt_limit_by_acts_in_two_months
    # _Info: P2; overdraft, autoru
    # _Summ: Акты в двух пред. месяцах в равных пропорциях. Сумма одного акта уменьшена на 0.03; Овердрафта нет.
    test_description = 'test_lt_limit_by_acts_in_two_months'
    client_id = data_generator(dt_deltas=[2, 1],
                               qty_modificators=[2, 2],
                               addition=-QUANT,
                               is_agency=0,
                               contract_info=None
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = None

    # 9
    # _Name: test_no_limit_by_act_out_of_dt_range
    # _Info: P2; overdraft, autoru
    # _Summ: Акт на сумму лимита 3 месяца назад. Овердрафта нет
    test_description = 'test_no_limit_by_act_out_of_dt_range'
    client_id = data_generator(dt_deltas=[3],
                               qty_modificators=[1],
                               addition=0,
                               is_agency=0,
                               contract_info=None
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = None

    # 10
    # _Name: test_no_limit_by_act_in_curr_month
    # _Info: P2; overdraft, autoru
    # _Summ: Акт на сумму лимита в текущем месяце. Овердрафта нет
    test_description = 'test_no_limit_by_act_in_curr_month'
    client_id = data_generator(dt_deltas=[0],
                               qty_modificators=[1],
                               addition=QUANT,
                               is_agency=0,
                               contract_info=None
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = None

    # 11
    # _Name: test_no_limit_agency_id
    # _Info: P2; overdraft, autoru
    # _Summ: Акт на полную сумму в пред. месяце. Агентство. Овердрафта нет
    test_description = 'test_no_limit_agency_id'
    client_id = data_generator(dt_deltas=[1],
                               qty_modificators=[1],
                               addition=QUANT,
                               is_agency=1,
                               contract_info=None
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = None

    # 12
    # _Name: test_gt_limit_with_postpay_contract
    # _Info: P2; overdraft, autoru
    # _Summ: Акт на полную сумму в пред.пред. месяце. Есть действующий постоплатный договор "Авто.Ру: не агентский".
    test_description = 'test_gt_limit_with_postpay_contract'
    client_id = data_generator(dt_deltas=[2],
                               qty_modificators=[1],
                               addition=QUANT,
                               is_agency=0,
                               contract_info=('auto_ru_non_agency_post', 1)
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = 0

    # 13  # TODO где комменты?
    test_description = 'test_already_has_non_zero_limit'
    client_id = data_generator(dt_deltas=[2],
                               qty_modificators=[1],
                               addition=QUANT,
                               is_agency=0,
                               previous_limit=100
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = 600

    # 14
    test_description = 'test_already_has_zero_limit'
    client_id = data_generator(dt_deltas=[2],
                               qty_modificators=[1],
                               addition=QUANT,
                               is_agency=0,
                               previous_limit=0
                               )
    clients[test_description] = dict()
    clients[test_description]['client_id'] = client_id
    clients[test_description]['expected'] = 600

    # TODO Missed case: not a agency subclient?
    print clients
    # TODO отличный кандидат в степ, точно понадобится еще
    query = "select state as val from t_export where type = 'OVERDRAFT' and object_id = :client_id"
    for client in clients.values():
        query_params = {'client_id': client['client_id']}
        db.wait_for(query, query_params, timeout=150)
    return clients


# TODO тоже степ
def get_limit(client_id):
    query = "select overdraft_limit from t_client_overdraft where client_id = :client_id and service_id = :service_id"
    query_params = {'client_id': client_id, 'service_id': SERVICE_ID}
    result = db.balance().execute(query, query_params)
    # TODO передаем одного клиента, можно распарсить ответ и возвращать конкретное значение
    return result


# 1
@pytest.mark.smoke
def test_limit_by_same_acts_in_two_months(client_list):
    test_description = 'test_limit_by_same_acts_in_two_months'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    assert len(limit) > 0
    if len(limit) > 0:
        assert limit[0]['overdraft_limit'] == client_list[test_description]['expected']


# 2
def test_limit_by_diff_acts_in_two_months(client_list):
    test_description = 'test_limit_by_diff_acts_in_two_months'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    assert len(limit) > 0
    if len(limit) > 0:
        assert limit[0]['overdraft_limit'] == client_list[test_description]['expected']


# 3
def test_limit_by_act_in_prev_month(client_list):
    test_description = 'test_limit_by_act_in_prev_month'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    assert len(limit) > 0
    if len(limit) > 0:
        assert limit[0]['overdraft_limit'] == client_list[test_description]['expected']


# 4
def test_gt_limit_by_act_in_prevprev_month(client_list):
    test_description = 'test_gt_limit_by_act_in_prevprev_month'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    assert len(limit) > 0
    if len(limit) > 0:
        assert limit[0]['overdraft_limit'] == client_list[test_description]['expected']


# 5
def test_gt_limit_with_inactive_postpay_contract(client_list):
    test_description = 'test_gt_limit_with_inactive_postpay_contract'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    assert len(limit) > 0
    if len(limit) > 0:
        assert limit[0]['overdraft_limit'] == client_list[test_description]['expected']


# 6
def test_gt_limit_with_prepay_contract(client_list):
    test_description = 'test_gt_limit_with_prepay_contract'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    assert len(limit) > 0
    if len(limit) > 0:
        assert limit[0]['overdraft_limit'] == client_list[test_description]['expected']


# 7
def test_lt_limit_by_act_in_one_month(client_list):
    test_description = 'test_lt_limit_by_act_in_one_month'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    # TODO bad check =( need to be redesigned
    assert len(limit) == 0


# 8
def test_lt_limit_by_acts_in_two_months(client_list):
    test_description = 'test_lt_limit_by_acts_in_two_months'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    # TODO bad check =( need to be redesigned
    assert len(limit) == 0


# 9
def test_no_limit_by_act_out_of_dt_range(client_list):
    test_description = 'test_no_limit_by_act_out_of_dt_range'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    # TODO bad check =( need to be redesigned
    assert len(limit) == 0


# 10
def test_no_limit_by_act_in_curr_month(client_list):
    test_description = 'test_no_limit_by_act_in_curr_month'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    # TODO bad check =( need to be redesigned
    assert len(limit) == 0


# 11
def test_no_limit_agency_id(client_list):
    test_description = 'test_no_limit_agency_id'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    # TODO bad check =( need to be redesigned
    assert len(limit) == 0


# 12
def test_gt_limit_with_postpay_contract(client_list):
    test_description = 'test_gt_limit_with_postpay_contract'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    assert len(limit) > 0
    if len(limit) > 0:
        assert limit[0]['overdraft_limit'] == client_list[test_description]['expected']


# 13
def test_already_has_non_zero_limit(client_list):
    test_description = 'test_already_has_non_zero_limit'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    assert len(limit) > 0
    if len(limit) > 0:
        assert limit[0]['overdraft_limit'] == client_list[test_description]['expected']


# 14
def test_already_has_zero_limit(client_list):
    test_description = 'test_already_has_zero_limit'
    client_id = client_list[test_description]['client_id']
    limit = get_limit(client_id)
    assert len(limit) > 0
    if len(limit) > 0:
        assert limit[0]['overdraft_limit'] == client_list[test_description]['expected']


if __name__ == '__main__':
    pytest.main()
