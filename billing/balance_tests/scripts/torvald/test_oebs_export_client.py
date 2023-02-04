# -*- coding: utf-8 -*-

import pytest
from hamcrest import equal_to

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils

CUSTOM = {}

COMMON = {
    'party_name': ('name',
                   'hz_parties.party_name'),

    'email_contact_point_type': (lambda x: 'EMAIL',
                                 'hz_contact_points.e.contact_point_type'),

    'email': ('email',
              'hz_contact_points.e.email_address'),

    'phone_contact_point_type': (lambda x: 'PHONE',
                                 'hz_contact_points.p.contact_point_type'),

    'phone': (lambda x: ''.join([ch for ch in x['phone'] if ch not in '+()-']),
              'hz_contact_points.p.phone_number'),

    'phone_line_type': (lambda x: 'GEN',
                        'hz_contact_points.p.phone_line_type'),

    'raw_phone_number': ('phone',
                         'hz_contact_points.p.raw_phone_number'),

    'transposed_phone_number': (lambda x: ''.join([ch for ch in x['phone'] if ch not in '+()-'])[::-1],
                                'hz_contact_points.p.transposed_phone_number'),

    'fax_contact_point_type': (lambda x: 'PHONE',
                               'hz_contact_points.f.contact_point_type'),

    'fax': (lambda x: ''.join([ch for ch in x['fax'] if ch not in '+()-']),
            'hz_contact_points.f.phone_number'),

    'fax_line_type': (lambda x: 'FAX',
                      'hz_contact_points.f.phone_line_type'),

    'raw_fax_number': ('fax',
                       'hz_contact_points.f.raw_phone_number'),

    'transposed_fax_number': (lambda x: ''.join([ch for ch in x['fax'] if ch not in '+()-'])[::-1],
                              'hz_contact_points.f.transposed_phone_number'),
}

AGENCY = {
    'type': (lambda x: 'AGENCY',
             'hz_parties.attribute1'),
}

CLIENT = {
    'type': (lambda x: 'CLIENT',
             'hz_parties.attribute1'),
}

ALL = reduce(lambda x, y: dict(x, **y), [CUSTOM, COMMON, AGENCY, CLIENT], {})


def get_balance_client_data(object_id):
    # t_person
    # person = db.get_person_by_id(object_id)[0]
    query = "select * from t_client where id = :object_id"
    result = db.balance().execute(query, {'object_id': object_id})
    client = result[0]

    # t_extprops for client
    extprops = db.balance().get_extprops_by_object_id('Client', object_id)
    prefixed_extprops = {'t_client.{0}'.format(key): extprops[key] for key in extprops}
    client.update(prefixed_extprops)

    return client


def get_oebs_client_data(object_id):
    client = {}

    # hz_parties
    mapping_string = 'C{0}'
    query = "select * from apps.hz_parties where orig_system_reference  = :object_id"
    result = api.test_balance().ExecuteOEBS(1, query, {'object_id': mapping_string.format(object_id)})
    if result:
        result = result[0]
        client.update({'hz_parties.{0}'.format(key): result[key] for key in result})

    # hz_contact_points
    mapping_list = {
        'e': 'C{0}_E',
        'p': 'C{0}_P',
        'f': 'C{0}_F',
    }
    for option in mapping_list:
        # TODO: почему не работает *? not well-formed (invalid token): line 67, column 18
        query = "select contact_point_type, email_address, phone_number, phone_line_type, raw_phone_number, transposed_phone_number from apps.hz_contact_points where orig_system_reference  = :object_id"
        result = api.test_balance().ExecuteOEBS(1, query, {'object_id': mapping_list[option].format(object_id)})
        if result:
            result = result[0]
            client.update({'hz_contact_points.{0}.{1}'.format(option, key): result[key] for key in result})

    return client


BALANCE_DATA_PROVIDER = get_balance_client_data
OEBS_DATA_PROVIDER = get_oebs_client_data


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


@pytest.mark.parametrize('type, checks', [
    (1, [COMMON, AGENCY]),
    (0, [COMMON, CLIENT])
])
def test_all_person_types_export(type, checks):
    client_id = None or steps.ClientSteps.create()
    steps.CommonSteps.export('OEBS', 'Client', client_id)
    compare(client_id, reduce(lambda x, y: dict(x, **y), checks, {}).keys())


if __name__ == "__main__":
    compare
