import os

from yt.wrapper import YsonFormat

from maps.analyzer.toolkit.lib.build_historic_jams import (
    average_travel_times,
    build_historic_jams,
    shift_travel_times,
)
from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
import maps.analyzer.toolkit.lib.schema as schema


def expected_result_table(input_table):
    return os.path.splitext(input_table)[0] + ".out"


def input_tables(ytc, test_dir):
    for table in ytc.list(test_dir):
        if table.endswith(".in"):
            yield test_dir + "/" + table


def test_shift_travel_times(ytc):
    for input_table in input_tables(ytc, "//shift_travel_times"):
        result = ytc.create_temp_table()
        with shift_travel_times() as shift_bin:
            ytc.run_map(
                mapper=shift_bin,
                source_table=input_table,
                destination_table=result,
                input_format=YsonFormat(),
                output_format=YsonFormat(),
            )
        assert_equal_tables(
            ytc, result,
            expected_result_table(input_table)
        )


def test_average_travel_times(ytc):
    # A dict of testing table names and arguments for the binary.
    test_cases = {
        "simple_average.in": {
            "grouping_period": 15,
            "interval_back": 60,
            "interval_forward": 60,
            "min_group_size": 1
        },
        "insufficient_group_size.in": {
            "grouping_period": 15,
            "interval_back": 60,
            "interval_forward": 60,
            "min_group_size": 5
        }
    }

    for table_name, reducer_args in test_cases.items():
        input_table = "//average_travel_times/" + table_name

        result = ytc.create_temp_table()

        with average_travel_times(**reducer_args) as average_bin:
            ytc.run_map_reduce(
                mapper=None,
                reducer=average_bin,
                source_table=input_table,
                destination_table=result,
                format=YsonFormat(),
                reduce_by=[schema.PERSISTENT_ID.name, schema.SEGMENT_INDEX.name],
                spec={
                    'reduce_job_io': {'control_attributes': {'enable_key_switch': True}},
                },
            )

        assert_equal_tables(
            ytc,
            expected_result_table(input_table),
            result,
            float_columns=["travel_time", "geolength"],
            unordered=True,
        )


def test_build_historic_jams(ytc):
    # This is basically an integration test
    # between shift_travel_times and average_travel_times
    # binaries.
    reducer_args = {
        "grouping_period": 15,
        "interval_back": 60,
        "interval_forward": 60,
        "min_group_size": 1
    }

    def verify_historic_jams(inputs_tables, expected_result):
        result = build_historic_jams(
            ytc,
            inputs_tables,
            **reducer_args
        )
        assert_equal_tables(
            ytc,
            expected_result,
            result,
            float_columns=["travel_time"],
            unordered=True,
        )

    def test_table(table_name):
        return "//build_historic_jams/" + table_name

    verify_historic_jams(
        [test_table("integration_test.in")],
        test_table("integration_test.out"),
    )
    verify_historic_jams(
        [
            test_table("multiple_tables_1.in"),
            test_table("multiple_tables_2.in")
        ],
        test_table("multiple_tables.out"),
    )
