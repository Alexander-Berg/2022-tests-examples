import gzip
import json
from typing import List
from unittest import mock

import pytest

from maps_adv.geosmb.clients.loyalty import CouponReviewResolution, LoyaltyIntException
from maps_adv.geosmb.clients.logbroker.logbroker import (
    LogbrokerReadTimeout,
)

pytestmark = [pytest.mark.asyncio]


def create_review_result(
    verdict="Yes",
    title="coupon title",
    products_description="coupon products description",
    conditions="some extra conditions",
    reasons=(101, 202, 203, 201),
    biz_id=1234,
    item_id=5674,
    version_id=1,
) -> dict:
    assert verdict in ("Yes", "No")
    return json.dumps(
        {
            "result": {
                "verdict": verdict,
                "corrected": {
                    "type": "coupons",
                    "content": {
                        "title": title,
                        "products_description": products_description,
                        "conditions": conditions,
                    },
                },
                "reasons": list(reasons),
            },
            "meta": {"biz_id": biz_id, "id": item_id, "version_id": version_id},
        }
    )


def encode_review_result(review_results: List[dict]) -> bytes:
    return gzip.compress("".join([f"{rr}\n" for rr in review_results]).encode("utf-8"))


async def test_sends_review_results_to_loyalty(
    domain, mock_topic_reader, mock_loyalty_client
):
    mock_topic_reader.read_batch.seq = [
        encode_review_result(
            [
                create_review_result(
                    verdict="Yes", title="coupon title_1", item_id=555
                ),
                create_review_result(verdict="No", title="coupon title_2", item_id=777),
                create_review_result(
                    verdict="Yes", title="coupon title_3", item_id=888
                ),
            ]
        ),
        encode_review_result(
            [create_review_result(verdict="No", title="coupon title_4", item_id=222)]
        ),
        LogbrokerReadTimeout(),
    ]

    await domain.export_coupons_reviewed_to_loyalty()

    mock_loyalty_client.submit_coupons_reviews_list.assert_has_calls(
        [
            mock.call(
                [
                    dict(
                        biz_id=1234,
                        item_id=555,
                        revision_id=1,
                        corrected=dict(
                            title="coupon title_1",
                            products_description="coupon products description",
                            conditions="some extra conditions",
                        ),
                        reason_codes=[101, 202, 203, 201],
                        resolution=CouponReviewResolution.APPROVED,
                    ),
                    dict(
                        biz_id=1234,
                        item_id=777,
                        revision_id=1,
                        corrected=dict(
                            title="coupon title_2",
                            products_description="coupon products description",
                            conditions="some extra conditions",
                        ),
                        reason_codes=[101, 202, 203, 201],
                        resolution=CouponReviewResolution.REJECTED,
                    ),
                    dict(
                        biz_id=1234,
                        item_id=888,
                        revision_id=1,
                        corrected=dict(
                            title="coupon title_3",
                            products_description="coupon products description",
                            conditions="some extra conditions",
                        ),
                        reason_codes=[101, 202, 203, 201],
                        resolution=CouponReviewResolution.APPROVED,
                    ),
                ]
            ),
            mock.call(
                [
                    dict(
                        biz_id=1234,
                        item_id=222,
                        revision_id=1,
                        corrected=dict(
                            title="coupon title_4",
                            products_description="coupon products description",
                            conditions="some extra conditions",
                        ),
                        reason_codes=[101, 202, 203, 201],
                        resolution=CouponReviewResolution.REJECTED,
                    )
                ]
            ),
        ],
        any_order=False,
    )


async def test_does_not_call_loyalty_client_if_nothing_to_submit(
    domain, mock_topic_reader, mock_loyalty_client
):
    mock_topic_reader.read_batch.seq = [LogbrokerReadTimeout()]

    await domain.export_coupons_reviewed_to_loyalty()

    mock_loyalty_client.submit_coupons_reviews_list.assert_not_called()


