import uuid
from dataclasses import asdict, replace

import pytest

from sendr_interactions.clients.spark_suggest import SparkSuggestItem

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.get import GetPartnerAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.suggest import PartnerSuggestAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.update import UpdatePartnerAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PartnerNotFoundError, RolePermissionError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Contact, Partner, RegistrationData
from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import check_error


@pytest.fixture
def partner_entity():
    return Partner(
        partner_id=uuid.uuid4(),
        name='partner_name',
        registration_data=RegistrationData(
            contact=Contact(
                email='email@test',
                phone='+1(000)555-0100',
                first_name='John',
                last_name='Doe',
                middle_name='Татьянович',
            ),
            tax_ref_number='0123 АБ',
        ),
    )


class TestUpdatePartner:
    @pytest.mark.asyncio
    async def test_patch(self, role, partner_entity, user, app, mock_action):
        name = 'new name'
        updated = replace(partner_entity, name=name, role=role)
        action_mock = mock_action(UpdatePartnerAction, updated)

        r = await app.patch(f'/api/web/v1/partners/{partner_entity.partner_id}', json={'name': name})

        assert_that(r.status, equal_to(200))
        data = await r.json()
        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'name': name,
                        'registration_data': asdict(partner_entity.registration_data),
                        'partner_id': str(partner_entity.partner_id),
                        'user_role': 'owner',
                    },
                }
            ),
        )

        action_mock.assert_run_once_with(user=user, partner_id=partner_entity.partner_id, name=name)

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'full_path, value, expected_error',
        (
            pytest.param('name', None, 'Field may not be null.', id='name_not_null'),
            pytest.param('name', 123, 'Not a valid string.', id='name_string_required'),
        ),
    )
    async def test_patch_validate_payload(self, partner_entity, app, mock_action, full_path, value, expected_error):
        action_mock = mock_action(UpdatePartnerAction)

        r = await app.patch(
            f'/api/web/v1/partners/{partner_entity.partner_id}',
            json={'name': value},
        )

        await check_error(r, full_path, expected_error)

        action_mock.assert_not_run()

    @pytest.mark.asyncio
    async def test_patch_forbidden(self, partner_entity, app, mock_action):
        action_mock = mock_action(UpdatePartnerAction, side_effect=RolePermissionError)

        r = await app.patch(
            f'/api/web/v1/partners/{partner_entity.partner_id}',
            json={'name': 'any'},
        )

        assert_that(r.status, equal_to(403))
        data = await r.json()
        assert_that(
            data,
            equal_to(
                {
                    'status': 'fail',
                    'code': 403,
                    'data': {
                        'message': 'FORBIDDEN',
                    },
                }
            ),
        )

        action_mock.assert_run_once()

    @pytest.mark.asyncio
    async def test_partner_not_found(self, partner_entity, app, mock_action):
        action_mock = mock_action(UpdatePartnerAction, side_effect=PartnerNotFoundError)

        r = await app.patch(
            f'/api/web/v1/partners/{partner_entity.partner_id}',
            json={'name': 'any'},
        )

        assert_that(r.status, equal_to(404))
        data = await r.json()
        assert_that(
            data,
            equal_to(
                {
                    'status': 'fail',
                    'code': 404,
                    'data': {
                        'message': 'PARTNER_NOT_FOUND',
                    },
                }
            ),
        )

        action_mock.assert_run_once()


class TestGetPartner:
    @pytest.mark.asyncio
    async def test_get(self, user, role, partner_entity, app, mock_action):
        partner_entity.role = role
        action_mock = mock_action(GetPartnerAction, partner_entity)

        r = await app.get(f'/api/web/v1/partners/{partner_entity.partner_id}')

        assert_that(r.status, equal_to(200))
        data = await r.json()
        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'name': partner_entity.name,
                        'registration_data': asdict(partner_entity.registration_data),
                        'partner_id': str(partner_entity.partner_id),
                        'user_role': 'owner',
                    },
                }
            ),
        )

        action_mock.assert_run_once_with(user=user, partner_id=partner_entity.partner_id)

    @pytest.mark.asyncio
    async def test_get_partner_not_found(self, partner_entity, app, mock_action):
        action_mock = mock_action(GetPartnerAction, side_effect=PartnerNotFoundError)

        r = await app.get(f'/api/web/v1/partners/{partner_entity.partner_id}')

        assert_that(r.status, equal_to(404))
        data = await r.json()
        assert_that(
            data,
            equal_to(
                {
                    'status': 'fail',
                    'code': 404,
                    'data': {
                        'message': 'PARTNER_NOT_FOUND',
                    },
                }
            ),
        )

        action_mock.assert_run_once()


class TestPartnerSuggest:
    @pytest.fixture
    def suggest_list(self):
        return [
            SparkSuggestItem(
                spark_id=i,
                name=f'suggested_name_{i}',
                full_name=f'suggested_full_name_{i}',
                inn=f'suggested_inn_{i}',
                ogrn=f'suggested_ogrn_{i}',
                address=f'suggested_address_{i}',
                leader_name=f'suggested_leader_name_{i}',
                region_name=f'suggested_region_name_{i}',
            )
            for i in range(3)
        ]

    @pytest.mark.asyncio
    async def test_merchant_suggest_response(self, app, mock_action, suggest_list):
        query = '  ИП "Иванов!" #047...    '  # has trailing spaces and punctuations
        action = mock_action(PartnerSuggestAction, suggest_list)

        response = await app.get('/api/web/v1/partners/suggest', params={'query': query}, raise_for_status=True)

        body = await response.json()
        assert_that(
            body['data']['items'],
            equal_to([item.__dict__ for item in suggest_list]),
        )
        action.assert_called_once_with(query='ИП Иванов 047')
