# -*- coding: utf-8 -*-
import datetime
import copy
import hamcrest
import pytest

import btestlib.environments as env
from balance import balance_db as db
from balance import balance_steps as steps
from balance import balance_api as api
from btestlib import utils as ut
from btestlib.matchers import contains_dicts_with_entries

NOW = datetime.datetime.now()


@pytest.mark.parametrize('person_param, expected',
                         [
                             ({'legal-address-code': '770000000007095'},
                              {'legal_fias_guid': '45141195-0a4b-49a5-813c-90fc0089ff6b',
                               'legaladdress': None}),

                             ({'legal-fias-guid': '45141195-0a4b-49a5-813c-90fc0089ff6b'},
                              {'legal_address_code': '770000000007095',
                               'legaladdress': None}),

                             # кладр, по которому не подбирается fias
                             ({'legaladdress': 'test_address',
                               'legal-address-code': '5000700100000'},
                              {'legaladdress': 'test_address',
                               'legal_fias_guid': None}),

                             # кладр, по которому подбирается 2 фиаса
                             ({'legaladdress': 'test_address',
                               'legal-address-code': '660000230000008'},
                              {'legaladdress': 'test_address',
                               'legal_fias_guid': None}),

                             ({'legaladdress': '',
                               'legal-fias-guid': '',
                               'legal_address_code': ''},
                              {})
                         ])
def test_create_person_fias(person_param, expected):
    client_id = steps.ClientSteps.create()
    try:
        person_id = steps.PersonSteps.create(client_id=client_id, type_='ur',
                                             params=person_param)
        person_params_db = db.get_person_by_id(person_id)[0]
        service_order_id = steps.OrderSteps.next_id(7)
        order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=7,
                                           product_id=1475, params={'AgencyID': None})
        orders_list = [{'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0,
                                                     contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)
        steps.ExportSteps.export_oebs(client_id=client_id, invoice_id=invoice_id)
        #
        try:
            fias_guid_db = steps.CommonSteps.get_extprops('Person', person_id, 'legal_fias_guid')[0]['value_str']
        except Exception as exc:
            fias_guid_db = None

        person_params_db.update({'legal_fias_guid': fias_guid_db})
        ut.check_that([person_params_db], contains_dicts_with_entries([expected],
                                                                      same_length=True))
    except Exception as exc:
        print exc
        ut.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('MISSING_MANDATORY_PERSON_FIELD'))


def test_rsya_ph_create_person():
    client_id = steps.ClientSteps.create()
    api.medium().CreatePerson(16571028, {'client_id': client_id,
                                         'is_partner': '1',
                                         'type': 'ph'},
                              False)
    api.medium().CreatePerson(16571028, {'client_id': client_id,
                                         'is_partner': '1',
                                         'type': 'ur'},
                              False)
    api.medium().CreatePerson(16571028, {'client_id': client_id,
                                         'is_partner': '1',
                                         'type': 'ph'})
    api.medium().CreatePerson(16571028, {'client_id': client_id,
                                         'is_partner': '1',
                                         'type': 'ur'})
    # api.medium().CreatePerson(16571028, {'client_id': client_id,
    #                                      'is_partner': '1',
    #                                      'type': 'ph'},
    #                           True)
    api.medium().CreatePerson(16571028, {'client_id': client_id,
                                         'is_partner': '1',
                                         'type': 'ur'},
                              True)


@pytest.mark.parametrize('address_part_dict, expected',
                         [
                             ({'legaladdress': 'legaladdress'}, True),

                             ({'legal-address-home': '33',
                               'legal-fias-guid': '14bd4cf7-30f0-4fd0-ac37-93001892b580'}, True),

                             ({'legal-fias-guid': '14bd4cf7-30f0-4fd0-ac37-93001892b580'},
                              'Invalid parameter for function: [\'missing: legal-address-home\']'),

                             ({'legal-address-home': '33'},
                              'Invalid parameter for function: [\'missing: legal-fias-guid\']'),

                             ({'legal-address-code': '770000000007095',
                               'legal-address-home': '44'},
                              'Invalid parameter for function: [\'missing: legal-fias-guid\']'),

                             ({'legal-address-code': '770000000007095'},
                              'Invalid parameter for function: [\'missing: legal-address-home\','
                              ' \'missing: legal-fias-guid\']'),

                             ({}, 'Invalid parameter for function: [\'missing: legal-address-home\','
                                  ' \'missing: legal-fias-guid\']'),

                             ({'legal-address-home': '33'},
                              'Invalid parameter for function: [\'missing: legal-fias-guid\']'),

                         ])
