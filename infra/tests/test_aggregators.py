from infra.reconf_juggler import Check, CheckSet
from infra.reconf_juggler.opts import aggregators
from infra.reconf_juggler.checksets import HostCheckSet


def test_default():
    class Foo(Check):
        validate_class = False
        opt_handlers = ('_aggregator',)

    check = Foo({'children': {'0': {}}}).build()

    expected = \
        {'aggregator': 'logic_or',
         'aggregator_kwargs': None,
         'children': {'0:Foo': {'aggregator': 'logic_or',
                                'aggregator_kwargs': None}}}
    assert expected == check


def test_LogicOr():
    class Foo(Check):
        validate_class = False
        opt_handlers = ('_aggregator',)
        _aggregator = aggregators.LogicOr

    check = Foo({'children': {'0': {}}}).build()

    expected = \
        {'aggregator': 'logic_or',
         'aggregator_kwargs': None,
         'children': {'0:Foo': {'aggregator': 'logic_or',
                                'aggregator_kwargs': None}}}
    assert expected == check


def test_TimedMoreThanLimitIsProblem():
    class Foo(Check):
        validate_class = False
        opt_handlers = ('_aggregator',)
        _aggregator = aggregators.TimedMoreThanLimitIsProblem

    check = Foo({'children': {'0': {}}}).build()

    expected = \
        {'aggregator': 'timed_more_than_limit_is_problem',
         'aggregator_kwargs': {'limits': [{'crit': '5%',
                                           'day_end': 7,
                                           'day_start': 1,
                                           'time_end': 23,
                                           'time_start': 0,
                                           'warn': '3%'}]},
         'children': {'0:Foo': {'aggregator': 'timed_more_than_limit_is_problem',
                                'aggregator_kwargs': {'limits': [{'crit': '5%',
                                                                  'day_end': 7,
                                                                  'day_start': 1,
                                                                  'time_end': 23,
                                                                  'time_start': 0,
                                                                  'warn': '3%'}]}}}}
    assert expected == check


def test_TimedMoreThanLimitIsProblem_unreach_service_calculated():
    cset = HostCheckSet({'0': {'children': {'00': {}}}}).build()

    assert [{'check': ':UNREACHABLE'}] == \
        cset['0:META']['children']['00:META']['aggregator_kwargs']['unreach_service']

    assert 'unreach_service' not in \
        cset['0:UNREACHABLE']['children']['00:UNREACHABLE']['aggregator_kwargs']

    assert [{'check': ':UNREACHABLE'}, {'check': ':META'}] == \
        cset['0:ssh']['children']['00:ssh']['aggregator_kwargs']['unreach_service']


def test_TimedMoreThanLimitIsProblem_unreach_service_overrided():
    class Foo(Check):
        validate_class = False
        opt_handlers = ('_aggregator',)
        _aggregator = aggregators.TimedMoreThanLimitIsProblem

        def get_aggr_unreach_service(self):
            return ({'check': ':META'}, {'check': ':UNREACHABLE'})

    check = Foo({'children': {}}).build()

    expected = [{'check': ':META'}, {'check': ':UNREACHABLE'}]
    assert expected == check['aggregator_kwargs']['unreach_service']
    assert 'skip' == check['aggregator_kwargs']['unreach_mode']


def test_TimedMoreThanLimitIsProblem_tune_limit_thresholds():
    aggr = aggregators.TimedMoreThanLimitIsProblem()

    limit = {'crit': '3%', 'warn': '1%'}
    aggr.tune_limit_thresholds(limit, 9)
    assert {'crit': '12.5%', 'warn': '0%'} == limit

    limit = {'crit': '3%', 'warn': '1%'}
    aggr.tune_limit_thresholds(limit, 100)
    assert {'crit': '3.23%', 'warn': '1.57%'} == limit

    limit = {'crit': '3%', 'warn': '1%'}
    aggr.tune_limit_thresholds(limit, 1000)
    assert {'crit': '2.71%', 'warn': '1.57%'} == limit


def test_unreach_service_hold():
    class Foo(Check):
        opt_handlers = ('_aggregator',)
        validate_class = False

    class Bar(Check):
        opt_handlers = ('_aggregator',)
        validate_class = False
        _unreach_service_classes = (Foo,)
        _unreach_service_hold = 42

    class CSet(CheckSet):
        branches = (Foo, Bar)

    cset = CSet({'0': {}}).build()

    assert [{'check': ':Foo', 'hold': 42}] == \
        cset['0:Bar']['aggregator_kwargs']['unreach_service']


def test_SelectiveAggregator():
    class Foo(Check):
        validate_class = False
        opt_handlers = ('_aggregator',)
        _aggregator = aggregators.SelectiveAggregator

    check = Foo({'children': {'0': {}}})

    expected = \
        {'aggregator': 'logic_or',
         'aggregator_kwargs': None,
         'children': {'0:Foo': {'aggregator': 'timed_more_than_limit_is_problem',
                                'aggregator_kwargs': {'limits': [{'crit': '5%',
                                                                  'day_end': 7,
                                                                  'day_start': 1,
                                                                  'time_end': 23,
                                                                  'time_start': 0,
                                                                  'warn': '3%'}]}}}}
    assert expected == check.build()
