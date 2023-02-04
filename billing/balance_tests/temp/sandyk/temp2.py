# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
# import balance.tests.conftest  as conftest
import balance.balance_steps as steps
import balance.balance_api as api

MAIN_DT = datetime.datetime.now()
# START_DT = str(MAIN_DT.strftime("%Y-%m-%d")) + 'T00:00:00'
# tm.Balance.GetManagersInfo(['168681342'])
# steps.InvoiceSteps.pay(69216832,1999.7)
# steps.InvoiceSteps.pay_fair(69168426,100)
# api.test_balance().ExportObject('OEBS', 'ThirdPartyTransaction', '15002631439', 2)
# api.test_balance().Enqueue('ThirdPartyTransaction',   '15002631439', 'OEBS', 0,0,1)
# steps.InvoiceSteps.pay(70314722)

# api.test_balance().MigrateToBatchStructure([{'ObjectID':1215922, 'ObjectType':'ContractCollateral' }])
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id  , {'Money': QTY}, 0, campaigns_dt=MAIN_DT)
# steps.ActsSteps.generate(client_id, 1, MAIN_DT)
#
# steps.CampaignsSteps.do_campaigns(129, 22777039  , {'Money': 90}, 0, campaigns_dt=MAIN_DT)
# steps.ActsSteps.generate(56699445, 1, MAIN_DT)

# test_ts = xmlrpclib.ServerProxy("http://xmlrpc.balance.greed-ts1f.yandex.ru:30702/xmlrpc", allow_none=1, use_datetime=1)
# test_ts.TestBalance.GetPartnerCompletions({'completion_source': 'health',
#   'end_dt': datetime.datetime(2017, 7, 7, 0, 0), 'start_dt': datetime.datetime(2017, 7, 7, 0, 0)})
# api.test_balance().Enqueue('ThirdPartyTransaction',   '15002631439', 'OEBS')
# steps.CommonSteps.export('OEBS', 'Act', 71489667)

# steps.CommonSteps.get_last_notification(10, 56368593)
# steps.ClientSteps.create()
# steps.CommonSteps.export('OEBS', 'Person', 6425597)
# steps.ExportSteps.export_oebs(client_id=57391220, invoice_id=70042297, person_id = 6471509)
#
# api.test_balance().GetHost()

steps.CloseMonth.update_limits(MAIN_DT, 1, [80850346])
# api.test_balance().Enqueue('Cli', 1975250, 'OEBS')
# client_id = steps.ClientSteps.create(params={'IS_AGENCY':0})
# client_id = 57527616
# person_id = steps.PersonSteps.create(client_id,'ur')
# steps.ClientSteps.link( 60551608 ,'sandyk-yndx-10')
# api.medium().GetContractPrintForm(974161, 'contract')
# steps.ExportSteps.export_oebs(client_id = 57611294, person_id = 6519023, contract_id=927040, invoice_id = 70101275, act_id=71520791)
# client_id = 57431410
# person_id = steps.PersonSteps.create(client_id,'ur')
# steps.ExportSteps.export_oebs(client_id=57391220, contract_id=843887, collateral_id=)
# person_id = steps.PersonSteps.create(client_id, 'ur',params={ 'legal_address_postcode':"191025"
# , 'legal_adress_home':u"а/я Кладр 0"})



# 'legal_adress_city':''
# steps.ClientSteps.get_client_id_by_passport_id('16571028')
# steps.ClientSteps.link(1975250   ,'clientuid45')
# steps.CommonSteps.export('OEBS', 'Person', 6425597)
# api.medium().GetPersonalAccount(
#     {'PersonID': 6622870, 'PaysysID': 1001, 'FirmID': 1, 'ProductID': 508899 })

# steps.ClientSteps.get_client_contracts(57066078, ContractSubtype.GENERAL, dt = datetime.datetime(2017,12,14))

