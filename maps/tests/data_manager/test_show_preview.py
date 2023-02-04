import pytest

from maps_adv.geosmb.harmonist.server.lib.enums import ColumnType
from maps_adv.geosmb.harmonist.server.lib.exceptions import InvalidSessionId

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("ignore_first_line", [True, False])
@pytest.mark.parametrize("segment", [dict(), dict(segment="Import from 03.02.2021")])
async def test_returns_preview_as_expected(ignore_first_line, segment, dm, factory):
    session_id = await factory.create_log(
        biz_id=123,
        parsed_input=[["Line", "One"], ["Second", "line"]],
        markup={
            "ignore_first_line": ignore_first_line,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 5},
                {"column_type": ColumnType.LAST_NAME, "column_number": 7},
                {"column_type": ColumnType.PHONE, "column_number": 8},
                {"column_type": ColumnType.COMMENT, "column_number": 9},
                {"column_type": ColumnType.EMAIL, "column_number": 10},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 11},
            ],
            **segment,
        },
    )

    got = await dm.show_preview(session_id=session_id, biz_id=123)

    assert got == dict(
        rows=[["Line", "One"], ["Second", "line"]],
        markup=dict(
            ignore_first_line=ignore_first_line,
            column_type_map=[
                {"column_type": ColumnType.FIRST_NAME, "column_number": 5},
                {"column_type": ColumnType.LAST_NAME, "column_number": 7},
                {"column_type": ColumnType.PHONE, "column_number": 8},
                {"column_type": ColumnType.COMMENT, "column_number": 9},
                {"column_type": ColumnType.EMAIL, "column_number": 10},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 11},
            ],
            **segment,
        ),
    )


async def test_does_not_return_markup_if_do_not_have_one(dm, factory):
    session_id = await factory.create_log(
        biz_id=123, parsed_input=[["Line", "One"], ["Second", "line"]], markup=None
    )

    got = await dm.show_preview(session_id=session_id, biz_id=123)

    assert got == dict(
        rows=[["Line", "One"], ["Second", "line"]],
    )


@pytest.mark.parametrize("lines_number", [10, 20])
async def test_returns_no_more_than_10_lines_in_preview(lines_number, dm, factory):
    session_id = await factory.create_log(
        biz_id=123,
        parsed_input=[
            [f"Some{idx}", f"Line{idx}", f"Content{idx}"] for idx in range(lines_number)
        ],
    )

    got = await dm.show_preview(session_id=session_id, biz_id=123)

    assert got == dict(
        rows=[[f"Some{idx}", f"Line{idx}", f"Content{idx}"] for idx in range(10)]
    )


async def test_raises_if_session_id_not_found(dm, factory):
    with pytest.raises(
        InvalidSessionId,
        match="Session not found: "
        "session_id=8b3b4257-c615-4184-b6a2-0203e59f76a4, biz_id=123",
    ):
        await dm.show_preview(
            session_id="8b3b4257-c615-4184-b6a2-0203e59f76a4", biz_id=123
        )


async def test_raises_if_session_id_do_not_match_biz_id(dm, factory):
    other_biz_session_id = await factory.create_log(biz_id=123)
    await factory.create_log(biz_id=321)

    with pytest.raises(
        InvalidSessionId,
        match=f"Session not found: session_id={other_biz_session_id}, biz_id=321",
    ):
        await dm.show_preview(session_id=other_biz_session_id, biz_id=321)
