# -*- coding: utf-8 -*-

from datetime import datetime
from decimal import Decimal as D

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import anything

import balance.balance_db as db
from balance.balance_steps import new_taxi_steps as steps
from balance.balance_steps import ContractSteps
from balance.balance_steps.new_taxi_steps import DEFAULT_TAXI_CONTEXTS, DEFAULT_PARAMETRIZATION, \
    DEFAULT_TAXI_CONTEXTS_WITH_MARKS

from balance.balance_steps.other_steps import SharedBlocks
from btestlib import shared
from btestlib import utils
from btestlib.constants import Currencies
from btestlib.constants import PaymentType
from btestlib.constants import Services
from btestlib.constants import TaxiOrderType
from btestlib.data.defaults import TaxiNewPromo as Taxi
from btestlib.matchers import contains_dicts_equal_to
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
COMPLETION_DT = utils.Date.first_day_of_month()

DONT_TEST_CONTEXTS_NAME = {
    'TAXI_BV_NOR_NOK_CONTEXT',
    'TAXI_YANGO_ISRAEL_CONTEXT',
    'TAXI_MLU_EUROPE_SWE_SEK_CONTEXT',
}


def get_v_partner_taxi_completion_data(contract_id):
    query = "SELECT dt, client_id, contract_id, currency_id, payment_type, " \
            "currency, iso_currency, order_type, card_commission_sum, cash_commission_sum, " \
            "promocode_sum, subsidy_sum, commission_sum, service_id FROM v_partner_taxi_completion " \
            "WHERE contract_id = :contract_id"
    params = {'contract_id': contract_id}
    data = db.balance().execute(query, params, descr='Выбираем открутки такси по договору')
    for item in data:
        item['card_commission_sum'] = D(item['card_commission_sum'])
        item['cash_commission_sum'] = D(item['cash_commission_sum'])
        item['commission_sum'] = D(item['commission_sum'])
    data.sort(key=lambda k: (k['commission_sum'], k['payment_type'], k['order_type']))
    return data


def get_v_partner_taxi_completion_data_tlog(contract_id):
    query = "SELECT dt, transaction_dt, client_id, contract_id, currency_id, " \
            "currency, iso_currency, order_type, amount, " \
            "service_id, last_transaction_id FROM v_partner_taxi_completion_tlog " \
            "WHERE contract_id = :contract_id"
    params = {'contract_id': contract_id}
    data = db.balance().execute(query, params, descr='Выбираем открутки tlog такси по договору')
    for item in data:
        item['amount'] = D(item['amount'])
    data.sort(key=lambda k: (k['service_id'], k['order_type'], k['amount']))
    return data


def get_amount_with_nds(amount, nds, dt):
    return amount * nds.koef_on_dt(dt)


def merge_common_and_unique_data(common, unique):
    final_data = common.copy()
    final_data.update(unique)
    return final_data


