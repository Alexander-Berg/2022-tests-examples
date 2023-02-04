# -*- coding: utf-8 -*-

from copy import deepcopy
from decimal import Decimal as D, ROUND_HALF_UP

import pytest
from datetime import datetime
from dateutil.relativedelta import relativedelta
import balance.balance_db as db
from balance.balance_steps import new_taxi_steps as tsteps
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.matchers import contains_dicts_with_entries, equal_to, has_entries
from hamcrest import empty

from balance import balance_api as api


# client_id = api.medium().CreateClient(16571028, {'CITY': u'Батт', 'EMAIL': 'client@in-fo.ru', 'FAX': '912',
#                                      'NAME': 'balance_test 2019-07-08 17:40:08.515000',
#                                      'PHONE': '911', 'URL': 'http://client.info/'})[2]

# one_star_params = {'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
#                    'email': 'Mj^q@vfOx.YFB',
#                    'phone': '+7 812 3017123',
#                    'postaddress': 'а/я Кладр 0',
#                    'legaladdress': 'Avenue 5',
#                    'inn': '7822267391',
#                    'kpp': '912788793',
#                    'longname': '000 UQg',
#                    'person_id': 0,
#                    'client_id': client_id}
#
# two_star_params = {'account': '40702810713762296704',
#                    'bik': '044525440'}
#
# no_star_params = {'ogrn': '379956466494603',
#                                            's_signer-position-name': 'President',
#                                            'signer_person_gender': 'W',
#                                            'signer_person_name': 'Signer RR',
#                                            'representative': 'tPLLK',
#                                            'revise-act-period-type': '',
#                                            'live-signature': '1',
#                                            'invalid-address': '0',
#                                            'invalid-bankprops': '0',
#                                            'fax': '+7 812 5696286',
#                                            'delivery-city': 'NSK',
#                                            'delivery-type': '4',
#                                            'authority-doc-details': 'g',
#                                            'authority-doc-type': u'Свидетельство о регистрации'}
# no_partner = []
# partner = []
#
# no_partner.append(api.medium().CreatePerson(16571028, {'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
#                                                        'email': 'Mj^q@vfOx.YFB',
#                                                        'phone': '+7 812 3017123',
#                                                        'postaddress': 'а/я Кладр 0',
#                                                        'legaladdress': 'Avenue 5',
#                                                        'inn': '7822267391',
#                                                        'kpp': '912788793',
#                                                        'longname': '000 UQg',
#                                                        'person_id': 0,
#                                                        'client_id': client_id,
#                                                        'type': 'ur',
#                                                        'postcode': '123456',
#                                                     'city': 'asdf',
#                                                     'street': 'afhahs',
#                                                     'postsuffix': '123'}))
#
# no_partner.append(api.medium().CreatePerson(16571028, {'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
#                                                        'email': 'Mj^q@vfOx.YFB',
#                                                        'phone': '+7 812 3017123',
#                                                        'postaddress': 'а/я Кладр 0',
#                                                        'legaladdress': 'Avenue 5',
#                                                        'inn': '7822267391',
#                                                        'kpp': '912788793',
#                                                        'longname': '000 UQg',
#                                                        'person_id': 0,
#                                                        'client_id': client_id,
#                                                        'ogrn': '379956466494603',
#                                                        's_signer-position-name': 'President',
#                                                        'signer_person_gender': 'W',
#                                                        'signer_person_name': 'Signer RR',
#                                                        'representative': 'tPLLK',
#                                                        'revise-act-period-type': '',
#                                                        'live-signature': '1',
#                                                        'invalid-address': '0',
#                                                        'invalid-bankprops': '0',
#                                                        'fax': '+7 812 5696286',
#                                                        'delivery-city': 'NSK',
#                                                        'delivery-type': '4',
#                                                        'authority-doc-details': 'g',
#                                                        'authority-doc-type': u'Свидетельство о регистрации',
#                                                        'type': 'ur',
#                                                        'postcode': '123456',
#                                                     'city': 'asdf',
#                                                     'street': 'afhahs',
#                                                     'postsuffix': '123'
#                                                        }))
#
# no_partner.append(api.medium().CreatePerson(16571028, {'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
#                                                        'email': 'Mj^q@vfOx.YFB',
#                                                        'phone': '+7 812 3017123',
#                                                        'postaddress': 'а/я Кладр 0',
#                                                        'legaladdress': 'Avenue 5',
#                                                        'inn': '7822267391',
#                                                        'kpp': '912788793',
#                                                        'longname': '000 UQg',
#                                                        'person_id': 0,
#                                                        'client_id': client_id,
#                                                        # 'account': '40702810713762296704',
#                                                        'type': 'ur',
#                                                        'postcode': '123456',
#                                                     'city': 'asdf',
#                                                     'street': 'afhahs',
#                                                     'postsuffix': '123'}))
#
# no_partner.append(api.medium().CreatePerson(16571028, {'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
#                                                        'email': 'Mj^q@vfOx.YFB',
#                                                        'phone': '+7 812 3017123',
#                                                        'postaddress': 'а/я Кладр 0',
#                                                        'legaladdress': 'Avenue 5',
#                                                        'inn': '7822267391',
#                                                        'kpp': '912788793',
#                                                        'longname': '000 UQg',
#                                                        'person_id': 0,
#                                                        'client_id': client_id,
#                                                        'bik': '044525440',
#                                                        'type': 'ur',
#                                                        'postcode': '123456',
#                                                     'city': 'asdf',
#                                                     'street': 'afhahs',
#                                                     'postsuffix': '123'}))
#
# partner.append(api.medium().CreatePerson(16571028, {'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
#                                                        'email': 'Mj^q@vfOx.YFB',
#                                                        'phone': '+7 812 3017123',
#                                                        'postaddress': 'а/я Кладр 0',
#                                                        'legaladdress': 'Avenue 5',
#                                                        'inn': '7822267391',
#                                                        'kpp': '912788793',
#                                                        'longname': '000 UQg',
#                                                        'person_id': 0,
#                                                        'client_id': client_id,
#                                                        'bik': '044525440',
#                                                     'account': '40702810713762296704',
#                                                        'type': 'ur',
#                                                        'postcode': '123456',
#                                                     'city': 'asdf',
#                                                     'street': 'afhahs',
#                                                     'postsuffix': '123'}))
#
# partner.append(api.medium().CreatePerson(16571028, {'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
#                                                        'email': 'Mj^q@vfOx.YFB',
#                                                        'phone': '+7 812 3017123',
#                                                        'postaddress': 'а/я Кладр 0',
#                                                        'legaladdress': 'Avenue 5',
#                                                        'inn': '7822267391',
#                                                        'kpp': '912788793',
#                                                        'longname': '000 UQg',
#                                                        'person_id': 0,
#                                                        'client_id': client_id,
#                                                        'bik': '044525440',
#                                                     'account': '40702810713762296704',
#                                                     'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
#                                                     'email': 'Mj^q@vfOx.YFB',
#                                                     'phone': '+7 812 3017123',
#                                                     'postaddress': 'а/я Кладр 0',
#                                                     'legaladdress': 'Avenue 5',
#                                                     'inn': '7822267391',
#                                                     'kpp': '912788793',
#                                                     'longname': '000 UQg',
#                                                     'person_id': 0,
#                                                     'client_id': client_id,
#                                                     'ogrn': '379956466494603',
#                                                     's_signer-position-name': 'President',
#                                                     'signer_person_gender': 'W',
#                                                     'signer_person_name': 'Signer RR',
#                                                     'representative': 'tPLLK',
#                                                     'revise-act-period-type': '',
#                                                     'live-signature': '1',
#                                                     'invalid-address': '0',
#                                                     'invalid-bankprops': '0',
#                                                     'fax': '+7 812 5696286',
#                                                     'delivery-city': 'NSK',
#                                                     'delivery-type': '4',
#                                                     'authority-doc-details': 'g',
#                                                     'authority-doc-type': u'Свидетельство о регистрации',
#                                                        'type': 'ur',
#                                                        'postcode': '123456',
#                                                     'postaddress': 'Пермь, Екатерининская, 216-39',
#                                                     }))
# print  no_partner
#
# for pers in no_partner:
#     api.medium().GetPerson({'ID': pers}, 0)
#     api.medium().GetPerson({'ID': pers}, 1)
#
# for pers in partner:
#     api.medium().GetPerson({'ID': pers}, 0)
#     api.medium().GetPerson({'ID': pers}, 1)

