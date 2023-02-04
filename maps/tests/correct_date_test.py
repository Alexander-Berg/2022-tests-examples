from collections import namedtuple
import mock
import pytest

from maps.pylibs.yt.lib import YtContext

from maps.carparks.tools.carparks_miner.lib import tables
from maps.carparks.tools.carparks_miner.lib.date_utils import to_date


Params = namedtuple('Params', [
    'begin_date',
    'end_date',
    'last_clusterized_date',
    'last_geocoded_date',
    'last_source_date'
])


@pytest.fixture()
def mock_and_run_get_corrected_dates(args):
    begin_date = args.begin_date
    end_date = args.end_date

    ytc = mock.MagicMock(spec_set=YtContext)
    yt_root = '//some_path'

    def _side_effect(ytc, table_name_function):
        if table_name_function.func == tables.clusterized_points_table_name:
            return args.last_clusterized_date
        elif table_name_function.func == tables.geocoded_suggested_points_table_name:
            return args.last_geocoded_date

    tables._get_last_date_for_table = mock.MagicMock(side_effect=_side_effect)

    tables.get_last_date_with_source_tables = mock.MagicMock(
        return_value=args.last_source_date
    )

    return tables.get_corrected_dates(ytc, yt_root, begin_date, end_date)


@pytest.mark.parametrize('args', [Params(
    begin_date=to_date('2020-05-11'),
    end_date=to_date('2020-05-17'),
    last_clusterized_date=to_date('2020-05-21'),
    last_geocoded_date=to_date('2020-05-21'),
    last_source_date=to_date('2020-05-25')
)])
def test_begin_and_end_date_remain_the_same(mock_and_run_get_corrected_dates):
    assert mock_and_run_get_corrected_dates == \
        (to_date('2020-05-11'), to_date('2020-05-17'))


@pytest.mark.parametrize('args', [Params(
    begin_date=None,
    end_date=to_date('2020-04-07'),
    last_clusterized_date=to_date('2020-04-05'),
    last_geocoded_date=to_date('2020-04-08'),
    last_source_date=to_date('2020-04-09')
)])
def test_begin_date_is_last_clusterized_date_plus_one_day(
        mock_and_run_get_corrected_dates):
    assert mock_and_run_get_corrected_dates == \
        (to_date('2020-04-06'), to_date('2020-04-07'))


@pytest.mark.parametrize('args', [Params(
    begin_date=to_date('2020-03-03'),
    end_date=None,
    last_clusterized_date=to_date('2020-03-04'),
    last_geocoded_date=to_date('2020-03-04'),
    last_source_date=to_date('2020-03-08')
)])
def test_end_date_is_last_source_date_if_clusterized_date_equals_geocoded_date(
        mock_and_run_get_corrected_dates):
    assert mock_and_run_get_corrected_dates == \
        (to_date('2020-03-03'), to_date('2020-03-08'))


@pytest.mark.parametrize('args', [Params(
    begin_date=None,
    end_date=None,
    last_clusterized_date=to_date('2020-02-11'),
    last_geocoded_date=to_date('2020-02-12'),
    last_source_date=to_date('2020-02-15')
)])
def test_end_date_is_last_geocoded_date_if_clusterized_date_less_than_geocoded_date(
        mock_and_run_get_corrected_dates):
    assert mock_and_run_get_corrected_dates == \
        (to_date('2020-02-12'), to_date('2020-02-12'))
