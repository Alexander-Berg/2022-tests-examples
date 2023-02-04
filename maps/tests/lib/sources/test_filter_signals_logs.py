from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
import maps.analyzer.pylibs.schema as s
import maps.analyzer.toolkit.lib.schema as schema

from maps.analyzer.toolkit.lib.sources import filter_signals_logs


def test_filter_signals_logs(ytc):
    datasets = [{'expected': '//filter_signals_logs/table.out', 'srcs': ['//filter_signals_logs/table.in'], 'no_extras': True}]
    filter_signals_logs_test(ytc, datasets)


def test_filter_signals_logs_with_extra_option_on(ytc):
    datasets = [{'expected': '//filter_signals_logs/table.out', 'srcs': ['//filter_signals_logs/table.in.extra'], 'no_extras': True}]
    filter_signals_logs_test(ytc, datasets)


def test_filter_signals_logs_with_extra(ytc):
    datasets = [{'expected': '//filter_signals_logs/table.out.extra', 'srcs': ['//filter_signals_logs/table.in.extra'], 'no_extras': False}]
    filter_signals_logs_test(ytc, datasets)


def test_filter_signals_logs_with_no_extra_option_off(ytc):
    datasets = [{'expected': '//filter_signals_logs/table.out.extra_null', 'srcs': ['//filter_signals_logs/table.in'], 'no_extras': False}]
    filter_signals_logs_test(ytc, datasets)


def filter_signals_logs_test(ytc, datasets):
    results = dict(
        (
            dataset['expected'],
            filter_signals_logs(
                ytc,
                srcs=dataset['srcs'],
                has_no_extra_columns=dataset['no_extras']
            )
        )
        for dataset in datasets
    )

    for expected, [result] in results.items():
        assert_equal_tables(
            ytc,
            expected,
            result,
            float_columns=[
                col.name for col in schema.SIGNALS_TABLE.columns
                if col.type.unwrapped == s.Double  # TODO: Use `get_table_float_column_names`?
            ],
            null_equals_unexistant=True,
        )
