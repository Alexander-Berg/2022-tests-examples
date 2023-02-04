import pytest

from infra.reconf_juggler import checks
from infra.reconf_juggler.builders import DeclaredChecksBuilder


def test_classes_propagating():
    class Builder(DeclaredChecksBuilder):
        default_check_class = 'infra.reconf_juggler.checks.UNREACHABLE'

        def build_initial_tree(self, initial_data):
            return {
                'root_default': {
                    'children': {
                        'child_default': {
                            'children': {
                                'alpha': {},
                            },
                        },
                        'child_redefined': {
                            'children': {
                                'redefined_propagated': {
                                    'children': {
                                        'alpha': {},
                                    },
                                },
                            },
                            'meta': {'reconf': {'class': 'infra.reconf_juggler.checks.ssh'}},
                        },
                    },
                },
                'root_defined': {
                    'children': {
                        'child_propagated_defined': {
                            'children': {
                                'alpha': {},
                            },
                        },
                    },
                    'meta': {'reconf': {'class': 'infra.reconf_juggler.checks.META'}},
                },
            }

    tree = Builder().build()
    assert isinstance(tree['root_default'], checks.UNREACHABLE)
    assert isinstance(tree['root_default']['children']
                      ['child_default'], checks.UNREACHABLE)
    assert isinstance(tree['root_default']['children']
                      ['child_redefined'], checks.ssh)
    assert isinstance(tree['root_default']['children']['child_redefined']
                      ['children']['redefined_propagated'], checks.ssh)
    assert isinstance(tree['root_defined'], checks.META)
    assert isinstance(tree['root_defined']['children']
                      ['child_propagated_defined'], checks.META)


def test_class_resolver_on_trimmed_aggregate():
    class Builder(DeclaredChecksBuilder):
        default_check_class = 'infra.reconf_juggler.checks.UNREACHABLE'

        def build_initial_tree(self, initial_data):
            return {'name': {}}  # should be skipped - empty body

    assert {'name': {}} == Builder().build()  # not resolved therefore not built


def test_class_resolver_on_nontrimmed_aggregate():
    class Builder(DeclaredChecksBuilder):
        default_check_class = 'infra.reconf_juggler.checks.UNREACHABLE'

        def build_initial_tree(self, initial_data):
            return {'name': {'anything in the body': 1}}

    assert 'icmpping' == Builder().build()['name']['active']  # resolved and built


def test_conflicting_names():
    class Builder(DeclaredChecksBuilder):
        def build_initial_tree(self, initial_data):
            tree = {
                '{one}{two}': {},
                '{one}{three}': {},
            }

            return super().build_initial_tree(tree)

        def get_names_map(self):
            keywords = super().get_names_map()
            keywords.update({
                'one': 'first',
                'two': '_same',
                'three': '_same'
            })

            return keywords

    with pytest.raises(RuntimeError):
        Builder().build()


def test_meta_urls():
    url_object = {
        'title': 'fake_title',
        'url': 'fake_url',
        'type': 'fake_type'
    }

    class Builder(DeclaredChecksBuilder):
        default_check_class = 'infra.reconf_juggler.checks.UNREACHABLE'

        def build_initial_tree(self, initial_data):
            return {
                'root': {
                    'meta': {
                        'urls': [url_object]
                    },
                    'children': {}
                }
            }

    tree = Builder().build('checks_full')
    assert tree['root']['meta']['urls'] == [url_object]
