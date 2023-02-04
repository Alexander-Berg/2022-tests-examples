import pytest

from datetime import datetime, timezone

pytestmark = [pytest.mark.asyncio]

NOW_IN_UTC = datetime.now(tz=timezone.utc)


async def test_creates_client_locally(factory, clients_dm):
    name = """
        Имя клиента, И его фамилия, и отчество ещё, а потом имя дедушки, и бабушки, и пса Татошки,
        А ещё погоняло во дворе, ник на форумах, учётка на работе, альтерэго из мировой истории,
        Контакт любовницы в телефонной книжке под именем "Санёк", перечень названий неудавшихся
        бизнесов, цитаты из Макиавелли и Бродского, ну и, например, выдержка из завещания, поручающая
        передать значительную часть непосильно нажитого имущества в фонд борьбы с алкоголизмом
    """
    phones = """Дальше следует список телефонов, утекших из базы BeliveryClud:
        8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,
        8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,
        8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,
        8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,8(499)123-45-67,
    """
    emails = """А ещё адреса почтовых ящиков оттуда же:
        email@example.com,email@example.com,email@example.com,email@example.com,email@example.com,
        email@example.com,email@example.com,email@example.com,email@example.com,email@example.com,
        email@example.com,email@example.com,email@example.com,email@example.com,email@example.com,
        email@example.com,email@example.com,email@example.com,email@example.com,email@example.com,
    """
    await clients_dm.insert_client(
        client_id=55,
        name=name,
        email=emails,
        phone=phones,
        account_manager_id=100500,
        domain="someDomain",
        partner_agency_id=None,
        has_accepted_offer=False,
    )

    await factory.update_created_at(55, NOW_IN_UTC)
    assert await factory.get_all_clients() == [
        {
            "id": 55,
            "name": name,
            "email": emails,
            "phone": phones,
            "account_manager_id": 100500,
            "is_agency": False,
            "created_from_cabinet": False,
            "domain": "someDomain",
            "partner_agency_id": None,
            "has_accepted_offer": False,
            "created_at": NOW_IN_UTC,
            "representatives": [],
        }
    ]


async def test_returns_created_client_data(factory, clients_dm):
    result = await clients_dm.insert_client(
        client_id=55,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        account_manager_id=100500,
        domain="someDomain",
        partner_agency_id=None,
        has_accepted_offer=True,
    )

    assert result == {
        "id": 55,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "account_manager_id": 100500,
        "partner_agency_id": None,
        "is_agency": False,
        "created_from_cabinet": False,
        "has_accepted_offer": True,
    }
