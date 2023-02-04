# coding: utf-8

import datetime

import pytest
from hamcrest import equal_to, is_in

import balance.balance_db as db
import btestlib.utils as utils
import pagediff_steps

from balance.pagediff_tests.data_preparator import prepare_invoice

## Данные  для генерации кейсов лежат в oferta_and_bank.xlsx
params = [
    {'offer_id':'3', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['7-1475'], 'date': datetime.datetime(2011, 1, 23)},
    {'offer_id':'5', 'paysys_id':1014, 'bank_details_id': [3], 'person_type':'yt', 'services_products':['7-503162'], 'date': datetime.datetime(2015,4,2)},
    {'offer_id':'7', 'paysys_id':1013, 'bank_details_id': [5], 'person_type':'yt', 'services_products':['7-503163'], 'date': datetime.datetime(2016,5,23)},
    {'offer_id':'8', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['7-1475'], 'date': datetime.datetime(2011, 1, 24)},
    ##{'offer_id':'9', 'paysys_id':1017, 'bank_details_id': [7], 'person_type':'ua', 'services_products':['7-1475'], 'date': datetime.datetime(2012, 9, 9)}, ##отключили Украину
    ##{'offer_id':'9', 'paysys_id':1017, 'bank_details_id': [7], 'person_type':'ua', 'services_products':['7-1475'], 'date': datetime.datetime(2012, 9, 9), 'is_agency':1}, ##BALANCE-24471   отключили Украину
    ##{'offer_id':'9', 'paysys_id':1018, 'bank_details_id': [7], 'person_type':'pu', 'services_products':['7-1475'], 'date': datetime.datetime(2012, 9, 9)}, ##отключили Украину
    {'offer_id':'10', 'paysys_id':1001, 'bank_details_id': [2], 'person_type':'ph', 'services_products':['7-1475'], 'date': datetime.datetime(2011, 9, 15)},
    {'offer_id':'10', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['7-1475'], 'date': datetime.datetime(2011, 9, 15)},
    {'offer_id':'11', 'paysys_id':1001, 'bank_details_id': [2], 'person_type':'ph', 'services_products':['5-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'11', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['6-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'12', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['114-502981'], 'date': datetime.datetime.now()},
    {'offer_id':'12', 'paysys_id':1014, 'bank_details_id': [3], 'person_type':'yt', 'services_products':['114-502981'], 'date': datetime.datetime.now()},
    {'offer_id':'13', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['7-1475'], 'date': datetime.datetime(2016, 5, 23)},
    {'offer_id':'14', 'paysys_id':1043, 'bank_details_id': [221], 'person_type':'sw_ur', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'14', 'paysys_id':1068, 'bank_details_id': [220], 'person_type':'sw_ph', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'14', 'paysys_id':1044, 'bank_details_id': [222], 'person_type':'sw_ur', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'15', 'paysys_id':1071, 'bank_details_id': [220], 'person_type':'sw_ytph', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'15', 'paysys_id':1047, 'bank_details_id': [224], 'person_type':'sw_yt', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'15', 'paysys_id':1046, 'bank_details_id': [223], 'person_type':'sw_yt', 'services_products':['7-503164'], 'date': datetime.datetime.now()},
    {'offer_id':'16', 'paysys_id':1029, 'bank_details_id': [41], 'person_type':'usp', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'16', 'paysys_id':1028, 'bank_details_id': [41], 'person_type':'usu', 'services_products':['7-1475'], 'date': datetime.datetime(2013, 8, 26)},
    {'offer_id':'17', 'paysys_id':1051, 'bank_details_id': [300], 'person_type':'trp', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'17', 'paysys_id':1050, 'bank_details_id': [300], 'person_type':'tru', 'services_products':['7-1475'], 'date': datetime.datetime.now()}, ##BALANCE-24533
    {'offer_id':'17', 'paysys_id':1050, 'bank_details_id': [300], 'person_type':'tru', 'services_products':['70-503278'], 'date': datetime.datetime.now()}, ##BALANCE-24533
    ##{'offer_id':'17', 'paysys_id':1050, 'bank_details_id': [300], 'person_type':'tru', 'services_products':['77-504083'], 'date': datetime.datetime.now()}, ##отключили Баян по оферте BALANCE-27119
    ##{'offer_id':'17', 'paysys_id':1051, 'bank_details_id': [300], 'person_type':'trp', 'services_products':['77-504083'], 'date': datetime.datetime.now()}, ##отключили Баян по оферте BALANCE-27120
    ##{'offer_id':'18', 'paysys_id':1017, 'bank_details_id': [7], 'person_type':'ua', 'services_products':['7-1475'], 'date': datetime.datetime(2012, 9, 10)}, ##отключили Украину
    ##{'offer_id':'18', 'paysys_id':1018, 'bank_details_id': [7], 'person_type':'pu', 'services_products':['7-1475'], 'date': datetime.datetime(2012, 9, 10)}, ##отключили Украину
    ##{'offer_id':'18', 'paysys_id':1017, 'bank_details_id': [7], 'person_type':'ua', 'services_products':['7-1475'], 'date': datetime.datetime(2012, 9, 10), 'is_agency':1}, ##BALANCE-24471    отключили Украину
    ##{'offer_id':'20', 'paysys_id':1060, 'bank_details_id': [320], 'person_type':'yt_kzu', 'services_products':['11-2136'], 'date': datetime.datetime(2013, 3, 1)}, ##20 оферта теперь 42
    {'offer_id':'21', 'paysys_id':1001, 'bank_details_id': [2], 'person_type':'ph', 'services_products':['48-503363'], 'date': datetime.datetime.now()},
    {'offer_id':'21', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['48-503363'], 'date': datetime.datetime.now()},
    {'offer_id':'22', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['7-503162'], 'date': datetime.datetime(2013, 8, 25), 'currency':'RUB'},
    {'offer_id':'24', 'paysys_id':1028, 'bank_details_id': [41], 'person_type':'usu', 'services_products':['7-1475'], 'date': datetime.datetime(2013, 8, 25)},
    {'offer_id':'24', 'paysys_id':1029, 'bank_details_id': [41], 'person_type':'usp', 'services_products':['7-1475'], 'date': datetime.datetime(2013, 8, 25)},
    ##{'offer_id':'25', 'paysys_id':1091, 'bank_details_id': [501], 'person_type':'ur_autoru', 'services_products':['99-504596'], 'date': datetime.datetime.now()}, ##10 фирмыуже нет
    ##{'offer_id':'25', 'paysys_id':1092, 'bank_details_id': [501], 'person_type':'ph_autoru', 'services_products':['99-504596'], 'date': datetime.datetime.now()}, ##10 фирмыуже нет
    ##{'offer_id':'26', 'paysys_id':1092, 'bank_details_id': [501], 'person_type':'ph_autoru', 'services_products':['99-504697'], 'date': datetime.datetime.now()}, ##10 фирмыуже нет
    ##{'offer_id':'26', 'paysys_id':1091, 'bank_details_id': [501], 'person_type':'ur_autoru', 'services_products':['99-504697'], 'date': datetime.datetime.now()}, ##10 фирмыуже нет
    {'offer_id':'27', 'paysys_id':1201003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['98-505057'], 'date': datetime.datetime(2015, 7, 29)},
    {'offer_id':'28', 'paysys_id':1201001, 'bank_details_id': [2], 'person_type':'ph', 'services_products':['98-505057'], 'date': datetime.datetime(2015, 7, 30)},
    {'offer_id':'28', 'paysys_id':1201033, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['98-505057'], 'date': datetime.datetime(2015, 7, 30)},
    ##{'offer_id':'29', 'paysys_id':1003, 'bank_details_id': [61], 'person_type':'ur', 'services_products':['35-506525'], 'date': datetime.datetime.now()}, ##не выставиться сейчас
    ##{'offer_id':'30', 'paysys_id':1014, 'bank_details_id': [3], 'person_type':'yt', 'services_products':['35-506525'], 'date': datetime.datetime.now()}, ##не выставиться сейчас
    {'offer_id':'31', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['98-506624'], 'date': datetime.datetime(2018,1,15)},
    ##{'offer_id':'32', 'paysys_id':1201001, 'bank_details_id': [502], 'person_type':'ph', 'services_products':['98-506624'], 'date': datetime.datetime.now()}, ##BALANCE-27064(туры теперь в 1 фирме)
    ##{'offer_id':'33', 'paysys_id':1201003, 'bank_details_id': [502], 'person_type':'ur', 'services_products':['98-505057'], 'date': datetime.datetime.now()}, ##BALANCE-27064(туры теперь в 1 фирме)
    {'offer_id':'34', 'paysys_id':1201003, 'bank_details_id': [502,88016007], 'person_type':'ur', 'services_products':['90-506655'], 'date': datetime.datetime.now()},
    {'offer_id':'35', 'paysys_id':1201003, 'bank_details_id': [502,88016007], 'person_type':'ur', 'services_products':['82-507211'], 'date': datetime.datetime(2018,6,3)}, ##BALANCE-28353
    {'offer_id':'36', 'paysys_id':1014, 'bank_details_id': [3], 'person_type':'yt', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'38', 'paysys_id':1001, 'bank_details_id': [2], 'person_type':'ph', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'38', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
    {'offer_id':'39', 'paysys_id':11101014, 'bank_details_id': [972], 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime(2017,3,31)},
    {'offer_id':'40', 'paysys_id':11101013, 'bank_details_id': [973], 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime(2017,1,22)}, ##BALANCE-24460
    ##{'offer_id':'40', 'paysys_id':11101100, 'bank_details_id': [514], 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime.now()},
    {'offer_id':'40', 'paysys_id':11101023, 'bank_details_id': [974], 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime.now()},
    {'offer_id':'41', 'paysys_id':11101003, 'bank_details_id': [972], 'person_type':'ur', 'services_products':['11-2136'], 'date': datetime.datetime(2017,1,23)}, ##BALANCE-24460
    {'offer_id':'41', 'paysys_id':11101001, 'bank_details_id': [972], 'person_type':'ph', 'services_products':['11-2136'], 'date': datetime.datetime.now()},
    {'offer_id':'42', 'paysys_id':11101060, 'bank_details_id': [975], 'person_type':'yt_kzu', 'services_products':['11-2136'], 'date': datetime.datetime.now()},
    {'offer_id':'43', 'paysys_id':11101003, 'bank_details_id': [972], 'person_type':'ur', 'services_products':['11-506537'], 'date': datetime.datetime.now()},
    {'offer_id':'43', 'paysys_id':11101001, 'bank_details_id': [972], 'person_type':'ph', 'services_products':['11-506525'], 'date': datetime.datetime.now()},
    {'offer_id':'44', 'paysys_id':11101014, 'bank_details_id': [972], 'person_type':'yt', 'services_products':['11-506525'], 'date': datetime.datetime.now()},
    {'offer_id':'45', 'paysys_id':1013, 'bank_details_id': [5], 'person_type':'yt', 'services_products':['7-1475'], 'date': datetime.datetime.now()}, ##BALANCE-24428
    {'offer_id':'45', 'paysys_id':1023, 'bank_details_id': [6], 'person_type':'yt', 'services_products':['7-1475'], 'date': datetime.datetime.now()}, ##BALANCE-24428
    ##{'offer_id':'46', 'paysys_id':1100, 'bank_details_id': [3], 'person_type':'yt', 'services_products':['37-502953'], 'date': datetime.datetime.now()}, ##BALANCE-24510     сейчас не выставиться
    ##{'offer_id':'47', 'paysys_id':1014, 'bank_details_id': [3], 'person_type':'yt', 'services_products':['77-2584'], 'date': datetime.datetime.now()}, ##отключили Баян по оферте BALANCE-27119
    {'offer_id':'48', 'paysys_id':1201003, 'bank_details_id': [502,88016007], 'person_type':'ur', 'services_products':['99-504533'], 'date': datetime.datetime.now()},
    {'offer_id':'48', 'paysys_id':1201001, 'bank_details_id': [502,88016007], 'person_type':'ph', 'services_products':['99-504533'], 'date': datetime.datetime.now()},
    {'offer_id':'49', 'paysys_id':1201003, 'bank_details_id': [502,88016007], 'person_type':'ur', 'services_products':['99-504697'], 'date': datetime.datetime.now()},
    {'offer_id':'49', 'paysys_id':1201001, 'bank_details_id': [502,88016007], 'person_type':'ph', 'services_products':['99-504697'], 'date': datetime.datetime.now()},
    {'offer_id':'51', 'paysys_id':1003, 'bank_details_id': [21], 'person_type':'ur', 'services_products':['102-504930'], 'date': datetime.datetime.now()},
    ##{'offer_id':'52', 'paysys_id':1075, 'bank_details_id': [240], 'person_type':'by_ytph', 'services_products':['7-1475'], 'date': datetime.datetime.now()}, ##BALANCE-24437   текст пока не показываем
    {'offer_id':'53', 'paysys_id':1801003, 'bank_details_id': [88016006], 'person_type':'ur', 'services_products':['26-508472'], 'date': datetime.datetime.now()}, ##BALANCE-24624
    {'offer_id':'54', 'paysys_id':11101003, 'bank_details_id': [972], 'person_type':'ur', 'services_products':['139-508244'], 'date': datetime.datetime.now()}, ##BALANCE-25081
    {'offer_id':'55', 'paysys_id':11101014, 'bank_details_id': [972], 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime.now()}, ##BALANCE-25057
    {'offer_id':'56', 'paysys_id':11101100, 'bank_details_id': [976], 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime.now()}, ##BALANCE-25057
    {'offer_id':'57', 'paysys_id':2501020, 'bank_details_id': [607], 'person_type':'kzu', 'services_products':['7-1475'], 'date': datetime.datetime.now()}, ##BALANCE-24902
    {'offer_id':'57', 'paysys_id':2501020, 'bank_details_id': [607], 'person_type':'kzu', 'services_products':['37-508261'], 'date': datetime.datetime.now()}, ##BALANCE-28711
    {'offer_id':'58', 'paysys_id':1003, 'bank_details_id': [21], 'person_type':'ur', 'services_products':['201-508305'], 'date': datetime.datetime.now()}, ##BALANCE-25534
    {'offer_id':'60', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['177-508314'], 'date': datetime.datetime.now()}, ##BALANCE-25908
    {'offer_id':'61', 'paysys_id':2701101, 'bank_details_id': [963], 'person_type':'byu', 'services_products':['7-1475'], 'date': datetime.datetime(2018,7,1)}, ##BALANCE-25908 дата для того, чтобы не скакали данные из-за курса
    {'offer_id':'62', 'paysys_id':1301003, 'bank_details_id': [510], 'person_type':'ur', 'services_products':['124-504691'], 'date': datetime.datetime.now()},
    {'offer_id':'62', 'paysys_id':1301001, 'bank_details_id': [510], 'person_type':'ph', 'services_products':['124-504691'], 'date': datetime.datetime.now()},
    {'offer_id':'64', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['98-506624'], 'date': datetime.datetime(2018,2,1)}, ##BALANCE-27113
    {'offer_id':'65', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['98-505057'], 'date': datetime.datetime(2018,2,1)}, ##BALANCE-27113
    {'offer_id':'66', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['202-508664'], 'date': datetime.datetime.now()}, ##BALANCE-27195
    ##{'offer_id':'68', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['98-506587'], 'date': datetime.datetime.now()}, ##BALANCE-27381, BALANCE-29321
    {'offer_id':'69', 'paysys_id':1003, 'bank_details_id': [3, 21, 61], 'person_type':'ur', 'services_products':['103-508986'], 'date': datetime.datetime.now()},
    {'offer_id':'70', 'paysys_id':1301003, 'bank_details_id': [510], 'person_type':'ur', 'services_products':['135-507154'], 'date': datetime.datetime.now()}, ##BALANCE-28161
    {'offer_id':'71', 'paysys_id':1201003, 'bank_details_id': [502,88016007], 'person_type':'ur', 'services_products':['82-507211'], 'date': datetime.datetime(2018,6,4)}, ##BALANCE-28353
    ##{'offer_id':'0', 'paysys_id':2701102, 'bank_details_id': [963], 'person_type':'byp', 'services_products':['37-502953'], 'date': datetime.datetime.now()}, ##BALANCE-24510  BALANCE-30634
    {'offer_id':'0', 'paysys_id':1601047, 'bank_details_id': [228], 'person_type':'sw_yt', 'services_products':['42-507130'], 'date': datetime.datetime.now()}, ##BALANCE-23421
    ##{'offer_id':'0', 'paysys_id':2701101, 'bank_details_id': [963], 'person_type':'byu', 'services_products':['37-502953'], 'date': datetime.datetime.now()}, ##BALANCE-24510  BALANCE-30634

]



@pytest.mark.parametrize('casedata', params,
                         ids=lambda x: "offer_{}_paysys_{}_services_products_{}".format(x['offer_id'], x['paysys_id'], x['services_products']))
def test_invoice_page_ci(casedata):
    unique_name = "invoice_ci_offer_{}_paysys_{}_services_products_{}".format(casedata['offer_id'],
                                                                              casedata['paysys_id'],
                                                                              casedata['services_products'])
    invoice_data = prepare_invoice(**casedata)
    check_offer_and_bank_id_in_invoice(invoice_data['invoice_id'], casedata['offer_id'], casedata['bank_details_id'])
    pagediff_steps.check_invoice_page_ci(unique_name, invoice_data['invoice_id'])



@pytest.mark.parametrize('casedata', params,
                         ids=lambda x: "offer_{}_paysys_{}_services_products_{}".format(x['offer_id'], x['paysys_id'], x['services_products']))
def test_invoice_publish_page(casedata):
    unique_name = "invoice_publish_pf_{}_paysys_{}_services_products_{}".format(casedata['offer_id'], casedata['paysys_id'], casedata['services_products'])
    invoice_data = prepare_invoice(**casedata)
    pagediff_steps.check_publish_page(unique_name, invoice_data['invoice_id'], invoice_data['invoice_eid'])

@pytest.mark.parametrize('casedata', params,
                         ids=lambda x: "offer_{}_paysys_{}_services_products_{}".format(x['offer_id'], x['paysys_id'],

                                                                                        x['services_products']))
def test_paypreview_page(casedata):
    unique_name = "paypreview_offer_{}_paysys_{}_services_products_{}".format(casedata['offer_id'],
                                                                              casedata['paysys_id'],
                                                                              casedata['services_products'])
    invoice_data = prepare_invoice(**casedata)
    pagediff_steps.check_paypreview_page(unique_name, invoice_data)


def check_offer_and_bank_id_in_invoice(invoice_id, expected_offer_id, expected_bank_details_id):
    query = 'select offer_type_id, bank_details_id from t_invoice where id=:invoice_id'
    fact_data = db.balance().execute(query, {'invoice_id': invoice_id})[0]
    utils.check_that(str(fact_data['offer_type_id']), equal_to(expected_offer_id),
                     u'Проверяем соответствие номера оферты в базе')
    utils.check_that(fact_data['bank_details_id'], is_in(expected_bank_details_id), u'Проверяем соответствие номера банка в базе')
