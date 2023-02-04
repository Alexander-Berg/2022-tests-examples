# -*- coding: utf-8 -*-

import datetime

import pytest
from hamcrest import equal_to

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils

CUSTOM = {
    'intercompany': ('t_client.intercompany',
                     'hz_cust_accounts.attribute16'),

    'invalid_bankprops': ('invalid_bankprops',
                          lambda x: 1 if x['hz_cust_accounts.hold_bill_flag'] == 'Y' else 0),
}

PH = {
    'party_type': (lambda: 'PERSON',
                   'hz_parties.p.party_type'),

    'party_name': (lambda x: '{0} {1}'.format(x['fname'], x['lname']),
                   'hz_parties.p.party_name'),

    'party_first_name': ('fname',
                         'hz_parties.p.party_first_name'),

    'party_middle_name': ('mname',
                          'hz_parties.p.party_middle_name'),

    'party_last_name': ('lname',
                        'hz_parties.p.party_last_name'),
}

UR = {
    # плательщиков фирмы 1,10 и 2 будем искать по ИНН (так как в оебс они склеиваются по нему)

    # для украинских юриков ЕГРПОУ хранится в поле kpp, и, если оно непустое,
    # то его значение передается в orig_system_reference вместо inn
    # если kpp=null, то в orig_system_reference передается inn
    # если пусты оба значения, то в orig_system_reference передается только I

    # плательщиков других фирм (у которых нет ИНН) будем искать по id

    # TODO: выборка данных по inn
    'party_type': (lambda: 'ORGANIZATION',
                   'hz_parties.p.party_type'),

    # TODO: выборка данных по inn
    'party_name': ('name',
                   'hz_parties.p.party_name'),

    # TODO: выборка данных по inn
    'inn': ('inn',
            'hz_parties.p.jgzz_fiscal_code'),

    # TODO: выборка данных по inn
    # у украинцев-спд нет кпп, и у них в это же поле грузится инн
    #         'kpp': (None,
    #                 None),

    'account': ('account',
                'hz_cust_accounts.attribute5'),

    'bik': ('bik',
            'hz_cust_accounts.attribute8')
}

NONRES = {
    'party_type': (lambda: 'ORGANIZATION',
                   'hz_parties.p.party_type'),

    'party_name': ('name',
                   'hz_parties.p.party_name'),

    'account': ('account',
                'hz_cust_accounts.attribute5')
}

PAYER_PER_COUNTRY = {  # firm_id = 1
    'ur': 'RU',
    'ph': 'RU',
    'yt': 'NU',
    'ytph': 'NU',
    'yt_kzu': 'KZ',
    'yt_kzp': 'KZ',
    # 'endbuyer_ph': 'RU',
    # 'endbuyer_ur': 'RU',
    # 'endbuyer_yt': 'NU',
    # firm_id = 2
    'pu': 'UA',
    'ua': 'UA',
    # firm_id = 3
    'kzp': 'KZ',
    'kzu': 'KZ',
    # firm_id = 4
    'usp': 'US',
    'usu': 'US',
    # firm_id = 5
    'byp': 'BY',
    'byu': 'BY',
    'by_ytph': 'NU',
    # firm_id = 6
    'eu_ur': 'SW',
    'eu_yt': 'NU'
    # TODO: finish it
}

ADDRESS = {
    'country': (lambda x: PAYER_PER_COUNTRY[x['type']],
                'hz_locations.country'),
    'postal_code': ('postcode',
                    'hz_locations.postal_code'),
    # TODO: finish it
    # 'address_info': (None,
    #                  None)
}

NAME = {
    'longname': (lambda x: x['longname'] or x['name'],
                 'hz_cust_accounts.attribute4'),

    'account_name': ('name',
                     'hz_cust_accounts.account_name'),

    'representative': ('representative',
                       'hz_parties.cp.person_last_name'),

    'signer_person_name': ('signer_person_name',
                           'hz_parties.dp.person_last_name'),

    # TODO: некорректная выгрузка подписанта
    's_signer_position_name': ('s_signer_position_name',
                               'hz_cust_account_roles.attribute1')
}

