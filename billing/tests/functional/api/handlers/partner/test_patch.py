from uuid import UUID

import pytest

from sendr_utils import json_value

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.fixture
def create_partner(app, partner_entity):
    default_partner_data = json_value(partner_entity)

    async def _inner(data=default_partner_data):
        r = await app.post(
            '/api/web/v1/partners',
            json=data,
            raise_for_status=True,
        )
        return (await r.json())['data']

    return _inner


@pytest.mark.asyncio
async def test_updates_partner_name(create_partner, app, rands):
    name = rands()
    created_partner = await create_partner()
    partner_id = UUID(created_partner['partner_id'])

    await app.patch(
        f'/api/web/v1/partners/{partner_id}',
        json={'name': name},
        raise_for_status=True,
    )

    updated_partner = (await (await app.get(f'/api/web/v1/partners/{partner_id}')).json())['data']
    expected_updated_partner = created_partner | {'name': name}
    assert_that(updated_partner, equal_to(expected_updated_partner))


@pytest.mark.asyncio
async def test_updates_registration_data(create_partner, app, rands):
    created_partner = await create_partner({'name': 'name', 'registration_data': {}})
    partner_id = UUID(created_partner['partner_id'])

    await app.patch(
        f'/api/web/v1/partners/{partner_id}',
        json={'name': 'name', 'registration_data': {'full_company_name': 'whats in a name'}},
        raise_for_status=True,
    )

    updated_partner = (await (await app.get(f'/api/web/v1/partners/{partner_id}')).json())['data']
    expected_updated_partner = created_partner | {
        'registration_data': created_partner['registration_data'] | {'full_company_name': 'whats in a name'}
    }
    assert_that(updated_partner, equal_to(expected_updated_partner))


@pytest.mark.asyncio
async def test_cannot_update_already_filled_registration_data_item(create_partner, app, rands):
    created_partner = await create_partner(
        {'name': 'name', 'registration_data': {'full_company_name': 'whats in a name'}}
    )
    partner_id = UUID(created_partner['partner_id'])

    r = await app.patch(
        f'/api/web/v1/partners/{partner_id}',
        json={'registration_data': {'full_company_name': 'whats in a name'}},
    )

    assert_that(r.status, equal_to(400))
    not_updated_partner = (await (await app.get(f'/api/web/v1/partners/{partner_id}')).json())['data']
    assert_that(not_updated_partner, equal_to(created_partner))


@pytest.mark.asyncio
async def test_viewer_cannot_update_partner(storage, create_partner, app, uid):
    data = await create_partner()
    partner_id = UUID(data['partner_id'])
    role = await storage.role.get(partner_id, uid)
    role.role = RoleType.VIEWER
    await storage.role.save(role)

    r = await app.patch(
        f'/api/web/v1/partners/{partner_id}',
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
                'data': {'message': 'FORBIDDEN'},
            }
        ),
    )
