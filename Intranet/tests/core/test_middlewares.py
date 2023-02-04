import datetime
import pytest
from fastapi import Request
from mock import patch
from typing import Any

from intranet.trip.src.config import settings
from intranet.trip.src.models import User
from intranet.trip.src.api.schemas import UserResponse
from intranet.trip.src.middlewares import auth

from ..conftest import mocked_create_redis_pool, MockedUnitOfWork, RollbackTestQueries, Factory
from ..helpers import TestClient


pytestmark = pytest.mark.asyncio
complement_user_contacts_path = 'intranet.trip.src.logic.persons.complement_user_contacts'


class MockRequest(Request):
    def __init__(self):
        pass


async def user_data_from_bb_mock(request=MockRequest()) -> dict[str, Any]:
    return await auth.DevMiddleware.get_user_data_from_bb(request)


async def get_user_by_uid_mock(request, uid):
    mocked_user_data_from_bb = await user_data_from_bb_mock(request)
    return await auth.DevMiddleware.get_user_or_error(
        request,
        mocked_user_data_from_bb,
    )


async def complement_user_contacts_mock(user):
    return user


@pytest.fixture
async def client_without_middleware(request, app):
    def fin():
        app.user_middleware = user_middleware
        app.middleware_stack = app.build_middleware_stack()

    user_middleware = app.user_middleware.copy()
    app.user_middleware = []
    app.middleware_stack = app.build_middleware_stack()

    request.addfinalizer(fin)
    with patch('intranet.trip.src.main.create_redis_pool', mocked_create_redis_pool):
        async with TestClient(app=app) as client:
            yield client


@pytest.fixture
async def yateam_client(client_without_middleware):
    client_without_middleware.app.add_middleware(auth.YaTeamMiddleware)
    yield client_without_middleware


@pytest.fixture
async def yandex_client(client_without_middleware):
    client_without_middleware.app.add_middleware(auth.YandexMiddleware)
    yield client_without_middleware


@patch('intranet.trip.src.middlewares.auth.YaTeamMiddleware.get_user_data_from_bb')
async def test_yateam_auth_middleware(bb_user_data_mocked, yateam_client):
    bb_user_data_mocked.return_value = await user_data_from_bb_mock()
    # test without person
    with patch(complement_user_contacts_path, complement_user_contacts_mock):
        response = await yateam_client.get('api/meta/')
    data = response.json()
    assert response.status_code == 403
    assert data['detail'] == 'FORBIDDEN'

    # test with person
    with (
        patch('intranet.trip.src.middlewares.auth.get_user_by_uid', get_user_by_uid_mock),
        patch(complement_user_contacts_path, complement_user_contacts_mock),
    ):
        response = await yateam_client.get('api/meta/')
    assert response.status_code == 200


@patch('intranet.trip.src.middlewares.auth.YandexMiddleware.get_user_data_from_bb')
async def test_yandex_auth_middleware_unauthorized(bb_user_data_mocked, yandex_client):
    """
    Unauthorized user in blackbox
    """
    bb_user_data_mocked.return_value = {}
    with patch(complement_user_contacts_path, complement_user_contacts_mock):
        response = await yandex_client.get('api/meta/')
    data = response.json()
    assert response.status_code == 401
    assert data['detail'] == 'UNAUTHORIZED'


@patch('intranet.trip.src.api.endpoints.token.get_csrf_token')
@patch('intranet.trip.src.middlewares.auth.get_user_by_uid')
@patch('intranet.trip.src.middlewares.auth.YandexMiddleware.get_user_data_from_bb')
async def test_yandex_auth_middleware_not_registered(
    bb_user_data_mocked,
    get_user_by_uid_mocked,
    get_csrf_token_mocked,
    yandex_client,
):
    """
    Not registered user in Trip
    """
    bb_user_data = await user_data_from_bb_mock()
    bb_user_data_mocked.return_value = bb_user_data
    get_user_by_uid_mocked.return_value = None
    get_csrf_token_mocked.return_value = {'token': 'xxx'}

    # Came for a token
    response = await yandex_client.get(settings.TOKEN_URL)
    assert response.status_code == 200

    # Test that user is not registered
    with patch(complement_user_contacts_path, complement_user_contacts_mock):
        response = await yandex_client.get('api/meta/')
    data = response.json()
    assert response.status_code == 401
    assert data['detail'] == 'NOT_REGISTERED'


