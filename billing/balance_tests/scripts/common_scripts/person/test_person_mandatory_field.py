# coding=utf-8
import pytest
import copy
import hamcrest

from btestlib import utils
from xmlrpclib import Fault
from balance import balance_steps as steps
from btestlib.constants import PersonTypes
from btestlib.data.person_defaults import Details, get_details

mandatory_fields_map = {
    PersonTypes.UR: {
        "postaddress": u"а/я Кладр 0",
        "email": "m-SC@qCWF.rKU",
        "longname": u"000 WBXG",
        "legaladdress": "Avenue 5",
        "inn": "7879679858",
        "postcode": "191025",
        "kpp": "767726208",
        "phone": "+7 812 3990776",
        "name": u"Юр. лицо или ПБОЮЛqdZd"
    },
    PersonTypes.PH: {
        "fname": "Test1",  # Физ.лицо РФ
        "lname": "Test2",
        "mname": "Test3",
        "phone": "+7 905 1234567",
        "email": "testagpi2@yandex.ru",
    },
    PersonTypes.YT: {
        "email": "xTW9@fOJz.XcJ",
        "phone": "+7 812 5389048",
        "address": u"Улица 4",
        "name": "YT_MTL",
    },
    PersonTypes.YTPH: {
        "fname": "Test1",  # Физ.лицо-нерезидент РФ
        "lname": "Test2",
        "phone": "+7 905 1234567",
        "email": "testagpi2@yandex.ru"
    },
    PersonTypes.YT_KZU: {
        "name": "Python yt_kzu",  # нерезиденты казахстан ЮЛ
        "iik": "KZ838560000000463517",
        "bik": "CASPKZKA",
        "phone": "+7 905 1234567",
        "email": "test-balance-notify@yandex-team.ru",
        "city": "citygKZ",
        "postaddress": "Py_Street 5",
        "kz-in": "892849352919",
    },
    PersonTypes.YT_KZP: {
        "lname": u"Жапбасбай",  # нерезиденты казахстан ФЛ
        "fname": u"Бекет",
        "email": "usp@email.com",
        "phone": "+1 650 5173548",
        "postaddress": "Py_Street 5",
        "city": "py_city",
        "kz-in": "892849352919",
    },
    PersonTypes.KZP: {
        "lname": u"Жапбасбай",
        "fname": u"Бекет",
        "email": "usp@email.com",
        "phone": "+1 650 5173548",
        "postaddress": "Py_Street 5",
        "city": "py_city",
        "kz-in": "892849352919",
    },
    PersonTypes.KZU: {
        "name": "KZ firm",
        "phone": "+7 727 1732223",
        "email": "usp@email.kz",
        "city": "py_city",
        "postaddress": "Py_Street 5",
        "bik": "CASPKZKA",
        "iik": "KZ838560000000463517",
        "kz-in": "892849352919",
        "kbe": '18',
    },
    PersonTypes.USP: {
        "lname": "MTL Doh",
        "fname": "MTL John",
        "email": "usp@email.com",
        "phone": "+1 650 5173548",
        "postaddress": "Py_Street 5",
        "city": "py_city",
        "postcode": "98411",
        "us-state": "CA",
    },
    PersonTypes.USU: {
        "name": "MTL John",
        "phone": "+1 650 5173548",
        "email": "usp@email.com",
        "postaddress": "Py_Street 5",
        "city": "py_city",
        "us-state": "CA",
        "postcode": "98411"
    },
    PersonTypes.BYP: {
        "fname": "Test1",
        "lname": "Test2",
        "mname": "Test3",
        "phone": "+1 650 5173548",
        "email": "usp@email.com",
        "organization": "Yandex",
        'postcode': "323",
        'city': 'vvv',
        'postaddress': '12323'
    },
    PersonTypes.BYU: {
        "name": "LTD, Belarus",
        "longname": "Long LTD BELARUS",
        "city": "city Kdt",
        "email": "E'Na@EsKd.FxY",
        "phone": "+41 32 5617074",
        "inn": "339293",
        "postcode": "81070",
        "legaladdress": "Avenue 4",
        "postaddress": "Street 4",
    },
    PersonTypes.BY_YTPH: {
        "fname": "Test1",
        "lname": "Test2",
        "phone": "+375 29 0601336",
        "email": "E'Na@EsKd.FxY",
        "city": "city Kdt",
        "postaddress": "Py_Street 5",
    },
    PersonTypes.EU_YT: {
        "email": "0^$-@rNcG.QdG",
        "phone": "+41 32 2304106",
        "postcode": "43857",
        "postaddress": u"Улица 5",
        "name": u"Test BV !",
    },
    PersonTypes.SW_PH: {
        "fname": "MTL_sw_ph",
        "email": "P!iY@DMII.umZ",
        "lname": "Individual, SwitzerlandPide",
        "phone": "+41 32 1020210",
    },
    PersonTypes.SW_YTPH: {
        "fname": "MTL_sw_ytph",
        "email": "Hc`F@lmsS.Sta",
        "lname": u"Физ. лицо-нерезидент, Швейцарияzbey",
        "phone": "+41 32 9126742",
    },
    PersonTypes.SW_UR: {
        "email": "E'Na@EsKd.FxY",
        "phone": "+41 32 5617074",
        "postcode": "81070",
        "postaddress": "Street 4",
        "name": "Legal entity, SwitzerlandRpo",
    },
    PersonTypes.SW_YT: {
        "email": "0^$-@rNcG.QdG",
        "phone": "+41 32 2304106",
        "postcode": "43857",
        "postaddress": u"Улица 5",
        "name": u"Нерезидент, ШвейцарияAMY"
    },
    PersonTypes.TRP: {
        "fname": "Test1",  # ФЛ Турция
        "lname": "Test2",
        "email": "testagpi2@yandex.ru",
        "phone": "+90 537 565810",
    },
    PersonTypes.TRU: {
        "name": "LTD TurName",  # ЮЛ Турция
        "inn": "99999",
        "email": "testagpi2@yandex.ru",
        "phone": "+90 537 851753",
        "postcode": "12345",
        "postaddress": "Tur addr",
    },
    PersonTypes.AM_UR: {
        "name": "am_jp_name",  # ЮЛ Армения
        "postaddress": "ARM addr",
        "longname": "am_jp_name_longname",
        "legaladdress": "am_jp_name_legaladdress",
        "phone": "+90 537 851753",
        "inn": "999854459",
        "account": "223",
        "ben-bank": 'bank_name_am',
        "swift": "2o76hkVS",
        "email": "testagpi2@yandex.ru",
        "postcode": "1235",
    },
    PersonTypes.AM_PH: {
        "fname": 'AM_fname',  # ФЛ Армения
        "lname": 'AM_lname',
        "phone": '34344',
        'email': 'reerg@erg.rr',
        "postcode": '5677',
        "city": 'Erevan',
        "postaddress": 'AM_postadress'
    },
    PersonTypes.SK_UR: get_details('sk_ur'),
}