def create_expected_data(context, client_id, contract_id, currency, nds, dt):
    common_expected_data = {
        'contract_id': contract_id,
        'currency_id': currency.num_code,
        'currency': currency.iso_code,
        'client_id': client_id,
        'dt': dt,
        'promocode_sum': Taxi.promocode_sum,
        'subsidy_sum': D('0'),
        'iso_currency': currency.iso_code}

    expected_data = [
        # prepaid order data
        merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': get_amount_with_nds(Taxi.order_commission_cash, nds, dt),
            'order_type': TaxiOrderType.commission,
            'payment_type': PaymentType.PREPAID,
            'service_id': Services.TAXI_111.id,
            'commission_sum': get_amount_with_nds(Taxi.order_commission_cash, nds, dt)}),
        # cash order data
        merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': get_amount_with_nds(Taxi.order_commission_cash, nds, dt),
            'order_type': TaxiOrderType.commission,
            'payment_type': PaymentType.CASH,
            'service_id': Services.TAXI_111.id,
            'commission_sum': get_amount_with_nds(Taxi.order_commission_cash, nds, dt)}),
        # card order data
        merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': get_amount_with_nds(Taxi.order_commission_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'cash_commission_sum': D('0'),
            'order_type': TaxiOrderType.commission,
            'payment_type': PaymentType.CARD,
            'promocode_sum': Taxi.promocode_sum if Services.TAXI_128.id in context.contract_services else D('0'),
            'service_id': Services.TAXI_128.id,
            'commission_sum': get_amount_with_nds(Taxi.order_commission_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0')}),
        # corp order data
        merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': get_amount_with_nds(Taxi.order_commission_corp, nds, dt),
            'order_type': TaxiOrderType.commission,
            'payment_type': PaymentType.CORPORATE,
            'service_id': Services.TAXI_111.id,
            'commission_sum': get_amount_with_nds(Taxi.order_commission_corp, nds, dt)}),
        # cash correction_commission data
        merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': get_amount_with_nds(Taxi.commission_correction_cash, nds, dt),
            'order_type': TaxiOrderType.commission_correction,
            'payment_type': PaymentType.CASH,
            'promocode_sum': D('0'),
            'service_id': Services.TAXI_111.id,
            'commission_sum': get_amount_with_nds(Taxi.commission_correction_cash, nds, dt)}),
        # card correction_commission data
        merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': get_amount_with_nds(Taxi.commission_correction_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'cash_commission_sum': D('0'),
            'order_type': TaxiOrderType.commission_correction,
            'payment_type': PaymentType.CARD,
            'promocode_sum': D('0'),
            'service_id': Services.TAXI_128.id,
            'commission_sum': get_amount_with_nds(Taxi.commission_correction_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0')}),
        # positive subsidy
        merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': D('0'),
            'order_type': TaxiOrderType.subsidy,
            'payment_type': PaymentType.CASH,
            'promocode_sum': D('0'),
            'subsidy_sum': Taxi.subsidy_sum,
            'service_id': Services.TAXI_111.id,
            'commission_sum': D('0')}),
        # negative subsidy (should be D('0'))
        merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': D('0'),
            'order_type': TaxiOrderType.subsidy,
            'payment_type': PaymentType.CASH,
            'promocode_sum': D('0'),
            'subsidy_sum': D('0'),
            'service_id': Services.TAXI_111.id,
            'commission_sum': D('0')}),
    ]

    if currency == Currencies.RUB or currency == Currencies.ILS:
        # childchair cash data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': get_amount_with_nds(Taxi.childchair_cash, nds, dt),
            'order_type': TaxiOrderType.childchair,
            'payment_type': PaymentType.CASH,
            'service_id': Services.TAXI_111.id,
            'commission_sum': get_amount_with_nds(Taxi.childchair_cash, nds, dt)}))
        # childchair card data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': get_amount_with_nds(Taxi.childchair_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'cash_commission_sum': D('0'),
            'order_type': TaxiOrderType.childchair,
            'promocode_sum': Taxi.promocode_sum if Services.TAXI_128.id in context.contract_services else D('0'),
            'payment_type': PaymentType.CARD,
            'service_id': Services.TAXI_128.id,
            'commission_sum': get_amount_with_nds(Taxi.childchair_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0')}))
        # childchair corp data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': get_amount_with_nds(Taxi.childchair_corp, nds, dt),
            'order_type': TaxiOrderType.childchair,
            'payment_type': PaymentType.CORPORATE,
            'service_id': Services.TAXI_111.id,
            'commission_sum': get_amount_with_nds(Taxi.childchair_corp, nds, dt)}))
        # hiring_with_car cash data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': get_amount_with_nds(Taxi.hiring_with_car_cash, nds, dt),
            'order_type': TaxiOrderType.hiring_with_car,
            'payment_type': PaymentType.CASH,
            'service_id': Services.TAXI_111.id,
            'commission_sum': get_amount_with_nds(Taxi.hiring_with_car_cash, nds, dt)}))
        # hiring_with_car card data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': get_amount_with_nds(Taxi.hiring_with_car_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'cash_commission_sum': D('0'),
            'order_type': TaxiOrderType.hiring_with_car,
            'promocode_sum': Taxi.promocode_sum if Services.TAXI_128.id in context.contract_services else D('0'),
            'payment_type': PaymentType.CARD,
            'service_id': Services.TAXI_128.id,
            'commission_sum': get_amount_with_nds(Taxi.hiring_with_car_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0')}))
        # marketplace data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': get_amount_with_nds(Taxi.marketplace_advert_call_cash, nds, dt),
            'order_type': TaxiOrderType.marketplace_advert_call,
            'payment_type': PaymentType.CASH,
            'service_id': Services.TAXI_111.id,
            'promocode_sum': D('0'),
            'subsidy_sum': D('0'),
            'commission_sum': get_amount_with_nds(Taxi.marketplace_advert_call_cash, nds, dt)}))
        # driver_workshift card data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': get_amount_with_nds(Taxi.driver_workshift_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'cash_commission_sum': D('0'),
            'order_type': TaxiOrderType.driver_workshift,
            'payment_type': PaymentType.CARD,
            'promocode_sum': D('0'),
            'service_id': Services.TAXI_128.id,
            'commission_sum': get_amount_with_nds(Taxi.driver_workshift_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0')}))

    if currency in (Currencies.USD, Currencies.RUB, Currencies.ILS, Currencies.KZT, Currencies.RON):
        # driver_workshift cash data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'card_commission_sum': D('0'),
            'cash_commission_sum': get_amount_with_nds(Taxi.driver_workshift_cash, nds, dt),
            'order_type': TaxiOrderType.driver_workshift,
            'payment_type': PaymentType.CASH,
            'promocode_sum': D('0'),
            'service_id': Services.TAXI_111.id,
            'commission_sum': get_amount_with_nds(Taxi.driver_workshift_cash, nds, dt)}))

    expected_data.sort(key=lambda k: (k['commission_sum'], k['payment_type'], k['order_type']))
    return expected_data


