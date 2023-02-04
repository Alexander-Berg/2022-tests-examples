# -*- coding: utf-8 -*-

import pytest
import hamcrest
from StringIO import StringIO
from balance.completions_fetcher.configurable_partner_completion import CSVIterator, IncompleteRead


def assert_dicts_equal(a, b):
    for actual_dict, expected_dict in zip(a, b):
        hamcrest.assert_that(actual_dict, hamcrest.has_entries(expected_dict))


@pytest.mark.parametrize('delimiter', [',', ';', '\t'])
def test_csv_iterator_delimiter(delimiter):
    data = '151198,,2019-01-20,66,10,5.45,545\n'\
           '2294597,,2019-01-20,15,0,0,0\n'\
           '141487,,2019-01-20,873,1,1.52,152\n'
    default_delimiter = ','
    fieldnames = ['place_id', 'vid', 'dt', 'shows', 'clicks', 'bucks', 'mbucks']
    expected = [
        {'place_id': '151198', 'vid': '', 'dt': '2019-01-20', 'shows': '66', 'clicks': '10', 'bucks': '5.45',
            'mbucks': '545'},
        {'place_id': '2294597', 'vid': '', 'dt': '2019-01-20', 'shows': '15', 'clicks': '0', 'bucks': '0',
            'mbucks': '0'},
        {'place_id': '141487', 'vid': '', 'dt': '2019-01-20', 'shows': '873', 'clicks': '1', 'bucks': '1.52',
            'mbucks': '152'}
    ]

    data = data.replace(default_delimiter, delimiter)
    data = StringIO(data)
    iterator = CSVIterator(fieldnames=fieldnames, delimiter=delimiter)
    actual = list(iterator.process(data))
    assert_dicts_equal(actual, expected)


@pytest.mark.parametrize('skip_header, skip_footer, expected_from, expected_to', [
    [False, False, 0, 3],
    [False, True,  0, 2],
    [True,  False, 1, 3],
    [True,  True,  1, 2],
])
def test_csv_iterator_skip(skip_header, skip_footer, expected_from, expected_to):
    data = '20190120000000	99758	542	9	0	1	0	0.0000	0	1651\n'\
           '20190120000000	90114	542	1	0	654	0	0.0000	0	395\n'\
           '20190120000000	90114	542	6	0	17	0	0.0000	0	395\n'
    fieldnames = ['dt', 'place_id', 'page_id', 'completion_type', 'type', 'shows', 'clicks', 'bucks', 'mbucks', 'hits']
    delimiter = '\t'

    expected = [
        {'dt': '20190120000000', 'place_id': '99758', 'page_id': '542',
         'completion_type': '9', 'type': '0', 'shows': '1', 'clicks': '0',
         'bucks': '0.0000', 'mbucks': '0', 'hits': '1651'},
        {'dt': '20190120000000', 'place_id': '90114', 'page_id': '542',
         'completion_type': '1', 'type': '0', 'shows': '654', 'clicks': '0',
         'bucks': '0.0000', 'mbucks': '0', 'hits': '395'},
        {'dt': '20190120000000', 'place_id': '90114', 'page_id': '542',
         'completion_type': '6', 'type': '0', 'shows': '17', 'clicks': '0',
         'bucks': '0.0000', 'mbucks': '0', 'hits': '395'}
    ]

    data = StringIO(data)
    iterator = CSVIterator(fieldnames=fieldnames, delimiter=delimiter,
                           skip_header=skip_header, skip_footer=skip_footer)
    actual = list(iterator.process(data))
    assert_dicts_equal(actual, expected[expected_from:expected_to])


def test_csv_iterator_mandatory_footer():
    data = '151198;;2019-01-20;66;10;5.45;545\n'\
           '#END'
    fieldnames = ['place_id', 'vid', 'dt', 'shows', 'clicks', 'bucks', 'mbucks']
    expected = [
        {'place_id': '151198', 'vid': '', 'dt': '2019-01-20', 'shows': '66', 'clicks': '10', 'bucks': '5.45',
            'mbucks': '545'},
    ]

    data = StringIO(data)
    iterator = CSVIterator(fieldnames=fieldnames, mandatory_footer='#END')
    actual = list(iterator.process(data))
    assert_dicts_equal(actual, expected)


def test_csv_iterator_incomplete_read():
    data = '151198;;2019-01-20;66;10;5.45;545\n'
    fieldnames = ['place_id', 'vid', 'dt', 'shows', 'clicks', 'bucks', 'mbucks']
    data = StringIO(data)
    iterator = CSVIterator(fieldnames=fieldnames, mandatory_footer='#END')
    with pytest.raises(IncompleteRead):
        list(iterator.process(data))
