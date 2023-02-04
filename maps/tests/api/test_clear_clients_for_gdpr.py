from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.doorman.proto.clients_pb2 import (
    ClearClientsForGdprInput,
    ClearClientsForGdprOutput,
)

pytestmark = [
    pytest.mark.asyncio,
]

url = "/internal/v1/clear_clients_for_gdpr/"


async def test_clears_clients_table_for_matched_clients(factory, api, con):
    client_id = await factory.create_client(passport_uid=12345)

    await api.post(url, proto=ClearClientsForGdprInput(passport_uid=12345))

    clients = await con.fetch(
        "SELECT * FROM clients ORDER BY id",
    )
    assert [dict(client) for client in clients] == [
        dict(
            id=client_id,
            biz_id=123,
            first_name=None,
            last_name=None,
            passport_uid=None,
            phone=None,
            email=None,
            gender=None,
            comment="this is comment",
            labels=["mark-2021"],
            created_at=Any(datetime),
            ts_storage=Any(str),
            cleared_for_gdpr=True,
        )
    ]


async def test_clears_clients_regardless_biz_id(factory, api, con):
    await factory.create_client(client_id=1, biz_id=111, passport_uid=12345)
    await factory.create_client(client_id=2, biz_id=222, passport_uid=12345)

    await api.post(url, proto=ClearClientsForGdprInput(passport_uid=12345))

    got = await con.fetchval(
        """
        SELECT count(*)
        FROM clients
        WHERE first_name IS NULL
            AND last_name IS NULL
            AND passport_uid IS NULL
            AND phone IS NULL
            AND email IS NULL
            AND gender IS NULL
        """
    )
    assert got == 2


@pytest.mark.real_db
async def test_clears_existed_revisions_for_matched_clients(factory, api, con):
    client_id = await factory.create_client(passport_uid=12345)
    await factory.create_revision(client_id=client_id)

    await api.post(url, proto=ClearClientsForGdprInput(passport_uid=12345))

    revisions = await con.fetch(
        """
        SELECT *
        FROM client_revisions
        ORDER BY created_at
        LIMIT 2
        """,
    )
    assert [dict(rev) for rev in revisions] == [
        dict(
            id=Any(int),
            client_id=client_id,
            biz_id=123,
            source="CRM_INTERFACE",
            metadata=None,
            first_name=None,
            last_name=None,
            passport_uid=None,
            phone=None,
            email=None,
            gender=None,
            comment="this is comment",
            initiator_id=112233,
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            client_id=client_id,
            biz_id=123,
            source="CRM_INTERFACE",
            metadata=None,
            first_name=None,
            last_name=None,
            passport_uid=None,
            phone=None,
            email=None,
            gender=None,
            comment="this is comment",
            initiator_id=112233,
            created_at=Any(datetime),
        ),
    ]


@pytest.mark.real_db
async def test_creates_revision_about_clearing(factory, api, con):
    client_id = await factory.create_client(passport_uid=12345)

    await api.post(url, proto=ClearClientsForGdprInput(passport_uid=12345))

    revisions = await con.fetch(
        """
        SELECT *
        FROM client_revisions
        ORDER BY created_at DESC
        LIMIT 1
        """,
    )
    assert [dict(rev) for rev in revisions] == [
        dict(
            id=Any(int),
            client_id=client_id,
            biz_id=123,
            source="GDPR",
            metadata=None,
            first_name=None,
            last_name=None,
            passport_uid=None,
            phone=None,
            email=None,
            gender=None,
            comment=None,
            initiator_id=None,
            created_at=Any(datetime),
        ),
    ]


async def test_does_not_clear_not_matched_clients(factory, api, con):
    await factory.create_client(biz_id=111, passport_uid=999)

    await api.post(url, proto=ClearClientsForGdprInput(passport_uid=12345))

    assert (
        await con.fetchval(
            """
        SELECT count(*)
        FROM clients
        WHERE first_name IS NULL
            AND last_name IS NULL
            AND passport_uid IS NULL
            AND phone IS NULL
            AND email IS NULL
            AND gender IS NULL
        """
        )
        == 0
    )
    assert (
        await con.fetchval(
            """
        SELECT count(*)
        FROM client_revisions
        WHERE first_name IS NULL
            AND last_name IS NULL
            AND passport_uid IS NULL
            AND phone IS NULL
            AND email IS NULL
            AND gender IS NULL
        """
        )
        == 0
    )


async def test_returns_cleared_clients_details(factory, api, con):
    client_id = await factory.create_client(biz_id=123, passport_uid=12345)

    got = await api.post(
        url,
        proto=ClearClientsForGdprInput(passport_uid=12345),
        decode_as=ClearClientsForGdprOutput,
        expected_status=200,
    )

    assert got == ClearClientsForGdprOutput(
        cleared_clients=[
            ClearClientsForGdprOutput.ClearedClient(client_id=client_id, biz_id=123),
        ]
    )


async def test_returns_nothing_if_no_matches(factory, api, con):
    await factory.create_client(passport_uid=12345)

    got = await api.post(
        url,
        proto=ClearClientsForGdprInput(passport_uid=999),
        decode_as=ClearClientsForGdprOutput,
        expected_status=200,
    )

    assert got == ClearClientsForGdprOutput(cleared_clients=[])


async def test_returns_cleared_clients_sorted_by_id(factory, api, con):
    for idx in [3, 1, 2]:
        client_id = await factory.create_client(
            client_id=idx, biz_id=idx * 10, passport_uid=12345
        )
        await factory.create_order_event(client_id)
        await factory.create_call_event(client_id)

    got = await api.post(
        url,
        proto=ClearClientsForGdprInput(passport_uid=12345),
        decode_as=ClearClientsForGdprOutput,
        expected_status=200,
    )

    assert [client.client_id for client in got.cleared_clients] == [1, 2, 3]
