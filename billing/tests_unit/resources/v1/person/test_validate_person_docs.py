# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import io
import pickle
import hamcrest as hm
import http.client as http

from balance import (
    constants as cst,
    mapper,
)

from brest.core.application import get_or_create_yb_app
from brest.core.tests import security
from yb_snout_api.resources import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role


@pytest.fixture(name='edit_person_role')
def create_edit_person_role():
    return create_role(
        (cst.PermissionCode.EDIT_PERSONS, {cst.ConstraintTypes.client_batch_id: None}),
    )


class TestValidatePerson(TestCaseApiAppBase):
    BASE_API = '/v1/person/validate-docs'

    def test_wo_file(self, person):
        res = self.test_client.secure_post(
            self.BASE_API,
            {'person_id': person.id},
            headers={'Content-Type': enums.Mimetype.FILES.value},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'file': hm.contains(hm.has_entries({'error': 'REQUIRED_FIELD_VALIDATION_ERROR'})),
                }),
            }),
        )

    def test_invalid_content_type(self, person):
        res = self.test_client.secure_post(
            self.BASE_API,
            {
                'person_id': person.id,
                'file': (io.BytesIO(b"abcdef"), 'test.jpg'),
            },
            headers={'Content-Type': enums.Mimetype.FORM.value},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'file': hm.contains(hm.has_entries({'error': 'REQUIRED_FIELD_VALIDATION_ERROR'})),
                }),
            }),
        )

    def test_send_email(self):
        person = create_person(type='ph')
        res = self.test_client.secure_post(
            self.BASE_API,
            {
                'person_id': person.id,
                'file': (io.BytesIO(b'abcdef'), 'test.jpg'),
            },
            headers={'Content-Type': enums.Mimetype.FILES.value},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        app = get_or_create_yb_app()
        mako = app.mako_renderer
        hm.assert_that(
            mako,
            hm.has_properties({
                'tmpl_name': 'verified_docs/request.mako',
                'kwargs': hm.has_entries({
                    'person': person,
                    'person_type': 'Физ. лицо',
                }),
            }),
        )

        email = (
            self.test_session
            .query(mapper.EmailMessage)
            .filter(
                mapper.EmailMessage.opcode == cst.GENERIC_MESSAGE_CREATOR_MESSAGE_OPCODE,
                mapper.EmailMessage.object_id == person.id,
            )
            .one()
        )
        assert email.recepient_name == 'Яндекс.Баланс'
        assert email.recepient_address == 'info@balance.yandex.ru'

        data = pickle.loads(email.data)
        hm.assert_that(
            data,
            hm.contains(
                'Проверка документов плательщика "%s" (%s)' % (person.sensible_name, 'Физ. лицо'),
                'Rendered text',
                hm.contains(person.email, person.sensible_name),
                hm.contains(
                    hm.contains('test.jpg', 'image/jpeg', b'abcdef'),
                ),
            ),
        )


@pytest.mark.permissions
class TestValidatePersonPermission(TestCaseApiAppBase):
    BASE_API = '/v1/person/validate-docs'

    @pytest.mark.parametrize(
        'match_client',
        [
            pytest.param(True, id='right client'),
            pytest.param(False, id='wrong client'),
            pytest.param(None, id='wo role'),
        ],
    )
    def test_role_w_client(self, admin_role, edit_person_role, client, match_client):
        roles = [admin_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((edit_person_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}))
        security.set_roles(roles)

        person = create_person(client=client)

        res = self.test_client.secure_post(
            self.BASE_API,
            {
                'person_id': person.id,
                'file': (io.BytesIO(b"abcdef"), 'test.jpg'),
            },
            headers={'Content-Type': enums.Mimetype.FILES.value},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))

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

        res = self.test_client.secure_post(
            self.BASE_API,
            {
                'person_id': person.id,
                'file': (io.BytesIO(b"abcdef"), 'test.jpg'),
            },
            headers={'Content-Type': enums.Mimetype.FILES.value},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))
