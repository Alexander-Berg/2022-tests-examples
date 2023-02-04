import pytest

from datetime import datetime, timezone

pytestmark = [pytest.mark.asyncio]
NOW_IN_UTC = datetime.now(tz=timezone.utc)


async def test_updates_client_fields(factory, clients_dm):
    await factory.create_client(
        id=22,
        name="Имя 22",
        email="22@ya.ru",
        phone="222",
        is_agency=False,
        account_manager_id=1,
        domain="",
        created_at=NOW_IN_UTC,
    )
    await factory.create_client(
        id=33,
        name="Имя 33",
        email="33@ya.ru",
        phone="333",
        is_agency=True,
        account_manager_id=None,
        domain="someDomain",
        created_at=NOW_IN_UTC,
    )

    await clients_dm.upsert_client(
        id=22,
        name="Имя 122",
        email="122@ya.ru",
        phone="222-222",
        is_agency=True,
        account_manager_id=None,
    )
    await clients_dm.upsert_client(
        id=33,
        name="Имя 133",
        email="133@ya.ru",
        phone="333-333",
        is_agency=False,
        account_manager_id=1,
    )

    assert await factory.get_client(22) == {
        "id": 22,
        "name": "Имя 122",
        "email": "122@ya.ru",
        "phone": "222-222",
        "is_agency": True,
        "created_from_cabinet": False,
        "account_manager_id": None,
        "domain": "",
        "partner_agency_id": None,
        "has_accepted_offer": False,
        "created_at": NOW_IN_UTC,
        "representatives": [],
    }
    assert await factory.get_client(33) == {
        "id": 33,
        "name": "Имя 133",
        "email": "133@ya.ru",
        "phone": "333-333",
        "is_agency": False,
        "created_from_cabinet": False,
        "account_manager_id": 1,
        "domain": "someDomain",
        "partner_agency_id": None,
        "has_accepted_offer": False,
        "created_at": NOW_IN_UTC,
        "representatives": [],
    }
