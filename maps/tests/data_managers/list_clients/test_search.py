from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.doorman.server.lib.enums import ClientGender, SegmentType, Source
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


async def test_returns_clients_details(factory, dm):
    client_id = await factory.create_client(first_name="Иван")

    _, got = await dm.list_clients(
        biz_id=123, search_string="Иван", limit=100500, offset=0
    )

    assert got == [
        dict(
            id=client_id,
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=456,
            first_name="Иван",
            last_name="client_last_name",
            gender=ClientGender.MALE,
            comment="this is comment",
            cleared_for_gdpr=False,
            labels=["mark-2021"],
            segments=[SegmentType.NO_ORDERS],
            statistics={
                "orders": {
                    "total": 0,
                    "successful": 0,
                    "unsuccessful": 0,
                    "last_order_timestamp": None,
                }
            },
            source=Source.CRM_INTERFACE,
            registration_timestamp=Any(datetime),
        )
    ]


async def test_returns_source_from_first_revision(factory, dm):
    client_id = await factory.create_client(
        source=Source.CRM_INTERFACE, first_name="Иван"
    )
    await factory.create_revision(client_id, source=Source.GEOADV_PHONE_CALL)

    _, got = await dm.list_clients(
        biz_id=123, search_string="Иван", limit=100500, offset=0
    )

    assert got[0]["source"] == Source.CRM_INTERFACE


async def test_filters_by_biz_id(factory, dm):
    id_1 = await factory.create_empty_client(
        biz_id=123, comment="работает таксистом в Москве"
    )
    await factory.create_empty_client(biz_id=999, comment="работает таксистом в Москве")

    _, got = await dm.list_clients(
        biz_id=123, search_string="работает", limit=100500, offset=0
    )

    assert extract_ids(got) == [id_1]


@pytest.mark.parametrize(
    "search_string",
    (
        # each column engage in search
        "Вася",
        "Иванов",
        "123456789",
        "email@yandex.ru",
        "работает",
        # ignore not matched search words
        "арбуз 123456789 ананас Иванов",
    ),
)
async def test_strict_search_works_in_all_searchable_columns(
    dm, factory, db, search_string
):
    client_id = await factory.create_empty_client(
        biz_id=123,
        first_name="Вася",
        last_name="Иванов",
        phone=123456789,
        email="email@yandex.ru",
        comment="работает таксистом в Москве",
    )
    await factory.create_empty_client(
        biz_id=123,
        first_name="Петя",
        last_name="Петров",
        phone=99999,
        email="petrov@mail.ru",
        comment="повар в Питере",
    )

    _, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=100500, offset=0
    )

    assert extract_ids(got) == [client_id]


@pytest.mark.parametrize(
    "search_string", ("ВасЫлий", "ивЫнов", "emYil@yandex.ru", "рабЫтает")
)
async def test_fuzzy_search_works_in_all_searchable_columns_except_phone(
    factory, db, search_string, dm
):
    client_id = await factory.create_empty_client(
        biz_id=123,
        first_name="Василий",
        last_name="Иванов",
        phone=123456789,
        email="email@yandex.ru",
        comment="работает таксистом в Москве",
    )
    await factory.create_empty_client(
        biz_id=123,
        first_name="Петя",
        last_name="Петров",
        phone=99999,
        email="petrov@mail.ru",
        comment="повар в Питере",
    )

    _, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=100500, offset=0
    )

    assert extract_ids(got) == [client_id]


async def test_fuzzy_search_does_not_work_for_phone_column(factory, db, dm):
    await factory.create_empty_client(
        biz_id=123,
        first_name="Василий",
        last_name="Иванов",
        phone=123456789,
        email="email@yandex.ru",
        comment="работает таксистом в Москве",
    )

    _, got = await dm.list_clients(
        biz_id=123, search_string="1234^6789", limit=100500, offset=0
    )

    assert extract_ids(got) == []