# api.medium().GetClientPersons(108129192, 0)
# api.medium().GetClientPersons(client_id, 1)
# api.medium().GetClientPersons(client_id)
#
# print 'no partner ' + no_partner.__str__()
# print 'partner ' + partner.__str__()
# api.medium().GetPerson({'ID': 9203804}, 1)
# api.medium().GetClientPersons(108101459, 1)


# # IBAN
# {'BEN_BANK': '123',
#               'CITY': 'asdf',
#               'CLIENT_ID': '108129192',
#               'DT': '2019-07-10 12:29:35',
#               'EMAIL': 'asdf@adsfas.ru',
#               'FNAME': '1',
#               'IBAN': '123',
#               'ID': '9222867',
#               'LNAME': '1',
#               'NAME': '1 1',
#               'OV_PARAMS_SERVICES': [],
#               'PHONE': '123',
#               'POSTADDRESS': 'asdf',
#               'POSTCODE': '123345',
#               'REGION': '211',
#               'SWIFT': 'YNDMRUM1XXX',
#               'TYPE': 'sw_ytph'}
#
# # расчетный счет
# {'ACCOUNT': '40817810455000000131',
#               'CITY': u'Москва',
#               'CLIENT_ID': '108129192',
#               'DT': '2019-07-10 12:31:03',
#               'EMAIL': 'asdf@adsfas.ru',
#               'FNAME': 'ASD',
#               'ID': '9222893',
#               'LNAME': u'фыва',
#               'MNAME': 'SD',
#               'NAME': u'фыва ASD SD',
#               'OV_PARAMS_SERVICES': [],
#               'PHONE': '+71238902347',
#               'POSTADDRESS': 'asdf',
#               'POSTCODE': '123345',
#               'REGION': '29386',
#               'SWIFT': 'ABSRNOK1XXX',
#               'TYPE': 'sw_ytph'}
#
# # прочее
# {'CITY': 'asdf',
#  'CLIENT_ID': '108129192',
#  'DT': '2019-07-10 12:33:25',
#  'EMAIL': 'asdf@adsfas.ru',
#  'FNAME': 'ASD',
#  'ID': '9222894',
#  'LNAME': '123',
#  'NAME': '123 ASD',
#  'OTHER': '123',
#  'OV_PARAMS_SERVICES': [],
#  'PHONE': '+71238902347',
#  'POSTADDRESS': 'asdf',
#  'POSTCODE': '123345',
#  'REGION': '20826',
#  'SWIFT': 'YNDMRUM1XXX',
#  'TYPE': 'sw_ytph'}


def create_client():
    client_id = api.medium().CreateClient(16571028, {'CITY': u'Батт', 'EMAIL': 'client@in-fo.ru', 'FAX': '912',
                                                     'NAME': 'balance_test 2019-07-08 17:40:08.515000',
                                                     'PHONE': '911', 'URL': 'http://client.info/'})[2]
    return client_id


