# -*- coding: utf-8 -*-

import copy
import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.matchers as matchers
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.constants import Services, Paysyses
from btestlib.utils import wait_until
import btestlib.config as balance_config
import balance.balance_api as api

DIRECT = Services.DIRECT.id

NOT_INSTANT_PAYSYS_ID = Paysyses.BANK_PH_RUB.id
INSTANT_PAYSYS_ID = Paysyses.CC_PH_RUB.id

PERSON_TYPE_PH = 'ph'
DIRECT_PRODUCT_ID = 1475

QTY = 100

dt = datetime.datetime.now()

UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT = 3  # Блок овердрафта


def create_invoice_with_act(service, client_id, product, person_id, paysys, dt, contract_id=None, payment_sum=None,
                            completion_qty=None, count_of_acts=1, overdraft=0, with_payment=True):
    ORDER_QTY = QTY
    COMPLETION_QTY = completion_qty if completion_qty is not None else ORDER_QTY

    service_order_id = steps.OrderSteps.next_id(service_id=service)
    steps.OrderSteps.create(client_id=client_id, product_id=product, service_id=service,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': service, 'ServiceOrderID': service_order_id, 'Qty': ORDER_QTY, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    credit = 1 if contract_id is not None else 0

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys,
                                                 credit=credit, contract_id=contract_id, overdraft=overdraft,
                                                 endbuyer_id=None)
    if with_payment:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=payment_sum)

    acts_list = []
    for x in range(count_of_acts):
        steps.CampaignsSteps.do_campaigns(service, service_order_id, {'Bucks': COMPLETION_QTY}, 0, dt)
        act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
        acts_list.append(act_id)
        COMPLETION_QTY += COMPLETION_QTY * 0.5
    return invoice_id, acts_list


@pytest.mark.parametrize('paysys', [NOT_INSTANT_PAYSYS_ID,
                                    # INSTANT_PAYSYS_ID
                                    ]
                         )
@pytest.mark.parametrize('overdraft_invoice_default_params', [({'payment_sum': 1499,
                                                                'invoice_payment_term_dt': 20
                                                                })]
                         )
@pytest.mark.parametrize('params',
                         [
                             {'overdraft_invoice_extra_params': {},
                              'expected': {'invoice_params': {'free_funds_sum': 3000}
                                           }
                              },
                             {'overdraft_invoice_extra_params': {'hidden_act': True},
                              'expected': {'invoice_params': {'free_funds_sum': 0}
                                           }
                              },
                             {'overdraft_invoice_extra_params': {'payment_sum': 1500},
                              'expected': {'invoice_params': {'free_funds_sum': 0}
                                           }
                              },
                             {'overdraft_invoice_extra_params': {'invoice_payment_term_dt': 14},
                              'expected': {'invoice_params': {'free_funds_sum': 0}
                                           }
                              },
                             {'overdraft_invoice_extra_params': {'invoice_payment_term_dt': None},
                              'expected': {'invoice_params': {'free_funds_sum': 0}
                                           }
                              },
                             {'overdraft_invoice_extra_params': {'our_fault_debt': True},
                              'expected': {'invoice_params': {'free_funds_sum': 0}
                                           }
                              },
                             {'overdraft_invoice_extra_params': {'our_fault_debt': True,
                                                                 'our_fault_debt_hidden': True},
                              'expected': {'invoice_params': {'free_funds_sum': 3000}
                                           }
                              },
                             {'overdraft_invoice_extra_params': {'payment_sum': 2249,
                                                                 'our_fault_debt': True,
                                                                 'another_act_with_bad_debt_not_our_fault': True},
                              'expected': {'invoice_params': {'free_funds_sum': 3000}
                                           }
                              },
                             {'overdraft_invoice_extra_params': {'payment_sum': 2250,
                                                                 'our_fault_debt': True,
                                                                 'another_act_with_bad_debt_not_our_fault': True},
                              'expected': {'invoice_params': {'free_funds_sum': 0}
                                           }
                              }
                         ]
                         )
