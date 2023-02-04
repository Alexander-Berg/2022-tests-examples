import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_expected_params(domain, dm):
    await domain.list_contacts([123])

    dm.list_contacts.assert_called_with([123])


async def test_returns_contacts_details(domain, dm):
    contacts = [
        dict(
            id=1,
            biz_id=123,
            passport_uid=456,
            phone=1234567890123,
            email="email@yandex.ru",
            first_name="client_first_name",
            last_name="client_last_name",
            cleared_for_gdpr=False,
        )
    ]
    dm.list_contacts.coro.return_value = contacts

    got = await domain.list_contacts([123])

    assert got == contacts
