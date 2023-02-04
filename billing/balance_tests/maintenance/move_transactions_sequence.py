# coding: utf-8
__author__ = 'a-vasin'

import balance.balance_db as db
from btestlib.constants import Firms


# a-vasin: это всё нужно, чтобы транзакции в OEBS выгружать в тесте без коллизий
def test_move_transactions_sequence():
    pass
    #TODO: этот скрипт ломает работу Баланса (за счёт изменения инкремента на 1 - должен быть 10 + id заканивается на 9
    #TODO: надо переписать
    # query = "SELECT max(billing_line_id) max_id FROM apps.xxap_agent_reward_data"
    # max_id = int(db.oebs().execute_oebs(Firms.YANDEX_1.id, query)[0]['max_id'])
    #
    # query = "SELECT S_REQUEST_ORDER_ID.nextval curval FROM dual"
    # cur_id = int(db.balance().execute(query)[0]['curval'])
    #
    # step = max_id - cur_id + 1
    # if step <= 1:
    #     return
    #
    # query = "ALTER SEQUENCE S_REQUEST_ORDER_ID INCREMENT BY {}".format(step)
    # db.balance().execute(query)
    #
    # query = "SELECT S_REQUEST_ORDER_ID.nextval curval FROM dual"
    # db.balance().execute(query)
    #
    # query = "ALTER SEQUENCE S_REQUEST_ORDER_ID INCREMENT BY 1"
    # db.balance().execute(query)