registered_user = User(
    uid='1',
    person_id=1,
    login='',
    timezone='UTC',
    language='ru',
    user_ticket='user:ticket',
    first_name='Биба',
    last_name='Боба',
    is_dismissed=False,
    is_active=True,
    is_coordinator=False,
    is_yandex_employee=False,
    email_confirmed_at=datetime.datetime.now(),
    company_id=1,
)

not_active_user = registered_user.copy()
not_active_user.is_active = False

dismissed_user = registered_user.copy()
dismissed_user.is_dismissed = True

not_confirmed_email_user = registered_user.copy()
not_confirmed_email_user.email_confirmed_at = None

empty_company_user = registered_user.copy()
empty_company_user.company_id = None

user_response = UserResponse(
    **registered_user.dict(
        exclude={'company', 'company_id', 'email_confirmed_at', 'is_active', 'user_ticket'},
    ),
)


@pytest.mark.parametrize('user, response_code, response_body', (
    (not_active_user, 403, {'detail': 'USER_DEACTIVATED'}),
    (dismissed_user, 403, {'detail': 'USER_DEACTIVATED'}),
    (not_confirmed_email_user, 401, {'detail': 'NOT_CONFIRMED_EMAIL'}),
    (empty_company_user, 401, {'detail': 'EMPTY_COMPANY'}),
    (registered_user, 200, user_response),
))
@patch('intranet.trip.src.middlewares.auth.get_user_by_uid')
@patch('intranet.trip.src.middlewares.auth.YandexMiddleware.get_user_data_from_bb')
async def test_yandex_auth_middleware_registered(
    bb_user_data_mocked,
    get_user_by_uid_mocked,
    yandex_client,
    user,
    response_code,
    response_body,
):
    """
    User is registered
    """
    bb_user_data = await user_data_from_bb_mock()
    bb_user_data_mocked.return_value = bb_user_data
    get_user_by_uid_mocked.return_value = user

    with patch(complement_user_contacts_path, complement_user_contacts_mock):
        response = await yandex_client.get('api/meta/')
    assert response.status_code == response_code
    data = response.json()
    assert data == response_body


@pytest.fixture
async def uow(yandex_client):
    redis = await mocked_create_redis_pool()

    async with yandex_client.app.state.db.acquire() as conn:
        uow = MockedUnitOfWork(conn=conn, redis=redis)

        yandex_client.app.state._global_uow = uow

        try:
            async with uow:
                yield uow
                raise RollbackTestQueries
        except RollbackTestQueries:
            pass
        finally:
            yandex_client.app.state._global_uow = None


@pytest.fixture
async def f(uow):
    yield Factory(conn=uow._conn)


@pytest.mark.parametrize('is_active, status_code, detail', (
    (True, 401, 'NOT_CONFIRMED_EMAIL'),
    (False, 403, 'USER_DEACTIVATED'),
))
@patch('intranet.trip.src.middlewares.auth.get_user_by_uid')
@patch('intranet.trip.src.middlewares.auth.YandexMiddleware.get_user_data_from_bb')
async def test_empty_company_person_response(
    bb_user_data_mocked,
    get_user_by_uid_mocked,
    is_active,
    status_code,
    detail,
    uow,
    f,
    yandex_client,
):
    uid = '21435'
    await f.create_person_without_company(person_id=int(uid), uid=uid, is_active=is_active)
    bb_user_data = await user_data_from_bb_mock()
    bb_user_data['uid']['value'] = uid
    bb_user_data_mocked.return_value = bb_user_data
    user = await uow.persons.get_user_by_uid(uid)
    get_user_by_uid_mocked.return_value = user

    with patch(complement_user_contacts_path, complement_user_contacts_mock):
        response = await yandex_client.get('api/meta/')
    assert response.status_code == status_code
    data = response.json()
    assert data['detail'] == detail