def test_auto_repayment_depend_on_overdraft_invoice(params, overdraft_invoice_default_params, paysys):
    PAYMENT_SUM = 3000
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE_PH)
    steps.OverdraftSteps.set_force_overdraft(client_id=client_id, service_id=DIRECT, limit=100, firm_id=1)

    overdraft_invoice_params = copy.deepcopy(overdraft_invoice_default_params)
    overdraft_invoice_params.update(params['overdraft_invoice_extra_params'])

    count_of_acts = 2 if overdraft_invoice_params.get('another_act_with_bad_debt_not_our_fault', False) else 1
    overdraft_invoice_id, act_list = create_invoice_with_act(service=DIRECT, client_id=client_id,
                                                             product=DIRECT_PRODUCT_ID,
                                                             person_id=person_id, paysys=paysys, dt=dt, overdraft=1,
                                                             count_of_acts=count_of_acts, completion_qty=50)
    act_id = act_list[0]

    if paysys == NOT_INSTANT_PAYSYS_ID:
        steps.InvoiceSteps.pay(overdraft_invoice_id, overdraft_invoice_params['payment_sum'] - PAYMENT_SUM)
    else:
        steps.InvoiceSteps.turn_on(invoice_id=overdraft_invoice_id, sum=overdraft_invoice_params['payment_sum'])

    reporter.log(overdraft_invoice_params.get('invoice_payment_term_dt', False))
    if overdraft_invoice_params.get('invoice_payment_term_dt', False) is not None:
        reporter.log('aa')
        steps.OverdraftSteps.expire_overdraft_invoice(overdraft_invoice_id,
                                                      delta=overdraft_invoice_params['invoice_payment_term_dt'])
    elif overdraft_invoice_params.get('invoice_payment_term_dt', False) is None:
        reporter.log('vv')
        db.balance().execute('UPDATE t_invoice SET payment_term_dt = NULL WHERE id = :invoice_id',
                             {'invoice_id': overdraft_invoice_id})

    if overdraft_invoice_params.get('hidden_act', False):
        steps.ActsSteps.hide(act_id)

    if overdraft_invoice_params.get('our_fault_debt', None) is not None:
        steps.BadDebtSteps.make_bad_debt(overdraft_invoice_id, our_fault=1)
        if overdraft_invoice_params.get('our_fault_debt_hidden', False):
            steps.BadDebtSteps.make_bad_debt_hidden(act_id)
        if overdraft_invoice_params.get('another_act_with_bad_debt_not_our_fault', False):
            steps.BadDebtSteps.make_not_our_fault(act_list[1])

    invoice_id, act_list = create_invoice_with_act(service=DIRECT, client_id=client_id, product=DIRECT_PRODUCT_ID,
                                                   person_id=person_id, paysys=NOT_INSTANT_PAYSYS_ID, dt=dt,
                                                   overdraft=0, count_of_acts=0)

    invoice = db.get_invoice_by_id(invoice_id)[0]

    receipt_sum = invoice['receipt_sum'] if paysys == NOT_INSTANT_PAYSYS_ID else invoice['receipt_sum_1c']
    free_funds_sum = receipt_sum - invoice['consume_sum']
    expected_free_funds_sum = params['expected']['invoice_params']['free_funds_sum']
    utils.check_that(free_funds_sum, hamcrest.equal_to(expected_free_funds_sum))

    extprops = db.balance().get_extprops_by_object_id('Invoice', invoice_id)

    if expected_free_funds_sum != 0:
        wait_until(lambda: db.balance().get_extprops_by_object_id('Invoice', invoice_id),
                   success_condition=matchers.contains_dicts_with_entries(
                       [{'attrname': 'unused_funds_lock', 'value_num': UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT},
                        {'attrname': 'service_ids', 'key': 7, 'value_num': 1}]),
                   timeout=5 * 60)
    else:
        value_num = 2 if balance_config.ENABLE_SINGLE_ACCOUNT else 3
        invoice_extprops = db.balance().get_extprops_by_object_id('Invoice', invoice_id)
        utils.check_that(len(invoice_extprops), hamcrest.equal_to(1))
        utils.check_that(invoice_extprops,
                         matchers.contains_dicts_with_entries([{'attrname': 'service_ids', 'key': 7,
                                                                'value_num': value_num}]))


@pytest.mark.parametrize('paysys', [NOT_INSTANT_PAYSYS_ID]
                         )
@pytest.mark.parametrize('overdraft_invoice_default_params', [({'payment_sum': 1499,
                                                                'invoice_payment_term_dt': 20
                                                                })]
                         )
@pytest.mark.parametrize('prepayment_invoice_default_params', [({'manual_turn_on': False,
                                                                 'invoice_dt': dt})]
                         )
@pytest.mark.parametrize('params',
                         [
                             {'prepayment_invoice_extra_params': {'manual_turn_on': True},
                              'expected': {'invoice_params': {'free_funds_sum': 0}
                                           }
                              },
                             {'prepayment_invoice_extra_params': {
                                 'invoice_dt': datetime.datetime(2011, 9, 14, 0, 0, 0)},
                                 'expected': {'invoice_params': {'free_funds_sum': 0}
                                              }
                             },
                             {'prepayment_invoice_extra_params': {},
                              'expected': {'invoice_params': {'free_funds_sum': 3000}
                                           }
                              },
                             {'prepayment_invoice_extra_params': {},
                              'expected': {'invoice_params': {'free_funds_sum': 3000}
                                           }
                              },
                         ]
                         )
