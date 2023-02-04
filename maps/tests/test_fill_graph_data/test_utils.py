import difflib
import json
import os
import re

from maps.garden.sdk.test_utils import geometry
from sprav.protos import export_pb2


def create_company_row(id, source, duplicated_ids=None, is_exported=True,
                       parking_price=None, per_first_hour=None):
    company = export_pb2.TExportedCompany()
    company.Id = id
    company.Source = source
    company.CommitId = 1675887888409037667
    company.UnixTime = 1560792223000
    company.Unreliable = False
    if duplicated_ids:
        company.DuplicateId.extend(duplicated_ids)

    if parking_price:
        feature = company.Feature.add()
        feature.Id = 'parking_price'
        price = feature.ExportedValue.add()
        price.TextValue = parking_price
    if per_first_hour:
        feature = company.Feature.add()
        feature.Id = 'per_first_hour'
        price = feature.ExportedValue.add()
        price.TextValue = per_first_hour

    return {
        'id': id,
        'export_proto': company.SerializeToString(),
        'is_exported': is_exported
    }


def companies_data(TEST_DATA_DIR):
    with open(os.path.join(TEST_DATA_DIR, 'companies.json')) as file:
        companies = json.load(file)
        return [create_company_row(**company) for company in companies]


def assert_files_equal(expected_file,
                       actual_file,
                       sort=False,
                       **formatters):
    with open(actual_file) as f:
        actual_lines = f.readlines()
    with open(expected_file) as f:
        expected_lines = f.readlines()
    assert_lines_equal(
        expected_lines,
        actual_lines,
        sort=sort,
        prefix='For {expected} and {actual}:'.format(
            expected=expected_file,
            actual=actual_file),
        **formatters)


def assert_lines_equal(expected,
                       actual,
                       sort=False,
                       prefix='',
                       **formatters):
    if sort:
        expected = sorted(expected)
        actual = sorted(actual)

    # First compare corresponding lines
    for line_index, (expected_line, generated_line) in enumerate(
            zip(expected + [None] * len(actual),
                actual + [None] * len(expected))):

        if (expected_line is None) != (generated_line is None):
            assert False, \
                ('{prefix} line {number} differs: '
                 '{expected} vs {generated}'.format(
                     prefix=prefix,
                     number=line_index + 1,
                     expected=expected_line,
                     generated=generated_line))

        if expected_line is None and generated_line is None:
            continue

        if formatters:
            expected_line = expected_line.format(**formatters)

        assert_strings_equal(
            expected_line,
            generated_line,
            prefix='{prefix} line {number} differs:'.format(
                prefix=prefix,
                number=line_index + 1))


def assert_strings_equal(expected, actual, prefix=''):
    expected_re = re.escape(expected)
    expected_re = expected_re.replace(re.escape('__WHATEVER__'), '.*')
    expected_re = '^' + expected_re + '$'

    if re.match(expected_re, actual):
        return

    expected = '' if expected is None else expected
    actual = '' if actual is None else actual

    for char_index, (expected_char, actual_char) in enumerate(
            zip(expected + ' ' * len(actual),
                actual + ' ' * len(expected))):

        if expected_char == actual_char:
            continue

        start_index = max(0, char_index - 35)
        end_index = start_index + 70
        message = prefix
        message += '\nExpected: '
        message += expected[start_index:end_index]
        message += '\n  Actual: '
        message += actual[start_index:end_index]
        message += '\n          '
        message += re.sub('[^ \t]', ' ', actual[start_index:char_index])
        message += '^\n'
        raise AssertionError(message)


def validate_several_tables(result, data_dir):
    tables_data = ((resource.table_name, resource.read_table())
                   for resource in result.values())
    _validate_result_data_for_tables(tables_data, data_dir)


def read_table_from_file(data_dir, table_name, fail_if_not_exists=True):
    filepath = os.path.join(data_dir, table_name + '.jsonl')
    if (fail_if_not_exists):
        assert os.path.exists(filepath),\
            f'File with data for {table_name} does not exists.'
    return list(geometry.convert_wkt_to_wkb_in_jsonl_file(filepath,
                                                          ['geom_geo']))


def _format_table(table):
    sorted_table = sorted([tuple(sorted(row.items())) for row in table])
    return [json.dumps(row) for row in sorted_table]


def _validate_result_data_for_tables(tables_data, data_dir):
    errors = ''
    for table_name, result_data in tables_data:
        result_data = _format_table(list(result_data))
        expected_data = _format_table(read_table_from_file(data_dir, table_name))
        if result_data != expected_data:
            errors += '\nTable:' + table_name + ' ' + data_dir + '\n'
            errors += ('Found a difference in expected and generated table:'
                       + '\n- Expected\n+ Generated\n? Comments\n'
                       + '\n'.join(difflib.ndiff(expected_data, result_data)))
    if errors:
        raise AssertionError(errors)
