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


def test_mt_mapper_one_thread(ytc):
    expected = "//mapper/table1_1.out"
    numbers = "//mapper/table1.in"
    result = square_numbers(numbers, ytc)
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_mt_mapper_two_threads(ytc):
    expected = "//mapper/table1_2.out"
    numbers = "//mapper/table1.in"
    result = square_numbers(numbers, ytc, threads=2)
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_mt_mapper_ten_threads(ytc):
    expected = "//mapper/table1_10.out"
    numbers = "//mapper/table1.in"
    result = square_numbers(numbers, ytc, threads=10)
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_mt_mapper_not_valid_table(ytc):
    with pytest.raises(Exception):
        square_numbers(
            "//mapper/fail_table.in",
            ytc,
            threads=2
        )


def test_mt_mapper_multiple_tables(ytc):
    expected_list = ["//mapper/table1_2.out",
                     "//mapper/table2.out"]
    result_list = square_numbers(
        ["//mapper/table1.in", "//mapper/table2.in"],
        ytc,
        threads=2
    )
    for expected, result in zip(expected_list, result_list):
        assert_equal_tables(ytc, expected, result, unordered=True)


def square_numbers(numbers, ytc, threads=1):
    numbers_list = numbers if isinstance(numbers, list) else [numbers]
    result_list = [
        ytc.create_temp_table()
        for _ in numbers_list
    ]
    params = Params()
    params.arg("--inputs-count", len(numbers_list))
    params.arg("--threads", threads)

    with envkit.resource_tool('/maps/analyzer/libs/mt_jobs/tests/bin/mt_mapper') as matcher:
        mapper = BinaryCmd(
            matcher,
            params,
        )
        ytc.run_map(
            mapper, numbers_list, result_list, format=YsonFormat()
        )
        return result_list if isinstance(numbers, list) else result_list[0]
