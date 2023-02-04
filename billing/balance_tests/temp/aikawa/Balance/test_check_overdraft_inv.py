# -*- coding: utf-8 -*-

import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.matchers as matchers
import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.environments import BalanceHosts as hosts
from temp.igogor.balance_objects import Contexts, Products, Firms, ContractCommissionType, PersonTypes, Paysyses

NOW = datetime.datetime.now()

PAYMENT_TERM_DT = NOW - datetime.timedelta(days=5)
DT_DELTA = 16
DEFAULT_PAYMENT_TERM_DT = NOW + datetime.timedelta(days=DT_DELTA)

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.PH,
                                              firm=Firms.YANDEX_1)

OVERDRAFT_LIMIT = 1000

UNUSED_FUNDS_LOCK_OVERDUE_OVERDRAFT = 3  # Блок овердрафта


def check_invoice_type_and_overdraft_value(invoice_id, type, overdraft):
    invoice = db.get_invoice_by_id(invoice_id)[0]
    type_value = invoice['type']
    overdraft_value = invoice['overdraft']
    utils.check_that(type_value, hamcrest.equal_to(type))
    utils.check_that(overdraft_value, hamcrest.equal_to(overdraft))


@pytest.mark.ignore_hosts(hosts.PT)
@pytest.mark.parametrize('data',
                         [
                             # # Неоплаченный просроченный счет без актов и откруток
                             # pytest.mark.smoke({'is_invoice_paid': False,
                             #                    'is_invoice_expired': True,
                             #                    'campaigns_before_act': False,
                             #                    'is_act_needed': False,
                             #                    'campaigns_after_act': None,
                             #                    'expected_invoice_params_after_job': {'consume_sum': 0,
                             #                                                          'overdraft': 0,
                             #                                                          'type': 'prepayment',
                             #                                                          'payment_term_dt': PAYMENT_TERM_DT.date(),
                             #                                                          'payment_term_id': None
                             #                                                          },
                             #                    'expected_consume_params_after_job': {'current_sum': 0,
                             #                                                          'consume_sum': 300,
                             #                                                          'current_qty': 0,
                             #                                                          'act_qty': 0,
                             #                                                          'completion_sum': 0,
                             #                                                          'completion_qty': 0,
                             #                                                          'act_sum': 0,
                             #                                                          'consume_qty': 10},
                             #                    'is_blocked': True
                             #                    }),
                             #
                             # # Неоплаченный просроченный счет без актов и откруток
                             # pytest.mark.smoke({'is_invoice_paid': False,
                             #                    'is_invoice_paid_instant': True,
                             #                    'is_invoice_expired': True,
                             #                    'campaigns_before_act': False,
                             #                    'is_act_needed': False,
                             #                    'campaigns_after_act': None,
                             #                    'expected_invoice_params_after_job': {'consume_sum': 0,
                             #                                                          'overdraft': 0,
                             #                                                          'type': 'prepayment',
                             #                                                          'payment_term_dt': PAYMENT_TERM_DT.date(),
                             #                                                          'payment_term_id': None
                             #                                                          },
                             #                    'expected_consume_params_after_job': {'current_sum': 0,
                             #                                                          'consume_sum': 300,
                             #                                                          'current_qty': 0,
                             #                                                          'act_qty': 0,
                             #                                                          'completion_sum': 0,
                             #                                                          'completion_qty': 0,
                             #                                                          'act_sum': 0,
                             #                                                          'consume_qty': 10},
                             #                    'is_blocked': True
                             #                    }),
                             #
                             # # Частично оплаченный просроченный счет без актов и откруток
                             # {'is_invoice_paid': 299.99,
                             #  'is_invoice_expired': True,
                             #  'campaigns_before_act': False,
                             #  'is_act_needed': False,
                             #  'campaigns_after_act': None,
                             #  'expected_invoice_params_after_job': {'consume_sum': 0,
                             #                                        'overdraft': 1,
                             #                                        'type': 'overdraft',
                             #                                        'payment_term_dt': PAYMENT_TERM_DT.date(),
                             #                                        },
                             #  'expected_consume_params_after_job': {'current_sum': 0,
                             #                                        'consume_sum': 300,
                             #                                        'current_qty': 0,
                             #                                        'act_qty': 0,
                             #                                        'completion_sum': 0,
                             #                                        'completion_qty': 0,
                             #                                        'act_sum': 0,
                             #                                        'consume_qty': 10},
                             #  'is_blocked': True
                             #  },
                             #
                             # # Частично оплаченный просроченный счет без актов и откруток, мгновенный способ оплаты
                             # {'is_invoice_paid': 299.99,
                             #  'is_invoice_expired': True,
                             #  'is_instant_paysys': True,
                             #  'campaigns_before_act': False,
                             #  'is_act_needed': False,
                             #  'campaigns_after_act': None,
                             #  'expected_invoice_params_after_job': {'consume_sum': 0,
                             #                                        'overdraft': 0,
                             #                                        'type': 'prepayment',
                             #                                        'payment_term_id': None
                             #                                        },
                             #  'expected_consume_params_after_job': {'current_sum': 0,
                             #                                        'consume_sum': 300,
                             #                                        'current_qty': 0,
                             #                                        'act_qty': 0,
                             #                                        'completion_sum': 0,
                             #                                        'completion_qty': 0,
                             #                                        'act_sum': 0,
                             #                                        'consume_qty': 10},
                             #  'is_blocked': True
                             #  },
                             #
                             # # Полностью оплаченный просроченный счет без актов и откруток
                             # {'is_invoice_paid': 300,
                             #  'is_invoice_expired': True,
                             #  'campaigns_before_act': False,
                             #  'is_act_needed': False,
                             #  'campaigns_after_act': None,
                             #  'expected_invoice_params_after_job': {'consume_sum': 300,
                             #                                        'overdraft': 1,
                             #                                        'type': 'overdraft',
                             #                                        'payment_term_dt': PAYMENT_TERM_DT.date(),
                             #                                        },
                             #  'expected_consume_params_after_job': {'current_sum': 300,
                             #                                        'consume_sum': 300,
                             #                                        'current_qty': 10,
                             #                                        'act_qty': 0,
                             #                                        'completion_sum': 0,
                             #                                        'completion_qty': 0,
                             #                                        'act_sum': 0,
                             #                                        'consume_qty': 10},
                             #  'is_blocked': False
                             #  },
                             #
                             # # Неоплаченный непросроченный счет без актов и откруток
                             # {'is_invoice_paid': False,
                             #  'is_invoice_expired': False,
                             #  'campaigns_before_act': False,
                             #  'is_act_needed': False,
                             #  'campaigns_after_act': None,
                             #  'expected_invoice_params_after_job': {'consume_sum': 300,
                             #                                        'overdraft': 1,
                             #                                        'type': 'overdraft',
                             #                                        },
                             #  'expected_consume_params_after_job': {'current_sum': 300,
                             #                                        'consume_sum': 300,
                             #                                        'current_qty': 10,
                             #                                        'act_qty': 0,
                             #                                        'completion_sum': 0,
                             #                                        'completion_qty': 0,
                             #                                        'act_sum': 0,
                             #                                        'consume_qty': 10},
                             #  'is_blocked': False
                             #  },

                             # Неоплаченный просроченный счет, с актами и открутками,
                             #  сумма актов больше, чем сумма открученного
                             {'is_invoice_paid': False,
                              'is_invoice_expired': True,
                              'campaigns_before_act': 5,
                              'is_act_needed': True,
                              'campaigns_after_act': 3,
                              'expected_invoice_params_after_job': {'consume_sum': 300,
                                                                    'overdraft': 1,
                                                                    'type': 'overdraft',
                                                                    'payment_term_dt': PAYMENT_TERM_DT.date(),
                                                                    },
                              'expected_consume_params_after_job': {'current_sum': 300,
                                                                    'consume_sum': 300,
                                                                    'current_qty': 10,
                                                                    'act_qty': 5,
                                                                    'completion_sum': 90,
                                                                    'completion_qty': 3,
                                                                    'act_sum': 150,
                                                                    'consume_qty': 10},
                              'is_blocked': False
                              },

                             # Неоплаченный просроченный счет, с актами и открутками, сумма откруток равна
                             # сумме актов
                             {'is_invoice_paid': False,
                              'is_invoice_expired': True,
                              'campaigns_before_act': 5,
                              'is_act_needed': True,
                              'campaigns_after_act': None,
                              'expected_invoice_params_after_job': {'consume_sum': 300,
                                                                    'overdraft': 1,
                                                                    'type': 'overdraft',
                                                                    'payment_term_dt': PAYMENT_TERM_DT.date(),
                                                                    },
                              'expected_consume_params_after_job': {'current_sum': 300,
                                                                    'consume_sum': 300,
                                                                    'current_qty': 10,
                                                                    'act_qty': 5,
                                                                    'completion_sum': 150,
                                                                    'completion_qty': 5,
                                                                    'act_sum': 150,
                                                                    'consume_qty': 10},
                              'is_blocked': False
                              },

                             # Неоплаченный просроченный счет, с актами и открутками, сумма откруток больше,
                             #  чем сумма актов
                             {'is_invoice_paid': False,
                              'is_invoice_expired': True,
                              'campaigns_before_act': 3,
                              'is_act_needed': True,
                              'campaigns_after_act': 5,
                              'expected_invoice_params_after_job': {'consume_sum': 150,
                                                                    'overdraft': 1,
                                                                    'type': 'overdraft',
                                                                    'payment_term_dt': PAYMENT_TERM_DT.date(),
                                                                    },
                              'expected_consume_params_after_job': {'current_sum': 150,
                                                                    'consume_sum': 300,
                                                                    'current_qty': 5,
                                                                    'act_qty': 3,
                                                                    'completion_sum': 150,
                                                                    'completion_qty': 5,
                                                                    'act_sum': 90,
                                                                    'consume_qty': 10},
                              'is_blocked': True
                              },

                             # Неоплаченный просроченный счет, с актами и открутками, сумма откруток равна 0
                             {'is_invoice_paid': False,
                              'is_invoice_expired': True,
                              'campaigns_before_act': 5,
                              'is_act_needed': True,
                              'campaigns_after_act': 0,
                              'expected_invoice_params_after_job': {'consume_sum': 150,
                                                                    'overdraft': 1,
                                                                    'type': 'overdraft',
                                                                    'payment_term_dt': PAYMENT_TERM_DT.date(),
                                                                    },
                              'expected_consume_params_after_job': {'current_sum': 150,
                                                                    'consume_sum': 300,
                                                                    'current_qty': 5,
                                                                    'act_qty': 5,
                                                                    'completion_sum': 0,
                                                                    'completion_qty': 0,
                                                                    'act_sum': 150,
                                                                    'consume_qty': 10},
                              'is_blocked': True
                              }
                         ]
                         )