def create_expected_data_tlog(context, client_id, contract_id, currency, nds, dt, transaction_dt=None):
    if not transaction_dt:
        transaction_dt = dt
    common_expected_data = {
        'contract_id': contract_id,
        'currency_id': currency.num_code,
        'currency': currency.iso_code,
        'client_id': client_id,
        'dt': dt,
        'transaction_dt': transaction_dt,
        'iso_currency': currency.iso_code,
        'last_transaction_id': anything()}

    expected_data = [
        # cash order data
        merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.order_commission_cash, nds, dt),
            'order_type': TaxiOrderType.commission,
            'service_id': Services.TAXI_111.id,
            }),
        merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.order_commission_cash / D('-2'), nds, dt),
            'order_type': TaxiOrderType.commission,
            'service_id': Services.TAXI_111.id,
        }),
        # card order data
        merge_common_and_unique_data(common_expected_data, {
            'service_id': Services.TAXI_128.id,
            'amount': get_amount_with_nds(Taxi.order_commission_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.commission,
        }),
        merge_common_and_unique_data(common_expected_data, {
            'service_id': Services.TAXI_128.id,
            'amount': get_amount_with_nds(Taxi.order_commission_card / D('-2'), nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.commission,
        }),
        # cash correction_commission data
        merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.commission_correction_cash, nds, dt),
            'order_type': TaxiOrderType.commission_correction,
            'service_id': Services.TAXI_111.id,
        }),
        merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.commission_correction_cash / D('-2'), nds, dt),
            'order_type': TaxiOrderType.commission_correction,
            'service_id': Services.TAXI_111.id,
        }),
        # card correction_commission data
        merge_common_and_unique_data(common_expected_data, {
            'service_id': Services.TAXI_128.id,
            'amount': get_amount_with_nds(Taxi.commission_correction_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.commission_correction,
        }),
        merge_common_and_unique_data(common_expected_data, {
            'service_id': Services.TAXI_128.id,
            'amount': get_amount_with_nds(Taxi.commission_correction_card / D('-2'), nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.commission_correction,
        }),
        # cash subsidy
        merge_common_and_unique_data(common_expected_data, {
            'amount': Taxi.subsidy_sum,
            'order_type': TaxiOrderType.subsidy_tlog,
            'service_id': Services.TAXI_111.id,
        }),
        merge_common_and_unique_data(common_expected_data, {
            'amount': Taxi.subsidy_sum / D('-2'),
            'order_type': TaxiOrderType.subsidy_tlog,
            'service_id': Services.TAXI_111.id,
        }),
        # card subsidy
        merge_common_and_unique_data(common_expected_data, {
            'amount': Taxi.subsidy_sum if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.subsidy_tlog,
            'service_id': Services.TAXI_128.id,
        }),
        merge_common_and_unique_data(common_expected_data, {
            'amount': Taxi.subsidy_sum / D('-2') if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.subsidy_tlog,
            'service_id': Services.TAXI_128.id,
        }),
        # cash promocode
        merge_common_and_unique_data(common_expected_data, {
            'amount': Taxi.promocode_sum,
            'order_type': TaxiOrderType.promocode_tlog,
            'service_id': Services.TAXI_111.id,
        }),
        merge_common_and_unique_data(common_expected_data, {
            'amount': Taxi.promocode_sum / D('-2'),
            'order_type': TaxiOrderType.promocode_tlog,
            'service_id': Services.TAXI_111.id,
        }),
        # card promocode
        merge_common_and_unique_data(common_expected_data, {
            'amount': Taxi.promocode_sum if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.promocode_tlog,
            'service_id': Services.TAXI_128.id,
        }),
        merge_common_and_unique_data(common_expected_data, {
            'amount': Taxi.promocode_sum / D('-2') if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.promocode_tlog,
            'service_id': Services.TAXI_128.id,
        }),

    ]
    if currency == Currencies.RUB or currency == Currencies.ILS:
        # childchair cash data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.childchair_cash, nds, dt),
            'order_type': TaxiOrderType.childchair,
            'service_id': Services.TAXI_111.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.childchair_cash / D('-2'), nds, dt),
            'order_type': TaxiOrderType.childchair,
            'service_id': Services.TAXI_111.id,
        }))
        # childchair card data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.childchair_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.childchair,
            'service_id': Services.TAXI_128.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.childchair_card / D('-2'), nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.childchair,
            'service_id': Services.TAXI_128.id,
        }))
        # hiring_with_car cash data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.hiring_with_car_cash, nds, dt),
            'order_type': TaxiOrderType.hiring_with_car,
            'service_id': Services.TAXI_111.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.hiring_with_car_cash / D('-2'), nds, dt),
            'order_type': TaxiOrderType.hiring_with_car,
            'service_id': Services.TAXI_111.id,
        }))
        # hiring_with_car card data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.hiring_with_car_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.hiring_with_car,
            'service_id': Services.TAXI_128.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.hiring_with_car_card / D('-2'), nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.hiring_with_car,
            'service_id': Services.TAXI_128.id,
        }))
        # marketplace data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
             'service_id': Services.TAXI_111.id,
             'amount': get_amount_with_nds(Taxi.marketplace_advert_call_cash, nds, dt),
             'order_type': TaxiOrderType.marketplace_advert_call,
        })),
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
             'service_id': Services.TAXI_111.id,
             'amount':  get_amount_with_nds(Taxi.marketplace_advert_call_cash / D('-2'), nds, dt),
             'order_type': TaxiOrderType.marketplace_advert_call,
        }))

        # driver_workshift card data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.driver_workshift_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.driver_workshift,
            'service_id': Services.TAXI_128.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.driver_workshift_card / D('-2'), nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.driver_workshift,
            'service_id': Services.TAXI_128.id,
        }))

    if currency in (Currencies.USD, Currencies.RUB, Currencies.ILS, Currencies.KZT, Currencies.RON):
        # driver_workshift cash data
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.driver_workshift_cash, nds, dt),
            'order_type': TaxiOrderType.driver_workshift,
            'service_id': Services.TAXI_111.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.driver_workshift_cash / D('-2'), nds, dt),
            'order_type': TaxiOrderType.driver_workshift,
            'service_id': Services.TAXI_111.id,
        }))

    # cargo data
    if currency == Currencies.RUB:
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.cargo_cash, nds, dt),
            'order_type': TaxiOrderType.cargo_order,
            'service_id': Services.TAXI_111.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.cargo_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.cargo_order,
            'service_id': Services.TAXI_128.id,
        }))

        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.cargo_driver_workshift_cash, nds, dt),
            'order_type': TaxiOrderType.cargo_driver_workshift,
            'service_id': Services.TAXI_111.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.cargo_driver_workshift_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.cargo_driver_workshift,
            'service_id': Services.TAXI_128.id,
        }))

        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.cargo_hiring_with_car_cash, nds, dt),
            'order_type': TaxiOrderType.cargo_hiring_with_car,
            'service_id': Services.TAXI_111.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.cargo_hiring_with_car_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.cargo_hiring_with_car,
            'service_id': Services.TAXI_128.id,
        }))

    # delivery data
    if currency in (Currencies.RUB, Currencies.USD, Currencies.ILS, Currencies.EUR):
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.delivery_cash, nds, dt),
            'order_type': TaxiOrderType.delivery_order,
            'service_id': Services.TAXI_111.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.delivery_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.delivery_order,
            'service_id': Services.TAXI_128.id,
        }))

        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.delivery_driver_workshift_cash, nds, dt),
            'order_type': TaxiOrderType.delivery_driver_workshift,
            'service_id': Services.TAXI_111.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.delivery_driver_workshift_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.delivery_driver_workshift,
            'service_id': Services.TAXI_128.id,
        }))

        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.delivery_hiring_with_car_cash, nds, dt),
            'order_type': TaxiOrderType.delivery_hiring_with_car,
            'service_id': Services.TAXI_111.id,
        }))
        expected_data.append(merge_common_and_unique_data(common_expected_data, {
            'amount': get_amount_with_nds(Taxi.delivery_hiring_with_car_card, nds, dt) if Services.TAXI_128.id in context.contract_services else D('0'),
            'order_type': TaxiOrderType.delivery_hiring_with_car,
            'service_id': Services.TAXI_128.id,
        }))

    expected_data.sort(key=lambda k: (k['service_id'], k['order_type'], k['amount']))
    return expected_data


