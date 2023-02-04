import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.tokenization_acceptance.create import CreateTokenizationAcceptanceAction
from billing.yandex_pay.yandex_pay.core.actions.tokenization_acceptance.get import GetTokenizationAcceptanceAction
from billing.yandex_pay.yandex_pay.core.entities.tokenization_acceptance import TokenizationAcceptance
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import CoreDuplicationError
from billing.yandex_pay.yandex_pay.tests.entities import APIKind


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/tokenization/acceptance',
        APIKind.MOBILE: '/api/mobile/v1/tokenization/acceptance',
    }[api_kind]


@pytest.fixture(autouse=True)
def mock_authentication(mocker, session_uid):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=User(session_uid)))


@pytest.fixture
def acceptance_result(session_uid) -> TokenizationAcceptance:
    return TokenizationAcceptance(
        uid=session_uid,
        user_ip='test_ip',
        accept_date=utcnow(),
    )


@pytest.fixture
def session_uid():
    return 10


@pytest.fixture
def mock_create_acceptance(mock_action, acceptance_result):
    return mock_action(CreateTokenizationAcceptanceAction, acceptance_result)


@pytest.fixture
def mock_create_acceptance_duplication(mock_action):
    return mock_action(CreateTokenizationAcceptanceAction, CoreDuplicationError)


@pytest.fixture
def mock_get_acceptance(mock_action, acceptance_result):
    return mock_action(GetTokenizationAcceptanceAction, acceptance_result)


@pytest.fixture
def mock_get_acceptance_not_exist(mock_action):
    return mock_action(GetTokenizationAcceptanceAction, None)


@pytest.fixture
def expected_json_body(acceptance_result):
    return {
        'code': 200,
        'status': 'success',
        'data':
            {
                'accepted': True,
                'accept_date': acceptance_result.accept_date.isoformat(),
            }
    }


@pytest.fixture
def expected_conflict_json_body():
    return {
        'code': 409,
        'status': 'fail',
        'data':
            {
                'message': 'ALREADY_EXIST',
            }
    }


@pytest.fixture
def expected_no_acceptance_json_body():
    return {
        'code': 200,
        'status': 'success',
        'data':
            {
                'accepted': False,
            }
    }


@pytest.mark.asyncio
async def test_handler_should_respond_create_result_from_action(
    app,
    api_url,
    expected_json_body,
    mock_create_acceptance,
):
    r = await app.post(api_url)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_json_body))


@pytest.mark.asyncio
async def test_handler_should_call_create_action_with_session_uid_and_user_ip(
    app,
    api_url,
    mock_create_acceptance,
    session_uid,
):
    ip = 'ip'
    await app.post(
        api_url,
        headers={
            'X-Real-IP': ip
        },
    )

    mock_create_acceptance.assert_called_once_with(uid=session_uid, user_ip=ip)


@pytest.mark.asyncio
async def test_handler_should_respond_conflict_on_duplication(
    app,
    api_url,
    expected_conflict_json_body,
    mock_create_acceptance_duplication,
):
    r = await app.post(api_url)
    json_body = await r.json()

    assert_that(r.status, equal_to(409))
    assert_that(json_body, equal_to(expected_conflict_json_body))


@pytest.mark.asyncio
async def test_handler_should_respond_get_result_from_action(
    app,
    api_url,
    expected_json_body,
    mock_get_acceptance,
):
    r = await app.get(api_url)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_json_body))


@pytest.mark.asyncio
async def test_handler_should_call_get_action_with_session_uid(
    app,
    api_url,
    mock_get_acceptance,
    session_uid,
):
    await app.get(api_url)

    mock_get_acceptance.assert_called_once_with(uid=session_uid)


@pytest.mark.asyncio
async def test_handler_should_respond_not_accepted_if_acceptance_not_exist(
    app,
    api_url,
    expected_no_acceptance_json_body,
    mock_get_acceptance_not_exist,
):
    r = await app.get(api_url)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_no_acceptance_json_body))
