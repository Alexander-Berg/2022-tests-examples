from infra.reconf_juggler.checksets import HostCheckSet
from infra.reconf_juggler.resolvers import AbstractResolver, RootResolver
from infra.reconf_juggler.trees.locations import Locations
from infra.reconf_juggler.util.jsdk import tree2jsdk


def test_main_workflow():
    class OverridedResolver(AbstractResolver):
        def resolve_query(self, query):
            if query == 'F%prj-0&datacenter=iva':
                return 2

            return 0  # no instances, this branches should be trimmed

    resolver = RootResolver()
    resolver['juggler']['instances_count'] = OverridedResolver()
    children = {
        'prj-0': {'children': {'F%prj-0': None}, 'tags': ['grp_prj-0']},
        'prj-1': {'children': {'F%prj-1': None}, 'tags': ['grp_prj-1']},
    }

    tree = Locations(
        {'root': {'children': children, 'tags': ['foo']}},
        resolver=resolver,
    )

    expected = \
        {'children': {'root_msk_iva_prj-0': {'children': {'F%prj-0&datacenter=iva': None},
                                             'tags': ['dc_iva',
                                                      'foo',
                                                      'geo_msk',
                                                      'grp_prj-0']}},
         'tags': ['dc_iva', 'foo', 'geo_msk']}

    assert expected == tree['root']['children']['root_msk']['children']['root_msk_iva']

    checks = HostCheckSet(tree, resolver=resolver).build()

    expected = \
        {'active': 'ssh',
         'active_kwargs': {'timeout': 40},
         'aggregator': 'timed_more_than_limit_is_problem',
         'aggregator_kwargs': {'limits': [{'crit': '50.0%',
                                           'day_end': 7,
                                           'day_start': 1,
                                           'time_end': 23,
                                           'time_start': 0,
                                           'warn': '0%'}],
                               'unreach_mode': 'skip',
                               'unreach_service': [{'check': ':UNREACHABLE'},
                                                   {'check': ':META'}]},
         'check_options': None,
         'children': {'F%prj-0&datacenter=iva:ssh': None},
         'creation_time': None,
         'description': '',
         'meta': {'reconf': {'class': 'infra.reconf_juggler.checks.ssh'}},
         'mtime': None,
         'namespace': None,
         'notifications': [],
         'refresh_time': 90,
         'tags': ['category_infra',
                  'dc_iva',
                  'foo',
                  'geo_msk',
                  'grp_prj-0',
                  'level_leaf'],
         'ttl': 900}

    assert expected == checks['root:ssh']['children']['root_msk:ssh']['children']['root_msk_iva:ssh']['children']['root_msk_iva_prj-0:ssh']

    # check juggler-sdk converter
    converted = tree2jsdk(checks)
    expected = \
        {'aggregator': 'logic_or',
         'aggregator_kwargs': None,
         'children': [{'group_type': 'HOST', 'host': 'root_msk', 'service': 'ssh'}],
         'creation_time': None,
         'description': '',
         'host': 'root',
         'meta': {'reconf': {'class': 'infra.reconf_juggler.checks.ssh'}},
         'mtime': None,
         'namespace': None,
         'notifications': [],
         'refresh_time': 90,
         'service': 'ssh',
         'tags': ['category_infra', 'foo', 'level_root'],
         'ttl': 900}

    assert expected == converted[-1]  # metacheck

    expected = \
        {'active': 'ssh',
         'active_kwargs': {'timeout': 40},
         'aggregator': 'timed_more_than_limit_is_problem',
         'aggregator_kwargs': {'limits': [{'crit': '50.0%',
                                           'day_end': 7,
                                           'day_start': 1,
                                           'time_end': 23,
                                           'time_start': 0,
                                           'warn': '0%'}],
                               'unreach_mode': 'skip',
                               'unreach_service': [{'check': ':UNREACHABLE'},
                                                   {'check': ':META'}]},
         'check_options': None,
         'children': [{'group_type': 'F',
                       'host': 'prj-0&datacenter=iva',
                       'service': 'ssh'}],
         'creation_time': None,
         'description': '',
         'host': 'root_msk_iva_prj-0',
         'meta': {'reconf': {'class': 'infra.reconf_juggler.checks.ssh'}},
         'mtime': None,
         'namespace': None,
         'notifications': [],
         'refresh_time': 90,
         'service': 'ssh',
         'tags': ['category_infra',
                  'dc_iva',
                  'foo',
                  'geo_msk',
                  'grp_prj-0',
                  'level_leaf'],
         'ttl': 900}

    assert expected == converted[-4]  # leaf check