@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_check_view_postpay(context, is_offer, shared_data):
    # не проверяем данные контексты, т.к. они появились уже после перехода на откруты из ОЕБС,
    # для них нет данных в проде.
    if context.name in DONT_TEST_CONTEXTS_NAME:
        return

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                           is_offer=is_offer,
                                                                                           additional_params=
                                                                                           {'start_dt': CONTRACT_START_DT})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = steps.TaxiData.generate_default_orders_data(COMPLETION_DT, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)

    expected_data = create_expected_data(context, client_id, contract_id, context.currency, context.nds, COMPLETION_DT)
    actual_data = get_v_partner_taxi_completion_data(contract_id)

    utils.check_that(actual_data, contains_dicts_equal_to(expected_data),
                     u'Проверяем, что данные из v_partner_taxi_completion совпадают с ожидаемыми')


@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS_WITH_MARKS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_check_view_prepay(context, is_offer, shared_data):
    # не проверяем данные контексты, т.к. они появились уже после перехода на откруты из ОЕБС,
    # для них нет данных в проде.
    if context.name in {'TAXI_BV_NOR_NOK_CONTEXT'}:
        return

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context, is_postpay=0,
                                                                                           is_offer=is_offer,
                                                                                           additional_params=
                                                                                           {'start_dt': CONTRACT_START_DT})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = steps.TaxiData.generate_default_orders_data(COMPLETION_DT, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)

    expected_data = create_expected_data(context, client_id, contract_id, context.currency, context.nds, COMPLETION_DT)
    actual_data = get_v_partner_taxi_completion_data(contract_id)

    utils.check_that(actual_data, contains_dicts_equal_to(expected_data),
                     u'Проверяем, что данные из v_partner_taxi_completion совпадают с ожидаемыми')


