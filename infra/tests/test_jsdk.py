import copy  # jsdk converters modify structures inplace

from infra.reconf_juggler.builders import DeclaredChecksBuilder
from infra.reconf_juggler.util.jsdk import jsdk2tree, tree2jsdk


class Builder(DeclaredChecksBuilder):
    default_check_class = 'infra.reconf_juggler.checks.META'

    def build_initial_tree(self, initial_data):
        return {
            'parent:META': {
                'children': {
                    'child_one:META': {
                        'children': {
                            'trimmed_grandchild:META': {},  # should not be dumped separately
                            'grandchild:META': None,  # should not be dumped separately
                            'usual_grandchild:META': {'children': {}},
                        },
                    },
                    'child_two:META': {
                        'children': {}
                    },
                },
            },
        }


def test_jsdk2tree():
    # There is no roundtrip here: trimmed checks can not be represented in jsdk
    # dump (at least for now), see 'trimmed_grandchild:META' body
    tree_dump = Builder().build('checks_tree')
    jsdk_dump = tree2jsdk(tree_dump)
    tree = jsdk2tree(jsdk_dump)

    expected = {
        'parent:META': {
            'children': {
                'child_one:META': {
                    'children': {
                        'grandchild:META': None,
                        'trimmed_grandchild:META': None,
                        'usual_grandchild:META': {
                            'children': {}
                        }
                    }
                },
                'child_two:META': {
                    'children': {}
                }
            }
        }
    }

    assert expected == tree


def test_jsdk2tree_parents_first():
    # reconf builders dumps children first in jsdk format, but other tools may
    # not follow this convention: https://st.yandex-team.ru/RUNTIMECLOUD-13619
    tree_dump = Builder().build('checks_tree')
    jsdk_dump = tree2jsdk(tree_dump)
    jsdk_dump.reverse()  # parents first now
    tree = jsdk2tree(jsdk_dump)

    expected = {
        'parent:META': {
            'children': {
                'child_one:META': {
                    'children': {
                        'grandchild:META': None,
                        'trimmed_grandchild:META': None,
                        'usual_grandchild:META': {
                            'children': {}
                        }
                    }
                },
                'child_two:META': {
                    'children': {}
                }
            }
        }
    }

    assert expected == tree


def test_jsdk2tree_several_parents():
    jsdk_dump = [
        {
            'host': 'kid',
            'service': 'ssh',
            'children': [],
        },
        {
            'host': 'dad',
            'service': 'ssh',
            'children': [{'host': 'kid', 'service': 'ssh'}],
        },
        {
            'host': 'mom',
            'service': 'ssh',
            'children': [{'host': 'kid', 'service': 'ssh'}],
        },
    ]

    expected = {
        'dad:ssh': {'children': {'kid:ssh': {'children': {}}}},
        'mom:ssh': {'children': {'kid:ssh': {'children': {}}}},
    }

    assert expected == jsdk2tree(jsdk_dump)


def test_jsdk2tree_kid_has_absent_children_key():
    # empty check in jsdk dump is definitely aggregate, not a filter
    jsdk_dump = [
        {
            'host': 'kid',
            'service': 'ssh',
        },
        {
            'host': 'dad',
            'service': 'ssh',
            'children': [{'host': 'kid', 'service': 'ssh'}],
        },
    ]

    expected = {'dad:ssh': {'children': {'kid:ssh': {}}}}

    assert expected == jsdk2tree(jsdk_dump)


def test_tree2jsdk():
    tree_dump = Builder().build('checks_tree')
    jsdk_dump = tree2jsdk(tree_dump)

    expected = [
        {
            'children': [],
            'host': 'usual_grandchild',
            'service': 'META'
        },
        {
            'children': [
                {
                    'group_type': 'HOST',
                    'host': 'grandchild',
                    'service': 'META'
                },
                {
                    'group_type': 'HOST',
                    'host': 'trimmed_grandchild',
                    'service': 'META'
                },
                {
                    'group_type': 'HOST',
                    'host': 'usual_grandchild',
                    'service': 'META'
                }
            ],
            'host': 'child_one',
            'service': 'META'
        },
        {
            'children': [],
            'host': 'child_two',
            'service': 'META'
        },
        {
            'children': [
                {
                    'group_type': 'HOST',
                    'host': 'child_one',
                    'service': 'META'
                },
                {
                    'group_type': 'HOST',
                    'host': 'child_two',
                    'service': 'META'
                }
            ],
            'host': 'parent',
            'service': 'META'
        }
    ]

    assert expected == jsdk_dump


def test_tree2jsdk_events_groups():
    tree_dump = {
        'parent:foo': {
            'children': {
                'EVENTS%service=foo&tag=f': None,
                'EVENTS%(service=foo&tag=f:all)|(service=bar&tag=b)': None,
            },
        },
    }
    jsdk_dump = tree2jsdk(tree_dump)

    expected = [
        {
            'host': 'parent',
            'service': 'foo',
            'children': [
                {
                    'group_type': 'EVENTS',
                    'host': '(service=foo&tag=f:all)|(service=bar&tag=b)',
                    'service': 'all',
                },
                {
                    'group_type': 'EVENTS',
                    'host': 'service=foo&tag=f',
                    'service': 'all',
                },
            ],
        },
    ]

    assert expected == jsdk_dump


def test_jsdk_skips_non_checks():
    dump = Builder().build('jsdk_dump')

    assert len(dump) == 4
    assert 'usual_grandchild' == dump[0]['host']
    assert 'child_one' == dump[1]['host']
    assert 'child_two' == dump[2]['host']
    assert 'parent' == dump[3]['host']


def test_instances():
    jsdk_dump = [
        {
            'children': [
                {
                    'group_type': 'HOST',
                    'host': 'child_one',
                    'instance': 'all',
                    'service': 'META'
                },
                {
                    'group_type': 'HOST',
                    'host': 'child_two',
                    'instance': '',
                    'service': 'META'
                },
            ],
            'host': 'parent',
            'service': 'META'
        }
    ]

    tree_dump = {'parent:META': {'children': {'child_one:META:all': None,
                                              'child_two:META': None}}}

    assert tree_dump == jsdk2tree(copy.deepcopy(jsdk_dump))

    # tree2jsdk does not produce empty instances, so drop it, rest should match
    del jsdk_dump[0]['children'][1]['instance']
    assert jsdk_dump == tree2jsdk(copy.deepcopy(tree_dump))
