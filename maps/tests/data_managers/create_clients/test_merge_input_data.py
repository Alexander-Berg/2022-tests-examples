import pytest

from maps_adv.geosmb.doorman.server.lib.enums import Source
from maps_adv.geosmb.doorman.server.tests.utils import make_alex, make_fedor, make_ivan

pytestmark = [pytest.mark.asyncio]


async def test_merges_duplicates(dm, factory):
    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[*[make_ivan() for _ in range(5)]],
    )

    clients = await factory.list_clients()
    assert len(clients) == 1


async def test_merges_by_phone(dm, factory):
    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[
            make_ivan(phone="999999999"),
            make_alex(phone="999999999"),
            make_fedor(phone="999999999"),
        ],
    )

    clients = await factory.list_clients()
    assert len(clients) == 1


async def test_does_not_merge_for_different_phones(dm, factory):
    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[
            make_ivan(phone="1111111111"),
            make_alex(phone="2222222222"),
            make_fedor(phone="3333333333"),
        ],
    )

    clients = await factory.list_clients()
    assert len(clients) == 3


@pytest.mark.parametrize(
    "fedor_params, expected",
    [
        # last not null overrides earlier
        (
            dict(
                first_name="Фёдор",
                last_name="Лисицын",
                email="fedor@yandex.ru",
                comment="fedor comment",
            ),
            dict(
                first_name="Фёдор",
                last_name="Лисицын",
                email="fedor@yandex.ru",
                comment="fedor comment",
            ),
        ),
        # null doesn't override
        (
            dict(
                first_name=None,
                last_name=None,
                email=None,
                comment=None,
            ),
            dict(
                first_name="Алекс",
                last_name="Зайцев",
                email="alex@yandex.ru",
                comment="alex comment",
            ),
        ),
    ],
)
async def test_overrides_earlier_with_not_null_latest_for_merge_by_phone(
    dm, factory, fedor_params, expected
):
    fedor_params.update(phone="999999999")
    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[
            make_ivan(phone="999999999"),
            make_alex(phone="999999999"),
            fedor_params,
        ],
    )

    merged_client = dict(
        biz_id=123,
        passport_uid=None,
        gender=None,
        labels=["feb-2021"],
        phone="999999999",
    )
    merged_client.update(expected)
    clients = await factory.list_clients()
    assert clients == [merged_client]


async def test_merges_by_email_if_phones_are_null(dm, factory):
    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[
            make_ivan(phone=None, email="super@yandex.ru"),
            make_alex(phone=None, email="super@yandex.ru"),
            make_fedor(phone=None, email="super@yandex.ru"),
        ],
    )

    clients = await factory.list_clients()
    assert len(clients) == 1


async def test_does_not_merge_for_different_emails_if_phones_are_null(dm, factory):
    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[
            make_ivan(phone=None, email="ivan@yandex.ru"),
            make_alex(phone=None, email="alex@yandex.ru"),
            make_fedor(phone=None, email="fedor@yandex.ru"),
        ],
    )

    clients = await factory.list_clients()
    assert len(clients) == 3


@pytest.mark.parametrize(
    "fedor_params, expected",
    [
        # last not null overrides earlier
        (
            dict(
                first_name="Фёдор",
                last_name="Лисицын",
                comment="fedor comment",
            ),
            dict(
                first_name="Фёдор",
                last_name="Лисицын",
                comment="fedor comment",
            ),
        ),
        # null doesn't override
        (
            dict(
                first_name=None,
                last_name=None,
                comment=None,
            ),
            dict(
                first_name="Алекс",
                last_name="Зайцев",
                comment="alex comment",
            ),
        ),
    ],
)
async def test_overrides_earlier_with_not_null_latest_for_merge_by_email(
    dm, factory, fedor_params, expected
):
    fedor_params.update(phone=None, email="super@yandex.ru")
    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[
            make_ivan(phone=None, email="super@yandex.ru"),
            make_alex(phone=None, email="super@yandex.ru"),
            fedor_params,
        ],
    )

    merged_client = dict(
        biz_id=123,
        passport_uid=None,
        gender=None,
        phone=None,
        labels=["feb-2021"],
        email="super@yandex.ru",
    )
    merged_client.update(expected)
    clients = await factory.list_clients()
    assert clients == [merged_client]


async def test_does_not_merge_by_email_if_one_phone_is_null(dm, factory):
    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[
            make_ivan(phone="1111111111", email="super@yandex.ru"),
            make_fedor(phone=None, email="super@yandex.ru"),
        ],
    )

    clients = await factory.list_clients()
    assert len(clients) == 2
