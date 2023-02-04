import pytest

from infra.reconf.resolvers import AbstractResolver


class SubResolver(AbstractResolver):
    def resolve_query(self, query):
        return query + ' resolved by subresolver'


class MetaResolver(AbstractResolver):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self['subresolver'] = SubResolver()

    def resolve_query(self, query):
        return query + ' resolved by metaresolver'


def test_subresolvers_access():
    resolver = MetaResolver()

    assert 'subresolver' in resolver
    assert 'no-such-resolver' not in resolver

    assert ['subresolver'] == list(resolver)
    assert SubResolver is resolver['subresolver'].__class__

    del resolver['subresolver']
    with pytest.raises(KeyError):
        resolver['subresolver']


def test_resolve():
    resolver = MetaResolver()
    assert 'foo resolved by metaresolver' == resolver.resolve('foo')
    assert 'bar resolved by subresolver' == resolver['subresolver'].resolve('bar')


def test_cache():
    resolver = MetaResolver()
    assert {} == resolver.dump_cache()

    resolver.load_cache(
        {
            'cache': {
                'foo': 'foo cached by metaresolver'
            },
            'sources': {
                'subresolver': {
                    'cache': {
                        'bar': 'bar cached by subresolver'
                    },
                },
            },
        }
    )

    assert 'foo cached by metaresolver' == resolver.resolve('foo')
    assert 'deadbeef resolved by metaresolver' == resolver.resolve('deadbeef')
    assert 'bar cached by subresolver' == resolver['subresolver'].resolve('bar')
    assert 'baz resolved by subresolver' == resolver['subresolver'].resolve('baz')

    expected = {
        'cache': {
            'deadbeef': 'deadbeef resolved by metaresolver',
            'foo': 'foo cached by metaresolver'
        },
        'sources': {
            'subresolver': {
                'cache': {
                    'bar': 'bar cached by subresolver',
                    'baz': 'baz resolved by subresolver'
                }
            }
        }
    }

    assert expected == resolver.dump_cache()


def test_cache_generators():
    class Resolver(AbstractResolver):
        def resolve_query(self, query):
            for i in ('foo', 'bar'):
                yield i

    resolver = Resolver()
    resolver.set_mode(use_cache=True)
    resolver.resolve('foobar')

    assert {'cache': {'foobar': ('foo', 'bar')}} == resolver.dump_cache()
