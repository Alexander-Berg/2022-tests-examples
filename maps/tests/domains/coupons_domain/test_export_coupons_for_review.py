import json
from datetime import datetime

import pytest
from freezegun import freeze_time

pytestmark = [pytest.mark.asyncio]


def decode_lb_message(message: bytes) -> dict:
    return json.loads(message.decode())


coupons_for_review_seq = [
    [
        dict(
            biz_id=111,
            item_id=1111,
            revision_id=1,
            title="title_1",
            cover_url="cover_url_1",
            products_description="description_1",
            conditions="conditions_1",
        ),
        dict(
            biz_id=222,
            item_id=2222,
            revision_id=2,
            title="title_2",
            cover_url="cover_url_2",
            products_description="description_2",
            conditions="conditions_2",
        ),
    ],
    [
        dict(
            biz_id=333,
            item_id=3333,
            revision_id=3,
            title="title_3",
            cover_url="cover_url_3",
            products_description="description_3",
            conditions="conditions_3",
        )
    ],
]


single_coupon_seq = [
    [
        dict(
            biz_id=111,
            item_id=1111,
            revision_id=1,
            title="title_1",
            cover_url="cover_url_1",
            products_description="description_1",
            conditions="conditions_1",
        )
    ]
]


async def test_writes_all_coupons_to_lb(domain, mock_loyalty_client, mock_topic_writer):
    mock_loyalty_client.get_coupons_list_for_review.seq = coupons_for_review_seq

    await domain.export_coupons_to_review()

    messages = [
        decode_lb_message(call_args[0][0])
        for call_args in mock_topic_writer.write_one.call_args_list
    ]
    assert [message["meta"]["biz_id"] for message in messages] == [111, 222, 333]


@freeze_time("2020-01-01 11:22:33.123")
async def test_writes_coupon_to_lb_in_expected_format(
    domain, mock_loyalty_client, mock_topic_writer
):
    mock_loyalty_client.get_coupons_list_for_review.seq = single_coupon_seq

    await domain.export_coupons_to_review()

    message_data = {
        "unixtime": 1577877753123,
        "workflow": "common",
        "service": "geosmb",
        "type": "coupons",
        "meta": {"biz_id": 111, "id": 1111, "version_id": 1},
        "data": {
            "title": "title_1",
            "cover_url": "cover_url_1",
            "conditions": "conditions_1",
            "products_description": "description_1",
        },
    }
    mock_topic_writer.write_one.assert_called_once_with(
        str.encode(json.dumps(message_data))
    )


@freeze_time("2020-01-01 11:22:33.1234567")
async def test_writes_time_as_unixtime_with_ms(
    domain, mock_loyalty_client, mock_topic_writer
):
    mock_loyalty_client.get_coupons_list_for_review.seq = single_coupon_seq

    await domain.export_coupons_to_review()

    message = mock_topic_writer.write_one.call_args[0][0]
    expected_dt = datetime.strptime(
        "2020-01-01 11:22:33.123+0000", "%Y-%m-%d %H:%M:%S.%f%z"
    )
    assert json.loads(message.decode())["unixtime"] == expected_dt.timestamp() * 1000


async def test_writes_nothing_to_lb_for_empty_coupon_iter(
    domain, mock_loyalty_client, mock_topic_writer
):
    mock_loyalty_client.get_coupons_list_for_review.seq = [[]]

    await domain.export_coupons_to_review()

    mock_topic_writer.write_one.assert_not_called()


async def test_submits_coupons_written_to_lb(
    domain, mock_loyalty_client, mock_topic_writer
):
    mock_loyalty_client.get_coupons_list_for_review.seq = coupons_for_review_seq

    await domain.export_coupons_to_review()

    mock_loyalty_client.confirm_coupons_sent_to_review.assert_called_once_with(
        [
            dict(biz_id=111, item_id=1111, revision_id=1),
            dict(biz_id=222, item_id=2222, revision_id=2),
            dict(biz_id=333, item_id=3333, revision_id=3),
        ]
    )


async def test_does_not_submit_coupons_for_empty_coupon_iter(
    domain, mock_loyalty_client, mock_topic_writer
):
    mock_loyalty_client.get_coupons_list_for_review.seq = [[]]

    await domain.export_coupons_to_review()

    mock_loyalty_client.confirm_coupons_sent_to_review.assert_not_called()


async def test_does_not_submit_coupons_if_lb_writer_creation_fails(
    domain, mock_loyalty_client, mock_lb_client
):
    mock_loyalty_client.get_coupons_list_for_review.seq = coupons_for_review_seq
    mock_lb_client.create_writer.side_effect = Exception()

    with pytest.raises(Exception):
        await domain.export_coupons_to_review()

    mock_loyalty_client.confirm_coupons_sent_to_review.assert_not_called()


async def test_submit_coupons_written_to_lb_before_writer_fails(
    domain, mock_loyalty_client, mock_topic_writer
):
    mock_loyalty_client.get_coupons_list_for_review.seq = coupons_for_review_seq
    mock_topic_writer.write_one.coro.side_effect = [None, None, Exception()]

    with pytest.raises(Exception):
        await domain.export_coupons_to_review()

    mock_loyalty_client.confirm_coupons_sent_to_review.assert_called_once_with(
        [
            dict(biz_id=111, item_id=1111, revision_id=1),
            dict(biz_id=222, item_id=2222, revision_id=2),
        ]
    )
