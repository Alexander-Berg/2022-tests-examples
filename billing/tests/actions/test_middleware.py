from datetime import datetime
from decimal import Decimal
from typing import Any
from unittest.mock import Mock

import arrow
import hamcrest as hm
import pytest

from billing.library.python.calculator.actions import (
    FillCommonTsMiddleware, FillTaxPolicyMiddleware,
    FillFromActionMiddleware
)
from billing.library.python.calculator.models.event import EventModel
from billing.library.python.calculator.test_utils.mock import action_mock


@pytest.fixture
def event() -> EventModel:
    return EventModel(payload={}, tariffer_payload={})


class TestFillCommonTsMiddleware:
    def test_fill_common_ts(self, event: EventModel):
        action = action_mock(
            event=event,
            references={},
            on_dt=datetime.now()
        )

        FillCommonTsMiddleware().apply(action)

        tariffer_payload = action.event.tariffer_payload or {}
        hm.assert_that(
            tariffer_payload.get('common_ts'),
            hm.equal_to(arrow.get(action.on_dt).int_timestamp),
        )


class TestFillFromActionMiddleware:
    @pytest.mark.parametrize('src, dst, value', [
        pytest.param('dry_run', None, True),
        pytest.param('dry_run', 'moved_dry_run', False),
        pytest.param('amount_wo_vat', None, Decimal('100.29')),
        pytest.param('external_id666', 'external_id', '03fa0dd9-39b0-4076-9747-8ac96a6c8159'),
        pytest.param('service_id', None, 131),
    ])
    def test_filling(self, event: EventModel, src: str, dst: str, value: Any):
        action = action_mock(
            event=event,
            references={},
            **{
                src: value,
            }
        )

        FillFromActionMiddleware(src, dst).apply(action)

        tariffer_payload = action.event.tariffer_payload or {}
        hm.assert_that(
            tariffer_payload.get(dst or src),
            hm.equal_to(value),
        )


class TestFillTaxPolicyMiddleware:
    def test_fill_tax_policy(
            self,
            event: EventModel,
    ):

        now = datetime.now()
        action = action_mock(
            event=event,
            migrated=True,
            references=Mock(firm=Mock(id=1)),
            on_dt=now,
            tax_policy=Mock(
                tax_percents=[Mock(dt=now)],
            ),
        )

        FillTaxPolicyMiddleware().apply(action)

        tariffer_payload = action.event.tariffer_payload or {}
        hm.assert_that(tariffer_payload.get('tax_policy_id'), hm.is_not(None))
        hm.assert_that(tariffer_payload.get('tax_policy_pct'), hm.is_not(None))
        hm.assert_that(tariffer_payload.get('firm_id'), hm.is_not(None))
