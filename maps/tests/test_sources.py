from datetime import date

import mock
import pytest

from maps.analyzer.pylibs.envkit.paths import PathsConfig
import maps.analyzer.pylibs.envkit.sources as s


class Config(PathsConfig):
    SIGNALS = '//{env}/signals'
    TRAVEL_TIMES = '//{env}/travel_times'
    ASSESSORS = '//{env}/assessors'
    STATS = '//{env}/stats'
    METRICS = '//{env}/metrics'
    EMPTY = '//{env}/empty'
    DATA = '//{env}/data'
    TRAINING_TRACKS = '//{env}/training_tracks'


CYPRESS = {
    Config.SIGNALS: [1, 2, 3, 4, 5, 6],
    Config.TRAVEL_TIMES: [1, 2, 3, 4, 5],
    Config.ASSESSORS: [2, 3, 4, 5, 6],
    Config.STATS: [1, 2, 3, 4],
    Config.METRICS: [1, 2, 3],
    Config.EMPTY: [],

    Config.DATA: [3, 5],
    Config.TRAINING_TRACKS: [3, 4],
}

YTC = object()


def day(d):
    return date(2020, 11, d)


def days(*ds):
    return sorted(map(day, ds))


def list_days(_ytc, src):
    return s.DateRanges.from_list(days(*CYPRESS[src]))


def data_source(src):
    def get_info(ytc):
        min_date = min(list_days(ytc, src), default=None)
        return s.PlaceInfo(
            dates=s.DateRanges.after(min_date) if min_date is not None else s.DateRanges.empty(),
            predicate=True,
        )

    return s.Place(
        path=src,
        info=get_info,
        target=False,
    )


MATCH_SIGNALS = s.plan(
    s.source(Config.SIGNALS),
    s.target(Config.TRAVEL_TIMES),
    s.target(Config.ASSESSORS),
)

CALC_METRICS = s.plan(
    s.source(Config.TRAVEL_TIMES),
    s.source(Config.ASSESSORS),
    s.target(Config.STATS),
    s.target(Config.METRICS),
)

EMPTY_METRICS = s.plan(
    s.source(Config.EMPTY),
    s.target(Config.METRICS),
)

JOIN_JAMS = s.plan(
    s.source(Config.ASSESSORS),
    data_source(Config.DATA),
    s.target(Config.TRAINING_TRACKS),
)


@mock.patch('maps.analyzer.pylibs.envkit.sources._list_days', side_effect=list_days)
def test_sources(_l):
    def schedule_match(force=False):
        return s.schedule(
            YTC,
            MATCH_SIGNALS,
            force=force,
            update_env=False,
        )

    def schedule_quality(force=False):
        return s.schedule(
            YTC,
            CALC_METRICS,
            force=force,
        )

    assert Config.ASSESSORS.value == '//production/assessors'

    p = schedule_match()

    # should not update target env (update_env is False)
    assert Config.ASSESSORS.value == '//production/assessors'

    assert p.scheduled == days(1, 6)
    assert not p.recalc
    assert not p.failed

    assert Config.STATS.value == '//production/stats'

    p = schedule_quality()

    # should update target env (update_env is True by default)
    assert Config.STATS.value == '//dev/stats'

    assert p.scheduled == days(4, 5)
    assert not p.recalc
    assert p.failed == {
        day(6): [Config.TRAVEL_TIMES],
    }

    p = schedule_match(force=True)

    assert p.scheduled == days(1, 2, 3, 4, 5, 6)
    assert p.recalc == days(1, 2, 3, 4, 5, 6)
    assert not p.failed

    p = schedule_quality(force=True)

    assert p.scheduled == days(2, 3, 4, 5)
    assert p.recalc == days(2, 3, 4)
    assert p.failed == {
        day(1): [Config.ASSESSORS],
        day(6): [Config.TRAVEL_TIMES],
    }


@mock.patch('maps.analyzer.pylibs.envkit.sources._list_days', side_effect=list_days)
def test_empty_sources(_l):
    p = s.schedule(YTC, EMPTY_METRICS, force=True)
    assert not p.scheduled
    assert not p.recalc
    assert not p.failed

    p = s.schedule(
        YTC, EMPTY_METRICS, force=True,
        begin_date=day(1),
        end_date=day(2),
    )

    assert not p.scheduled
    assert not p.recalc
    assert p.failed == {
        day(1): [Config.EMPTY],
        day(2): [Config.EMPTY],
    }

    p = s.schedule(
        YTC, EMPTY_METRICS, force=True,
        end_date=day(3),
        days_back=1
    )

    assert not p.scheduled
    assert not p.recalc
    assert p.failed == {
        day(2): [Config.EMPTY],
        day(3): [Config.EMPTY],
    }

    with pytest.raises(ValueError):
        p = s.schedule(
            YTC, EMPTY_METRICS, force=True,
            begin_date=day(1),
            end_date=day(3),
            days_back=1
        )

    with pytest.raises(ValueError):
        s.plan(s.source(Config.EMPTY))
    with pytest.raises(ValueError):
        s.plan(s.target(Config.METRICS))


@mock.patch('maps.analyzer.pylibs.envkit.sources._list_days', side_effect=list_days)
def test_data_source(_l):
    p = s.schedule(
        YTC,
        JOIN_JAMS,
    )

    assert p.scheduled == days(5, 6)
    assert not p.recalc
    assert p.failed == {
        day(2): [Config.DATA],
    }
