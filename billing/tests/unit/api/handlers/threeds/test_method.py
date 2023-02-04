from uuid import UUID

import pytest

from sendr_pytest.mocks import mock_action

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.threeds.method import ThreeDSMethodAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.enums import ThreeDSMethodHTMLDocumentID

_dummy_use_fixture = [mock_action]
TRANSACTION_ID = '1d94f624-1e41-4e05-9653-d944c347a672'


@pytest.mark.asyncio
async def test_returned(public_app, mock_user_authentication):
    res = await public_app.get(f'/3ds/method/{TRANSACTION_ID}')

    assert_that(res.status, equal_to(200))
    assert_that(
        await res.text(),
        equal_to('the-html'),
    )


@pytest.mark.parametrize(
    'path_suffix, expected_document_id',
    (
        ('', None),
        ('/index', ThreeDSMethodHTMLDocumentID.INDEX),
        ('/acs', ThreeDSMethodHTMLDocumentID.ACS),
        ('/3ds-server', ThreeDSMethodHTMLDocumentID.THREEDS_SERVER),
    )
)
@pytest.mark.asyncio
async def test_calls_action(
    public_app, path_suffix, mock_tds_method_action, mock_user_authentication, expected_document_id, entity_auth_user
):
    await public_app.get(f'/3ds/method/{TRANSACTION_ID}{path_suffix}', raise_for_status=True)

    mock_tds_method_action.assert_run_once_with(
        user=entity_auth_user,
        transaction_id=UUID(TRANSACTION_ID),
        html_document_id=expected_document_id,
    )


@pytest.fixture(autouse=True)
def mock_tds_method_action(mock_action):
    return mock_action(ThreeDSMethodAction, return_value='the-html')
