# coding: utf-8
import datetime
# from btestlib import utils as utils
from balance import balance_steps as steps
from balance import balance_db as db
from balance import balance_api as api
import json
import xmlrpclib

#

# steps.ExportSteps.export_oebs(
#     contract_id= 14933024)
# steps.OverdraftSteps.reset_overdraft_invoices(92914305)


# steps.CashbackSteps.create(1351807489, 7, 'BYN', 1000000)
# steps.CommonSteps.get_last_notification(10, 1355105876)
# steps.common_steps.CommonSteps.export('OVERDRAFT', 'Client', 75849095, with_enqueue=True)
# steps.ClientSteps.create()
# # steps.CommonSteps.export('OEBS', 'CurrencyRate', 914796)
# client_id = steps.ClientSteps.create(params={'REGION_ID': 225})
# api.medium().GetRequestChoices({'OperatorUid': '675283008', 'RequestID': '3658867926', 'ShowDisabledPaysyses': False, 'PersonID': '14869098'},)
# import pickle
# from decimal import Decimal
# products = [
# steps.ActsSteps.hide(145303065)
# steps.ActsSteps.hide(145196008)
# 508605,
# 508612,
# 509747,
# 509781,
# 507912,
# 507953,
# 505151,
# 508613,
# 508639,
# 510057,
# 507211,
# 507835,
# # 509640,
# import xmlrpclib


# steps.CommonSteps.export("UA_TRANSFER", "Client", 90113895)
# steps.ExportSteps.export_oebs(act_id=143626327)
#
# print(xmlrpclib.dumps((16571028, [{'ClientID': 1337998889, 'GroupServiceOrderID': -1,
#                                    'GroupWithoutTransfer': 1, 'ProductID': 1475, 'ServiceID': 7,
#                                    'ServiceOrderID': 56645840, 'Text': 'Py_Test order 7-1475'}]),
#                       methodname='Balance.CreateOrUpdateOrdersBatch'))
# # filter = "t_product.engine_id = 102"
# # t_product = api.medium().QueryCatalog(['t_product'], "t_product.engine_id = 102")
# api.medium().GetRequestChoices({'OperatorUid': 887308675, 'RequestID': -1}, 30417100,
#                                            [{'Qty': 10, 'ServiceID': 11, 'ServiceOrderID': 1000751547,
#                                              'PersonID': 11945598,
#                                              'PaysysID': 11101001}], {'Overdraft': 1})
# a = api.medium().GetClientDiscountsAll({'ClientID':1339064393})
# pass
# 508656
# api.medium().UpdatePayment({'TrustPaymentID': '5a4c74f09e9cfab5f8e3c0f7c804e8ad'}, {'PayoutReady': '2017-10-24T13:53:25'})
# ]
# steps.InvoiceSteps.pay(129120883)
# for product_id in products:
#     steps.ExportSteps.export_oebs(product_id=product_id)
# print steps.ExportSteps.get_export_data(object_id=8776101, classname='Person', queue_type='OEBS_API')
# api.test_balance().GeneratePartnerAct(4117885, datetime.datetime(2020, 3, 1, 0, 0))
#

host =  steps.CommonSteps.get_host()
#
# api.test_balance().ExecuteSQL('balance', """update (
#     select *
#     from bo.T_PYCRON_SCHEDULE
#     where NAME like '%monthly-act_report%')
# set host = :host""", {'host': host})
# #
# client_id = 133325582
# db.balance().execute('''update t_export set input = Null where classname = :classname
#                                          and object_id = :object_id''',
#                      {'object_id': client_id, 'classname': 'Client'})
# try:
#     steps.ExportSteps.export_oebs(act_id=118811909)
# except Exception:
#     pass
# db.balance().execute('''update t_export set input = Null where classname = :classname
#                                          and object_id = :object_id''',
#                      {'object_id': client_id, 'classname': 'Client'})
# try:
#     # client_id = steps.ClientSteps.create({'CLIENT_ID': client_id})
#     steps.CommonSteps.export(queue_='OEBS_API', object_id=client_id, classname='Client')
# except Exception:
#     pass

# 118908250
# 118908253
#
# 113493964
#
# 11685765
# steps.ExportSteps.export_oebs(act_id=118908250)
# steps.ExportSteps.export_oebs(person_id=11685765)
# steps.ExportSteps.export_oebs(act_id=118908253)
# # steps.ExportSteps.export_oebs(invoice_id=113493964)
# client_id=1354813018, person_id=19029911,
# steps.ExportSteps.export_oebs(contract_id= 15325556)
# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur', {'inn': '7883306231',
#                                                        # 'fias-guid': '13a5bcdb-9187-4d0f-b027-45f8ec29591a',
#                                                        'postcode': '6666666',
#                                                        'postaddress': 'ewrwer'})

# steps.PersonSteps.create(client_id, 'ur', {'inn': '7883306231',
#                                            'postcode': 666666,
#                                            # 'is-postbox': '1',
#                                            'postsuffix': 'wef',
#                                            'fias-guid': '13a5bcdb-9187-4d0f-b027-45f8ec29591a'})


# steps.PersonSteps.create(client_id, 'ur', {'person_id': person_id,
#                                            'inn': '7883306231',
#                                            'postcode': 666666,
#                                            'is-partner': '1'
#                                            })
# api.medium().CreatePerson(16571028,
#                                           {'client_id': 5131,
#                                            # 'country_id': '225',
#                                            'email': 'testagpi2@yandex.ru',
#                                            'fname': u'Серафим',
#                                            # 'kpp': '123456789',
#                                            'lname': u'Овчинникова',
#                                            'mname': u'Егорович',
#                                            # 'person_id': 0,
#                                            'phone': '+7 905 1234567',
#                                            'type': 'ph'})

# api.test_balance().GeneratePartnerAct(2510445, datetime.datetime(2020, 12, 31, 0, 0))
# client_id = steps.ClientSteps.create()
# person = steps.PersonSteps.create(client_id, 'ur_autoru')
# steps.CommonSteps.export('EMAIL_DOCUMENT', 'Passport', 1120000000079605)
# steps.ClientSteps.link(785775, 'aikawa-test-10')
# steps.ExportSteps.export_oebs(act_id=153225888, person_id=14156280)
# print xmlrpclib.dumps((0, 0, []))
# json.dumps()

# api.medium().GetRequestChoices({'OperatorUid': 16571028, 'RequestID': 2442388568, 'ShowDisabledPaysyses': True})
# #
# def get_overdraft_object_id(firm_id, service_id, client_id):
#     return str(firm_id * 10 + service_id * 100000 + client_id * 1000000000)
#
#


# print api.medium().GetFirmCountryCurrency({'region_id': 126})

# api.medium().PayRequest(1015162790, {'RequestID': '2178970310', 'PaymentMethodID': 'trust_web_papge', 'Currency': 'RUB',
#                                      'PersonID': '83006872', 'ContractID': '17017278'})

# print steps.CommonSteps.get_last_notification(11, '42042534000700010')[-1]
#
# steps.InvoiceSteps.pay_with_charge_note({'InvoiceID': 101240181, 'PaymentSum': 320})
# steps.CommonSteps.export('EMAIL_MESSAGE', 'EmailMessage', 35847582)
# steps.CommonSteps.export('CASH_REGISTER', 'Payment', '5197587020')
# steps.ExportSteps.export_oebs(person_id=3503923)

# import xmlrpc
import xmlrpclib
from SimpleHTTPServer import SimpleHTTPRequestHandler

# #
# # s = xmlrpclib.ServerProxy('http://xmlrpc-eda-selfemployed.greed-branch.paysys.yandex.ru/xmlrpc')
# def find_client(params):
#     response_xml = (
#             "<methodResponse>\n%s</methodResponse>\n"
#             % xmlrpclib.dumps(
#         (0, 0, {}),
#         methodresponse=0,
#         allow_none=True,
#         encoding="utf-8",
#     )
#     )
#
#     # a = ((0, 0, []))
#     return response_xml
#
#
#
# server = Simp(("localhost", 8000))
# print "Listening on port 8000..."
# server.register_function(find_client, "find_client")
# server.serve_forever()
#
# # s = xmlrpclib.ServerProxy('http://xmlrpc-eda-selfemployed.greed-branch.paysys.yandex.ru/xmlrpc')
# # from xmlrpclib.server import SimpleXMLRPCServer
# with xmlrpclib.ServerProxy("http://localhost:8000/") as server:
#     @server.register_function(name='FindClient')
#     def find_client(params):
#         a = ((0, 0, []))
#         return (a)

