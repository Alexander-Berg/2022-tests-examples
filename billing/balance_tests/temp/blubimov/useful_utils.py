# coding: utf-8
__author__ = 'blubimov'

from urlparse import urljoin

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import constants as const
from btestlib import environments as env
from btestlib import reporter


# вывоводим ссылки на все объекты по клиенту
def print_all_for_client(client_id):
    service_order_ids = get_attr_values_list(db.balance().execute("SELECT * FROM t_order WHERE CLIENT_ID = :client_id",
                                                                  {'client_id': client_id}), attr='service_order_id')

    invoice_ids = get_attr_values_list(db.balance().execute("SELECT * FROM t_invoice WHERE CLIENT_ID = :client_id",
                                                            {'client_id': client_id}), attr='id')

    act_ids = get_attr_values_list(db.balance().execute("SELECT * FROM t_act WHERE CLIENT_ID = :client_id",
                                                        {'client_id': client_id}), attr='id')

    print "------------------------------------------------------------------------------------\n"
    print "PRINT_ALL_FOR_CLIENT:"

    if service_order_ids:
        print_values(urljoin(env.balance_env().balance_ai, "/order.xml?service_cc=rasp&service_order_id={}"),
                     service_order_ids)
    else:
        print "No orders"

    if invoice_ids:
        print_values(urljoin(env.balance_env().balance_ai, "/invoice.xml?invoice_id={}"), invoice_ids)
    else:
        print "No invoices"

    if act_ids:
        print_values(urljoin(env.balance_env().balance_ai, "/act.xml?act_id={}"), act_ids)
    else:
        print "No acts"

    print "------------------------------------------------------------------------------------\n"


# вызываем экспорт всех объектов по клиенту
def export_all_for_client(client_id, check_export=True, fake_transactions=False):
    if check_export:

        print "------------------------------------------------------------------------------------\n"
        print "EXPORT_ALL_FOR_CLIENT:"

        # клиент
        _check_oebs_export_list(const.Export.Classname.CLIENT, [client_id])

        # договора
        contract_id_list = get_attr_values_list(
            db.balance().execute('SELECT id FROM T_CONTRACT2 WHERE CLIENT_ID = :client_id',
                                 {'client_id': client_id}))
        _check_oebs_export_list(const.Export.Classname.CONTRACT, contract_id_list)

        # дс
        collateral_id_list = get_attr_values_list(db.balance().execute(
            'SELECT id FROM t_contract_collateral WHERE contract2_id IN (SELECT id FROM T_CONTRACT2 WHERE CLIENT_ID = :client_id)',
            {'client_id': client_id}))
        _check_oebs_export_list(const.Export.Classname.CONTRACT_COLLATERAL, collateral_id_list)

        # счета
        invoice_id_list = get_attr_values_list(
            db.balance().execute('SELECT id FROM t_invoice WHERE CLIENT_ID = :client_id', {'client_id': client_id}))
        _check_oebs_export_list(const.Export.Classname.INVOICE, invoice_id_list)

        # плательщики
        # пока отключил, т.к. он по идее выгружается с договором/счетом, а здесь перевыгружается и при этом часто таймаутится
        # person_id_list = get_attr_values_list(
        #     db.balance().execute('SELECT id FROM t_person WHERE client_id = :client_id',
        #                          {'client_id': client_id}))
        # _check_oebs_export_list(const.Export.Classname.PERSON, person_id_list)

        # акты
        act_id_list = get_attr_values_list(
            db.balance().execute('SELECT id FROM t_act WHERE CLIENT_ID = :client_id', {'client_id': client_id}))
        _check_oebs_export_list(const.Export.Classname.ACT, act_id_list)

        if not fake_transactions:
            # транзакции
            # (почему то по partner_id очень долго ищет)
            transaction_id_list = get_attr_values_list(db.balance().execute(
                'SELECT id FROM t_thirdparty_transactions WHERE CONTRACT_ID IN (SELECT id FROM T_CONTRACT2 WHERE CLIENT_ID = :client_id)',
                {'client_id': client_id}))
            _check_oebs_export_list(const.Export.Classname.TRANSACTION, transaction_id_list)

        print "------------------------------------------------------------------------------------\n"
    else:
        print "------------------------------------------------------------------------------------\n"
        print "EXPORT_ALL_FOR_CLIENT IS SWITCHED OFF\n"
        print "------------------------------------------------------------------------------------\n"


def get_client_id(contract_id):
    client_id = db.balance().execute('SELECT client_id FROM T_CONTRACT2 WHERE id = :id',
                                     {'id': contract_id}, single_row=True)['client_id']
    return client_id


# ------------------------------------------ utils ------------------------------------------

def get_attr_values_list(dict_list, attr='id'):
    return [d[attr] for d in dict_list]


def print_values(template, vals):
    for v in vals:
        reporter.log(template.format(v))


def _check_oebs_export_list(classname, id_list):
    for id in id_list:
        steps.CommonSteps.export(const.Export.Type.OEBS, classname, id)