# def check_sw_ytph():
#     one_star_params = {'CITY': u'Москва',
#                        'POSTADDRESS': 'asdf',
#                        'POSTCODE': '123345',
#                        'BIRTHDAY': '1997-09-23',
#                        'BEN_ACCOUNT': 'woop'
#                        }
#
#     no_star_params = {'MNAME': 'SD',
#                       'CORR_SWIFT': 'YNDMRUM1XXX', }
#
#     iban_params = {'BEN_BANK': '123',
#                    'SWIFT': 'YNDMRUM1XXX',
#                    'IBAN': '123'}
#
#     account_params = {'ACCOUNT': '40817810455000000131',
#                       'SWIFT': 'YNDMRUM1XXX', }
#
#     other_params = {'OTHER': '123',
#                     'SWIFT': 'YNDMRUM1XXX'}
#
#     oblig_params = [one_star_params, iban_params, account_params, other_params]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'LNAME': u'фыва',
#                         'FNAME': u'фыва ASD SD',
#                         'PHONE': '+71238902347',
#                         'EMAIL': 'asdf@adsfas.ru',
#                         'REGION': '29386',
#                         'type': 'sw_ytph'}
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         if params_set == one_star_params:
#             for i in range(len(one_star_params)):
#                 person_params = two_stars_params.copy()
#                 one_star_params_copy = one_star_params.copy()
#                 del one_star_params_copy[one_star_params_copy.keys()[i]]
#                 person_params.update(one_star_params_copy)
#                 person_params.update(iban_params)
#                 person_params_list_no_partner.append(person_params)
#         else:
#             for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params.update(one_star_params)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options = [iban_params, account_params, other_params]
#     for params_set in oblig_params_options:
#         person_params = two_stars_params.copy()
#         person_params.update(one_star_params)
#         person_params.update(params_set)
#         person_params_list_partner.append(person_params)
#         person_params.update(no_star_params)
#         person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(sorted(all_pers), equal_to(sorted(person_list_res_getperson)),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(sorted(partner_pers), equal_to(sorted(partner_list)),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_ur():
#     one_star_params = {'ACCOUNT': '40817810455000000131',
#                        'BIK': '044030653'}
#
#     no_star_params = {'OGRN': '1023601070140',
#                       'SIGNER_POSITION_NAME': u'Директор',
#                       'AUTHORITY_DOC_DETAILS': u'ыва',
#                       'SIGNER_PERSON_GENDER': 'X',
#                       'SIGNER_PERSON_NAME': u'фывафывафыва',
#                       'ADDRESS': 'adfasdf',
#                       'DELIVERY_TYPE': '1',
#                       'REVISE_ACT_PERIOD_TYPE': '1',
#                       'KBK': '18210202140061110160',
#                       'OKTMO': '45301000',
#                       'PAYMENT_PURPOSE': '123',
#                       }
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'TYPE': 'ur',
#                         'NAME': u'ОАО "А-ХИМИЯ"',
#                         'EMAIL': 'client@in-fo.ru',
#                         'PHONE': '123456789',
#                         'POSTCODE': '123456',
#                         'POSTSUFFIX': '123',
#                         'LONGNAME': u'ОТКРЫТОЕ АКЦИОНЕРНОЕ ОБЩЕСТВО "ВЕРХНЕМАМОНАГРОХИМИЯ"',
#                         'INN': '3606001417',
#                         'KPP': '360601001',
#                         'LEGAL_ADDRESS_CITY': u'с Верхний Мамон',
#                         'LEGAL_ADDRESS_CODE': '360070000010039',
#                         'LEGAL_ADDRESS_HOME': u'д 21',
#                         'LEGAL_ADDRESS_POSTCODE': '123345',
#                         'LEGAL_ADDRESS_STREET': u'ул Строительная',
#                         'LEGAL_FIAS_GUID': '60e20755-4b7d-4935-a682-a1d911bbe40e',
#                         }
#     person_params_list_no_partner = [two_stars_params]
#
#     person_params = two_stars_params.copy()
#     # убираю только чет, т.к. есть проверка на счет без бика в создании плательщика
#     one_star_params_copy = {'BIK': '044030653'}
#     person_params.update(one_star_params_copy)
#     person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     person_params = two_stars_params.copy()
#     person_params.update(one_star_params)
#     person_params_list_partner.append(person_params)
#     person_params.update(no_star_params)
#     person_params_list_partner.append(person_params)
#
#     print person_params_list_no_partner
#     print person_params_list_partner
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_ph():
#     one_star_params = {
#                         'passport_s': '7777',
#                         'passport_n': '123456',
#                         'passport_e': u'фыва',
#                         'passport_code': '123-456',
#                         'passport_d': '1997-09-23',
#                         'birthday': '1997-09-23',
#                         'passport_birthplace': u'йцук',
#                         'inn': '3358359869',
#                         'pfr': '123456789',
#                         'bank_type': '1',
#                         'bank_inn': '3358359869',
#                        }
#
#     no_star_params = {'delivery_type': '1',
#                       'account': '40817810455000000131',
#                       'bik': '044030653',
#                       'mname': u'иваныч',
#                       'delivery_city': 'SPB',
#                       }
#
#     reg_address_no_dict = {'legaladdress': u'фывайцук',}
#
#     reg_address_dict = {'legal_address_city': u'г москва',
#                         'legal_address_code': '770000000007095',
#                         'legal_address_home': '123',
#                         'legal_address_postcode': '123456',
#                         'legal_address_street': u'льва толстого ул',}
#
#     delivery_addr_dict = {'city': u'г москва',
#                           'postcode': '123456',
#                           'postsuffix': '135',
#                           'street': u'льва толстого ул',}
#
#     delivery_addr_box = {'postcode': '123456',
#                           'postsuffix': '135',}
#
#     oblig_params = [one_star_params, reg_address_no_dict, reg_address_dict, delivery_addr_dict, delivery_addr_box]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'type': 'ph',
#                         'fname': u'иван',
#                         'lname': u'иванов',
#                         'email': 'asdf@adsfas.ru',
#                         'phone': '+7123',
#                         }
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         if params_set == one_star_params:
#             for i in range(len(one_star_params)):
#                 person_params = two_stars_params.copy()
#                 one_star_params_copy = one_star_params.copy()
#                 del one_star_params_copy[one_star_params_copy.keys()[i]]
#                 person_params.update(one_star_params_copy)
#                 person_params.update(reg_address_no_dict)
#                 person_params.update(delivery_addr_dict)
#                 person_params_list_no_partner.append(person_params)
#         elif (params_set == reg_address_no_dict or params_set == reg_address_dict):
#             for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params.update(one_star_params)
#                 person_params.update(delivery_addr_dict)
#                 person_params_list_no_partner.append(person_params)
#         else:
#             for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params.update(one_star_params)
#                 person_params.update(reg_address_no_dict)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options_1 = [reg_address_no_dict, reg_address_dict]
#     oblig_params_options_2 = [delivery_addr_dict, delivery_addr_box]
#     for params_set_1 in oblig_params_options_1:
#         for params_set_2 in oblig_params_options_2:
#             person_params = two_stars_params.copy()
#             person_params.update(one_star_params)
#             person_params.update(params_set_1)
#             person_params.update(params_set_2)
#             person_params_list_partner.append(person_params)
#             person_params.update(no_star_params)
#             person_params_list_partner.append(person_params)
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_yt():
#     no_star_params = {'INN': '3358359869',
#                       'CORR_SWIFT': 'YNDMRUM1XXX',
#                       'REPRESENTATIVE': u'фыва',
#                       'SIGNER_PERSON_NAME': u'фывафывафыва',
#                       'SIGNER_POSITION_NAME': 'General Director',
#                       'AUTHORITY_DOC_TYPE': u'Приказ',
#                       }
#
#     iban_params = {'BEN_BANK': '123',
#                    'SWIFT': 'YNDMRUM1XXX',
#                    'IBAN': '123'}
#
#     account_params = {'ACCOUNT': '40817810455000000131',
#                       'SWIFT': 'YNDMRUM1XXX', }
#
#     other_params = {'OTHER': '123',
#                     'SWIFT': 'YNDMRUM1XXX'}
#
#     oblig_params = [iban_params, account_params, other_params]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'NAME': 'ASD',
#                         'LONGNAME': u'фыва',
#                         'EMAIL': 'asdf@adsfas.ru',
#                         'PHONE': '+71238902347',
#                         'POSTCODE': '123456',
#                         'LEGAL_ADDRESS_POSTCODE': '123345',
#                         'POSTSUFFIX': u'фыва',
#                         'REGION': '10532',
#                         'LEGALADDRESS': 'asdf',
#                         'ADDRESS': u'фыва',
#                         'type': 'yt'}
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options = [iban_params, account_params, other_params]
#     for params_set in oblig_params_options:
#         person_params = two_stars_params.copy()
#         person_params.update(params_set)
#         person_params_list_partner.append(person_params)
#         person_params.update(no_star_params)
#         person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_eu_yt():
#     no_star_params = {'OWNERSHIP_TYPE': 'ORGANIZATION',
#                       'CORR_SWIFT': 'YNDMRUM1XXX',
#                       'FAX': '12345',
#                       'INN': '1234567890',
#                       'LOCAL_POSTADDRESS': 'asdfqwer',
#                       'LOCAL_REPRESENTATIVE': 'wwwwwwoop',
#                       'REPRESENTATIVE': 'wwwoop',
#                       }
#
#     iban_params = {'BEN_BANK': '123',
#                    'SWIFT': 'YNDMRUM1XXX',
#                    'IBAN': '123'}
#
#     account_params = {'ACCOUNT': '40817810455000000131',
#                       'SWIFT': 'YNDMRUM1XXX', }
#
#     other_params = {'OTHER': '123',
#                     'SWIFT': 'YNDMRUM1XXX'}
#
#     oblig_params = [iban_params, account_params, other_params]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'NAME': 'ASD',
#                         'LOCAL_NAME': u'ASD',
#                         'EMAIL': 'asdf@adsfas.ru',
#                         'PHONE': '+71238902347',
#                         'type': 'eu_yt',
#                         'POSTADDRESS': 'asdf',
#                         'POSTCODE': '123',
#                         'REGION': '211',}
#
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options = [iban_params, account_params, other_params]
#     for params_set in oblig_params_options:
#         person_params = two_stars_params.copy()
#         person_params.update(params_set)
#         person_params_list_partner.append(person_params)
#         if oblig_params_options == iban_params:
#             person_params.update({'LOCAL_BEN_BANK': 'qwertyuio'})
#         if oblig_params_options == other_params:
#             person_params.update({'LOCAL_OTHER': 'asdf'})
#         person_params.update(no_star_params)
#         person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_yt_kzu():
#     no_star_params = {'FAX': '123456',
#                       'AUTHORITY_DOC_DETAILS': 'asdf',
#                       'AUTHORITY_DOC_TYPE': u'Протокол',
#               'CORR_SWIFT': 'swift123',
#               'BANK': 'bank',
#               'DELIVERY_TYPE': '2',
#               'SIGNER_PERSON_GENDER': 'M',
#               'SIGNER_PERSON_NAME': 'asdf',
#               'SIGNER_POSITION_NAME': 'Financial Director',
#                       }
#
#     oblig_params = []
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'NAME': 'ASD',
#                         'EMAIL': 'asdf@adsfas.ru',
#                         'PHONE': '+71238902347',
#                         'type': 'yt_kzu',
#               'POSTADDRESS': 'asdfg',
#               'POSTCODE': '1234',
#               'CITY': 'asdf',
#               'LEGALADDRESS': 'asdfgh',
#               'KZ_IN': '123456789012',
#               'BIK': 'ASDFASDF',
#               'iban': '12345678901234567890',
#                         }
#
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options = []
#     for params_set in oblig_params_options:
#         person_params = two_stars_params.copy()
#         person_params.update(params_set)
#         person_params_list_partner.append(person_params)
#         person_params.update(no_star_params)
#         person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_byu():
#     one_star_params = {'BEN_BANK': 'asdf',
#                        'SWIFT': 'ABSRNOK1XXX',
#                        'ACCOUNT': '123',}
#
#     no_star_params = {'FAX': '1234',
#                       'REPRESENTATIVE': 'asdf',
#                       'AUTHORITY_DOC_DETAILS': 'asdf',
#                       'AUTHORITY_DOC_TYPE': u'Договор',
#                       'LIVE_SIGNATURE': '1',
#                       'REGION': '51',
#                       'SIGNER_PERSON_GENDER': 'W',
#                       'SIGNER_PERSON_NAME': 'asdf',
#                       'SIGNER_POSITION_NAME': 'General Director',
#                       'SWIFT': 'ABSRNOK1XXX',
#                       }
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'TYPE': 'byu',
#                         'NAME': u'фыва',
#                         'PHONE': '123',
#                         'EMAIL': 'asd@asd.ru','POSTADDRESS': 'asdf',
#               'POSTCODE': '123456',
#                         'CITY': 'asdf',
#                         'INN': '123456789',
#                         'LONGNAME': 'adsf',
#                         'LEGALADDRESS': 'asdf',
#                         }
#     person_params_list_no_partner = [two_stars_params]
#
#     for i in range(len(one_star_params)):
#         person_params = two_stars_params.copy()
#         params_set_copy = one_star_params.copy()
#         del params_set_copy[params_set_copy.keys()[i]]
#         person_params.update(params_set_copy)
#         person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     person_params = two_stars_params.copy()
#     person_params.update(one_star_params)
#     person_params_list_partner.append(person_params)
#     person_params.update(no_star_params)
#     person_params_list_partner.append(person_params)
#
#     print person_params_list_no_partner
#     print person_params_list_partner
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_kzu():
#     one_star_params = {'BIK': 'BIKBIKBI',
#                        'IBAN': 'KZ123456789012345678',}
#
#     no_star_params = {'LOCAL_NAME': 'local full name',
#                       'FAX': '+7456',
#                       'RNN': '123456789012',
#                       'CORR_SWIFT': 'swiftswi',
#                       'LOCAL_BANK': 'local bank name',
#                       'BANK': 'bank name',
#                       'AUTHORITY_DOC_DETAILS': 'details',
#                       'AUTHORITY_DOC_TYPE': u'Приказ',
#                       'LOCAL_AUTHORITY_DOC_DETAILS': 'local details',
#                       'LOCAL_BANK': 'local bank name',
#                       'LOCAL_CITY': 'local city',
#                       'LOCAL_LEGALADDRESS': 'local legal address',
#                       'LOCAL_NAME': 'local full name',
#                       'LOCAL_POSTADDRESS': 'local post address',
#                       'LOCAL_SIGNER_PERSON_NAME': 'local repr',
#                       'LOCAL_SIGNER_POSITION_NAME': 'local pos',
#                       'SIGNER_PERSON_GENDER': 'W',
#                       'SIGNER_PERSON_NAME': 'repr',
#                       'SIGNER_POSITION_NAME': u'Управляющий',
#                       }
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'TYPE': 'kzu',
#                         'NAME': 'full name',
#                         'PHONE': '+7123',
#                         'EMAIL': 'asdf@asdf.ru',
#                         'POSTCODE': '123456',
#                         'CITY': 'city',
#                         'POSTADDRESS': 'post address',
#                         'KBE': '17',
#                         'KZ_IN': '098765432109',
#                         'KZ_KBE': '17',
#                         'LEGALADDRESS': 'legal address',
#                         }
#     person_params_list_no_partner = [two_stars_params]
#
#     for i in range(len(one_star_params)):
#         person_params = two_stars_params.copy()
#         params_set_copy = one_star_params.copy()
#         del params_set_copy[params_set_copy.keys()[i]]
#         person_params.update(params_set_copy)
#         person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     person_params = two_stars_params.copy()
#     person_params.update(one_star_params)
#     person_params_list_partner.append(person_params)
#     person_params.update(no_star_params)
#     person_params_list_partner.append(person_params)
#
#     print person_params_list_no_partner
#     print person_params_list_partner
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
#
# def check_sw_ur():
#     no_star_params = {
#               'LEGALADDRESS': 'legal address',
#               'REPRESENTATIVE': 'repr',
#               'SIGNER_PERSON_NAME': 'signer name',
#               'SIGNER_POSITION_NAME': 'Financial Director',
#         'AUTHORITY_DOC_TYPE': u'Доверенность',
#                       }
#
#     iban_params = {'BEN_BANK': '123',
#                    'SWIFT': 'YNDMRUM1XXX',
#                    'IBAN': '123'}
#
#     account_params = {'ACCOUNT': '40817810455000000131',
#                       'SWIFT': 'YNDMRUM1XXX', }
#
#     other_params = {'OTHER': '123',
#                     'SWIFT': 'YNDMRUM1XXX'}
#
#     oblig_params = [iban_params, account_params, other_params]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'type': 'sw_ur',
#               'LONGNAME': 'full name',
#               'NAME': 'name',
#               'INN': '1234567890',
#               'EMAIL': 'asdf@asdf.ru',
#               'PHONE': '+7123',
#               'REGION': '51',
#               'CITY': 'city',
#               'POSTADDRESS': 'post address',
#               'POSTCODE': 'post code',
#                         }
#
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options = [iban_params, account_params, other_params]
#     for params_set in oblig_params_options:
#         person_params = two_stars_params.copy()
#         person_params.update(params_set)
#         person_params_list_partner.append(person_params)
#         person_params.update(no_star_params)
#         person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_am_jp():
#     one_star_params = {'BEN_BANK': 'asdf',
#                        'SWIFT': 'ABSRNOK1XXX',
#                        'ACCOUNT': '123',}
#
#     no_star_params = {
#               'LOCAL_NAME': 'local short name',
#               'LOCAL_CITY': 'local city',
#               'LOCAL_POSTADDRESS': 'local post address',
#               'LOCAL_LONGNAME': 'local full name',
#               'JPC': 'legal code',
#               'RN': 'regnum',
#               'LOCAL_LEGALADDRESS': 'local legal address',
#               'LOCAL_BEN_BANK': 'local bank name',
#               'REPRESENTATIVE': 'contact',
#               'LOCAL_REPRESENTATIVE': 'local contract',
#               'LOCAL_SIGNER_PERSON_NAME': 'local repr',
#               'LOCAL_SIGNER_POSITION_NAME': 'local position',
#               'AUTHORITY_DOC_DETAILS': 'details',
#               'AUTHORITY_DOC_TYPE': u'Доверенность',
#               'DELIVERY_TYPE': '2',
#               'LOCAL_AUTHORITY_DOC_DETAILS': 'local details',
#               'SIGNER_PERSON_GENDER': 'M',
#               'SIGNER_PERSON_NAME': 'repr',
#               'SIGNER_POSITION_NAME': 'position',
#                       }
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'TYPE': 'am_jp',
#                         'NAME': 'short name',
#                         'PHONE': '123',
#                         'EMAIL': 'asd@asd.ru',
#                         'REGION': '10740',
#                         'POSTCODE': '12345',
#                         'CITY': 'city',
#                         'POSTADDRESS': 'post address',
#                         'LONGNAME': 'full name',
#                         'INN': '12345667',
#                         'LEGALADDRESS': 'legal address',
#                         }
#     person_params_list_no_partner = [two_stars_params]
#
#     for i in range(len(one_star_params)):
#         person_params = two_stars_params.copy()
#         params_set_copy = one_star_params.copy()
#         del params_set_copy[params_set_copy.keys()[i]]
#         person_params.update(params_set_copy)
#         person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     person_params = two_stars_params.copy()
#     person_params.update(one_star_params)
#     person_params_list_partner.append(person_params)
#     person_params.update(no_star_params)
#     person_params_list_partner.append(person_params)
#
#     print person_params_list_no_partner
#     print person_params_list_partner
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_hk_ur():
#     no_star_params = {'OWNERSHIP_TYPE': 'ORGANIZATION',
#                       'CORR_SWIFT': 'YNDMRUM1XXX',
#                       'LOCAL_POSTADDRESS': 'asdfqwer',
#                       'LOCAL_REPRESENTATIVE': 'wwwwwwoop',
#                       'REPRESENTATIVE': 'wwwoop',
#                       'LOCAL_CITY': 'local city',
#                       'LOCAL_LEGALADDRESS': 'local legal address',
#                       'LOCAL_LONGNAME': 'local long name',
#                       'LOCAL_NAME': 'local short name',
#                       'LOCAL_POSTADDRESS': 'local post address',
#               'SIGNER_PERSON_GENDER': 'X',
#                       }
#
#
#     iban_params = {'BEN_BANK': '123',
#                    'SWIFT': 'YNDMRUM1XXX',
#                    'IBAN': '123'}
#
#     account_params = {'ACCOUNT': '40817810455000000131',
#                       'SWIFT': 'YNDMRUM1XXX', }
#
#     other_params = {'OTHER': '123',
#                     'SWIFT': 'YNDMRUM1XXX'}
#
#     webmoney_params = { 'WEBMONEY_WALLET': 'webmoney',
#                         'BANK_TYPE': '4',}
#     paypal_params = {
#               'PAYPAL_WALLET': 'paypal',
#                         'BANK_TYPE': '5',}
#
#     payoneer_params = {
#               'PAYONEER_WALLET': '1234567890',
#                         'BANK_TYPE': '7',}
#
#     pingpong_params = {
#               'PINGPONG_WALLET': 'pingpong',
#                         'BANK_TYPE': '8',}
#
#     bank_params = [iban_params, account_params, other_params]
#     one_star_params = [webmoney_params, paypal_params, payoneer_params, pingpong_params, bank_params]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'NAME': 'ASD',
#                         'EMAIL': 'asdf@adsfas.ru',
#                         'PHONE': '+71238902347',
#                         'type': 'hk_ur',
#                         'POSTADDRESS': 'asdf',
#                         'POSTCODE': '123',
#                         'REGION': '211',
#               'INN': 'asdf',
#               'CITY': 'asdf',
#               'LONGNAME': 'asdf',
#               'LEGALADDRESS': 'adf',}
#
#     person_params_list_no_partner = [two_stars_params]
#     for params in one_star_params:
#         if params == bank_params:
#             for params_set in params:
#                 for i in range(len(params_set)):
#                         person_params = two_stars_params.copy()
#                         params_set_copy = params_set.copy()
#                         del params_set_copy[params_set_copy.keys()[i]]
#                         person_params.update(params_set_copy)
#                         person_params_list_no_partner.append(person_params)
#         else:
#             person_params = two_stars_params.copy()
#             person_params.update({'BANK_TYPE': params['BANK_TYPE']})
#             person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     bank_params_options = [iban_params, account_params, other_params]
#     oblig_params_options = [webmoney_params, paypal_params, payoneer_params, pingpong_params, bank_params_options]
#
#     for params in oblig_params_options:
#         if params == bank_params_options:
#             for params_set in params:
#                 person_params = two_stars_params.copy()
#                 person_params.update(params_set)
#                 person_params_list_partner.append(person_params)
#                 if params_set == iban_params:
#                     person_params.update({'LOCAL_BEN_BANK': 'qwertyuio'})
#                 if params_set == other_params:
#                     person_params.update({'LOCAL_OTHER': 'asdf'})
#                 person_params.update(no_star_params)
#                 person_params_list_partner.append(person_params)
#         else:
#             person_params = two_stars_params.copy()
#             person_params.update(params)
#             person_params_list_partner.append(person_params)
#             person_params.update(no_star_params)
#             person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_az_ur():
#     no_star_params = {'OWNERSHIP_TYPE': 'ORGANIZATION',
#                       'CORR_SWIFT': 'YNDMRUM1XXX',
#                       'LOCAL_POSTADDRESS': 'asdfqwer',
#                       'LOCAL_REPRESENTATIVE': 'wwwwwwoop',
#                       'REPRESENTATIVE': 'wwwoop',
#                       'LOCAL_CITY': 'local city',
#                       'LOCAL_LEGALADDRESS': 'local legal address',
#                       'LOCAL_LONGNAME': 'local long name',
#                       'LOCAL_NAME': 'local short name',
#                       'LOCAL_POSTADDRESS': 'local post address',
#               'SIGNER_PERSON_GENDER': 'X',
#                       }
#
#     iban_params = {'BEN_BANK': '123',
#                    'SWIFT': 'YNDMRUM1XXX',
#                    'IBAN': '123'}
#
#     account_params = {'ACCOUNT': '40817810455000000131',
#                       'SWIFT': 'YNDMRUM1XXX', }
#
#     other_params = {'OTHER': '123',
#                     'SWIFT': 'YNDMRUM1XXX'}
#
#     oblig_params = [iban_params, account_params, other_params]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'type': 'az_ur',
#                         'NAME': 'ASD',
#                         'EMAIL': 'asdf@adsfas.ru',
#                         'PHONE': '+71238902347',
#                         'POSTADDRESS': 'asdf',
#                         'POSTCODE': '123',
#                         'REGION': '211',
#               'INN': 'asdf',
#               'CITY': 'asdf',
#               'LONGNAME': 'asdf',
#               'LEGALADDRESS': 'adf',}
#
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options = [iban_params, account_params, other_params]
#     for params_set in oblig_params_options:
#         person_params = two_stars_params.copy()
#         person_params.update(params_set)
#         person_params_list_partner.append(person_params)
#         person_params.update(no_star_params)
#         person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
# def check_il_ur():
#     no_star_params = {'OWNERSHIP_TYPE': 'ORGANIZATION',
#                       'CORR_SWIFT': 'YNDMRUM1XXX',
#                       'LOCAL_POSTADDRESS': 'asdfqwer',
#                       'LOCAL_REPRESENTATIVE': 'wwwwwwoop',
#                       'REPRESENTATIVE': 'wwwoop',
#                       'LOCAL_CITY': 'local city',
#                       'LOCAL_LEGALADDRESS': 'local legal address',
#                       'LOCAL_LONGNAME': 'local long name',
#                       'LOCAL_NAME': 'local short name',
#                       'LOCAL_POSTADDRESS': 'local post address',
#               'SIGNER_PERSON_GENDER': 'X',
#                       }
#
#     iban_params = {'BEN_BANK': '123',
#                    'SWIFT': 'YNDMRUM1XXX',
#                    'IBAN': '123'}
#
#     account_params = {'ACCOUNT': '40817810455000000131',
#                       'SWIFT': 'YNDMRUM1XXX', }
#
#     other_params = {'OTHER': '123',
#                     'SWIFT': 'YNDMRUM1XXX'}
#
#     oblig_params = [iban_params, account_params, other_params]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'type': 'il_ur',
#                         'NAME': 'ASD',
#                         'EMAIL': 'asdf@adsfas.ru',
#                         'PHONE': '+71238902347',
#                         'POSTADDRESS': 'asdf',
#                         'POSTCODE': '123',
#                         'REGION': '211',
#               'INN': 'asdf',
#               'CITY': 'asdf',
#               'LONGNAME': 'asdf',
#               'LEGALADDRESS': 'adf',
#                         'IL_ID': 'adsf',}
#
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options = [iban_params, account_params, other_params]
#     for params_set in oblig_params_options:
#         person_params = two_stars_params.copy()
#         person_params.update(params_set)
#         person_params_list_partner.append(person_params)
#         person_params.update(no_star_params)
#         person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
#
# def check_sw_yt():
#     no_star_params = {'OWNERSHIP_TYPE': 'ORGANIZATION',
#                       'CORR_SWIFT': 'YNDMRUM1XXX',
#                       'LOCAL_POSTADDRESS': 'asdfqwer',
#                       'LOCAL_REPRESENTATIVE': 'wwwwwwoop',
#                       'REPRESENTATIVE': 'wwwoop',
#                       'LOCAL_CITY': 'local city',
#                       'LOCAL_LEGALADDRESS': 'local legal address',
#                       'LOCAL_LONGNAME': 'local long name',
#                       'LOCAL_NAME': 'local short name',
#                       'LOCAL_POSTADDRESS': 'local post address',
#               'SIGNER_PERSON_GENDER': 'X',
#               'INN': 'asdf',
#                       }
#
#     iban_params = {'BEN_BANK': '123',
#                    'SWIFT': 'YNDMRUM1XXX',
#                    'IBAN': '123'}
#
#     account_params = {'ACCOUNT': '40817810455000000131',
#                       'SWIFT': 'YNDMRUM1XXX', }
#
#     other_params = {'OTHER': '123',
#                     'SWIFT': 'YNDMRUM1XXX'}
#
#     oblig_params = [iban_params, account_params, other_params]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'type': 'sw_yt',
#                         'NAME': 'ASD',
#                         'EMAIL': 'asdf@adsfas.ru',
#                         'PHONE': '+71238902347',
#                         'POSTADDRESS': 'asdf',
#                         'POSTCODE': '123',
#                         'REGION': '211',
#               'CITY': 'asdf',
#               'LONGNAME': 'asdf',
#               'LEGALADDRESS': 'adf',}
#
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options = [iban_params, account_params, other_params]
#     for params_set in oblig_params_options:
#         person_params = two_stars_params.copy()
#         person_params.update(params_set)
#         person_params_list_partner.append(person_params)
#         person_params.update(no_star_params)
#         person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')
#
# def check_ytph():
#     no_star_params = {
#                       'CORR_SWIFT': 'YNDMRUM1XXX',
#               'SIGNER_PERSON_GENDER': 'X',
#               'INN': 'asdf',
#         'mname': 'wer',
#                         'birthday': '1997-09-23',
#
#                       }
#
#     iban_params = {'BEN_BANK': '123',
#                    'SWIFT': 'YNDMRUM1XXX',
#                    'IBAN': '123',
#                    'ben_account': '123',
#                    'LEGALADDRESS': 'adf'}
#
#     account_params = {'ACCOUNT': '40817810455000000131',
#                       'SWIFT': 'YNDMRUM1XXX',
#                       'ben_account': '123',
#                       'LEGALADDRESS': 'adf'}
#
#     other_params = {'OTHER': '123',
#                     'SWIFT': 'YNDMRUM1XXX',
#                     'ben_account': '123',
#                     'LEGALADDRESS': 'adf'}
#
#     oblig_params = [iban_params, account_params, other_params]
#
#     client_id = create_client()
#
#     two_stars_params = {'client_id': client_id,
#                         'type': 'ytph',
#                         'fname': u'иван',
#                         'lname': u'иванов',
#                         'email': 'asdf@adsfas.ru',
#                         'phone': '+7123',
#                         'POSTADDRESS': 'asdf',
#                         'POSTCODE': '123',
#                         'REGION': '211',
#                         'CITY': 'asdf',
#                         }
#
#     person_params_list_no_partner = [two_stars_params]
#     for params_set in oblig_params:
#         for i in range(len(params_set)):
#                 person_params = two_stars_params.copy()
#                 params_set_copy = params_set.copy()
#                 del params_set_copy[params_set_copy.keys()[i]]
#                 person_params.update(params_set_copy)
#                 person_params_list_no_partner.append(person_params)
#
#     person_params_list_partner = []
#     oblig_params_options = [iban_params, account_params, other_params]
#     for params_set in oblig_params_options:
#         person_params = two_stars_params.copy()
#         person_params.update(params_set)
#         person_params_list_partner.append(person_params)
#         person_params.update(no_star_params)
#         person_params_list_partner.append(person_params)
#
#
#     person_list_res_getperson = []
#     partner_list_res_getperson_fail = []
#     no_partner_list = []
#     for params_set in person_params_list_no_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         no_partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res != []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     partner_list = []
#     for params_set in person_params_list_partner:
#         person_id = api.medium().CreatePerson(16571028, params_set)
#         partner_list.append(person_id)
#         res = api.medium().GetPerson({'ID': person_id}, 1)
#         person_list_res_getperson.append(api.medium().GetPerson({'ID': person_id}, 0)[0]['id'])
#         if res == []:
#             partner_list_res_getperson_fail.append(person_id)
#
#     all_pers = [x['ID'] for x in api.medium().GetClientPersons(client_id, 0)]
#     partner_pers = [int(x['ID']) for x in api.medium().GetClientPersons(client_id, 1)]
#
#     utils.check_that(all_pers, equal_to(person_list_res_getperson),
#                      step=u'Сравним список всех плательщиков с ожидаемым')
#     utils.check_that(partner_pers, equal_to(partner_list),
#                      step=u'Сравним список партнеров с ожидаемым')
#     utils.check_that(partner_list_res_getperson_fail, empty(),
#                      step=u'Проверим, что все партнеры считаются партнерами')


