# -*- coding: utf-8 -*-
import pytest
from time import sleep
from balance.core import Core
from balance.constants import ExportState, CHECKED_BANNED_DOMAIN, NOT_BANNED_DOMAIN
from balance import exc

from tests.balance_tests.client.client_common import (create_client, create_person, create_role, create_passport,
                           ADDITIONAL_FUNCTIONS, BILLING_SUPPORT, CLIENT_FRAUD_STATUS_EDIT)
from tests import object_builder as ob


@pytest.mark.parametrize('params, permission',
                         [({'manual_suspect': 1}, ADDITIONAL_FUNCTIONS),
                          ({'reliable_cc_payer': 1}, BILLING_SUPPORT),
                          ({'deny_cc': 1}, CLIENT_FRAUD_STATUS_EDIT),
                          ({'direct25': 1}, BILLING_SUPPORT),
                          ({'manual_suspect_comment': 1}, ADDITIONAL_FUNCTIONS),

                          ({'is_non_resident': 1,
                            'non_resident_currency': 'RUR',
                            'name': 'client_name'},
                           ADDITIONAL_FUNCTIONS),

                          ({'domain_check_status': 1}, ADDITIONAL_FUNCTIONS),
                          ({'domain_check_comment': 1}, ADDITIONAL_FUNCTIONS)

                          ])
def test_restricted_fields(session, params, permission):
    role = create_role(session)
    create_passport(session, [role], patch_session=True)
    client = create_client(session)
    with pytest.raises(exc.PERMISSION_DENIED) as excinfo:
        Core(session=session).create_or_update_client(client.id, params=params.copy())
    assert excinfo.value == exc.PERMISSION_DENIED(
        u'Change field {0} without perm {1}'.format(params.keys()[0], permission))
    role = create_role(session, permission)
    create_passport(session, [role], patch_session=True)
    Core(session=session).create_or_update_client(client.id, params=params)


@pytest.mark.parametrize(
    'match_client',
    [True, None, False, ],
)
def test_client_fraud_flag(session, client, match_client):
    roles = []
    if match_client is not None:
        role = create_role(session, (CLIENT_FRAUD_STATUS_EDIT, {'client_batch_id': None}))
        client_batch = ob.RoleClientBuilder.construct(session, client=client if match_client else None).client_batch_id
        roles.append((role, {'client_batch_id': client_batch}))
    ob.set_roles(session, session.passport, roles)
    client = Core(session=session).create_or_update_client(client.id, params={'client_fraud_flag': 1, 'client_fraud_desc': 'billing fraud'})
    if match_client:
        assert client.fraud_status.fraud_flag == 1
        assert client.fraud_status.fraud_flag_desc == 'billing fraud'
        client = Core(session=session).create_or_update_client(client.id, params={'client_fraud_desc': 'billing fraud'})
        print client.fraud_status
    else:
        assert client.fraud_status is None


@pytest.mark.parametrize('is_agency_before, is_agency_after', [(0, 1), (1, 0)])
def test_update_set_once_field(session, is_agency_before, is_agency_after):
    client = create_client(session)
    client.is_agency = is_agency_before
    client_params = {'client_id': client.id}
    client_params.update({'is_agency': is_agency_after})
    with pytest.raises(exc.INVALID_PARAM) as excinfo:
        Core(session=session).create_or_update_client(client.id, params=client_params)
    assert excinfo.type == exc.INVALID_PARAM
    assert excinfo.value.msg == u'Invalid parameter for function: Cannot change is_agency'


def test_change_domain_status(session):
    client = create_client(session)
    client.class_.domain_check_status = CHECKED_BANNED_DOMAIN
    with pytest.raises(exc.INVALID_PARAM) as excinfo:
        Core(session=session).create_or_update_client(client.id, params={'domain_check_status': NOT_BANNED_DOMAIN})
    assert excinfo.value == exc.INVALID_PARAM(u'reset domain check status from check_banned')


def test_change_intercompany_value(session):
    new_intercompany_value = 'RU10'
    person = create_person(session)
    person.exports['OEBS'].state = ExportState.exported
    client = person.client
    Core(session=session).create_or_update_client(client.id, params={'intercompany': new_intercompany_value})

    assert person.exports['OEBS'].state == ExportState.enqueued
    assert client.intercompany == new_intercompany_value


def test_create_non_resident_error(session):
    client = create_client(session)
    with pytest.raises(exc.INVALID_PARAM) as excinfo:
        Core(session=session).create_or_update_client(client.id, params={'non_resident_currency': 'RUR'})
    assert excinfo.value == exc.INVALID_PARAM(u'Undefined full name or currency for non-resident client')


def test_create_non_resident(session):
    client = create_client(session)
    Core(session=session).create_or_update_client(client.id, params={'non_resident_currency': 'RUR',
                                                                     'name': 'client_name'})
    assert client.is_docs_separated == 1
    assert client.is_non_resident == 1
    assert client.currency_payment == 'RUR'
    assert client.iso_currency_payment == 'RUB'


def test_client_add_to_blacklist(session):
    client = create_client(session)
    client.add_to_black_list(payment_id=None, desc='fdf', type_='ergerg')


def test_empty_string_export_oebs(session):
    params = {
        'client_type_id': 1,
        'name': 'name',
        'email': 'new@email.ru',
        'subregion_id': 225,
        'fax': '',
        'phone': '',
        'url': '',
        'full_payment': 1,
        'manual_suspect': 1,
    }
    client = Core(session=session).create_or_update_client(None, params=params)
    first_enq = client.exports['OEBS'].enqueue_dt
    session.expire_all()
    sleep(1)

    params['client_id'] = client.id

    client = Core(session=session).create_or_update_client(client.id, params=params)
    second_enq = client.exports['OEBS'].enqueue_dt

    assert second_enq == first_enq