PH_CONTACT = {
    'email_contact_point_type': (lambda: 'EMAIL',
                                 'hz_contact_points.e.contact_point_type'),

    'email': ('email',
              'hz_contact_points.e.email_address'),

    'phone_contact_point_type': (lambda: 'PHONE',
                                 'hz_contact_points.p.contact_point_type'),

    'phone': (lambda x: [ch for ch in x['phone'] if ch not in '+()-'],
              lambda x: '{0}{1}{2}'.format(x['hz_contact_points.p.phone_country_code'],
                                           x['hz_contact_points.p.phone_area_code'],
                                           x['hz_contact_points.p.phone_number'])),

    'phone_line_type': (lambda: 'GEN',
                        'hz_contact_points.p.phone_line_type'),

    'fax_contact_point_type': (lambda: 'PHONE',
                               'hz_contact_points.f.contact_point_type'),

    'fax': (lambda x: [ch for ch in x['fax'] if ch not in '+()-'],
            lambda x: '{0}{1}{2}'.format(x['hz_contact_points.p.phone_country_code'],
                                         x['hz_contact_points.p.phone_area_code'],
                                         x['hz_contact_points.p.phone_number'])),

    'fax_line_type': (lambda: 'FAX',
                      'hz_contact_points.f.phone_line_type'),

    'hz_parties.email': ('email',
                         'hz_parties.p.email_address'),

    'hz_parties.phone': (lambda x: [ch for ch in x['phone'] if ch not in '+()-'],
                         lambda x: '{0}{1}{2}'.format(x['hz_parties.p.primary_phone_country_code'],
                                                      x['hz_parties.p.primary_phone_area_code'],
                                                      x['hz_parties.p.primary_phone_number'])),
}

NON_PH_CONTACT = {
    'email_contact_point_type': (lambda: 'EMAIL',
                                 'hz_contact_points.ce.contact_point_type'),

    'email': ('email',
              'hz_contact_points.ce.email_address'),

    'phone_contact_point_type': (lambda: 'PHONE',
                                 'hz_contact_points.cp.contact_point_type'),

    'phone': (lambda x: [ch for ch in x['phone'] if ch not in '+()-'],
              lambda x: '{0}{1}{2}'.format(x['hz_contact_points.cp.phone_country_code'],
                                           x['hz_contact_points.cp.phone_area_code'],
                                           x['hz_contact_points.cp.phone_number'])),

    'phone_line_type': (lambda: 'GEN',
                        'hz_contact_points.cp.phone_line_type'),

    'fax_contact_point_type': (lambda: 'PHONE',
                               'hz_contact_points.cf.contact_point_type'),

    'fax': (lambda x: [ch for ch in x['fax'] if ch not in '+()-'],
            lambda x: '{0}{1}{2}'.format(x['hz_contact_points.cf.phone_country_code'],
                                         x['hz_contact_points.cf.phone_area_code'],
                                         x['hz_contact_points.cf.phone_number'])),

    'fax_line_type': (lambda: 'FAX',
                      'hz_contact_points.cf.phone_line_type'),
}

ALL = reduce(lambda x, y: dict(x, **y), [CUSTOM, PH, UR, NONRES, ADDRESS, NAME, PH_CONTACT, NON_PH_CONTACT], {})

CHECKS_BY_TYPE = [
    # ('ph', [PH, ADDRESS, PH_CONTACT]),
    ('ur', [UR, ADDRESS, NON_PH_CONTACT, NAME]),
    ('yt', [NONRES, ADDRESS, NON_PH_CONTACT, NAME]),
    ('ytph', [NONRES, ADDRESS, NON_PH_CONTACT, NAME]),
    ('yt_kzu', [NONRES, ADDRESS, NON_PH_CONTACT, NAME]),
    ('yt_kzp', [NONRES, ADDRESS, NON_PH_CONTACT, NAME]),
    # TODO: finish it
]


