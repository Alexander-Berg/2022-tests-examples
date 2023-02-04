#!/usr/bin/python
# coding=utf-8

from decimal import Decimal
from collections import OrderedDict
from dateutil.relativedelta import relativedelta

import pytest
from hamcrest import contains_string, is_in, empty, has_length, equal_to

from balance import balance_steps as steps
from btestlib import utils as b_utils
import btestlib.reporter as reporter
from check import db
from check import defaults
from check import steps as check_steps
from check import shared_steps
from check.shared import CheckSharedBefore

"""
    В рамках теста осуществляется сравнение оказанных услуг (откруток) в рамках одного заказа в Биллинге и Справочнике
"""

QTY_DAYS = Decimal("22")
DIFFS_COUNT = 4

@pytest.fixture(scope="module")
def fixtures():
    check_code = 'obg'
    check_defaults = defaults.data[check_code]
    client_id = check_steps.create_client()
    person_id = check_steps.create_person(
        client_id, person_category=check_defaults['person_category'],
        additional_params=check_defaults['person_additional_params']
        )
    service_id = check_defaults['service_id']
    return client_id, person_id, service_id, check_defaults


"""
По умолчанию сверка сверяет за предыдущий месяц
В тесте специально указываем даты - для наглядности
"""

START_DT = b_utils.Date.first_day_of_month() - relativedelta(months=1)
END_DT = START_DT + relativedelta(months=1, days=-1)  # last day of previous month

# DATES_VARS = ['start_dt', 'end_dt']
# COMPLETIONS_VARS = ['billing_completions', 'geo_completions']


def rd(days):
    return START_DT + relativedelta(days=days - 1)

def create_billing_completions(service_id, service_order_id, dict):
    for key, value in dict.items():
        steps.CampaignsSteps.do_campaigns(service_id, service_order_id,
                                          campaigns_params={'Days': value},
                                          do_stop=0,
                                          campaigns_dt=rd(key))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG2)