def test_rsya_ph_create_or_update_partner(address_part_dict, expected):
    steps.ClientSteps.unlink_from_login(450606190)
    default_params = copy.deepcopy({'mode': 'form',
                                    'type': 'ph',
                                    'passport-id': 450606190,
                                    'lname': 'lname',
                                    'fname': 'fname',
                                    'phone': '+7 812 9164191',
                                    'email': 'email@email.ru',
                                    'pfr': '001-001-999 65',
                                    'inn': '788223856590',
                                    'legal-address-gni': 'erre',
                                    'legal-address-region': 'erre',
                                    'legal-address-postcode': 'gfbg',
                                    'address-gni': 'address-gni',
                                    'address-region': 'fdfdf',
                                    'address-postcode': '888888',
                                    'address-code': '770000000007095',
                                    'birthday': "1997-09-23",
                                    'passport-d': "2000-01-01",
                                    'passport-s': '4656',
                                    'passport-n': '566282',
                                    'passport-e': 'passport-e',
                                    'passport-code': 'passport-code',
                                    'yamoney-wallet': '324324342',
                                    'bank-type': 3})
    default_params.update(address_part_dict)
    try:
        steps.PartnerSteps.create_or_update_partner(16571028, default_params)
        assert expected is True
    except Exception as exc:
        msg = steps.CommonSteps.get_exception_code(exc, 'msg')
        print msg
        assert expected == msg


@pytest.mark.parametrize('address_part_dict, expected',
                         [
                             ({'legaladdress': 'legaladdress'}, True),

                             ({'legal-address-home': '33',
                               'legal-fias-guid': '14bd4cf7-30f0-4fd0-ac37-93001892b580'}, True),

                             ({'legal-fias-guid': '14bd4cf7-30f0-4fd0-ac37-93001892b580'},
                              'Invalid parameter for function: [\'missing: legal-address-home\']'),

                             ({'legal-address-home': '33'},
                              'Invalid parameter for function: [\'missing: legal-fias-guid\']'),

                             ({'legal-address-code': '770000000007095',
                               'legal-address-home': '44'},
                              'Invalid parameter for function: [\'missing: legal-fias-guid\']'),

                             ({'legal-address-code': '770000000007095'},
                              'Invalid parameter for function: [\'missing: legal-address-home\','
                              ' \'missing: legal-fias-guid\']'),

                             ({}, 'Invalid parameter for function: [\'missing: legal-address-home\','
                                  ' \'missing: legal-fias-guid\']'),

                             ({'legal-address-home': '33'},
                              'Invalid parameter for function: [\'missing: legal-fias-guid\']'),

                         ])
def test_rsya_ph_create_person(address_part_dict, expected):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.unlink_from_login(450606190)
    default_params = copy.deepcopy({'mode': 'form',
                                    'type': 'ph',
                                    'passport-id': '450606190',
                                    'lname': 'lname',
                                    'fname': 'fname',
                                    'mname': 'mnme',
                                    'phone': '+7 812 9164191',
                                    'email': 'email@email.ru',
                                    'pfr': '001-001-999 65',
                                    'inn': '788223856590',
                                    'legal-address-gni': 'erre',
                                    'legal-address-region': 'erre',
                                    'legal-address-postcode': 'gfbg',
                                    'address-gni': 'address-gni',
                                    'address-region': 'fdfdf',
                                    'address-postcode': '888888',
                                    'address-code': '770000000007095',
                                    'birthday': "1997-09-23",
                                    'passport-d': "2000-01-01",
                                    'passport-s': '4656',
                                    'passport-n': '566282',
                                    'passport-e': 'passport-e',
                                    'passport-code': 'passport-code',
                                    'yamoney-wallet': '324324342',
                                    'bank-type': '3',
                                    'is_partner': '1',
                                    'client_id': str(client_id)
                                    })
    default_params.update(address_part_dict)
    try:
        api.test_balance().CreatePerson(16571028, default_params, True)
        persons_page_url = '{base_url}/subpersons.xml?tcl_id={client_id}'.format(
            base_url=env.balance_env().balance_ai, client_id=client_id)
        print persons_page_url
        assert expected is True
    except Exception as exc:
        msg = steps.CommonSteps.get_exception_code(exc, 'msg')
        print msg
        assert expected == msg


