import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.mock_ch_query_log
@pytest.mark.parametrize(
    ["solomon_period", "solomon_datetime", "from_datetime", "to_datetime"],
    [
        (
            "30s",
            "2020-07-28T00:00:30Z",
            dt("2020-07-28 00:00:00"),
            dt("2020-07-28 00:00:30"),
        ),
        (
            "15m",
            "2020-07-28T00:15:00Z",
            dt("2020-07-28 00:00:00"),
            dt("2020-07-28 00:15:00"),
        ),
    ],
)
async def test_returns_expected(
    from_datetime,
    to_datetime,
    solomon_period,
    solomon_datetime,
    ch_query_log,
    api_provider,
    freezer,
):
    freezer.move_to(to_datetime)

    ch_query_log.retrieve_metrics_for_queries.coro.return_value = [
        {"kek": "lol"},
        {"metrics": "keklol"},
    ]

    got = await api_provider.calculate_monitoring_data_for_queries(
        solomon_datetime, solomon_period
    )

    assert got == {"metrics": [{"kek": "lol"}, {"metrics": "keklol"}]}
    assert ch_query_log.retrieve_metrics_for_queries.call_args[0] == ()
    assert ch_query_log.retrieve_metrics_for_queries.call_args[1] == {
        "from_datetime": from_datetime,
        "to_datetime": to_datetime,
    }
    assert ch_query_log.retrieve_metrics_for_queries.call_count == 1