@pytest.mark.parametrize(
    "db_value, search_string",
    [
        # fuzzy search
        ("рАботает", "рЫботает"),  # typos
        ("мёдоед", "медоед"),
        ("медоед", "мёдоед"),
        ("молоко", "алоко"),
        ("молоко", "мол"),
        ("works", "work"),
        ("123456789", "2345678"),  # numbers
        ("АБВГДXYZ", "БВГДXY"),  # unknown words
        ("email@yandex.ru", "yandex.ru"),
        ("email@yandex.ru", "email"),
        ("ivanov@yandex.ru", "petrov@yandex.rработаетu"),
        # case-agnostic
        ("ИВАНов", "ИванОВ"),
        # not russian
        ("work is perfect", "perfect work"),
        # ignore punctuation
        ("если, кажется: быть - не быть?", "если кажется быть не быть"),
        # time cases
        ("запись на 19:00", "19:00"),
        ("запись на 19:00", "19:35"),
        ("запись на 19:00", "22:00"),
        ("запись на 19:00", "00 запись 19"),
        # not all words of search string used
        ("мыло", "купить мыло дёшево"),
        # match by word parts
        ("123456789", "x2345678x"),  # middle
        ("123456789", "1234x"),  # start
        ("123456789", "x56789"),  # end
        ("123456789", "123xxx789"),  # not middle
    ],
)
async def test_finds_by_fuzzy_search(factory, dm, db_value, search_string):
    client_id = await factory.create_empty_client(biz_id=123, comment=db_value)

    _, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=100500, offset=0
    )

    assert extract_ids(got) == [client_id]


@pytest.mark.parametrize(
    "db_value, search_string",
    [
        # low similarity
        ("молоко", "шоко"),
        ("молоко", "олок"),
        ("123456789", "123"),
        ("123456789", "89"),
        # email is treated as single word
        ("email@yandex.ru", "emai"),
        # fuzzy search works on short words badly
        ("мёда", "меда"),
    ],
)
async def test_does_not_find_by_fuzzy_search(factory, dm, db_value, search_string):
    await factory.create_empty_client(biz_id=123, comment=db_value)

    _, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=100500, offset=0
    )

    assert got == []


@pytest.mark.parametrize(
    "search_string",
    (
        # ignores stop-words
        "в",
        "к",
        "to",
        "with",
        # ignores whitespaces
        " ",
        "\n",
        "\t",
        # ignores not words
        "@",
        "?????",
    ),
)
async def test_ignores_useless_words_for_fuzzy_search(factory, dm, search_string):
    await factory.create_empty_client(biz_id=123, comment="в к to with @ \n ?????  \t")

    _, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=100500, offset=0
    )

    assert got == []


@pytest.mark.parametrize(
    "priority_column", ["phone", "email", "first_name", "last_name"]
)
@pytest.mark.parametrize("search_string", ["1", "89", "6"])
async def test_finds_short_pattern_in_priority_columns(
    factory, dm, priority_column, search_string
):
    client_id = await factory.create_empty_client(
        biz_id=123, **{priority_column: "123456789"}
    )

    _, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=100500, offset=0
    )

    assert extract_ids(got) == [client_id]


@pytest.mark.parametrize(
    "priority_column", ["phone", "email", "first_name", "last_name"]
)
@pytest.mark.parametrize("search_string", ["1", "2", "12"])
async def test_finds_short_words_in_priority_columns(
    factory, dm, priority_column, search_string
):
    client_id = await factory.create_empty_client(biz_id=123, **{priority_column: "12"})

    _, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=100500, offset=0
    )

    assert extract_ids(got) == [client_id]


@pytest.mark.parametrize("search_string", ["1", "89", "6"])
async def test_strict_search_does_not_work_for_secondary_columns(
    factory, dm, search_string
):
    await factory.create_empty_client(biz_id=123, comment="123456789")

    _, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=100500, offset=0
    )

    assert extract_ids(got) == []