def get_balance_person_data(object_id):
    # t_person
    # person = db.get_person_by_id(object_id)[0]
    query = "select * from t_person where id = :object_id"
    result = db.balance().execute(query, {'object_id': object_id})
    person = result[0]

    # t_extprops for persons
    extprops = db.balance().get_extprops_by_object_id('Person', object_id)
    person.update(extprops)

    # t_extprops for client (for 'intercompany' attribute)
    client_id = db.get_person_by_id(object_id)[0]['client_id']
    extprops = db.balance().get_extprops_by_object_id('Client', client_id)
    prefixed_extprops = {'t_client.{0}'.format(key): extprops[key] for key in extprops}
    person.update(prefixed_extprops)

    return person


def get_oebs_person_data(object_id):
    person = {}

    # hz_cust_accounts
    mapping_string = 'P{0}'
    query = "select * from apps.hz_cust_accounts where account_number = :object_id"
    # result = db.Oebs().execute(query, {'object_id': mapping_string.format(object_id)})
    result = api.test_balance().ExecuteOEBS(1, query, {'object_id': mapping_string.format(object_id)})
    if result:
        result = result[0]
        person.update({'hz_cust_accounts.{0}'.format(key): result[key] for key in result})

    # hz_cust_account_roles
    mapping_string = 'P{0}_D'
    query = "select * from apps.hz_cust_account_roles where orig_system_reference  = :object_id"
    # result = db.Oebs().execute(query, {'object_id': mapping_string.format(object_id)})
    result = api.test_balance().ExecuteOEBS(1, query, {'object_id': mapping_string.format(object_id)})
    if result:
        result = result[0]
        person.update({'hz_cust_account_roles.{0}'.format(key): result[key] for key in result})

    # hz_parties
    mapping_list = {'p': 'P{0}',
                    'cp': 'CP{0}',
                    'dp': 'DP{0}',
                    }
    for option in mapping_list:
        query = "select * from apps.hz_parties where orig_system_reference  = :object_id"
        # result = db.Oebs().execute(query, {'object_id': mapping_strings[option].format(object_id)})
        result = api.test_balance().ExecuteOEBS(1, query, {'object_id': mapping_list[option].format(object_id)})
        if result:
            result = result[0]
            person.update({'hz_parties.{0}.{1}'.format(option, key): result[key] for key in result})

    # hz_locations
    mapping_string = 'P{0}_B'
    query = "select * from apps.hz_locations where orig_system_reference  = :object_id"
    # result = db.Oebs().execute(query, {'object_id': mapping_string.format(object_id)})
    result = api.test_balance().ExecuteOEBS(1, query, {'object_id': mapping_string.format(object_id)})
    if result:
        result = result[0]
        person.update({'hz_locations.{0}'.format(key): result[key] for key in result})

    # hz_contact_points
    mapping_list = {'e': 'P{0}_E',
                    'ce': 'P{0}_CE',
                    'p': 'P{0}_P',
                    'cp': 'P{0}_CP',
                    'f': 'P{0}_F',
                    'cf': 'P{0}_CF'
                    }
    for option in mapping_list:
        query = "select * from apps.hz_contact_points where orig_system_reference  = :object_id"
        # result = db.Oebs().execute(query, {'object_id': mapping_strings[option].format(object_id)})
        result = api.test_balance().ExecuteOEBS(1, query, {'object_id': mapping_list[option].format(object_id)})
        if result:
            result = result[0]
            person.update({'hz_contact_points.{0}.{1}'.format(option, key): result[key] for key in result})

    return person


BALANCE_DATA_PROVIDER = get_balance_person_data
OEBS_DATA_PROVIDER = get_oebs_person_data


