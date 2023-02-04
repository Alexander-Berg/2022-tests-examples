import datetime

import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_yql_sql_operation_request(mocker):
    mock = mocker.MagicMock()

    mocker.patch("yql.client.operation.YqlSqlOperationRequest.run")
    mocker.patch(
        "yql.client.operation.YqlSqlOperationRequest.get_results"
    ).return_value = mock

    return mock.table.get_iterator


@pytest.mark.parametrize(
    ["records_in_yt", "expected_result"],
    [
        (
            [("1", "2000-01-02", 1, None, None, None, None, None, None)],
            [(1, datetime.date(2000, 1, 2), 1, 0, 0, 0, 0, 0, 0)],
        ),
        (
            [
                ("1", "2000-01-02", 1, 0, 0, 0, 0, 0, 0),
                ("2", "2000-01-02", 8, 2, 3, 4, 5, 6, 7),
            ],
            [
                (1, datetime.date(2000, 1, 2), 1, 0, 0, 0, 0, 0, 0),
                (2, datetime.date(2000, 1, 2), 8, 2, 3, 4, 5, 6, 7),
            ],
        ),
    ],
)
async def test_success_insert_new_reports(
    records_in_yt, expected_result, mock_yql_sql_operation_request, dm, con
):
    mock_yql_sql_operation_request.return_value = iter(records_in_yt)

    await dm.sync_category_search_reports()

    result = list(
        map(
            tuple,
            await con.fetch(
                """
                    SELECT campaign_id, date, created_at,
                           icon_clicks, icon_shows, pin_clicks, pin_shows,
                           routes, devices
                    FROM category_search_report"""
            ),
        )
    )

    assert result == expected_result


@pytest.mark.parametrize(
    ["records_in_pg", "records_in_yt", "expected_result"],
    [
        (
            [(1, dt("2000-01-02"), 2, 3, 4, 5, 6, 7, 8)],
            [("1", "2000-01-02", 20, 30, 40, 50, 60, 70, 80)],
            [(1, datetime.date(2000, 1, 2), 20, 30, 40, 50, 60, 70, 80)],
        ),
        (  # Remove record by campaign 2 because in yt one record
            [
                (1, dt("2000-01-02"), 2, 3, 4, 5, 6, 7, 8),
                (2, dt("2000-01-02"), 2, 3, 4, 5, 6, 7, 8),
            ],
            [("1", "2000-01-02", 20, 30, 40, 50, 60, 70, 80)],
            [(1, datetime.date(2000, 1, 2), 20, 30, 40, 50, 60, 70, 80)],
        ),
        (  # Replace campaign by date and other records saved
            [
                (1, dt("2000-01-01"), 1, 1, 1, 1, 1, 1, 1),
                (1, dt("2000-01-02"), 1, 1, 1, 1, 1, 1, 1),
                (2, dt("2000-01-02"), 1, 1, 1, 1, 1, 1, 1),
            ],
            [("1", "2000-01-02", 2, 2, 2, 2, 2, 2, 2)],
            [
                (1, datetime.date(2000, 1, 1), 1, 1, 1, 1, 1, 1, 1),
                (1, datetime.date(2000, 1, 2), 2, 2, 2, 2, 2, 2, 2),
            ],
        ),
        (  # Re-syncing reports by dates with new records
            [
                (1, dt("2000-01-01"), 1, 1, 1, 1, 1, 1, 1),
                (3, dt("2000-01-01"), 1, 1, 1, 1, 1, 1, 1),
                (1, dt("2000-01-02"), 2, 2, 2, 2, 2, 2, 2),
                (2, dt("2000-01-02"), 3, 3, 3, 3, 3, 3, 3),
            ],
            [
                ("1", "2000-01-01", 4, 4, 4, 4, 4, 4, 4),
                ("3", "2000-01-01", 1, 6, 6, 6, 6, 6, 6),
                ("1", "2000-01-02", 5, 5, 5, 5, 5, 5, 5),
            ],
            [
                (1, datetime.date(2000, 1, 1), 4, 4, 4, 4, 4, 4, 4),
                (3, datetime.date(2000, 1, 1), 1, 6, 6, 6, 6, 6, 6),
                (1, datetime.date(2000, 1, 2), 5, 5, 5, 5, 5, 5, 5),
            ],
        ),
    ],
)
async def test_changed_campaign_report_for_date_without_change_other_campaign_reports(
    records_in_pg,
    records_in_yt,
    expected_result,
    mock_yql_sql_operation_request,
    dm,
    con,
):
    mock_yql_sql_operation_request.return_value = iter(records_in_yt)

    async with dm._pg.acquire() as con:
        await con.copy_records_to_table(
            "category_search_report",
            records=records_in_pg,
            columns=[
                "campaign_id",
                "date",
                "created_at",
                "icon_clicks",
                "icon_shows",
                "pin_clicks",
                "pin_shows",
                "routes",
                "devices",
            ],
        )

    await dm.sync_category_search_reports()

    result = list(
        map(
            tuple,
            await con.fetch(
                """
                SELECT campaign_id, date, created_at,
                       icon_clicks, icon_shows, pin_clicks, pin_shows,
                       routes, devices
                FROM category_search_report
                ORDER BY date, campaign_id"""
            ),
        )
    )

    assert result == expected_result
