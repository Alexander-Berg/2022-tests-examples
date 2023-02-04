# coding: utf-8

import json
from decimal import Decimal as D

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import constants as c
from btestlib import reporter
from btestlib import utils
from btestlib.data.defaults import Date
from cashmachines.data.constants import CMNds
from simpleapi.common import payment_methods
from simpleapi.data.uids_pool import User as SimpleUser
from simpleapi.steps import simple_steps

CONTRACT_START_DT = utils.Date.first_day_of_month(utils.Date.shift_date(Date.NOW(), months=-2))

# user = uids.get_random_of_type(uids.Types.random_from_all)
user = SimpleUser(675282885, 'yb-atst-user-26')


# Создаем платеж в трасте и прописываем его bl_oebs_agent_payment_data (для каждого запуска нужно использовать разные payment_batch_id)
# После этого такой платеж подхватывается задачей receipts_enqueuer и ставится в очередь CASH_REGISTER
def test_insert_fake_agent_payment_data():
    # нужные payment_batch_id из MV_OEBS_AGENT_INVOICE_DATA
    # платежи с ai.payment_date is not null and ai.reconcilation_date is not null
    # 5584079, 6102130, 6102130, 6661776, 5996168, 6101663, 6567787, 6711138, 6566899, 6657555, 4524636, 4327772, 5734194, 6386744, 6556919, 4638012, 6047354, 6557275, 4524719, 6386743, 6217567, 6386390

    # еще не использованные, когда закончатся можно очистить bl_oebs_agent_payment_data и начать все сначала
    # ,6386744,6556919,4638012,6047354,6557275,4524719,6386743,6217567,6386390

    payment_batch_id = 5734194

    new_tpt_id_list = generate_real_tpt_ids()

    payment_batch_id_list = [payment_batch_id] * len(new_tpt_id_list)

    for tpt_id, payment_batch_id in zip(new_tpt_id_list, payment_batch_id_list):
        db.balance().execute("""INSERT INTO bl_oebs_agent_payment_data
      (billing_line_id, payment_amount, payment_batch_id)
        VALUES
      (:tpt_id, 1, :payment_batch_id)""", {'tpt_id': tpt_id, 'payment_batch_id': payment_batch_id})


def generate_real_tpt_ids():
    shop_client_id = create_shop_contract()
    ph_contract_eid = create_ph_contract()

    developer_payload = json.dumps({'print_receipt': True, 'external_id': ph_contract_eid, 'orderId': 'test'})

    payment_id = create_payment(shop_client_id, ph_contract_eid, developer_payload)

    res = db.balance().execute(
        """SELECT id FROM T_THIRDPARTY_TRANSACTIONS
           WHERE PAYMENT_ID = :payment_id
           AND TRANSACTION_TYPE = 'payment'""", {'payment_id': payment_id})
    tpt_id_list = [tpt['id'] for tpt in res]
    return tpt_id_list


def test_blue_market_refund():
    shop_client_id = create_shop_contract()
    ph_contract_eid = create_ph_contract()

    developer_payload = json.dumps({'print_receipt': True, 'external_id': ph_contract_eid, 'orderId': 'test'})

    create_payment(shop_client_id, ph_contract_eid, developer_payload)


def create_payment(shop_client_id, ph_contract_eid, developer_payload_basket):
    PaymentData = utils.namedtuple_with_defaults('PaymentData', 'fee, price, qty, fiscal_nds',
                                                 default_values={'fiscal_nds': CMNds.NDS_18})
    payment_data_list = [
        PaymentData(fee=1, price=D('100'), qty='1'),
        PaymentData(fee=2, price=D('200'), qty='2'),
        PaymentData(fee=3, price=D('300'), qty='3'),
        PaymentData(fee=4, price=D('400'), qty='4'),
    ]

    payments_fees = [pd.fee for pd in payment_data_list]
    product_prices = [pd.price for pd in payment_data_list]
    qty_list = [pd.qty for pd in payment_data_list]
    fiscal_nds_list = [pd.fiscal_nds for pd in payment_data_list]

    # создадим трастовые продукты и продукт фи
    product_list = [
        steps.SimpleApi.create_service_product(c.Services.BLUE_MARKET_REFUNDS, shop_client_id, service_fee=i)
        for i in payments_fees]

    # создадим трастовый платеж в зависимости от требуемых service_fee
    # от quark про developer_payload:
    # Сейчас сервис передает его и в CreateOrders и в CreateBasket
    # В будущем мы будем брать договор и заказ из баскета и в createOrders ничего передаваться не будет
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(c.Services.BLUE_MARKET_REFUNDS, product_list,
                                                       prices_list=product_prices,
                                                       # paymethod=payment_methods.Compensation(),
                                                       paymethod=payment_methods.Cash(),
                                                       developer_payload_list=[
                                                           u'{{"external_id":"{}", "orderId":"tst"}}'.format(
                                                               ph_contract_eid)],
                                                       fiscal_nds_list=fiscal_nds_list,
                                                       developer_payload_basket=developer_payload_basket,
                                                       user=user,
                                                       back_url='https://user-balance.greed-tm.paysys.yandex.ru',
                                                       qty_list=qty_list)

    # экспортируем его
    steps.CommonPartnerSteps.export_payment(payment_id)

    return payment_id