def test_campaign_yt_without_changes(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество открученного сходится
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            # shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
            shared_data=shared_data, cache_vars=['geo_completions', 'service_order_id',
                                                 'start_dt', 'end_dt']
    ) as before:
        before.validate()

        start_dt = START_DT
        end_dt = END_DT

        client_id, person_id, service_id, check_defaults = fixtures

        # TODO create method and put all below into it
        order_id, service_order_id = check_steps.create_order(client_id, service_id,
                                                              check_defaults['product_id'],
                                                              agency_id=None)


        #TODO сделать открутку до сверяемого периода, чтобы проверить как запрос в сверке отработает
        # steps.CampaignsSteps.do_campaigns(service_id, service_order_id,
        #                                   campaigns_params={'Days': Decimal("200")},
        #                                   do_stop=0,
        #                                   campaigns_dt=rd(1))
        #
        # steps.CampaignsSteps.do_campaigns(service_id, service_order_id,
        #                                   campaigns_params={'Days': Decimal("300")},  #+100
        #                                   do_stop=0,
        #                                   campaigns_dt=rd(2))
        # steps.CampaignsSteps.do_campaigns(service_id, service_order_id,
        #                                   campaigns_params={'Days': Decimal("310")},  #+10
        #                                   do_stop=0,
        #                                   campaigns_dt=rd(3))
        # steps.CampaignsSteps.do_campaigns(service_id, service_order_id,
        #                                   campaigns_params={'Days': Decimal("322.35")}, #+12.35
        #                                   do_stop=0,
        #                                   campaigns_dt=rd(4))

        bill_completions = {
            1: Decimal('200'),
            2: Decimal('300'),
            3: Decimal('310'),
            4: Decimal('322.35')
        }

        create_billing_completions(service_id, service_order_id, bill_completions)


        # service_order_id = db.get_service_order_id_by_act_id(client_id)


        # TODO look at cbd check. "rd" constantly increase in next checks

        geo_completions = [
            {'amount': '200', 'dt': rd(1)},
            {'amount': '100', 'dt': rd(2)},
            {'amount': '10', 'dt': rd(3)},
            {'amount': '12.35', 'dt': rd(4)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_obg2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    b_utils.check_that(result, empty(), "Расхождений нет")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG2)
def test_campaign_yt_without_changes_with_prev_completion(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        - есть открутки за прошлый период
        -количество открученного сходится
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            # shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
            shared_data=shared_data, cache_vars=['geo_completions', 'service_order_id',
                                                 'start_dt', 'end_dt']
    ) as before:
        before.validate()

        start_dt = START_DT
        end_dt = END_DT

        client_id, person_id, service_id, check_defaults = fixtures

        # TODO create method and put all below into it
        order_id, service_order_id = check_steps.create_order(client_id, service_id,
                                                              check_defaults['product_id'],
                                                              agency_id=None)

        bill_completions = OrderedDict([
            (-10, Decimal('111')),
            (1, Decimal('311')),
            (2, Decimal('411')),
            (3, Decimal('421')),
            (4, Decimal('433.35'))
        ])

        create_billing_completions(service_id, service_order_id, bill_completions)

        geo_completions = [
            {'amount': '111', 'dt': rd(-10)},
            {'amount': '200', 'dt': rd(1)},
            {'amount': '100', 'dt': rd(2)},
            {'amount': '10', 'dt': rd(3)},
            {'amount': '12.35', 'dt': rd(4)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_obg2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    b_utils.check_that(result, empty(), "Расхождений нет")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG2)
def test_not_integer_shipment(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Справочнике, сумма открученного на дату сверки = 322.43 ед.
        -заказ присутствует в Биллинге, сумма открученного на дату сверки = 322.43 ед.
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            # shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
            shared_data=shared_data, cache_vars=['geo_completions', 'service_order_id',
                                                 'start_dt', 'end_dt']
    ) as before:
        before.validate()

        start_dt = START_DT
        end_dt = END_DT

        client_id, person_id, service_id, check_defaults = fixtures

        # TODO create method and put all below into it
        order_id, service_order_id = check_steps.create_order(client_id, service_id,
                                                              check_defaults['product_id'],
                                                              agency_id=None)

        bill_completions = {
            1: Decimal('200.11'),
            2: Decimal('299.14'),
            3: Decimal('309.44'),
            4: Decimal('322.43')
        }

        create_billing_completions(service_id, service_order_id, bill_completions)

        geo_completions = [
            {'amount': '200.11', 'dt': rd(1)},
            {'amount': '99.03', 'dt': rd(2)},
            {'amount': '10.30', 'dt': rd(3)},
            {'amount': '12.99', 'dt': rd(4)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_obg2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    b_utils.check_that(result, empty(), "Расхождений нет")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG2)
def test_not_integer_shipment_with_diff(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Справочнике, сумма открученного на дату сверки = 199,98 ед.
        -заказ присутствует в Биллинге, сумма открученного на дату сверки = 200 ед.
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            # shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
            shared_data=shared_data, cache_vars=['geo_completions', 'service_order_id',
                                                 'start_dt', 'end_dt']
    ) as before:
        before.validate()

        start_dt = START_DT
        end_dt = END_DT

        client_id, person_id, service_id, check_defaults = fixtures

        # TODO create method and put all below into it
        order_id, service_order_id = check_steps.create_order(client_id, service_id,
                                                              check_defaults['product_id'],
                                                              agency_id=None)

        bill_completions = {
            2: Decimal('100'),
            3: Decimal('200'),
        }

        create_billing_completions(service_id, service_order_id, bill_completions)

        geo_completions = [
            {'amount': '100', 'dt': rd(2)},
            {'amount': '99.98', 'dt': rd(3)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_obg2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    expected_result = [(service_order_id, 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Заказ со статусом 3 ('Расходятся суммы открученного') попадает в список "
                       "расхождений: {}".format(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG2)
def test_campaign_not_found_in_billing(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Справочнике
        -заказ отсутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в биллинге
    """
    with CheckSharedBefore(
            # shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
            shared_data=shared_data, cache_vars=['geo_completions', 'service_order_id',
                                                 'start_dt', 'end_dt']
    ) as before:
        before.validate()

        start_dt = START_DT
        end_dt = END_DT

        client_id, person_id, service_id, check_defaults = fixtures

        service_order_id = steps.OrderSteps.next_id(service_id)

        geo_completions = [
            {'amount': '11', 'dt': rd(3)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_obg2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    expected_result = [(service_order_id, 2)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Заказ со статусом 2 ('Отсутствует в биллинге') попадает в список "
                       "расхождений: {}".format(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG2)
def test_campaign_not_found_in_geo(shared_data, fixtures):
    """
    Начальные условия:
        -заказ отсутствует в Справочнике
        -заказ присутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в справочнике
    """
    with CheckSharedBefore(
            # shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
            shared_data=shared_data, cache_vars=['geo_completions', 'service_order_id',
                                                 'start_dt', 'end_dt']
    ) as before:
        before.validate()

        start_dt = START_DT
        end_dt = END_DT

        client_id, person_id, service_id, check_defaults = fixtures

        # TODO create method and put all below into it
        order_id, service_order_id = check_steps.create_order(client_id, service_id,
                                                              check_defaults['product_id'],
                                                              agency_id=None)

        bill_completions = {
            1: Decimal('200'),
            2: Decimal('300'),
            3: Decimal('310'),
            4: Decimal('322.35')
        }

        create_billing_completions(service_id, service_order_id, bill_completions)

        geo_completions = None

    cmp_data = shared_steps.SharedBlocks.run_obg2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    expected_result = [(service_order_id, 1)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Заказ со статусом 1 ('Отсутствует в Справочнике') попадает в список "
                       "расхождений: {}".format(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG2)
def test_campaign_yt_with_changes(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество открученного расходится
    Ожидаемый результат:
        заказ присутствует в списке расхождений
    """
    with CheckSharedBefore(
            # shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
            shared_data=shared_data, cache_vars=['geo_completions', 'service_order_id',
                                                 'start_dt', 'end_dt']
    ) as before:
        before.validate()

        start_dt = START_DT
        end_dt = END_DT

        client_id, person_id, service_id, check_defaults = fixtures

        # TODO create method and put all below into it
        order_id, service_order_id = check_steps.create_order(client_id, service_id,
                                                              check_defaults['product_id'],
                                                              agency_id=None)

        bill_completions = {
            1: Decimal('210'),
            2: Decimal('310'),
            3: Decimal('320'),
            4: Decimal('332.35')
        }

        create_billing_completions(service_id, service_order_id, bill_completions)

        geo_completions = [
            {'amount': '200', 'dt': rd(1)},
            {'amount': '100', 'dt': rd(2)},
            {'amount': '10', 'dt': rd(3)},
            {'amount': '12.35', 'dt': rd(4)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_obg2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    expected_result = [(service_order_id, 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Заказ со статусом 3 ('Расходятся суммы открученного') попадает в список "
                       "расхождений: {}".format(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG2)
def test_obg2_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_obg2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    # active_tests = [row for row in pytest.active_tests if 'test_obg2.py' in row]
    assert len(cmp_data) == DIFFS_COUNT