@pytest.mark.parametrize('context', [DIRECT])
def test_overdraft_invoice_reset_ch(data, context):
    # steps.CloseMonth.resolve_task('monthly_limits')
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, OVERDRAFT_LIMIT, context.firm.id)

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)

    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': NOW}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW))

    if data.get('is_instant_paysys', False):
        context.paysys = Paysyses.CC_PH_RUB
    else:
        context.paysys = Paysyses.BANK_PH_RUB
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    if data['is_invoice_paid']:
        if data.get('is_instant_paysys', False):
            steps.InvoiceSteps.pay(invoice_id, payment_sum=data['is_invoice_paid'])
        else:
            steps.InvoiceSteps.turn_on(invoice_id, sum=data['is_invoice_paid'])

    if data['campaigns_before_act']:
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id,
                                          campaigns_params={'Bucks': data['campaigns_before_act']}, do_stop=0,
                                          campaigns_dt=NOW)

    if data['is_act_needed']:
        steps.ActsSteps.generate(client_id, force=1, date=NOW)

    if data['campaigns_after_act'] is not None:
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id,
                                          campaigns_params={'Bucks': data['campaigns_after_act']}, do_stop=0,
                                          campaigns_dt=NOW)

    if data['is_invoice_expired']:
        steps.OverdraftSteps.expire_overdraft_invoice(invoice_id, delta=5)

    steps.OverdraftSteps.reset_overdraft_invoices(client_id)

    invoice_after_reset = db.get_invoice_by_id(invoice_id)[0]
    print invoice_after_reset[0]['total_sum']


