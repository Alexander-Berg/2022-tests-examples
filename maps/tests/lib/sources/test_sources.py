from dateutil.parser import parse

from yt.wrapper import ypath_join

from maps.pylibs.yt.lib import YtContext

import maps.analyzer.toolkit.lib as tk
import maps.analyzer.toolkit.lib.sources as tks


def test_sources(ytc, schematize, sources_fixture):
    signals_count = sum([
        ytc.get(ypath_join(cached_signals_for('2017-11-03'), '@row_count')),
        ytc.get('//sources/logs/1d/2017-11-04/@row_count'),
        # logs for 2017-11-05 could possibly have signals for previous date
        # but in our test data there are no such rows
    ])

    signals = tks.fetch_signals(ytc, '20171103T000000', '20171104T235959', create=True)
    assert ytc.get(ypath_join(signals, '@row_count')) == signals_count

    ttimes = tks.fetch_travel_times(ytc, '20171102', '20171104', cache=True, create=True)
    assert ytc.get(ypath_join(ttimes, '@row_count')) != 0
    # ensure cache created
    assert ytc.exists(cached_travel_times_for('2017-11-03'))
    assert ytc.exists(cached_signals_for('2017-11-04'))
    assert ytc.exists(cached_travel_times_for('2017-11-04'))


def cached_signals_for(date):
    return tks.path_for_date(tk.paths.Caches.SIGNALS_CACHE, date)


def cached_travel_times_for(date):
    return tks.path_for_date(tk.paths.Caches.TRAVEL_TIMES_CACHE, date)


def test_find_suitable_statistical_data(ytc):
    ytc.create('file', '//sources/find_suitables/statistical_data/X-Y-20191012/statistical_data.fb', recursive=True)
    ytc.create('file', '//sources/find_suitables/statistical_data/X-Y-20191013/statistical_data.fb', recursive=True)

    expected = '//sources/find_suitables/statistical_data/X-Y-20191013/statistical_data.fb'
    assert tks.find_suitable_statistical_data(ytc, '//sources/find_suitables/statistical_data', parse('2019-10-14').date()) == expected
    expected = '//sources/find_suitables/statistical_data/X-Y-20191012/statistical_data.fb'
    assert tks.find_suitable_statistical_data(ytc, '//sources/find_suitables/statistical_data', parse('2019-10-13').date()) == expected

    assert tks.find_suitable_statistical_data(ytc, '//sources/find_suitables/statistical_data', parse('1970-01-01').date()) is None


def test_find_suitable_user_traits(ytc):
    ytc.create('file', '//sources/find_suitables/user_traits/X-Y-20191012/user_traits_data.mms', recursive=True)
    ytc.create('file', '//sources/find_suitables/user_traits/X-Y-20191013/user_traits_data.mms', recursive=True)

    expected = '//sources/find_suitables/user_traits/X-Y-20191013/user_traits_data.mms'
    assert tks.find_suitable_user_traits(ytc, '//sources/find_suitables/user_traits', parse('2019-10-14').date()) == expected
    expected = '//sources/find_suitables/user_traits/X-Y-20191012/user_traits_data.mms'
    assert tks.find_suitable_user_traits(ytc, '//sources/find_suitables/user_traits', parse('2019-10-13').date()) == expected

    assert tks.find_suitable_user_traits(ytc, '//sources/find_suitables/user_traits', parse('1970-01-01').date()) is None


def test_find_suitable_model(ytc):
    ytc.create('file', '//sources/find_suitables/model_first/latest/matrixnet.info', recursive=True)
    ytc.create('file', '//sources/find_suitables/model_first/latest/config.meta', recursive=True)
    ytc.create('file', '//sources/find_suitables/model_first/2019-10-13.01/matrixnet.info', recursive=True)
    ytc.create('file', '//sources/find_suitables/model_first/2019-10-13.01/config.meta', recursive=True)
    ytc.create('file', '//sources/find_suitables/model_first/2019-10-12.16/matrixnet.info', recursive=True)
    ytc.create('file', '//sources/find_suitables/model_first/2019-10-12.16/config.meta', recursive=True)

    model, config = tks.find_suitable_model(ytc, '//sources/find_suitables/model_first', parse('2019-10-14').date())
    assert model == '//sources/find_suitables/model_first/2019-10-13.01/matrixnet.info'
    assert config == '//sources/find_suitables/model_first/2019-10-13.01/config.meta'

    model, config = tks.find_suitable_model(ytc, '//sources/find_suitables/model_first', parse('2019-10-13').date())
    assert model == '//sources/find_suitables/model_first/2019-10-12.16/matrixnet.info'
    assert config == '//sources/find_suitables/model_first/2019-10-12.16/config.meta'

    ytc.create('file', '//sources/find_suitables/model_second/2019-10-13.01/matrixnet.info', recursive=True)  # without config
    ytc.create('file', '//sources/find_suitables/model_second/2019-10-12.16/matrixnet.info', recursive=True)
    ytc.create('file', '//sources/find_suitables/model_second/2019-10-12.16/config.meta', recursive=True)

    assert tks.find_suitable_model(ytc, '//sources/find_suitables/model_second', parse('2019-10-14').date()) == (None, None)


def test_path_or_date(ytc):
    assert tks.path_for_date(tk.paths.Caches.SIGNALS_CACHE, '2017-11-02') == '//sources/cache/signals/2017-11-02'
    assert tks.path_for_date(
        tk.paths.Caches.SIGNALS_CACHE, '2017-11-05', check_exists=False
    ) == '//sources/cache/signals/2017-11-05'
    assert tks.path_for_date(tk.paths.Caches.SIGNALS_CACHE, '2017-11-05', ytc=ytc, check_exists=True) is None


def test_find_suitable_version_in_pool(ytc: YtContext):
    pool = '//find_suitable_version_in_pool'
    day_1 = '2042-01-01'
    day_2 = '2042-01-02'
    day_3 = '2042-01-03'

    ytc.create('map_node', pool)

    ytc.create('map_node', ypath_join(pool, day_1))
    ytc.create('file', ypath_join(pool, day_1, 'foo'))
    ytc.create('file', ypath_join(pool, day_1, 'bar'))

    ytc.create('map_node', ypath_join(pool, day_2))
    ytc.create('file', ypath_join(pool, day_2, 'foo'))

    ytc.create('map_node', ypath_join(pool, day_3))
    ytc.create('file', ypath_join(pool, day_3, 'foo'))
    ytc.create('file', ypath_join(pool, day_3, 'bar'))

    def exist_foo_and_bar(ytc, path):
        return ytc.exists(ypath_join(path, 'foo')) and ytc.exists(ypath_join(path, 'bar'))

    def exist_foo(ytc, path):
        return ytc.exists(ypath_join(path, 'foo'))

    assert tks.find_suitable_version_in_pool(
        ytc, pool, parse(day_3).date(), '%Y-%m-%d', converter=lambda x: x, predicate=exist_foo_and_bar,
    ) == ypath_join(pool, day_1)

    assert tks.find_suitable_version_in_pool(
        ytc, pool, parse(day_3).date(), '%Y-%m-%d', converter=lambda x: x, predicate=exist_foo,
    ) == ypath_join(pool, day_2)

    assert tks.find_suitable_version_in_pool(
        ytc, pool, parse(day_2).date(), '%Y-%m-%d', converter=lambda x: x, predicate=exist_foo,
    ) == ypath_join(pool, day_1)

    assert tks.find_suitable_version_in_pool(
        ytc, pool, parse(day_3).date(), '%Y-%m-%d', converter=lambda x: x,
    ) == ypath_join(pool, day_2)
