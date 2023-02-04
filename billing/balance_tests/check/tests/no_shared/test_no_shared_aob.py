# coding=utf-8

import pytest
from hamcrest import equal_to, contains_string

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import utils as butils
from check import steps as check_steps
from check import utils
from check.defaults import Products, Services


# TODO: Сверки не существует
# def test_aob_ua_CHECK_2577_withount_diff():
#     check_name = 'aob_ua'
#     client_id = check_steps.create_client()
#     person_id = check_steps.create_person(client_id, person_category='ur')
#     steps.ExportSteps.export_oebs(client_id=client_id)
#     act_map = check_steps.create_act_map({1: {'paysys_id': 1003,
#                                               'service_id': Services.direct,
#                                               'product_id': Products.direct_pcs,
#                                               'shipment_info': {'Bucks': 30}}}, client_id, person_id)
#     invoice_id = act_map['invoice']['id']
#
#     steps.ExportSteps.export_oebs(invoice_id=invoice_id)
#     steps.ExportSteps.export_oebs(act_id=act_map['id'])
#     cmp_id = utils.run_check_new(check_name, str(act_map['eid']), {'silent': ''})
#     query = """
#         select runtype, diffcount
#         from cmp.{0}_cmp
#         where id = {1}
#             """.format(check_name, cmp_id)
#     res = api.test_balance().ExecuteSQL('cmp', query)[0]
#     runtype, diffcount = res['runtype'], res['diffcount']
#     butils.check_that(runtype, equal_to(3))
#     butils.check_that(diffcount, equal_to(0))


# TODO: Сейчас сверка не умеет возвращать ошибку
# def test_CHECK_2232_invalid_command_1():
#     # Run checks
#     with pytest.raises(Exception) as exceptions:
#         utils.run_check_new('aob', str('1'), {'notify-without-diffs': 'Y'})
#     butils.check_that(exceptions.value.faultString, contains_string(u'ArgumentsParsingError'),
#                       u'Проверяем, что запуск произошел с ошибкой')

# TODO: Сейчас сверка не умеет возвращать ошибку
# def test_CHECK_2232_invalid_command_2():
#     # Run checks
#     with pytest.raises(Exception) as exceptions:
#         utils.run_check_new('aob', str('1'), {'dt_from': '16112016'})
#     butils.check_that(exceptions.value.faultString, contains_string(u'ArgumentsParsingError'),
#                       u'Проверяем, что запуск произошел с ошибкой')
