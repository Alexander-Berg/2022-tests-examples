# coding: utf-8

import itertools
import json

PREFIX = '/Users/aikawa/Work/MNCLOSE/'


def return_coverage_list():
    data = open('/Users/aikawa/Work/SQL/04-10-16/t_hash_data.tsv', 'r')
    headers = ['coverage_id', 'coverage']
    coverage_list = []
    for line in data:
        coverage_list.append(dict(zip(headers, line.split('||+|'))))
    return coverage_list


def collect_all_file_names(coverage_list):
    file_names_set = set()
    for coverage in coverage_list:
        json_acceptable_string = coverage['coverage'].replace("'", "\"")
        json_string = (json.loads(json_acceptable_string))['coverage_data']
        file_names_set = file_names_set.union(set(json_string.keys()))
    return sorted(list(file_names_set))


def return_sorted_dict(keys_list=None):
    result_dict = {}
    coverage_list = return_coverage_list()
    if keys_list is None:
        keys_list = collect_all_file_names(coverage_list)
    for key in keys_list:
        dict_list = []
        for coverage in coverage_list:
            json_acceptable_string = coverage['coverage'].replace("'", "\"")
            json_string = (json.loads(json_acceptable_string))['coverage_data']
            if key in json_string:
                dict_list.append({key: json_string[key]})
        keys = lambda c: c.items()
        result = list(itertools.groupby(sorted(dict_list, key=keys), key=keys))
        print key, len(result)
        result_dict.update({key: [result[x][0][0][1] for x in range(0, len(result))]})
    return result_dict


def all_rows_from_all_version(result):
    print result
    print len(result)
    row_set = set(result[1])
    for x in range(1, len(result)):
        row_set = row_set.union(result[x])
    return row_set


def common_row_for_all_versions(result):
    row_set = set(result[1])
    for x in range(1, len(result)):
        row_set = row_set.intersection(result[x])
    return row_set


def create_versions(result, file_name):
    all_rows = all_rows_from_all_version(result)
    common_rows = common_row_for_all_versions(result)
    for version in range(len(result)):
        row_set = result[version]
        file = open('{0}{1}.py'.format(PREFIX, file_name), 'r')
        file_write_to = open('{0}{1}{2}.py'.format(PREFIX, file_name, version), 'w')
        file_write_to_common = open('{0}{1}_all.py'.format(PREFIX, file_name), 'w')
        for index, file_line in enumerate(file):
            if index + 1 in all_rows:
                file_write_to_common.write('-5\t{0}'.format(file_line))
            else:
                file_write_to_common.write('\t{0}'.format(file_line))
        for index, file_line in enumerate(file):
            if index + 1 in row_set and index + 1 not in (common_rows):
                file_write_to.write('-5\t{0}'.format(file_line))
            elif index + 1 in row_set and index + 1 in (common_rows):
                file_write_to.write('-3\t{0}'.format(file_line))
            else:
                file_write_to.write('\t{0}'.format(file_line))


def print_variations(result):
    _dict = {}
    for key in result.keys():
        _dict.update({key: len(result[key])})
    return _dict


def make_list_of_non_common_intervals(result_list):
    common_rows = list(common_row_for_all_versions(result_list))
    _set = set()
    for result in result_list:
        intervals = [list(g) for k, g in itertools.groupby(result, key=lambda x: x not in common_rows) if k]
        for interval in intervals:
            _set.add(tuple(interval))
    sorted_sets = sorted(_set, key=lambda tup: tup[0])
    for _set in sorted_sets:
        print _set
    return sorted_sets


def create_versions_of_intervals(sets_of_intervals, file_name):
    for index, _set in enumerate(sets_of_intervals):
        file = open('{0}{1}.py'.format(PREFIX, file_name), 'r')
        file_write_to = open('{0}{1}{2}_interval.py'.format(PREFIX, file_name, index), 'w')
        for index, file_line in enumerate(file):
            if index + 1 in _set:
                file_write_to.write('-5\t{0}'.format(file_line))


keys_list = [
    # '/usr/share/pyshared/balance/actions/acts/accounter.py'
    # '/usr/share/pyshared/balance/processors/month_proc.py'
    # '/usr/share/pyshared/balance/actions/unified_account.py',
    # '/usr/share/pyshared/balance/mapper/extprops.py'
    #   '/usr/share/pyshared/balance/actions/acts/completion_cache.py'
    '/usr/share/pyshared/balance/actions/acts/account.py'

]
# '/usr/share/pyshared/balance/actions/acts/account.py']
# result = return_sorted_dict()
result = return_sorted_dict(keys_list)

print result
variations_list = result[keys_list[0]]
create_versions(variations_list, 'account')
sets_of_intervals = make_list_of_non_common_intervals(variations_list)
create_versions_of_intervals(sets_of_intervals, 'account')
