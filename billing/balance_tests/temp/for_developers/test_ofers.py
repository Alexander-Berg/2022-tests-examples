# coding: utf-8
__author__ = 'sandyk'

import datetime

import pytest

from balance.pagediff_tests.data_preparator import prepare_invoice

params = [
{'unique_name':'oferta_3_paysys_1003_service_product_[7-1475]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['7-1475'], 'date': datetime.datetime(2011, 1, 23)},
{'unique_name':'oferta_5_paysys_1014_service_product_[7-503162]', 'paysys_id':1014, 'person_type':'yt', 'services_products':['7-503162'], 'date': datetime.datetime(2015,4,2)},
{'unique_name':'oferta_7_paysys_1013_service_product_[7-503163]', 'paysys_id':1013, 'person_type':'yt', 'services_products':['7-503163'], 'date': datetime.datetime(2016,5,23)},
{'unique_name':'oferta_8_paysys_1003_service_product_[7-1475]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['7-1475'], 'date': datetime.datetime(2011, 1, 24)},
{'unique_name':'oferta_10_paysys_1001_service_product_[7-1475]', 'paysys_id':1001, 'person_type':'ph', 'services_products':['7-1475'], 'date': datetime.datetime(2011, 9, 15)},
{'unique_name':'oferta_10_paysys_1003_service_product_[7-1475]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['7-1475'], 'date': datetime.datetime(2011, 9, 15)},
{'unique_name':'oferta_11_paysys_1001_service_product_[5-1475]', 'paysys_id':1001, 'person_type':'ph', 'services_products':['5-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_11_paysys_1003_service_product_[6-1475]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['6-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_12_paysys_1003_service_product_[114-502981]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['114-502981'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_12_paysys_1014_service_product_[114-502981]', 'paysys_id':1014, 'person_type':'yt', 'services_products':['114-502981'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_14_paysys_1043_service_product_[7-1475]', 'paysys_id':1043, 'person_type':'sw_ur', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_14_paysys_1068_service_product_[7-1475]', 'paysys_id':1068, 'person_type':'sw_ph', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_14_paysys_1044_service_product_[7-1475]', 'paysys_id':1044, 'person_type':'sw_ur', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_15_paysys_1071_service_product_[7-1475]', 'paysys_id':1071, 'person_type':'sw_ytph', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_15_paysys_1047_service_product_[7-1475]', 'paysys_id':1047, 'person_type':'sw_yt', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_15_paysys_1046_service_product_[7-503164]', 'paysys_id':1046, 'person_type':'sw_yt', 'services_products':['7-503164'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_16_paysys_1029_service_product_[7-1475]', 'paysys_id':1029, 'person_type':'usp', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_16_paysys_1028_service_product_[7-1475]', 'paysys_id':1028, 'person_type':'usu', 'services_products':['7-1475'], 'date': datetime.datetime(2013, 8, 26)},
{'unique_name':'oferta_17_paysys_1051_service_product_[7-1475]', 'paysys_id':1051, 'person_type':'trp', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_17_paysys_1050_service_product_[7-1475]', 'paysys_id':1050, 'person_type':'tru', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_17_paysys_1050_service_product_[70-503278]', 'paysys_id':1050, 'person_type':'tru', 'services_products':['70-503278'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_21_paysys_1001_service_product_[48-503363]', 'paysys_id':1001, 'person_type':'ph', 'services_products':['48-503363'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_21_paysys_1003_service_product_[48-503363]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['48-503363'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_22_paysys_1003_service_product_[7-503162]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['7-503162'], 'date': datetime.datetime(2013, 8, 25), 'currency':'RUB'},
{'unique_name':'oferta_24_paysys_1028_service_product_[7-1475]', 'paysys_id':1028, 'person_type':'usu', 'services_products':['7-1475'], 'date': datetime.datetime(2013, 8, 25)},
{'unique_name':'oferta_24_paysys_1029_service_product_[7-1475]', 'paysys_id':1029, 'person_type':'usp', 'services_products':['7-1475'], 'date': datetime.datetime(2013, 8, 25)},
{'unique_name':'oferta_27_paysys_1201003_service_product_[98-505057]', 'paysys_id':1201003, 'person_type':'ur', 'services_products':['98-505057'], 'date': datetime.datetime(2015, 7, 29)},
{'unique_name':'oferta_28_paysys_1201001_service_product_[98-505057]', 'paysys_id':1201001, 'person_type':'ph', 'services_products':['98-505057'], 'date': datetime.datetime(2015, 7, 30)},
{'unique_name':'oferta_28_paysys_1201033_service_product_[98-505057]', 'paysys_id':1201033, 'person_type':'ur', 'services_products':['98-505057'], 'date': datetime.datetime(2015, 7, 30)},
{'unique_name':'oferta_32_paysys_1201003_service_product_[98-506624]', 'paysys_id':1201003, 'person_type':'ur', 'services_products':['98-506624'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_32_paysys_1201001_service_product_[98-506624]', 'paysys_id':1201001, 'person_type':'ph', 'services_products':['98-506624'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_33_paysys_1201003_service_product_[98-505057]', 'paysys_id':1201003, 'person_type':'ur', 'services_products':['98-505057'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_34_paysys_1201003_service_product_[90-506655]', 'paysys_id':1201003, 'person_type':'ur', 'services_products':['90-506655'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_35_paysys_1201003_service_product_[82-507211]', 'paysys_id':1201003, 'person_type':'ur', 'services_products':['82-507211'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_36_paysys_1014_service_product_[7-1475]', 'paysys_id':1014, 'person_type':'yt', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_38_paysys_1001_service_product_[7-1475]', 'paysys_id':1001, 'person_type':'ph', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_38_paysys_1003_service_product_[7-1475]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['7-1475'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_39_paysys_11101014_service_product_[11-2136]', 'paysys_id':11101014, 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime(2017,3,31)},
{'unique_name':'oferta_40_paysys_11101013_service_product_[11-2136]', 'paysys_id':11101013, 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime(2017,1,22)}, ##BALANCE-24460
{'unique_name':'oferta_40_paysys_11101100_service_product_[11-2136]', 'paysys_id':11101100, 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_40_paysys_11101023_service_product_[11-2136]', 'paysys_id':11101023, 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_41_paysys_11101003_service_product_[11-2136]', 'paysys_id':11101003, 'person_type':'ur', 'services_products':['11-2136'], 'date': datetime.datetime(2017,1,23)}, ##BALANCE-24460
{'unique_name':'oferta_41_paysys_11101001_service_product_[11-2136]', 'paysys_id':11101001, 'person_type':'ph', 'services_products':['11-2136'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_42_paysys_11101060_service_product_[11-2136]', 'paysys_id':11101060, 'person_type':'yt_kzu', 'services_products':['11-2136'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_43_paysys_11101003_service_product_[11-506537]', 'paysys_id':11101003, 'person_type':'ur', 'services_products':['11-506537'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_43_paysys_11101001_service_product_[11-506525]', 'paysys_id':11101001, 'person_type':'ph', 'services_products':['11-506525'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_44_paysys_11101014_service_product_[11-506525]', 'paysys_id':11101014, 'person_type':'yt', 'services_products':['11-506525'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_45_paysys_1013_service_product_[7-1475]', 'paysys_id':1013, 'person_type':'yt', 'services_products':['7-1475'], 'date': datetime.datetime.now()}, ##BALANCE-24428
{'unique_name':'oferta_45_paysys_1023_service_product_[7-1475]', 'paysys_id':1023, 'person_type':'yt', 'services_products':['7-1475'], 'date': datetime.datetime.now()}, ##BALANCE-24428
{'unique_name':'oferta_48_paysys_1201003_service_product_[99-504533]', 'paysys_id':1201003, 'person_type':'ur', 'services_products':['99-504533'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_48_paysys_1201001_service_product_[99-504533]', 'paysys_id':1201001, 'person_type':'ph', 'services_products':['99-504533'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_49_paysys_1201003_service_product_[99-504697]', 'paysys_id':1201003, 'person_type':'ur', 'services_products':['99-504697'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_49_paysys_1201001_service_product_[99-504697]', 'paysys_id':1201001, 'person_type':'ph', 'services_products':['99-504697'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_53_paysys_1801003_service_product_[26-508472]', 'paysys_id':1801003, 'person_type':'ur', 'services_products':['26-508472'], 'date': datetime.datetime.now()}, ##BALANCE-24624
{'unique_name':'oferta_54_paysys_11101003_service_product_[139-508244]', 'paysys_id':11101003, 'person_type':'ur', 'services_products':['139-508244'], 'date': datetime.datetime.now()}, ##BALANCE-25081
{'unique_name':'oferta_55_paysys_11101014_service_product_[11-2136]', 'paysys_id':11101014, 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime.now()}, ##BALANCE-25057
{'unique_name':'oferta_56_paysys_11101100_service_product_[11-2136]', 'paysys_id':11101100, 'person_type':'yt', 'services_products':['11-2136'], 'date': datetime.datetime.now()}, ##BALANCE-25057
{'unique_name':'oferta_57_paysys_2501020_service_product_[7-1475]', 'paysys_id':2501020, 'person_type':'kzu', 'services_products':['7-1475'], 'date': datetime.datetime.now()}, ##BALANCE-24902
{'unique_name':'oferta_58_paysys_1003_service_product_[201-508305]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['201-508305'], 'date': datetime.datetime.now()}, ##BALANCE-25534
{'unique_name':'oferta_60_paysys_1003_service_product_[177-508314]', 'paysys_id':1003, 'person_type':'ur', 'services_products':['177-508314'], 'date': datetime.datetime.now()}, ##BALANCE-25908
{'unique_name':'oferta_61_paysys_2701101_service_product_[7-1475]', 'paysys_id':2701101, 'person_type':'byu', 'services_products':['7-1475'], 'date': datetime.datetime(2018,1,31)}, ##BALANCE-25908
{'unique_name':'oferta_62_paysys_1301003_service_product_[124-504691]', 'paysys_id':1301003, 'person_type':'ur', 'services_products':['124-504691'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_62_paysys_1301001_service_product_[124-504691]', 'paysys_id':1301001, 'person_type':'ph', 'services_products':['124-504691'], 'date': datetime.datetime.now()},
{'unique_name':'oferta_-_paysys_1601047_service_product_[42-507130]', 'paysys_id':1601047, 'person_type':'sw_yt', 'services_products':['42-507130'], 'date': datetime.datetime.now()},
]

@pytest.mark.parametrize('casedata', params,
                         ids=lambda x: x['unique_name'])
def test_invoice_page_ci(casedata):
    prepare_invoice(**casedata)

