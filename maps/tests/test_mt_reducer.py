import pytest

import maps.analyzer.pylibs.envkit as envkit
import maps.analyzer.pylibs.test_tools as test_tools

from yt.wrapper import YsonFormat

from maps.pylibs.yt.lib import Params, BinaryCmd
from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables


@pytest.fixture(scope='session')
def ytc():
    with test_tools.local_ytc(
        local_cypress_dir='maps/analyzer/libs/mt_jobs/tests/cypress'
    ) as ctx:
        yield ctx


def test_mt_reducer_one_thread(ytc):
    expected = "//reducer/table1_1.out"
    numbers = "//reducer/table1.in"
    result = filter_even(numbers, ytc)
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_mt_reducer_two_threads(ytc):
    expected = "//reducer/table1_2.out"
    numbers = "//reducer/table1.in"
    result = filter_even(numbers, ytc, threads=2)
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_mt_reducer_ten_threads(ytc):
    expected = "//reducer/table1_10.out"
    numbers = "//reducer/table1.in"
    result = filter_even(numbers, ytc, threads=10)
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_mt_reducer_jobs_skip_rows(ytc):
    expected = "//reducer/table1_skip.out"
    numbers = "//reducer/table1.in"
    result = filter_even(numbers, ytc, threads=2, skip=True)
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_mt_reducer_multiple_tables(ytc):
    expected = "//reducer/table2.out"
    result = filter_even(
        ["//reducer/table1.in", "//reducer/table2.in"],
        ytc,
        threads=3
    )
    assert_equal_tables(ytc, expected, result, unordered=True)


def filter_even(numbers, ytc, threads=1, skip=False):
    numbers_list = numbers if isinstance(numbers, list) else [numbers]
    result = ytc.create_temp_table()
    params = Params()
    params.arg("--inputs-count", len(numbers_list))
    params.arg("--threads", threads)
    params.flag("--skip", skip)

    for tab in numbers_list:
        ytc.run_sort(
            tab,
            sort_by=["key"]
        )

    with envkit.resource_tool('/maps/analyzer/libs/mt_jobs/tests/bin/mt_reducer') as reducer_bin:
        reducer = BinaryCmd(
            reducer_bin,
            params,
        )
        ytc.run_reduce(
            reducer=reducer,
            source_table=numbers_list,
            destination_table=result,
            reduce_by=["key"],
            format=YsonFormat(),
        )
        return result
