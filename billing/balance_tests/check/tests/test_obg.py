#!/usr/bin/python
# coding=utf-8

from decimal import Decimal

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



@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG)
def test_campaign_without_changes(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество открученного сходится
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
    ) as before:
        before.validate()

        client_id, person_id, service_id, check_defaults = fixtures

        act_id = check_steps.create_act(client_id, person_id, check_defaults['paysys_id'], service_id,
                                        check_defaults['product_id'], QTY_DAYS=QTY_DAYS)
        service_order_id = db.get_service_order_id_by_act_id(act_id)

        order_id = db.get_order_id(service_order_id, service_id)
        geo_data = [service_order_id, db.get_completion_qty(order_id)]

    cmp_data = shared_steps.SharedBlocks.run_obg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    b_utils.check_that(result, empty(), "Расхождений нет")



@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG)
def test_campaign_not_found_in_billing(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Справочнике
        -заказ отсутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в биллинге"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
    ) as before:
        before.validate()

        client_id, person_id, service_id, check_defaults = fixtures

        act_id = check_steps.create_act(client_id, person_id, check_defaults['paysys_id'], service_id,
                                        check_defaults['product_id'], QTY_DAYS=QTY_DAYS)

        service_order_id = steps.OrderSteps.next_id(service_id)

        geo_data = [service_order_id, 121]

    cmp_data = shared_steps.SharedBlocks.run_obg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    expected_result = [(service_order_id, 2)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Заказ со статусом 2 ('Отсутствует в биллинге') попадает в список "
                       "расхождений: {}".format(result))



@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG)
def test_campaign_with_changed_sum(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Справочнике, сумма открученного на дату сверки = 100 ед.
        -заказ присутствует в Биллинге, сумма открученного на дату сверки = 150 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходятся суммы открученного"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
    ) as before:
        before.validate()

        client_id, person_id, service_id, check_defaults = fixtures

        act_id = check_steps.create_act(client_id, person_id, check_defaults['paysys_id'], service_id,
                                        check_defaults['product_id'], QTY_DAYS=QTY_DAYS)
        service_order_id = db.get_service_order_id_by_act_id(act_id)
        order_id = db.get_order_id(service_order_id, service_id)
        geo_data = [service_order_id, db.get_completion_qty(order_id) + 1]

    cmp_data = shared_steps.SharedBlocks.run_obg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    expected_result = [(service_order_id, 4)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Заказ со статусом 4 ('Расходятся суммы открученного') попадает в список "
                       "расхождений: {}".format(result))



@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG)
def test_not_integet_shipment(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Справочнике, сумма открученного на дату сверки = 8.75 ед.
        -заказ присутствует в Биллинге, сумма открученного на дату сверки = 8.75 ед.
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
    ) as before:
        before.validate()

        client_id, person_id, service_id, check_defaults = fixtures

        act_id = check_steps.create_act(client_id, person_id, check_defaults['paysys_id'], service_id,
                                        check_defaults['product_id'], QTY_DAYS=Decimal("8.75"))
        service_order_id = db.get_service_order_id_by_act_id(act_id)
        order_id = db.get_order_id(service_order_id, service_id)
        geo_data= [service_order_id, db.get_completion_qty(order_id)]

    cmp_data = shared_steps.SharedBlocks.run_obg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    b_utils.check_that(result, empty(), "Расхождений нет")



@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG)
def test_not_integet_shipment_with_diff(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Справочнике, сумма открученного на дату сверки = 9 ед.
        -заказ присутствует в Биллинге, сумма открученного на дату сверки = 8.75 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходятся суммы открученного" (4)
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['service_order_id', 'geo_data']
    ) as before:
        before.validate()

        client_id, person_id, service_id, check_defaults = fixtures

        act_id = check_steps.create_act(client_id, person_id, check_defaults['paysys_id'], service_id,
                                        check_defaults['product_id'], QTY_DAYS=Decimal("8.75"))
        service_order_id = db.get_service_order_id_by_act_id(act_id)
        order_id = db.get_order_id(service_order_id, service_id)
        geo_data = [service_order_id, round(Decimal(db.get_completion_qty(order_id)))]

    cmp_data = shared_steps.SharedBlocks.run_obg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    expected_result = [(service_order_id, 4)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Заказ со статусом 4 ('Расходятся суммы открученного') попадает в список "
                       "расхождений: {}".format(result))



@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBG)
def test_campaign_not_found_in_geo(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Биллинге
        -заказ отсутствует в Справочнике
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в Справочнике" (1)
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['service_order_id']
    ) as before:
        before.validate()

        client_id, person_id, service_id, check_defaults = fixtures

        act_id = check_steps.create_act(client_id, person_id, check_defaults['paysys_id'], service_id,
                                        check_defaults['product_id'], QTY_DAYS=QTY_DAYS)
        service_order_id = db.get_service_order_id_by_act_id(act_id)

    cmp_data = shared_steps.SharedBlocks.run_obg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['order_id'], row['state']) for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT: {}".format(result))

    expected_result = [(service_order_id, 1)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Заказ со статусом 1 ('Отсутствует в Справочнике') попадает в список "
                       "расхождений: {}".format(result))
