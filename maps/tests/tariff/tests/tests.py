from maps.wikimap.stat.tasks_payment.dictionaries.tariff.schema import TariffsSchema, get_duplicates

from util import get_yamls, get_all_valid_tariffs_descriptions

from collections import defaultdict
from marshmallow import ValidationError
import datetime
import pytest
import yaml


def are_closed_intervals_overlapped(intervals):
    'Checks if there is at least one pair of overlapping *closed* intervals in the list.'

    if len(intervals) == 0:
        return False

    intervals = sorted(intervals)
    prev_interval_end = intervals[0][1]
    for interval in intervals[1:]:
        if interval[0] <= prev_interval_end:
            return True
        prev_interval_end = interval[1]

    return False


def get_task_prefixes(tariffs_description):
    if 'task_prefix' in tariffs_description:
        return [tariffs_description['task_prefix']]
    if 'task_prefixes' in tariffs_description:
        return tariffs_description['task_prefixes']


@pytest.mark.parametrize('tariffs_yaml', get_yamls())
def test_should_validate_by_schema(tariffs_yaml):
    validation_error_msg = None
    try:
        TariffsSchema().load(yaml.full_load(tariffs_yaml))
    except ValidationError as e:
        validation_error_msg = e.messages

    assert validation_error_msg is None,\
        '{} in\n{}'.format(validation_error_msg, tariffs_yaml)


@pytest.mark.parametrize('tariffs_yaml', get_yamls())
def test_should_contain_only_one_yaml_document(tariffs_yaml):
    documents = list(yaml.load_all(tariffs_yaml, Loader=yaml.FullLoader))
    assert len(documents) == 1,\
        "YAML file must contain exactly one document:\n{}".format(tariffs_yaml)


def test_all_tariffs_should_not_contain_duplicate_task_names():
    task_names = []
    for tariffs_description in get_all_valid_tariffs_descriptions():
        for tariff in tariffs_description['tariffs']:
            for task in tariff['tasks']:
                task_names.append(
                    '/'.join([
                        tariffs_description['task_group'],
                        tariffs_description.get('task_subgroup', ''),
                        tariff['tariff_name'],
                        task
                    ])
                )

    duplicates = get_duplicates(task_names)
    assert duplicates is None, 'Duplicate task names: {}.'.format(duplicates)


def test_all_tariffs_should_not_contain_duplicate_task_ids():
    task_id_to_time_intervals = defaultdict(list)
    for tariffs_description in get_all_valid_tariffs_descriptions():
        for tariff in tariffs_description['tariffs']:
            start_date = tariff.get('start_date', datetime.date.min)
            end_date = tariff.get('end_date', datetime.date.max)
            for task in tariff['tasks']:
                for task_prefix in get_task_prefixes(tariffs_description):
                    task_id = task_prefix + task
                    task_id_to_time_intervals[task_id].append((start_date, end_date))

    duplicates = list()
    for task_id, time_intervals in task_id_to_time_intervals.items():
        if are_closed_intervals_overlapped(time_intervals):
            duplicates.append(task_id)

    assert len(duplicates) == 0, 'Duplicate task ids: {}.'.format(duplicates)