@pytest.mark.parametrize('address_part_dict, expected',
                         [
                             ({'legal-address-home': '33',
                               'legal-fias-guid': '14bd4cf7-30f0-4fd0-ac37-93001892b580',
                               'legaladdress': 'legaladdress'}, True),

                         ])
def test_clear_legal_address(address_part_dict, expected):
    steps.ClientSteps.unlink_from_login(450606190)
    default_params = copy.deepcopy({'mode': 'form',
                                    'type': 'ph',
                                    'passport-id': 450606190,
                                    'lname': 'lname',
                                    'fname': 'fname',
                                    'phone': '+7 812 9164191',
                                    'email': 'email@email.ru',
                                    'pfr': '001-001-999 65',
                                    'inn': '788223856590',
                                    'legal-address-gni': 'erre',
                                    'legal-address-region': 'erre',
                                    'legal-address-postcode': 'gfbg',
                                    'address-gni': 'address-gni',
                                    'address-region': 'fdfdf',
                                    'address-postcode': '111111',
                                    'address-code': '770000000007095',
                                    'birthday': "1997-09-23",
                                    'passport-d': "2000-01-01",
                                    'passport-s': '4656',
                                    'passport-n': '566282',
                                    'passport-e': 'passport-e',
                                    'passport-code': 'passport-code',
                                    'yamoney-wallet': '324324342',
                                    'bank-type': 3})
    default_params.update(address_part_dict)
    client_id = steps.PartnerSteps.create_or_update_partner(16571028, default_params)
    assert db.get_persons_by_client(client_id)[0]['legaladdress'] is None


@pytest.mark.parametrize('person_param, expected, exception_text',
                         [
                             ({'legal-address-code': '770000000007095'},

                              {'legal-fias-guid': '45141195-0a4b-49a5-813c-90fc0089ff6b',
                               'legaladdress': None},

                              'Invalid parameter for function: [\'missing: legal-address-home\', \'missing: legal-fias-guid\']'),

                             ({'legal-fias-guid': '57c050d1-fd87-4d7b-89db-0520db3a51de',
                              'legal-address-home': '2'},
                              {'legal_address_code': None},
                              ''),

                             ({'legal-fias-guid': '45141195-0a4b-49a5-813c-90fc0089ff6b',
                               'legal-address-home': '2'},

                              {'legal_address_code': '770000000007095',
                               'legaladdress': None},

                              ''),

                             ({'legaladdress': 'drdr',
                               'legal-fias-guid': '',
                               'legal_address_code': ''},

                              {'legal_address_code': None},
                              '')
                         ])
def test_create_rsya_person_fias(person_param, expected, exception_text):
    steps.ClientSteps.unlink_from_login(450606190)
    try:
        default_params = copy.deepcopy({'mode': 'form',
                                        'type': 'ph',
                                        'passport-id': 450606190,
                                        'lname': 'lname',
                                        'fname': 'fname',
                                        'phone': '+7 812 9164191',
                                        'email': 'email@email.ru',
                                        'pfr': '001-001-999 65',
                                        'inn': '788223856590',
                                        'legal-address-gni': 'erre',
                                        'legal-address-region': 'erre',
                                        'legal-address-postcode': 'gfbg',
                                        'address-gni': 'address-gni',
                                        'address-region': 'fdfdf',
                                        'address-postcode': '111111',
                                        'address-code': '770000000007095',
                                        'birthday': "1997-09-23",
                                        'passport-d': "2000-01-01",
                                        'passport-s': '4656',
                                        'passport-n': '566282',
                                        'passport-e': 'passport-e',
                                        'passport-code': 'passport-code',
                                        'yamoney-wallet': '324324342',
                                        'bank-type': 3})
        default_params.update(person_param)
        client_id = steps.PartnerSteps.create_or_update_partner(16571028, default_params)
        person = db.get_persons_by_client(client_id)[0]
        try:
            fias_guid_db = steps.CommonSteps.get_extprops('Person', person['id'], 'legal_fias_guid')[0]['value_str']
        except Exception as exc:
            fias_guid_db = None

        person.update({'legal_fias_guid': fias_guid_db})
        ut.check_that([person], contains_dicts_with_entries([expected], same_length=True))
    except Exception as exc:
        print exc
        ut.check_that(steps.CommonSteps.get_exception_code(exc, 'msg'), hamcrest.equal_to(exception_text))