# steps.CommonSteps.export('OEBS', 'Product', 511166)
# steps.ExportSteps.export_oebs(invoice_id=597614)
# steps.ExportSteps.export_oebs(invoice_id=108732943)
# steps.ExportSteps.export_oebs(
# client_id=132582815,
# act_id=114494805,
# person_id=32044194,
# invoice_id=108593879,
# contract_id=1500067
# )
# #
# rate_ids = [rate['id'] for rate in
#             db.balance().execute("select id from t_currency_rate_v2 where rate_dt = DATE '2020-01-31'")]
# for rate_id in rate_ids:
#     try:
#         steps.CommonSteps.export('OEBS', 'CurrencyRate', rate_id)
#     except Exception as exc:
#         pass
# steps.ExportSteps.export_oebs(client_id=1354650105, contract_id=15259760)
# #
# # #
# for person_id in (
#         32377001,
# ):
#     #     # #     #     # client_id = None
#     #     # #     #     # person_id = None
#     # client_id = db.get_contract_by_id(contract_id)[0]['client_id']
#     # person_id = db.get_contract_by_id(contract_id)[0]['person_id']
#     #     # #     #     # contract_       1XASid = db.get_invoice_by_id(invoice_id)[0]['contract_id']
#     #     # #     # invoice_id = db.get_act_by_id(act_id)[0]['invoice_id']
#     #     # #     #     #
#     steps.CommonSteps.export(classname='Person', queue_='OEBS_API', object_id=person_id)
#     steps.ExportSteps.export_oebs(
#         # person_id=person_id,
#         #         #
#         # client_id=client_id,
#         person_id=person_id,
#         # contract_id=contract_id,
#         #         # invoice_id=invoice_id,
#         #         #
#         # contract_id=act_id
#     )
#     #     # # 10023364
#     #     # print steps.CurrencySteps.get_currency_rate(dt=datetime.datetime.now(), currency='USD', base_cc='GHS', rate_src_id=1100)
#     #     # import math
#     #
#     #     # # #
#     #     # # list_ = []
#     #     # # for x in range(1, 40):
#     #     # #     list_.append('api-trust-testuser-{}'.format(x))
#     #     # #
#     #     # # print ', '.join(list_)
#     #     # #
#     #     #
#     #     # a = '''erfe
#     #     # refe
#     #     # erf'''
#     #     # json.dumps({'memo': a
#     #     #                     a})
#     #     # #
#     #     # # # # # # n = 11
#     #     # from balance import balance_api as api
#     #     #
#     #     # api.test_balance().CreatePerson(
#     #     #     16571028,
#     #     #     {
#     #     #         "delivery-type": "0",
#     #     #         "live-signature": "0",
#     #     #         "type": "yt",
#     #     #         "client_id": "109036687",
#     #     #         "invalid-bankprops": "0",
#     #     #         "delivery-city": "ODS",
#     #     #         "bank": "Bank Uw",
#     #     #         "early-docs": "1",
#     #     #         "authority-doc-details": "Md",
#     #         "fax": "+7 812 7054212",
#     #         "invalid-address": "0",
#     #         "vip": "1",
#     #         "representative": "foWw",
#     #         "email": "4key@JWca.oad",
#     #         "person_id": 0,
#     #         "longname": "000 ryqb",
#     #         "address": "Улица 3",
#     #         "legaladdress": "Avenue 3",
#     #         "authority-doc-type": "Распоряжение",
#     #         "phone": "+7 812 7748241",
#     #         "name": "НерезидентHwj",
#     #         "s_signer-position-name": "Управляющий",
#     #         "signer-person-gender": "X",
#     #         "region": "21199",
#     #         "account": "",
#     #         "signer-person-name": "Signer P"
#     #     }
#     # )
#     # api.test_balance().SyncRevPartnerServices()
#     # #
#     # # api.test_balance().ExecuteOEBS(10, """select 1 from dual""",)
#     # # result = [n, n - 1]
#     # # while result[-1] >= 1:
#     # #     result.append(int(math.sqrt(result[-2] ** 2 - result[-1] ** 2)))
#     # # print result
#     # # from balance import balance_db as db
#     # from balance import balance_api as api
#
#     # from btestlib.matchers import contains_dicts_with_entries
#     # from decimal import Decimal as D
#
#     # print steps.PaymentTermSteps.payment_term_with_holidays(payment_term=15, dt_from=datetime.datetime.now().replace(day=1))
#     # print steps.PassportSteps.get_passport_by_uid(561983498)
#     # steps.ClientSteps.fair_link()
#     # from balance import balance_api as api
#     # import balance.balance_api as api
#
#     # api.test_balance().ExportObject('NIRVANA_BLOCK', 'NirvanaBlock', 5997)
# api.medium().GetRequestPaymentMethods({"OperatorUid":"4061803418","RequestID":"3559944263","ContractID":"2569751"})
#
#     # #
#     # print api.medium().SignCollateral(0, 4828, {'signed_dt': '2019-12-10T00:00:00',
#     #                                             'faxed_dt': '2019-12-10T00:00:00'})
#
#     # print steps.CommonSteps.get_last_notification(1, 1465200603)
#     # agency_id = steps.ClientSteps.crecrate_agency()
#
#     # client_id = steps.ClientSteps.create({'REGION_ID': 171
#     #                                       })
# api.medium().CreateClient('296244631', {'SERVICE_ID': 23, 'IS_AGENCY': 0, 'NAME': 'gdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfsgdfgfs'})
# #     #
# api.medium().CreateOrUpdateOrdersBatch(16571028,
#                                              [{'AgencyID': None,
#                                                'ClientID': 187702112,
#                                                'GroupServiceOrderID': -1,
#                                                'GroupWithoutTransfer': 1,
#                                                'ProductID': 1475,
#                                                'ServiceID': 7,
#                                                'ServiceOrderID': 60705038,
#                                                'ManagerUID': 20851,
#                                                'Text': 'Py_Test order 7-1475'}])
# #     #     {
#     #         'OperatorUid': 16571028,
#     #         'RequestID': 1923133599,
#     #         # 'PersonID': person_id
#     #     }
#     # )
#     # for id in [81501917,
#     # 82771030,
#     # 83763296
#     # ]:
#     #     steps.InvoiceSteps.pay(id)
#     # steps.InvoiceSteps.turn_on(84643347)
#     # # g
#
#     # client_id = steps.ClientSteps.create()
#     #
#     # client_id = steps.ClientSteps.create()
#     #
#     # person_id = steps.PersonSteps.create(client_id, 'ur',
#     #                                      params={'inn': '7839343259',
#     #                                              'fname': 'ert',
#     #                                              'lname': 'we',
#     #                                              'ownership_type': 'SELFEMPLOYED'})
#     # person_id2 = steps.PersonSteps.create(client_id, 'ur',
#     #                                      params={'inn': '7839343259',
#     #                                              'fname': 'ert',
#     #                                              'lname': 'we',
#     #                                              'ownership_type': 'SELFEMPLOYED'})
#     #
# api.medium().CreatePersonValidate('793360492', {'account': '40817810138051929363', 'bik': '044525225',
#                                                 'client_id':1223, 'type': 'ph', 'delivery_type': '4'})
# #     #
#     #
#     #
# client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
# steps.PersonSteps.create(client_id, 'ph', {"is-partner": 1})
# s
#     # # print steps.CommonSteps.build_notification(10, object_id='6768467')
#     # steps.OverdraftSteps.run_overdraft_ban()
#     # # Response: (502, u'<html>\r\n<head><title>502 Bad Gateway</title></head>\r\n<body bgcolor="white">\r\n<center><h1>502 Bad Gateway</h1></center>\r\n<hr><center>nginx</center>\r\n</body>\r\n</html>\r\n')
#     # # steps.InvoiceSteps.turn_on(83865655)
#     # api.medium().GetCardBindingURL(846391823,
#     #                                {'Currency': 'RUB', 'NotificationURL': 'https://yandex.ru', 'Payload': 'payload_value',
#     #                                 'ReturnPath': 'rfef', 'ServiceID': 143})
#     # # # client_id = 104078875
#     # dt = datetime.datetime.now()
#     # steps.OrderSteps.ua_enqueue([104081385], dt)
#     # query = "select input from t_export where type = 'MIGRATE_TO_CURRENCY' and classname = 'Client' and object_id =  107181943"
#     # input = steps.ExportSteps.get_export_data(query, 'input')
#     # print input
#
#     # print  steps.ExportSteps.get_export_data(classname='Client', object_id=107181943,
#     #                                                                    queue_type='MIGRATE_TO_CURRENCY')
#     # steps.CommonSteps.get_pickled_value()
#     # steps.CommonSteps.export('PROCESS_COMPLETION', 'Order', 484903949)
#     import pytest
#     from btestlib import utils
#     from balance import balance_db as db
#     from hamcrest import not_, equal_to
#
#     # client_id = steps.ClientSteps.create()
#
#     # db.balance().execute("""insert into t_person_firm (person_id, firm_id) values (:person_id, :firm_id)""",
#     #                      {'person_id': 82688907, 'firm_id': 1})
#     # steps.ExportSteps.export_oebs(person_id=person_id)
#     # db.balance().execute("""insert into t_person_firm (person_id, firm_id) values (:person_id, :firm_id)""",
#     #                      {'person_id': person_id, 'firm_id': 2})
#     # steps.ExportSteps.export_oebs(person_id=person_id)
#     # #
#     #
#     # def export(person_id, W_API):
#     #     # try:
#     #     #     db.balance().execute("""insert into t_person_firm (person_id, firm_id) values (:person_id, :firm_id)""",
#     #     #                          {'person_id': person_id, 'firm_id': firm_id})
#     #     # except Exception:
#     #     #     pass
#     #
#     #     if not W_API:
#     #         steps.CommonSteps.export(queue_='OEBS', classname='Person', object_id=person_id)
#     #     else:
#     #         db.balance().execute('''update t_export set input = Null where classname = 'Person'
#     #                                  and object_id = :person_id''',
#     #                              {'person_id': person_id})
#     #         with pytest.raises(utils.XmlRpc.XmlRpcError) as error:
#     #             steps.CommonSteps.export(queue_='OEBS_API', classname='Person', object_id=person_id)
#     #         assert error.value.response == 'Retrying OEBS_API processing'
#     #
#     #         def get_error():
#     #             try:
#     #                 steps.CommonSteps.export(queue_='OEBS_API', classname='Person', object_id=person_id)
#     #             except Exception:
#     #                 return error.value.response
#     #
#     #         utils.wait_until(lambda: get_error(), not_(equal_to('Retrying OEBS_API processing')), timeout=300)
#
#     #
#     # for person_id in [9919366, 9919364, 9919361, 9919359, 9919356, 9917537, 9914619, 9914608, 9912232, 9912118, 9920262,
#     #                   9919446, 9919445, 9919443, 9919442, 9920041, 9919871,
#     #                   9919670, 9919663, 9919626, 9920230, 9920179, 9920173, 9920140, 9920135, 9919895, 9918478, 9918322,
#     #                   9917870, 9917390, 9916818, 9912127, 9911737, 9911537,
#     #                   9911331, 9916764, 9912173, 9911718, 9911365, 9910980, 9916494, 9914614, 9912082, 9911810, 9911474,
#     #                   9920368, 9920364, 9920360, 9920355, 9920352, 7033121,
#     #                   7030719, 7030007, 7029680, 7029584, 9919423, 9919418, 9919417, 9919410, 9919409, 7002962, 7002948,
#     #                   7002946, 7002938, 7002936, 9920291, 9920272, 9918150,
#     #                   9918149, 9918102, 7033335, 7027864, 7024181, 7022821, 7016257, 9862675, 9862668, 9920282, 9920075,
#     #                   9919956, 9919416, 9919395, 9919927, 9919643, 9919636,
#     #                   9919623, 9919584, 9920395, 9920393, 9920385, 9920374, 9920369, 6987875, 6945200, 6941357, 6619954,
#     #                   6073157, 9920270, 9920265, 9920261, 9920259, 9920254,
#     #                   7033719, 7033672, 7033323, 7033283, 7033074, 4193408, 3720042, 3641928, 3609223, 3192139, 9920046,
#     #                   9919939, 9919938, 9919903, 9919893, 9920052, 9920042,
#     #                   9919926, 9919918, 9919885, 9920019, 9919991, 9919944, 9919933, 9919901, 7030824, 7029373, 7026765,
#     #                   7023495, 7022852, 9920050, 9920023, 9920017, 9919987,
#     #                   9919948, 7033636, 7033372, 7033018, 7032728, 7032700, 9920073, 9920072, 9920069, 9920068, 9920065,
#     #                   9920074, 9920071, 9920070, 9920067, 9920066, 9918158,
#     #                   9916970, 9906743, 9905851, 9901232, 9920399, 9920398, 9920397, 9920396, 9920394, 7034167, 7034142,
#     #                   7034070, 7034058, 7034032, 9920037, 9919937, 9919898,
#     #                   9919675, 9919673, 9920036, 9919950, 9919946, 9919922, 9919894, 9919942, 9919936, 9919932, 9919930,
#     #                   9919923, 6818479, 9919644, 9918484, 9918462, 9918436,
#     #                   9918157, 9920053, 9920045, 9920043, 9920031, 9920029]:
#     #     export(9916764, 1)
#
#     # 617960
#     # 629084
#     # 786912
#
#     # # export617960
#     # steps.CommonSteps.export('OEBS', 'Contract', 16883757)
#     # db.balance().execute("""insert into t_person_firm (person_id, firm_id) values (:person_id, :firm_id)""",
#     #                              {'person_id': 10107341, 'firm_id': 2})
# steps.ExportSteps.export_oebs(client_id=1354332113, person_id=17710228, contract_id=15077163)
#     # steps.CommonSteps.export('OEBS_API', 'Person', 617960)
#     # steps.CommonSteps.export('OEBS', 'Invoice', 104697041)
#     # # steps.CommonSteps.export('OEBS', 'CardRegister', 1396948)
#     # # steps.CommonSteps.export('OEBS', 'Contract', 1293876)
#     # steps.CommonSteps.export('OEBS', 'Act', 109613302)
#     # steps.CommonSteps.export('OEBS', 'ContractCollateral', 1232591)
#     # steps.CommonSteps.export('OEBS', 'ContractCollateral', 1232619)
#     # steps.CommonSteps.export('OEBS', 'ContractCollateral', 1232624)
#     # 10082058
#     # 87622740, 87622741
#     # dt = datetime.datetime.now()
#     # last_day_of_month = utils.Date.last_day_of_month(dt)
#     # api.test_balance().AutoOverdraftEnqueue(7, 8416291, last_day_of_month)
#     # id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
#     #                           {'item': client_id})[0]['id']
#
#     # product_list = [509059, 509060, 508436]
#     # for product in product_list:
#     #     steps.CommonSteps.export('OEBS', 'Product', product)
#
#     # steps.InvoiceSteps.pay(101209374)
#
#     # steps.OverdraftSteps.run_overdraft_ban(59774)
#     # api.
#     # print steps.CommonSteps.get_last_notification(1, 349351519)
#     # def get_overdraft_object_id(firm_id, service_id, client_id):
#     #     return str(firm_id * 10 + service_id * 100000 + client_id * 1000000000)
#     # #
#     # #
#     # object_id = get_overdraft_object_id(firm_id=12, service_id=99,
#     #                                     client_id=6768467)
#     # print steps.CommonSteps.build_notification(11, object_id)
#
#     # steps.OverdraftSteps.export_client(61183, with_enqueue=True)
#     # steps.
#     # NOW = datetime.datetime.today()
#     # steps.CommonSteps.export('UA_TRANSFER', object_id=60661757, classname='Client', input_={'for_dt': NOW})
#     # steps.InvoiceSteps.turn_on(84026010)
#     # steps.OverdraftSteps.export_client(6768467, with_enqueue=False)
#     # steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id=83701314, bonus=50, is_with_discount=True,
#     #                                                     qty=D('249'), nds=D('1.18'))
#     # client_id = steps.ClientSteps.create()
#     # steps.PersonSteps.create(client_id=client_id, type_='yt')
#     # steps.PersonSteps.create(client_id=client_id, type_='ph')
#     # import datetime
#     # steps.ActsSteps.generate(client_id=60661757, date=datetime.datetime.now(), force=1)
#     # acts = [96364224
#     #         ]
#     # for act_id in acts:
#     #     steps.ActsSteps.hide(act_id=act_id)
#     # print ''.join(set('Dermatoglyphics'.lower()))
#     # steps.CommonSteps.export(queue_='PROCESS_COMPLETION', classname='Order', object_id=739526912)
#     # steps.ExportSteps.get_export_data(object_id=38804541, classname='Client', queue_type='UA_TRANSFER')
#     # steps.CommonSteps.export(queue_='OVERDRAFT', classname='Client', object_id=67056443)
#
#     # print utils.InnGenerator.generate_inn_rus_ph_12()
#     #
#     #
#     #
#     # client_id = steps.ClientSteps.create()
#     # api.test_balance().CreatePerson('1120000000080718', {u'signer-person-gender': u'M', u'ogrn': u'316723200124232',
#     #                                                       u'inn': u'720401505794',
#     #                                                       u'street': u'улица отдельно',
#     #                                                       u'postcode': u'625000',
#     #                                                       u'city': u'\\u0422\\u044e\\u043c\\u0435\\u043d\\u044c',
#     #                                                       u'type': u'ur', u'email': u'rus-tumen495645@yandex.ru',
#     #                                                       u'bik': u'047102651',
#     #                                                      u'phone': u'+79199243164',
#     #                                                       u'client_id': client_id,
#     #                                                       u'account': u'40802810867100004908',
#     #                                                       u'name': 'name',
#     #                                                       u'longname': 'longname',
#     #                                                       u'legaladdress': u'625000,юридическая улица 4',
#     #                                                       u'postaddress': u'625000,автодорога Екатеринбург-Тюмень, почтовая Улица,80 кв 4',
#     #                                                       u'postsuffix': u'80 кв 4'})
#
#     # print api.test_balance().ExecuteOEBS(1,'SELECT * FROM apps.XXAR_CUSTOMER_ATTRIBUTES WHERE CUSTOMER_ID =(SELECT ac.CUST_ACCOUNT_ID FROM apps.HZ_CUST_ACCOUNTS ac WHERE ac.ACCOUNT_NUMBER = :object_id)',
#     # #                                              {'object_id': 'P7145809'})
#     # #
#     # dt = datetime.datetime.now()
#     # PROMOCODE_BONUS = 100
#     # firm_id = 1
#     # is_global_unique = True
#     # pr = []
#     # for x in range(2):
#     #     promocode_id = steps.PromocodeSteps.create(start_dt=dt, end_dt=None, bonus1=PROMOCODE_BONUS, bonus2=PROMOCODE_BONUS,
#     #                                            minimal_qty=1, reservation_days=None,
#     #                                            firm_id=firm_id, is_global_unique=is_global_unique)
#     # print promocode_id
#     #     pr.append(promocode_id)
#     # print pr
#
#     # # # print 'rttrrt', NOW
#     # # # dt = datetime.datetime.now()
#     # # # client_id = steps.ClientSteps.create()
#     # steps.CommonSteps.export('OEBS', 'Product', 510041)
#     # #
#     # steps.ExportSteps.get_export_data(322057, 'Client', 'UA_TRANSFER')
#
#     # FFOFH1ZTTKUX3YJH
#     # GHBPU6GYLYBOBDCM
#     # steps.E
#
#     # from btestlib.utils import Date
#
#     # db.get_person_by_id()
#     # # qty = 1
# steps.ClientSteps.link(134323917, 'aikawa-test-10')
#
#     #
#     # api.test_balance().GetNotificationInfo(1, 606775264)
#     # contract_params = {
#     #     'CLIENT_ID': client_id,
#     #     'PERSON_ID': person_id,
#     #     'PAYMENT_TYPE': ContractPaymentType.PREPAY,
#     #     'SERVICES': [Services.APIKEYS.id],
#     #     'DT': Date.TODAY_ISO,
#     #     'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
#     #     'IS_SIGNED': Date.TODAY_ISO,
#     #     'CURRENCY': Currencies.RUB.num_code
#     # }
#     #
#     # # # contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY, contract_params)
#     # client_id = steps.ClientSteps.create()
#     # another_client = steps.ClientSteps.create()
#     # agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
#     # steps.ClientSteps.link(322057, 'aikawa-test-10')
#     # person_id = steps.PersonSteps.create(agency_id, 'sw_ur')
#     # service_order_id = steps.OrderSteps.next_id(service_id=7)
#     # child_order = steps.OrderSteps.create(client_id, service_order_id, service_id=7,
#     #                         product_id=1475)
#     #
#     # service_order_id = steps.OrderSteps.next_id(service_id=7)
#     # parent_order = steps.OrderSteps.create(client_id, service_order_id, service_id=7,
#     #                         product_id=1475)
#     #
#     # steps.OrderSteps.merge(parent_order, [child_order])
#     # print steps.CommonSteps.build_notification(1, object_id=parent_order)
#     # service_order_id = steps.OrderSteps.next_id(service_id=7)
#     # steps.OrderSteps.create(client_id, service_order_id, service_id=7,
#     #                         product_id=1475, params={'AgencyID': agency_id})
#     #
#     # service_order_id = steps.OrderSteps.next_id(service_id=7)
#     # steps.OrderSteps.create(another_client, service_order_id, service_id=7,
#     #                         product_id=1475)
#     # orders_list = [{'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 1,
#     #                 'BeginDT': NOW}]
#     # request_id = steps.RequestSteps.create(client_id, orders_list,
#     #                                        additional_params=dict(InvoiceDesireDT=NOW))
#     # # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1003,
#     #                                              credit=0, contract_id=None, overdraft=0)
#     #
#     # steps.InvoiceSteps.turn_on(99699600)
#     # steps.CampaignsSteps.do_campaigns(7, 33312399, {'Money': 1}, 0, datetime.datetime.now())
#     # 33312399
#     # act_id = steps.ActsSteps.generate(109180988, force=1, date=datetime.datetime.now())[0]
#
#     # Так как работаеем с ApiKeys, необходимо вручную указать FIRM_ID в таблице T_EXTPROPS
#     # query = "INSERT INTO T_EXTPROPS (ID, OBJECT_ID, CLASSNAME, ATTRNAME, VALUE_NUM) " \
#     #         "VALUES (S_EXTPROPS.nextval, :request_id, 'Request', 'firm_id', 1)"
#     # db.balance().execute(query, dict(request_id=request_id))
#
#     # query = "UPDATE T_REQUEST SET FIRM_ID = 1 WHERE ID = :request_id"
#     # db.balance().execute(query, dict(request_id=request_id))
#     # steps.ContractSteps.get_contract_credits_detailed(643026)
#     # steps.CommonSteps.export('PARTNER_ACTS', 'Contract', 285816)
#     #
#     # # api.test_balance().GetNotificationParams(201)
#     # # print dt.strftime('%Y%m%d%H%M%S')+str(dt.time().microsecond)[0:3]
#     # steps.InvoiceSteps.pay(75941639)
#     # #
#     # client_id = steps.ClientSteps.create()
#     # steps.PersonSteps.create(client_id, 'ur', {'postaddress': u'Москва, ул. Новая, д. 75'})
#
#     # NOW = datetime.datetime.now()
#     # steps.ClientSteps.link(61178755, 'testclientuid1')
#     # keys_a = set(dict_a.keys())
#     # keys_b = set(dict_b.keys())
#     # intersection = keys_a & keys_b
#     # list_of_dict = [{},
#     #                 {'a': 2},
#     #                 {'a': 3},
#     #                 {'b': 2, 'c': 4}]
#     #
#     # # import copy
#     # print api.test_balance().GetCurrencyRate('USD', NOW)
#     # api.test_balance().GetTestSequenceNameForService(7)
#
#     #
#     # print steps.CommonSteps.build_notification(1, object_id=3365506)
#     # print steps.CommonSteps.build_notification(1, object_id=13317245)
#     # steps.ClientSteps.create({
#     #     'CLIENT_ID': 1756259,
#     #     'REGION_ID': 225,
#     #     'CURRENCY': 'RUB',
#     #     'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(hours=5),
#     #     'SERVICE_ID': 7,
#     #     'CURRENCY_CONVERT_TYPE': 'MODIFY'
#     # })
#     # print steps.CommonSteps.build_notification(1, object_id=3365506)
#     # print steps.CommonSteps.build_notification(1, object_id=13317245)
#     # steps.CommonSteps.build_notification(1, object_id=2750572)
#     # steps.ClientSteps.migrate_to_currency(client_id=1277399, currency_convert_type='MODIFY')
#     # steps.CommonSteps.build_notification(1, object_id=2750572)
#     # {'args': [{'CompletionFixedMoneyQty': '194322.1710',
#     #                        'CompletionFixedQty': '7117.466993',
#     #                        'CompletionQty': '7117.466993',
#     #                        'ConsumeAmount': '194315.27',
#     #                        'ConsumeCurrency': 'RUB',
#     #                        'ConsumeMoneyQty': '194345.6070',
#     #                        'ConsumeQty': '7118.306993',
#     #                        'ConsumeSum': '169194.02',
#     #                        'ProductCurrency': '',
#     #                        'ServiceID': 7,
#     #                        'ServiceOrderID': 466505,
#     #                        'Signal': 1,
#     #                        'SignalDescription': 'Order balance have been changed',
#     #                        'Tid': '9010656980338'}],
#     #
#     # dcts = []
#     # buffer = {}
#     # for i in list_of_dict:
#     #     if not i or any(j in buffer for j in i.keys()):
#     #         buffer = i
#     #     else:
#     #         new_ = copy.deepcopy(i)
#     #         new_.update(buffer)
#     #         buffer = copy.deepcopy(new_)
#     #         dcts.append(buffer)
#     # print dcts
#
#     # dcts.append(buffer)
#     #         buffer = i
#     #     else:
#     #         new_ = copy.deepcopy(i)
#     #         buffer = new_.update(buffer)
#     #
#     # dcts.append(buffer)
#     # print(dcts)
#     # steps.ClientSteps.link(5131, 'yb-atst-user-17')
#     # steps.ClientSteps.fair_unlink_from_login(81168714, 436363467)
#     # steps.ClientSteps.fair_link(5131, 436363467)
#     # steps.InvoiceSteps.pay(75831459)
#     # order = 366192642
#     # steps.Co
#     # print steps.ActsSteps.hide(97765996)
#     # print steps.ActsSteps.unhide(78547797)
#     # print steps.ActsSteps.enqueue([1090538], force=1, date=NOW)
#     # steps.ExportSteps.get_export_data(7933632, 'Client', 'MONTH_PROC')
#     # steps.CommonSteps.export('MONTH_PROC', 'Client', 1092385)
#
#     # print steps.get_order_notifications(order)
#     # print api.test_balance().GetNotification(1, 366192642)
#     # api.medium().GetRequestChoices({'OperatorUid': 16571028, 'RequestID': 695084324})
#
#     # api.test_balance().GetTestSequenceNameForService(7)
#
#     # from faker import Faker
#     # fake = Faker('ru_RU')
#     # for _ in range(10):
#     #     print(fake.name())
#
print api.test_balance().GetHost()
#     # print u'\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432 \u0434\u043b\u044f \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0445 \u0441\u0435\u0440\u0432\u0438\u0441\u043e\u0432 \u0441 \u0444\u0438\u0440\u043c\u043e\u0439 \u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441 \u043d\u0430 \u0434\u0430\u0442\u0443 \u043f\u043e\u0441\u043b\u0435 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u044f \u0411\u0438\u0437\u043d\u0435\u0441-\u042e\u043d\u0438\u0442\u0430 \u0437\u0430\u043f\u0440\u0435\u0449\u0435\u043d\u043e'

