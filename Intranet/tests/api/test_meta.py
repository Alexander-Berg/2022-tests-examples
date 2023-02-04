import pytest
from fastapi import Request
from mock import patch

from ..core.test_middlewares import user_data_from_bb_mock


pytestmark = pytest.mark.asyncio


@pytest.mark.parametrize('is_coordinator, type_alias, is_yandex_employee, is_ya_team', (
    (True, '6', True, True),
    (True, '6', False, False),
    (False, '13', True, False),
))
async def test_meta_flags(client, is_coordinator, type_alias, is_yandex_employee, is_ya_team):
    user_data_from_bb_mocked = await user_data_from_bb_mock()
    user_data_from_bb_mocked['is_coordinator'] = is_coordinator
    user_data_from_bb_mocked['aliases'] = {'1': 'dev', type_alias: 'alt-login'}

    async def mock_get_user_from_bb(self, request: Request):
        return user_data_from_bb_mocked

    async def mock_complement_user_contacts(user):
        return user

    src = 'intranet.trip.src.'
    with (
        patch(f'{src}middlewares.auth.DevMiddleware.get_user_data_from_bb', mock_get_user_from_bb),
        patch(f'{src}api.endpoints.meta.complement_user_contacts', mock_complement_user_contacts),
        patch(f'{src}api.schemas.user.settings.IS_YA_TEAM', is_ya_team),
    ):
        response = await client.get('api/meta/')
    data = response.json()
    assert data['is_coordinator'] == is_coordinator
    assert data['is_yandex_employee'] == is_yandex_employee
    assert data['is_b2b'] != is_ya_team
    assert data['display_messenger_chats'] == is_ya_team
    assert data['display_tracker_issues'] == is_ya_team
