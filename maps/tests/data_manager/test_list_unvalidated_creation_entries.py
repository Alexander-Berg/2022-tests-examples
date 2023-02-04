from operator import itemgetter

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.harmonist.server.lib.enums import ColumnType

pytestmark = [pytest.mark.asyncio]


async def test_return_data(factory, dm):
    session_id = await factory.create_log(
        biz_id=123,
        parsed_input=[["Line", "One"], ["Second", "line"]],
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 1},
                {"column_type": ColumnType.LAST_NAME, "column_number": 2},
            ],
        },
    )

    result = await dm.list_unvalidated_creation_entries()

    assert result == [
        dict(
            session_id=session_id,
            biz_id=123,
            parsed_input=[["Line", "One"], ["Second", "line"]],
            markup={
                "ignore_first_line": True,
                "column_type_map": [
                    {"column_type": ColumnType.FIRST_NAME, "column_number": 1},
                    {"column_type": ColumnType.LAST_NAME, "column_number": 2},
                ],
            },
        )
    ]


async def test_returns_all_unvalidated_entries(factory, dm):
    for biz_id in (123, 123, 321):
        await factory.create_log(
            biz_id=biz_id,
            parsed_input=[["Line", "One"], ["Second", "line"]],
            markup={
                "ignore_first_line": True,
                "column_type_map": [
                    {"column_type": ColumnType.FIRST_NAME, "column_number": 1},
                    {"column_type": ColumnType.LAST_NAME, "column_number": 2},
                ],
            },
        )

    result = await dm.list_unvalidated_creation_entries()

    assert len(result) == 3


async def test_returns_entries_sorted_by_created_at(factory, dm):
    session_id1 = await factory.create_log(
        biz_id=123,
        parsed_input=[["Line", "One"], ["Second", "line"]],
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 1},
                {"column_type": ColumnType.LAST_NAME, "column_number": 2},
            ],
        },
        created_at=dt("2020-02-02 15:00:00"),
    )
    session_id2 = await factory.create_log(
        biz_id=123,
        parsed_input=[["Line", "One"], ["Second", "line"]],
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 1},
                {"column_type": ColumnType.LAST_NAME, "column_number": 2},
            ],
        },
        created_at=dt("2020-02-02 11:00:00"),
    )
    session_id3 = await factory.create_log(
        biz_id=123,
        parsed_input=[["Line", "One"], ["Second", "line"]],
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 1},
                {"column_type": ColumnType.LAST_NAME, "column_number": 2},
            ],
        },
        created_at=dt("2020-02-02 13:00:00"),
    )

    result = await dm.list_unvalidated_creation_entries()

    assert list(map(itemgetter("session_id"), result)) == [
        session_id2,
        session_id3,
        session_id1,
    ]


async def test_does_not_return_entries_with_field_markup(factory, dm):
    await factory.create_log(
        biz_id=123,
        parsed_input=[["Line", "One"], ["Second", "line"]],
    )

    result = await dm.list_unvalidated_creation_entries()

    assert result == []


async def test_does_not_return_entries_with_validated_data(factory, dm):
    await factory.create_log(
        biz_id=123,
        parsed_input=[["Line", "One"], ["Second", "line"]],
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 1},
                {"column_type": ColumnType.LAST_NAME, "column_number": 2},
            ],
        },
        valid_clients=[dict(email="some@email.ru")],
        invalid_clients=[dict(row=["some", "values"], reason="because")],
    )

    result = await dm.list_unvalidated_creation_entries()

    assert result == []