#     # PROMOCODE_CURRENCY_BONUS = 100
#     # PROMOCODE_BONUS = 100
#     # list_of_promo = list()
#     # for firm_id in [1, 25, 27, 7, 8, 4]:
#     #     for x in range(20):
#     #         TODAY = datetime.datetime.now()
#     #         YEAR_AFTER = TODAY + datetime.timedelta(days=365)
#     #
#     #         promocode_id = steps.PromocodeSteps.create(start_dt=TODAY, end_dt=YEAR_AFTER, bonus1=PROMOCODE_BONUS,
#     #                                                    bonus2=PROMOCODE_BONUS, minimal_qty=1,
#     #                                                    reservation_days=None,
#     #                                                    firm_id=firm_id, is_global_unique=1,
#     #                                                    new_clients_only=0, valid_until_paid=1,
#     #                                                    need_unique_urls=0)
#     #         if firm_id == 25:
#     #             currency = 'KZT'
#     #         elif firm_id == 27:
#     #             currency = 'BYN'
#     #         elif firm_id == 8:
#     #             currency = 'TRY'
#     #         elif firm_id == 7:
#     #             currency = 'EUR'
#     #         elif firm_id == 4:
#     #             currency = 'USD'
#     #         elif firm_id == 1:
#     #             currency = 'RUB'
#     #         steps.PromocodeSteps.set_multicurrency_bonuses(promocode_id, [
#     #             {'currency': currency, 'bonus1': PROMOCODE_CURRENCY_BONUS, 'bonus2': PROMOCODE_CURRENCY_BONUS,
#     #              'minimal_qty': 1}])
#     #         promocode_code = db.get_promocode_by_id(promocode_id)[0]['code']
#     #
#     #         list_of_promo.append([promocode_code, firm_id, currency])
#     # import pprint
#     # pprint.pprint(list_of_promo)
#
#     # steps.ClientSteps.link(105180394, 'aikawa-test-10')
#     #
#
# rates = [3656836,3656837,3656838,3656839,3656840,3656841,3656842,3656843,3656844,3656845,3656846,3656848,3656849,3656850,3656851,3656852,3656853,3656854,3656855,3656856,3656857,3656858,3656859,3656860,3656861,3656862,3656863,3656864,3656866,3656867,3656868]
#
# for rate_id in rates:
#     steps.CommonSteps.export('OEBS', 'CurrencyRate', rate_id)
#     # 509347
#     # 509350
#     # 509351
#
#     # steps.CommonSteps.export('OEBS', 'Invoice', 86005027)
#     # steps.ExportSteps.get_export_data(6900414, 'Person', 'OEBS')
#     # steps.CommonSteps.export('OEBS', 'Client', 111712474)
#     # steps.CommonSteps.export('OEBS', 'Manager', 37724)
#     # steps.CommonSteps.export('OEBS', 'ProductGroup', 509949)
#     # steps.CommonSteps.export('OEBS', 'Product', 510041)
#     # steps.CommonSteps.export('OEBS', 'Contract', 381708)
#     # steps.CommonSteps.export('OEBS', 'Contract', 633390)
#     # steps.CommonSteps.export('OEBS', 'Contract', 17078497)
#     # # # steps.CommonSteps.export('OEBS', 'Product', 507862)
#     # steps.CommonSteps.export('OEBS', 'ContractCollateral', '10002739749')
#     # steps.CommonSteps.export('OEBS', 'ContractCollateral', 801370)
#     # steps.CommonSteps.export('OEBS', 'ContractCollateral', 801387)
#     # steps.CommonSteps.export('OEBS', 'ContractCollateral', 607114)
#     from balance import balance_api as api
#
#     #
#     #
#     # # print [] in [1, 3]
#     # # steps.CommonSteps.export('BYR', 'Act', 71382164)
#     # # steps.CommonSteps.export('BYR', 'Client', 56check947186)
#     # # steps.CommonSteps.export('BYR', 'Person', 6114956)
#     #
#     # # print steps.CommonSteps.get_last_notification(1, 363968159)
#     # # steps.CloseMonth.resolve_mnclose_status('monthly_limits')
#
#     # from balance import balance_api as api
#
#     # from temp.igogor.balance_objects import Contexts
#     # context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.AUTORU, product=Products.AUTORU)
#     # # # client_id = 81127952
#     # client_id = 82559351
#     # # # # # # steps.ClientSteps.link(client_id,
#     # # # # # # 'aikawa-test-10')
#     # # person_id = steps.PersonSteps.create(client_id, 'ur', {'fias_guid': '14bd4cf7-30f0-4fd0-ac37-93001892b580',
#     # #                                                        'street':'массив Никулинка тер'})
#     # person_id = 7060935
#     # # 'legaladdress': 'test_address'
#     # # })
#     # # # # NOW = datetime.datetime.now()
#     # # # #
#     # # # # SERVICE_ID = 7
#     # # # PRODUCT_ID = 507211
#     # # # #
#     # # NOW = datetime.datetime.now()
#     # client_id = steps.ClientSteps.create()
#     # service_order_id = steps.OrderSteps.next_id(7)
#     # order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=7,
#     #                                    product_id=1475, params={'AgencyID': None,
#     #                                                             'IsUAOptimize': 1})
#     # orders_list = [{'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
#     # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
#     # # print steps.RequestSteps.get_request_choices(request_id=request_id)
# invoice_id, _, _ = steps.InvoiceSteps.create(3824654581, 9661707, 2701102, credit=0,
#                                                  contract_id=None, overdraft=1, endbuyer_id=None)
#     steps.InvoiceSteps.pay(invoice_id)
#
#     # l
#
#     # steps.ExportSteps.export_oebs(client_id=client_id, invoice_id=invoice_id, act_id=act_id)
#
#     # steps.ExportSteps.export_oebs(client_id=57611285, contract_id=927025, invoice_id=70101315, act_id=71520792)
#     # agency_id = 32694148
#     # db.balance().execute('''update t_person set CLIENT_ID = :agency_id where id =:person_id''', {'agency_id': agency_id, 'person_id': person_id})
#     # steps.ClientSteps.link(client_id, 'yndx-tst-role-3')
#     # # steps.ClientSteps.link(56006822, 'supportuid1')
#     # #
#     #
#     # # act_id = [
#     # #     # 71383928,
#     # # # 71383973,
#     # # # 71384366,
#     # # # 71384840,
#     # # # 71386311,
#     # # # 71386366,
#     # # # 71387438,
#     # # # 71387719,
#     # # # 71391117,
#     # # 71388477,
#     # # 71388742
#     # #
#     # # ]
#     # # for act in act_id:
#     # print steps.CommonSteps.export('CASH_REGISTER', 'Payment',  603333578)
#     # # print steps.ExportSteps.get_export_data(queue_type='CASH_REGISTER', classname='Payment', object_id=334404752)
#     #
#     #
#     # # print steps.CommonSteps.export('BY_FILE', 'Contract', 694825)
#     #
#     # print steps.CommonSteps.export('BY_FILE', classname='Person', object_id=person_id)
#
#     # print steps.RequestSteps.get_request_choices(427113279)
#     # print steps.RequestSteps.get_request_choices(389919068)
#     #
#     # steps.PromocodeSteps.delete_reservation_for_client(7879278)
#     #
#     # print steps.RequestSteps.get_request_choices(request_id=315659189)
#     #
#     #
#     # if not False:
#     #     print 'fbb'
#     # # request_choices = steps.RequestSteps.get_request_choices(389856622)
#     # # print steps.RequestSteps.format_request_choices(request_choices)
#
#     # steps.CommonSteps.export('OEBS', 'Contract', 457794)
#     # steps.CommonSteps.export('OEBS', 'CurrencyRate', 517621)
#     # steps.CommonSteps.export('OEBS', 'ContractCollateral', 606789)
#     # invoice_list = [
#     #     87641979,
#     #     87641982,
#     #     87641986,
#     #     87641988,
#     #     87641991,
#     #     87641992,
#     #     87642010,
#     #     87642035,
#     #     87642041
#     #
#     # ]
#     # from balance import balance_api as api
#     #
#     # client_id = steps.ClientSteps.create()
#     # api.test_balance().MigrateClientToEls({'ClientID': 108695189})
#     # #
#     # # steps.CommonSteps.export('OEBS', 'Contract', 1043700
#     # #                          )
#     # for i in invoice_list:
#     #     steps.CommonSteps.export('OEBS', 'Act', i)
#     # steps.CommonSteps.export('OEBS', 'Invoice', 81148557)
#     # steps.CommonSteps.export('OEBS', 'Person', 6318558)
#     # steps.CommonSteps.export('OEBS', 'Act', 87641979)
#     # TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now() - datetime.timedelta(days=90))
#     # TODAY_ISO = utils.Date.date_to_iso_format(TODAY)
#     # WEEK_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=7))
#     # #
#     # PERSON_TYPE = 'ur'
#     #
#     # # SERVICE_ID = 82
#     # # PRODUCT_ID = 507211
#     # # FIRM_ID = Firms.VERTICAL_12.id
#     # # PAYSYS_ID = 1201003
#     #
#     # SERVICE_ID = 7
#     # PRODUCT_ID = 1475
#     # FIRM_ID = Firms.YANDEX_1.id
#     # PAYSYS_ID = 1003
#     #
#     # # SERVICE_ID = 42
#     # # PRODUCT_ID = 1475
#     # # FIRM_ID = Firms.YANDEX_1.id
#     # # PAYSYS_ID = 1003
#     #
#     from btestlib import constants
#
#     # SERVICE_ID = 11
#     # PRODUCT_ID = 2136
#     # # # FIRM_ID = Firms.MARKET_111.id
#     # # # PAYSYS_ID = 11101003
#     # # #
#     # agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
#     # client_id = steps.ClientSteps.create()
#     # # # steps.ClientSteps.link(client_id, 'aikawa-test-10')
#     # # person_id = steps.PersonSteps.create(client_id, 'ur')
#     # # client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag(person_type='ur')
#     # # # person_id = steps.PersonSteps.create_partner(client_id, PersonTypes.EU_YT.code)
#     # #
#     # # dt = datetime.datetime.now()
#     # # #
#     # #
#     # service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     # order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#     #                                    service_order_id=service_order_id,
#     #                                    params={'AgencyID': agency_id, 'ActText': 'ActText'})
#     # service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     # order_id2 = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#     #                                     service_order_id=service_order_id2,
#     #                                     params={'AgencyID': agency_id, 'ActText': 'ActText'})
#     # api.test_balance().CreateOrUpdateOrdersBatch(16571028, [{
#     #     'AgencyID': agency_id,
#     #     'ClientID': client_id,
#     #     'ProductID': PRODUCT_ID,
#     #     'ServiceID': SERVICE_ID,
#     #     'ServiceOrderID': service_order_id,
#     #     'Text': 'dfd',
#     #     'ManagerUID': constants.Managers.PERANIDZE.uid}])
#     # api.CreateOrUpdateOrdersBatch(16571028, [{'AgencyID': None,
#     #                                           'ClientID': 104028662,
#     #                                           'GroupServiceOrderID': -1,
#     #                                           'GroupWithoutTransfer': 1,
#     #                                           'ProductID': 2136,
#     #                                           'ServiceID': 11,
#     #                                           'ServiceOrderID': '10589571333',
#     #                                           'Text': 'Py_Test order 11-2136'}])
#     # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#     #                                        additional_params={'InvoiceDesireDT': TODAY})
#     # print steps.InvoiceSteps.pay_with_certificate_or_compensation(order_id, 100)
#     # # contract_type = ContractCommissionType.NO_AGENCY
#     # # contract_params_default = {
#     # #     'CLIENT_ID': client_id,
#     # #     'PERSON_ID': person_id,
#     # #     'SERVICES': [SERVICE_ID],
#     # #     'DT': TODAY_ISO,
#     # #     'FINISH_DT': WEEK_AFTER_ISO,
#     # #     'IS_SIGNED': TODAY_ISO,
#     # # }
#     # # contract_params_default.update({
#     # #     'FIRM': FIRM_ID,
#     # #     'CURRENCY': Currencies.RUB.num_code,
#     # #     'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
#     # #     'PERSONAL_ACCOUNT': 1,
#     # #     'PERSONAL_ACCOUNT_FICTIVE': 0,
#     # #     'DEAL_PASSPORT': TODAY_ISO,
#     # #     'PAYMENT_TERM': 34
#     # # })
#     # # contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params_default,
#     # #                                                          prevent_oebs_export=True)
#     # # # SERVICE_ID = 7
#     # # # PRODUCT_ID = 1475
#     # # # # agency_id = 7325359
#     # # # client_id = steps.ClientSteps.create()
#     # # # # contract_id = 574496
#     # # # person_id = steps.PersonSteps.create(client_id, 'ph')
#     # # # service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     # # # order_id = steps.OrderSteps.create4client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#     # # #                                    service_order_id=service_order_id, params={'AgencyID': None})
#     # # # orders_list = [
#     # # #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
#     # # # ]
#     # # # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params={'InvoiceDesireDT': dt})
#     # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=755042232, person_id=6504613, paysys_id=1301003,
#     #                                              credit=0, contract_id=533675, overdraft=0, passport_uid=389886597)
#
#     # 389886597, {'PersonID': 6504613, 'PaysysID': 1301003, 'RequestID': 755042232, 'ContractID': 533675})
#     # steps.InvoiceSteps.pay(84683626)
#
#     # steps.CampaignsSteps.do_campaigns(7, 115186601, {'Money': 100}, 0, dt-datetime.timedelta(days=1))
#     # steps.OrderSteps.ua_enqueue([766975])
#     # steps.CommonSteps.export('OEBS', 'Product', 510041)
#     # NOW = datetime.datetime.now()
#     # steps.ActsSteps.enqueue([1929840], 1, TODAY)
#     # steps.ExportSteps.get_export_data(1929840, classname='Client', queue_type='MONTH_PROC')
#     # act_id = steps.ActsSteps.generate(107953288, force=1, date=NOW)[0]
#     # # print act_id
#     # # print db.get_act_by_id(act_id)[0]['payment_term_dt']
#     # # print db.get_act_by_id(act_id)[0]['external_id']
#     # # invoice_id = db.get_act_by_id(act_id)[0]['invoice_id']
#     # # steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id, act_id=act_id, invoice_id=invoice_id)
#     # # steps.ExportSteps.export_oebs(act_id=act_id, invoice_id=invoice_id)
#     #
#     # #
#     # #
#     # #
#     # # SERVICE_ID = 7
#     # # PRODUCT_ID = 1475
#     # # agency_id = 7325359
#     # client_id = steps.ClientSteps.create({'AGENCY_ID': 0, 'NAME': 'Racionika | Диеты, рецепты, пп 🥑'})
#     # # client_id = 80893097
#     # #
#     # # # # endbuyer_id = steps.PersonSteps.create(client_id, PersonTypes.ENDBUYER_UR.code)
#     # # contract_id = 425526
#     # # person_id = 6353810
#     # # # SERVICE_ID = 82
#     # # # PRODUCT_ID = 507211
#     # person_id = steps.PersonSteps.create(client_id, 'ur')
#     # service_order_id = steps.OrderSteps.next_id(service_id=7)
#     # order_id = steps.OrderSteps.create(client_id=client_id, product_id=1475, service_id=7,
#     #                                    service_order_id=service_order_id, params={'AgencyID': None})
#     # orders_list = [
#     #     {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': datetime.datetime.now()}
#     # ]
#     # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#     #                                        additional_params={'InvoiceDesireDT': datetime.datetime.now()})
#     # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1003,
#     #                                              credit=None, contract_id=None, overdraft=0
#     #                                              , endbuyer_id=None)
#     # steps.InvoiceSteps.pay(invoice_id)
#     # service_order_id3 = steps.OrderSteps.next_id(service_id=7)
#     # order_id3 = steps.OrderSteps.create(client_id=client_id, product_id=1475, service_id=7,
#     #                                     service_order_id=service_order_id3, params={'AgencyID': None})
#     #
#     # api.medium().CreateTransferMultiple(673234452, [
#     #     {'QtyNew': 16750.0, 'ServiceID': 7, 'Tolerance': 0.0001, 'QtyOld': 17950.0, 'ServiceOrderID': 32687044}],
#     #                                     [{'ServiceID': 7, 'QtyDelta': 1200.0, 'ServiceOrderID': 81662868}], '')
#     #
#     # # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1003,
#     #                                              credit=1, contract_id=contract_id, overdraft=0
#     #                                              , endbuyer_id=endbuyer_id)
#     # # steps.InvoiceSteps.pay(invoice_id)
#     # # # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Days': 100}, 0, dt)
#     # print steps.ActsSteps.enqueue([81097773], force=1, date=NOW)
#     # steps.ExportSteps.get_export_data(81097773, 'Client', 'MONTH_PROC')
#     # steps.ActsSteps.hide(80156373)
#     # act_id = steps.ActsSteps.generate(98, force=1, date=dt)[0]
#     # # print act_id
#     # # steps.ExportSteps.export_oebs(act_id=act_id, invoice_id=invoice_id)
#     #
#     #
#     # # coding: utf-8
#     #
#     # # import datetime
#     # #
#     # # import pytest
#     # # from hamcrest import equal_to
#     # #
#     # # from balance import balance_db as db
#     # # from balance import balance_steps as steps
#     # # from btestlib import utils as utils
#     # #
#     # # dt = datetime.datetime.now() - datetime.timedelta(days=50)
#     # #
#     # # DIRECT_SERVICE_ID = 7
#     # # MARKET_SERVICE_ID = 11
#     # #
#     # # DIRECT_PRODUCT_ID = 1475
#     # # MARKET_PRODUCT_ID = 2136
#     # #
#     # # YANDEX_FIRM_ID = 1
#     # # UA_FIRM_ID = 2
#     # # MARKET_FIRM_ID = 111
#     # #
#     # # PRODUCTS = {DIRECT_SERVICE_ID: DIRECT_PRODUCT_ID,
#     # #             MARKET_SERVICE_ID: MARKET_PRODUCT_ID}
#     # #
#     # # PAYSYS_PERSON_TYPE = {
#     # #     YANDEX_FIRM_ID: (1003, 'ur'),
#     # #     MARKET_FIRM_ID: (11101003, 'ur'),
#     # #     UA_FIRM_ID: (1017, 'ua')
#     # # }
#     # #
#     # # LIMIT_FIRM_SERVICE = {
#     # #     MARKET_SERVICE_ID: {MARKET_FIRM_ID: 1000,
#     # #                         YANDEX_FIRM_ID: 1000,
#     # #                         UA_FIRM_ID: 300},
#     # #     DIRECT_SERVICE_ID: {YANDEX_FIRM_ID: 1000,
#     # #                         UA_FIRM_ID: 300},
#     # # }
#     # #
#     # # OVERDRAFT_LIMIT_FIRM_SERVICE = {
#     # #     MARKET_SERVICE_ID: {MARKET_FIRM_ID: 330,
#     # #                         YANDEX_FIRM_ID: 330,
#     # #                         UA_FIRM_ID: 100},
#     # #     DIRECT_SERVICE_ID: {YANDEX_FIRM_ID: 330,
#     # #                         UA_FIRM_ID: 100},
#     # # }
#     # #
#     # # ACT_DATES = [utils.add_months_to_date(dt, -7),
#     # #              utils.add_months_to_date(dt, -5),
#     # #              utils.add_months_to_date(dt, -4),
#     # #              utils.add_months_to_date(dt, -3),
#     # #              utils.add_months_to_date(dt, -2)
#     # #              ]
#     # #
#     # #
#     # # @pytest.mark.parametrize('overdraft_usage_params', [
#     # #     # {'overdraft_given': [(DIRECT_SERVICE_ID, YANDEX_FIRM_ID, True)],
#     # #     #  'overdraft_use_to': [
#     # #     #      {'params': (MARKET_SERVICE_ID, YANDEX_FIRM_ID), 'result': False},
#     # #     #      {'params': (DIRECT_SERVICE_ID, YANDEX_FIRM_ID), 'result': True}
#     # #     #  ]},
#     # #     # pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
#     # #     # {'overdraft_given': [(MARKET_SERVICE_ID, UA_FIRM_ID, True)],
#     # #     #  'overdraft_use_to': [
#     # #     #      {'params': (DIRECT_SERVICE_ID, UA_FIRM_ID), 'result': False},
#     # #     #      {'params': (MARKET_SERVICE_ID, UA_FIRM_ID), 'result': True},
#     # #     #  ]},
#     # #     #
#     # #     # pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
#     # #     # {'overdraft_given': [(DIRECT_SERVICE_ID, UA_FIRM_ID, True)],
#     # #     #  'overdraft_use_to': [
#     # #     #      {'params': (MARKET_SERVICE_ID, UA_FIRM_ID), 'result': False},
#     # #     #      {'params': (DIRECT_SERVICE_ID, UA_FIRM_ID), 'result': True}
#     # #     #  ]},
#     # #
#     # #     {'overdraft_given': [(MARKET_SERVICE_ID, MARKET_FIRM_ID, True)],
#     # #      'overdraft_use_to': [
#     # #          {'params': (MARKET_SERVICE_ID, MARKET_FIRM_ID), 'result': True}]},
#     # #
#     # #     # {'overdraft_given': [(MARKET_SERVICE_ID, MARKET_FIRM_ID, True)],
#     # #     #  'overdraft_use_to': [
#     # #     #      {'params': (MARKET_SERVICE_ID, YANDEX_FIRM_ID), 'result': True}]},
#     # #     #
#     # #     # {'overdraft_given': None,
#     # #     #  'overdraft_use_to': [
#     # #     #      {'params': (DIRECT_SERVICE_ID, YANDEX_FIRM_ID), 'result': False},
#     # #     #      {'params': (MARKET_SERVICE_ID, YANDEX_FIRM_ID), 'result': False},
#     # #     #  ]},
#     # #     #
#     # #     # {'overdraft_given': None,
#     # #     #  'overdraft_use_to': [
#     # #     #      {'params': (MARKET_SERVICE_ID, UA_FIRM_ID), 'result': False},
#     # #     #      {'params': (DIRECT_SERVICE_ID, UA_FIRM_ID), 'result': False},
#     # #     #  ]},
#     # #     #
#     # #     # {'overdraft_given': None,
#     # #     #  'overdraft_use_to': [
#     # #     #      {'params': (MARKET_SERVICE_ID, MARKET_FIRM_ID), 'result': False}
#     # #     #  ]},
#     # #     #
#     # #     # {'overdraft_given': [(DIRECT_SERVICE_ID, YANDEX_FIRM_ID, True),
#     # #     #                      (DIRECT_SERVICE_ID, UA_FIRM_ID, False)],
#     # #     #  'overdraft_use_to': [
#     # #     #      {'params': (DIRECT_SERVICE_ID, UA_FIRM_ID), 'result': False}
#     # #     #  ]},
#     # #     #
#     # #     # {'overdraft_given': [(DIRECT_SERVICE_ID, YANDEX_FIRM_ID, True),
#     # #     #                      (DIRECT_SERVICE_ID, UA_FIRM_ID, False)],
#     # #     #  'overdraft_use_to': [
#     # #     #      {'params': (DIRECT_SERVICE_ID, YANDEX_FIRM_ID), 'result': True}
#     # #     #  ]},
#     # #
#     # # ])
#     # # def test_overdraft_usage(overdraft_usage_params):
#     # #     client_id = steps.ClientSteps.create()
#     # #     order_owner = client_id
#     # #     invoice_owner = client_id
#     # #
#     # #     steps.ClientSteps.link(invoice_owner, 'aikawa-test-10')
#     # #
#     # #     if overdraft_usage_params['overdraft_given']:
#     # #         for service_id, firm_id, is_not_null_overdraft in overdraft_usage_params['overdraft_given']:
#     # #             if is_not_null_overdraft:
#     # #                 limit = 100
#     # #             else:
#     # #                 limit = 0
#     # steps.OverdraftSteps.set_force_overdraft(6768467, 99, 100000, 12)
#     # #             actual_limit = db.balance().get_overdraft_limit_by_firm(client_id, service_id, firm_id)
#     # #             assert limit == actual_limit
#     # #     else:
#     # #         limit = db.balance().get_overdraft_limit(client_id, DIRECT_SERVICE_ID)
#     # #         assert limit is None
#     # #         limit = db.balance().get_overdraft_limit(client_id, MARKET_SERVICE_ID)
#     # #         assert limit is None
#     # #
#     # #     for overdraft in overdraft_usage_params['overdraft_use_to']:
#     # #         service_id, firm_id = overdraft['params']
#     # #         paysys_id, person_type = PAYSYS_PERSON_TYPE[firm_id]
#     # #         person_id = steps.PersonSteps.create(invoice_owner, person_type)
#     # #         product_id = PRODUCTS[service_id]
#     # #         service_order_id = steps.OrderSteps.next_id(service_id)
#     # #         steps.OrderSteps.create(order_owner, service_order_id, service_id=service_id, product_id=product_id)
#     # #
#     # #         orders_list = [
#     # #             {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}]
#     # #         request_id = steps.RequestSteps.create(invoice_owner, orders_list,
#     # #                                                additional_params={'InvoiceDesireDT': dt})
#     # #         try:
#     # #             invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
#     # #                                                          credit=0, overdraft=1, contract_id=None)
#     # #             steps.InvoiceSteps.pay(invoice_id)
#     # #             db.balance().execute(
#     # #                 '''UPDATE (SELECT * FROM t_invoice WHERE id = :invoice_id) SET PAYMENT_TERM_DT = :dt''',
#     # #                 {'invoice_id': invoice_id, 'dt': datetime.datetime.now() - datetime.timedelta(days=15)})
#     # #             steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 100}, 0, dt)
#     # #             print dt
#     # #             act_id = steps.ActsSteps.generate(client_id, force=0, date=dt)[0]
#     # #             print act_id
#     # #             print db.get_act_by_id(act_id)[0]['payment_term_dt']
#     # #             print db.get_act_by_id(act_id)[0]['dt']
#     # #             print db.get_act_by_id(act_id)[0]['external_id']
#     # #             db.balance().execute(
#     # #                 '''UPDATE (SELECT * FROM T_ACT_INTERNAL WHERE id = :act_id) SET DT = :dt''',
#     # #                 {'act_id': act_id, 'dt': datetime.datetime.now() - datetime.timedelta(days=15)})
#     # #             invoice_id = db.get_act_by_id(act_id)[0]['invoice_id']
#     # #             steps.ExportSteps.export_oebs(client_id=client_id, contract_id=None, act_id=act_id, invoice_id=invoice_id)
#     # #             assert overdraft['result'] is True
#     # #         except Exception, exc:
#     # #             print exc
#     # #             utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'NOT_ENOUGH_OVERDRAFT_LIMIT'))
#     # #             assert overdraft['result'] is False
