from infra.reconf_juggler.resolvers import AbstractResolver

from infra.rtc.juggler.reconf.checks.hostman import AbstractHostmanUnitCheck
from infra.rtc.juggler.reconf.checks.unreachable import UNREACHABLE
from infra.rtc.juggler.reconf.checksets import CommonCheckSetBase


def test_hostman_units_templated_unit_name():
    class Resolver(AbstractResolver):
        pass

    class HostmanResolver(AbstractResolver):
        pass

    class HostmanUnitsResolver(AbstractResolver):
        def resolve_query(self, query):
            yield (
                'yandex-sol-rtc.yaml',
                {'meta': {'kind': 'SystemService', 'name': 'yandex-sol-rtc@'}},
                {},
                None,
            )
            yield (
                'yandex-hw-watcher@.yaml',
                {'meta': {'kind': 'TimerJob', 'name': 'yandex-hw-watcher@'}},
                {},
                None,
            )

    resolver = Resolver()
    resolver['hostman'] = HostmanResolver()
    resolver['hostman']['units'] = HostmanUnitsResolver()

    branches = CommonCheckSetBase({}, resolver=resolver).get_initial_branches()
    branches_names = tuple(x.__name__ for x in branches)

    assert 'hostman_unit_system_service_yandex-sol-rtc' in branches_names
    assert 'hostman_unit_timer_job_yandex-hw-watcher' in branches_names


def test_hostman_units_filtering():
    class Resolver(AbstractResolver):
        pass

    class HostmanResolver(AbstractResolver):
        pass

    class HostmanUnitsResolver(AbstractResolver):
        def resolve_query(self, query):
            yield (
                'foo.yaml',
                {'meta': {'kind': 'SystemService', 'name': 'foo'}},
                {},
                None,
            )
            yield (
                'bar.yaml',
                {'meta': {'kind': 'SystemService', 'name': 'bar'}},
                {'generate_aggregates': False},
                None,
            )

    resolver = Resolver()
    resolver['hostman'] = HostmanResolver()
    resolver['hostman']['units'] = HostmanUnitsResolver()

    branches = CommonCheckSetBase({}, resolver=resolver).get_initial_branches()
    branches_names = tuple(x.__name__ for x in branches)

    assert 'hostman_unit_system_service_foo' in branches_names
    assert 'hostman_unit_system_service_bar' not in branches_names


def test_hostman_units_class_override():
    class Resolver(AbstractResolver):
        pass

    class HostmanResolver(AbstractResolver):
        pass

    class HostmanUnitsResolver(AbstractResolver):
        def resolve_query(self, query):
            yield (
                'foo.yaml',
                {'meta': {'kind': 'SystemService', 'name': 'foo'}},
                {'base_class': 'infra.rtc.juggler.reconf.checks.unreachable.UNREACHABLE'},
                None,
            )
            yield (
                'bar.yaml',
                {'meta': {'kind': 'SystemService', 'name': 'bar'}},
                {'base_class': 'some.not.exissting.class'},
                None,
            )

    resolver = Resolver()
    resolver['hostman'] = HostmanResolver()
    resolver['hostman']['units'] = HostmanUnitsResolver()

    branches = CommonCheckSetBase({}, resolver=resolver).get_initial_branches()
    assert issubclass(branches[0], UNREACHABLE)
    assert issubclass(branches[1], AbstractHostmanUnitCheck)


def test_hostman_units_doc_url():
    class Resolver(AbstractResolver):
        pass

    class HostmanResolver(AbstractResolver):
        pass

    class HostmanUnitsResolver(AbstractResolver):
        def resolve_query(self, query):
            yield (
                'foo.yaml',
                {'meta': {'kind': 'SystemService', 'name': 'foo'}},
                {'doc_url': 'https://example.com/foo'},
                None,
            )
            yield (
                'bar.yaml',
                {'meta': {'kind': 'SystemService', 'name': 'bar'}},
                {'doc_url': None},
                None,
            )

    resolver = Resolver()
    resolver['hostman'] = HostmanResolver()
    resolver['hostman']['units'] = HostmanUnitsResolver()

    branches = CommonCheckSetBase({}, resolver=resolver).get_initial_branches()
    assert "https://example.com/foo" == branches[0].doc_url
    assert None is branches[1].doc_url


def test_hostman_units_broken_spec():
    class Resolver(AbstractResolver):
        pass

    class HostmanResolver(AbstractResolver):
        pass

    class HostmanUnitsResolver(AbstractResolver):
        def resolve_query(self, query):
            yield (
                'foo.yaml',
                None,
                None,
                'Error message',
            )
            yield (
                'bar.yaml',
                {'meta': {'kind': 'SystemService', 'name': 'bar'}},
                {'doc_url': None},
                None,
            )

    resolver = Resolver()
    resolver['hostman'] = HostmanResolver()
    resolver['hostman']['units'] = HostmanUnitsResolver()

    branches = CommonCheckSetBase({}, resolver=resolver).get_initial_branches()
    branches_names = tuple(x.__name__ for x in branches)

    assert 'hostman_unit_system_service_foo' not in branches_names
    assert 'hostman_unit_system_service_bar' in branches_names
