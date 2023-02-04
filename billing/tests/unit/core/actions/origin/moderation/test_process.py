import logging
from datetime import timedelta
from uuid import uuid4

import pytest

from sendr_interactions.clients.startrek.entities import Issue, IssueResolution, IssueStatus
from sendr_interactions.clients.startrek.exceptions import NotFoundStartrekError
from sendr_pytest.matchers import close_to_datetime
from sendr_utils import utcnow

from hamcrest import assert_that, has_entries, has_item, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.finalize import (
    FinalizeOriginModerationAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.process import (
    ProcessOriginModerationsAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.pay_backend.put_merchant import PayBackendPutMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PutMerchantDataError
from billing.yandex_pay_admin.yandex_pay_admin.interactions.startrek import StartrekClient
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration


@pytest.fixture(autouse=True)
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(origin_id=uuid4(), origin='https://a.test', merchant_id=merchant.merchant_id)
    )


@pytest.fixture
async def origin_moderation(storage, origin):
    return await storage.origin_moderation.create(
        OriginModeration(
            origin_moderation_id=uuid4(), origin_id=origin.origin_id, revision=origin.revision, ticket='TICKET-1'
        )
    )


@pytest.fixture
def issue(rands):
    return Issue(
        id=rands(),
        key='TICKET-1',
        status=IssueStatus(id='3', key='closed'),
        resolution=IssueResolution(id='26', key='successful'),
    )


@pytest.fixture(autouse=True)
def mock_get_issue(mocker, issue):
    return mocker.patch.object(StartrekClient, 'get_issue_info', mocker.AsyncMock(return_value=issue))


@pytest.fixture
def mock_put_merchant(mock_action):
    return mock_action(PayBackendPutMerchantAction)


@pytest.mark.usefixtures('mock_put_merchant')
class TestProcessOriginModerationsSucceeds:
    @pytest.mark.asyncio
    async def test_moderation_approved(self, origin_moderation, merchant, mock_put_merchant, storage):
        await ProcessOriginModerationsAction().run()

        mock_put_merchant.assert_run_once_with(merchant=merchant)

        loaded_moderation = await storage.origin_moderation.get(origin_moderation.origin_moderation_id)
        assert_that(
            loaded_moderation,
            has_properties(
                resolved=True,
                approved=True,
            ),
        )

    @pytest.mark.asyncio
    async def test_moderation_declined(self, issue, origin_moderation, mock_put_merchant, merchant, storage):
        issue.resolution.key = 'declined'

        await ProcessOriginModerationsAction().run()

        mock_put_merchant.assert_run_once_with(merchant=merchant)

        loaded_moderation = await storage.origin_moderation.get(origin_moderation.origin_moderation_id)
        assert_that(
            loaded_moderation,
            has_properties(
                resolved=True,
                approved=False,
            ),
        )

    @pytest.mark.asyncio
    async def test_processing_logged(self, origin_moderation, partner, dummy_logs, storage):
        await ProcessOriginModerationsAction().run()

        loaded_moderation = await storage.origin_moderation.get(origin_moderation.origin_moderation_id)

        logs = dummy_logs()
        assert_that(
            logs,
            has_item(
                has_properties(
                    message='ORIGIN_MODERATION_PROCESSED',
                    levelno=logging.INFO,
                    _context=has_entries(origin_moderation=loaded_moderation),
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_no_moderations_available_for_processing(self, origin_moderation, storage, mock_action):
        origin_moderation.ignored = True
        await storage.origin_moderation.save(origin_moderation)
        mock_finalize = mock_action(FinalizeOriginModerationAction)

        await ProcessOriginModerationsAction().run()

        mock_finalize.assert_not_run()


class TestProcessOriginModerationsFails:
    @pytest.fixture(autouse=True)
    def mock_finalization_cadence(self, mocker):
        return mocker.patch.object(ProcessOriginModerationsAction, 'finalization_cadence_sec', 120)

    @pytest.mark.asyncio
    async def test_put_merchant_fails(
        self,
        origin_moderation,
        partner,
        dummy_logs,
        storage,
        mock_action,
        merchant,
        mock_finalization_cadence,
    ):
        mock_put_merchant = mock_action(PayBackendPutMerchantAction, side_effect=PutMerchantDataError)

        await ProcessOriginModerationsAction().run()

        mock_put_merchant.assert_run_once_with(merchant=merchant)

        loaded_moderation = await storage.origin_moderation.get(origin_moderation.origin_moderation_id)
        expected_finalize_at = utcnow() + timedelta(seconds=mock_finalization_cadence)
        assert_that(
            loaded_moderation,
            has_properties(
                resolved=False,
                approved=False,
                finalize_at=close_to_datetime(expected_finalize_at, delta=timedelta(seconds=10)),
            ),
        )

        assert_that(
            dummy_logs(),
            has_item(
                has_properties(
                    message='ORIGIN_MODERATION_FINALIZATION_FAILED',
                    levelno=logging.ERROR,
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_moderation_not_resolved(
        self,
        issue,
        origin_moderation,
        mock_put_merchant,
        mock_finalization_cadence,
        storage,
    ):
        issue.status.key = 'open'

        await ProcessOriginModerationsAction().run()

        mock_put_merchant.assert_not_run()

        loaded_moderation = await storage.origin_moderation.get(origin_moderation.origin_moderation_id)
        expected_finalize_at = utcnow() + timedelta(seconds=mock_finalization_cadence)
        assert_that(
            loaded_moderation,
            has_properties(
                resolved=False,
                approved=False,
                finalize_at=close_to_datetime(expected_finalize_at, delta=timedelta(seconds=10)),
            ),
        )

    @pytest.mark.asyncio
    async def test_issue_not_found(
        self,
        origin_moderation,
        mock_put_merchant,
        mock_finalization_cadence,
        dummy_logs,
        storage,
        mocker,
    ):
        mocker.patch.object(
            StartrekClient,
            'get_issue_info',
            side_effect=NotFoundStartrekError(method='get', service=StartrekClient.SERVICE),
        )

        await ProcessOriginModerationsAction().run()

        mock_put_merchant.assert_not_run()

        loaded_moderation = await storage.origin_moderation.get(origin_moderation.origin_moderation_id)
        expected_finalize_at = utcnow() + timedelta(seconds=mock_finalization_cadence)
        assert_that(
            loaded_moderation,
            has_properties(
                resolved=False,
                approved=False,
                finalize_at=close_to_datetime(expected_finalize_at, delta=timedelta(seconds=10)),
            ),
        )

        assert_that(
            dummy_logs(),
            has_item(
                has_properties(
                    message='ORIGIN_MODERATION_FINALIZATION_DECLINED',
                    levelno=logging.WARNING,
                )
            ),
        )