# check_sw_yt()
# api.medium().GetClientPersons(109448742, 0)
# from btestlib.data.partner_contexts import AVIA_SW_CONTEXT
#
# _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(AVIA_SW_CONTEXT, client_id=108937602, person_id=9240969,
#                                                                                        additional_params={
#                                                                                            'start_dt': datetime.today()})


# _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
#     CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED, client_id=108916990, person_id=9226555)


# check_ytph()

# from btestlib.constants import *
# # client_id = 109421257
# # person_id = 71209063
# from btestlib.data.partner_contexts import RED_MARKET_620_SW_CONTEXT, RED_MARKET_SW_SPENDABLE_CONTEXT
# steps.ContractSteps.create_partner_contract(RED_MARKET_620_SW_CONTEXT, client_id=109449252, person_id=71226013)
# steps.ContractSteps.create_partner_contract(RED_MARKET_SW_SPENDABLE_CONTEXT, client_id=109449252, person_id=71226013)
# # #

# api.medium().GetClientPersons(109449409, 1)
# check_yt_kzu()

from btestlib.constants import *

client_id = 112006549

# params = {'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
#           'email': 'Mj^q@vfOx.YFB',
#           'phone': '+7 812 3017123',
#           'postcode': '123123',
#           'postaddress': 'а/я Кладр 0',
#           # 'legal_address_postcode': '123123',
#           'legaladdress': '119021, Москва, ул. Льва Толстого, 16',
#           'inn': '7822267391',
#           'kpp': '912788793',
#           'longname': '000 UQg',
#           'person_id': 0,
#           'client_id': client_id,
#           'bik': '044525440',
#           'account': '40702810713762296704',
#           'country-id': '225',
#           'type': 'ur'}
params = {'name': u'Юр. лицо РСЯRDWF АО «Терентьева Власов»',
          'email': 'Mj^q@vfOx.YFB',
          'phone': '+7 812 3017123',
          'postcode': '123123',
          'postaddress': 'а/я Кладр 0',
          'legal_address_postcode': '123123',
          'legaladdress': 'Москва, улица какая-то, 5',
          'inn': '7822267391',
          'kpp': '912788793',
          'longname': '000 UQg',
          'person_id': 0,
          'client_id': 112134881,
          # 'bik': '044525440',
          # 'account': '40702810713762296704',
          'country-id': '225',
          'type': 'ur'}

api.medium().CreatePerson(16571028, params)

# person_id = steps.PersonSteps.create(create_client(), PersonTypes.PH.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.UR.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.SW_YTPH.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.SW_YT.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.AZ_UR.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.IL_UR.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.HK_YT.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.HK_UR.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.AM_UR.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.AM_PH.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.YT_KZU.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.YT.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.PH.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.YTPH.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.SW_UR.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.EU_YT.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.BYU.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.KZU.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.BY_YTPH.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.BYP.code, full=True)
# # person_id = steps.PersonSteps.create(create_client(), PersonTypes.EU_UR.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.KZP.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.YT_KZP.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.SW_PH.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.TRP.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.TRU.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.USU.code, full=True)
# person_id = steps.PersonSteps.create(create_client(), PersonTypes.USP.code, full=True)