def create_shop_contract():
    with reporter.step(u'Создадим  договор для магазина с 610 и 612 сервисами'):
        shop_client_id = steps.SimpleApi.create_partner(c.Services.BLUE_MARKET_PAYMENTS)
        shop_person_id = steps.PersonSteps.create(shop_client_id, c.PersonTypes.UR.code)

        shop_contract_id, _ = steps.ContractSteps.create_offer({'firm_id': c.Firms.MARKET_111.id,
                                                                'manager_uid': c.Managers.SOME_MANAGER.uid,
                                                                'currency': c.Currencies.RUB.char_code,
                                                                'start_dt': CONTRACT_START_DT,
                                                                'payment_type': 3,
                                                                'payment_term': 10,
                                                                'personal_account': 1,
                                                                'services': [c.Services.BLUE_MARKET_PAYMENTS.id,
                                                                             c.Services.BLUE_MARKET.id],
                                                                'client_id': shop_client_id,
                                                                'person_id': shop_person_id,
                                                                # 'external_id': '1286304' # что это за параметр?
                                                                })

        return shop_client_id


def create_ph_contract():
    # этот договор создается в момент, когда физик решает вернуть товар.
    # External_id договора совпадает с OrderId заказа на маркете
    with reporter.step(u'Создадим расходный договор для физика с 613 сервисом'):
        ph_client_id = steps.SimpleApi.create_partner(c.Services.BLUE_MARKET_REFUNDS)
        steps.UserSteps.link_user_and_client(user, ph_client_id)

        ph_person_id = steps.PersonSteps.create(ph_client_id, c.PersonTypes.PH.code)

        ph_contract_id, ph_contract_eid = steps.ContractSteps.create_offer({
            'firm_id': c.Firms.MARKET_111.id,
            'manager_uid': c.Managers.SOME_MANAGER.uid,
            'currency': c.Currencies.RUB.char_code,
            'start_dt': CONTRACT_START_DT,
            'client_id': ph_client_id,
            'person_id': ph_person_id,
            # 'external_id': '1286304' # что это за параметр?
            'payment_type': 1,
            'services': [
                c.Services.BLUE_MARKET_REFUNDS.id],
            'nds': c.Nds.ZERO
        })

        return ph_contract_eid


def test_check_basket():
    trust_payment_id = '5b6d6b37910d3940a2aa90c2'
    simple_steps.check_basket(service=c.Services.BLUE_MARKET_REFUNDS, trust_payment_id=trust_payment_id)


# ВАЖНО: в тестовом whitespirit можно выбивать чеки только на ИНН ООО Яндекс поэтому перед экспортом нужно поменять ИНН у фирмы Маркет (под схемой meta)
# -- ставим ИНН фирмы 1 для Маркета
# update meta.t_firm set inn = 7736207543 where id = 111;
# -- ставим ИНН маркета для Маркета
# update meta.t_firm set inn = 7704357909 where id = 111;
# Здесь получаем ни о чем не говорящие ошибки, например 499 Client Error,
# настоящую ошибку нужно смотреть в логе test_xmlrpc по payment_id: rest_client.*<payment_id>
# Если там NoFreeKKT - то это "нет свободных касс" - нужно просто перезапустить
def test_export_payment():
    payment_id = 604046537  # service_fee=1,2,3,4     из бага про service_fee=4  - https://st.yandex-team.ru/BALANCE-28917)
    # payment_id = 604049850 # service_fee = 2
    # payment_id = 604050316 # service_fee = 3
    # payment_id = 604050642  # service_fee = 1,2,3
    # payment_id = 604224599  # service_fee = 1,2,3,4 new
    steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)


# из задачи:
# нотификация с новым opcode = 80 по back_url, переданному в CreateBasket
# Посылаем POST c параметрами: mode = 'receipt' status = 'success' receipt_data_url = 'https://greed-tm.paysys.yandex.net:8616/v1/fiscal_storages/9999078900011919/documents/53821/3194997567'
# Т.е. в balance-notifier.log нотификации можно найти по back_url или по урлу/параметрам чека
def test_get_notification():
    opcode = 80
    payment_id = 604049850
    api.test_balance().GetNotification(opcode, payment_id)
