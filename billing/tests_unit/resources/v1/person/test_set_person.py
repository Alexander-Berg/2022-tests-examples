# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import datetime

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import (
    constants as cst,
    mapper,
)
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.utils import clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role

pytestmark = [
    pytest.mark.set_person,
]


@pytest.fixture(name='edit_person_role')
def create_edit_person_role():
    return create_role(
        (cst.PermissionCode.EDIT_PERSONS, {cst.ConstraintTypes.client_batch_id: None}),
    )


class TestSetPerson(TestCaseApiAppBase):
    BASE_API = '/v1/person/set-person'

    def test_invalid_email(self, client):
        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={
                "client_id": client.id,
                "is_partner": True,
                "person_type": "ur",
                "data": {
                    "email": "",
                },
            }
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'email': hm.contains(hm.has_entries({'error': 'WRONG_EMAIL'})),
                }),
            }),
        )

    @pytest.mark.smoke
    def test_create_person(self, client):
        person_data = {
            'is_partner': 0,
            'lname': 'Naaaataly',
            'fname': 'Наталья',
            'mname': 'Юр',
            'phone': '79778158057',
            'fax': '1234',
            'email': 'nata-test@info.ru',
            'country_id': 225,  # Россия
            'bik': '046577001',
        }
        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={
                'client_id': client.id,
                'mode': 'EDIT',
                'person_id': None,
                'person_type': 'ph',
                'data': person_data,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        person_id = res.get_json().get('data').get('id')
        person = self.test_session.query(mapper.Person).getone(person_id)

        person_data['type'] = 'ph'
        person_data['client_id'] = client.id
        hm.assert_that(
            person,
            hm.has_properties(person_data),
        )

    @pytest.mark.smoke
    def test_change_person(self, client, person):
        person.fname = 'Harry'
        person.api_version = 1
        person.country_id = 225  # Россия
        self.test_session.flush()

        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_id': person.id,
                'person_type': 'ph',
                'mode': 'EDIT',
                'data': {
                    'fname': 'Ron',
                    'api_version': 2,
                    'country_id': 149,  # Беларусь
                },
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.refresh(person)
        assert person.fname == 'Ron'
        assert person.api_version == '2'
        assert person.country_id == 149

    def test_set_hidden(self, client, person):
        person.hidden = 1
        self.test_session.flush()

        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_id': person.id,
                'person_type': person.type,
                'mode': 'SET_HIDDEN',
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        assert person.hidden == 0

    def test_invalid_person_type(self, client):
        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_type': 'abc',
                'data': {'mname': 'Nata'},
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'person_type': hm.contains(hm.has_entries({'error': 'INVALID_PERSON_TYPE_FIELD_VALIDATION_ERROR'})),
                }),
            }),
        )

    def test_empty_data(self, client):
        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'data': {},
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        data = res.get_json()
        hm.assert_that(
            data,
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'person_type': hm.contains(hm.has_entries({'error': 'REQUIRED_FIELD_VALIDATION_ERROR'})),
                    'data': hm.contains(hm.has_entries({'error': 'NOT_EMPTY_FIELD_VALIDATION_ERROR'})),
                }),
            }),
        )

    def test_invalid_field(self, client):
        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_type': 'ph',
                'mode': 'EDIT',
                'data': {
                    'lname': 'Naaaataly',
                    'fname': 'Наталья',
                    'mname': 'Юр',
                },
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'email': hm.contains(hm.has_entries({'error': 'MISSING_MANDATORY_PERSON_FIELD'})),
                    'phone': hm.contains(hm.has_entries({'error': 'MISSING_MANDATORY_PERSON_FIELD'})),
                }),
            }),
        )

    def test_invalid_single_account_creation(self):
        person_data = {
            'lname': 'Naaaataly',
            'fname': 'Наталья',
            'mname': 'Юр',
            'phone': '79778158057',
            'email': 'nata-test@info.ru',
        }
        client = create_client(
            name='snout_test_%s' % ob.get_big_number(),
            single_account_number=ob.get_big_number(),
        )
        person = create_person(  # не сможем создать ещё одного плательщика ph
            client=client,
            **person_data  # noqa C815
        )
        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={
                'client_id': client.id,
                'mode': 'EDIT',
                'person_id': None,
                'person_type': 'ph',
                'data': person_data,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    '?': hm.contains(hm.has_entries({'error': 'MULTIPLE_INDIVIDUALS'})),
                }),
            }),
        )

    def test_invalid_single_account_changing(self):
        person_data = {
            'lname': 'Naaaataly',
            'fname': 'Наталья',
            'mname': 'Юр',
            'phone': '79778158057',
            'email': 'nata-test@info.ru',
        }
        # сначала создаем плательщиков, а затем добавляем клиенту ЕЛС
        # тогда сможем дойти до проверки изменения неправильных плательщиков
        client = create_client(
            name='snout_test_%s' % ob.get_big_number(),
        )
        persons = [
            create_person(
                client=client,
                type='ph',
                **person_data  # noqa C815
            )
            for _i in range(2)
        ]
        client.single_account_number = ob.get_big_number()
        self.test_session.flush()

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={
                'client_id': client.id,
                'mode': 'EDIT',
                'person_id': persons[1].id,
                'person_type': 'ph',
                'data': {'fname': 'Random name'},
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    '?': hm.contains(hm.has_entries({'error': 'MULTIPLE_INDIVIDUALS'})),
                }),
            }),
        )

    def test_invalid_changing_inn(self):
        """Перехватываем INVALID_PARAM в снауте
        """
        # self.test_session.config.__dict__['CHANGING_INN_CONFIGURABLE_CHECK'] = 0
        # self.test_session.config.__dict__['CHANGING_INN_ALLOWED_PERSON_TYPES'] = []

        person_data = {
            'lname': 'Naaaataly',
            'fname': 'Наталья',
            'mname': 'Юр',
            'phone': '79778158057',
            'email': 'nata-test@info.ru',
        }
        client = create_client(
            name='snout_test_%s' % ob.get_big_number(),
            single_account_number=ob.get_big_number(),
        )
        person = create_person(
            client=client,
            type='ur',
            inn='1000000003',
            **person_data  # noqa C815
        )

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={
                'client_id': client.id,
                'mode': 'EDIT',
                'person_id': person.id,
                'person_type': 'ph',
                'data': {'inn': '1000000002'},
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'inn': hm.contains(hm.has_entries({'error': 'CHANGING_INN_IS_PROHIBITED'})),
                }),
            }),
        )

    def test_valid_changing_inn_configurable(self):
        # self.test_session.config.__dict__['CHANGING_INN_CONFIGURABLE_CHECK'] = 1
        # self.test_session.config.__dict__['CHANGING_INN_ALLOWED_PERSON_TYPES'] = ['ur', 'ph']

        person_data = {
            'lname': 'Naaaataly',
            'fname': 'Наталья',
            'mname': 'Юр',
            'phone': '79778158057',
            'email': 'nata-test@info.ru',
        }
        client = create_client(
            name='snout_test_%s' % ob.get_big_number(),
            single_account_number=ob.get_big_number(),
        )
        security.set_roles([])
        security.set_passport_client(client)

        person = create_person(
            client=client,
            type='ur',
            inn='1000000003',
            **person_data  # noqa C815
        )

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={
                'client_id': client.id,
                'mode': 'EDIT',
                'person_id': person.id,
                'person_type': 'ph',
                'data': {'inn': '1000000002'},
            },
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'form_errors': hm.has_entries({
                    'inn': hm.contains(hm.has_entries({'error': 'CHANGING_INN_IS_PROHIBITED'})),
                }),
            }),
        )

    @pytest.mark.now
    def test_set_hidden_single_account_case(self, client):
        # сначала создаем условия, которые не пройдут проверку,
        # потом добавляем ЕЛС
        persons = [
            create_person(client=client, type='ph')
            for _i in range(2)
        ]
        persons[1].hidden = 1
        client.single_account_number = ob.get_big_number()
        self.test_session.flush()

        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_id': persons[1].id,
                'person_type': 'ph',
                'mode': 'SET_HIDDEN',
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    '?': hm.contains(hm.has_entries({'error': 'MULTIPLE_INDIVIDUALS'})),
                }),
            }),
        )

    def test_set_region(self, client):
        person_data = {
            'lname': 'Naaaataly',
            'fname': 'Наталья',
            'mname': 'Юр',
            'phone': '79778158057',
            'email': 'nata-test@info.ru',
            'country_id': 146,  # Беларусь
            'region_id': 73,  # Россия, Дальневосточный федеральный округ
        }
        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_type': 'ph',  # резидент России
                'mode': 'EDIT',
                'data': person_data,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        person_id = res.get_json().get('data')['id']
        person = self.test_session.query(mapper.Person).getone(person_id)
        assert person.region == 73
        assert person.country_id == 146

    @pytest.mark.parametrize(
        'name, longname, req_name, req_longname, res_longname',
        [
            ('1', '2', '3', '4', '4'),
            ('1', '2', '3', None, '2'),
            ('1', '2', None, None, '2'),
            ('1', None, '3', None, '3'),
            ('1', None, None, None, '1'),
        ],
    )
    def test_longname_for_sw_yt(self, client, name, longname, req_name, req_longname, res_longname):
        person = create_person(
            client=client,
            type='sw_yt',
            is_partner=1,
            name=name,
            longname=longname,
        )
        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_type': 'sw_yt',
                'person_id': person.id,
                'mode': 'EDIT',
                'is_partner': True,
                'data': clean_dict({'name': req_name, 'longname': req_longname, 'phone': '+41 32 2304106'}),
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.refresh(person)
        assert person.longname == res_longname

    def test_longname_for_sw_ytph_invalid_edit(self, client):
        self.test_session.config.__dict__['CREATE_PARTNER_PERSON_FROM_CLIENT_UI'] = True
        security.set_roles([])
        security.set_passport_client(client)

        data = {
            'city': 'city QtrIf',
            'country_id': 225,
            'email': 'Hc`F@lmsS.Sta',
            'fax': '+41 32 2814442',
            'file': '/testDocs.txt',
            'fname': u'Майя',
            'lname': u'Суханов',
            'phone': '+41 32 9126742',
            'postaddress': u'Улица 3',
            'postcode': '53431',
            'region': '21595',
            'type': 'sw_ytph',
        }
        base_data = {
            'client_id': client.id,
            'person_type': 'sw_yt',
            'mode': 'EDIT',
            'is_partner': 1,
            'data': data,
        }

        # создаем
        person = create_person(client=client, verified_docs=False, is_partner=True, **data)

        base_data['person_id'] = person.id
        data['fname'] = 'Настя'
        data['lname'] = 'Пушкарёва'
        data['birthday'] = '2001-01-01'

        # можно менять без verified_docs
        res = self.test_client.secure_post_json(self.BASE_API, base_data, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            person,
            hm.has_properties({
                'name': 'Пушкарёва Настя',
                'fname': 'Настя',
                'lname': 'Пушкарёва',
                'birthday': datetime.date(2001, 1, 1),
                'is_partner': True,
            }),
        )

        person.verified_docs = 1
        self.test_session.flush()

        # не падаем, если нет изменений
        res = self.test_client.secure_post_json(self.BASE_API, base_data, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data['fname'] = u'Сказочный'
        data['lname'] = u'Герой'
        data['birthday'] = '2010-10-10'

        # нельзя менять с verified_docs
        res = self.test_client.secure_post_json(self.BASE_API, base_data, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'fname': hm.contains(hm.has_entries({'error': 'CHANGING_FIELD_IS_PROHIBITED'})),
                    'lname': hm.contains(hm.has_entries({'error': 'CHANGING_FIELD_IS_PROHIBITED'})),
                    'birthday': hm.contains(hm.has_entries({'error': 'CHANGING_FIELD_IS_PROHIBITED'})),
                }),
            }),
        )

    def test_longname_for_sw_yt_invalid_create(self, client):
        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_type': 'sw_yt',
                'mode': 'EDIT',
                'is_partner': True,
                'data': {
                    'city': 'city FVHzd',
                    'fax': '+41 32 8617302',
                    'file': '/testDocs.txt',
                    'representative': u'Нерезидент, ШвейцарияBcz',
                    'verified-docs': '1',
                    # 'swift': 'HABALV22',
                    'region': '206',
                    's_signer-position-name': u'Финансовый директор',
                    'email': '0^$-@rNcG.QdG',
                    'phone': '+41 32 2304106',
                    'inn': 'LV40103259867',
                    'postcode': '43857',
                    'account': 'LV37HABA0551028187454',
                    'legaladdress': 'Avenue 5',
                    'postaddress': u'Улица 5',
                    'signer-person-name': 'Signer zXKgO',
                    'country_id': '225',
                },
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    '?': hm.contains(hm.has_entries({
                        'error': 'INVALID_PARAM',
                        'description': 'Invalid parameter for function: longname or name is required for sw_yt',
                    })),
                }),
            }),
        )

    @pytest.mark.parametrize(
        'w_role',
        [True, False],
    )
    @pytest.mark.parametrize(
        'w_flag',
        [True, False],
    )
    def test_change_partner_person(self, client, edit_person_role, w_role, w_flag):
        self.test_session.config.__dict__['CREATE_PARTNER_PERSON_FROM_CLIENT_UI'] = w_flag
        roles = []
        if w_role:
            roles.append(edit_person_role)
        security.set_roles(roles)
        security.set_passport_client(client)

        person = create_person(client=client, is_partner=True)

        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_id': person.id,
                'person_type': person.type,
                'mode': 'EDIT',
                'is_partner': True,
                'data': {
                    'name': 'New person name',
                },
            },
            is_admin=False,
        )

        if w_role or w_flag:
            hm.assert_that(res.status_code, hm.equal_to(http.OK))
            self.test_session.refresh(person)
            assert person.name == 'New person name'

        else:
            hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))
            hm.assert_that(
                res.get_json(),
                hm.has_entries({'error': 'PERMISSION_DENIED'}),
            )


