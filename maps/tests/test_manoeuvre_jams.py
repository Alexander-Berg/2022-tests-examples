import datetime

from maps.analyzer.pylibs.realtime_jams.lib.build_jams import append_manoeuvre_info, ManoeuvresPath

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables

MANOEUVRES_PATH = "//manoeuvres_data/20210307-20210315/manoeuvres.mms"
CREATED = datetime.date(2021, 3, 16)


def test_append_manoeuvre_info_1(ytc):
    expected = "//append_manoeuvre_info/table1.out"
    result = append_manoeuvre_info(
        ytc,
        travel_times="//append_manoeuvre_info/table1.in",
        manoeuvres_paths=[ManoeuvresPath(MANOEUVRES_PATH, CREATED)],
    )
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_append_manoeuvre_info_2(ytc):
    expected = "//append_manoeuvre_info/table2.out"
    result = append_manoeuvre_info(
        ytc,
        travel_times="//append_manoeuvre_info/table1.in",
        manoeuvres_paths=[ManoeuvresPath(MANOEUVRES_PATH, CREATED)],
        jams_mode=True
    )
    assert_equal_tables(ytc, expected, result, unordered=True)