# Чекнем хардкодом, что НДС считается правильно при смене ставки!
# @pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, [DEFAULT_TAXI_CONTEXTS[0]], ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_check_view_nds_calc(shared_data):
    context = TAXI_RU_CONTEXT
    dt1_old_nds = datetime(2018, 12, 31)
    dt2_new_nds = datetime(2019,  1,  1)
    expected_data = []
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    commission_sum_wo_nds = D('100.11')
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context, is_postpay=0,
                                                                                           is_offer=1,
                                                                                           additional_params=
                                                                                           {'start_dt': dt1_old_nds})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    for dt in [dt1_old_nds, dt2_new_nds]:
        order_data = {
            'dt': dt,
            'payment_type': PaymentType.CASH,
            'order_type': TaxiOrderType.commission,
            'commission_sum': commission_sum_wo_nds,
            'currency': context.currency.iso_code,
        }
        steps.TaxiSteps.create_order(client_id, **order_data)
        expected_data.append({
            'contract_id': contract_id,
            'currency_id': context.currency.num_code,
            'currency': context.currency.iso_code,
            'client_id': client_id,
            'dt': dt,
            'promocode_sum': 0,
            'subsidy_sum': 0,
            'iso_currency': context.currency.iso_code,
            'card_commission_sum': 0,
            'cash_commission_sum': commission_sum_wo_nds * context.nds.koef_on_dt(dt),
            'order_type': TaxiOrderType.commission,
            'payment_type': PaymentType.CASH,
            'service_id': Services.TAXI_111.id,
            'commission_sum': commission_sum_wo_nds * context.nds.koef_on_dt(dt),
        })

    actual_data = get_v_partner_taxi_completion_data(contract_id)

    utils.check_that(actual_data, contains_dicts_equal_to(expected_data),
                     u'Проверяем, что данные из v_partner_taxi_completion совпадают с ожидаемыми')


