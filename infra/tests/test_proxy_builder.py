import pytest

import infra.reconf_juggler.builders


# Use parent just to minimize copy-paste
class ParentBuilder(infra.reconf_juggler.builders.JugglerChecksBuilder):
    def build_initial_tree(self, initial_data):
        initial_tree = {}
        for i in initial_data:
            initial_tree[i] = None
        return initial_tree

    def build_checks_tree(self, initial_tree):
        for key in initial_tree:
            initial_tree[key] = ''
        return initial_tree

    def build_checks_full(self, checks_tree):
        for key in checks_tree:
            checks_tree[key] = {}
        return checks_tree

    def build_jsdk_dump(self, checks_tree):
        return list(checks_tree.keys())


class BuilderFoo(ParentBuilder):
    def build_initial_data(self, unused):
        return ['Foo:0', 'Foo:1']


class BuilderBar(ParentBuilder):
    def build_initial_data(self, unused):
        return ['Bar:0', 'Bar:1']


class TheProxyChecksBuilder(infra.reconf_juggler.builders.ProxyChecksBuilder):
    builders = (
        BuilderFoo,
        BuilderBar,
    )


def test_default_target():
    expected = {'Bar:0': {}, 'Bar:1': {}, 'Foo:0': {}, 'Foo:1': {}}
    assert expected == TheProxyChecksBuilder().build()


def test_build_initial_data():
    expected = ['Foo:0', 'Foo:1', 'Bar:0', 'Bar:1']
    assert expected == TheProxyChecksBuilder().build(target='initial_data')


def test_build_initial_tree():
    expected = {'Bar:0': None, 'Bar:1': None, 'Foo:0': None, 'Foo:1': None}
    assert expected == TheProxyChecksBuilder().build(target='initial_tree')


def test_build_checks_tree():
    expected = {'Bar:0': '', 'Bar:1': '', 'Foo:0': '', 'Foo:1': ''}
    assert expected == TheProxyChecksBuilder().build(target='checks_tree')


def test_build_checks_full():
    expected = {'Bar:0': {}, 'Bar:1': {}, 'Foo:0': {}, 'Foo:1': {}}
    assert expected == TheProxyChecksBuilder().build(target='checks_full')


def test_build_jsdk_dump():
    expected = ['Bar:0', 'Bar:1', 'Foo:0', 'Foo:1']
    assert expected == sorted(TheProxyChecksBuilder().build(target='jsdk_dump'))


def test_target_mismatch():
    class BuilderBaz(ParentBuilder):
        def build_initial_tree(self, initial_data):
            return ['Baz:0', 'Baz:1']

    class Proxy(infra.reconf_juggler.builders.ProxyChecksBuilder):
        builders = (BuilderFoo, BuilderBaz)

    with pytest.raises(RuntimeError):
        Proxy().build(target='initial_tree')


def test_target_unsupported():
    class BuilderBaz(ParentBuilder):
        def build_initial_tree(self, initial_data):
            return None

    class Proxy(infra.reconf_juggler.builders.ProxyChecksBuilder):
        builders = (BuilderFoo, BuilderBaz)

    with pytest.raises(RuntimeError):
        Proxy().build(target='initial_tree')


def test_resolver_propagated():
    class Proragated(object):
        pass

    class ToBuilder(infra.reconf_juggler.builders.JugglerChecksBuilder):
        def __init__(self, *args, resolver=None, **kwargs):
            if not isinstance(resolver, Proragated):
                raise RuntimeError('Resolver does not propagated!')

            super().__init__(*args, resolver=resolver, **kwargs)

        def build_initial_data(self, initial_data):
            return []

    class FromBuilder(infra.reconf_juggler.builders.ProxyChecksBuilder):
        builders = (ToBuilder,)

    assert [] == FromBuilder(resolver=Proragated()).build(target='initial_tree')