def test_auto_repayment_depend_on_prepayment_invoice(params, prepayment_invoice_default_params, paysys,
                                                     overdraft_invoice_default_params):
    PAYMENT_SUM = 3000
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE_PH)
    steps.OverdraftSteps.set_force_overdraft(client_id=client_id, service_id=DIRECT, limit=100, firm_id=1)
    overdraft_invoice_default_params = copy.deepcopy(overdraft_invoice_default_params)
    reporter.log(overdraft_invoice_default_params)
    overdraft_invoice_params = overdraft_invoice_default_params

    overdraft_invoice_id, act_list = create_invoice_with_act(service=DIRECT, client_id=client_id,
                                                             product=DIRECT_PRODUCT_ID,
                                                             person_id=person_id, paysys=paysys, dt=dt, overdraft=1,
                                                             count_of_acts=1, completion_qty=50)

    steps.OverdraftSteps.expire_overdraft_invoice(overdraft_invoice_id,
                                                  delta=overdraft_invoice_params['invoice_payment_term_dt'])

    steps.InvoiceSteps.pay(overdraft_invoice_id, overdraft_invoice_params['payment_sum'] - PAYMENT_SUM)

    prepayment_invoice_params = prepayment_invoice_default_params.copy()
    prepayment_invoice_params.update(params['prepayment_invoice_extra_params'])

    invoice_id, act_list = create_invoice_with_act(service=DIRECT, client_id=client_id, product=DIRECT_PRODUCT_ID,
                                                   with_payment=False, person_id=person_id, paysys=paysys,
                                                   dt=prepayment_invoice_params['invoice_dt'], overdraft=0,
                                                   count_of_acts=0)

    if prepayment_invoice_params['manual_turn_on']:
        steps.InvoiceSteps.turn_on_ai(invoice_id)
    else:
        steps.InvoiceSteps.pay(invoice_id)

    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    invoice = db.get_invoice_by_id(invoice_id)[0]

    receipt_sum = invoice['receipt_sum'] if paysys == NOT_INSTANT_PAYSYS_ID else invoice['receipt_sum_1c']
    free_funds_sum = receipt_sum - invoice['consume_sum']
    expected_free_funds_sum = params['expected']['invoice_params']['free_funds_sum']
    utils.check_that(free_funds_sum, hamcrest.equal_to(expected_free_funds_sum))

    extprops = db.balance().get_extprops_by_object_id('Invoice', invoice_id)

    if expected_free_funds_sum != 0:
        utils.check_that(extprops, matchers.contains_dicts_with_entries(
            [{'attrname': 'unused_funds_lock', 'value_num': UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT},
             {'attrname': 'service_ids', 'key': 7, 'value_num': 1}]))
    else:
        value_num = 2 if balance_config.ENABLE_SINGLE_ACCOUNT else 3
        invoice_extprops = db.balance().get_extprops_by_object_id('Invoice', invoice_id)
        utils.check_that(len(invoice_extprops), hamcrest.equal_to(1))
        utils.check_that(invoice_extprops,
                         matchers.contains_dicts_with_entries([{'attrname': 'service_ids', 'key': 7,
                                                                'value_num': value_num}]))


def test_single_account_with_overdraft_debt():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE_PH)
    steps.OverdraftSteps.set_force_overdraft(client_id=client_id, service_id=DIRECT, limit=100, firm_id=1)

    overdraft_invoice_id, act_list = create_invoice_with_act(service=DIRECT, client_id=client_id,
                                                             product=DIRECT_PRODUCT_ID,
                                                             person_id=person_id, paysys=Paysyses.BANK_PH_RUB.id,
                                                             dt=dt, overdraft=1,
                                                             count_of_acts=1, completion_qty=50, with_payment=False)

    steps.OverdraftSteps.expire_overdraft_invoice(overdraft_invoice_id, delta=20)

    with reporter.step(u"Переносим дату создания клиента в будущее"):
        query = "update bo.t_client set creation_dt = date'2031-01-01' where id = :id"
        db.balance().execute(query, {'id': client_id})

    with reporter.step(u"Включаем клиенту ЕЛС"):
        single_account_number = api.test_balance().SingleAccountProcessClient(client_id)
        reporter.attach(u'single_account_number', single_account_number)

    invoice_id, act_list = create_invoice_with_act(service=DIRECT, client_id=client_id, product=DIRECT_PRODUCT_ID,
                                                   with_payment=False, person_id=person_id,
                                                   paysys=Paysyses.BANK_PH_RUB.id,
                                                   dt=dt, overdraft=0, count_of_acts=0)

    steps.InvoiceSteps.pay(invoice_id)

    invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    invoice = db.get_invoice_by_id(invoice_id)[0]

    receipt_sum = invoice['receipt_sum']
    free_funds_sum = receipt_sum - invoice['consume_sum']
    expected_free_funds_sum = 0
    utils.check_that(free_funds_sum, hamcrest.equal_to(expected_free_funds_sum))

    extprops = db.balance().get_extprops_by_object_id('Invoice', invoice_id)
    utils.check_that(extprops, matchers.contains_dicts_with_entries(
        [{'attrname': 'service_ids', 'key': 7, 'value_num': 2}]))