@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_check_view_postpay_tlog(context, is_offer, shared_data):
    # не проверяем данные контексты, т.к. они появились уже после перехода на откруты из ОЕБС,
    # для них нет данных в проде.
    if context.name in DONT_TEST_CONTEXTS_NAME:
        return
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_offer=is_offer,
                                                                                     additional_params=
                                                                                       {'start_dt': CONTRACT_START_DT})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = steps.TaxiData.generate_default_orders_data_tlog(COMPLETION_DT, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data)

    expected_data = create_expected_data_tlog(context, client_id, contract_id, context.currency, context.nds, COMPLETION_DT)
    actual_data = get_v_partner_taxi_completion_data_tlog(contract_id)

    utils.check_that(actual_data, contains_dicts_equal_to(expected_data),
                     u'Проверяем, что данные из v_partner_taxi_completion совпадают с ожидаемыми')


@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS_WITH_MARKS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_check_view_prepay_tlog(context, is_offer, shared_data):
    # не проверяем данные контексты, т.к. они появились уже после перехода на откруты из ОЕБС,
    # для них нет данных проде.
    if context.name in DONT_TEST_CONTEXTS_NAME:
        return

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     is_offer=is_offer,
                                                                                     additional_params=
                                                                                       {'start_dt': CONTRACT_START_DT})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = steps.TaxiData.generate_default_orders_data_tlog(COMPLETION_DT, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data)

    expected_data = create_expected_data_tlog(context, client_id, contract_id, context.currency, context.nds, COMPLETION_DT)
    actual_data = get_v_partner_taxi_completion_data_tlog(contract_id)

    utils.check_that(actual_data, contains_dicts_equal_to(expected_data),
                     u'Проверяем, что данные из v_partner_taxi_completion совпадают с ожидаемыми')
