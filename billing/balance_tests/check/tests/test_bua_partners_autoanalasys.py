# coding: utf-8
__author__ = 'chihiro'
import datetime
from decimal import Decimal

import pytest
from hamcrest import contains_string
from startrek_client import Startrek

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import utils as b_utils
from check import steps as check_steps
from check import shared_steps
from check import utils
from check.defaults import Services, Products, STARTREK_PARAMS
from check.utils import relative_date, LAST_DAY_OF_MONTH
from check.shared import CheckSharedBefore

END_OF_MONTH = relative_date(months=-1, day=LAST_DAY_OF_MONTH)


@pytest.mark.skip
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA_PARTNER)
def test_bua_partners_auto_analasys(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()
        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ur')
        finish_dt = datetime.datetime.now() + datetime.timedelta(weeks=5)
        contract_id, _ = steps.ContractSteps.create_contract('no_agency_post', {'CLIENT_ID': client_id,
                                                                                'PERSON_ID': person_id,
                                                                                'DT': END_OF_MONTH,
                                                                                'SERVICES': [114],
                                                                                'FINISH_DT': finish_dt})

        # Заказ, у которого на последний день предыдущего месяца сумма
        # откруток больше, чем сумма актов по сервису Авиабилеты.

        # Создаем заказ с заакченной откруткой на последний день предыдущего месяца
        # сумма актов = сумме откруток
        orders_map = check_steps.create_acted_orders(
            {1: {'shipment_info': {'Bucks': 30},
                 'consume_qty': 50,
                 'product_id': Products.ticket,
                 'service_id': Services.ticket,
                 'paysys_id': 1003}},
            client_id, person_id, END_OF_MONTH, contract_id
        )

        # создаем перекрутку на заказе и после этого не актим
        # сумма актов < сумма откруток
        orders_map[1]['shipment_info']['Bucks'] += Decimal('100.1')
        check_steps.do_campaign(orders_map[1])

        # здесь проверяем тикет без авторазбора

    cmp_data = shared_steps.SharedBlocks.run_bua_partner(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (orders_map[1]['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]
    utils.run_auto_analyze('bua_partners', cmp_data[0]['cmp_id'])

    query = 'select issue_key from bua_cmp where id = {cmp_id}'.format(cmp_id=cmp_data[0]['cmp_id'])
    issue_key = api.test_balance().ExecuteSQL('cmp', query)[0][
        'issue_key']
    startrek = Startrek(**STARTREK_PARAMS)
    ticket = startrek.issues[issue_key]

    comments = list(ticket.comments.get_all())
    b_utils.check_that(comments[0].text, contains_string(u'Авторазбор не обнаружил подходящих расхождений'))