# api.medium().QueryCatalog(['V_DISTRIBUTION_CONTRACT'],"v_distribution_contract.client_id =873982")
# api.medium().GetClientPersons('477530')
# api.medium().GetInvoice(30416341,67962927)
# api.medium().FindClient({"PrimaryClients":1,"AgencySelectPolicy":1,"PassportID":"313835460"})
api.test_balance().GetHost()
# steps.ClientSteps.create()
# @pytest.mark.xdist_same_slave
# def test_qwe():
#     api.medium().CreateUserClientAssociation(16571028, 56594969, '559623017')
#
# def test_qwe2():
#     api.medium().CreateUserClientAssociation(16571028, 56559826, '559623017')


#################################
# class App:
#     def __init__(self):
#         print 'init'
#
# @pytest.fixture(scope="session")
# def app():
#     return App()
#
# def test_exists(app):
#     api.medium().CreateUserClientAssociation(16571028, 56594969, '559623017')
#     assert  app
#
# def test_exists2(app):
#     api.medium().CreateUserClientAssociation(16571028, 56559826, '559623017')
#     assert app
#################################


# api.test_balance().OEBSPayment(69314509)
# api.medium().GetRequestChoices({'OperatorUid': '128280859', 'RequestID': 425875975})
# steps.ClientSteps.create({'REGION_ID':983})
# agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
##168681342  16571028
# steps.PersonSteps.create(30416341,'sw_ytph', passport_uid='168681342',
#                          params={  'client_id': 30416341,
#                          'person_id':4724594, 'postcode': '4444', 'verified-docs': '0'} )
# steps.ClientSteps.migrate_to_currency(19353881,'MODIFY',None,7,149,'RUB')
# steps.InvoiceSteps.pay(69618888)
# def test_q():
#  print steps.PromocodeSteps.check_invoice_is_with_discount(69618484, bonus=126,
#                                                     is_with_discount=True, qty=126,
#                                                     nds=1.2,
#                                                     precision='0.000001')

# steps.ClientSteps.link(56513592,'sandyk-yndx-14')
# steps.CampaignsSteps.do_campaigns(7, 61689883, {'Bucks': 50}, 0, campaigns_dt=MAIN_DT)
# steps.CommonPartnerSteps.generate_partner_acts_fair(5885953, MAIN_DT)
# steps.ClientSteps.set_force_overdraft(4395268,7,99999,currency = 'RUB')
# steps.ActsSteps.generate(31658553,1,MAIN_DT)

# steps.ActsSteps.generate(12035627, force=1, date=datetime.datetime.now())
# import balance.balance_db as db
# db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_DISTR_CONTRACT_PLACES','C'); END;")
# steps.CommonSteps.get_last_notification(1,366186107)
# steps.CommonSteps.get_last_notification(10,32458447)
# steps.CommonSteps.export('RESUSPENSION', 'Contract', 574480)
# steps.CommonSteps.get_last_notification(10,32326428)
# steps.ContractSteps.create_contract_new('sw_opt_client',
#                              {'CLIENT_ID': 29988075, 'PERSON_ID': 4696841, 'DT': START_DT, 'IS_SIGNED': START_DT,
#                              'SERVICES': [114],
#                              'FIRM': 16, 'PAYMENT_TYPE': 2, 'CURRENCY': 978})
# steps.CommonSteps.export('RESUSPENSION','Contract',320335)

# api.test_balance().Enqueue('ThirdPartyCorrection', '1259737476', 'OEBS')

# steps.ContractSteps.get_contract_collateral_ids(591836)
# print steps.ContractSteps.get_contract_atribute_value(591836,'CONTRACT_PROJECTS','128787239181488926388254787329526501914')[0]['value_num']
#
# steps.CloseMonth.get_task_id('adfox_alignment')
# steps.CloseMonth.resolve_task('adfox_alignment')

# steps.CloseMonth.set_mnclose_status
# api.medium().GetClientContracts({'ClientID': 39722117, 'ContractType': 'GENERAL'})
# api.medium().GetClientPersons(46110)
