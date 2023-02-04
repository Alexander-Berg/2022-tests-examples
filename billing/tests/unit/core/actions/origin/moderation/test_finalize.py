import logging
from uuid import uuid4

import pytest

from sendr_interactions.clients.startrek.entities import Issue, IssueResolution, IssueStatus, TicketResolutions
from sendr_interactions.clients.startrek.exceptions import NotFoundStartrekError

from hamcrest import assert_that, equal_to, has_item, has_properties, is_

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.finalize import (
    FinalizeOriginModerationAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.pay_backend.put_merchant import PayBackendPutMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import (
    OriginModerationFinalizationDeclinedError,
    OriginModerationIssueBadResolutionError,
    OriginModerationIssueNotResolvedError,
)
from billing.yandex_pay_admin.yandex_pay_admin.interactions.startrek import StartrekClient
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration
from billing.yandex_pay_admin.yandex_pay_admin.utils.stats import origin_moderation_bad_final_status


@pytest.fixture(autouse=True)
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(origin_id=uuid4(), origin='https://a.test', merchant_id=merchant.merchant_id)
    )


@pytest.fixture
async def moderation(storage, origin):
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


@pytest.fixture
def mock_get_issue(mocker, issue):
    return mocker.patch.object(StartrekClient, 'get_issue_info', mocker.AsyncMock(return_value=issue))


@pytest.fixture(autouse=True)
def mock_put_merchant(mock_action):
    return mock_action(PayBackendPutMerchantAction)


class TestFinalizeSucceeds:
    @pytest.mark.asyncio
    async def test_moderation_approved(
        self, issue, moderation, merchant, mock_get_issue, dummy_logs, mock_put_merchant
    ):
        assert_that(moderation, has_properties(resolved=False, approved=False))

        finalized = await FinalizeOriginModerationAction(origin_moderation=moderation).run()

        mock_get_issue.assert_awaited_once_with(moderation.ticket)
        assert_that(
            finalized,
            has_properties(
                origin_moderation_id=moderation.origin_moderation_id,
                resolved=True,
                approved=True,
            ),
        )
        [*_, log] = dummy_logs()
        assert_that(
            log,
            has_properties(
                levelno=logging.INFO,
                message='ORIGIN_MODERATION_FINALIZED',
            ),
        )
        mock_put_merchant.assert_run_once_with(merchant=merchant)

    @pytest.mark.asyncio
    async def test_moderation_declined(self, issue, mocker, merchant, moderation, dummy_logs, mock_put_merchant):
        issue.resolution.key = 'declined'
        mock = mocker.patch.object(StartrekClient, 'get_issue_info', mocker.AsyncMock(return_value=issue))
        assert_that(moderation, has_properties(resolved=False, approved=False))

        finalized = await FinalizeOriginModerationAction(origin_moderation=moderation).run()

        mock.assert_awaited_once_with(moderation.ticket)
        assert_that(
            finalized,
            has_properties(
                origin_moderation_id=moderation.origin_moderation_id,
                resolved=True,
                approved=False,
            ),
        )
        [*_, log] = dummy_logs()
        assert_that(
            log,
            has_properties(
                levelno=logging.INFO,
                message='ORIGIN_MODERATION_FINALIZED',
            ),
        )
        mock_put_merchant.assert_run_once_with(merchant=merchant)

    @pytest.mark.asyncio
    async def test_moderation_already_resolved(self, moderation, mock_get_issue, dummy_logs, mock_put_merchant):
        moderation.resolved = True

        finalized = await FinalizeOriginModerationAction(origin_moderation=moderation).run()

        mock_get_issue.assert_not_awaited()
        assert_that(finalized, is_(moderation))
        [log] = dummy_logs()
        assert_that(
            log,
            has_properties(
                levelno=logging.INFO,
                message='ORIGIN_MODERATION_ALREADY_RESOLVED',
            ),
        )
        mock_put_merchant.assert_not_run()


class TestFinalizeFails:
    @pytest.mark.asyncio
    async def test_moderation_ignored(self, moderation, mock_get_issue, dummy_logs, mock_put_merchant):
        moderation.ignored = True

        with pytest.raises(OriginModerationFinalizationDeclinedError):
            await FinalizeOriginModerationAction(origin_moderation=moderation).run()

        mock_get_issue.assert_not_awaited()
        [log] = dummy_logs()
        assert_that(
            log,
            has_properties(
                levelno=logging.ERROR,
                message='ORIGIN_MODERATION_IGNORED',
            ),
        )
        mock_put_merchant.assert_not_run()

    @pytest.mark.asyncio
    async def test_issue_not_found(self, moderation, mock_get_issue, dummy_logs, mock_put_merchant):
        mock_get_issue.side_effect = NotFoundStartrekError(method='get', service=StartrekClient.SERVICE)

        with pytest.raises(OriginModerationFinalizationDeclinedError):
            await FinalizeOriginModerationAction(origin_moderation=moderation).run()

        mock_get_issue.assert_awaited_once_with(moderation.ticket)
        [log] = dummy_logs()
        assert_that(
            log,
            has_properties(
                levelno=logging.ERROR,
                message='ISSUE_NOT_FOUND',
            ),
        )
        mock_put_merchant.assert_not_run()

    @pytest.mark.asyncio
    async def test_issue_is_not_closed(self, moderation, mock_get_issue, issue, dummy_logs, mock_put_merchant):
        issue.status.key = 'resolved'

        with pytest.raises(OriginModerationFinalizationDeclinedError):
            await FinalizeOriginModerationAction(origin_moderation=moderation).run()

        mock_get_issue.assert_awaited_once_with(moderation.ticket)
        assert_that(dummy_logs(), equal_to([]))
        mock_put_merchant.assert_not_run()

    @pytest.mark.asyncio
    async def test_issue_resolution_is_none(self, moderation, mock_get_issue, issue, dummy_logs, mock_put_merchant):
        issue.resolution = None

        with pytest.raises(OriginModerationIssueNotResolvedError):
            await FinalizeOriginModerationAction(origin_moderation=moderation).run()

        mock_get_issue.assert_awaited_once_with(moderation.ticket)
        assert_that(dummy_logs(), equal_to([]))
        mock_put_merchant.assert_not_run()

    @pytest.mark.asyncio
    async def test_issue_resolution_bad_status_logged(
        self, moderation, mock_get_issue, issue, dummy_logs, mock_put_merchant
    ):
        issue.resolution.key = 'dontdo'

        with pytest.raises(OriginModerationIssueBadResolutionError):
            await FinalizeOriginModerationAction(origin_moderation=moderation).run()

        mock_get_issue.assert_awaited_once_with(moderation.ticket)
        [log] = dummy_logs()
        assert_that(
            log,
            has_properties(
                levelno=logging.ERROR,
                message='ORIGIN_MODERATION_ISSUE_BAD_RESOLVE_STATUS',
            ),
        )
        mock_put_merchant.assert_not_run()

    @pytest.mark.asyncio
    async def test_issue_resolution_bad_status_counter_increased(
        self, moderation, mock_get_issue, issue, mock_put_merchant
    ):
        issue.resolution.key = 'dontdo'
        origin_moderation_bad_final_status.remove(TicketResolutions.DONT_DO.casefold())

        with pytest.raises(OriginModerationIssueBadResolutionError):
            await FinalizeOriginModerationAction(origin_moderation=moderation).run()

        assert_that(
            list(origin_moderation_bad_final_status.get()),
            has_item(equal_to(('origin_moderation_bad_final_status_dontdo_summ', 1.0))),
        )