@pytest.mark.permissions
class TestSetPersonPermission(TestCaseApiAppBase):
    BASE_API = '/v1/person/set-person'

    @pytest.mark.parametrize(
        'match_client',
        [
            pytest.param(True, id='right client'),
            pytest.param(False, id='wrong client'),
            pytest.param(None, id='wo role'),
        ],
    )
    @pytest.mark.parametrize(
        'is_admin_request',
        [True, False],
    )
    def test_role_w_client(self, admin_role, edit_person_role, client, match_client, is_admin_request):
        roles = [admin_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((edit_person_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}))
        security.set_roles(roles)

        person = create_person(client=client)

        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_id': person.id,
                'person_type': 'ph',
                'data': {'lname': 'Snout 123456'},
            },
            is_admin=is_admin_request,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))

        if match_client:
            self.test_session.refresh(person)
            assert person.lname == 'Snout 123456'

    @pytest.mark.parametrize(
        'match_client',
        [
            pytest.param(True, id='person belongs to client'),
            pytest.param(False, id='person does not belong to client'),
        ],
    )
    def test_client_ui(self, client, match_client):
        security.set_roles([])
        security.set_passport_client(client if match_client else create_client())

        person = create_person(client=client)

        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_id': person.id,
                'person_type': 'ph',
                'data': {'lname': 'Snout 123456'},
            },
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))

        if match_client:
            self.test_session.refresh(person)
            assert person.lname == 'Snout 123456'
