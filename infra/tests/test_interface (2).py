from infra.reconf_juggler import Check, CheckSet
from infra.reconf_juggler.opts.aggregators import AggregatorHandler


def test_default_check_init():
    assert {} == Check({})


def test_default_check_build():
    expected = {
        'aggregator': 'logic_or',
        'aggregator_kwargs': None,
        'check_options': None,
        'creation_time': None,
        'description': '',
        'meta': {'reconf': {'class': 'infra.reconf_juggler.Check'}},
        'mtime': None,
        'namespace': None,
        'notifications': [],
        'refresh_time': 90,
        'tags': ['level_leaf', 'level_root'],
        'ttl': 900
    }

    assert expected == Check({}).build()


class META(Check):
    level_autotags = False
    validate_class = False
    opt_handlers = ('_ttl',)


class cron(Check):
    level_autotags = False
    validate_class = False
    opt_handlers = ('_tags',)


def test_custom_check_init():
    conf = META({'children': {'0': {}, '1': {}}})
    assert {'children': {'0:META': {}, '1:META': {}}} == conf


def test_custom_check_build():
    conf = META({'children': {'0': {}, '1': {}}})
    expected = {
        'children': {
            '0:META': {'ttl': 900},
            '1:META': {'ttl': 900},
        },
        'ttl': 900
    }
    assert expected == conf.build()


def test_custom_check_init_recursive():
    conf = cron({'children': {'0': {'children': {'00': {}}}, '1': {}}})
    expected = {
        'children': {
            '0:cron': {
                'children': {
                    '00:cron': {},
                },
            },
            '1:cron': {},
        },
    }
    assert expected == conf


def test_custom_check_build_recursive():
    conf = cron({'children': {'0': {'children': {'00': {}}}, '1': {}}})
    expected = {
        'children': {
            '0:cron': {
                'children': {
                    '00:cron': {'tags': []},
                },
                'tags': [],
            },
            '1:cron': {'tags': []},
        },
        'tags': [],
    }
    assert expected == conf.build()


def test_checkset_init():
    class FooCheckSet(CheckSet):
        branches = (META, cron)

    cset = FooCheckSet({'0': {}, '1': {}})
    assert {'0:META': {}, '1:META': {}, '0:cron': {}, '1:cron': {}} == cset


def test_checkset_init_recursive():
    class BarCheckSet(CheckSet):
        branches = (META,)

    cset = BarCheckSet({'0': {'children': {'00': {}}}, '1': {}})
    assert {'0:META': {'children': {'00:META': {}}}, '1:META': {}} == cset


def test_checkset_build_recursive():
    class ssh(Check):
        validate_class = False
        opt_handlers = ('_aggregator', '_ttl')

    class SSHCheckSet(CheckSet):
        branches = (ssh,)

    cset = SSHCheckSet({'0': {'children': {'00': {}}}}).build()
    expected = {
        '0:ssh': {
            'aggregator': 'logic_or',
            'aggregator_kwargs': None,
            'children': {
                '00:ssh': {
                    'aggregator': 'logic_or',
                    'aggregator_kwargs': None,
                    'ttl': 900
                },
            },
            'ttl': 900
        },
    }
    assert expected == cset


def test_origin_idx():
    class BarCheckSet(CheckSet):
        branches = (META, cron)

    cset = BarCheckSet({'0': {'children': {'00': {}}}, '1': {}}).build()

    for i in '0', '1', '00':
        assert i in cset['0:cron'].shared['origin_idx']
        assert i in cset['0:cron']['children']['00:cron'].shared['origin_idx']
        assert i in cset['1:META'].shared['origin_idx']


def test_tags_handled_before_aggregators():
    class TagsUsingAggregator(AggregatorHandler):
        def get_default_value(self, key):
            if key == 'aggregator':
                return 'aggregator_for_' + self._bound['tags'][0]

    class NewCheck(Check):
        validate_class = False
        _aggregator = TagsUsingAggregator

    conf = NewCheck({'tags': ['root']}).build()
    assert 'aggregator_for_root' == conf['aggregator']
