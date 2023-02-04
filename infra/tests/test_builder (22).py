import pytest

from infra.reconf_juggler.resolvers import AbstractResolver, RootResolver
from infra.rtc.juggler.reconf.builders.builder import Builder
from infra.rtc.juggler.reconf.checks import monitoring as monitoring_checks


class HostmanUnitsResolver(AbstractResolver):
    """ Pretend thre is no hostman units """
    def resolve_query(self, query):
        return []


class JugglerInstancesCountResolver(AbstractResolver):
    """ Pretend that any selector resolves into two instances. """
    def resolve_query(self, query):
        return 2


class WalleHostsCountResolver(AbstractResolver):
    """ Pretend any wall-e project has two hosts. """
    def resolve_query(self, query):
        return 2


class WalleProjectsResolver(AbstractResolver):
    def resolve_query(self, query):
        if query == {'tags': ('yt',), 'fields': ('id',)}:
            return [{'id': 'rtc-yt-mtn'}]

        if query == {'tags': ('yt',), 'fields': ('id', 'tags')}:
            return [{'id': 'rtc-yt-mtn', 'tags': ['rtc.yt_cluster-arnold']}]

        if query == {'tags': ('yp',), 'fields': ('id', 'tags')}:
            return [{'id': 'yp_workers', 'tags': ['yp']}]

        if query == {'tags': ('yp_masters',), 'fields': ('id',)}:
            return [{'id': 'yp_masters'}]

        if query == {'tags': ('runtime',), 'fields': ('id', 'tags')}:
            return [{'id': 'prj_runtime', 'tags': []}]

        if query == {'tags': ('rtc.stage-prestable',), 'fields': ('id', 'tags')}:
            return [{'id': 'prj_runtime_prestable', 'tags': []}]

        if query == {'tags': ('qloud',), 'fields': ('id',)}:
            return [{'id': 'prj_qloud'}]

        if query == {'tags': ('rtc',), 'fields': ('id',)}:
            return [{'id': 'prj_runtime'}]

        raise RuntimeError('Unknown query ' + repr(query))


RESOLVER = RootResolver()
RESOLVER['hostman']['units'] = HostmanUnitsResolver()
RESOLVER['juggler']['instances_count'] = JugglerInstancesCountResolver()
RESOLVER['walle']['projects'] = WalleProjectsResolver()
RESOLVER['walle']['hosts_count'] = WalleHostsCountResolver()
RESOLVER.set_mode(use_cache=False, use_fetch=True, propagate=True)

AGGRS_FULL_TREE = Builder(resolver=RESOLVER).build('checks_full')
AGGRS_JSDK_DUMP = Builder(resolver=RESOLVER).build('jsdk_dump')


def test_checks_full_target():
    """
    Check that jsdk dump target for full subbuilders tree works and contain
    checks from subbuilders.

    """
    roots = (
        'all_infra_prestable',
        'qloud',
        'rtc',
        'sysdev_experiment',
        'salt_masters',
        'yp',
        'yp_masters',
        'yt',
    )

    for name in roots:
        name += ':UNREACHABLE'
        assert name in AGGRS_FULL_TREE

    assert 'rtc_jobs:sandbox' in AGGRS_FULL_TREE
    assert 'rtc_janitor:status' in AGGRS_FULL_TREE
    assert 'rtc_sas_routes_duty:infrastructure_walle_tainted_kernel' in AGGRS_FULL_TREE  # builders/shared/panels/rtc
    assert 'sysdev_overall:hbf_rules_validation' in AGGRS_FULL_TREE
    assert 'hbf.drops.hcount.sas:hbf_drops_asearch_hostcount_geowide_sas_input' in AGGRS_FULL_TREE


def test_jsdk_dump_target():
    """
    Check that jsdk dump target for full subbuilders tree works and returns
    something useful.

    """
    assert 'host' in AGGRS_JSDK_DUMP[-1]


def test_all_aggregators_has_solomon_notification():
    solomon_notify_rule = {
        'description': 'st/RUNTIMECLOUD-9489',
        'template_name': 'solomon',
        'template_kwargs': {}
    }
    __tracebackhide__ = True

    for aggr in AGGRS_JSDK_DUMP:
        if solomon_notify_rule not in aggr['notifications']:
            pytest.fail('Aggregate {}:{} has no solomon notification'.format(
                aggr['host'], aggr['service']))


def test_monitoring_agents_aggrs_depends():
    """
    Ensure juggler and yasm agents active aggregates have no passive checks in
    depends, only UNREACHABLE and ssh. https://st.yandex-team.ru/HOSTMAN-465
    """
    agents_checks = {
        monitoring_checks.yasmagent_ping.provides(),
    }
    depends = [
        {'check': ':UNREACHABLE', 'hold': 600},
        {'check': ':ssh', 'hold': 600},
    ]

    for aggr in AGGRS_JSDK_DUMP:
        if aggr['service'] not in agents_checks:
            continue

        aggr_opts = aggr.get('aggregator_kwargs', None)
        if aggr_opts is None:
            continue  # metaaggregators have no depends

        if aggr_opts['unreach_service'] != depends:
            pytest.fail('Aggregate {}:{} has wrong depends'.format(
                aggr['host'], aggr['service']))
            break