def compare(object_id, attrs=None, excluded_attrs=None):
    balance_object = BALANCE_DATA_PROVIDER(object_id)
    oebs_object = OEBS_DATA_PROVIDER(object_id)
    balance_object = {key.lower(): balance_object[key] for key in balance_object}
    oebs_object = {key.lower(): oebs_object[key] for key in oebs_object}
    balance_values = {}
    oebs_values = {}
    for key, (balance_loc, oebs_loc) in {key: ALL[key] for key in attrs}.items():
        # balance_values[key] = balance_loc(balance_object) if callable(balance_loc) else balance_object[balance_loc]
        if callable(balance_loc):
            try:
                balance_values[key] = balance_loc(balance_object)
            except:
                balance_values[key] = 'no_value'
        else:
            balance_values[key] = balance_object.get(balance_loc, 'no_value')
        # oebs_values[key] = oebs_loc(oebs_object) if callable(oebs_loc) else oebs_object[oebs_loc]
        if callable(oebs_loc):
            try:
                oebs_values[key] = oebs_loc(oebs_object)
            except:
                oebs_values[key] = 'no_value'
        else:
            oebs_values[key] = oebs_object.get(oebs_loc, 'no_value')
    utils.check_that(oebs_values, equal_to(balance_values))


DIRECT_SERVICE_ID = 7
DIRECT_PRODUCT_ID = 1475
BANK_UR_PAYSYS_ID = 1003
NOW = datetime.datetime.now()
QTY = 100


def create_simple_exported_invoice(client_id, person_id):
    campaigns_list = [{'client_id': client_id, 'service_id': DIRECT_SERVICE_ID, 'product_id': DIRECT_PRODUCT_ID,
                       'qty': QTY, 'begin_dt': NOW}]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id, person_id, campaigns_list,
                                                                  BANK_UR_PAYSYS_ID, NOW, agency_id=None, credit=0,
                                                                  contract_id=None, overdraft=0, manager_uid=None)
    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)


@pytest.mark.parametrize('type, checks', CHECKS_BY_TYPE)
def test_all_person_types_export(type, checks):
    client_id = None or steps.ClientSteps.create()
    person_id = None or steps.PersonSteps.create(client_id, type)
    create_simple_exported_invoice(client_id, person_id)
    compare(person_id, reduce(lambda x, y: dict(x, **y), checks, {}).keys())


def test_intercompany_ur():
    # Создаём клиента, с заполненным параметром интеркомпани и счётом,
    # так как плательщик выгружается только вместе с счётом\договором\актом
    INTERCOMPANY_VALUE = 'RU10'
    client_id = None or steps.ClientSteps.create()
    steps.CommonSteps.set_extprops('Client', client_id, 'intercompany', {'value_str': INTERCOMPANY_VALUE})
    person_id = None or steps.PersonSteps.create(client_id, 'ur')
    create_simple_exported_invoice(client_id, person_id)
    compare(person_id, ['intercompany'])

    # TODO: перевыгрузка работает только при изменении значения через интерфейс
    # OTHER_INTERCOMPANY_VALUE = 'RU20'
    # steps.CommonSteps.set_extprops('Client', client_id, 'intercompany', {'value_str': OTHER_INTERCOMPANY_VALUE})
    # compare(person_id, ['intercompany'])


def test_invalid_bankprops_after_reexport_ur():
    client_id = None or steps.ClientSteps.create()
    person_id = None or steps.PersonSteps.create(client_id, 'ur', params={'invalid-bankprops': '1'})
    create_simple_exported_invoice(client_id, person_id)
    compare(person_id, ['invalid-bankprops'])

    steps.CommonSteps.set_extprops('Person', person_id, 'invalid-bankprops', {'value_num': '0'})
    steps.CommonSteps.export('OEBS', 'Person', person_id)
    compare(person_id, ['invalid-bankprops'])


if __name__ == "__main__":
    compare
