from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.integration.delete import DeleteIntegrationAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.integration.upsert import UpsertIntegrationAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    DuplicateDeployedIntegrationError,
    DuplicateMerchantPSPPairIntegrationError,
    IntegrationNotFoundError,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration


@pytest.fixture
def integration_id():
    return uuid4()


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def psp_id():
    return uuid4()


@pytest.fixture
def url(integration_id):
    return f'/api/internal/v1/integrations/{integration_id}'


@pytest.fixture
def now():
    return utcnow()


class TestPatchIntegration:
    @pytest.fixture
    def mock_integration(self, integration_id, merchant_id, psp_id, now):
        return Integration(
            integration_id=integration_id,
            merchant_id=merchant_id,
            psp_id=psp_id,
            status=IntegrationStatus.READY,
            creds='secret',
            created=now,
            updated=now,
        )

    @pytest.fixture(autouse=True)
    def mock_upsert_action(self, mock_action, mock_integration):
        return mock_action(UpsertIntegrationAction, mock_integration)

    @pytest.mark.asyncio
    async def test_success(
        self, app, url, integration_id, merchant_id, psp_id, mock_upsert_action, now
    ):
        data = {'merchant_id': str(merchant_id), 'psp_id': str(psp_id)}

        r = await app.patch(
            url,
            json=data,
            raise_for_status=True,
        )
        body = await r.json()

        assert_that(
            body,
            equal_to(
                {
                    'data': {
                        'integration': {
                            'integration_id': str(integration_id),
                            'merchant_id': str(merchant_id),
                            'psp_id': str(psp_id),
                            'status': IntegrationStatus.READY.value,
                            'enabled': True,
                            'created': now.isoformat(),
                            'updated': now.isoformat(),
                        }
                    },
                    'code': 200,
                    'status': 'success',
                }
            )
        )
        mock_upsert_action.assert_called_once_with(
            integration_id=integration_id,
            merchant_id=merchant_id,
            psp_id=psp_id,
        )

    @pytest.mark.asyncio
    async def test_invalid_integration_id(self, app, merchant_id, psp_id):
        url = '/api/internal/v1/integrations/bad_uuid'
        data = {'merchant_id': str(merchant_id), 'psp_id': str(psp_id)}

        r = await app.patch(url, json=data)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'code': 400,
                    'data': {
                        'params': {'integration_id': ['Not a valid UUID.']},
                        'message': 'BAD_FORMAT',
                    },
                    'status': 'fail',
                }
            ),
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('key', ['merchant_id', 'psp_id'])
    async def test_body_missing_required_field(self, app, url, merchant_id, psp_id, key):
        data = {'merchant_id': str(merchant_id), 'psp_id': str(psp_id)}
        data.pop(key)

        r = await app.patch(url, json=data)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'code': 400,
                    'data': {
                        'params': {key: ['Missing data for required field.']},
                        'message': 'BAD_FORMAT',
                    },
                    'status': 'fail',
                }
            ),
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'error',
        [DuplicateMerchantPSPPairIntegrationError, DuplicateDeployedIntegrationError],
    )
    async def test_action_error(
        self, app, url, merchant_id, psp_id, mock_upsert_action, error
    ):
        mock_upsert_action.side_effect = error
        data = {'merchant_id': str(merchant_id), 'psp_id': str(psp_id)}

        r = await app.patch(url, json=data)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'code': 400,
                    'status': 'fail',
                    'data': {'message': error.message},
                }
            ),
        )


class TestDeleteIntegration:
    @pytest.fixture
    def mock_delete_action(self, mock_action):
        return mock_action(DeleteIntegrationAction)

    @pytest.mark.asyncio
    async def test_success(
        self, app, url, integration_id, mock_delete_action
    ):
        r = await app.delete(
            url,
            raise_for_status=True,
        )

        assert_that(
            await r.json(),
            equal_to(
                {
                    'code': 200,
                    'status': 'success',
                }
            ),
        )
        mock_delete_action.assert_called_once_with(integration_id=integration_id)

    @pytest.mark.asyncio
    async def test_integration_not_found(
        self, app, url, integration_id, mock_delete_action
    ):
        mock_delete_action.side_effect = IntegrationNotFoundError

        r = await app.delete(url)

        assert_that(
            await r.json(),
            equal_to(
                {
                    'code': 404,
                    'status': 'fail',
                    'data': {'message': 'INTEGRATION_NOT_FOUND'},
                }
            ),
        )