@pytest.mark.parametrize('person_type', mandatory_fields_map.keys())
def test_create_person_mandatory_field(person_type):
    client_id = steps.ClientSteps.create()
    person_params = mandatory_fields_map[person_type]
    steps.PersonSteps.create(client_id, person_type.code,
                             params=person_params,
                             strict_params=True)
    for field in person_params.keys():
        if field in ['client_id', 'type']:
            continue
        person_params_copy = copy.deepcopy(person_params)
        del person_params_copy[field]
        with pytest.raises(Fault) as exc_info:
            steps.PersonSteps.create(client_id, person_type.code,
                                     params=person_params_copy,
                                     strict_params=True)
        error_msg = 'Missing mandatory person field \'{0}\' for person type {1}'.format(field, person_type.code)
        utils.check_that(hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc_info.value, 'contents')), error_msg)


@pytest.mark.parametrize('person_type, person_subtype, base_params, extra_params',
                         [(PersonTypes.UR, 'ur-kladr', {'postsuffix': '223', 'kladr-code': '7000000100000'},
                           {"email": "m-SC@qCWF.rKU",
                            "longname": u"000 WBXG",
                            "legaladdress": "Avenue 5",
                            "inn": "7879679858",
                            "postcode": "191025",
                            "phone": "+7 812 3990776",
                            "name": u"Юр. лицо или ПБОЮЛqdZd"}),
                          (PersonTypes.UR, 'ur-postbox', {'postsuffix': '223'},
                           {"email": "m-SC@qCWF.rKU",
                            "longname": u"000 WBXG",
                            "legaladdress": "Avenue 5",
                            "inn": "7879679858",
                            "postcode": "191025",
                            "phone": "+7 812 3990776",
                            "name": u"Юр. лицо или ПБОЮЛqdZd"}),
                          (PersonTypes.UR, 'ur', {'kladr-code': '7000000100000'},
                          mandatory_fields_map[PersonTypes.UR])])
def test_ur_with_address_mandatory_field(person_type, person_subtype, base_params, extra_params):
    client_id = steps.ClientSteps.create()
    person_params = base_params.copy()
    person_params.update(extra_params)
    steps.PersonSteps.create(client_id, person_type.code,
                             params=person_params,
                             strict_params=True)
    for field in extra_params.keys():
        if field in ['client_id', 'type']:
            continue
        person_params_copy = copy.deepcopy(person_params)
        del person_params_copy[field]
        with pytest.raises(Fault) as exc_info:
            steps.PersonSteps.create(client_id, person_type.code,
                                     params=person_params_copy,
                                     strict_params=True)
        error_msg = 'Missing mandatory person field \'{0}\' for person type {1}'.format(field, person_subtype)
        utils.check_that(error_msg, hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc_info.value, 'contents')))