@pytest.mark.parametrize(
    "review_results, commit_call_count",
    [
        ([], 0),
        (
            [
                encode_review_result([create_review_result()]),
                encode_review_result([create_review_result()]),
            ],
            2,
        ),
    ],
)
async def test_finishes_reading_correctly(
    commit_call_count, review_results, domain, mock_topic_reader, mock_loyalty_client
):
    mock_topic_reader.read_batch.seq = [*review_results, LogbrokerReadTimeout()]

    await domain.export_coupons_reviewed_to_loyalty()

    assert mock_topic_reader.commit.call_count == commit_call_count
    assert mock_topic_reader.finish_reading.call_count == 1


async def test_finishes_reading_correctly_on_any_not_expected_logbroker_exception(
    domain, mock_topic_reader, mock_loyalty_client
):
    mock_topic_reader.read_batch.seq = [
        encode_review_result([create_review_result()]),
        Exception(),
    ]

    await domain.export_coupons_reviewed_to_loyalty()

    mock_topic_reader.commit.assert_called()
    mock_topic_reader.finish_reading.assert_called()


async def test_logs_not_expected_logbroker_exception(domain, mock_topic_reader, caplog):
    mock_topic_reader.read_batch.seq = [
        encode_review_result([create_review_result()]),
        Exception("Any Exception"),
    ]

    await domain.export_coupons_reviewed_to_loyalty()

    warning_messages = [r for r in caplog.records if r.levelname == "ERROR"]

    assert len(warning_messages) == 1
    assert (
        warning_messages[0].message
        == "Unhandled exception while reading from logbroker"
    )


async def test_does_not_commit_if_submiting_to_loyalty_fails(
    domain, mock_topic_reader, mock_loyalty_client
):
    mock_topic_reader.read_batch.seq = [
        encode_review_result([create_review_result()]),
        LogbrokerReadTimeout(),
    ]
    mock_loyalty_client.submit_coupons_reviews_list.coro.side_effect = (
        LoyaltyIntException()
    )

    await domain.export_coupons_reviewed_to_loyalty()

    mock_topic_reader.commit.assert_not_called()
    mock_topic_reader.finish_reading.assert_called()


async def test_logs_loyalty_exceptions(
    domain, mock_topic_reader, mock_loyalty_client, caplog
):
    mock_topic_reader.read_batch.seq = [
        encode_review_result([create_review_result()]),
        LogbrokerReadTimeout(),
    ]
    mock_loyalty_client.submit_coupons_reviews_list.coro.side_effect = (
        LoyaltyIntException()
    )

    await domain.export_coupons_reviewed_to_loyalty()

    warning_messages = [r for r in caplog.records if r.levelname == "ERROR"]

    assert len(warning_messages) == 1
    assert (
        warning_messages[0].message
        == "Unhandled exception while submiting review results"
    )


async def test_does_not_logs_logbroker_timeout_exception(
    domain, mock_topic_reader, caplog
):
    mock_topic_reader.read_batch.seq = [LogbrokerReadTimeout()]

    await domain.export_coupons_reviewed_to_loyalty()

    warning_messages = [r for r in caplog.records if r.levelname == "WARNING"]

    assert len(warning_messages) == 0


async def test_sends_collected_data_to_loyalty_regardless_any_not_expected_exception(
    domain, mock_topic_reader, mock_loyalty_client
):
    mock_topic_reader.read_batch.seq = [
        encode_review_result(
            [
                create_review_result(
                    verdict="Yes", title="coupon title_1", item_id=555
                ),
                create_review_result(verdict="No", title="coupon title_2", item_id=777),
            ]
        ),
        Exception(),
    ]

    await domain.export_coupons_reviewed_to_loyalty()

    mock_loyalty_client.submit_coupons_reviews_list.assert_called_with(
        [
            dict(
                biz_id=1234,
                item_id=555,
                revision_id=1,
                corrected=dict(
                    title="coupon title_1",
                    products_description="coupon products description",
                    conditions="some extra conditions",
                ),
                reason_codes=[101, 202, 203, 201],
                resolution=CouponReviewResolution.APPROVED,
            ),
            dict(
                biz_id=1234,
                item_id=777,
                revision_id=1,
                corrected=dict(
                    title="coupon title_2",
                    products_description="coupon products description",
                    conditions="some extra conditions",
                ),
                reason_codes=[101, 202, 203, 201],
                resolution=CouponReviewResolution.REJECTED,
            ),
        ]
    )