async def test_excludes_duplicates(factory, dm):
    client_id = await factory.create_empty_client(
        email="email@yandex.ru", comment="email@yandex.ru"
    )

    _, got = await dm.list_clients(
        biz_id=123, search_string="email@yandex.ru", limit=100500, offset=0
    )

    assert extract_ids(got) == [client_id]


async def test_returns_fts_result_sorted_by_rank(factory, dm):
    # 1 match
    id_1 = await factory.create_empty_client(first_name="Вася")
    # 3 matches
    id_2 = await factory.create_empty_client(comment="123456789 Иванов Вася")
    # 2 matches
    id_3 = await factory.create_empty_client(first_name="Вася", last_name="Иванов")
    # 1 match
    id_4 = await factory.create_empty_client(first_name="Иванов")

    _, got = await dm.list_clients(
        biz_id=123,
        search_string="Вася Иванов 123456789 email@yandex.ru работает",
        limit=100500,
        offset=0,
    )

    assert extract_ids(got) == [id_2, id_3, id_4, id_1]


@pytest.mark.parametrize(
    "db_column, db_value_1, db_value_2, search_string",
    [
        ("phone", "123456", "123450", "123"),
        ("email", "email_1@yandex.ru", "email_2@yandex.ru", "email"),
        ("first_name", "Вася Иванов", "Вася Иванов", "Вася Иванов"),
        ("last_name", "Вася Иванов", "Вася Иванов", "Вася Иванов"),
        ("comment", "Вася Иванов", "Вася Иванов", "Вася Иванов"),
    ],
)
async def test_sorts_clients_with_identical_similarity_by_creation_time(
    factory, dm, db_column, db_value_1, db_value_2, search_string
):
    id_1 = await factory.create_empty_client(**{db_column: db_value_1})
    id_2 = await factory.create_empty_client(**{db_column: db_value_2})

    _, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=100500, offset=0
    )

    assert extract_ids(got) == [id_2, id_1]


@pytest.mark.parametrize("db_column", ["phone", "email", "first_name", "last_name"])
async def test_matched_by_priority_columns_has_priority(factory, dm, db_column):
    id_1 = await factory.create_empty_client(**{db_column: "123456789"})
    id_2 = await factory.create_empty_client(comment="123456789")

    _, got = await dm.list_clients(
        biz_id=123, search_string="1234", limit=100500, offset=0
    )

    assert extract_ids(got) == [id_1, id_2]


async def test_strict_matching_has_higher_priority_over_fuzzy_matching(factory, dm):
    id_1 = await factory.create_empty_client(email="meow@yandex.ru")
    id_2 = await factory.create_empty_client(email="meok@yandex.ru")

    _, got = await dm.list_clients(
        biz_id=123, search_string="meow", limit=100500, offset=0
    )

    assert extract_ids(got) == [id_1, id_2]


async def test_preserves_order_inside_each_priority_group(factory, dm):
    # FTS
    id_1 = await factory.create_empty_client(comment="emaiN")
    id_2 = await factory.create_empty_client(comment="emaiN")
    # Priority column search (strict)
    id_3 = await factory.create_empty_client(first_name="emaiN@yandex.ru")
    id_4 = await factory.create_empty_client(first_name="emaiN@yandex.ru")
    # Priority column search (fuzzy)
    id_5 = await factory.create_empty_client(email="email_1@yandex.ru")
    id_6 = await factory.create_empty_client(email="email_2@yandex.ru")

    _, got = await dm.list_clients(
        biz_id=123, search_string="emaiN", limit=100500, offset=0
    )

    assert extract_ids(got) == [id_4, id_3, id_6, id_5, id_2, id_1]


async def test_counts_valid_total(factory, dm):
    # FTS
    await factory.create_empty_client(comment="email@gmail.com")
    # Priority column search
    await factory.create_empty_client(email="email@gmail.com")

    total_counts, got = await dm.list_clients(
        biz_id=123, search_string="email@gmail.com", limit=1, offset=0
    )

    assert total_counts == 2
